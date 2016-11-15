package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.millennialmedia.AppInfo;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.NativeAd;
import com.millennialmedia.internal.ActivityListenerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

/**
 * Compatible with version 6.3 of the Millennial Media SDK.
 */

public class MillennialNative extends CustomEventNative {
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";
    private final static String TAG = MillennialNative.class.getSimpleName();
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    protected void loadNativeAd(final Context context,
            final CustomEventNativeListener customEventNativeListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        String placementId;
        String siteId;
        if (!initializeSDK(context)) {
            Log.e(TAG, "Unable to initialize MMSDK");
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(APID_KEY);
            siteId = serverExtras.get(DCN_KEY);
        } else {
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        try {
            AppInfo ai = new AppInfo().setMediator("mopubsdk");
            if (siteId != null && siteId.length() > 0) {
                ai = ai.setSiteId(siteId);
            } else {
                ai = ai.setSiteId(null);
            }

            try {
                MMSDK.setAppInfo(ai);
            } catch (MMException e) {
                Log.e(TAG, "MM SDK is not initialized", e);
            }
        } catch (IllegalStateException e) {
            Log.w(TAG, "App info error", e);
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        try {
            NativeAd nativeAd = NativeAd.createInstance(placementId, NativeAd.NATIVE_TYPE_INLINE);
            final MillennialStaticNativeAd millennialStaticNativeAd =
                    new MillennialStaticNativeAd(context,
                            nativeAd,
                            new ImpressionTracker(context),
                            new NativeClickHandler(context),
                            customEventNativeListener);
            millennialStaticNativeAd.loadAd();
        } catch (MMException e) {
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        String placementId = serverExtras.get(APID_KEY);
        return (serverExtras.containsKey(APID_KEY) &&
                placementId != null && placementId.length() > 0);
    }


    static class MillennialStaticNativeAd extends StaticNativeAd implements NativeAd.NativeListener {
        private final Context mContext;
        private NativeAd mNativeAd;
        private final ImpressionTracker mImpressionTracker;
        private final NativeClickHandler mNativeClickHandler;
        private final CustomEventNativeListener mListener;
        private final MillennialStaticNativeAd mMillennialStaticNativeAd;

        public MillennialStaticNativeAd(final Context context,
                final NativeAd nativeAd,
                final ImpressionTracker impressionTracker,
                final NativeClickHandler nativeClickHandler,
                final CustomEventNativeListener customEventNativeListener) {
            mContext = context.getApplicationContext();
            mNativeAd = nativeAd;
            mImpressionTracker = impressionTracker;
            mNativeClickHandler = nativeClickHandler;
            mListener = customEventNativeListener;
            mMillennialStaticNativeAd = this;

            nativeAd.setListener(this);
        }

        void loadAd() {
            Log.d(TAG, "Millennial native ad loading.");
            try {
                mNativeAd.load(mContext, null);
            } catch (MMException e) {
                Log.w(TAG, "Configuration error", e);
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onNativeAdFailed(NativeErrorCode
                                .NATIVE_ADAPTER_CONFIGURATION_ERROR);
                    }
                });
            }
        }

        // Lifecycle Handlers
        @Override
        public void prepare(final View view) {
            // Must access these methods directly to get impressions to fire.
            mNativeAd.getIconImage();
            mNativeAd.getDisclaimer();
            mImpressionTracker.addView(view, this);
            mNativeClickHandler.setOnClickListener(view, this);
        }

        @Override
        public void clear(final View view) {
            mImpressionTracker.removeView(view);
            mNativeClickHandler.clearOnClickListener(view);
        }

        @Override
        public void destroy() {
            mImpressionTracker.destroy();
            mNativeAd.setListener(null);
            mNativeAd = null;
        }

        // Event Handlers
        @Override
        public void recordImpression(final View view) {
            notifyAdImpressed();
            try {
                mNativeAd.fireImpression();
                Log.d(TAG, "Millennial native ad impression recorded.");
            } catch (MMException m) {
                Log.e(TAG, "Error tracking Millennial native ad impression", m);
            }
        }

        @Override
        public void handleClick(final View view) {
            notifyAdClicked();
            mNativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
            mNativeAd.fireCallToActionClicked();
            Log.d(TAG, "Millennial native ad clicked.");
        }

        // MM'S Native mListener
        @Override
        public void onLoaded(NativeAd nativeAd) {
            // Set assets
            String iconImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.ICON_IMAGE, 1);
            String mainImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.MAIN_IMAGE, 1);

