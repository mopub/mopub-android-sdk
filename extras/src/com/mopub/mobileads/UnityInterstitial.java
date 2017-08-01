package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

public class UnityInterstitial extends CustomEventInterstitial implements IUnityAdsExtendedListener {

    private static boolean sInitialized = false;
    private static boolean sAdCached = false;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Activity mLauncherActivity;
    private String mPlacementId = "video";

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, mPlacementId);
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!sInitialized) {
            if (context == null || !(context instanceof Activity)) {
                mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                return;
            }

            mLauncherActivity = (Activity) context;

            if (!UnityRouter.initUnityAds(serverExtras, mLauncherActivity, this, new Runnable() {
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                }
            })) {
                return;
            }

            UnityAds.setListener(this);

            UnityRouter.initPlacement(mPlacementId, new Runnable() {
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                }
            }, new Runnable() {
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialLoaded();
                }
            });

            sInitialized = true;
        } else {
            UnityAds.setListener(this);
            if (UnityAds.isReady(mPlacementId)) {
                mCustomEventInterstitialListener.onInterstitialLoaded();
            } else {
                sAdCached = false;
            }
        }
    }

    @Override
    protected void showInterstitial() {
        if (UnityAds.isReady(mPlacementId) && mLauncherActivity != null) {
            UnityAds.show(mLauncherActivity, mPlacementId);
        }
    }

    @Override
    protected void onInvalidate() {
        UnityAds.setListener(null);
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (!sAdCached && placementId.equals(mPlacementId)) {
            sAdCached = true;
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onUnityAdsStart(String s) {
        mCustomEventInterstitialListener.onInterstitialShown();
    }

    @Override
    public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
        mCustomEventInterstitialListener.onInterstitialDismissed();
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        mCustomEventInterstitialListener.onInterstitialClicked();
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {
        MoPubErrorCode errorCode;
        switch (unityAdsError) {
            case VIDEO_PLAYER_ERROR:
                errorCode = MoPubErrorCode.VIDEO_PLAYBACK_ERROR;
                break;
            case INTERNAL_ERROR:
                errorCode = MoPubErrorCode.INTERNAL_ERROR;
                break;
            default:
                errorCode = MoPubErrorCode.NETWORK_INVALID_STATE;
                break;
        }
        mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
    }
}