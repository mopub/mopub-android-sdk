package com.mopub.common;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import static com.mopub.common.util.ResponseHeader.USER_AGENT;

public class HttpClient {
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int SOCKET_TIMEOUT = 10000;

    public static AndroidHttpClient getHttpClient() {
        final String userAgent = Networking.getCachedUserAgent();

        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(userAgent);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpClientParams.setRedirecting(params, true);

        return httpClient;
    }

    public static HttpGet initializeHttpGet(@NonNull final String url) {
        return initializeHttpGet(url, null);
    }

    public static HttpGet initializeHttpGet(@NonNull String url, @Nullable final Context context) {
        Preconditions.checkNotNull(url);

        // Try to encode url. If this fails, then fallback on the original url
        String getUrl;
        try {
            getUrl = urlEncode(url);
        } catch (Exception e) {
            getUrl = url;
        }

        final HttpGet httpGet = new HttpGet(getUrl);

        final String webViewUserAgent;
        if (context != null) {
            webViewUserAgent = Networking.getUserAgent(context);
        } else {
            webViewUserAgent = Networking.getCachedUserAgent();
        }

        if (webViewUserAgent != null) {
            httpGet.addHeader(USER_AGENT.getKey(), webViewUserAgent);
        }

        return httpGet;
    }

    /**
     * This method constructs a properly encoded and valid URI adhering to legal characters for
     * each component. See Android docs on these classes for reference.
     */
    public static String urlEncode(@NonNull final String url) throws Exception {
        Preconditions.checkNotNull(url);

        // If the URL is improperly encoded, then fail
        if (isUrlImproperlyEncoded(url)) {
            throw new UnsupportedEncodingException("URL is improperly encoded: " + url);
        }

        // If the url is unencoded, then encode it. Otherwise it is already properly encoded
        // and leave it as is.
        URI uri;
        if (isUrlUnencoded(url)) {
            uri = encodeUrl(url);
        } else {
            uri = new URI(url);
        }

        return uri.toURL().toString();
    }

    /**
     * This method tries to decode the URL and returns false if it can't due to improper encoding.
     */
    static boolean isUrlImproperlyEncoded(@NonNull String url) {
        try {
            URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            MoPubLog.w("Url is improperly encoded: " + url);
            return true;
        }
        return false;
    }

    /**
     * This method tries to construct a URI and returns true if it can't due to illegal characters
     * in the url.
     */
    static boolean isUrlUnencoded(@NonNull String url) {
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            return true;
        }
        return false;
    }

    /**
     * This method encodes each component of the URL into a valid URI.
     */
    static URI encodeUrl(@NonNull String urlString) throws Exception {
        URI uri;
        try {
            URL url = new URL(urlString);
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());
        } catch (Exception e) {
            MoPubLog.w("Failed to encode url: " + urlString);
            throw e;
        }
        return uri;
    }
}
