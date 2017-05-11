/*

    SpotX - MoPub Advertising Plugin Integration

    To use the the SpotX Advertising Pluging, copy the SpotX* source files into your application’s
    com.mopub.mobileads package, and add a build dependency to your application's build.gradle
    file (e.g. compile 'com.spotxchange:spotx-sdk-android:2.2.+').

 */

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mopub.mobileads.CustomEventInterstitial;

import java.util.HashMap;
import java.util.Map;

import com.mopub.mobileads.MoPubErrorCode;
import com.spotxchange.sdk.android.SpotxAdListener;
import com.spotxchange.sdk.android.SpotxAdSettings;
import com.spotxchange.sdk.android.SpotxAdView;


public class SpotXInterstitial extends CustomEventInterstitial {

    private SpotxAdView _adView;

    private CustomEventInterstitialListener _customEventInterstitialListener;

    private final SpotxAdListener _spotxAdListener = new SpotxAdListener() {
        @Override
        public void adLoaded() {
            if(_customEventInterstitialListener != null) {
                _customEventInterstitialListener.onInterstitialLoaded();
            }
        }

        @Override
        public void adStarted() {
            if(_customEventInterstitialListener != null) {
                _customEventInterstitialListener.onInterstitialShown();
            }
        }

        @Override
        public void adCompleted() {
            _adView.setVisibility(View.INVISIBLE);
            if(_customEventInterstitialListener != null) {
                _customEventInterstitialListener.onInterstitialDismissed();
            }
        }

        @Override
        public void adError() {
            if(_customEventInterstitialListener != null) {
                //TODO: Infer and specify which kind of error this is based off event log tracking.
                _customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void adExpired() {
            if(_customEventInterstitialListener != null) {
                //TODO: Infer and specify which kind of error this is based off event log tracking.
                _customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void adClicked() {
            if(_customEventInterstitialListener != null) {
                _customEventInterstitialListener.onInterstitialClicked();
            }
        }
    };


    /**
     *
     * @param context
     * @param customEventInterstitialListener
     * @param localExtras
     * @param serverExtras
     * @throws java.lang.IllegalArgumentException if localExtras and serverExtras do not have the required parameters
     */
    @Override
    protected void loadInterstitial(
        Context context,
        CustomEventInterstitialListener customEventInterstitialListener,
        Map<String, Object> localExtras,
        Map<String, String> serverExtras
        )
    {
        _customEventInterstitialListener = customEventInterstitialListener;
        SpotxAdSettings adSettings = SpotXCommon.constructAdSettings(localExtras, serverExtras, true);
        _adView = new SpotxAdView(context, adSettings);
        _adView.setVisibility(View.INVISIBLE);
        _adView.setAdListener(_spotxAdListener);
        ((Activity)context).addContentView(_adView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        _adView.init();
    }

    @Override
    protected void showInterstitial() {
        _adView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onInvalidate() {
        _adView = null;
    }
}
