// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.MoPubError;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.os.SystemClock.uptimeMillis;

class ContentDownloadAnalytics {
    enum DownloadResult {
        AD_LOADED("ad_loaded"),
        MISSING_ADAPTER("missing_adapter"),
        TIMEOUT("timeout"),
        INVALID_DATA("invalid_data");

        @NonNull
        private final String value;

        DownloadResult(@NonNull String loadResult) {
            value = loadResult;
        }
    }

    private static final String LOAD_DURATION_MS_MACRO = "%%LOAD_DURATION_MS%%";
    private static final String LOAD_RESULT_MACRO = "%%LOAD_RESULT%%";

    @Nullable
    Long mBeforeLoadTime = null;
    @NonNull
    private AdResponse mAdResponse;

    ContentDownloadAnalytics(@NonNull AdResponse adResponse) {
        Preconditions.checkNotNull(adResponse);

        mAdResponse = adResponse;
    }

    void reportBeforeLoad(@Nullable Context context) {
        if (context == null) {
            return;
        }

        String url = mAdResponse.getBeforeLoadUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }

        mBeforeLoadTime = uptimeMillis();
        TrackingRequest.makeTrackingHttpRequest(url, context);
    }

    void reportAfterLoad(@Nullable Context context, @Nullable final MoPubError errorCode) {
        if (context == null || mBeforeLoadTime == null) {
            return;
        }

        DownloadResult result = errorCodeToDownloadResult(errorCode);
        List<String> urls = generateAfterLoadUrls(mAdResponse.getAfterLoadUrls(), result.value);

        TrackingRequest.makeTrackingHttpRequest(urls, context);
    }

    void reportAfterLoadSuccess(@Nullable Context context) {
        if (context == null || mBeforeLoadTime == null) {
            return;
        }

        List<String> urls = generateAfterLoadUrls(mAdResponse.getAfterLoadSuccessUrls(), DownloadResult.AD_LOADED.value);

        TrackingRequest.makeTrackingHttpRequest(urls, context);
    }

    void reportAfterLoadFail(@Nullable Context context, @Nullable final MoPubError errorCode) {
        if (context == null || mBeforeLoadTime == null) {
            return;
        }

        DownloadResult result = errorCodeToDownloadResult(errorCode);
        List<String> urls = generateAfterLoadUrls(mAdResponse.getAfterLoadFailUrls(), result.value);

        TrackingRequest.makeTrackingHttpRequest(urls, context);
    }

    @Nullable
    private List<String> generateAfterLoadUrls(@Nullable List<String> urls, @NonNull String loadResult) {
        if (urls == null || urls.isEmpty() || mBeforeLoadTime == null) {
            return null;
        }

        List<String> newUrls = new ArrayList<>();

        for (String url : urls) {
            url = url.replace(LOAD_DURATION_MS_MACRO, String.valueOf(uptimeMillis() - mBeforeLoadTime));
            url = url.replace(LOAD_RESULT_MACRO, Uri.encode(loadResult));
            newUrls.add(url);
        }
        return newUrls;
    }

    @NonNull
    private DownloadResult errorCodeToDownloadResult(@Nullable final MoPubError errorCode) {
        if (null == errorCode) {
            return DownloadResult.AD_LOADED;
        }

        switch (errorCode.getIntCode()) {
            case MoPubError.ER_SUCCESS:
                return DownloadResult.AD_LOADED;
            case MoPubError.ER_TIMEOUT:
                return DownloadResult.TIMEOUT;
            case MoPubError.ER_ADAPTER_NOT_FOUND:
                return DownloadResult.MISSING_ADAPTER;
        }

        return DownloadResult.INVALID_DATA;
    }
}

