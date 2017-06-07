package com.mopub.network;

import android.support.annotation.Nullable;

import com.mopub.common.util.ResponseHeader;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

public class HeaderUtils {

	@Nullable
	public static String extractHeader(Map<String, String> headers, ResponseHeader responseHeader) {
		return headers.get(responseHeader.getKey());
	}

	public static Integer extractIntegerHeader(Map<String, String> headers, ResponseHeader responseHeader) {
		return formatIntHeader(extractHeader(headers, responseHeader));
	}

	public static boolean extractBooleanHeader(Map<String, String> headers, ResponseHeader responseHeader, boolean defaultValue) {
		return formatBooleanHeader(extractHeader(headers, responseHeader), defaultValue);
	}

	public static Integer extractPercentHeader(Map<String, String> headers, ResponseHeader responseHeader) {
		return formatPercentHeader(extractHeader(headers, responseHeader));
	}

	@Nullable
	public static String extractPercentHeaderString(Map<String, String> headers, ResponseHeader responseHeader) {
		Integer percentHeaderValue = extractPercentHeader(headers, responseHeader);
		return percentHeaderValue != null
				? percentHeaderValue.toString()
				: null;
	}

	public static String extractHeader(HttpResponse response, ResponseHeader responseHeader) {
		Header header = response.getFirstHeader(responseHeader.getKey());
		return header != null
				? header.getValue()
				: null;
	}

	public static boolean extractBooleanHeader(HttpResponse response, ResponseHeader responseHeader, boolean defaultValue) {
		return formatBooleanHeader(extractHeader(response, responseHeader), defaultValue);
	}

	public static Integer extractIntegerHeader(HttpResponse response, ResponseHeader responseHeader) {
		String headerValue = extractHeader(response, responseHeader);
		return formatIntHeader(headerValue);
	}

	public static int extractIntHeader(HttpResponse response, ResponseHeader responseHeader, int defaultValue) {
		Integer headerValue = extractIntegerHeader(response, responseHeader);
		return headerValue != null
				? headerValue
				: defaultValue;
	}

	private static boolean formatBooleanHeader(@Nullable String headerValue, boolean defaultValue) {
		return headerValue != null
				? headerValue.equals("1")
				: defaultValue;
	}

	private static Integer formatIntHeader(String headerValue) {
		try {
			return headerValue != null
					? Integer.parseInt(headerValue)
					: null;
		} catch (NumberFormatException n) {
			// Continue below if we can't parse it quickly
			try {
				// The number format way of parsing integers is way slower than Integer.parseInt, but
				// for numbers like 3.14, we would like to return 3, not null.
				NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
				numberFormat.setParseIntegerOnly(true);
				Number value = numberFormat.parse(headerValue.trim());
				return value.intValue();
			} catch (ParseException e) {
				return null;
			}
		}
	}

	@Nullable
	private static Integer formatPercentHeader(@Nullable String headerValue) {
		final Integer percentValue = headerValue != null
				? formatIntHeader(headerValue.replace("%", ""))
				: null;
		return percentValue != null
				&& percentValue >= 0
				&& percentValue <= 100
				? percentValue : null;
	}
}
