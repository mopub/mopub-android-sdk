package com.mopub.common.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.mopub.common.ClientMetadata.MoPubNetworkType;

public abstract class BaseEvent {

    public static enum ScribeCategory {
        EXCHANGE_CLIENT_EVENT("exchange_client_event"),
        EXCHANGE_CLIENT_ERROR("exchange_client_error");

        @NonNull private final String mScribeCategory;
        private ScribeCategory(@NonNull String scribeCategory) {
            mScribeCategory = scribeCategory;
        }

        @NonNull
        public String getCategory() {
            return mScribeCategory;
        }
    }

    public static enum SdkProduct {
        NONE(0),
        WEB_VIEW(1),
        NATIVE(2);

        private final int mType;
        private SdkProduct(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }
    }

    public static enum AppPlatform {
        NONE(0),
        IOS(1),
        ANDROID(2),
        MOBILE_WEB(3);

        private final int mType;
        private AppPlatform(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }
    }

    public enum Name {
        AD_REQUEST("ad_request"),
        IMPRESSION_REQUEST("impression_request"),
        CLICK_REQUEST("click_request");

        @NonNull private final String mName;
        private Name(@NonNull String name) {
            mName = name;
        }

        @NonNull
        public String getName() {
            return mName;
        }
    }

    public enum Category {
        REQUESTS("requests");

        @NonNull private final String mCategory;
        private Category(@NonNull String category) {
            mCategory = category;
        }

        @NonNull
        public String getCategory() {
            return mCategory;
        }
    }

    public enum SamplingRate {
        AD_REQUEST(0.1);

        private final double mSamplingRate;
        private SamplingRate(double samplingRate) {
            mSamplingRate = samplingRate;
        }

        public double getSamplingRate() {
            return mSamplingRate;
        }
    }

    @NonNull private final ScribeCategory mScribeCategory;
    @NonNull private final Name mName;
    @NonNull private final Category mCategory;
    @Nullable private final SdkProduct mSdkProduct;
    @Nullable private final String mAdUnitId;
    @Nullable private final String mAdCreativeId;
    @Nullable private final String mAdType;
    @Nullable private final String mAdNetworkType;
    @Nullable private final Double mAdWidthPx;
    @Nullable private final Double mAdHeightPx;
    @Nullable private final Integer mDeviceScreenWidthDip;
    @Nullable private final Integer mDeviceScreenHeightDip;
    @Nullable private final Double mGeoLat;
    @Nullable private final Double mGeoLon;
    @Nullable private final Double mGeoAccuracy;
    @Nullable private final MoPubNetworkType mNetworkType;
    @Nullable private final String mNetworkOperator;
    @Nullable private final String mNetworkOperatorName;
    @Nullable private final String mIsoCountryCode;
    @Nullable private final String mSimOperator;
    @Nullable private final String mSimOperatorName;
    @Nullable private final String mSimIsoCountryCode;
    @Nullable private final Double mPerformanceDurationMs;
    @Nullable private final String mRequestId;
    @Nullable private final Integer mRequestStatusCode;
    @Nullable private final String mRequestUri;
    @Nullable private final Integer mRequestRetries;
    private final long mTimestampUtcMs;
    @Nullable private ClientMetadata mClientMetaData;

     /**
     * The percentage of events, in range 0 - 1.0, to be logged.
     */
    private final double mSamplingRate;

