package com.mopub.mobileads.util;

import com.mopub.common.util.ResponseHeader;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.text.NumberFormat;
import java.util.Locale;

public class HttpResponses {
    public static String extractHeader(HttpResponse response, ResponseHeader responseHeader) {
        Header header = response.getFirstHeader(responseHeader.getKey());
        return header != null ? header.getValue() : null;
    }

    public static boolean extractBooleanHeader(HttpResponse response, ResponseHeader responseHeader, boolean defaultValue) {
        String header = extractHeader(response, responseHeader);
        if (header == null) {
            return defaultValue;
        }
        return header.equals("1");
    }

    public static Integer extractIntegerHeader(HttpResponse response, ResponseHeader responseHeader) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        numberFormat.setParseIntegerOnly(true);

        String headerValue = extractHeader(response, responseHeader);
        try {
            Number value = numberFormat.parse(headerValue.trim());
            return value.intValue();
        } catch (Exception e) {
            return null;
        }
    }

    public static int extractIntHeader(HttpResponse response, ResponseHeader responseHeader, int defaultValue) {
        Integer headerValue = extractIntegerHeader(response, responseHeader);
        if (headerValue == null) {
            return defaultValue;
        }

        return headerValue;
    }
}
