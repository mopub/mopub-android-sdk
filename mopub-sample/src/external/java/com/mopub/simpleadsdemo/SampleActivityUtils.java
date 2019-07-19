// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;

class SampleActivityUtils {
    static void addDefaultNetworkConfiguration(@NonNull final SdkConfiguration.Builder builder) {
        Preconditions.checkNotNull(builder);

        // We have no default networks to initialize
    }
}
