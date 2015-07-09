package com.mopub.nativeads;

import android.support.annotation.NonNull;

/**
 * An object that represents placed ads in a {@link com.mopub.nativeads.MoPubStreamAdPlacer}
 */
public class NativeAdData {
    @NonNull private final String adUnitId;
    @NonNull private final MoPubAdRenderer adRenderer;
    @NonNull private final NativeResponse adResponse;

    NativeAdData(@NonNull final String adUnitId,
            @NonNull final MoPubAdRenderer adRenderer,
            @NonNull final NativeResponse adResponse) {
        this.adUnitId = adUnitId;
        this.adRenderer = adRenderer;
        this.adResponse = adResponse;
    }

    @NonNull
    String getAdUnitId() {
        return adUnitId;
    }

    @NonNull
    public  MoPubAdRenderer getAdRenderer() {
        return adRenderer;
    }

    @NonNull
    public NativeResponse getAd() {
        return adResponse;
    }
}