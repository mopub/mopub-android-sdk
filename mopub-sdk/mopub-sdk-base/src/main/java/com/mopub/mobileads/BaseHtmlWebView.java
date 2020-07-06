// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Constants;
import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class BaseHtmlWebView extends BaseWebView {

    public interface BaseWebViewListener {
        void onLoaded(View view);
        void onFailedToLoad(@NonNull final MoPubErrorCode errorCode);
        void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode);
        void onFailed();
        void onClicked();
        void onExpand();
        void onResize(final boolean toOriginalSize);
        void onClose();
    }

    @NonNull
    private final ViewGestureDetector mViewGestureDetector;

    public BaseHtmlWebView(Context context) {
        super(context);

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);

        mViewGestureDetector = new ViewGestureDetector(context);

        setBackgroundColor(Color.TRANSPARENT);
    }

    public void init() {
        initializeOnTouchListener();
    }

    @Override
    public void loadUrl(@Nullable final String url) {
        if (url == null) {
            return;
        }

        if (url.startsWith("javascript:")) {
            super.loadUrl(url);
            return;
        }

        MoPubLog.log(CUSTOM, "Loading url: " + url);
    }

    @Override
    public void stopLoading() {
        if (mIsDestroyed) {
            MoPubLog.log(CUSTOM, BaseHtmlWebView.class.getSimpleName() + "#stopLoading() called after destroy()");
            return;
        }

        final WebSettings webSettings = getSettings();
        if (webSettings == null) {
            MoPubLog.log(CUSTOM, BaseHtmlWebView.class.getSimpleName() + "#getSettings() returned null");
            return;
        }

        webSettings.setJavaScriptEnabled(false);
        super.stopLoading();
        webSettings.setJavaScriptEnabled(true);
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

    void initializeOnTouchListener() {
        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mViewGestureDetector.onTouchEvent(event);

                // We're not handling events if the current action is ACTION_MOVE
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });
    }

    public void onResetUserClick() {
        final ViewGestureDetector gestureDetector = mViewGestureDetector;
        if (gestureDetector != null) {
            gestureDetector.onResetUserClick();
        }
    }

    public boolean wasClicked() {
        final ViewGestureDetector gestureDetector = mViewGestureDetector;
        return gestureDetector != null && gestureDetector.isClicked();
    }
}
