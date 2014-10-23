package com.mopub.nativeads;

import android.content.Context;

import com.mopub.common.DownloadResponse;
import com.mopub.common.HttpResponses;
import com.mopub.common.util.Json;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ResponseHeader;
import com.mopub.nativeads.factories.CustomEventNativeFactory;

import java.util.HashMap;
import java.util.Map;

final class CustomEventNativeAdapter {
    private CustomEventNativeAdapter() {}

    static final String RESPONSE_BODY_KEY = "response_body_key";

    public static void loadNativeAd(final Context context,
            final Map<String, Object> localExtras,
            final DownloadResponse downloadResponse,
            final CustomEventNative.CustomEventNativeListener customEventNativeListener) {

        final String customEventNativeData = downloadResponse.getFirstHeader(ResponseHeader.CUSTOM_EVENT_DATA);
        final String customEventNativeClassName = downloadResponse.getFirstHeader(ResponseHeader.CUSTOM_EVENT_NAME);

        final CustomEventNative customEventNative;
        try {
            customEventNative = CustomEventNativeFactory.create(customEventNativeClassName);
        } catch (Exception e) {
            MoPubLog.w("Failed to load Custom Event Native class: " + customEventNativeClassName);
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
            return;
        }

        Map<String, String> serverExtras = new HashMap<String, String>();
        // Attempt to load the JSON extras into mServerExtras.
        try {
            serverExtras = Json.jsonStringToMap(customEventNativeData);
        } catch (Exception e) {
            MoPubLog.w("Failed to create Map from JSON: " + customEventNativeData, e);
        }

        serverExtras.put(RESPONSE_BODY_KEY, HttpResponses.asResponseString(downloadResponse));

        customEventNative.loadNativeAd(
                context,
                customEventNativeListener,
                localExtras,
                serverExtras
        );
    }
}
