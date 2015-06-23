package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.event.ErrorEvent;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.util.Numbers.parseDouble;
import static com.mopub.common.DataKeys.JSON_BODY_KEY;
import static com.mopub.nativeads.NativeResponse.Parameter;

public class MoPubCustomEventNative extends CustomEventNative {

    @Override
    protected void loadNativeAd(@NonNull final Context context,
            @NonNull final CustomEventNativeListener customEventNativeListener,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) {

        Object json = localExtras.get(JSON_BODY_KEY);
        // null or non-JSONObjects should not be passed in localExtras as JSON_BODY_KEY
        if (!(json instanceof JSONObject)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_JSON);
            return;
        }

        final MoPubForwardingNativeAd moPubForwardingNativeAd =
                new MoPubForwardingNativeAd(context.getApplicationContext(),
                        (JSONObject) json,
                        customEventNativeListener);

        try {
            moPubForwardingNativeAd.loadAd();
        } catch (IllegalArgumentException e) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }
    }

    static class MoPubForwardingNativeAd extends BaseForwardingNativeAd {
        @NonNull private final Context mContext;
        @NonNull private final CustomEventNativeListener mCustomEventNativeListener;
        @NonNull private final JSONObject mJsonObject;

        MoPubForwardingNativeAd(@NonNull final Context context,
                @NonNull final JSONObject jsonBody,
                @NonNull final CustomEventNativeListener customEventNativeListener) {
            mJsonObject = jsonBody;
            mContext = context;
            mCustomEventNativeListener = customEventNativeListener;
        }

        void loadAd() throws IllegalArgumentException {
            if (!containsRequiredKeys(mJsonObject)) {
                throw new IllegalArgumentException("JSONObject did not contain required keys.");
            }

            final Iterator<String> keys = mJsonObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final Parameter parameter = Parameter.from(key);

                if (parameter != null) {
                    try {
                        addInstanceVariable(parameter, mJsonObject.opt(key));
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("JSONObject key (" + key + ") contained unexpected value.");
                    }
                } else {
                    addExtra(key, mJsonObject.opt(key));
                }
            }

            preCacheImages(mContext, getAllImageUrls(), new ImageListener() {
                @Override
                public void onImagesCached() {
                    mCustomEventNativeListener.onNativeAdLoaded(MoPubForwardingNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(final NativeErrorCode errorCode) {
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });
        }

        private boolean containsRequiredKeys(@NonNull final JSONObject jsonObject) {
            final Set<String> keys = new HashSet<String>();
            final Iterator<String> jsonKeys = jsonObject.keys();
            while (jsonKeys.hasNext()) {
                keys.add(jsonKeys.next());
            }

            return keys.containsAll(Parameter.requiredKeys);
        }

        private void addInstanceVariable(@NonNull final Parameter key,
                @Nullable final Object value) throws ClassCastException {
            try {
                switch (key) {
                    case MAIN_IMAGE:
                        setMainImageUrl((String) value);
                        break;
                    case ICON_IMAGE:
                        setIconImageUrl((String) value);
                        break;
                    case IMPRESSION_TRACKER:
                        addImpressionTrackers(value);
                        break;
                    case CLICK_TRACKER:
                        break;
                    case CLICK_DESTINATION:
                        setClickDestinationUrl((String) value);
                        break;
                    case CALL_TO_ACTION:
                        setCallToAction((String) value);
                        break;
                    case TITLE:
                        setTitle((String) value);
                        break;
                    case TEXT:
                        setText((String) value);
                        break;
                    case STAR_RATING:
                        setStarRating(parseDouble(value));
                        break;
                    default:
                        MoPubLog.d("Unable to add JSON key to internal mapping: " + key.name);
                        break;
                }
            } catch (ClassCastException e) {
                if (!key.required) {
                    MoPubLog.d("Ignoring class cast exception for optional key: " + key.name);
                } else {
                    throw e;
                }
            }
        }

        private void addImpressionTrackers(final Object impressionTrackers) throws ClassCastException {
            if (!(impressionTrackers instanceof JSONArray)) {
                throw new ClassCastException("Expected impression trackers of type JSONArray.");
            }

            final JSONArray trackers = (JSONArray) impressionTrackers;
            for (int i = 0; i < trackers.length(); i++) {
                try {
                    addImpressionTracker(trackers.getString(i));
                } catch (JSONException e) {
                    // This will only occur if we access a non-existent index in JSONArray.
                    MoPubLog.d("Unable to parse impression trackers.");
                }
            }
        }

        private boolean isImageKey(@Nullable final String name) {
            return name != null && name.toLowerCase(Locale.US).endsWith("image");
        }

        @NonNull
        List<String> getExtrasImageUrls() {
            final List<String> extrasBitmapUrls = new ArrayList<String>(getExtras().size());
            for (final Map.Entry<String, Object> entry : getExtras().entrySet()) {
                if (isImageKey(entry.getKey()) && entry.getValue() instanceof String) {
                    extrasBitmapUrls.add((String) entry.getValue());
                }
            }

            return extrasBitmapUrls;
        }

        @NonNull
        List<String> getAllImageUrls() {
            final List<String> imageUrls = new ArrayList<String>();
            if (getMainImageUrl() != null) {
                imageUrls.add(getMainImageUrl());
            }
            if (getIconImageUrl() != null) {
                imageUrls.add(getIconImageUrl());
            }

            imageUrls.addAll(getExtrasImageUrls());
            return imageUrls;
        }
    }
}
