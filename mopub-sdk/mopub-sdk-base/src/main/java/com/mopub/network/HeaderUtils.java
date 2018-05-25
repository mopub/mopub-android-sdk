package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.util.ResponseHeader;
import com.mopub.volley.Header;
import com.mopub.volley.toolbox.HttpResponse;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeaderUtils {
    @Nullable
    public static String extractHeader(Map<String, String> headers, ResponseHeader responseHeader) {
        return headers.get(responseHeader.getKey().toLowerCase());
    }

    @NonNull
    public static String extractHeader(@Nullable final JSONObject headers,
            @NonNull final ResponseHeader responseHeader) {
        Preconditions.checkNotNull(responseHeader);

        if (headers == null) {
            return "";
        }

        final String key = getKeyIgnoreCase(headers, responseHeader.getKey());
        return headers.optString(key);
    }

    @Nullable
    public static Integer extractIntegerHeader(JSONObject headers, ResponseHeader responseHeader) {
        return formatIntHeader(extractHeader(headers, responseHeader));
    }

    public static boolean extractBooleanHeader(Map<String, String> headers, ResponseHeader responseHeader, boolean defaultValue) {
        return formatBooleanHeader(extractHeader(headers, responseHeader), defaultValue);
    }

    public static boolean extractBooleanHeader(JSONObject headers, ResponseHeader responseHeader, boolean defaultValue) {
        return formatBooleanHeader(extractHeader(headers, responseHeader), defaultValue);
    }

    @Nullable
    public static Integer extractPercentHeader(JSONObject headers, ResponseHeader responseHeader) {
        return formatPercentHeader(extractHeader(headers, responseHeader));
    }

    @Nullable
    public static String extractPercentHeaderString(JSONObject headers,
            ResponseHeader responseHeader) {
        Integer percentHeaderValue = extractPercentHeader(headers, responseHeader);
        return percentHeaderValue != null ? percentHeaderValue.toString() : null;
    }

    @Nullable
    public static String extractHeader(HttpResponse response, ResponseHeader responseHeader) {
        final Header header = getFirstHeader(response.getHeaders(), responseHeader);
        return header != null ? header.getValue() : null;
    }

    public static boolean extractBooleanHeader(HttpResponse response, ResponseHeader responseHeader, boolean defaultValue) {
        return formatBooleanHeader(extractHeader(response, responseHeader), defaultValue);
    }

    @Nullable
    public static Integer extractIntegerHeader(HttpResponse response, ResponseHeader responseHeader) {
        String headerValue = extractHeader(response, responseHeader);
        return formatIntHeader(headerValue);
    }

    public static int extractIntHeader(HttpResponse response, ResponseHeader responseHeader, int defaultValue) {
        Integer headerValue = extractIntegerHeader(response, responseHeader);
        if (headerValue == null) {
            return defaultValue;
        }

        return headerValue;
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

    private static Header getFirstHeader(@Nullable final List<Header> headers,
            @NonNull final ResponseHeader responseHeader) {
        Preconditions.checkNotNull(responseHeader);

        if (headers == null) {
            return null;
        }

        for (final Header header : headers) {
            if (header.getName().equalsIgnoreCase(responseHeader.getKey())) {
                return header;
            }
        }
        return null;
    }

    @NonNull
    private static String getKeyIgnoreCase(@NonNull final JSONObject json,
                                           @NonNull final String searchKey) {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(searchKey);

        final Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            if (searchKey.equalsIgnoreCase(key)) {
                return key;
            }
        }
        return searchKey;
    }
}
