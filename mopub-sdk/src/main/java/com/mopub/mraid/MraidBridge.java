package com.mopub.mraid;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.AdReport;
import com.mopub.common.CloseableLayout.ClosePosition;
import com.mopub.common.Constants;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.BaseWebView;
import com.mopub.mobileads.ViewGestureDetector;
import com.mopub.mobileads.ViewGestureDetector.UserClickListener;
import com.mopub.mobileads.resource.MraidJavascript;
import com.mopub.mraid.MraidBridge.MraidWebView.OnVisibilityChangedListener;
import com.mopub.mraid.MraidNativeCommandHandler.MraidCommandFailureListener;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class MraidBridge {
    private final AdReport mAdReport;

    public interface MraidBridgeListener {
        void onPageLoaded();

        void onPageFailedToLoad();

        void onVisibilityChanged(boolean isVisible);

        boolean onJsAlert(@NonNull String message, @NonNull JsResult result);

        boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage);

        void onResize(int width, int height, int offsetX,
                int offsetY, @NonNull ClosePosition closePosition, boolean allowOffscreen)
                throws MraidCommandException;

        void onExpand(URI uri, boolean shouldUseCustomClose) throws MraidCommandException;

        void onClose();

        void onUseCustomClose(boolean shouldUseCustomClose);

        void onSetOrientationProperties(boolean allowOrientationChange, MraidOrientation
                forceOrientation) throws MraidCommandException;

        void onOpen(URI uri);

        void onPlayVideo(URI uri);
    }

    private final String FILTERED_JAVASCRIPT_SOURCE = MraidJavascript.JAVASCRIPT_SOURCE
            .replaceAll("(?m)^\\s+", "")
            .replaceAll("(?m)^//.*(?=\\n)", "");

    @NonNull private final PlacementType mPlacementType;

    @NonNull private final MraidNativeCommandHandler mMraidNativeCommandHandler;

    @Nullable private MraidBridgeListener mMraidBridgeListener;

    @Nullable private MraidWebView mMraidWebView;

    private boolean mIsClicked;

    private boolean mHasLoaded;

    MraidBridge(@Nullable AdReport adReport, @NonNull PlacementType placementType) {
        this(adReport, placementType, new MraidNativeCommandHandler());
    }

    @VisibleForTesting
    MraidBridge(@Nullable AdReport adReport, @NonNull PlacementType placementType,
            @NonNull MraidNativeCommandHandler mraidNativeCommandHandler) {
        mAdReport = adReport;
        mPlacementType = placementType;
        mMraidNativeCommandHandler = mraidNativeCommandHandler;
    }

    void setMraidBridgeListener(@Nullable MraidBridgeListener listener) {
        mMraidBridgeListener = listener;
    }

    void attachView(@NonNull MraidWebView mraidWebView) {
        mMraidWebView = mraidWebView;
        mMraidWebView.getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (mPlacementType == PlacementType.INTERSTITIAL) {
                mraidWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            }
        }

        mMraidWebView.loadUrl("javascript:" + FILTERED_JAVASCRIPT_SOURCE);
        mMraidWebView.setScrollContainer(false);
        mMraidWebView.setVerticalScrollBarEnabled(false);
        mMraidWebView.setHorizontalScrollBarEnabled(false);
        mMraidWebView.setBackgroundColor(Color.BLACK);

        mMraidWebView.setWebViewClient(mMraidWebViewClient);

        mMraidWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(final WebView view, final String url, final String message,
                    final JsResult result) {
                if (mMraidBridgeListener != null) {
                    return mMraidBridgeListener.onJsAlert(message, result);
                }
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onConsoleMessage(@NonNull final ConsoleMessage consoleMessage) {
                if (mMraidBridgeListener != null) {
                    return mMraidBridgeListener.onConsoleMessage(consoleMessage);
                }
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onShowCustomView(final View view, final CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
            }
        });

        final ViewGestureDetector gestureDetector = new ViewGestureDetector(
                mMraidWebView.getContext(), mMraidWebView, mAdReport);
        gestureDetector.setUserClickListener(new UserClickListener() {
            @Override
            public void onUserClick() {
                mIsClicked = true;
            }

            @Override
            public void onResetUserClick() {
                mIsClicked = false;
            }

            @Override
            public boolean wasClicked() {
                return mIsClicked;
            }
        });

        mMraidWebView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                gestureDetector.sendTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });

        mMraidWebView.setVisibilityChangedListener(new OnVisibilityChangedListener() {
            @Override
            public void onVisibilityChanged(final boolean isVisible) {
                if (mMraidBridgeListener != null) {
                    mMraidBridgeListener.onVisibilityChanged(isVisible);
                }
            }
        });
    }

    void detach() {
        mMraidWebView = null;
    }

    public void setContentHtml(@NonNull String htmlData) {
        if (mMraidWebView == null) {
            MoPubLog.d("MRAID bridge called setContentHtml before WebView was attached");
            return;
        }

        mHasLoaded = false;
        mMraidWebView.loadDataWithBaseURL("http://" + Constants.HOST + "/",
                htmlData, "text/html", "UTF-8", null);
    }

    public void setContentUrl(String url) {
        if (mMraidWebView == null) {
            MoPubLog.d("MRAID bridge called setContentHtml while WebView was not attached");
            return;
        }

        mHasLoaded = false;
        mMraidWebView.loadUrl(url);
    }

    void injectJavaScript(@NonNull String javascript) {
        if (mMraidWebView == null) {
            MoPubLog.d("Attempted to inject Javascript into MRAID WebView while was not "
                    + "attached:\n\t" + javascript);
            return;
        }
        MoPubLog.v("Injecting Javascript into MRAID WebView:\n\t" + javascript);
        mMraidWebView.loadUrl("javascript:" + javascript);
    }

    private void fireErrorEvent(@NonNull MraidJavascriptCommand command, @NonNull String message) {
        injectJavaScript("window.mraidbridge.notifyErrorEvent("
                + JSONObject.quote(command.toJavascriptString()) + ", "
                + JSONObject.quote(message) + ")");
    }

    private void fireNativeCommandCompleteEvent(@NonNull MraidJavascriptCommand command) {
        injectJavaScript("window.mraidbridge.nativeCallComplete("
                + JSONObject.quote(command.toJavascriptString()) + ")");
    }

    public static class MraidWebView extends BaseWebView {
        public interface OnVisibilityChangedListener {
            void onVisibilityChanged(boolean isVisible);
        }

        @Nullable private OnVisibilityChangedListener mOnVisibilityChangedListener;

        private boolean mIsVisible;

        public MraidWebView(Context context) {
            super(context);
            mIsVisible = getVisibility() == View.VISIBLE;
        }

        void setVisibilityChangedListener(@Nullable OnVisibilityChangedListener listener) {
            mOnVisibilityChangedListener = listener;
        }

        @Override
        protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            boolean newIsVisible = (visibility == View.VISIBLE);
            if (newIsVisible != mIsVisible) {
                mIsVisible = newIsVisible;
                if (mOnVisibilityChangedListener != null) {
                    mOnVisibilityChangedListener.onVisibilityChanged(mIsVisible);
                }
            }
        }

        public boolean isVisible() {
            return mIsVisible;
        }
    }

    private final WebViewClient mMraidWebViewClient = new WebViewClient() {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            MoPubLog.d("Error: " + description);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) {
            return handleShouldOverrideUrl(url);
        }

        @Override
        public void onPageFinished(@NonNull WebView view, @NonNull String url) {
            handlePageFinished();
        }
    };

    @VisibleForTesting
    boolean handleShouldOverrideUrl(@NonNull final String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            MoPubLog.w("Invalid MRAID URL: " + url);
            fireErrorEvent(MraidJavascriptCommand.UNSPECIFIED, "Mraid command sent an invalid URL");
            return true;
        }

        // Note that scheme will be null when we are passed a relative Uri
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if ("mopub".equals(scheme)) {
            if ("failLoad".equals(host)) {
                if (mPlacementType == PlacementType.INLINE && mMraidBridgeListener != null) {
                    mMraidBridgeListener.onPageFailedToLoad();
                }
            }
            return true;
        }

        if ("mraid".equals(scheme)) {
            Map<String, String> params = new HashMap<String, String>();
            for (NameValuePair pair : URLEncodedUtils.parse(uri, "UTF-8")) {
                params.put(pair.getName(), pair.getValue());
            }
            MraidJavascriptCommand command = MraidJavascriptCommand.fromJavascriptString(host);
            try {
                runCommand(command, params);
            } catch (MraidCommandException exception) {
                fireErrorEvent(command, exception.getMessage());
            }
            fireNativeCommandCompleteEvent(command);
            return true;
        }

        // This block handles all other URLs, including sms://, tel://,
        // clicking a hyperlink, or setting window.location directly in Javascript. It checks for
        // clicked in order to avoid interfering with automatically browser redirects.
        if (mIsClicked) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                if (mMraidWebView == null) {
                    MoPubLog.d("WebView was detached. Unable to load a URL");
                    return true;
                }
                mMraidWebView.getContext().startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                MoPubLog.d("No activity found to handle this URL " + url);
                return false;
            }
        }

        return false;
    }

    @VisibleForTesting
    private void handlePageFinished() {
        // This can happen a second time if the ad does something that changes the window location,
        // such as a redirect, changing window.location in Javascript, or programmatically clicking
        // a hyperlink. Note that the handleShouldOverrideUrl method skips doing its own
        // processing if the user hasn't clicked the ad.
        if (mHasLoaded) {
            return;
        }

        mHasLoaded = true;
        if (mMraidBridgeListener != null) {
            mMraidBridgeListener.onPageLoaded();
        }
    }

    @VisibleForTesting
    void runCommand(@NonNull final MraidJavascriptCommand command,
            @NonNull Map<String, String> params)
            throws MraidCommandException {
        if (command.requiresClick(mPlacementType) && !mIsClicked) {
            throw new MraidCommandException("Cannot execute this command unless the user clicks");
        }

        if (mMraidBridgeListener == null) {
            throw new MraidCommandException("Invalid state to execute this command");
        }

        if (mMraidWebView == null) {
            throw new MraidCommandException("The current WebView is being destroyed");
        }

        switch (command) {
            case CLOSE:
                mMraidBridgeListener.onClose();
                break;
            case RESIZE:
                // All these params are required
                int width = checkRange(parseSize(params.get("width")), 0, 100000);
                int height = checkRange(parseSize(params.get("height")), 0, 100000);
                int offsetX = checkRange(parseSize(params.get("offsetX")), -100000, 100000);
                int offsetY = checkRange(parseSize(params.get("offsetY")), -100000, 100000);
                ClosePosition closePosition = parseClosePosition(
                        params.get("customClosePosition"), ClosePosition.TOP_RIGHT);
                boolean allowOffscreen = parseBoolean(params.get("allowOffscreen"), true);
                mMraidBridgeListener.onResize(
                        width, height, offsetX, offsetY, closePosition, allowOffscreen);
                break;
            case EXPAND:
                URI uri = parseURI(params.get("url"), null);
                boolean shouldUseCustomClose = parseBoolean(params.get("shouldUseCustomClose"),
                        false);
                mMraidBridgeListener.onExpand(uri, shouldUseCustomClose);
                break;
            case USE_CUSTOM_CLOSE:
                shouldUseCustomClose = parseBoolean(params.get("shouldUseCustomClose"), false);
                mMraidBridgeListener.onUseCustomClose(shouldUseCustomClose);
                break;
            case OPEN:
                uri = parseURI(params.get("url"));
                mMraidBridgeListener.onOpen(uri);
                break;
            case SET_ORIENTATION_PROPERTIES:
                boolean allowOrientationChange = parseBoolean(params.get("allowOrientationChange"));
                MraidOrientation forceOrientation = parseOrientation(params.get("forceOrientation"));

                mMraidBridgeListener.onSetOrientationProperties(allowOrientationChange,
                        forceOrientation);
                break;
            case PLAY_VIDEO:
                uri = parseURI(params.get("uri"));
                mMraidBridgeListener.onPlayVideo(uri);
                break;
            case STORE_PICTURE:
                uri = parseURI(params.get("uri"));
                mMraidNativeCommandHandler.storePicture(mMraidWebView.getContext(), uri.toString(),
                        new MraidCommandFailureListener() {
                            @Override
                            public void onFailure(final MraidCommandException exception) {
                                fireErrorEvent(command, exception.getMessage());
                            }
                        });
                break;

            case CREATE_CALENDAR_EVENT:
                mMraidNativeCommandHandler.createCalendarEvent(mMraidWebView.getContext(), params);
                break;
            case UNSPECIFIED:
                throw new MraidCommandException("Unspecified MRAID Javascript command");
        }
    }

    private ClosePosition parseClosePosition(@NonNull String text,
            @NonNull ClosePosition defaultValue)
            throws MraidCommandException {
        if (TextUtils.isEmpty(text)) {
            return defaultValue;
        }

        if (text.equals("top-left")) {
            return ClosePosition.TOP_LEFT;
        } else if (text.equals("top-right")) {
            return ClosePosition.TOP_RIGHT;
        } else if (text.equals("center")) {
            return ClosePosition.CENTER;
        } else if (text.equals("bottom-left")) {
            return ClosePosition.BOTTOM_LEFT;
        } else if (text.equals("bottom-right")) {
            return ClosePosition.BOTTOM_RIGHT;
        } else if (text.equals("top-center")) {
            return ClosePosition.TOP_CENTER;
        } else if (text.equals("bottom-center")) {
            return ClosePosition.BOTTOM_CENTER;
        } else {
            throw new MraidCommandException("Invalid close position: " + text);
        }
    }

    private int parseSize(@NonNull String text) throws MraidCommandException {
        int result;
        try {
            result = Integer.parseInt(text, 10);
        } catch (NumberFormatException e) {
            throw new MraidCommandException("Invalid numeric parameter: " + text);
        }
        return result;
    }

    private MraidOrientation parseOrientation(String text) throws MraidCommandException {
        if ("portrait".equals(text)) {
            return MraidOrientation.PORTRAIT;
        } else if ("landscape".equals(text)) {
            return MraidOrientation.LANDSCAPE;
        } else if ("none".equals(text)) {
            return MraidOrientation.NONE;
        } else {
            throw new MraidCommandException("Invalid orientation: " + text);
        }
    }

    private int checkRange(int value, int min, int max) throws MraidCommandException {
        if (value < min || value > max) {
            throw new MraidCommandException("Integer parameter out of range: " + value);
        }
        return value;
    }

    private boolean parseBoolean(
            @Nullable String text, boolean defaultValue) throws MraidCommandException {
        if (text == null) {
            return defaultValue;
        }
        return parseBoolean(text);
    }

    private boolean parseBoolean(final String text) throws MraidCommandException {
        if ("true".equals(text)) {
            return true;
        } else if ("false".equals(text)) {
            return false;
        }
        throw new MraidCommandException("Invalid boolean parameter: " + text);
    }

    @NonNull
    private URI parseURI(@Nullable String encodedText, URI defaultValue)
            throws MraidCommandException {
        if (encodedText == null) {
            return defaultValue;
        }
        return parseURI(encodedText);
    }

    @NonNull
    private URI parseURI(@Nullable String encodedText) throws MraidCommandException {
        if (encodedText == null) {
            throw new MraidCommandException("Parameter cannot be null");
        }
        try {
            return new URI(encodedText);
        } catch (URISyntaxException e) {
            throw new MraidCommandException("Invalid URL parameter: " + encodedText);
        }
    }

    void notifyViewability(boolean isViewable) {
        injectJavaScript("mraidbridge.setIsViewable("
                + isViewable
                + ")");
    }

    void notifyPlacementType(PlacementType placementType) {
        injectJavaScript("mraidbridge.setPlacementType("
                + JSONObject.quote(placementType.toJavascriptString())
                + ")");
    }

    void notifyViewState(ViewState state) {
        injectJavaScript("mraidbridge.setState("
                + JSONObject.quote(state.toJavascriptString())
                + ")");
    }

    void notifySupports(boolean sms, boolean telephone, boolean calendar,
            boolean storePicture, boolean inlineVideo) {
        injectJavaScript("mraidbridge.setSupports("
                + sms + "," + telephone + "," + calendar + "," + storePicture + "," + inlineVideo
                + ")");
    }

    @NonNull
    private String stringifyRect(Rect rect) {
        return rect.left + "," + rect.top + "," + rect.width() + "," + rect.height();
    }

    @NonNull
    private String stringifySize(Rect rect) {
        return rect.width() + "," + rect.height();
    }

    public void notifyScreenMetrics(@NonNull final MraidScreenMetrics screenMetrics) {
        injectJavaScript("mraidbridge.setScreenSize("
                + stringifySize(screenMetrics.getScreenRectDips())
                + ");mraidbridge.setMaxSize("
                + stringifySize(screenMetrics.getRootViewRectDips())
                + ");mraidbridge.setCurrentPosition("
                + stringifyRect(screenMetrics.getCurrentAdRectDips())
                + ");mraidbridge.setDefaultPosition("
                + stringifyRect(screenMetrics.getDefaultAdRectDips())
                + ")");
        injectJavaScript("mraidbridge.notifySizeChangeEvent("
                + stringifySize(screenMetrics.getCurrentAdRect())
                + ")");
    }

    void notifyReady() {
        injectJavaScript("mraidbridge.notifyReadyEvent();");
    }

    boolean isClicked() {
        return mIsClicked;
    }

    boolean isVisible() {
        return mMraidWebView != null && mMraidWebView.isVisible();
    }

    boolean isAttached() {
        return mMraidWebView != null;
    }

    boolean isLoaded() {
        return mHasLoaded;
    }

    @VisibleForTesting
    MraidWebView getMraidWebView() {
        return mMraidWebView;
    }

    @VisibleForTesting
    void setClicked(boolean clicked) {
        mIsClicked = clicked;
    }
}
