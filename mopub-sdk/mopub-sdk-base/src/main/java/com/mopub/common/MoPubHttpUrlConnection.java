package com.mopub.common;

import android.support.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

public abstract class MoPubHttpUrlConnection extends HttpURLConnection {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;

    private MoPubHttpUrlConnection(URL url) {
        super(url);
    }

    public static HttpURLConnection getHttpUrlConnection(@NonNull final String url)
            throws IOException {
        Preconditions.checkNotNull(url);

        // If the passed-in url has already been encoded improperly, there is no way to salvage this
        // connection request -- fail quickly instead.
        if (isUrlImproperlyEncoded(url)) {
            throw new IllegalArgumentException("URL is improperly encoded: " + url);
        }

        // Attempt to encode the passed-in url and use that, if possible. If this fails, then
        // fallback to the original url instead.
        String getUrl;
        try {
            getUrl = urlEncode(url);
        } catch (Exception e) {
            getUrl = url;
        }

        final HttpURLConnection urlConnection =
                (HttpURLConnection) new URL(getUrl).openConnection();
        urlConnection.setRequestProperty("User-Agent", Networking.getCachedUserAgent());
        urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(READ_TIMEOUT);

        return urlConnection;
    }

    /**
     * This method constructs a properly encoded and valid URI adhering to legal characters for
     * each component. See Android docs on these classes for reference.
     */
    @NonNull
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
    @NonNull
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
