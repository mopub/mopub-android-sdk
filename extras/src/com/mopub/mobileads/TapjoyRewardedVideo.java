package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;

import java.util.Map;

// Tested with Tapjoy SDK 11.1.0
public class TapjoyRewardedVideo extends CustomEventRewardedVideo {
    private static final String TAPJOY_AD_NETWORK_CONSTANT = "tapjoy_id";
    private TJPlacement tjPlacement;
    private static TapjoyRewardedVideoListener sTapjoyListener = new TapjoyRewardedVideoListener();

    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sTapjoyListener;
    }

    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected String getAdNetworkId() {
        return TAPJOY_AD_NETWORK_CONSTANT;
    }

    @Override
    protected void onInvalidate() {
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {
        // Always return false, no special initialization steps to be done from here
        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {
        MoPubLog.d("Requesting Tapjoy rewarded video");

        String name = serverExtras.get("name");
        if (TextUtils.isEmpty(name)) {
            MoPubLog.d("Tapjoy interstitial loaded with empty 'name' field. Request will fail.");
        }
        tjPlacement = new TJPlacement(activity, name, sTapjoyListener);
        tjPlacement.requestContent();
    }

    @Override
    protected boolean hasVideoAvailable() {
        return tjPlacement.isContentAvailable();
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            MoPubLog.d("Tapjoy rewarded video will be shown.");
            tjPlacement.showContent();
        } else {
            MoPubLog.d("Failed to show Tapjoy rewarded video.");
        }

    }

    private static class TapjoyRewardedVideoListener implements TJPlacementListener, CustomEventRewardedVideoListener {
        @Override
        public void onRequestSuccess(TJPlacement placement) {
            if (!placement.isContentAvailable()) {
                MoPubLog.d("No Tapjoy rewarded videos available");
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onContentReady(TJPlacement placement) {
            MoPubLog.d("Tapjoy rewarded video content is ready");
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onRequestFailure(TJPlacement placement, TJError error) {
            MoPubLog.d("Tapjoy rewarded video request failed");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void onContentShow(TJPlacement placement) {
            MoPubLog.d("Tapjoy rewarded video content shown");
            MoPubRewardedVideoManager.onRewardedVideoStarted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onContentDismiss(TJPlacement placement) {
            MoPubLog.d("Tapjoy rewarded video content dismissed");
            MoPubRewardedVideoManager.onRewardedVideoClosed(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
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

    public static final class TapjoyMediationSettings implements MediationSettings {
        public TapjoyMediationSettings() {

        }
    }

}
