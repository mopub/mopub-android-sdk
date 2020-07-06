// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.mopub.common.CloseableLayout.ClosePosition;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibilityTracker;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.BaseWebView;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.ViewGestureDetector;
import com.mopub.network.Networking;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.mobileads.MoPubErrorCode.RENDER_PROCESS_GONE_UNSPECIFIED;
import static com.mopub.mobileads.MoPubErrorCode.RENDER_PROCESS_GONE_WITH_CRASH;
import static com.mopub.network.MoPubRequestUtils.getQueryParamMap;

public class MraidBridge {
    public interface MraidBridgeListener {
        void onPageLoaded();

        void onPageFailedToLoad();

        void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode);

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

    static final String MRAID_OPEN = "mraid://open?url=";

    @NonNull private final PlacementType mPlacementType;

    @NonNull private final MraidNativeCommandHandler mMraidNativeCommandHandler;

    @Nullable private MraidBridgeListener mMraidBridgeListener;

    @Nullable private MraidWebView mMraidWebView;

    @Nullable private ViewGestureDetector mGestureDetector;

    private boolean mHasLoaded;

    private boolean mAllowCustomClose;

    MraidBridge(@NonNull final PlacementType placementType, final boolean allowCustomClose) {
        this(placementType, new MraidNativeCommandHandler(), allowCustomClose);
    }

    @VisibleForTesting
    MraidBridge(@NonNull final PlacementType placementType,
                @NonNull final MraidNativeCommandHandler mraidNativeCommandHandler,
                final boolean allowCustomClose) {
        mPlacementType = placementType;
        mMraidNativeCommandHandler = mraidNativeCommandHandler;
        mAllowCustomClose = allowCustomClose;
    }

    void setMraidBridgeListener(@Nullable MraidBridgeListener listener) {
        mMraidBridgeListener = listener;
    }

