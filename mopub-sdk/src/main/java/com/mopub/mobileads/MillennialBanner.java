/*
 * Copyright (c) 2011, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;
import com.millennialmedia.android.RequestListener;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

/**
 * Compatible with version 5.3.0 of the Millennial Media SDK.
 */

class MillennialBanner extends CustomEventBanner {
    private MMAdView mMillennialAdView;
    private CustomEventBannerListener mBannerListener;
    public static final String APID_KEY = "adUnitID";
    public static final String AD_WIDTH_KEY = "adWidth";
    public static final String AD_HEIGHT_KEY = "adHeight";

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;


        final String apid;
        final int width;
        final int height;
        if (extrasAreValid(serverExtras)) {
            apid = serverExtras.get(APID_KEY);
            width = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            height = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        MMSDK.initialize(context);

        mMillennialAdView = new MMAdView(context);
        mMillennialAdView.setListener(new MillennialBannerRequestListener());

        mMillennialAdView.setApid(apid);
        mMillennialAdView.setWidth(width);
        mMillennialAdView.setHeight(height);

        final Location location = (Location) localExtras.get("location");
        if (location != null) {
            MMRequest.setUserLocation(location);
        }

        mMillennialAdView.setMMRequest(new MMRequest());
        mMillennialAdView.setId(MMSDK.getDefaultAdId());
        AdViewController.setShouldHonorServerDimensions(mMillennialAdView);
        mMillennialAdView.getAd();
    }

    @Override
    protected void onInvalidate() {
        mMillennialAdView.setListener(null);
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        try {
            Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } catch (NumberFormatException e) {
            return false;
        }

        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialBannerRequestListener implements RequestListener {
        @Override
        public void MMAdOverlayLaunched(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad Launched.");
            mBannerListener.onBannerExpanded();
        }

        @Override
        public void MMAdOverlayClosed(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad closed.");
            mBannerListener.onBannerCollapsed();
        }

        @Override
        public void MMAdRequestIsCaching(final MMAd mmAd) {}

        @Override
        public void requestCompleted(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad loaded successfully. Showing ad...");
            mBannerListener.onBannerLoaded(mMillennialAdView);
        }

        @Override
        public void requestFailed(final MMAd mmAd, final MMException e) {
            Log.d("MoPub", "Millennial banner ad failed to load.");
            mBannerListener.onBannerFailed(NETWORK_NO_FILL);
        }

        @Override
        public void onSingleTap(final MMAd mmAd) {
            mBannerListener.onBannerClicked();
        }
    }

    @Deprecated
    MMAdView getMMAdView() {
        return mMillennialAdView;
    }
}
