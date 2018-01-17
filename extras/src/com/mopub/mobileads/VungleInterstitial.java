package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;

import java.util.Map;

/**
 * A custom event for showing Vungle Interstitial.
 * <p>
 * Certified with Vungle SDK 5.3.0
 */
public class VungleInterstitial extends CustomEventInterstitial {

    private static final String INTERSTITIAL_TAG = "Vungle Interstitial: ";

    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String PLACEMENT_ID_KEY = "pid";
    public static final String PLACEMENT_IDS_KEY = "pids";

    private static VungleRouter sVungleRouter;
    private final Handler mHandler;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private VungleInterstitialRouterListener mVungleRouterListener;
    private String mAppId;
    private String mPlacementId;
    private String[] mPlacementIds;
    private boolean mIsPlaying;


    public VungleInterstitial() {
        mHandler = new Handler(Looper.getMainLooper());
        sVungleRouter = VungleRouter.getInstance();
    }

    @Override
    protected void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mIsPlaying = false;

        if (context == null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                }
            });

            return;
        }

        if (!validateIdsInServerExtras(serverExtras)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            });

            return;
        }

        if (mVungleRouterListener == null) {
            mVungleRouterListener = new VungleInterstitialRouterListener();
        }

        if (!sVungleRouter.isVungleInitialized()) {
            sVungleRouter.initVungle(context, mAppId, mPlacementIds);
        }

        sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRouterListener);
    }

    @Override
    protected void showInterstitial() {
        if (sVungleRouter.isAdPlayableForPlacement(mPlacementId)) {
            sVungleRouter.playAdForPlacement(mPlacementId, null);
            mIsPlaying = true;
        } else {
            MoPubLog.d(INTERSTITIAL_TAG + "SDK tried to show a Vungle interstitial ad before it finished loading. Please try again.");
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.d(INTERSTITIAL_TAG + "onInvalidate is called for Placement ID:" + mPlacementId);
        sVungleRouter.removeRouterListener(mPlacementId);
        mVungleRouterListener = null;
    }

    // private functions
    private boolean validateIdsInServerExtras(Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId.isEmpty()) {
                MoPubLog.w(INTERSTITIAL_TAG + "App ID is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(INTERSTITIAL_TAG + "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId.isEmpty()) {
                MoPubLog.w(INTERSTITIAL_TAG + "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(INTERSTITIAL_TAG + "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            mPlacementIds = serverExtras.get(PLACEMENT_IDS_KEY).replace(" ", "").split(",", 0);
            if (mPlacementIds.length == 0) {
                MoPubLog.w(INTERSTITIAL_TAG + "Placement IDs are empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(INTERSTITIAL_TAG + "Placement IDs for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (isAllDataValid) {
            boolean foundInList = false;
            for (String pid : mPlacementIds) {
                if (pid.equals(mPlacementId)) {
                    foundInList = true;
                }
            }
            if (!foundInList) {
                MoPubLog.w(INTERSTITIAL_TAG + "Placement IDs for this Ad Unit is not in the array of Placement IDs");
                isAllDataValid = false;
            }
        }

        return isAllDataValid;
    }


    /*
     * VungleRouterListener
     */
    private class VungleInterstitialRouterListener implements VungleRouterListener {
        @Override
        public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(INTERSTITIAL_TAG + "onAdEnd - Placement ID: " + placementReferenceId + ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: " + wasCallToActionClicked);
                mIsPlaying = false;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (wasCallToActionClicked) {
                            mCustomEventInterstitialListener.onInterstitialClicked();
                        }
                        mCustomEventInterstitialListener.onInterstitialDismissed();
                    }
                });

                sVungleRouter.removeRouterListener(mPlacementId);
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(INTERSTITIAL_TAG + "onAdStart - Placement ID: " + placementReferenceId);
                mIsPlaying = true;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCustomEventInterstitialListener.onInterstitialShown();
                    }
                });
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(INTERSTITIAL_TAG + "onUnableToPlayAd - Placement ID: " + placementReferenceId + ", reason: " + reason);
                mIsPlaying = false;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                    }
                });
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull String placementReferenceId, boolean isAdAvailable) {
            if (mPlacementId.equals(placementReferenceId)) {
                if (!mIsPlaying) {
                    if (isAdAvailable) {
                        MoPubLog.d(INTERSTITIAL_TAG + "interstitial ad successfully loaded - Placement ID: " + placementReferenceId);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCustomEventInterstitialListener.onInterstitialLoaded();
                            }
                        });
                    } else {
                        MoPubLog.d(INTERSTITIAL_TAG + "interstitial ad is not loaded - Placement ID: " + placementReferenceId);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                            }
                        });
                    }
                }
            }
        }
    }
}
