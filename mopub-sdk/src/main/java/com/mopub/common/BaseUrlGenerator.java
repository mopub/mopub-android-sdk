package com.mopub.common;

import android.net.Uri;

import static com.mopub.common.util.Strings.isEmpty;

public abstract class BaseUrlGenerator {
    private static final String IFA_PREFIX = "ifa:";
    private static final String SHA_PREFIX = "sha:";

    private StringBuilder mStringBuilder;
    private boolean mFirstParam;

    public abstract String generateUrlString(String serverHostname);

    protected void initUrlString(String serverHostname, String handlerType) {
        mStringBuilder = new StringBuilder("http://" + serverHostname + handlerType);
        mFirstParam = true;
    }

    protected String getFinalUrlString() {
        return mStringBuilder.toString();
    }

    protected void addParam(String key, String value) {
        if (value == null || isEmpty(value)) {
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
}
