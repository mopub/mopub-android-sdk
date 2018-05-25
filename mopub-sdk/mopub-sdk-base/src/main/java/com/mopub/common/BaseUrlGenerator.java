package com.mopub.common;

import android.graphics.Point;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.network.Networking;
import com.mopub.network.PlayServicesUrlRewriter;

public abstract class BaseUrlGenerator {

    /**
     * The ad unit id which identifies a spot for an ad to be placed.
     */
    protected static final String AD_UNIT_ID_KEY = "id";

    /**
     * nv = native version. This is the version of MoPub.
     */
    protected static final String SDK_VERSION_KEY = "nv";

    /**
     * User ifa or mopub-generated identifier.
     */
    protected static final String UDID_KEY = "udid";

    /**
     * "Do not track." Equal to 1 when limit ad tracking is turned on. Equal to 0 otherwise.
     */
    protected static final String DNT_KEY = "dnt";

    /**
     * Bundle ID, as in package name.
     */
    protected static final String BUNDLE_ID_KEY = "bundle";

    /**
     * The current consent state.
     */
    protected static final String CURRENT_CONSENT_STATUS_KEY = "current_consent_status";

    /**
     * The version of the vendor list that has been consented to. Null if no consent given.
     */
    protected static final String CONSENTED_VENDOR_LIST_VERSION_KEY = "consented_vendor_list_version";

    /**
     * The version of the privacy policy that has been consented to. Null if no consent given.
     */
    protected static final String CONSENTED_PRIVACY_POLICY_VERSION_KEY = "consented_privacy_policy_version";

    /**
     * Whether or not GDPR applies to this user. Can be different from whether or not this user is
     * in a GDPR region.
     */
    protected static final String GDPR_APPLIES = "gdpr_applies";

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

    protected void addParam(String key, Boolean value) {
        if (value == null) {
            return;
        }

        mStringBuilder.append(getParamDelimiter());
        mStringBuilder.append(key);
        mStringBuilder.append("=");
        mStringBuilder.append(value ? "1" : "0");
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
            addParam(DNT_KEY, "1");
        }
    }

    protected void setUdid(String udid) {
        addParam(UDID_KEY, udid);
    }

    /**
     * Appends special keys/values for advertising id and do-not-track. PlayServicesUrlRewriter will
     * replace these templates with the correct values when the request is processed.
     */
    protected void appendAdvertisingInfoTemplates() {
        addParam(UDID_KEY, PlayServicesUrlRewriter.UDID_TEMPLATE);
        addParam(DNT_KEY, PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE);
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
