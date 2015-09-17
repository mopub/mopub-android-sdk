package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;

import com.millennialmedia.AppInfo;
import com.millennialmedia.InterstitialAd;
import com.millennialmedia.InterstitialAd.InterstitialErrorStatus;
import com.millennialmedia.InterstitialAd.InterstitialListener;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;

/**
 * Compatible with version 6.0 of the Millennial Media SDK.
 */

class MillennialInterstitial extends CustomEventInterstitial {

    public static final String LOGCAT_TAG = "MP->MM Int.";
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";

    private InterstitialAd mMillennialInterstitial;
    private Context mContext;
    private CustomEventInterstitialListener mInterstitialListener;
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    protected void loadInterstitial(final Context context, final CustomEventInterstitialListener customEventInterstitialListener,
            final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        String dcn = null;
        mInterstitialListener = customEventInterstitialListener;
        mContext = context;

        final String apid;

        if ( !MMSDK.isInitialized() ) {
            try {
                MMSDK.initialize((Activity) context);
            } catch ( Exception e ) {
                Log.e(LOGCAT_TAG, "Unable to initialize the Millennial SDK-- " + e.getMessage());
                e.printStackTrace();
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                    }
                });
                return;
            }
        }

        if (extrasAreValid(serverExtras)) {
            dcn = serverExtras.get(DCN_KEY);
            apid = serverExtras.get(APID_KEY);
        } else {
            Log.e(LOGCAT_TAG, "Invalid extras-- Be sure you have an placement ID specified.");
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
            if ( dcn != null && dcn.length() > 0 ) {
                ai = ai.setSiteId(dcn);
            } else {
                ai.setSiteId(null);
            }
            MMSDK.setAppInfo(ai);
        } catch ( IllegalStateException e ) {
            Log.i(LOGCAT_TAG, "SDK not finished initializing-- " + e.getMessage());
        }
        
        /* If MoPub gets location, so do we. */
        MMSDK.setLocationEnabled( (localExtras.get("location") != null) );

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
            } catch ( MMException e ) {
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
            Log.w(LOGCAT_TAG, "showInterstitial called before Millennial's ad was loaded.");
        }
    }

    @Override
    protected void onInvalidate() {
        if (mMillennialInterstitial != null) {
            mMillennialInterstitial.setListener(null);
            mMillennialInterstitial = null;
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialInterstitialListener implements InterstitialListener {

        @Override
        public void onAdLeftApplication(InterstitialAd arg0) {
            // Intentionally not calling MoPub's onLeaveApplication to avoid double-count
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Leaving application");
        }

        @Override
        public void onClicked(InterstitialAd arg0) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Ad was clicked");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialClicked();
                }
            });
        }

        @Override
        public void onClosed(InterstitialAd arg0) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Ad was closed");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialDismissed();
                }
            });
        }

        @Override
        public void onExpired(InterstitialAd arg0) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Ad expired");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                }
            });
        }

        @Override
        public void onLoadFailed(InterstitialAd arg0,
                InterstitialErrorStatus err) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - load failed (" + err.getErrorCode() + "): " + err.getDescription() );
            final MoPubErrorCode moPubErrorCode;

            switch (err.getErrorCode() ) {
                case InterstitialErrorStatus.ALREADY_LOADED:
                    // This will generate discrepancies, as requests will NOT be sent to Millennial.
                    mInterstitialListener.onInterstitialLoaded();
                    Log.w(LOGCAT_TAG, "Millennial Interstitial Ad - Attempted to load ads when ads are already loaded." );
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
        public void onLoaded(InterstitialAd arg0) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Ad loaded splendidly");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialLoaded();
                }
            });
        }

        @Override
        public void onShowFailed(InterstitialAd arg0,
                InterstitialErrorStatus arg1) {
            Log.e(LOGCAT_TAG, "Millennial Interstitial Ad - Show failed (" + arg1.getErrorCode() + "): " + arg1.getDescription());
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }

        @Override
        public void onShown(InterstitialAd arg0) {
            Log.d(LOGCAT_TAG, "Millennial Interstitial Ad - Ad shown");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mInterstitialListener.onInterstitialShown();
                }
            });
        }
    }
}
