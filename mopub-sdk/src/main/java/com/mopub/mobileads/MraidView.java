package com.mopub.mobileads;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Strings;
import com.mopub.mobileads.factories.HttpClientFactory;
import com.mopub.mobileads.resource.MraidJavascript;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.URI;
import java.util.*;

import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand;
import static com.mopub.mobileads.ViewGestureDetector.UserClickListener;

public class MraidView extends BaseWebView implements UserClickListener {
    private static final String LOGTAG = "MraidView";
    
    private MoPubBrowserController mBrowserController;
    private MraidDisplayController mDisplayController;
    
    private WebViewClient mWebViewClient;

    private boolean mHasFiredReadyEvent;
    private boolean mClicked;
    private final PlacementType mPlacementType;
    private ViewGestureDetector mViewGestureDetector;
    private AdConfiguration mAdConfiguration;
    private boolean mIsVisible;

    static class MraidListenerInfo {
        private MraidListener mMraidListener;
        private OnCloseButtonStateChangeListener mOnCloseButtonListener;
    }
    private MraidListenerInfo mListenerInfo;

    public enum ViewState {
        LOADING,
        DEFAULT,
        EXPANDED,
        HIDDEN
    }

    public enum ExpansionStyle {
        ENABLED,
        DISABLED
    }

    public enum NativeCloseButtonStyle {
        ALWAYS_VISIBLE,
        ALWAYS_HIDDEN,
        AD_CONTROLLED
    }

    public enum PlacementType {
        INLINE,
        INTERSTITIAL
    }

    public MraidView(Context context, AdConfiguration adConfiguration) {
        this(context, adConfiguration, ExpansionStyle.ENABLED, NativeCloseButtonStyle.AD_CONTROLLED,
                PlacementType.INLINE);
    }

    public MraidView(Context context, AdConfiguration adConfiguration, ExpansionStyle expStyle, NativeCloseButtonStyle buttonStyle,
                     PlacementType placementType) {
        super(context);
        mPlacementType = placementType;

        mAdConfiguration = adConfiguration;
        mViewGestureDetector = new ViewGestureDetector(context, this, adConfiguration);
        mViewGestureDetector.setUserClickListener(this);

        mIsVisible = (getVisibility() == View.VISIBLE);

        initialize(expStyle, buttonStyle);
    }

    @Override
    public void onUserClick() {
        mClicked = true;
    }

    @Override
    public void onResetUserClick() {
        mClicked = false;
    }

    @Override
    public boolean wasClicked() {
        return mClicked;
    }

    private void initialize(ExpansionStyle expStyle, NativeCloseButtonStyle buttonStyle) {
        setScrollContainer(false);
        setBackgroundColor(Color.TRANSPARENT);
        
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        
        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mViewGestureDetector.sendTouchEvent(event);

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
        
        getSettings().setJavaScriptEnabled(true);
        
        mBrowserController = new MoPubBrowserController(this);
        mDisplayController = new MraidDisplayController(this, expStyle, buttonStyle);
        
        mWebViewClient = new MraidWebViewClient();
        setWebViewClient(mWebViewClient);

        mListenerInfo = new MraidListenerInfo();
    }

    AdConfiguration getAdConfiguration() {
        return mAdConfiguration;
    }

    @Override
    public void destroy() {
        mDisplayController.destroy();
        super.destroy();
    }

    public void loadHtmlData(String data) {
        if (data == null) {
            return;
        }

        // If the string data lacks the HTML boilerplate, add it.
        if (!data.contains("<html>")) {
            data = "<html><head></head><body style='margin:0;padding:0;'>" + data +
                    "</body></html>";
        }
        
        // Inject the MRAID JavaScript bridge.
        data = data.replace("<head>", "<head><script>" + MraidJavascript.JAVASCRIPT_SOURCE + "</script>");

        loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
    }

    @Override
    public void loadUrl(String url) {
        if (url == null) {
            return;
        }

        if (url.startsWith("javascript:")) {
            super.loadUrl(url);
            return;
        }

        HttpClient httpClient = HttpClientFactory.create();
        String outString = "";
        
        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
    
            if (entity != null) {
                outString = Strings.fromStream(entity.getContent());
            }
        } catch (IllegalArgumentException e) {
            MoPubLog.d("Mraid loadUrl failed (IllegalArgumentException): " + url);
            notifyOnFailureListener();
            return;
        } catch (ClientProtocolException e) {
            notifyOnFailureListener();
            return;
        } catch (IOException e) {
            notifyOnFailureListener();
            return;
        }

