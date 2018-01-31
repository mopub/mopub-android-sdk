package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.logging.MoPubLog;

import com.vungle.publisher.AdConfig;
import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.env.WrapperFramework;
import com.vungle.publisher.inject.Injector;

import java.util.HashMap;
import java.util.Map;


/**
 * Certified with Vungle SDK 5.3.0
 */
public class VungleRouter {

    private static final String ROUTER_TAG = "Vungle Router: ";

    // Version of the adapter, intended for Vungle internal use.
    private static final String VERSION = "5.3.0";

    private static VungleRouter instance = new VungleRouter();
    private enum SDKInitState {
        NOTINITIALIZED,
        INITIALIZING,
        INITIALIZED;
    }

    private static SDKInitState sInitState = SDKInitState.NOTINITIALIZED;
    private static VunglePub sVunglePub;
    private static Map<String, VungleRouterListener> sVungleRouterListeners = new HashMap<>();
    private static Map<String, VungleRouterListener> sWaitingList = new HashMap<>();

    private static final LifecycleListener sLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull final Activity activity) {
            super.onPause(activity);
            sVunglePub.onPause();
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
            sVunglePub.onResume();
        }
    };


    private VungleRouter() {
        Injector injector = Injector.getInstance();
        injector.setWrapperFramework(WrapperFramework.mopub);
        injector.setWrapperFrameworkVersion(VERSION.replace('.', '_'));

        sVunglePub = VunglePub.getInstance();
    }

    public static VungleRouter getInstance() {
        return instance;
    }

    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }


    public void initVungle(Context context, String vungleAppId, String[] placementReferenceIds) {
        sVunglePub.init(context, vungleAppId, placementReferenceIds, new VungleInitListener() {
            @Override
            public void onSuccess() {
                MoPubLog.d(ROUTER_TAG + "SDK is initialized successfully.");

                sInitState = SDKInitState.INITIALIZED;

                sVunglePub.clearAndSetEventListeners(vungleDefaultListener);
                clearWaitingList();
            }

            @Override
            public void onFailure(Throwable throwable) {
                MoPubLog.w(ROUTER_TAG + "Initialization is failed.");

                sInitState = SDKInitState.NOTINITIALIZED;
            }
        });

        sInitState = SDKInitState.INITIALIZING;
    }

    public boolean isVungleInitialized() {
        if (sInitState == SDKInitState.NOTINITIALIZED) {
            return false;
        } else if (sInitState == SDKInitState.INITIALIZING) {
            return true;
        } else if (sInitState == SDKInitState.INITIALIZED) {
            return true;
        }

        return sVunglePub.isInitialized();
    }

    public void loadAdForPlacement(String placementId, VungleRouterListener routerListener) {
        switch (sInitState) {
            case NOTINITIALIZED:
                MoPubLog.w(ROUTER_TAG + "There should not be this case. loadAdForPlacement is called before initialization starts.");
                break;

            case INITIALIZING:
                sWaitingList.put(placementId, routerListener);
                break;

            case INITIALIZED:
                addRouterListener(placementId, routerListener);
                sVunglePub.loadAd(placementId);
                break;
        }
    }

    private void addRouterListener(String placementId, VungleRouterListener routerListener) {
        sVungleRouterListeners.put(placementId, routerListener);
    }

    public void removeRouterListener(String placementId) {
        sVungleRouterListeners.remove(placementId);
    }

    public boolean isAdPlayableForPlacement(String placementId) {
        return sVunglePub.isAdPlayable(placementId);
    }

    public void playAdForPlacement(String placementId, AdConfig adConfig) {
        if (sVunglePub.isAdPlayable(placementId)) {
            sVunglePub.playAd(placementId, adConfig);
        }
        else {
            MoPubLog.w(ROUTER_TAG + "There should not be this case. playAdForPlacement is called before an ad is loaded for Placement ID: " + placementId);
        }
    }

    private void clearWaitingList() {
        for (Map.Entry<String, VungleRouterListener> entry : sWaitingList.entrySet()) {
            sVunglePub.loadAd(entry.getKey());
            sVungleRouterListeners.put(entry.getKey(),entry.getValue());
        }

        sWaitingList.clear();
    }


    /*
     * VungleAdEventListener
     */
    private final VungleAdEventListener vungleDefaultListener = new VungleAdEventListener() {
        @Override
        public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            MoPubLog.d(ROUTER_TAG + "onAdEnd - Placement ID: " + placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onAdEnd(placementReferenceId, wasSuccessfulView, wasCallToActionClicked);
            } else {
                MoPubLog.w(ROUTER_TAG + "onAdEnd - VungleRouterListener is not found for Placement ID: " + placementReferenceId);
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            MoPubLog.d(ROUTER_TAG + "onAdStart - Placement ID: " + placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onAdStart(placementReferenceId);
            } else {
                MoPubLog.w(ROUTER_TAG + "onAdStart - VungleRouterListener is not found for Placement ID: " + placementReferenceId);
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            MoPubLog.d(ROUTER_TAG + "onUnableToPlayAd - Placement ID: " + placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onUnableToPlayAd(placementReferenceId, reason);
            } else {
                MoPubLog.w(ROUTER_TAG + "onUnableToPlayAd - VungleRouterListener is not found for Placement ID: " + placementReferenceId);
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull String placementReferenceId, boolean isAdAvailable) {
            MoPubLog.d(ROUTER_TAG + "onAdAvailabilityUpdate - Placement ID: " + placementReferenceId);

            VungleRouterListener targetListener = sVungleRouterListeners.get(placementReferenceId);
            if (targetListener != null) {
                targetListener.onAdAvailabilityUpdate(placementReferenceId, isAdAvailable);
            } else {
                MoPubLog.w(ROUTER_TAG + "onAdAvailabilityUpdate - VungleRouterListener is not found for Placement ID: " + placementReferenceId);
            }
        }
    };
}
