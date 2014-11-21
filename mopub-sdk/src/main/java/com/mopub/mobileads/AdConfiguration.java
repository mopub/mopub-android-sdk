package com.mopub.mobileads;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.webkit.WebView;
import com.mopub.common.MoPub;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.Utils;
import com.mopub.common.util.VersionCode;
import org.apache.http.HttpResponse;

import java.io.Serializable;
import java.util.Map;

import static com.mopub.common.util.ResponseHeader.AD_TIMEOUT;
import static com.mopub.common.util.ResponseHeader.AD_TYPE;
import static com.mopub.common.util.ResponseHeader.CLICKTHROUGH_URL;
import static com.mopub.common.util.ResponseHeader.DSP_CREATIVE_ID;
import static com.mopub.common.util.ResponseHeader.FAIL_URL;
import static com.mopub.common.util.ResponseHeader.HEIGHT;
import static com.mopub.common.util.ResponseHeader.IMPRESSION_URL;
import static com.mopub.common.util.ResponseHeader.NETWORK_TYPE;
import static com.mopub.common.util.ResponseHeader.REDIRECT_URL;
import static com.mopub.common.util.ResponseHeader.REFRESH_TIME;
import static com.mopub.common.util.ResponseHeader.WIDTH;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.util.HttpResponses.extractHeader;
import static com.mopub.mobileads.util.HttpResponses.extractIntHeader;
import static com.mopub.mobileads.util.HttpResponses.extractIntegerHeader;

public class AdConfiguration implements Serializable {
    private static final long serialVersionUID = 0L;

    private static final int MINIMUM_REFRESH_TIME_MILLISECONDS = 10000;
    private static final int DEFAULT_REFRESH_TIME_MILLISECONDS = 60000;
    private static final String mPlatform = "Android";
    private final String mSdkVersion;

    private final String mHashedUdid;
    private final String mUserAgent;
    private final String mDeviceLocale;
    private final String mDeviceModel;
    private final int mPlatformVersion;

    private long mBroadcastIdentifier;
    private String mResponseString;
    private String mAdUnitId;

    private String mAdType;
    private String mNetworkType;
    private String mRedirectUrl;
    private String mClickthroughUrl;
    private String mFailUrl;
    private String mImpressionUrl;
    private long mTimeStamp;
    private int mWidth;
    private int mHeight;
    private Integer mAdTimeoutDelay;
    private int mRefreshTimeMilliseconds;
    private String mDspCreativeId;

    static AdConfiguration extractFromMap(Map<String,Object> map) {
        if (map == null) {
            return null;
        }

        Object adConfiguration = map.get(AD_CONFIGURATION_KEY);

        if (adConfiguration instanceof AdConfiguration) {
            return (AdConfiguration) adConfiguration;
        }

        return null;
    }

    AdConfiguration(final Context context) {
        setDefaults();

        if (context != null) {
            String udid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            mHashedUdid = Utils.sha1((udid != null) ? udid : "");

            mUserAgent = new WebView(context).getSettings().getUserAgentString();
            mDeviceLocale = context.getResources().getConfiguration().locale.toString();
        } else {
            mHashedUdid = null;
            mUserAgent = null;
            mDeviceLocale = null;
        }

        mBroadcastIdentifier = Utils.generateUniqueId();
        mDeviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        mPlatformVersion = VersionCode.currentApiLevel().getApiLevel();
        mSdkVersion = MoPub.SDK_VERSION;
    }

    void cleanup() {
        setDefaults();
    }

