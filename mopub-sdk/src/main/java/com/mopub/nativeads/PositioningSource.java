package com.mopub.nativeads;

import android.os.Handler;

import com.mopub.common.VisibleForTesting;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

/**
 * Allows asynchronously requesting positioning information.
 */
interface PositioningSource {

    interface PositioningListener {
        void onLoad(MoPubClientPositioning positioning);

        void onFailed();
    }

    void loadPositions(String adUnitId, PositioningListener listener);

}