        loadHtmlData(outString);
    }
    
    private void notifyOnFailureListener() {
        if (mListenerInfo.mMraidListener != null) {
            mListenerInfo.mMraidListener.onFailure(this);
        }
    }

    // Controllers /////////////////////////////////////////////////////////////////////////////////
    
    protected MoPubBrowserController getBrowserController() {
        return mBrowserController;
    }
    
    protected MraidDisplayController getDisplayController() {
        return mDisplayController;
    }
    
    // Listeners ///////////////////////////////////////////////////////////////////////////////////

    public void setMraidListener(MraidListener mraidListener) {
        mListenerInfo.mMraidListener = mraidListener;
    }

    public MraidListener getMraidListener() {
        return mListenerInfo.mMraidListener;
    }

    public void setOnCloseButtonStateChange(OnCloseButtonStateChangeListener listener) {
        mListenerInfo.mOnCloseButtonListener = listener;
    }
    
    public OnCloseButtonStateChangeListener getOnCloseButtonStateChangeListener() {
        return mListenerInfo.mOnCloseButtonListener;
    }
    
    // JavaScript injection ////////////////////////////////////////////////////////////////////////
    
    protected void injectJavaScript(String js) {
        if (js != null) super.loadUrl("javascript:" + js);
    }
    
    protected void fireChangeEventForProperty(MraidProperty property) {
        String json = "{" + property.toString() + "}";
        injectJavaScript("window.mraidbridge.fireChangeEvent(" + json + ");");
        Log.d(LOGTAG, "Fire change: " + json);
    }
    
    protected void fireChangeEventForProperties(ArrayList<MraidProperty> properties) {
        String props = properties.toString();
        if (props.length() < 2) return;
        
        String json = "{" + props.substring(1, props.length() - 1) + "}";
        injectJavaScript("window.mraidbridge.fireChangeEvent(" + json + ");");
        Log.d(LOGTAG, "Fire changes: " + json);
    }
    
    protected void fireErrorEvent(MraidJavascriptCommand mraidJavascriptCommand, String message) {
        String action = mraidJavascriptCommand.getCommand();

        injectJavaScript("window.mraidbridge.fireErrorEvent('" + action + "', '" + message + "');");
    }
    
    protected void fireReadyEvent() {
        injectJavaScript("window.mraidbridge.fireReadyEvent();");
    }
    
    protected void fireNativeCommandCompleteEvent(String command) {
        injectJavaScript("window.mraidbridge.nativeCallComplete('" + command + "');");
    }
    
    private boolean tryCommand(URI uri) {
        String commandType = uri.getHost();
        List<NameValuePair> list = URLEncodedUtils.parse(uri, "UTF-8");
        Map<String, String> params = new HashMap<String, String>();
        for (NameValuePair pair : list) {
            params.put(pair.getName(), pair.getValue());
        }

        MraidCommand command = MraidCommandFactory.create(commandType, params, this);

        if (command == null) {
            fireNativeCommandCompleteEvent(commandType);
            return false;
        } else if (command.isCommandDependentOnUserClick(mPlacementType) && !wasClicked()) {
            return false;
        } else {
            command.execute();
            fireNativeCommandCompleteEvent(commandType);
            return true;
        }
    }

    private class MraidWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d(LOGTAG, "Error: " + description);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);

            // Note that scheme will be null when we are passed a relative Uri
            String scheme = uri.getScheme();

            if ("mopub".equals(scheme)) {
                return true;
            }

            if ("mraid".equals(scheme)) {
                tryCommand(URI.create(url)); // java.net.URI, not android.net.Uri
                return true;
            }

            if (wasClicked()) {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    getContext().startActivity(i);
                    return true;
                } catch (ActivityNotFoundException e) {
                    return false;
                }
            }

            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            if (!mHasFiredReadyEvent) {
                mDisplayController.initializeJavaScriptState();
                fireChangeEventForProperty(MraidPlacementTypeProperty.createWithType(mPlacementType));
                fireReadyEvent();
                if (getMraidListener() != null) {
                    getMraidListener().onReady(MraidView.this);
                }
                mIsVisible = (getVisibility() == View.VISIBLE);
                fireChangeEventForProperty(MraidViewableProperty.createWithViewable(mIsVisible));
                mHasFiredReadyEvent = true;
            }
        }
        
        @Override
        public void onLoadResource(WebView view, String url) {
            Log.d(LOGTAG, "Loaded resource: " + url);
        }
    }

    public interface MraidListener {
        public void onReady(MraidView view);
        public void onFailure(MraidView view);
        public void onExpand(MraidView view);
        public void onOpen(MraidView view);
        public void onClose(MraidView view, ViewState newViewState);
    }

    public static class BaseMraidListener implements MraidListener {
        @Override public void onReady(MraidView view) { }
        @Override public void onFailure(MraidView view) { }
        @Override public void onExpand(MraidView view) { }
        @Override public void onOpen(MraidView view) { }
        @Override public void onClose(MraidView view, ViewState newViewState) { }
    }

    public interface OnCloseButtonStateChangeListener {
        public void onCloseButtonStateChange(MraidView view, boolean enabled);
    }

    public boolean getIsVisible() {
        return mIsVisible;
    }

    @Override
    protected void onVisibilityChanged (View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        boolean newIsVisible = (visibility == View.VISIBLE);
        if (newIsVisible != mIsVisible) {
            mIsVisible = newIsVisible;
            if (mHasFiredReadyEvent) {
                fireChangeEventForProperty(MraidViewableProperty.createWithViewable(mIsVisible));
            }
        }
    }

    @Deprecated // for testing
    void setHasFiredReadyEvent(boolean hasFired) {
        mHasFiredReadyEvent = hasFired;
    }

    @Deprecated // for testing
    WebViewClient getMraidWebViewClient() {
        return mWebViewClient;
    }

    @Deprecated // for testing
    void setMraidDisplayController(MraidDisplayController mraidDisplayController) {
        mDisplayController = mraidDisplayController;
    }
}
