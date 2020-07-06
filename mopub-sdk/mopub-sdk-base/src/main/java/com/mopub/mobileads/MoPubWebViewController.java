// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.util.WebViews;
import com.mopub.mraid.WebViewDebugListener;

import java.lang.ref.WeakReference;

public abstract class MoPubWebViewController {

    /**
     * Holds a weak reference to the activity if the context that is passed in is an activity.
     * While this field is never null, the reference could become null. This reference starts out
     * null if the passed-in context is not an activity.
     */
    @NonNull protected WeakReference<Activity> mWeakActivity;
    @NonNull protected final Context mContext;

    // An ad container, which contains the ad web view in default state, but is empty when expanded.
    @NonNull protected final ViewGroup mDefaultAdContainer;

    // Listeners
    @Nullable protected BaseHtmlWebView.BaseWebViewListener mBaseWebViewListener;
    @Nullable protected WebViewDebugListener mDebugListener;


    @Nullable protected BaseWebView mWebView;
    @Nullable protected String mDspCreativeId;

    protected boolean mIsPaused = true;


    public interface WebViewCacheListener {
        void onReady(final BaseWebView webView, final ExternalViewabilitySessionManager viewabilityManager);
    }

    public MoPubWebViewController(@NonNull Context context, @Nullable String dspCreativeId) {
        mContext = context.getApplicationContext();
        Preconditions.checkNotNull(mContext);
        mDspCreativeId = dspCreativeId;
        if (context instanceof Activity) {
            mWeakActivity = new WeakReference<>((Activity) context);
        } else {
            // Make sure mWeakActivity itself is never null, though the reference
            // it's pointing to could be null.
            mWeakActivity = new WeakReference<>(null);
        }
        mDefaultAdContainer = new FrameLayout(mContext);
    }

    protected abstract BaseWebView createWebView();
    protected abstract void doFillContent(@NonNull final String htmlData);

    /**
     * Creates a BaseWebView and fills it with data.
     *
     * @param htmlData            The HTML of the ad. This will only be loaded if a cached WebView
     *                            is not found.
     * @param listener            Optional listener that (if non-null) is notified when an
     *                            MraidWebView is loaded from the cache or created.
     */
    public final void fillContent(@NonNull final String htmlData,
                            @Nullable final WebViewCacheListener listener) {
        Preconditions.checkNotNull(htmlData, "htmlData cannot be null");

        mWebView = createWebView();

        if (listener != null) {
            listener.onReady(mWebView, null);
        }

        doFillContent(htmlData);
    }

    protected abstract ViewGroup.LayoutParams getLayoutParams();

    public void loadJavascript(@NonNull String javascript) {
        /* default no-op */
    }

    /**
     * Updates the activity and calls any onShow() callbacks when an ad is showing.
     *
     * @param activity The new activity associated with this mraid controller
     */
    public void onShow(@NonNull final Activity activity) {
        Preconditions.checkNotNull(activity);
        mWeakActivity = new WeakReference<>(activity);
    }

    protected void pause(boolean isFinishing) {
        mIsPaused = true;

        // This causes a video to pause if there is one playing
        if (mWebView != null) {
            WebViews.onPause(mWebView, isFinishing);
        }
    }

    protected void resume() {
        mIsPaused = false;

        // This causes a video to resume if it was playing previously
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    protected void destroy() {
        // Pause the controller to make sure the video gets stopped.
        if (!mIsPaused) {
            pause(true);
        }
    }

    @NonNull
    public Context getContext() {
        return mContext;
    }

    @NonNull
    WeakReference<Activity> getWeakActivity() {
        return mWeakActivity;
    }

    public void setMoPubWebViewListener(@Nullable BaseHtmlWebView.BaseWebViewListener baseWebViewListener) {
        mBaseWebViewListener = baseWebViewListener;
    }

    public void setDebugListener(@Nullable WebViewDebugListener debugListener) {
        mDebugListener = debugListener;
    }

    @NonNull
    public View getAdContainer() {
        return mDefaultAdContainer;
    }

    @VisibleForTesting
    public static class ScreenMetricsWaiter {
        public static class WaitRequest {
            @NonNull private final View[] mViews;
            @NonNull private final Handler mHandler;
            @Nullable private Runnable mSuccessRunnable;
            int mWaitCount;

            WaitRequest(@NonNull Handler handler, @NonNull final View[] views) {
                mHandler = handler;
                mViews = views;
            }

            void countDown() {
                mWaitCount--;
                if (mWaitCount == 0 && mSuccessRunnable != null) {
                    mSuccessRunnable.run();
                    mSuccessRunnable = null;
                }
            }

            final Runnable mWaitingRunnable = new Runnable() {
                @Override
                public void run() {
                    for (final View view : mViews) {
                        // Immediately count down for any views that already have a size
                        if (view.getHeight() > 0 || view.getWidth() > 0) {
                            countDown();
                            continue;
                        }

                        // For views that didn't have a size, listen (once) for a preDraw. Note
                        // that this doesn't leak because the ViewTreeObserver gets detached when
                        // the view is no longer part of the view hierarchy.
                        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                view.getViewTreeObserver().removeOnPreDrawListener(this);
                                countDown();
                                return true;
                            }
                        });
                    }
                }
            };

            public void start(@NonNull Runnable successRunnable) {
                mSuccessRunnable = successRunnable;
                mWaitCount = mViews.length;
                mHandler.post(mWaitingRunnable);
            }

            void cancel() {
                mHandler.removeCallbacks(mWaitingRunnable);
                mSuccessRunnable = null;
            }
        }

        @NonNull private final Handler mHandler = new Handler();
        @Nullable private WaitRequest mLastWaitRequest;

        public WaitRequest waitFor(@NonNull View... views) {
            mLastWaitRequest = new WaitRequest(mHandler, views);
            return mLastWaitRequest;
        }

        public void cancelLastRequest() {
            if (mLastWaitRequest != null) {
                mLastWaitRequest.cancel();
                mLastWaitRequest = null;
            }
        }
    }

    @VisibleForTesting
    public BaseHtmlWebView.BaseWebViewListener getBaseWebViewListener() {
        return mBaseWebViewListener;
    }
}
