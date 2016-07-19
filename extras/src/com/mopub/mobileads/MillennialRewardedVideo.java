package com.mopub.mobileads;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.millennialmedia.AppInfo;
import com.millennialmedia.InterstitialAd;
import com.millennialmedia.InterstitialAd.InterstitialErrorStatus;
import com.millennialmedia.InterstitialAd.InterstitialListener;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.XIncentivizedEventListener;
import com.millennialmedia.internal.ActivityListenerManager;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;

import java.util.Map;

/**
 * Compatible with version 6.3 of the Millennial Media SDK.
 */

class MillennialRewardedVideo extends CustomEventRewardedVideo {

    private static final String TAG = MillennialRewardedVideo.class.getSimpleName();
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";

    private InterstitialAd mMillennialInterstitial;
    private MillennialRewardedVideoListener mMillennialRewardedVideoListener = new MillennialRewardedVideoListener();
    private Activity mActivity;
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());
    private String apid = "";

    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return mMillennialRewardedVideoListener;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return new BaseLifecycleListener();
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return apid;
    }

    @Override
    protected void onInvalidate() {
        if (mMillennialInterstitial != null) {
            mMillennialInterstitial.setListener(null);
            mMillennialInterstitial = null;
            apid = "";
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (!MMSDK.isInitialized()) {
                        try {
                            MMSDK.initialize(launcherActivity, ActivityListenerManager.LifecycleState.RESUMED);
                        } catch (Exception e) {
                            Log.e(TAG, "Error initializing MMSDK", e);
                            return false;
                        }
                    }

            } else {
                Log.e(TAG, "MMSDK minimum supported API is 16");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MMSDK", e);
            return false;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        String dcn;
        mActivity = activity;
        apid = "";

        if (extrasAreValid(serverExtras)) {
            dcn = serverExtras.get(DCN_KEY);
            apid = serverExtras.get(APID_KEY);
        } else {
            Log.e(TAG, "Invalid extras-- Be sure you have a placement ID specified.");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, "", MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        // Add DCN support
        AppInfo ai = new AppInfo().setMediator("mopubsdk").setSiteId(dcn);
        try {
            MMSDK.setAppInfo(ai);
            /* If MoPub gets location, so do we. */
            MMSDK.setLocationEnabled((localExtras.get("location") != null));

            mMillennialInterstitial = InterstitialAd.createInstance(apid);
        } catch (MMException e) {
            Log.e(TAG, "Could not create instance of InterstitialAd", e);
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, apid, MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }

        if (mMillennialInterstitial != null) {

            mMillennialInterstitial.setListener(mMillennialRewardedVideoListener);
            mMillennialInterstitial.xSetIncentivizedListener(mMillennialRewardedVideoListener);
            mMillennialInterstitial.load(activity, null);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return (mMillennialInterstitial != null && mMillennialInterstitial.isReady());
    }

    @Override
    protected void showVideo() {
        if (mMillennialInterstitial != null && mMillennialInterstitial.isReady()) {
            try {
                mMillennialInterstitial.show(mActivity);
            } catch (MMException e) {
                Log.e(TAG, "Could not show interstitial", e);
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MillennialRewardedVideo.class, mMillennialInterstitial.placementId, MoPubErrorCode.INTERNAL_ERROR);
                    }
                });
            }
        } else {
            Log.w(TAG, "showInterstitial called before MillennialInterstitial ad was loaded.");
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialRewardedVideoListener implements InterstitialListener, XIncentivizedEventListener, CustomEventRewardedVideoListener {

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
            // Intentionally not calling MoPub's onLeaveApplication to avoid double-count
            Log.d(TAG, "Millennial Rewarded Video Ad - Leaving application");
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Ad was clicked");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoClicked(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Ad was closed");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoClosed(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onExpired(final InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Ad expired");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId, MoPubErrorCode.VIDEO_NOT_AVAILABLE);
                }
            });
        }

        @Override
        public void onLoadFailed(final InterstitialAd interstitialAd,
                                 InterstitialErrorStatus interstitialErrorStatus) {
            Log.d(TAG, "Millennial Rewarded Video Ad - load failed (" + interstitialErrorStatus.getErrorCode() + "): " + interstitialErrorStatus.getDescription());
            final MoPubErrorCode moPubErrorCode;

            switch (interstitialErrorStatus.getErrorCode()) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    UI_THREAD_HANDLER.post(new Runnable() {
                        @Override
                        public void run() {
                            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(MillennialRewardedVideo.class, interstitialAd.placementId);
                        }
                    });
                    Log.w(TAG, "Millennial Rewarded Video Ad - Attempted to load ads when ads are already loaded.");
                    return;
                case InterstitialErrorStatus.EXPIRED:
                case InterstitialErrorStatus.DISPLAY_FAILED:
                case InterstitialErrorStatus.INIT_FAILED:
                case InterstitialErrorStatus.ADAPTER_NOT_FOUND:
                    moPubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case InterstitialErrorStatus.NO_NETWORK:
                    moPubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case InterstitialErrorStatus.UNKNOWN:
                    moPubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case InterstitialErrorStatus.NOT_LOADED:
                case InterstitialErrorStatus.LOAD_FAILED:
                default:
                    moPubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
            }

            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId, moPubErrorCode);
                }
            });
        }

        @Override
        public void onLoaded(final InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Ad loaded splendidly");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public void onShowFailed(final InterstitialAd interstitialAd,
                                 InterstitialErrorStatus interstitialErrorStatus) {
            Log.e(TAG, "Millennial Rewarded Video Ad - Show failed (" + interstitialErrorStatus.getErrorCode() + "): " + interstitialErrorStatus.getDescription());
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MillennialRewardedVideo.class, interstitialAd.placementId, MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Ad shown");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoStarted(MillennialRewardedVideo.class, interstitialAd.placementId);
                }
            });
        }

        @Override
        public boolean onVideoComplete() {
            Log.d(TAG, "Millennial Rewarded Video Ad - Video completed");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    MoPubRewardedVideoManager.onRewardedVideoCompleted(MillennialRewardedVideo.class, mMillennialInterstitial.placementId,
                            MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT ));
                }
            });
            return false;
        }

        @Override
        public boolean onCustomEvent(XIncentiveEvent xIncentiveEvent) {
            Log.d(TAG, "Millennial Rewarded Video Ad - Custom event received: "+xIncentiveEvent.eventId+", "+xIncentiveEvent.args);
            return false;
        }
    }
}
