package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.millennialmedia.AppInfo;
import com.millennialmedia.InlineAd;
import com.millennialmedia.InlineAd.AdSize;
import com.millennialmedia.InlineAd.InlineAdMetadata;
import com.millennialmedia.InlineAd.InlineErrorStatus;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.internal.ActivityListenerManager;

import java.util.Map;

/**
 * Compatible with version 6.3 of the Millennial Media SDK.
 */

class MillennialBanner extends CustomEventBanner {

    private static final String TAG = MillennialBanner.class.getSimpleName();
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";
    public static final String AD_WIDTH_KEY = "adWidth";
    public static final String AD_HEIGHT_KEY = "adHeight";

    private InlineAd mInlineAd;
    private CustomEventBannerListener mBannerListener;
    private LinearLayout mInternalView;
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());


    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
            final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        LayoutParams lp;
        String apid;
        String dcn;
        int width;
        int height;
        mBannerListener = customEventBannerListener;

        if (!initializeSDK(context)) {
            Log.e(TAG, "Unable to initialize MMSDK");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
            return;
        }

        if (extrasAreValid(serverExtras)) {
            dcn = serverExtras.get(DCN_KEY);
            apid = serverExtras.get(APID_KEY);
            width = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            height = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } else {
            Log.e(TAG, "We were given invalid extras! Make sure placement ID, width, and height are specified.");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        // Set DCN on the AppInfo if provided
        try {
            AppInfo ai = new AppInfo().setMediator("mopubsdk");
            if (dcn != null && dcn.length() > 0) {
                ai = ai.setSiteId(dcn);
            } else {
                ai = ai.setSiteId(null);
            }
            try {
                MMSDK.setAppInfo(ai);
            } catch (MMException e) {
                Log.e(TAG, "MM SDK is not initialized", e);
            }
        } catch (IllegalStateException e) {
            Log.i(TAG, "Caught exception " + e.getMessage());
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
            return;
        }

        mInternalView = new LinearLayout(context);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        mInternalView.setLayoutParams(lp);

        InlineAdMetadata mInlineAdMetadata = null;

        try {
            mInlineAd = InlineAd.createInstance(apid, mInternalView);
            mInlineAdMetadata = new InlineAdMetadata().setAdSize(new AdSize(width, height));
        } catch (MMException e) {
            e.printStackTrace();
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
                }
            });
            return;
        }

        mInlineAd.setListener(new MillennialInlineListener());

        try {
        /* If MoPub gets location, so do we. */
            MMSDK.setLocationEnabled((localExtras.get("location") != null));
        } catch (MMException e) {
            Log.e(TAG, "MM SDK is not initialized", e);
        }

        AdViewController.setShouldHonorServerDimensions(mInternalView);

        mInlineAd.request(mInlineAdMetadata);
    }

    @Override
    protected void onInvalidate() {
        // Destroy any hanging references.
        if (mInlineAd != null) {
            mInlineAd.setListener(null);
            mInlineAd = null;
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

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        try {
            // Add pos / non-null and APIDs.
            int w = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            int h = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
            if (h < 0 || w < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            Log.e(TAG, "Width and height must exist and contain positive integers!");
            e.printStackTrace();
            return false;
        }

        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialInlineListener implements InlineAd.InlineListener {

        @Override
        public void onAdLeftApplication(InlineAd inlineAd) {
            // Intentionally not calling MoPub's onLeaveApplication to avoid double-count
            Log.d(TAG, "Millennial Inline Ad - Leaving application");
        }

        @Override
        public void onClicked(InlineAd inlineAd) {
            Log.d(TAG, "Millennial Inline Ad - Ad clicked");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerClicked();
                }
            });
        }

        @Override
        public void onCollapsed(InlineAd inlineAd) {
            Log.d(TAG, "Millennial Inline Ad - Banner collapsed");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerCollapsed();
                }
            });

        }

        @Override
        public void onExpanded(InlineAd inlineAd) {
            Log.d(TAG, "Millennial Inline Ad - Banner expanded");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerExpanded();
                }
            });
        }

        @Override
        public void onRequestFailed(InlineAd inlineAd, InlineErrorStatus inlineErrorStatus) {
            Log.d(TAG, "Millennial Inline Ad - Banner failed (" + inlineErrorStatus.getErrorCode() + "): " + inlineErrorStatus.getDescription());
            MoPubErrorCode mopubErrorCode;

            switch (inlineErrorStatus.getErrorCode()) {
                case InlineErrorStatus.ADAPTER_NOT_FOUND:
                    mopubErrorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                    break;
                case InlineErrorStatus.DISPLAY_FAILED:
                    mopubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case InlineErrorStatus.INIT_FAILED:
                    mopubErrorCode = MoPubErrorCode.WARMUP;
                    break;
                case InlineErrorStatus.NO_NETWORK:
                    mopubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case InlineErrorStatus.UNKNOWN:
                    mopubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case InlineErrorStatus.LOAD_FAILED:
                default:
                    mopubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
            }

            final MoPubErrorCode fErrorCode = mopubErrorCode;
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerFailed(fErrorCode);
                }
            });

        }

        @Override
        public void onRequestSucceeded(InlineAd inlineAd) {
            Log.d(TAG, "Millennial Inline Ad - Banner request succeeded");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mBannerListener.onBannerLoaded(mInternalView);
                }
            });
        }

        @Override
        public void onResize(InlineAd inlineAd, int w, int h) {
            Log.d(TAG, "Millennial Inline Ad - Banner about to resize (width: " + w + ", height: " + h + ")");
        }

        @Override
        public void onResized(InlineAd inlineAd, int w, int h, boolean isClosed) {
            Log.d(TAG, "Millennial Inline Ad - Banner resized (width: " + w + ", height: " + h + "). "
                    + (isClosed ? "Returned to original placement." : "Got a fresh, new place."));

        }

    }

}
