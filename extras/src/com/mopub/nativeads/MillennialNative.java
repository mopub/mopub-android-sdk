package com.mopub.nativeads;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.millennialmedia.NativeAd;
import com.mopub.mobileads.MillennialUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

/**
 * Compatible with version 6.6 of the Millennial Media SDK.
 */

public class MillennialNative extends CustomEventNative {
    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private final static String TAG = MillennialNative.class.getSimpleName();

    MillennialStaticNativeAd staticNativeAd;

    public CreativeInfo getCreativeInfo() {

        if (staticNativeAd == null) {
            return null;
        }

        return staticNativeAd.getCreativeInfo();
    }


    @Override
    protected void loadNativeAd(final Context context, final CustomEventNativeListener customEventNativeListener,
                                Map<String, Object> localExtras, Map<String, String> serverExtras) {

        if (!MillennialUtils.initSdk(context)) {
            Log.e(TAG, "MM SDK must be initialized with an Activity or Application context.");
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String placementId = serverExtras.get(APID_KEY);
        String siteId = serverExtras.get(DCN_KEY);

        if (MillennialUtils.isEmpty(placementId)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        AppInfo ai = new AppInfo().setMediator("mopubsdk").setSiteId(siteId);

        try {
            MMSDK.setAppInfo(ai);

            NativeAd nativeAd = NativeAd.createInstance(placementId, NativeAd.NATIVE_TYPE_INLINE);
            staticNativeAd = new MillennialStaticNativeAd(context, nativeAd, new ImpressionTracker(context),
                    new NativeClickHandler(context), customEventNativeListener);

            staticNativeAd.loadAd();

        } catch (MMException e) {
            Log.e(TAG, "An exception occurred loading a native ad from MM SDK", e);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
        }
    }


    static class MillennialStaticNativeAd extends StaticNativeAd implements NativeAd.NativeListener {

        private final Context context;
        private NativeAd nativeAd;
        private final ImpressionTracker impressionTracker;
        private final NativeClickHandler nativeClickHandler;
        private final CustomEventNativeListener listener;


        public MillennialStaticNativeAd(final Context context, final NativeAd nativeAd,
                                        final ImpressionTracker impressionTracker, final NativeClickHandler nativeClickHandler,
                                        final CustomEventNativeListener customEventNativeListener) {

            this.context = context.getApplicationContext();
            this.nativeAd = nativeAd;
            this.impressionTracker = impressionTracker;
            this.nativeClickHandler = nativeClickHandler;
            listener = customEventNativeListener;

            nativeAd.setListener(this);
        }


        void loadAd() throws MMException {

            Log.d(TAG, "Millennial native ad loading.");

            nativeAd.load(context, null);
        }


        CreativeInfo getCreativeInfo() {

            if (nativeAd == null) {
                return null;
            }

            return nativeAd.getCreativeInfo();
        }


        // Lifecycle Handlers
        @Override
        public void prepare(final View view) {
            // Must access these methods directly to get impressions to fire.
            nativeAd.getIconImage();
            nativeAd.getDisclaimer();
            impressionTracker.addView(view, this);
            nativeClickHandler.setOnClickListener(view, this);
        }


        @Override
        public void clear(final View view) {

            impressionTracker.removeView(view);
            nativeClickHandler.clearOnClickListener(view);
        }


        @Override
        public void destroy() {

            impressionTracker.destroy();
            nativeAd.destroy();
            nativeAd = null;
        }


        // Event Handlers
        @Override
        public void recordImpression(final View view) {

            notifyAdImpressed();

            try {
                nativeAd.fireImpression();
                Log.d(TAG, "Millennial native ad impression recorded.");
            } catch (MMException e) {
                Log.e(TAG, "Error tracking Millennial native ad impression", e);
            }
        }


        @Override
        public void handleClick(final View view) {

            notifyAdClicked();

            nativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
            nativeAd.fireCallToActionClicked();
            Log.d(TAG, "Millennial native ad clicked.");
        }


        // MM'S Native listener
        @Override
        public void onLoaded(NativeAd nativeAd) {

            CreativeInfo creativeInfo = getCreativeInfo();
            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {

                MMLog.d(TAG, "Native Creative Info: " + creativeInfo);
            }

            // Set assets
            String iconImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.ICON_IMAGE, 1);
            String mainImageUrl = nativeAd.getImageUrl(NativeAd.ComponentName.MAIN_IMAGE, 1);

            setTitle(nativeAd.getTitle().getText().toString());
            setText(nativeAd.getBody().getText().toString());
            setCallToAction(nativeAd.getCallToActionButton().getText().toString());

            final String clickDestinationUrl = nativeAd.getCallToActionUrl();
            if (clickDestinationUrl == null) {
                MillennialUtils.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(TAG, "Millennial native ad encountered null destination url.");
                        listener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
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

            addExtra("disclaimer", nativeAd.getDisclaimer().getText());

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // This has to be run on the main thread:
                    preCacheImages(context, urls, new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            listener.onNativeAdLoaded(MillennialStaticNativeAd.this);
                            Log.d(TAG, "Millennial native ad loaded.");
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
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onNativeAdFailed(error);
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
}
