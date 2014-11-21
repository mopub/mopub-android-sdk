package com.mopub.nativeads;

import android.content.Context;

import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.util.Numbers.parseDouble;
import static com.mopub.nativeads.CustomEventNativeAdapter.RESPONSE_BODY_KEY;
import static com.mopub.nativeads.NativeResponse.Parameter;

public class MoPubCustomEventNative extends CustomEventNative {

    @Override
    protected void loadNativeAd(final Context context,
            final CustomEventNativeListener customEventNativeListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        final MoPubForwardingNativeAd moPubForwardingNativeAd =
                new MoPubForwardingNativeAd(context.getApplicationContext(),
                        serverExtras.get(RESPONSE_BODY_KEY),
                        customEventNativeListener);

        try {
            moPubForwardingNativeAd.loadAd();
        } catch (IllegalArgumentException e) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            return;
        } catch (JSONException e) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_JSON);
            return;
        }
    }

    static class MoPubForwardingNativeAd extends BaseForwardingNativeAd {
        private final Context mContext;
        private final String mJsonString;
        private final CustomEventNativeListener mCustomEventNativeListener;

        MoPubForwardingNativeAd(final Context context,
                final String jsonString,
                final CustomEventNativeListener customEventNativeListener) {
            mContext = context;
            mJsonString = jsonString;
            mCustomEventNativeListener = customEventNativeListener;
        }

        void loadAd() throws IllegalArgumentException, JSONException {
            if (mJsonString == null) {
                throw new IllegalArgumentException("Json String cannot be null");
            }

            final JSONTokener jsonTokener = new JSONTokener(mJsonString);
            final JSONObject jsonObject = new JSONObject(jsonTokener);

            if (!containsRequiredKeys(jsonObject)) {
                throw new IllegalArgumentException("JSONObject did not contain required keys.");
            }

            final Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final Parameter parameter = Parameter.from(key);

                if (parameter != null) {
                    try {
                        addInstanceVariable(parameter, jsonObject.opt(key));
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("JSONObject key (" + key + ") contained unexpected value.");
                    }
                } else {
                    addExtra(key, jsonObject.opt(key));
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

        private boolean containsRequiredKeys(final JSONObject jsonObject) {
            final Set<String> keys = new HashSet<String>();

            final Iterator<String> jsonKeys = jsonObject.keys();
            while (jsonKeys.hasNext()) {
                keys.add(jsonKeys.next());
            }

            return keys.containsAll(Parameter.requiredKeys);
        }

        private void addInstanceVariable(final Parameter key, final Object value) throws ClassCastException {
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

        private boolean isImageKey(final String name) {
            return name != null && name.toLowerCase().endsWith("image");
        }

        List<String> getExtrasImageUrls() {
            final List<String> extrasBitmapUrls = new ArrayList<String>(getExtras().size());
            for (final Map.Entry<String, Object> entry : getExtras().entrySet()) {
                if (isImageKey(entry.getKey()) && entry.getValue() instanceof String) {
                    extrasBitmapUrls.add((String) entry.getValue());
                }
            }

            return extrasBitmapUrls;
        }

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
