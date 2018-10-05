// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;

public class DefaultInterstitialAdListener implements InterstitialAdListener {
    @Override public void onInterstitialLoaded(MoPubInterstitial interstitial) { }
    @Override public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) { }
    @Override public void onInterstitialShown(MoPubInterstitial interstitial) { }
    @Override public void onInterstitialClicked(MoPubInterstitial interstitial) { }
    @Override public void onInterstitialDismissed(MoPubInterstitial interstitial) { }
}
