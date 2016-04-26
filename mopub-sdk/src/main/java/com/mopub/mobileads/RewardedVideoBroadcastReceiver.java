package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class RewardedVideoBroadcastReceiver extends BaseBroadcastReceiver {

    public static final String ACTION_REWARDED_VIDEO_COMPLETE = "com.mopub.action.rewardedvideo.complete";
    private static IntentFilter sIntentFilter;

    @Nullable
    private RewardedVastVideoInterstitial.CustomEventRewardedVideoInterstitialListener mRewardedVideoListener;

    public RewardedVideoBroadcastReceiver(
            @Nullable RewardedVastVideoInterstitial.CustomEventRewardedVideoInterstitialListener rewardedVideoListener,
            final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mRewardedVideoListener = rewardedVideoListener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(ACTION_REWARDED_VIDEO_COMPLETE);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        if (mRewardedVideoListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (ACTION_REWARDED_VIDEO_COMPLETE.equals(action)) {
            mRewardedVideoListener.onVideoComplete();
            unregister(this);
        }
    }
}
