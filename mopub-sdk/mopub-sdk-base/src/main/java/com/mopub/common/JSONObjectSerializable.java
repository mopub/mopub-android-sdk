package com.mopub.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;

/**
 * Helps with serialization
 */
public class JSONObjectSerializable implements Serializable {
    private static final long serialVersionUID = 0L;

    @Nullable
    private WeakReference<JSONObject> mJSONObject = null;

    public JSONObjectSerializable(@NonNull final JSONObject o) {
        mJSONObject = new WeakReference<>(o);
    }

    private void readObject(@NonNull final ObjectInputStream inputStream) {
        try {
            if (inputStream.available() > 0) {
                mJSONObject = new WeakReference<>(new JSONObject(inputStream.readUTF()));
            }
        } catch (JSONException | IOException e) {
        }
    }

    private void writeObject(@NonNull final ObjectOutputStream outputStream) {
        if (mJSONObject == null) {
            return;
        }
        
        JSONObject tmp = mJSONObject.get();
        if (tmp != null) {
            try {
                outputStream.writeUTF(tmp.toString());
            } catch (IOException e) {
            }
        }
    }

    @Nullable
    public WeakReference<JSONObject> getJSONObject() {
        return mJSONObject;
    }
}
