// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.base.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

@SuppressWarnings({"WeakerAccess"})
public class ImpressionData implements Serializable {
    private static final long serialVersionUID = BuildConfig.VERSION_CODE;

    public static final String ADUNIT_ID = "adunit_id";
    public static final String ADUNIT_NAME = "adunit_name";
    public static final String ADUNIT_FORMAT = "adunit_format";
    public static final String IMPRESSION_ID = "id";
    public static final String CURRENCY = "currency";
    public static final String PUBLISHER_REVENUE = "publisher_revenue";
    public static final String ADGROUP_ID = "adgroup_id";
    public static final String ADGROUP_NAME = "adgroup_name";
    public static final String ADGROUP_TYPE = "adgroup_type";
    public static final String ADGROUP_PRIORITY = "adgroup_priority";
    public static final String COUNTRY = "country";
    public static final String PRECISION = "precision";
    public static final String NETWORK_NAME = "network_name";
    public static final String NETWORK_PLACEMENT_ID = "network_placement_id";

    @NonNull
    private SerializableJson mJson;

    private ImpressionData(@NonNull final JSONObject json) throws JSONException {
        mJson = new SerializableJson(json);
    }

    @Nullable
    static ImpressionData create(@Nullable final JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        try {
            return new ImpressionData(jsonObject);
        } catch (Exception ex) {
            MoPubLog.log(CUSTOM, ex.toString());
        }

        return null;
    }

    @Nullable
    public String getAdUnitId() {
        return mJson.optString(ADUNIT_ID, null);
    }

    @Nullable
    public String getAdUnitName() {
        return mJson.optString(ADUNIT_NAME, null);
    }

    @Nullable
    public String getAdUnitFormat() {
        return mJson.optString(ADUNIT_FORMAT, null);
    }

    @Nullable
    public String getImpressionId() {
        return mJson.optString(IMPRESSION_ID, null);
    }

    @Nullable
    public String getCurrency() {
        return mJson.optString(CURRENCY, null);
    }

    @Nullable
    public Double getPublisherRevenue() {
        try {
            return mJson.getDouble(PUBLISHER_REVENUE);
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    public String getAdGroupId() {
        return mJson.optString(ADGROUP_ID, null);
    }

    @Nullable
    public String getAdGroupName() {
        return mJson.optString(ADGROUP_NAME, null);
    }

    @Nullable
    public String getAdGroupType() {
        return mJson.optString(ADGROUP_TYPE, null);
    }

    @Nullable
    public Integer getAdGroupPriority() {
        try {
            return mJson.getInt(ADGROUP_PRIORITY);
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    public String getCountry() {
        return mJson.optString(COUNTRY, null);
    }

    @Nullable
    public String getPrecision() {
        return mJson.optString(PRECISION, null);
    }

    @Nullable
    public String getNetworkName() {
        return mJson.optString(NETWORK_NAME, null);
    }

    @Nullable
    public String getNetworkPlacementId() {
        return mJson.optString(NETWORK_PLACEMENT_ID, null);
    }

    /**
     * @return - original JSON object from the server.
     */
    @NonNull
    public JSONObject getJsonRepresentation() {
        return mJson;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeUTF(mJson.toString());
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException, JSONException {
        ois.defaultReadObject();
        mJson = new SerializableJson(ois.readUTF());
    }

    private static class SerializableJson extends JSONObject implements Serializable {
        SerializableJson(@NonNull final JSONObject json) throws JSONException {
            super(json.toString());
        }

        SerializableJson(@NonNull final String json) throws JSONException {
            super(json);
        }
    }
}
