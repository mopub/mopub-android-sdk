/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
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
import android.os.Handler;
import android.util.Log;
import com.vungle.sdk.VunglePub;

import java.util.*;
import java.util.concurrent.*;

import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

/*
 * Tested with Vungle SDK 1.3.3.
 */
public class VungleInterstitial extends CustomEventInterstitial implements VunglePub.EventListener {

    public static final String DEFAULT_VUNGLE_APP_ID = "YOUR_DEFAULT_VUNGLE_APP_ID";

    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    private static final String APP_ID_KEY = "appId";

    private final Handler mHandler;
    private final ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private boolean mIsLoading;

    public VungleInterstitial() {
        mHandler = new Handler();
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(10);
    }

    @Override
    protected void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (context == null) {
            mCustomEventInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            return;
        }

        /*
         * You may pass the Vungle App Id in the serverExtras Map by specifying Custom Event Data
         * in MoPub's web interface.
         */
        final String appId;
        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(APP_ID_KEY);
        } else {
            appId = DEFAULT_VUNGLE_APP_ID;
        }

        VunglePub.init(context, appId);
        VunglePub.setEventListener(this);

        scheduleOnInterstitialLoaded();
    }

    @Override
    protected void showInterstitial() {
        if (VunglePub.isVideoAvailable(true)) {
            VunglePub.displayAdvert();
        } else {
            Log.d("MoPub", "Tried to show a Vungle interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected void onInvalidate() {
        VunglePub.setEventListener(null);
        mScheduledThreadPoolExecutor.shutdownNow();
        mIsLoading = false;
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APP_ID_KEY);
    }

    private void scheduleOnInterstitialLoaded() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            if (VunglePub.isVideoAvailable()) {
                Log.d("MoPub", "Vungle interstitial ad successfully loaded.");
                mScheduledThreadPoolExecutor.shutdownNow();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCustomEventInterstitialListener.onInterstitialLoaded();
                    }
                });
                mIsLoading = false;
            }
            }
        };

        if (!mIsLoading) {
            mScheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
            mIsLoading = true;
        }
    }

    /*
     * VunglePub.EventListener implementation
     */

    @Override
    public void onVungleView(double watchedSeconds, double totalAdSeconds) {
        final double watchedPercent = watchedSeconds / totalAdSeconds * 100;
        Log.d("MoPub", String.format("%.1f%% of Vungle video watched.", watchedPercent));
    }

    @Override
    public void onVungleAdStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MoPub", "Showing Vungle interstitial ad.");
                mCustomEventInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onVungleAdEnd() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MoPub", "Vungle interstitial ad dismissed.");
                mCustomEventInterstitialListener.onInterstitialDismissed();
            }
        });
    }

    @Deprecated // for testing
    ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return mScheduledThreadPoolExecutor;
    }
}
