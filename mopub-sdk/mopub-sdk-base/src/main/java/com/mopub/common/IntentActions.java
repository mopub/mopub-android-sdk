package com.mopub.common;

/**
 * IntentActions are used by a {@link com.mopub.mobileads.BaseBroadcastReceiver}
 * to relay information about the current state of a custom event activity.
 */
public class IntentActions {
    public static final String ACTION_INTERSTITIAL_FAIL = "com.mopub.action.interstitial.fail";
    public static final String ACTION_INTERSTITIAL_SHOW = "com.mopub.action.interstitial.show";
    public static final String ACTION_INTERSTITIAL_DISMISS = "com.mopub.action.interstitial.dismiss";
    public static final String ACTION_INTERSTITIAL_CLICK = "com.mopub.action.interstitial.click";

    public static final String ACTION_REWARDED_VIDEO_COMPLETE = "com.mopub.action.rewardedvideo.complete";
    public static final String ACTION_REWARDED_PLAYABLE_COMPLETE = "com.mopub.action.rewardedplayable.complete";
    private IntentActions() {}
}
