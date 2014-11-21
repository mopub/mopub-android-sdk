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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mopub.common.util.Json.getJsonValue;
import static com.mopub.common.util.Numbers.parseDouble;

/*
 * Tested with InMobi SDK 4.4.1
 */
class InMobiNative extends CustomEventNative {
    private static final String APP_ID_KEY = "app_id";

    // CustomEventNative implementation
    @Override
    protected void loadNativeAd(final Context context,
            final CustomEventNativeListener customEventNativeListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        if (!(context instanceof Activity)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        final Activity activity = (Activity) context;

        final String appId;
        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(APP_ID_KEY);
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        InMobi.initialize(activity, appId);
        final InMobiForwardingNativeAd inMobiForwardingNativeAd =
                new InMobiForwardingNativeAd(context, customEventNativeListener);
        inMobiForwardingNativeAd.setIMNative(new IMNative(inMobiForwardingNativeAd));
        inMobiForwardingNativeAd.loadAd();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(APP_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    static class InMobiForwardingNativeAd extends BaseForwardingNativeAd implements IMNativeListener {
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
        private IMNative mImNative;

        InMobiForwardingNativeAd(final Context context,
                final CustomEventNativeListener customEventNativeListener) {
            mContext = context.getApplicationContext();
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
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_JSON);
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

            preCacheImages(mContext, imageUrls, new ImageListener() {
                @Override
                public void onImagesCached() {
                    mCustomEventNativeListener.onNativeAdLoaded(InMobiForwardingNativeAd.this);
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

        @Override
        public void prepare(final View view) {
            if (view != null && view instanceof ViewGroup) {
                mImNative.attachToView((ViewGroup) view);
            } else if (view != null && view.getParent() instanceof ViewGroup) {
                mImNative.attachToView((ViewGroup) view.getParent());
            } else {
                Log.e("MoPub", "InMobi did not receive ViewGroup to attachToView, unable to record impressions");
            }
        }

        @Override
        public void handleClick(final View view) {
            mImNative.handleClick(null);
        }

        @Override
        public void destroy() {
            mImNative.detachFromView();
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

            setClickDestinationUrl(getJsonValue(jsonObject, LANDING_URL, String.class));
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
