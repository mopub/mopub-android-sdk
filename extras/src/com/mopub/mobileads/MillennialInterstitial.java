package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.millennialmedia.AppInfo;
import com.millennialmedia.InterstitialAd;
import com.millennialmedia.InterstitialAd.InterstitialErrorStatus;
import com.millennialmedia.InterstitialAd.InterstitialListener;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.internal.ActivityListenerManager;

import java.util.Map;

/**
 * Compatible with version 6.3 of the Millennial Media SDK.
 */

class MillennialInterstitial extends CustomEventInterstitial {

    private static final String TAG = MillennialInterstitial.class.getSimpleName();
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";

    private InterstitialAd mMillennialInterstitial;
    private Context mContext;
    private CustomEventInterstitialListener mInterstitialListener;
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    protected void loadInterstitial(final Context context, final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        String dcn;
        mInterstitialListener = customEventInterstitialListener;
        mContext = context;

        final String apid;

        if (!initializeSDK(context)) {
            Log.e(TAG, "Unable to initialize MMSDK");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
            return;
        }

        if (extrasAreValid(serverExtras)) {
            dcn = serverExtras.get(DCN_KEY);
            apid = serverExtras.get(APID_KEY);
        } else {
            Log.e(TAG, "Invalid extras-- Be sure you have an placement ID specified.");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        // Add DCN support
        try {
            AppInfo ai = new AppInfo().setMediator("mopubsdk");
            if (dcn != null && dcn.length() > 0) {
                ai = ai.setSiteId(dcn);
            } else {
                ai.setSiteId(null);
            }
            try {
                MMSDK.setAppInfo(ai);
            } catch (MMException e) {
                Log.e(TAG, "MM SDK is not initialized", e);
            }
        } catch (IllegalStateException e) {
            Log.i(TAG, "SDK not finished initializing-- " + e.getMessage());
        }

        try {
        /* If MoPub gets location, so do we. */
            MMSDK.setLocationEnabled((localExtras.get("location") != null));
        } catch (MMException e) {
            Log.e(TAG, "MM SDK is not initialized", e);
        }

        try {
            mMillennialInterstitial = InterstitialAd.createInstance(apid);
        } catch (MMException e) {
            e.printStackTrace();
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
            return;
        }

        mMillennialInterstitial.setListener(new MillennialInterstitialListener());
        mMillennialInterstitial.load(context, null);
    }

    @Override
    protected void showInterstitial() {
        if (mMillennialInterstitial.isReady()) {
            try {
                mMillennialInterstitial.show(mContext);
            } catch (MMException e) {
                e.printStackTrace();
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                    }
                });
                return;
            }
        } else {
            Log.w(TAG, "showInterstitial called before Millennial's ad was loaded.");
        }
    }

    @Override
    protected void onInvalidate() {
        if (mMillennialInterstitial != null) {
            mMillennialInterstitial.setListener(null);
            mMillennialInterstitial = null;
        }
    }

    private boolean initializeSDK(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (!MMSDK.isInitialized()) {
                    if (context instanceof Activity) {
                        try {
                            MMSDK.initialize(((Activity) context), ActivityListenerManager.LifecycleState.RESUMED);
                        } catch (Exception e) {
                            Log.e(TAG, "Error initializing MMSDK", e);
                            return false;
                        }
                    } else {
                        Log.e(TAG, "MMSDK.initialize must be explicitly called when instantiating the MoPub AdView or InterstitialAd without an Activity.");
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

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialInterstitialListener implements InterstitialListener {

        @Override
        public void onAdLeftApplication(InterstitialAd interstitialAd) {
            // Intentionally not calling MoPub's onLeaveApplication to avoid double-count
            Log.d(TAG, "Millennial Interstitial Ad - Leaving application");
        }

        @Override
        public void onClicked(InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Interstitial Ad - Ad was clicked");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialClicked();
                }
            });
        }

        @Override
        public void onClosed(InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Interstitial Ad - Ad was closed");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialDismissed();
                }
            });
        }

        @Override
        public void onExpired(InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Interstitial Ad - Ad expired");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                }
            });
        }

        @Override
        public void onLoadFailed(InterstitialAd interstitialAd,
                InterstitialErrorStatus interstitialErrorStatus) {
            Log.d(TAG, "Millennial Interstitial Ad - load failed (" + interstitialErrorStatus.getErrorCode() + "): " + interstitialErrorStatus.getDescription());
            final MoPubErrorCode moPubErrorCode;

            switch (interstitialErrorStatus.getErrorCode()) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    mInterstitialListener.onInterstitialLoaded();
                    Log.w(TAG, "Millennial Interstitial Ad - Attempted to load ads when ads are already loaded.");
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
                    mInterstitialListener.onInterstitialFailed(moPubErrorCode);
                }
            });
        }

        @Override
        public void onLoaded(InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Interstitial Ad - Ad loaded splendidly");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialLoaded();
                }
            });
        }

        @Override
        public void onShowFailed(InterstitialAd interstitialAd,
                InterstitialErrorStatus interstitialErrorStatus) {
            Log.e(TAG, "Millennial Interstitial Ad - Show failed (" + interstitialErrorStatus.getErrorCode() + "): " + interstitialErrorStatus.getDescription());
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }

        @Override
        public void onShown(InterstitialAd interstitialAd) {
            Log.d(TAG, "Millennial Interstitial Ad - Ad shown");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialShown();
                }
            });
        }
    }
}
