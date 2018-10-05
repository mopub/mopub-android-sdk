// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.content.pm.ActivityInfo;

enum MraidOrientation {
    PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    NONE(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

    private final int mActivityInfoOrientation;

    MraidOrientation(final int activityInfoOrientation) {
        mActivityInfoOrientation = activityInfoOrientation;
    }

    int getActivityInfoOrientation() {
        return mActivityInfoOrientation;
    }
}
