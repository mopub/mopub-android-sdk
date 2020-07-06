// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mopub.common.CloseableLayout;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.FullAdType;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Intents;
import com.mopub.mobileads.factories.HtmlControllerFactory;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.MraidVideoViewController;
import com.mopub.mraid.PlacementType;
import com.mopub.mraid.WebViewDebugListener;

import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_FAIL;
import static com.mopub.common.IntentActions.ACTION_REWARDED_AD_COMPLETE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.AdData.DEFAULT_DURATION_FOR_CLOSE_BUTTON_MILLIS;
import static com.mopub.mobileads.AdData.MILLIS_IN_SECOND;
import static com.mopub.mobileads.BaseBroadcastReceiver.broadcastAction;

public class FullscreenAdController implements BaseVideoViewController.BaseVideoViewControllerListener {

    @NonNull
    private final Activity mActivity;
    @Nullable
    private BaseVideoViewController mVideoViewController;
    @NonNull
    private final MoPubWebViewController mMoPubWebViewController;
    @NonNull
    private final AdData mAdData;
    @NonNull
    private ControllerState mState = ControllerState.MRAID;
    @Nullable
    private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;
    @Nullable
    private WebViewDebugListener mDebugListener;
    @Nullable
    private CloseableLayout mCloseableLayout;
    @Nullable
    private RadialCountdownWidget mRadialCountdownWidget;
    @Nullable
    private CloseButtonCountdownRunnable mCountdownRunnable;
    private int mCurrentElapsedTimeMillis;
    private int mShowCloseButtonDelayMillis;
    private boolean mShowCloseButtonEventFired;
    private boolean mIsCalibrationDone;
    private boolean mIsRewarded;

    private enum ControllerState {
        VIDEO,
        MRAID,
        HTML,
        IMAGE
    }

    public FullscreenAdController(@NonNull final Activity activity,
                                  @Nullable final Bundle savedInstanceState,
                                  @NonNull final Intent intent,
                                  @NonNull final AdData adData) {
        mActivity = activity;
        mAdData = adData;

        boolean preloaded = false;
        WebViewCacheService.Config config = null;
        config = WebViewCacheService.popWebViewConfig(adData.getBroadcastIdentifier());
        if (config != null && config.getController() != null) {
            preloaded = true;
            mMoPubWebViewController = config.getController();
        } else if ("html".equals(adData.getAdType())) {
            mMoPubWebViewController = HtmlControllerFactory.create(activity,
                    adData.getDspCreativeId(),
                    adData.getExtras().get(CLICKTHROUGH_URL_KEY));
        } else {
            // If we hit this, then we assume this is MRAID since it isn't HTML
            mMoPubWebViewController = new MraidController(activity,
                    adData.getDspCreativeId(),
                    PlacementType.INTERSTITIAL,
                    mAdData.getAllowCustomClose());
        }

        final String htmlData = adData.getAdPayload();
        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.log(CUSTOM, "MoPubFullscreenActivity received an empty HTML body. Finishing the activity.");
            activity.finish();
            return;
        }

        mMoPubWebViewController.setDebugListener(mDebugListener);
        mMoPubWebViewController.setMoPubWebViewListener(new BaseHtmlWebView.BaseWebViewListener() {
            @Override
            public void onLoaded(View view) {
                mMoPubWebViewController.loadJavascript(WEB_VIEW_DID_APPEAR.getJavascript());
            }

            @Override
            public void onFailedToLoad(MoPubErrorCode errorCode) {
                /* NO-OP. Loading has already completed if we're here */
            }

            @Override
            public void onFailed() {
                MoPubLog.log(CUSTOM, "FullscreenAdController failed to load. Finishing MoPubFullscreenActivity.");
                broadcastAction(activity, adData.getBroadcastIdentifier(),
                        ACTION_FULLSCREEN_FAIL);
                activity.finish();
            }

            @Override
            public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) {
                MoPubLog.log(CUSTOM, "Finishing the activity due to a problem: " + errorCode);
                activity.finish();
            }

            @Override
            public void onClicked() {
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_CLICK);
            }

            public void onClose() {
                broadcastAction(activity, adData.getBroadcastIdentifier(), ACTION_FULLSCREEN_DISMISS);
                mMoPubWebViewController.loadJavascript(WEB_VIEW_DID_CLOSE.getJavascript());
                activity.finish();
            }

            @Override
            public void onExpand() {
                // No-op. The interstitial is always expanded.
                int i = 0;
            }

