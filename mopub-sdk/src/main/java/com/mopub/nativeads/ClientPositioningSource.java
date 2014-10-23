package com.mopub.nativeads;

import android.os.Handler;

import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

/**
 * Returns a preset client positioning object.
 */
class ClientPositioningSource implements PositioningSource {
    private Handler mHandler = new Handler();
    private final MoPubClientPositioning mPositioning;

    ClientPositioningSource(MoPubClientPositioning positioning) {
        mPositioning = MoPubNativeAdPositioning.clone(positioning);
    }

    @Override
    public void loadPositions(final String adUnitId, final PositioningListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onLoad(mPositioning);
            }
        });
    }
}