    void addHttpResponse(final HttpResponse httpResponse) {
        mAdType = extractHeader(httpResponse, AD_TYPE);

        // Set the network type of the ad.
        mNetworkType = extractHeader(httpResponse, NETWORK_TYPE);

        // Set the redirect URL prefix: navigating to any matching URLs will send us to the browser.
        mRedirectUrl = extractHeader(httpResponse, REDIRECT_URL);

        // Set the URL that is prepended to links for click-tracking purposes.
        mClickthroughUrl = extractHeader(httpResponse, CLICKTHROUGH_URL);

        // Set the fall-back URL to be used if the current request fails.
        mFailUrl = extractHeader(httpResponse, FAIL_URL);

        // Set the URL to be used for impression tracking.
        mImpressionUrl = extractHeader(httpResponse, IMPRESSION_URL);

        // Set the timestamp used for Ad Alert Reporting.
        mTimeStamp = DateAndTime.now().getTime();

        // Set the width and height.
        mWidth = extractIntHeader(httpResponse, WIDTH, 0);
        mHeight = extractIntHeader(httpResponse, HEIGHT, 0);

        // Set the allowable amount of time an ad has before it automatically fails.
        mAdTimeoutDelay = extractIntegerHeader(httpResponse, AD_TIMEOUT);

        // Set the auto-refresh time. A timer will be scheduled upon ad success or failure.
        if (!httpResponse.containsHeader(REFRESH_TIME.getKey())) {
            mRefreshTimeMilliseconds = 0;
        } else {
            mRefreshTimeMilliseconds = extractIntHeader(httpResponse, REFRESH_TIME, 0) * 1000;
            mRefreshTimeMilliseconds = Math.max(
                    mRefreshTimeMilliseconds,
                    MINIMUM_REFRESH_TIME_MILLISECONDS);
        }

        // Set the unique identifier for the creative that was returned.
        mDspCreativeId = extractHeader(httpResponse, DSP_CREATIVE_ID);
    }

    /*
     * MoPubView
     */

    String getAdUnitId() {
        return mAdUnitId;
    }

    void setAdUnitId(String adUnitId) {
        mAdUnitId = adUnitId;
    }

    String getResponseString() {
        return mResponseString;
    }

    void setResponseString(String responseString) {
        mResponseString = responseString;
    }

    long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    /*
     * HttpResponse
     */

    String getAdType() {
        return mAdType;
    }

    String getNetworkType() {
        return mNetworkType;
    }

    String getRedirectUrl() {
        return mRedirectUrl;
    }

    String getClickthroughUrl() {
        return mClickthroughUrl;
    }

    @Deprecated
    void setClickthroughUrl(String clickthroughUrl) {
        mClickthroughUrl = clickthroughUrl;
    }

    String getFailUrl() {
        return mFailUrl;
    }

    void setFailUrl(String failUrl) {
        mFailUrl = failUrl;
    }

    String getImpressionUrl() {
        return mImpressionUrl;
    }

    long getTimeStamp() {
        return mTimeStamp;
    }

    int getWidth() {
        return mWidth;
    }

    int getHeight() {
        return mHeight;
    }

    Integer getAdTimeoutDelay() {
        return mAdTimeoutDelay;
    }

    int getRefreshTimeMilliseconds() {
        return mRefreshTimeMilliseconds;
    }

    @Deprecated
    void setRefreshTimeMilliseconds(int refreshTimeMilliseconds) {
        mRefreshTimeMilliseconds = refreshTimeMilliseconds;
    }

    String getDspCreativeId() {
        return mDspCreativeId;
    }

    /*
     * Context
     */

    String getHashedUdid() {
        return mHashedUdid;
    }

    String getUserAgent() {
        return mUserAgent;
    }

    String getDeviceLocale() {
        return mDeviceLocale;
    }

    String getDeviceModel() {
        return mDeviceModel;
    }

    int getPlatformVersion() {
        return mPlatformVersion;
    }

    String getPlatform() {
        return mPlatform;
    }

    /*
     * Misc.
     */

    String getSdkVersion() {
        return mSdkVersion;
    }

    private void setDefaults() {
        mBroadcastIdentifier = 0;
        mAdUnitId = null;
        mResponseString = null;
        mAdType = null;
        mNetworkType = null;
        mRedirectUrl = null;
        mClickthroughUrl = null;
        mImpressionUrl = null;
        mTimeStamp = DateAndTime.now().getTime();
        mWidth = 0;
        mHeight = 0;
        mAdTimeoutDelay = null;
        mRefreshTimeMilliseconds = DEFAULT_REFRESH_TIME_MILLISECONDS;
        mFailUrl = null;
        mDspCreativeId = null;
    }
}