            @Override
            public void onResize(final boolean toOriginalSize) {
                // No-op. The interstitial is always expanded.
                int i = 0;
            }
        });

        if (preloaded) {
            mExternalViewabilitySessionManager = config.getViewabilityManager();
        } else {
            mMoPubWebViewController.fillContent(htmlData, new MoPubWebViewController.WebViewCacheListener() {
                @Override
                public void onReady(final @NonNull BaseWebView webView,
                                    final @Nullable ExternalViewabilitySessionManager viewabilityManager) {
                    if (viewabilityManager != null) {
                        mExternalViewabilitySessionManager = viewabilityManager;
                    } else {
                        mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(
                                activity);
                        mExternalViewabilitySessionManager.createDisplaySession(
                                activity, webView);
                    }
                }
            });
        }

        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.startDeferredDisplaySession(activity);
        }

        mCloseableLayout = new CloseableLayout(mActivity);

        if (FullAdType.VAST.equals(mAdData.getFullAdType())) {
            mVideoViewController = createVideoViewController(activity, savedInstanceState, intent, adData.getBroadcastIdentifier());
            mState = ControllerState.VIDEO;
            mVideoViewController.onCreate();
        } else {
            if ("html".equals(mAdData.getAdType())) {
                mState = ControllerState.HTML;
            } else {
                mState = ControllerState.MRAID;
            }

            final int blackColor = mActivity.getResources().getColor(android.R.color.black);
            mCloseableLayout.setBackgroundColor(blackColor);
            mCloseableLayout.setOnCloseListener(new CloseableLayout.OnCloseListener() {
                @Override
                public void onClose() {
                    mActivity.finish();
                }
            });
            mCloseableLayout.addView(mMoPubWebViewController.getAdContainer(),
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            if (mAdData.isRewarded()) {
                mCloseableLayout.setCloseAlwaysInteractable(false);
                mCloseableLayout.setCloseVisible(false);
            }
            mActivity.setContentView(mCloseableLayout);
            mMoPubWebViewController.onShow(mActivity);

            if (mAdData.isRewarded()) {
                addRadialCountdownWidget(activity, View.INVISIBLE);
                mShowCloseButtonDelayMillis = adData.getRewardedDurationSeconds() >= 0 ?
                        adData.getRewardedDurationSeconds() * MILLIS_IN_SECOND :
                        DEFAULT_DURATION_FOR_CLOSE_BUTTON_MILLIS;
                mRadialCountdownWidget.calibrateAndMakeVisible(mShowCloseButtonDelayMillis);
                mIsCalibrationDone = true;
                final Handler mainHandler = new Handler(Looper.getMainLooper());
                mCountdownRunnable = new CloseButtonCountdownRunnable(this, mainHandler);
            } else {
                mShowCloseButtonEventFired = true;
            }
        }
    }

    @VisibleForTesting
    BaseVideoViewController createVideoViewController(Activity activity, Bundle savedInstanceState, Intent intent, Long broadcastIdentifier) throws IllegalStateException {
        if (FullAdType.VAST.equals(mAdData.getFullAdType())) {
            return new VastVideoViewController(activity, intent.getExtras(), savedInstanceState, broadcastIdentifier, this);
        } else {
            return new MraidVideoViewController(activity, intent.getExtras(), savedInstanceState, this);
        }
    }

    @Override
    public void onSetContentView(View view) {
        mActivity.setContentView(view);
    }

    @Override
    public void onSetRequestedOrientation(int requestedOrientation) {
        mActivity.setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onFinish() {
        mActivity.finish();
    }

    @Override
    public void onStartActivityForResult(Class<? extends Activity> clazz, int requestCode, Bundle extras) {
        if (clazz == null) {
            return;
        }

        final Intent intent = Intents.getStartActivityIntent(mActivity, clazz, extras);

        try {
            mActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            MoPubLog.log(CUSTOM, "Activity " + clazz.getName() + " not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    public void pause() {
        if (ControllerState.VIDEO.equals(mState) && mVideoViewController != null) {
            mVideoViewController.onPause();
        } else if (ControllerState.MRAID.equals(mState) || ControllerState.HTML.equals(mState)) {
            mMoPubWebViewController.pause(false);
        }
        stopRunnables();
    }

    public void resume() {
        if (ControllerState.VIDEO.equals(mState) && mVideoViewController != null) {
            mVideoViewController.onResume();
        } else if (ControllerState.MRAID.equals(mState) || ControllerState.HTML.equals(mState)) {
            mMoPubWebViewController.resume();
        }
        startRunnables();
    }

    public void destroy() {
        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }
        mMoPubWebViewController.destroy();
        if (mVideoViewController != null) {
            mVideoViewController.onDestroy();
        }
        stopRunnables();
        broadcastAction(mActivity, mAdData.getBroadcastIdentifier(), ACTION_FULLSCREEN_DISMISS);
    }

    boolean backButtonEnabled() {
        if (ControllerState.VIDEO.equals(mState) && mVideoViewController != null) {
            return mVideoViewController.backButtonEnabled();
        } else if (ControllerState.MRAID.equals(mState)) {
            return mShowCloseButtonEventFired;
        }
        return true;
    }

    private boolean isCloseable() {
        return !mShowCloseButtonEventFired && mCurrentElapsedTimeMillis >= mShowCloseButtonDelayMillis;
    }

    @VisibleForTesting
    void showCloseButton() {
        mShowCloseButtonEventFired = true;

        mRadialCountdownWidget.setVisibility(View.GONE);
        mCloseableLayout.setCloseVisible(true);

        if (!mIsRewarded) {
            broadcastAction(mActivity, mAdData.getBroadcastIdentifier(), ACTION_REWARDED_AD_COMPLETE);
            mIsRewarded = true;
        }
    }

    private void updateCountdown(int currentElapsedTimeMillis) {
        mCurrentElapsedTimeMillis = currentElapsedTimeMillis;
        if (mIsCalibrationDone) {
            mRadialCountdownWidget.updateCountdownProgress(mShowCloseButtonDelayMillis,
                    mCurrentElapsedTimeMillis);
        }
    }

    private void startRunnables() {
        if (mCountdownRunnable != null) {
            mCountdownRunnable.startRepeating(AdData.COUNTDOWN_UPDATE_INTERVAL_MILLIS);
        }
    }

    private void stopRunnables() {
        if (mCountdownRunnable != null) {
            mCountdownRunnable.stop();
        }
    }

    private void addRadialCountdownWidget(@NonNull final Context context, int initialVisibility) {
        mRadialCountdownWidget = new RadialCountdownWidget(context);
        mRadialCountdownWidget.setVisibility(initialVisibility);

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) mRadialCountdownWidget.getLayoutParams();
        final int widgetWidth = lp.width;
        final int widgetHeight = lp.height;

        FrameLayout.LayoutParams widgetLayoutParams =
                new FrameLayout.LayoutParams(widgetWidth, widgetHeight);
        widgetLayoutParams.rightMargin =
                Dips.dipsToIntPixels(DrawableConstants.CloseButton.EDGE_MARGIN, context);
        widgetLayoutParams.topMargin =
                Dips.dipsToIntPixels(DrawableConstants.CloseButton.EDGE_MARGIN, context);
        widgetLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        mCloseableLayout.addView(mRadialCountdownWidget, widgetLayoutParams);
    }

    static class CloseButtonCountdownRunnable extends RepeatingHandlerRunnable {
        @NonNull
        private final FullscreenAdController mController;
        private int mCurrentElapsedTimeMillis;

        private CloseButtonCountdownRunnable(@NonNull final FullscreenAdController controller,
                                             @NonNull final Handler handler) {
            super(handler);
            Preconditions.checkNotNull(handler);
            Preconditions.checkNotNull(controller);

            mController = controller;
        }

        @Override
        public void doWork() {
            mCurrentElapsedTimeMillis += mUpdateIntervalMillis;
            mController.updateCountdown(mCurrentElapsedTimeMillis);

            if (mController.isCloseable()) {
                mController.showCloseButton();
            }
        }

        @Deprecated
        @VisibleForTesting
        int getCurrentElapsedTimeMillis() {
            return mCurrentElapsedTimeMillis;
        }
    }

    @Deprecated
    @VisibleForTesting
    void setDebugListener(@Nullable final WebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        mMoPubWebViewController.setDebugListener(mDebugListener);
    }

    @Deprecated
    @VisibleForTesting
    int getShowCloseButtonDelayMillis() {
        return mShowCloseButtonDelayMillis;
    }

    @Deprecated
    @VisibleForTesting
    @NonNull
    CloseableLayout getCloseableLayout() {
        return mCloseableLayout;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    CloseButtonCountdownRunnable getCountdownRunnable() {
        return mCountdownRunnable;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    RadialCountdownWidget getRadialCountdownWidget() {
        return mRadialCountdownWidget;
    }

    @Deprecated
    @VisibleForTesting
    boolean isCalibrationDone() {
        return mIsCalibrationDone;
    }

    @Deprecated
    @VisibleForTesting
    boolean isRewarded() {
        return mIsRewarded;
    }

    @Deprecated
    @VisibleForTesting
    boolean isShowCloseButtonEventFired() {
        return mShowCloseButtonEventFired;
    }
}