    public BaseEvent(@NonNull final Builder builder) {
        Preconditions.checkNotNull(builder);

        mScribeCategory = builder.mScribeCategory;
        mName = builder.mName;
        mCategory = builder.mCategory;
        mSdkProduct = builder.mSdkProduct;
        mAdUnitId = builder.mAdUnitId;
        mAdCreativeId = builder.mAdCreativeId;
        mAdType = builder.mAdType;
        mAdNetworkType = builder.mAdNetworkType;
        mAdWidthPx = builder.mAdWidthPx;
        mAdHeightPx = builder.mAdHeightPx;
        mGeoLat = builder.mGeoLat;
        mGeoLon = builder.mGeoLon;
        mGeoAccuracy = builder.mGeoAccuracy;
        mPerformanceDurationMs = builder.mPerformanceDurationMs;
        mRequestId = builder.mRequestId;
        mRequestStatusCode = builder.mRequestStatusCode;
        mRequestUri = builder.mRequestUri;
        mRequestRetries = builder.mRequestRetries;
        mSamplingRate = builder.mSamplingRate;
        mTimestampUtcMs = System.currentTimeMillis();

        mClientMetaData = ClientMetadata.getInstance();
        if (mClientMetaData != null) {
            mDeviceScreenWidthDip = mClientMetaData.getDeviceScreenWidthDip();
            mDeviceScreenHeightDip = mClientMetaData.getDeviceScreenHeightDip();
            mNetworkType = mClientMetaData.getActiveNetworkType();
            mNetworkOperator = mClientMetaData.getNetworkOperator();
            mNetworkOperatorName = mClientMetaData.getNetworkOperatorName();
            mIsoCountryCode = mClientMetaData.getIsoCountryCode();
            mSimOperator = mClientMetaData.getSimOperator();
            mSimOperatorName = mClientMetaData.getSimOperatorName();
            mSimIsoCountryCode = mClientMetaData.getSimIsoCountryCode();
        } else {
            // Need to silence warnings about variables not being initialized
            mDeviceScreenWidthDip = null;
            mDeviceScreenHeightDip = null;
            mNetworkType = null;
            mNetworkOperator = null;
            mNetworkOperatorName = null;
            mIsoCountryCode = null;
            mSimOperator = null;
            mSimOperatorName = null;
            mSimIsoCountryCode = null;
        }
    }

    @NonNull
    public ScribeCategory getScribeCategory() {
        return mScribeCategory;
    }

    @NonNull
    public Name getName() {
        return mName;
    }

    @NonNull
    public Category getCategory() {
        return mCategory;
    }

    @Nullable
    public SdkProduct getSdkProduct() {
        return mSdkProduct;
    }

    @Nullable
    public String getSdkVersion() {
        return mClientMetaData == null ? null : mClientMetaData.getSdkVersion();
    }

    @Nullable
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @Nullable
    public String getAdCreativeId() {
        return mAdCreativeId;
    }

    @Nullable
    public String getAdType() {
        return mAdType;
    }

    @Nullable
    public String getAdNetworkType() {
        return mAdNetworkType;
    }

    @Nullable
    public Double getAdWidthPx() {
        return mAdWidthPx;
    }

    @Nullable
    public Double getAdHeightPx() {
        return mAdHeightPx;
    }

    @Nullable
    public AppPlatform getAppPlatform() {
        return AppPlatform.ANDROID;
    }

    @Nullable
    public String getAppName() {
        return mClientMetaData == null ? null : mClientMetaData.getAppName();
    }

    @Nullable
    public String getAppPackageName() {
        return mClientMetaData == null ? null : mClientMetaData.getAppPackageName();
    }

    @Nullable
    public String getAppVersion() {
        return mClientMetaData == null ? null : mClientMetaData.getAppVersion();
    }

    @Nullable
    public String getClientAdvertisingId() {
        return mClientMetaData == null ? null : mClientMetaData.getDeviceId();
    }

    @NonNull
    public String getObfuscatedClientAdvertisingId() {
        // This is a placeholder for the advertising id until we approve a plan to use the
        // real value
        return "ifa:XXXX";
    }

    @NonNull
    public Boolean getClientDoNotTrack() {
        // Default to true if we don't have access to the client meta data
        return mClientMetaData == null || mClientMetaData.isDoNotTrackSet();
    }

    @Nullable
    public String getDeviceManufacturer() {
        return mClientMetaData == null ? null : mClientMetaData.getDeviceManufacturer();
    }

    @Nullable
    public String getDeviceModel() {
        return mClientMetaData == null ? null : mClientMetaData.getDeviceModel();
    }

    @Nullable
    public String getDeviceProduct() {
        return mClientMetaData == null ? null : mClientMetaData.getDeviceProduct();
    }

    @Nullable
    public String getDeviceOsVersion() {
        return mClientMetaData == null ? null : mClientMetaData.getDeviceOsVersion();
    }

