package com.mopub.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Map;

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
}
