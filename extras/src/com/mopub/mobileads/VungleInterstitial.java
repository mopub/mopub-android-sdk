package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.env.WrapperFramework;
import com.vungle.publisher.inject.Injector;

import java.util.Map;

/**
 * A custom event for showing Vungle Interstitial.
 *
 * Certified with Vungle SDK 5.1.0
 */
public class VungleInterstitial extends CustomEventInterstitial {

    private static final String MAIN_TAG = "MoPub";
    private static final String SUB_TAG = "Vungle Interstitial: ";

    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String PLACEMENT_ID_KEY = "pid";
    public static final String PLACEMENT_IDS_KEY = "pids";

    // Version of the adapter, intended for Vungle internal use.
    private static final String VERSION = "5.1.0";


    private final VunglePub mVunglePub;
    private final Handler mHandler;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private VungleAdEventListener mVungleAdEventListener;

    private String mAppId;
    private String mPlacementId;
    private String[] mPlacementIds;

    private boolean mIsPlaying;


    public VungleInterstitial() {
        mHandler = new Handler(Looper.getMainLooper());
        mVunglePub = VunglePub.getInstance();
        Injector injector = Injector.getInstance();
        injector.setWrapperFramework(WrapperFramework.mopub);
        injector.setWrapperFrameworkVersion(VERSION.replace('.', '_'));
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

        mVungleAdEventListener = getVungleAdEventListener();

        if (!mVunglePub.isInitialized()) {
            mVunglePub.init(context, mAppId, mPlacementIds, new VungleInitListener() {
                @Override
                public void onSuccess() {
                    Log.d(MAIN_TAG, SUB_TAG + "SDK is initialized successfully.");

                    mVunglePub.addEventListeners(mVungleAdEventListener);
                    mVunglePub.loadAd(mPlacementId);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.w(MAIN_TAG, SUB_TAG + "Initialization is failed.");

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                        }
                    });
                }
            });
        }
        else {
            mVunglePub.addEventListeners(mVungleAdEventListener);
            mVunglePub.loadAd(mPlacementId);
        }
    }

    @Override
    protected void showInterstitial() {
        if (mVunglePub.isAdPlayable(mPlacementId)) {
            mVunglePub.playAd(mPlacementId, null);
            mIsPlaying = true;
        } else {
            Log.d(MAIN_TAG, SUB_TAG + "SDK tried to show a Vungle interstitial ad before it finished loading. Please try again.");
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void onInvalidate() {
        mVunglePub.removeEventListeners(mVungleAdEventListener);
        mVungleAdEventListener = null;

        Log.d(MAIN_TAG, SUB_TAG + "onInvalidate is called for Placement ID:" + mPlacementId);
    }

    // private functions
    private boolean validateIdsInServerExtras (Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId.isEmpty()) {
                Log.w(MAIN_TAG, SUB_TAG + "App ID is empty.");
                isAllDataValid = false;
            }
        }
        else {
            Log.w(MAIN_TAG, SUB_TAG + "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId.isEmpty()) {
                Log.w(MAIN_TAG, SUB_TAG + "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        }
        else {
            Log.w(MAIN_TAG, SUB_TAG + "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            mPlacementIds = serverExtras.get(PLACEMENT_IDS_KEY).replace(" ", "").split(",", 0);
            if (mPlacementIds.length == 0) {
                Log.w(MAIN_TAG, SUB_TAG + "Placement ID sare empty.");
                isAllDataValid = false;
            }
        }
        else {
            Log.w(MAIN_TAG, SUB_TAG + "Placement IDs for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (isAllDataValid) {
            boolean foundInList = false;
            for (String pid:  mPlacementIds) {
                if(pid.equals(mPlacementId)) {
                    foundInList = true;
                }
            }
            if(!foundInList) {
                Log.w(MAIN_TAG, SUB_TAG + "Placement IDs for this Ad Unit is not in the array of Placement IDs");
                isAllDataValid = false;
            }
        }

        return isAllDataValid;
    }

    /*
     * VungleAdEventListener
     */
    private final VungleAdEventListener getVungleAdEventListener() {
        if(mVungleAdEventListener != null) {
            return  mVungleAdEventListener;
        }
        else {
            return new VungleAdEventListener() {
                @Override
                public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
                    if (mPlacementId.equals(placementReferenceId)) {
                        Log.d(MAIN_TAG, SUB_TAG + "onAdEnd - Placement ID: " + placementReferenceId + ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: " + wasCallToActionClicked);
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
                    }
                }

                @Override
                public void onAdStart(@NonNull String placementReferenceId) {
                    if (mPlacementId.equals(placementReferenceId)) {
                        Log.d(MAIN_TAG, SUB_TAG + "onAdStart - Placement ID: " + placementReferenceId);
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
                        Log.d(MAIN_TAG, SUB_TAG + "onUnableToPlayAd - Placement ID: " + placementReferenceId + ", reason: " + reason);
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
                                Log.d(MAIN_TAG, SUB_TAG + "interstitial ad successfully loaded - Placement ID: " + placementReferenceId);

                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCustomEventInterstitialListener.onInterstitialLoaded();
                                    }
                                });
                            } else {
                                Log.d(MAIN_TAG, SUB_TAG + "interstitial ad is not loaded - Placement ID: " + placementReferenceId);

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
            };
        }
    };
}
