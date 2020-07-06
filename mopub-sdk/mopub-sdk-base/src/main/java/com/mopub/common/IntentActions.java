// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

/**
 * IntentActions are used by a {@link com.mopub.mobileads.BaseBroadcastReceiver}
 * to relay information about the current state of a base ad activity.
 */
public class IntentActions {
    public static final String ACTION_FULLSCREEN_FAIL = "com.mopub.action.fullscreen.fail";
    public static final String ACTION_FULLSCREEN_SHOW = "com.mopub.action.fullscreen.show";
    public static final String ACTION_FULLSCREEN_DISMISS = "com.mopub.action.fullscreen.dismiss";
    public static final String ACTION_FULLSCREEN_CLICK = "com.mopub.action.fullscreen.click";
    public static final String ACTION_REWARDED_AD_COMPLETE = "com.mopub.action.rewardedad.complete";

    private IntentActions() {}
}
