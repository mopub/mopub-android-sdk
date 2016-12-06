package com.mopub.common;

import android.graphics.Bitmap;

import org.json.JSONObject;
import org.json.JSONTokener;

import static android.graphics.BitmapFactory.decodeByteArray;

public final class HttpResponses {

    private HttpResponses() {}

    public static Bitmap asBitmap(final DownloadResponse downloadResponse) {
        if (downloadResponse == null) {
            return null;
        }

        final byte[] bytes = downloadResponse.getByteArray();
        return decodeByteArray(bytes, 0, bytes.length);
    }

    public static JSONObject asJsonObject(final DownloadResponse downloadResponse) {
        if (downloadResponse == null) {
            return null;
        }

        try {
            final String responseString = asResponseString(downloadResponse);

            final JSONTokener tokener = new JSONTokener(responseString);
            return new JSONObject(tokener);
        } catch (Exception e) {
            return null;
        }
    }

    public static String asResponseString(final DownloadResponse downloadResponse) {
        if (downloadResponse == null) {
            return null;
        }

        try {
            return new String(downloadResponse.getByteArray(), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
