package com.mopub.nativeads;

/**
 * An object that represents placed ads in a {@link com.mopub.nativeads.MoPubStreamAdPlacer}
 */
class NativeAdData {
    private final String adUnitId;
    private final MoPubAdRenderer adRenderer;
    private final NativeResponse adResponse;

    NativeAdData(final String adUnitId,
            final MoPubAdRenderer adRenderer,
            final NativeResponse adResponse) {
        this.adUnitId = adUnitId;
        this.adRenderer = adRenderer;
        this.adResponse = adResponse;
    }

    String getAdUnitId() {
        return adUnitId;
    }

    MoPubAdRenderer getAdRenderer() {
        return adRenderer;
    }

    NativeResponse getAd() {
        return adResponse;
    }
}