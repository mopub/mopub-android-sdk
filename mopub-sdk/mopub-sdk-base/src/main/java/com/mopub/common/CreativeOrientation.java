// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Represents the orientation returned for MoPub ads from the MoPub ad server.
 */
public enum CreativeOrientation {
    PORTRAIT, LANDSCAPE, UNDEFINED;

    @NonNull
    public static CreativeOrientation fromHeader(@Nullable String orientation) {
        if ("l".equalsIgnoreCase(orientation)) {
            return LANDSCAPE;
        }

        if ("p".equalsIgnoreCase(orientation)) {
            return PORTRAIT;
        }

        return UNDEFINED;
    }
}
