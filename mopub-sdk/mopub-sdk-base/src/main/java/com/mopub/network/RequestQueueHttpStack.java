package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.util.ResponseHeader;
import com.mopub.volley.AuthFailureError;
import com.mopub.volley.Request;
import com.mopub.volley.toolbox.HttpResponse;
import com.mopub.volley.toolbox.HurlStack;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLSocketFactory;

/**
 * Modified Volley HurlStack with explicitly specified User-Agent.
 *
 * Used by Networking's singleton RequestQueue to ensure all network requests use WebView's
 * User-Agent.
 */
public class RequestQueueHttpStack extends HurlStack {
    @NonNull private final String mUserAgent;

    public RequestQueueHttpStack(@NonNull final String userAgent) {
        this(userAgent, null);
    }

    public RequestQueueHttpStack(@NonNull final String userAgent, @Nullable final UrlRewriter urlRewriter) {
        this(userAgent, urlRewriter, null);
    }

    public RequestQueueHttpStack(@NonNull final String userAgent, @Nullable final UrlRewriter urlRewriter,
                                 @Nullable final SSLSocketFactory sslSocketFactory) {
        super(urlRewriter, sslSocketFactory);

        mUserAgent = userAgent;
    }

    @Override
    public HttpResponse executeRequest(@NonNull final Request<?> request,
            @Nullable Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        // If the headers map is null or empty, make a new once since Collections.emptyMap()
        // returns an unmodifiable map.
        if (additionalHeaders == null || additionalHeaders.isEmpty()) {
            additionalHeaders = new TreeMap<String, String>();
        }

        additionalHeaders.put(ResponseHeader.USER_AGENT.getKey(), mUserAgent);

        return super.executeRequest(request, additionalHeaders);
    }
}
