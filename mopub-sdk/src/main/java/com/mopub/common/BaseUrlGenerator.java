package com.mopub.common;

import android.graphics.Point;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

public abstract class BaseUrlGenerator {

    private static final String WIDTH_KEY = "w";
    private static final String HEIGHT_KEY = "h";

    private StringBuilder mStringBuilder;
    private boolean mFirstParam;

    public abstract String generateUrlString(String serverHostname);

    protected void initUrlString(String serverHostname, String handlerType) {
        mStringBuilder = new StringBuilder(Networking.getScheme()).append("://")
                .append(serverHostname).append(handlerType);
        mFirstParam = true;
    }

    protected String getFinalUrlString() {
        return mStringBuilder.toString();
    }

    protected void addParam(String key, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        mStringBuilder.append(getParamDelimiter());
        mStringBuilder.append(key);
        mStringBuilder.append("=");
        mStringBuilder.append(Uri.encode(value));
    }

    private String getParamDelimiter() {
        if (mFirstParam) {
            mFirstParam = false;
            return "?";
        }
        return "&";
    }

    protected void setApiVersion(String apiVersion) {
        addParam("v", apiVersion);
    }

    protected void setAppVersion(String appVersion) {
        addParam("av", appVersion);
    }

    protected void setExternalStoragePermission(boolean isExternalStoragePermissionGranted) {
        addParam("android_perms_ext_storage", isExternalStoragePermissionGranted ? "1" : "0");
    }

    protected void setDeviceInfo(String... info) {
        StringBuilder result = new StringBuilder();
        if (info == null || info.length < 1) {
            return;
        }

        for (int i=0; i<info.length-1; i++) {
            result.append(info[i]).append(",");
        }
        result.append(info[info.length-1]);

        addParam("dn", result.toString());
    }

    protected void setDoNotTrack(boolean dnt) {
        if (dnt) {
            addParam("dnt", "1");
        }
    }

    protected void setUdid(String udid) {
        addParam("udid", udid);
    }

    /**
     * Appends special keys/values for advertising id and do-not-track. PlayServicesUrlRewriter will
     * replace these templates with the correct values when the request is processed.
     */
    protected void appendAdvertisingInfoTemplates() {
        addParam("udid", PlayServicesUrlRewriter.UDID_TEMPLATE);
        addParam("dnt", PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE);
    }

    /**
     * Adds the width and height.
     *
     * @param dimensions The width and height of the screen
     */
    protected void setDeviceDimensions(@NonNull final Point dimensions) {
        addParam(WIDTH_KEY, "" + dimensions.x);
        addParam(HEIGHT_KEY, "" + dimensions.y);
    }
}