    void attachView(@NonNull MraidWebView mraidWebView) {
        mMraidWebView = mraidWebView;
        mMraidWebView.getSettings().setJavaScriptEnabled(true);

        if (mPlacementType == PlacementType.INTERSTITIAL) {
            mraidWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

        mMraidWebView.setScrollContainer(false);
        mMraidWebView.setVerticalScrollBarEnabled(false);
        mMraidWebView.setHorizontalScrollBarEnabled(false);
        mMraidWebView.setBackgroundColor(Color.TRANSPARENT);

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

        mGestureDetector = new ViewGestureDetector(mMraidWebView.getContext());

        mMraidWebView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                mGestureDetector.onTouchEvent(event);

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

        mMraidWebView.setVisibilityChangedListener(new MraidWebView.OnVisibilityChangedListener() {
            @Override
            public void onVisibilityChanged(final boolean isVisible) {
                if (mMraidBridgeListener != null) {
                    mMraidBridgeListener.onVisibilityChanged(isVisible);
                }
            }
        });
    }

    void detach() {
        if (mMraidWebView != null) {
            mMraidWebView.destroy();
            mMraidWebView = null;
        }
    }

    public void setContentHtml(@NonNull String htmlData) {
        if (mMraidWebView == null) {
            MoPubLog.log(CUSTOM, "MRAID bridge called setContentHtml before WebView was attached");
            return;
        }

        mHasLoaded = false;
        mMraidWebView.loadDataWithBaseURL(Networking.getBaseUrlScheme() + "://" + Constants.HOST + "/",
                htmlData, "text/html", "UTF-8", null);
    }

    public void setContentUrl(String url) {
        if (mMraidWebView == null) {
            MoPubLog.log(CUSTOM, "MRAID bridge called setContentHtml while WebView was not attached");
            return;
        }

        mHasLoaded = false;
        mMraidWebView.loadUrl(url);
    }

    void injectJavaScript(@NonNull String javascript) {
        if (mMraidWebView == null) {
            MoPubLog.log(CUSTOM, "Attempted to inject Javascript into MRAID WebView while was not "
                    + "attached:\n\t" + javascript);
            return;
        }
        MoPubLog.log(CUSTOM, "Injecting Javascript into MRAID WebView:\n\t" + javascript);
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

        private static final int DEFAULT_MIN_VISIBLE_PX = 1;

        public interface OnVisibilityChangedListener {
            void onVisibilityChanged(boolean isVisible);
        }

        @Nullable private OnVisibilityChangedListener mOnVisibilityChangedListener;
        @Nullable private VisibilityTracker mVisibilityTracker;

        private boolean mMraidViewable;

        public MraidWebView(Context context) {
            super(context);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // Android 22 and lower has a bug where onVisibilityChanged is not called all
                // the time when views are attached.
                mMraidViewable = getVisibility() == View.VISIBLE;
                return;
            }

            mVisibilityTracker = new VisibilityTracker(context);
            final VisibilityTracker.VisibilityTrackerListener visibilityTrackerListener = new VisibilityTracker.VisibilityTrackerListener() {
                @Override
                public void onVisibilityChanged(@NonNull final List<View> visibleViews,
                        @NonNull final List<View> invisibleViews) {
                    Preconditions.checkNotNull(visibleViews);
                    Preconditions.checkNotNull(invisibleViews);

                    setMraidViewable(visibleViews.contains(MraidWebView.this));
                }
            };
            mVisibilityTracker.setVisibilityTrackerListener(visibilityTrackerListener);
        }

        void setVisibilityChangedListener(@Nullable OnVisibilityChangedListener listener) {
            mOnVisibilityChangedListener = listener;
        }

        @Override
        protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (mVisibilityTracker == null) {
                setMraidViewable(visibility == View.VISIBLE);
                return;
            }
            if (visibility == View.VISIBLE) {
                mVisibilityTracker.clear();
                mVisibilityTracker.addView(changedView, this, 0, 0, DEFAULT_MIN_VISIBLE_PX);
            } else {
                mVisibilityTracker.removeView(this);
                setMraidViewable(false);
            }
        }

        private void setMraidViewable(final boolean viewable) {
            if (mMraidViewable == viewable) {
                return;
            }
            mMraidViewable = viewable;
            if (mOnVisibilityChangedListener != null) {
                mOnVisibilityChangedListener.onVisibilityChanged(viewable);
            }
        }

        public boolean isMraidViewable() {
            return mMraidViewable;
        }

