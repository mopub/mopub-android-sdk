package com.mopub.common;

import android.os.Build;
import android.support.annotation.NonNull;

import com.mopub.network.AdResponse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A value class used for generating reports to send data back to MoPub
 */
public class AdReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String DATE_FORMAT_PATTERN = "M/d/yy hh:mm:ss a z";
    private final AdResponse mAdResponse;
    private final String mAdUnitId;
    private final String mSdkVersion;
    private final String mDeviceModel;
    private final Locale mDeviceLocale;
    private final String mUdid;

    public AdReport(@NonNull String adUnitId, @NonNull ClientMetadata clientMetadata, @NonNull AdResponse adResponse) {
        mAdUnitId = adUnitId;
        mSdkVersion = clientMetadata.getSdkVersion();
        mDeviceModel = clientMetadata.getDeviceModel();
        mDeviceLocale = clientMetadata.getDeviceLocale();
        mUdid = clientMetadata.getDeviceId();
        mAdResponse = adResponse;
    }

    @Override
    public String toString() {
        StringBuilder parameters = new StringBuilder();
        appendKeyValue(parameters, "sdk_version", mSdkVersion);
        appendKeyValue(parameters, "creative_id", mAdResponse.getDspCreativeId());
        appendKeyValue(parameters, "platform_version", Integer.toString(Build.VERSION.SDK_INT));
        appendKeyValue(parameters, "device_model", mDeviceModel);
        appendKeyValue(parameters, "ad_unit_id", mAdUnitId);
        appendKeyValue(parameters, "device_locale",
                mDeviceLocale == null ? null : mDeviceLocale.toString());
        appendKeyValue(parameters, "device_id", mUdid);
        appendKeyValue(parameters, "network_type", mAdResponse.getNetworkType());
        appendKeyValue(parameters, "platform", "android");
        appendKeyValue(parameters, "timestamp", getFormattedTimeStamp(mAdResponse.getTimestamp()));
        appendKeyValue(parameters, "ad_type", mAdResponse.getAdType());
        Integer width = mAdResponse.getWidth();
        Integer height = mAdResponse.getHeight();
        appendKeyValue(parameters, "ad_size", "{"
                + (width == null ? "0" : width)
                + ", "
                + (height == null ? "0" : height)
                + "}");

        return parameters.toString();
    }

    public String getResponseString() {
        return mAdResponse.getStringBody();
    }

    public String getDspCreativeId() {
        return mAdResponse.getDspCreativeId();
    }

    private void appendKeyValue(StringBuilder parameters, String key, String value) {
        parameters.append(key);
        parameters.append(" : ");
        parameters.append(value);
        parameters.append("\n");
    }

    private String getFormattedTimeStamp(long timeStamp) {
        if (timeStamp != -1) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US);
            return dateFormat.format(new Date(timeStamp));
        } else {
            return null;
        }
    }
}
