package com.mopub.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.util.ResponseHeader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Helps with serialization
 */
public class JSONObjectSerializable implements Serializable {
    private static final long serialVersionUID = 0L;

    public static class AdResponseInfo {
        @Nullable
        public String creativeId = null;
        @Nullable
        public String adSourceId = null;
    }

    @Nullable
    private JSONObject mJSONObject = null;

    public JSONObjectSerializable(@NonNull final JSONObject o) {
        mJSONObject = o;
    }

    private void readObject(@NonNull final ObjectInputStream inputStream) {
        try {
            if (inputStream.available() > 0) {
                mJSONObject = new JSONObject(inputStream.readUTF());
            }
        } catch (JSONException | IOException e) {
        }
    }

    private void writeObject(@NonNull final ObjectOutputStream outputStream) {
        if (mJSONObject == null) {
            return;
        }

        try {
            outputStream.writeUTF(mJSONObject.toString());
        } catch (IOException e) {
        }
    }

    @Nullable
    public JSONObject getJSONObject() {
        return mJSONObject;
    }

    @Nullable
    public synchronized AdResponseInfo getAdResponseInfoAndReset() {
        if (mJSONObject == null) {
            return null;
        }
        AdResponseInfo adResponseInfo = new AdResponseInfo();
        try {
            adResponseInfo.adSourceId = mJSONObject.getString(ResponseHeader.AD_SOURCE_ID.getKey());
            adResponseInfo.creativeId = mJSONObject.getString(ResponseHeader.CREATIVE_ID.getKey());
        } catch (Exception e) {
            return null;
        }

        mJSONObject = null;
        return adResponseInfo;
    }
}
