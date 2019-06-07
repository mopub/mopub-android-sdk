package com.mopub.mobileads;

  public interface InterstitialCustomEventAdListener {
    void onCustomEventInterstitialAttempted(MoPubInterstitial interstitial, String customEventClassName);
    void onCustomEventInterstitialAttemptSucceeded(MoPubInterstitial interstitial, String creativeId);
    void onCustomEventInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode);
}