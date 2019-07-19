// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * ImpressionListener is an interface to notify the application about ad impressions for all
 * ad formats.
 */
public interface ImpressionListener {
    /**
     * SDK will call method onImpression once the ad becomes visible for the first time.
     *
     * @param adUnitId  - ad unit ID of the ad.
     * @param impressionData - extended information about the ad including revenue per impression.
     *                       This value can be null if impression level revenue data is not enabled
     *                       for this MoPub account.
     */
    @AnyThread
    void onImpression(@NonNull String adUnitId, @Nullable ImpressionData impressionData);
}