            setTitle(nativeAd.getTitle().getText().toString());
            setText(nativeAd.getBody().getText().toString());
            setCallToAction(nativeAd.getCallToActionButton().getText().toString());

            final String clickDestinationUrl = nativeAd.getCallToActionUrl();
            if (clickDestinationUrl == null) {
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,
                                "Millennial native ad encountered null destination url. Failing over.");
                        mListener.onNativeAdFailed(
                                NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                    }
                });
                return;
            }

            setClickDestinationUrl(clickDestinationUrl);
            setIconImageUrl(iconImageUrl);
            setMainImageUrl(mainImageUrl);

            final List<String> urls = new ArrayList<>();
            if (iconImageUrl != null) {
                urls.add(iconImageUrl);
            }
            if (mainImageUrl != null) {
                urls.add(mainImageUrl);
            }

            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    // This has to be run on the main thread:
                    preCacheImages(mContext, urls, new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            mListener.onNativeAdLoaded(mMillennialStaticNativeAd);
                            Log.d(TAG, "Millennial native ad loaded.");
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            mListener.onNativeAdFailed(errorCode);
                        }
                    });

                }
            });
        }

        @Override
        public void onLoadFailed(NativeAd nativeAd, NativeAd.NativeErrorStatus nativeErrorStatus) {
            final NativeErrorCode error;
            switch (nativeErrorStatus.getErrorCode()) {
                case NativeAd.NativeErrorStatus.LOAD_TIMED_OUT:
                    error = NativeErrorCode.NETWORK_TIMEOUT;
                    break;
                case NativeAd.NativeErrorStatus.NO_NETWORK:
                    error = NativeErrorCode.CONNECTION_ERROR;
                    break;
                case NativeAd.NativeErrorStatus.UNKNOWN:
                    error = NativeErrorCode.UNSPECIFIED;
                    break;
                case NativeAd.NativeErrorStatus.LOAD_FAILED:
                case NativeAd.NativeErrorStatus.INIT_FAILED:
                    error = NativeErrorCode.UNEXPECTED_RESPONSE_CODE;
                    break;
                case NativeAd.NativeErrorStatus.ADAPTER_NOT_FOUND:
                    error = NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR;
                    break;
                case NativeAd.NativeErrorStatus.DISPLAY_FAILED:
                case NativeAd.NativeErrorStatus.EXPIRED:
                    error = NativeErrorCode.UNSPECIFIED;
                    break;
                default:
                    error = NativeErrorCode.NETWORK_NO_FILL;
            }
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onNativeAdFailed(error);
                }
            });
            Log.i(TAG, "Millennial native ad failed: " + nativeErrorStatus.getDescription());
        }

        @Override
        public void onClicked(NativeAd nativeAd, NativeAd.ComponentName componentName, int i) {
            Log.d(TAG, "Millennial native ad click tracker fired.");
        }

        @Override
        public void onAdLeftApplication(NativeAd nativeAd) {
            Log.d(TAG, "Millennial native ad has left the application.");

        }

        @Override
        public void onExpired(NativeAd nativeAd) {
            Log.d(TAG, "Millennial native ad has expired!");
        }

    }

    private boolean initializeSDK(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (!MMSDK.isInitialized()) {
                    try {
                        MMSDK.initialize((Activity) context, ActivityListenerManager.LifecycleState.RESUMED);
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
}