    @Nullable
    public Integer getDeviceScreenWidthDip() {
        return mDeviceScreenWidthDip;
    }

    @Nullable
    public Integer getDeviceScreenHeightDip() {
        return mDeviceScreenHeightDip;
    }

    @Nullable
    public Double getGeoLat() {
        return mGeoLat;
    }

    @Nullable
    public Double getGeoLon() {
        return mGeoLon;
    }

    @Nullable
    public Double getGeoAccuracy() {
        return mGeoAccuracy;
    }

    @Nullable
    public Double getPerformanceDurationMs() {
        return mPerformanceDurationMs;
    }

    @Nullable
    public MoPubNetworkType getNetworkType() {
        return mNetworkType;
    }

    @Nullable
    public String getNetworkOperatorCode() {
        return mNetworkOperator;
    }

    @Nullable
    public String getNetworkOperatorName() {
        return mNetworkOperatorName;
    }

    @Nullable
    public String getNetworkIsoCountryCode() {
        return mIsoCountryCode;
    }

    @Nullable
    public String getNetworkSimCode() {
        return mSimOperator;
    }

    @Nullable
    public String getNetworkSimOperatorName() {
        return mSimOperatorName;
    }

    @Nullable
    public String getNetworkSimIsoCountryCode() {
        return mSimIsoCountryCode;
    }

    @Nullable
    public String getRequestId() {
        return mRequestId;
    }

    @Nullable
    public Integer getRequestStatusCode() {
        return mRequestStatusCode;
    }

    @Nullable
    public String getRequestUri() {
        return mRequestUri;
    }

    @Nullable
    public Integer getRequestRetries() {
        return mRequestRetries;
    }

    public double getSamplingRate() {
        return mSamplingRate;
    }

    @NonNull
    public Long getTimestampUtcMs() {
        return mTimestampUtcMs;
    }

    @Override
    public String toString() {
        return  "BaseEvent\n" +
                "ScribeCategory: " + getScribeCategory() + "\n" +
                "Name: " + getName() + "\n" +
                "Category: " + getCategory() + "\n" +
                "SdkProduct: " + getSdkProduct() + "\n" +
                "SdkVersion: " + getSdkVersion() + "\n" +
                "AdUnitId: " + getAdUnitId() + "\n" +
                "AdCreativeId: " + getAdCreativeId() + "\n" +
                "AdType: " + getAdType() + "\n" +
                "AdNetworkType: " + getAdNetworkType() + "\n" +
                "AdWidthPx: " + getAdWidthPx() + "\n" +
                "AdHeightPx: " + getAdHeightPx() + "\n" +
                "AppPlatform: " + getAppPlatform() + "\n" +
                "AppName: " + getAppName() + "\n" +
                "AppPackageName: " + getAppPackageName() + "\n" +
                "AppVersion: " + getAppVersion() + "\n" +
                "DeviceManufacturer: " + getDeviceManufacturer() + "\n" +
                "DeviceModel: " + getDeviceModel() + "\n" +
                "DeviceProduct: " + getDeviceProduct() + "\n" +
                "DeviceOsVersion: " + getDeviceOsVersion() + "\n" +
                "DeviceScreenWidth: " + getDeviceScreenWidthDip() + "\n" +
                "DeviceScreenHeight: " + getDeviceScreenHeightDip() + "\n" +
                "GeoLat: " + getGeoLat() + "\n" +
                "GeoLon: " + getGeoLon() + "\n" +
                "GeoAccuracy: " + getGeoAccuracy() + "\n" +
                "PerformanceDurationMs: " + getPerformanceDurationMs() + "\n" +
                "NetworkType: " + getNetworkType() + "\n" +
                "NetworkOperatorCode: " + getNetworkOperatorCode() + "\n" +
                "NetworkOperatorName: " + getNetworkOperatorName() + "\n" +
                "NetworkIsoCountryCode: " + getNetworkIsoCountryCode() + "\n" +
                "NetworkSimCode: " + getNetworkSimCode() + "\n" +
                "NetworkSimOperatorName: " + getNetworkSimOperatorName() + "\n" +
                "NetworkSimIsoCountryCode: " + getNetworkSimIsoCountryCode() + "\n" +
                "RequestId: " + getRequestId() + "\n" +
                "RequestStatusCode: " + getRequestStatusCode() + "\n" +
                "RequestUri: " + getRequestUri() + "\n" +
                "RequestRetries: " + getRequestRetries() + "\n" +
                "SamplingRate: " + getSamplingRate() + "\n" +
                "TimestampUtcMs: " + new SimpleDateFormat().format(new Date(getTimestampUtcMs())) + "\n";
    }

