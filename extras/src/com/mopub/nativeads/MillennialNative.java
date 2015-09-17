package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.millennialmedia.AppInfo;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.millennialmedia.NativeAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MillennialNative extends CustomEventNative {
    public static final String DCN_KEY = "dcn";
    public static final String APID_KEY = "adUnitID";
    private final static String LOGCAT_TAG = "MoPub->MM-Native";
    private static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    @Override
    protected void loadNativeAd(final Context context,
            final CustomEventNativeListener listener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        String placementId;
        String siteId;

        if ( !MMSDK.isInitialized() ) {
            try {
                MMSDK.initialize((Activity) context);
            } catch ( Exception e ) {
                Log.e(LOGCAT_TAG, "Unable to initialize the Millennial SDK-- " + e.getMessage());
                e.printStackTrace();
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                    }
                });
                return;
            }
        }

        if ( extrasAreValid( serverExtras )) {
            placementId = serverExtras.get(APID_KEY);
            siteId = serverExtras.get(DCN_KEY);
        } else {
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        try {
            AppInfo ai = new AppInfo().setMediator("mopubsdk");
            if ( siteId != null && siteId.length() > 0 ) {
                ai = ai.setSiteId(siteId);
            } else {
                ai = ai.setSiteId(null);
            }

            MMSDK.setAppInfo(ai);
        } catch ( IllegalStateException e ) {
            Log.w(LOGCAT_TAG, "Caught exception: " + e.getMessage());
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
            return;
        }

        try {
            NativeAd n = NativeAd.createInstance(placementId, NativeAd.NATIVE_TYPE_INLINE);
            final MillennialForwardingNativeAd mmForwardingNativeAd = new MillennialForwardingNativeAd(context, n, listener);
            mmForwardingNativeAd.loadAd();
        } catch ( MMException e ) {
            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                }
            });
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        String placementId = serverExtras.get(APID_KEY);
        return (serverExtras.containsKey(APID_KEY) &&
                placementId != null & placementId.length() > 0 );
    }


    static class MillennialForwardingNativeAd extends BaseForwardingNativeAd implements NativeAd.NativeListener {
        private Context context;
        private NativeAd nativeAd;
        private CustomEventNativeListener listener;
        private MillennialForwardingNativeAd millennialForwardingNativeAd;

        public MillennialForwardingNativeAd(final Context context, final NativeAd nativeAd, final CustomEventNativeListener customEventNativeListener) {
            this.context = context.getApplicationContext();
            this.nativeAd = nativeAd;
            this.listener = customEventNativeListener;
            this.millennialForwardingNativeAd = this;

            nativeAd.setListener(this);
        }

        void loadAd() {
            Log.i(LOGCAT_TAG, "Loading native ad...");
            try {
                nativeAd.load(this.context, null);
            } catch (MMException e) {
                Log.w(MillennialNative.LOGCAT_TAG, "Caught configuration error Exception.");
                e.printStackTrace();
                UI_THREAD_HANDLER.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
                    }
                });
            }
        }

        // BaseForwardingNativeAd
        @Override
        public void destroy() {
            nativeAd.setListener(null);
            nativeAd = null;
        }

        @Override
        public void prepare(final View view) {
            // Must access these methods directly to get impressions to fire.
            nativeAd.getIconImage();
            nativeAd.getDisclaimer();
        }

        @Override
        public void handleClick(final View view) {
            nativeAd.fireClicked();
            Log.i(LOGCAT_TAG, "Millennial native ad clicked!");
        }

        @Override
        public void recordImpression() {
            super.recordImpression();
            try {
                nativeAd.fireImpression();
                Log.i(LOGCAT_TAG, "Millennial native impression recorded.");
            } catch ( MMException m ) {
                Log.e(LOGCAT_TAG, "Millennial native impression NOT tracked: " + m.getMessage() );
            }
        }

        // MM'S Native listener
        @Override
        public void onLoaded(NativeAd nativeAd) {
            // Set assets
            String iconImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.ICON_IMAGE, 1);
            String mainImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.MAIN_IMAGE, 1);

            this.setTitle(nativeAd.getTitle().getText().toString());
            this.setText(nativeAd.getBody().getText().toString());
            this.setCallToAction(nativeAd.getCallToActionButton().getText().toString());
            this.setClickDestinationUrl(nativeAd.getCallToActionUrl());
            this.setIconImageUrl(iconImageUrl);
            this.setMainImageUrl(mainImageUrl);

            final List<String> urls = new ArrayList<String>();
            if ( iconImageUrl != null ) { urls.add(iconImageUrl); }
            if ( mainImageUrl != null ) { urls.add(mainImageUrl); }

            // Use MoPub's click/impression trackers
            this.setOverridingImpressionTracker(false);
            this.setOverridingClickTracker(false);

            UI_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    // This has to be run on the main thread:
                    preCacheImages(context, urls, new ImageListener() {
                        @Override
                        public void onImagesCached() {
                            listener.onNativeAdLoaded(millennialForwardingNativeAd);
                            Log.i(LOGCAT_TAG, "Millennial native ad loaded");
                        }

                        @Override
                        public void onImagesFailedToCache(NativeErrorCode errorCode) {
                            listener.onNativeAdFailed(errorCode);
                        }
                    });

                }
            });
        }

        @Override
        public void onLoadFailed(NativeAd nativeAd, NativeAd.NativeErrorStatus nativeErrorStatus) {
            final NativeErrorCode error;
            switch ( nativeErrorStatus.getErrorCode() ) {
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
                    listener.onNativeAdFailed(error);
                }
            });
            Log.i(LOGCAT_TAG, "Millennial native ad failed: " + nativeErrorStatus.getDescription() );
        }

        @Override
        public void onClicked(NativeAd nativeAd, NativeAd.ComponentName componentName, int i) {
            Log.i(LOGCAT_TAG, "Millennial native SDK's click tracker fired.");
        }

        @Override
        public void onAdLeftApplication(NativeAd nativeAd) {
            Log.i(LOGCAT_TAG, "Millennial native SDK has left the application.");

        }

        @Override
        public void onExpired(NativeAd nativeAd) {
            Log.i(LOGCAT_TAG, "Millennial native ad has expired!");
        }

    }
}
