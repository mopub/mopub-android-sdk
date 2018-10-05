// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

/**
 * Returns a preset client positioning object.
 */
class ClientPositioningSource implements PositioningSource {
    @NonNull private final Handler mHandler = new Handler();
    @NonNull private final MoPubClientPositioning mPositioning;

    ClientPositioningSource(@NonNull MoPubClientPositioning positioning) {
        mPositioning = MoPubNativeAdPositioning.clone(positioning);
    }

    @Override
    public void loadPositions(@NonNull final String adUnitId,
            @NonNull final PositioningListener listener) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onLoad(mPositioning);
            }
        });
    }
}
