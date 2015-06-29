package com.mopub.mobileads;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.logging.MoPubLog;

import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static com.mopub.common.util.VersionCode.currentApiLevel;
import static com.mopub.mobileads.ViewGestureDetector.UserClickListener;


public class BaseHtmlWebView extends BaseWebView implements UserClickListener {
    private final ViewGestureDetector mViewGestureDetector;
    private boolean mClicked;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public BaseHtmlWebView(Context context, AdReport adReport) {
        super(context);

        //RHT: This is to disable hardware acceleration, which is believed to be the cause of signal 11 SIGSEGV.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);

        mViewGestureDetector = new ViewGestureDetector(context, this, adReport);
        mViewGestureDetector.setUserClickListener(this);

        if (currentApiLevel().isAtLeast(ICE_CREAM_SANDWICH)) {
            enablePlugins(true);
        }
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void init(boolean isScrollable) {
        initializeOnTouchListener(isScrollable);
    }

    @Override
    public void loadUrl(String url) {
        if (url == null) return;

        MoPubLog.d("Loading url: " + url);
        if (url.startsWith("javascript:")) {
            // If destroy has been called on this webview, we won't be able to navigate
            try {
                super.loadUrl(url);
            } catch (NullPointerException e) {
                Log.e("MoPub", "Attempt to call javascript on destroyed WebView", e);
            }
        }
    }

    private void disableScrollingAndZoom() {
        setHorizontalScrollBarEnabled(false);
        setHorizontalScrollbarOverlay(false);
        setVerticalScrollBarEnabled(false);
        setVerticalScrollbarOverlay(false);
        getSettings().setSupportZoom(false);
    }

    void loadHtmlResponse(String htmlResponse) {
        loadDataWithBaseURL("http://" + Constants.HOST + "/", htmlResponse, "text/html", "utf-8",
                null);
    }

    void initializeOnTouchListener(final boolean isScrollable) {
        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mViewGestureDetector.sendTouchEvent(event);

                // We're not handling events if the current action is ACTION_MOVE
                return (event.getAction() == MotionEvent.ACTION_MOVE) && !isScrollable;
            }
        });
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
}
