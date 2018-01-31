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
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.mraid.MraidController.MraidListener;
import com.mopub.mraid.MraidWebViewDebugListener;
import com.mopub.mraid.PlacementType;
import com.mopub.mraid.RewardedMraidController;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REWARDED_AD_DURATION_KEY;
import static com.mopub.common.DataKeys.SHOULD_REWARD_ON_CLICK_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;

public class RewardedMraidActivity extends MraidActivity {
    @Nullable private RewardedMraidController mRewardedMraidController;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    public static void start(@NonNull Context context, @Nullable AdReport adreport,
            @Nullable String htmlData, long broadcastIdentifier, int rewardedDuration,
            boolean shouldRewardOnClick) {
        final Intent intent = createIntent(context, adreport, htmlData, broadcastIdentifier,
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
            @Nullable String htmlData, long broadcastIdentifier, int rewardedDuration,
            boolean shouldRewardOnClick) {
        Intent intent = new Intent(context, RewardedMraidActivity.class);
        intent.putExtra(HTML_RESPONSE_BODY_KEY, htmlData);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        intent.putExtra(AD_REPORT_KEY, adReport);
        intent.putExtra(REWARDED_AD_DURATION_KEY, rewardedDuration);
        intent.putExtra(SHOULD_REWARD_ON_CLICK_KEY, shouldRewardOnClick);
        return intent;
    }

    @Override
    public View getAdView() {
        final Intent intent = getIntent();
        final String htmlData = intent.getStringExtra(HTML_RESPONSE_BODY_KEY);
        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.w("RewardedMraidActivity received a null HTML body. Finishing the activity.");
            finish();
            return new View(this);
        } else if (getBroadcastIdentifier() == null) {
            MoPubLog.w("RewardedMraidActivity received a null broadcast id. Finishing the activity.");
            finish();
            return new View(this);
        }

        final int rewardedDurationInSeconds = intent.getIntExtra(REWARDED_AD_DURATION_KEY,
                RewardedMraidController.DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS);
        final boolean shouldRewardOnClick = intent.getBooleanExtra(SHOULD_REWARD_ON_CLICK_KEY,
                RewardedMraidController.DEFAULT_PLAYABLE_SHOULD_REWARD_ON_CLICK);

        mRewardedMraidController = new RewardedMraidController(
                this, mAdReport, PlacementType.INTERSTITIAL, rewardedDurationInSeconds,
                getBroadcastIdentifier());

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
                MoPubLog.d("RewardedMraidActivity failed to load. Finishing the activity");
                broadcastAction(RewardedMraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_FAIL);
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
            public void onOpen() {
                if (shouldRewardOnClick) {
                    mRewardedMraidController.showPlayableCloseButton();
                }
                broadcastAction(RewardedMraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_CLICK);
            }
        });

        mRewardedMraidController.fillContent(getBroadcastIdentifier(), htmlData, null);
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