        @Override
        public void destroy() {
            super.destroy();
            mVisibilityTracker = null;
            mOnVisibilityChangedListener = null;
        }
    }

    private final WebViewClient mMraidWebViewClient = new MraidWebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) {
            return handleShouldOverrideUrl(url);
        }

        @Override
        public void onPageFinished(@NonNull WebView view, @NonNull String url) {
            handlePageFinished();
        }

        @Override
        public void onReceivedError(@NonNull WebView view, int errorCode,
                @NonNull String description, @NonNull String failingUrl) {
            MoPubLog.log(CUSTOM, "Error: " + description);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @Override
        public boolean onRenderProcessGone(@Nullable final WebView view, @Nullable final RenderProcessGoneDetail detail) {
            handleRenderProcessGone(detail);
            return true;
        }
    };

    @VisibleForTesting
    boolean handleShouldOverrideUrl(@NonNull final String url) {
        try {
            // This is purely for validating the URI before proceeding
            final URI uri = new URI(url);
        } catch (URISyntaxException e) {
            MoPubLog.log(CUSTOM, "Invalid MRAID URL: " + url);
            fireErrorEvent(MraidJavascriptCommand.UNSPECIFIED, "Mraid command sent an invalid URL");
            return true;
        }

        Uri uri = Uri.parse(url);

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

        // This block converts all other URLs, including sms://, tel:// into MRAID URL. It checks for
        // 'clicked' in order to avoid interfering with automatic browser redirects.
        if (isClicked() && !"mraid".equals(scheme)) {
            try {
                uri = Uri.parse(MRAID_OPEN + URLEncoder.encode(url, "UTF-8"));
                host = uri.getHost();
                scheme = uri.getScheme();
            } catch (UnsupportedEncodingException e) {
                MoPubLog.log(CUSTOM, "Invalid MRAID URL encoding: " + url);
                fireErrorEvent(MraidJavascriptCommand.OPEN, "Non-mraid URL is invalid");
                return false;
            }
        }

        if ("mraid".equals(scheme)) {
            MraidJavascriptCommand command = MraidJavascriptCommand.fromJavascriptString(host);
            try {
                runCommand(command, getQueryParamMap(uri));
            } catch (MraidCommandException | IllegalArgumentException exception) {
                fireErrorEvent(command, exception.getMessage());
            }
            fireNativeCommandCompleteEvent(command);
            return true;
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
    @TargetApi(Build.VERSION_CODES.O)
    void handleRenderProcessGone(@Nullable final RenderProcessGoneDetail detail) {
        final MoPubErrorCode errorCode = (detail != null && detail.didCrash())
                ? RENDER_PROCESS_GONE_WITH_CRASH
                : RENDER_PROCESS_GONE_UNSPECIFIED;

        MoPubLog.log(CUSTOM, errorCode);
        detach();

        if (mMraidBridgeListener != null) {
            mMraidBridgeListener.onRenderProcessGone(errorCode);
        }
    }

    @VisibleForTesting
    void runCommand(@NonNull final MraidJavascriptCommand command,
            @NonNull Map<String, String> params)
            throws MraidCommandException {
        if (command.requiresClick(mPlacementType) && !isClicked()) {
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
                mMraidBridgeListener.onUseCustomClose(mAllowCustomClose);
                break;
            case EXPAND:
                URI uri = parseURI(params.get("url"), null);
                mMraidBridgeListener.onExpand(uri, parseAllowCustomClose(params, mAllowCustomClose));
                break;
            case USE_CUSTOM_CLOSE:
                mMraidBridgeListener.onUseCustomClose(parseAllowCustomClose(params, mAllowCustomClose));
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
                // STORE_PICTURE and CREATE_CALENDAR_EVENT are no longer supported
            case STORE_PICTURE:
            case CREATE_CALENDAR_EVENT:
                throw new MraidCommandException("Unsupported MRAID Javascript command");
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

    private static boolean parseAllowCustomClose(@NonNull final Map<String, String> params,
                                                 final boolean allowCustomClose) throws MraidCommandException {
        boolean useCustomClose = parseBoolean(params.get("shouldUseCustomClose"), false);
        useCustomClose &= allowCustomClose;
        return useCustomClose;
    }

    private int checkRange(int value, int min, int max) throws MraidCommandException {
        if (value < min || value > max) {
            throw new MraidCommandException("Integer parameter out of range: " + value);
        }
        return value;
    }

    private static boolean parseBoolean(
            @Nullable String text, boolean defaultValue) throws MraidCommandException {
        if (text == null) {
            return defaultValue;
        }
        return parseBoolean(text);
    }

    private static boolean parseBoolean(final String text) throws MraidCommandException {
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
                + stringifySize(screenMetrics.getCurrentAdRectDips())
                + ")");
    }

    void notifyReady() {
        injectJavaScript("mraidbridge.notifyReadyEvent();");
    }

    boolean isClicked() {
        final ViewGestureDetector gDetector = mGestureDetector;
        return gDetector != null && gDetector.isClicked();
    }

    boolean isViewable() {
        final MraidWebView mraidWebView = mMraidWebView;
        return mraidWebView != null && mraidWebView.isMraidViewable();
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
        final ViewGestureDetector gDetector = mGestureDetector;
        if (gDetector != null) {
            gDetector.setClicked(clicked);
        }
    }
}
