// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

public class HtmlController extends MoPubWebViewController {

    private String mClickThroughUrl;
    private BaseHtmlWebView.BaseWebViewListener mHtmlWebViewListener = new HtmlWebViewListener();

    public HtmlController(final @NonNull Context context,
                          final @Nullable String dspCreativeId,
                          final @Nullable String clickthroughUrl) {
        super(context, dspCreativeId);

        Preconditions.checkNotNull(clickthroughUrl);
        mClickThroughUrl = clickthroughUrl;
        mDefaultAdContainer.setLayoutParams(getLayoutParams());
    }

    @Override
    protected BaseWebView createWebView() {
        final HtmlWebView htmlWebView = new HtmlWebView(mContext);
        AdViewController.setShouldHonorServerDimensions(htmlWebView);
        htmlWebView.init(mHtmlWebViewListener, mClickThroughUrl, mDspCreativeId);
        return htmlWebView;
    }

    @Override
    protected void doFillContent(@NonNull String htmlData) {
        ((HtmlWebView) mWebView).loadHtmlResponse(htmlData);
    }

    @Override
    protected void destroy() {
        super.destroy();

        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    @Override
    protected ViewGroup.LayoutParams getLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
    }

    @NonNull
    @Override
    public View getAdContainer() {
        return (mWebView != null) ? mWebView : mDefaultAdContainer;
    }

    class HtmlWebViewListener implements BaseHtmlWebView.BaseWebViewListener {

        @Override
        public void onLoaded(View view) {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onLoaded(view);
            }
        }

        @Override
        public void onFailedToLoad(@NonNull MoPubErrorCode errorCode) {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onFailedToLoad(MoPubErrorCode.HTML_LOAD_ERROR);
            }
        }

        @Override
        public void onFailed() {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onFailed();
            }
        }

        @Override
        public void onRenderProcessGone(@NonNull MoPubErrorCode errorCode) {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onRenderProcessGone(errorCode);
            }
        }

        @Override
        public void onClicked() {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onClicked();
            }
        }

        @Override
        public void onExpand() {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onExpand();
            }
        }

        @Override
        public void onResize(boolean toOriginalSize) {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onResize(toOriginalSize);
            }
        }

        @Override
        public void onClose() {
            if (mBaseWebViewListener != null) {
                mBaseWebViewListener.onClose();
            }
        }
    }

    @VisibleForTesting
    public void setWebView(final BaseWebView baseWebView) {
        mWebView = baseWebView;
    }

    @VisibleForTesting
    public BaseWebView getWebView() {
        return mWebView;
    }
}
