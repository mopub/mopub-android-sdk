package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJVideoListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyLog;

import java.util.Hashtable;
import java.util.Map;

// Tested with Tapjoy SDK 11.5.1
public class TapjoyRewardedVideo extends CustomEventRewardedVideo {
    private static final String TAG = TapjoyRewardedVideo.class.getSimpleName();
    private static final String TJC_MOPUB_NETWORK_CONSTANT = "mopub";
    private static final String TJC_MOPUB_ADAPTER_VERSION_NUMBER = "4.0.0";
    private static final String TAPJOY_AD_NETWORK_CONSTANT = "tapjoy_id";

    private String sdkKey;
    private Hashtable<String, Object> connectFlags;
    private TJPlacement tjPlacement;
    private static TapjoyRewardedVideoListener sTapjoyListener = new TapjoyRewardedVideoListener();

    static {
        TapjoyLog.i(TAG, "Class initialized with network adapter version " + TJC_MOPUB_ADAPTER_VERSION_NUMBER);
    }

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

        if (!Tapjoy.isConnected()) {
            if (checkAndInitMediationSettings()) {
                MoPubLog.d("Request to connect to Tapjoy");

                Tapjoy.connect(launcherActivity, sdkKey, connectFlags, new TJConnectListener() {
                    @Override
                    public void onConnectSuccess() {
                        MoPubLog.d("Tapjoy connected successfully");
                    }

                    @Override
                    public void onConnectFailure() {
                        MoPubLog.e("Tapjoy connect failed");
                    }
                });

                return true;
            } else {
                MoPubLog.d("Cannot connect to Tapjoy -- missing 'sdkkey' declaration via TapjoyMediationSettings");
            }
        }

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
        tjPlacement.setMediationName(TJC_MOPUB_NETWORK_CONSTANT);
        tjPlacement.setAdapterVersion(TJC_MOPUB_ADAPTER_VERSION_NUMBER);
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

    private boolean checkAndInitMediationSettings() {
        MoPubLog.d("Initializing Tapjoy mediation settings");

        final TapjoyMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(TapjoyMediationSettings.class);

        if (globalMediationSettings != null) {
            if (!TextUtils.isEmpty(globalMediationSettings.getSdkKey())) {
                sdkKey = globalMediationSettings.getSdkKey();
            } else {
                return false;
            }

            if (globalMediationSettings.getConnectFlags() != null) {
                connectFlags = globalMediationSettings.getConnectFlags();
            }

            return true;
        } else {
            return false;
        }
    }

    private static class TapjoyRewardedVideoListener implements TJPlacementListener, CustomEventRewardedVideoListener, TJVideoListener {
        @Override
        public void onRequestSuccess(TJPlacement placement) {
            if (!placement.isContentAvailable()){
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
            Tapjoy.setVideoListener(this);
            MoPubLog.d("Tapjoy rewarded video content shown");
            MoPubRewardedVideoManager.onRewardedVideoStarted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT);
        }

        @Override
        public void onContentDismiss(TJPlacement placement) {
            Tapjoy.setVideoListener(null);
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

        @Override
        public void onVideoStart() {

        }

        @Override
        public void onVideoError(int statusCode) {
        }

        @Override
        public void onVideoComplete() {
            MoPubLog.d("Tapjoy rewarded video completed");
            MoPubRewardedVideoManager.onRewardedVideoCompleted(TapjoyRewardedVideo.class, TAPJOY_AD_NETWORK_CONSTANT, MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));
        }
    }

    public static final class TapjoyMediationSettings implements MediationSettings {
        @Nullable
        private final String mSdkKey;
        @Nullable
        Hashtable<String, Object> mConnectFlags;

        public TapjoyMediationSettings(String sdkKey) {
            this.mSdkKey = sdkKey;
        }

        public TapjoyMediationSettings(String sdkKey, Hashtable<String, Object> connectFlags) {
            this.mSdkKey = sdkKey;
            this.mConnectFlags = connectFlags;
        }

        @NonNull
        public String getSdkKey() {
            return mSdkKey;
        }

        @NonNull
        public Hashtable<String, Object> getConnectFlags() {
            return mConnectFlags;
        }
    }

}
