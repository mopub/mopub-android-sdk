package com.mopub.common.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.util.Json;

import java.util.HashMap;
import java.util.Map;

public class EventDetails {

    public static class Builder {
        @NonNull private final Map<String, String> eventDetailsMap;

        public Builder() {
            eventDetailsMap = new HashMap<String, String>();
        }

        @NonNull
        public Builder adUnitId(@Nullable final String adUnitId) {
            if (adUnitId != null) {
                eventDetailsMap.put(AD_UNIT_ID_KEY, adUnitId);
            }
            return this;
        }

        @NonNull
        public Builder dspCreativeId(@Nullable final String dspCreativeId) {
            if (dspCreativeId != null) {
                eventDetailsMap.put(DSP_CREATIVE_ID_KEY, dspCreativeId);
            }
            return this;
        }

        @NonNull
        public Builder adType(@Nullable final String adType) {
            if (adType != null) {
                eventDetailsMap.put(AD_TYPE_KEY, adType);
            }
            return this;
        }

        @NonNull
        public Builder adNetworkType(@Nullable final String adNetworkType) {
            if (adNetworkType != null) {
                eventDetailsMap.put(AD_NETWORK_TYPE_KEY, adNetworkType);
            }
            return this;
        }

        @NonNull
        public Builder adWidthPx(@Nullable final Integer adWidthPx) {
            if (adWidthPx != null) {
                eventDetailsMap.put(AD_WIDTH_PX_KEY, String.valueOf(adWidthPx));
            }
            return this;
        }

        @NonNull
        public Builder adHeightPx(@Nullable final Integer adHeightPx) {
            if (adHeightPx != null) {
                eventDetailsMap.put(AD_HEIGHT_PX_KEY, String.valueOf(adHeightPx));
            }
            return this;
        }

        @NonNull
        public Builder geoLatitude(@Nullable final Double geoLatitude) {
            if (geoLatitude != null) {
                eventDetailsMap.put(GEO_LATITUDE_KEY, String.valueOf(geoLatitude));
            }
            return this;
        }

        @NonNull
        public Builder geoLongitude(@Nullable final Double geoLongitude) {
            if (geoLongitude != null) {
                eventDetailsMap.put(GEO_LONGITUDE_KEY, String.valueOf(geoLongitude));
            }
            return this;
        }

        @NonNull
        public Builder geoAccuracy(@Nullable final Float geoAccuracy) {
            if (geoAccuracy != null) {
                eventDetailsMap.put(GEO_ACCURACY_KEY, String.valueOf((double) geoAccuracy));
            }
            return this;
        }

        @NonNull
        public Builder performanceDurationMs(@Nullable final Long performanceDurationMs) {
            if (performanceDurationMs != null) {
                eventDetailsMap.put(PERFORMANCE_DURATION_MS_KEY,
                        String.valueOf((double) performanceDurationMs));
            }
            return this;
        }

        @NonNull
        public Builder requestId(@Nullable final String requestId) {
            if (requestId != null) {
                eventDetailsMap.put(REQUEST_ID_KEY, requestId);
            }
            return this;
        }

        @NonNull
        public Builder requestStatusCode(@Nullable final Integer requestStatusCode) {
            if (requestStatusCode != null) {
                eventDetailsMap.put(REQUEST_STATUS_CODE_KEY, String.valueOf(requestStatusCode));
            }
            return this;
        }

        @NonNull
        public Builder requestUri(@Nullable final String requestUri) {
            if (requestUri != null) {
                eventDetailsMap.put(REQUEST_URI_KEY, requestUri);
            }
            return this;
        }

        @NonNull
        public EventDetails build() {
            return new EventDetails(eventDetailsMap);
        }
    }


    private static final String AD_UNIT_ID_KEY = "ad_unit_id";
    private static final String DSP_CREATIVE_ID_KEY = "dsp_creative_id";
    private static final String AD_TYPE_KEY = "ad_type";
    private static final String AD_NETWORK_TYPE_KEY = "ad_network_type";
    private static final String AD_WIDTH_PX_KEY = "ad_width_px";
    private static final String AD_HEIGHT_PX_KEY = "ad_height_px_key";
    private static final String GEO_LATITUDE_KEY = "geo_latitude";
    private static final String GEO_LONGITUDE_KEY = "geo_longitude";
    private static final String GEO_ACCURACY_KEY = "geo_accuracy_key";
    private static final String PERFORMANCE_DURATION_MS_KEY = "performance_duration_ms";
    private static final String REQUEST_ID_KEY = "request_id_key";
    private static final String REQUEST_STATUS_CODE_KEY = "request_status_code";
    private static final String REQUEST_URI_KEY = "request_uri_key";

    @NonNull private final Map<String, String> mEventDetailsMap;

    private EventDetails(@NonNull final Map<String, String> eventDetailsMap) {
        Preconditions.checkNotNull(eventDetailsMap);
        mEventDetailsMap = eventDetailsMap;
    }

    @Nullable
    public String getAdUnitId() {
        return mEventDetailsMap.get(AD_UNIT_ID_KEY);
    }

    @Nullable
    public String getDspCreativeId() {
        return mEventDetailsMap.get(DSP_CREATIVE_ID_KEY);
    }

    @Nullable
    public String getAdType() {
        return mEventDetailsMap.get(AD_TYPE_KEY);
    }

    @Nullable
    public String getAdNetworkType() {
        return mEventDetailsMap.get(AD_NETWORK_TYPE_KEY);
    }

    @Nullable
    public Double getAdWidthPx() {
        return getNullableDoubleValue(mEventDetailsMap, AD_WIDTH_PX_KEY);
    }

    @Nullable
    public Double getAdHeightPx() {
        return getNullableDoubleValue(mEventDetailsMap, AD_HEIGHT_PX_KEY);

    }

    @Nullable
    public Double getGeoLatitude() {
        return getNullableDoubleValue(mEventDetailsMap, GEO_LATITUDE_KEY);
    }

    @Nullable
    public Double getGeoLongitude() {
        return getNullableDoubleValue(mEventDetailsMap, GEO_LONGITUDE_KEY);
    }

    @Nullable
    public Double getGeoAccuracy() {
        return getNullableDoubleValue(mEventDetailsMap, GEO_ACCURACY_KEY);
    }

    @Nullable
    public Double getPerformanceDurationMs() {
        return getNullableDoubleValue(mEventDetailsMap, PERFORMANCE_DURATION_MS_KEY);
    }

    @Nullable
    public String getRequestId() {
        return mEventDetailsMap.get(REQUEST_ID_KEY);
    }

    @Nullable
    public Integer getRequestStatusCode() {
        return getNullableIntegerValue(mEventDetailsMap, REQUEST_STATUS_CODE_KEY);
    }

    @Nullable
    public String getRequestUri() {
        return mEventDetailsMap.get(REQUEST_URI_KEY);
    }

    public String toJsonString() {
        return Json.mapToJsonString(mEventDetailsMap);
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @Nullable
    private static Double getNullableDoubleValue(@NonNull final Map<String, String> map,
            @NonNull final String key) {
        final String value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Integer getNullableIntegerValue(@NonNull final Map<String, String> map,
            @NonNull final String key) {
        final String value = map.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
