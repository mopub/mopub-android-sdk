// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.mraid.MraidBridge;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.MraidController.MraidListener;
import com.mopub.mraid.MraidWebViewDebugListener;
import com.mopub.mraid.PlacementType;
import com.mopub.mraid.RewardedMraidController;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.REWARDED_AD_DURATION_KEY;
import static com.mopub.common.DataKeys.SHOULD_REWARD_ON_CLICK_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;

public class RewardedMraidActivity extends MraidActivity {
    @Nullable private RewardedMraidController mRewardedMraidController;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    public static void preRenderHtml(@NonNull final Interstitial mraidInterstitial,
            @NonNull final Context context,
            @NonNull final CustomEventInterstitial.CustomEventInterstitialListener customEventInterstitialListener,
            @NonNull final Long broadcastIdentifier,
            @Nullable final AdReport adReport,
            final int rewardedDuration) {
        Preconditions.checkNotNull(mraidInterstitial);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventInterstitialListener);
        Preconditions.checkNotNull(broadcastIdentifier);

        preRenderHtml(mraidInterstitial, customEventInterstitialListener, getResponseString(adReport),
                new MraidBridge.MraidWebView(context), broadcastIdentifier,
                new RewardedMraidController(context, adReport, PlacementType.INTERSTITIAL,
                        rewardedDuration, broadcastIdentifier));
    }

    public static void start(@NonNull Context context, @Nullable AdReport adreport,
                             long broadcastIdentifier, int rewardedDuration,
                             boolean shouldRewardOnClick) {
        final Intent intent = createIntent(context, adreport, broadcastIdentifier,
                rewardedDuration, shouldRewardOnClick);
        try {
            Intents.startActivity(context, intent);
        } catch (IntentNotResolvableException exception) {
            Log.d("RewardedMraidActivity", "RewardedMraidActivity.class not found. " +
                    "Did you declare RewardedMraidActivity in your manifest?");
        }
    }

    @VisibleForTesting
    protected static Intent createIntent(@NonNull Context context, @Nullable AdReport adReport,
                                         long broadcastIdentifier, int rewardedDuration,
                                         boolean shouldRewardOnClick) {
        Intent intent = new Intent(context, RewardedMraidActivity.class);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        intent.putExtra(AD_REPORT_KEY, adReport);
        intent.putExtra(REWARDED_AD_DURATION_KEY, rewardedDuration);
        intent.putExtra(SHOULD_REWARD_ON_CLICK_KEY, shouldRewardOnClick);
        return intent;
    }

    @Override
    public View getAdView() {
        final Intent intent = getIntent();
        final String htmlData = getResponseString();
        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.log(CUSTOM, "RewardedMraidActivity received a null HTML body. Finishing the activity.");
            finish();
            return new View(this);
        } else if (getBroadcastIdentifier() == null) {
            MoPubLog.log(CUSTOM, "RewardedMraidActivity received a null broadcast id. Finishing the activity.");
            finish();
            return new View(this);
        }

        final int rewardedDurationInSeconds = intent.getIntExtra(REWARDED_AD_DURATION_KEY,
                RewardedMraidController.DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS);
        final boolean shouldRewardOnClick = intent.getBooleanExtra(SHOULD_REWARD_ON_CLICK_KEY,
                RewardedMraidController.DEFAULT_PLAYABLE_SHOULD_REWARD_ON_CLICK);

        boolean preloaded = false;
        final Long broadcastIdentifier = getBroadcastIdentifier();
        WebViewCacheService.Config config = null;
        if (broadcastIdentifier != null) {
            config = WebViewCacheService.popWebViewConfig(broadcastIdentifier);
        }
        if (config != null && config.getController() instanceof RewardedMraidController) {
            preloaded = true;
            mRewardedMraidController = (RewardedMraidController) config.getController();
        } else {
            mRewardedMraidController = new RewardedMraidController(
                    this, mAdReport, PlacementType.INTERSTITIAL, rewardedDurationInSeconds,
                    getBroadcastIdentifier());
        }

        mRewardedMraidController.setDebugListener(mDebugListener);
        mRewardedMraidController.setMraidListener(new MraidListener() {
            @Override
            public void onLoaded(View view) {
                // This is only done for the interstitial. Banners have a different mechanism
                // for tracking third party impressions.
                mRewardedMraidController.loadJavascript(WEB_VIEW_DID_APPEAR.getJavascript());
            }

            @Override
            public void onFailedToLoad() {
                MoPubLog.log(CUSTOM, "RewardedMraidActivity failed to load. Finishing the activity");
                broadcastAction(RewardedMraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_FAIL);
                finish();
            }

            @Override
            public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) {
                MoPubLog.log(CUSTOM, "Finishing the activity due to a problem: " + errorCode);
                finish();
            }

            public void onClose() {
                mRewardedMraidController.loadJavascript(WEB_VIEW_DID_CLOSE.getJavascript());
                finish();
            }

            @Override
            public void onExpand() {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onResize(final boolean toOriginalSize) {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onOpen() {
                if (shouldRewardOnClick) {
                    mRewardedMraidController.showPlayableCloseButton();
                }
                broadcastAction(RewardedMraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_CLICK);
            }
        });

        if (preloaded) {
            mExternalViewabilitySessionManager = config.getViewabilityManager();
        } else {
            mRewardedMraidController.fillContent(htmlData,
                    new MraidController.MraidWebViewCacheListener() {
                        @Override
                        public void onReady(@NonNull final MraidBridge.MraidWebView webView,
                                @Nullable final ExternalViewabilitySessionManager viewabilityManager) {
                            if (viewabilityManager != null) {
                                mExternalViewabilitySessionManager = viewabilityManager;
                            } else {
                                mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(
                                        RewardedMraidActivity.this);
                                mExternalViewabilitySessionManager.createDisplaySession(
                                        RewardedMraidActivity.this, webView, true);
                            }
                        }
                    });
        }

        mRewardedMraidController.onShow(this);
        return mRewardedMraidController.getAdContainer();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mRewardedMraidController != null) {
            mRewardedMraidController.create(RewardedMraidActivity.this, getCloseableLayout());
        }
    }

    @Override
    protected void onPause() {
        if (mRewardedMraidController != null) {
            mRewardedMraidController.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRewardedMraidController != null) {
            mRewardedMraidController.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mRewardedMraidController != null) {
            mRewardedMraidController.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mRewardedMraidController == null || mRewardedMraidController.backButtonEnabled()) {
            super.onBackPressed();
        }
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable MraidWebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mRewardedMraidController != null) {
            mRewardedMraidController.setDebugListener(debugListener);
        }
    }

    @Nullable
    @Deprecated
    @VisibleForTesting
    public RewardedMraidController getRewardedMraidController() {
        return mRewardedMraidController;
    }
}
