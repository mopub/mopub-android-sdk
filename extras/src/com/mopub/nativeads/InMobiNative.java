package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.inmobi.commons.InMobi;
import com.inmobi.monetization.IMErrorCode;
import com.inmobi.monetization.IMNative;
import com.inmobi.monetization.IMNativeListener;
import com.mopub.common.logging.MoPubLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.util.Json.getJsonValue;
import static com.mopub.common.util.Numbers.parseDouble;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;

/*
 * Tested with InMobi SDK 4.4.1
 */
class InMobiNative extends CustomEventNative {
    private static final String APP_ID_KEY = "app_id";

    // CustomEventNative implementation
    @Override
    protected void loadNativeAd(final Activity activity,
            final CustomEventNativeListener customEventNativeListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        final String appId;
        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(APP_ID_KEY);
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        InMobi.initialize(activity, appId);
        final InMobiStaticNativeAd inMobiStaticNativeAd =
                new InMobiStaticNativeAd(activity,
                        new ImpressionTracker(activity),
                        new NativeClickHandler(activity),
                        customEventNativeListener);
        inMobiStaticNativeAd.setIMNative(new IMNative(inMobiStaticNativeAd));
        inMobiStaticNativeAd.loadAd();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(APP_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    static class InMobiStaticNativeAd extends StaticNativeAd implements IMNativeListener {
        static final int IMPRESSION_MIN_TIME_VIEWED = 0;

        // Modifiable keys
        static final String TITLE = "title";
        static final String DESCRIPTION = "description";
        static final String SCREENSHOTS = "screenshots";
        static final String ICON = "icon";
        static final String LANDING_URL = "landing_url";
        static final String CTA = "cta";
        static final String RATING = "rating";

        // Constant keys
        static final String URL = "url";

        private final Context mContext;
        private final CustomEventNativeListener mCustomEventNativeListener;
        private final ImpressionTracker mImpressionTracker;
        private final NativeClickHandler mNativeClickHandler;
        private IMNative mImNative;

        InMobiStaticNativeAd(final Context context,
                final ImpressionTracker impressionTracker,
                final NativeClickHandler nativeClickHandler,
                final CustomEventNativeListener customEventNativeListener) {
            mContext = context.getApplicationContext();
            mImpressionTracker = impressionTracker;
            mNativeClickHandler = nativeClickHandler;
            mCustomEventNativeListener = customEventNativeListener;
        }

        void setIMNative(final IMNative imNative) {
            mImNative = imNative;
        }

        void loadAd() {
            mImNative.loadAd();
        }

        // IMNativeListener implementation
        @Override
        public void onNativeRequestSucceeded(final IMNative imNative) {
            if (imNative == null) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
                return;
            }

            try {
                parseJson(imNative);
            } catch (JSONException e) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
                return;
            }

            final List<String> imageUrls = new ArrayList<String>();
            final String mainImageUrl = getMainImageUrl();
            if (mainImageUrl != null) {
                imageUrls.add(mainImageUrl);
            }

            final String iconUrl = getIconImageUrl();
            if (iconUrl != null) {
                imageUrls.add(iconUrl);
            }

            preCacheImages(mContext, imageUrls, new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    mCustomEventNativeListener.onNativeAdLoaded(InMobiStaticNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });
        }

        @Override
        public void onNativeRequestFailed(final IMErrorCode errorCode) {
            if (errorCode == IMErrorCode.INVALID_REQUEST) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_REQUEST);
            } else if (errorCode == IMErrorCode.INTERNAL_ERROR || errorCode == IMErrorCode.NETWORK_ERROR) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
            } else if (errorCode == IMErrorCode.NO_FILL) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            } else {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            }
        }

        // Lifecycle Handlers
        @Override
        public void prepare(final View view) {
            if (view != null && view instanceof ViewGroup) {
                mImNative.attachToView((ViewGroup) view);
            } else if (view != null && view.getParent() instanceof ViewGroup) {
                mImNative.attachToView((ViewGroup) view.getParent());
            } else {
                Log.e("MoPub", "InMobi did not receive ViewGroup to attachToView, unable to record impressions");
            }
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
            mImNative.detachFromView();
            mImpressionTracker.destroy();
        }

        // Event Handlers
        @Override
        public void recordImpression(final View view) {
            notifyAdImpressed();
        }

        @Override
        public void handleClick(final View view) {
            notifyAdClicked();
            mNativeClickHandler.openClickDestinationUrl(getClickDestinationUrl(), view);
            mImNative.handleClick(null);
        }

        void parseJson(final IMNative imNative) throws JSONException  {
            final JSONTokener jsonTokener = new JSONTokener(imNative.getContent());
            final JSONObject jsonObject = new JSONObject(jsonTokener);

            setTitle(getJsonValue(jsonObject, TITLE, String.class));
            setText(getJsonValue(jsonObject, DESCRIPTION, String.class));

            final JSONObject screenShotJsonObject = getJsonValue(jsonObject, SCREENSHOTS, JSONObject.class);
            if (screenShotJsonObject != null) {
                setMainImageUrl(getJsonValue(screenShotJsonObject, URL, String.class));
            }

            final JSONObject iconJsonObject = getJsonValue(jsonObject, ICON, JSONObject.class);
            if (iconJsonObject != null) {
                setIconImageUrl(getJsonValue(iconJsonObject, URL, String.class));
            }

            final String clickDestinationUrl = getJsonValue(jsonObject, LANDING_URL, String.class);
            if (clickDestinationUrl == null) {
                final String errorMessage = "InMobi JSON response missing required key: "
                        + LANDING_URL + ". Failing over.";
                MoPubLog.d(errorMessage);
                throw new JSONException(errorMessage);
            }

            setClickDestinationUrl(clickDestinationUrl);
            setCallToAction(getJsonValue(jsonObject, CTA, String.class));

            try {
                setStarRating(parseDouble(jsonObject.opt(RATING)));
            } catch (ClassCastException e) {
                Log.d("MoPub", "Unable to set invalid star rating for InMobi Native.");
            }
            setImpressionMinTimeViewed(IMPRESSION_MIN_TIME_VIEWED);
        }

    }
}
