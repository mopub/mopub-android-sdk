// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ResponseHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeaderUtils {

    @NonNull
    public static String extractHeader(@Nullable final JSONObject headers,
            @NonNull final ResponseHeader responseHeader) {
        Preconditions.checkNotNull(responseHeader);

        if (headers == null) {
            return "";
        }

        return headers.optString(responseHeader.getKey());
    }

    @Nullable
    public static Integer extractIntegerHeader(JSONObject headers, ResponseHeader responseHeader) {
        return formatIntHeader(extractHeader(headers, responseHeader));
    }

    public static boolean extractBooleanHeader(JSONObject headers, ResponseHeader responseHeader, boolean defaultValue) {
        return formatBooleanHeader(extractHeader(headers, responseHeader), defaultValue);
    }

    @Nullable
    public static Integer extractPercentHeader(JSONObject headers, ResponseHeader responseHeader) {
        return formatPercentHeader(extractHeader(headers, responseHeader));
    }

    @NonNull
    static List<String> extractStringArray(@NonNull final JSONObject headers,
            @NonNull final ResponseHeader responseHeader) {
        Preconditions.checkNotNull(headers);
        Preconditions.checkNotNull(responseHeader);

        final List<String> stringArray = new ArrayList<>();
        final JSONArray jsonArray = headers.optJSONArray(responseHeader.getKey());
        if (jsonArray == null) {
            return stringArray;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                stringArray.add(jsonArray.getString(i));
            } catch (JSONException e) {
                MoPubLog.d("Unable to parse item " + i + " from " + responseHeader.getKey());
            }
        }

        return stringArray;
    }

    @Nullable
    static String extractPercentHeaderString(JSONObject headers,
            ResponseHeader responseHeader) {
        Integer percentHeaderValue = extractPercentHeader(headers, responseHeader);
        return percentHeaderValue != null ? percentHeaderValue.toString() : null;
    }

    private static boolean formatBooleanHeader(@Nullable String headerValue, boolean defaultValue) {
        if (headerValue == null) {
            return defaultValue;
        }
        return headerValue.equals("1");
    }

    @Nullable
    private static Integer formatIntHeader(String headerValue) {
        try {
            return Integer.parseInt(headerValue);
        } catch (Exception e) {
            // Continue below if we can't parse it quickly
        }

        // The number format way of parsing integers is way slower than Integer.parseInt, but
        // for numbers like 3.14, we would like to return 3, not null.
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        numberFormat.setParseIntegerOnly(true);

        try {
            Number value = numberFormat.parse(headerValue.trim());
            return value.intValue();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Integer formatPercentHeader(@Nullable String headerValue) {
        if (headerValue == null) {
            return null;
        }

        final Integer percentValue = formatIntHeader(headerValue.replace("%", ""));

        if (percentValue == null || percentValue < 0 || percentValue > 100) {
            return null;
        }

        return percentValue;
    }
}
