package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;

import java.util.Map;

// Tested with Tapjoy SDK 11.1.0
public class TapjoyInterstitial extends CustomEventInterstitial implements TJPlacementListener {
    private TJPlacement tjPlacement;
    private CustomEventInterstitialListener mInterstitialListener;

    @Override
    protected void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        MoPubLog.d("Requesting Tapjoy interstitial");

        mInterstitialListener = customEventInterstitialListener;

        String name = serverExtras.get("name");
        if (TextUtils.isEmpty(name)) {
            MoPubLog.d("Tapjoy interstitial loaded with empty 'name' field. Request will fail.");
        }
        tjPlacement = new TJPlacement(context, name, this);
        tjPlacement.requestContent();
    }

    @Override
    protected void onInvalidate() {
        // No custom cleanup to do here.
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.d("Tapjoy interstitial will be shown");
        tjPlacement.showContent();
    }

    // Tapjoy

    @Override
    public void onRequestSuccess(TJPlacement placement) {
        if (placement.isContentAvailable()) {
            MoPubLog.d("Tapjoy interstitial request successful");
            mInterstitialListener.onInterstitialLoaded();
        } else {
            MoPubLog.d("No Tapjoy interstitials available");
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    public void onRequestFailure(TJPlacement placement, TJError error) {
        MoPubLog.d("Tapjoy interstitial request failed");
        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void onContentShow(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial shown");
        mInterstitialListener.onInterstitialShown();
    }

    @Override
    public void onContentDismiss(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial dismissed");
        mInterstitialListener.onInterstitialDismissed();
    }

    @Override
    public void onContentReady(TJPlacement placement) {
    }

    @Override
    public void onPurchaseRequest(TJPlacement placement, TJActionRequest request,
            String productId) {
    }

    @Override
    public void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId,
            int quantity) {
    }

}