    public static abstract class Builder {
        @NonNull private ScribeCategory mScribeCategory;
        @NonNull private Name mName;
        @NonNull private Category mCategory;
        @Nullable private SdkProduct mSdkProduct;
        @Nullable private String mAdUnitId;
        @Nullable private String mAdCreativeId;
        @Nullable private String mAdType;
        @Nullable private String mAdNetworkType;
        @Nullable private Double mAdWidthPx;
        @Nullable private Double mAdHeightPx;
        @Nullable private Double mGeoLat;
        @Nullable private Double mGeoLon;
        @Nullable private Double mGeoAccuracy;
        @Nullable private Double mPerformanceDurationMs;
        @Nullable private String mRequestId;
        @Nullable private Integer mRequestStatusCode;
        @Nullable private String mRequestUri;
        @Nullable private Integer mRequestRetries;

        /**
         * The percentage of events, in range 0 - 1.0, to be logged.
         */
        private double mSamplingRate;

        public Builder(@NonNull ScribeCategory scribeCategory,
                @NonNull Name name,
                @NonNull Category category,
                double samplingRate) {
            Preconditions.checkNotNull(scribeCategory);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(category);
            Preconditions.checkArgument(samplingRate >= 0 && samplingRate <= 1.0);

            mScribeCategory = scribeCategory;
            mName = name;
            mCategory = category;
            mSamplingRate = samplingRate;
        }

        @NonNull
        public Builder withSdkProduct(@Nullable SdkProduct sdkProduct) {
            mSdkProduct = sdkProduct;
            return this;
        }

        @NonNull
        public Builder withAdUnitId(@Nullable String adUnitId) {
            mAdUnitId = adUnitId;
            return this;
        }

        @NonNull
        public Builder withAdCreativeId(@Nullable String adCreativeId) {
            mAdCreativeId = adCreativeId;
            return this;
        }

        @NonNull
        public Builder withAdType(@Nullable String adType) {
            mAdType = adType;
            return this;
        }

        @NonNull
        public Builder withAdNetworkType(@Nullable String adNetworkType) {
            mAdNetworkType = adNetworkType;
            return this;
        }

        @NonNull
        public Builder withAdWidthPx(@Nullable Double adWidthPx) {
            mAdWidthPx = adWidthPx;
            return this;
        }

        @NonNull
        public Builder withAdHeightPx(@Nullable Double adHeightPx) {
            mAdHeightPx = adHeightPx;
            return this;
        }

        @NonNull
        public Builder withGeoLat(@Nullable Double geoLat) {
            mGeoLat = geoLat;
            return this;
        }

        @NonNull
        public Builder withGeoLon(@Nullable Double geoLon) {
            mGeoLon = geoLon;
            return this;
        }

        @NonNull
        public Builder withGeoAccuracy(@Nullable Double geoAccuracy) {
            mGeoAccuracy = geoAccuracy;
            return this;
        }

        @NonNull
        public Builder withPerformanceDurationMs(@Nullable Double performanceDurationMs) {
            mPerformanceDurationMs = performanceDurationMs;
            return this;
        }

        @NonNull
        public Builder withRequestId(@Nullable String requestId) {
            mRequestId = requestId;
            return this;
        }

        @NonNull
        public Builder withRequestStatusCode(@Nullable Integer requestStatusCode) {
            mRequestStatusCode = requestStatusCode;
            return this;
        }

        @NonNull
        public Builder withRequestUri(@Nullable String requestUri) {
            mRequestUri = requestUri;
            return this;
        }

        @NonNull
        public Builder withRequestRetries(@Nullable Integer requestRetries) {
            mRequestRetries = requestRetries;
            return this;
        }

        public abstract BaseEvent build();
    }
}
