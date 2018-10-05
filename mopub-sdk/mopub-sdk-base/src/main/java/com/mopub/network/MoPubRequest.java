// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.util.ResponseHeader;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Changes the type of request it is based on whether or not the request is going to MoPub's ad
 * server. If the request is for ad server in some way, reconstruct it as a POST request and
 * set the body and content type to json.
 */
public abstract class MoPubRequest<T> extends Request<T>  {

    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    @NonNull private final String mOriginalUrl;
    @NonNull private final Context mContext;

    public MoPubRequest(@NonNull final Context context,
                        @NonNull final String url,
                        @Nullable final Response.ErrorListener listener) {
        super(MoPubRequestUtils.chooseMethod(url), MoPubRequestUtils.truncateQueryParamsIfPost(url),
                listener);

        mOriginalUrl = url;
        mContext = context.getApplicationContext();
    }

    @Override
    protected Map<String, String> getParams() {
        if (!MoPubRequestUtils.isMoPubRequest(getUrl())) {
            return null;
        }

        return MoPubRequestUtils.convertQueryToMap(mContext, mOriginalUrl);
    }

    @Override
    public String getBodyContentType() {
        if (MoPubRequestUtils.isMoPubRequest(getUrl())) {
            return JSON_CONTENT_TYPE;
        }
        return super.getBodyContentType();
    }

    @Override
    public byte[] getBody() {
        final String body = MoPubRequestUtils.generateBodyFromParams(getParams(), getUrl());
        if (body == null) {
            return null;
        }
        return body.getBytes();
    }

    @NonNull
    protected String parseStringBody(@NonNull final NetworkResponse response) {
        Preconditions.checkNotNull(response);

        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return parsed;
    }

    @NonNull
    public String getOriginalUrl() {
        return mOriginalUrl;
    }

    @Override
    public Map<String, String> getHeaders() {
        final TreeMap<String, String> headers = new TreeMap<>();

        Locale userLocale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList list = mContext.getResources().getConfiguration().getLocales();
            if (list.size() > 0) {
                userLocale = list.get(0);
            }
        } else {
            userLocale = mContext.getResources().getConfiguration().locale;
        }

        final String language;
        final String country;
        if (userLocale != null && !TextUtils.isEmpty(userLocale.toString().trim())) {
            // If user's preferred locale is available
            language = userLocale.getLanguage().trim();
            country = userLocale.getCountry().trim();
        } else {
            // Use default locale
            language = Locale.getDefault().getLanguage().trim();
            country = Locale.getDefault().getCountry().trim();
        }

        String languageCode;
        if (!TextUtils.isEmpty(language)) {
            languageCode = language;
            if (!TextUtils.isEmpty(country)) {
                languageCode += "-" + country.toLowerCase();
            }
            headers.put(ResponseHeader.ACCEPT_LANGUAGE.getKey(), languageCode);
        }

        return headers;
    }
}
