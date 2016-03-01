package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;

import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static com.mopub.common.util.VersionCode.currentApiLevel;
import static com.mopub.mobileads.ViewGestureDetector.UserClickListener;

public class BaseHtmlWebView extends BaseWebView implements UserClickListener {
    private final ViewGestureDetector mViewGestureDetector;
    private boolean mClicked;

    public BaseHtmlWebView(Context context, AdReport adReport) {
        super(context);

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
            super.loadUrl(url);
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
        loadDataWithBaseURL(Networking.getBaseUrlScheme() + "://" + Constants.HOST + "/", htmlResponse,
                "text/html", "utf-8", null);
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
