package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TapjoyLog;

import java.util.Map;

// Tested with Tapjoy SDK 11.5.1
public class TapjoyInterstitial extends CustomEventInterstitial implements TJPlacementListener {
    private static final String TAG = TapjoyInterstitial.class.getSimpleName();
    private static final String TJC_MOPUB_NETWORK_CONSTANT = "mopub";
    private static final String TJC_MOPUB_ADAPTER_VERSION_NUMBER = "4.0.0";

    private TJPlacement tjPlacement;
    private CustomEventInterstitialListener mInterstitialListener;
    private Handler mHandler;

    static {
        TapjoyLog.i(TAG, "Class initialized with network adapter version " + TJC_MOPUB_ADAPTER_VERSION_NUMBER);
    }

    @Override
    protected void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        MoPubLog.d("Requesting Tapjoy interstitial");

        mInterstitialListener = customEventInterstitialListener;
        mHandler = new Handler(Looper.getMainLooper());

        String name = serverExtras.get("name");
        if (TextUtils.isEmpty(name)) {
            MoPubLog.d("Tapjoy interstitial loaded with empty 'name' field. Request will fail.");
        }
        tjPlacement = new TJPlacement(context, name, this);
        tjPlacement.setMediationName(TJC_MOPUB_NETWORK_CONSTANT);
        tjPlacement.setAdapterVersion(TJC_MOPUB_ADAPTER_VERSION_NUMBER);
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
    public void onRequestSuccess(final TJPlacement placement) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (placement.isContentAvailable()) {
                    MoPubLog.d("Tapjoy interstitial request successful");
                    mInterstitialListener.onInterstitialLoaded();
                } else {
                    MoPubLog.d("No Tapjoy interstitials available");
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }
        });
    }

    @Override
    public void onRequestFailure(TJPlacement placement, TJError error) {
        MoPubLog.d("Tapjoy interstitial request failed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        });
    }

    @Override
    public void onContentShow(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial shown");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onContentDismiss(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial dismissed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialDismissed();
            }
        });
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
