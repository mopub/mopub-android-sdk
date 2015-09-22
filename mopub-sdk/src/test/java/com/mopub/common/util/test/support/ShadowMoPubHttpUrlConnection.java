package com.mopub.common.util.test.support;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.MoPubHttpUrlConnection;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Implements(MoPubHttpUrlConnection.class)
public abstract class ShadowMoPubHttpUrlConnection extends HttpURLConnection {
    private static String sLatestRequestUrl;
    private static final Queue<HttpURLConnection> sPendingUrlConnections =
            new ArrayDeque<HttpURLConnection>();

    private ShadowMoPubHttpUrlConnection(URL url) {
        super(url);
    }

    public static void reset() {
        sPendingUrlConnections.clear();
    }

    @Implementation
    @Nullable
    public static HttpURLConnection getHttpUrlConnection(@NonNull final String url)
            throws IOException {
        sLatestRequestUrl = url;

        return sPendingUrlConnections.poll();
    }

    public static void addPendingResponse(final int statusCode, @NonNull final String response)
            throws IOException {
        addPendingResponse(statusCode, response, new HashMap<String, List<String>>());
    }

    public static void addPendingResponse(final int statusCode, @NonNull final String response,
            @NonNull final Map<String, List<String>> headers) throws IOException {
        final byte[] bytes = response.getBytes();
        HttpURLConnection mockUrlConnection = mock(HttpURLConnection.class);

        when(mockUrlConnection.getInputStream()).thenReturn(
                new ByteArrayInputStream(bytes));
        when(mockUrlConnection.getContentLength()).thenReturn(bytes.length);
        when(mockUrlConnection.getResponseCode()).thenReturn(statusCode);
        when(mockUrlConnection.getHeaderFields()).thenReturn(headers);

        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            when(mockUrlConnection.getHeaderField(entry.getKey())).thenReturn(entry.getValue().get(0));
        }

        sPendingUrlConnections.add(mockUrlConnection);
    }

    @NonNull
    public static Queue getPendingUrlConnections() {
        return sPendingUrlConnections;
    }

    @Nullable
    public static String getLatestRequestUrl() {
        return sLatestRequestUrl;
    }
}
