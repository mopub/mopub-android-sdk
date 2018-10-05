// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.support.annotation.NonNull;

/**
 * An object that represents placed ads in a {@link com.mopub.nativeads.MoPubStreamAdPlacer}
 */
class NativeAdData {
    @NonNull private final String adUnitId;
    @NonNull private final MoPubAdRenderer adRenderer;
    @NonNull private final NativeAd adResponse;

    NativeAdData(@NonNull final String adUnitId,
            @NonNull final MoPubAdRenderer adRenderer,
            @NonNull final NativeAd adResponse) {
        this.adUnitId = adUnitId;
        this.adRenderer = adRenderer;
        this.adResponse = adResponse;
    }

    @NonNull
    String getAdUnitId() {
        return adUnitId;
    }

    @NonNull
    MoPubAdRenderer getAdRenderer() {
        return adRenderer;
    }

    @NonNull
    NativeAd getAd() {
        return adResponse;
    }
}