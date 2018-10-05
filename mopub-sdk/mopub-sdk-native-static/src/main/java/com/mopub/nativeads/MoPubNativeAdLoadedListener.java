// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

/**
 * A listener for determining when ads are loaded into an ad Placer
 */
public interface MoPubNativeAdLoadedListener {
    /**
     * Called when an ad is loaded at the specified position.
     *
     * @param position The ad position of the newly loaded ad.
     */
    void onAdLoaded(int position);

    /**
     * Called when an ad is removed at the specified position.
     *
     * @param position The removed ad position.
     */
    void onAdRemoved(int position);
}
