package com.mopub.mraid;

import android.content.Context;
import android.support.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.RewardedMraidActivity;

import java.util.Map;

import static com.mopub.common.DataKeys.REWARDED_AD_DURATION_KEY;
import static com.mopub.common.DataKeys.SHOULD_REWARD_ON_CLICK_KEY;

/**
 * Handles the showing of rewarded MRAID interstitials. 'Rewarded duration' and 'should reward on
 * click' are optional and will default to the values set in {@link RewardedMraidController}.
 */
public class RewardedMraidInterstitial extends MraidInterstitial {

    public interface RewardedMraidInterstitialListener extends CustomEventInterstitial.CustomEventInterstitialListener {
        void onMraidComplete();
    }

    @Nullable private RewardedPlayableBroadcastReceiver mRewardedPlayableBroadcastReceiver;
    private int mRewardedDuration;
    private boolean mShouldRewardOnClick;

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        super.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        if (customEventInterstitialListener instanceof RewardedMraidInterstitialListener) {
            mRewardedPlayableBroadcastReceiver = new RewardedPlayableBroadcastReceiver(
                    (RewardedMraidInterstitialListener) customEventInterstitialListener,
                    mBroadcastIdentifier);
            mRewardedPlayableBroadcastReceiver.register(mRewardedPlayableBroadcastReceiver, context);
        }

        final Object rewardedDurationObject = localExtras.get(REWARDED_AD_DURATION_KEY);
        mRewardedDuration = rewardedDurationObject instanceof Integer ?
                (int) rewardedDurationObject :
                RewardedMraidController.DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS;
        final Object shouldRewardOnClickObject = localExtras.get(SHOULD_REWARD_ON_CLICK_KEY);
        mShouldRewardOnClick = shouldRewardOnClickObject instanceof Boolean ?
                (boolean) shouldRewardOnClickObject :
                RewardedMraidController.DEFAULT_PLAYABLE_SHOULD_REWARD_ON_CLICK;
    }

    @Override
    public void showInterstitial() {
        RewardedMraidActivity.start(mContext, mAdReport, mHtmlData, mBroadcastIdentifier,
                mRewardedDuration, mShouldRewardOnClick);
    }


    @Override
    public void onInvalidate() {
        super.onInvalidate();
        if (mRewardedPlayableBroadcastReceiver != null) {
            mRewardedPlayableBroadcastReceiver.unregister(mRewardedPlayableBroadcastReceiver);
        }
    }

    @Deprecated
    @VisibleForTesting
    int getRewardedDuration() {
        return mRewardedDuration;
    }

    @Deprecated
    @VisibleForTesting
    boolean isShouldRewardOnClick() {
        return mShouldRewardOnClick;
    }
}
