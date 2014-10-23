package com.mopub.mobileads.factories;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpClientFactory {
    public static final int SOCKET_SIZE = 8192;

    private static HttpClientFactory instance = new HttpClientFactory();

    @Deprecated // for testing
    public static void setInstance(HttpClientFactory factory) {
        instance = factory;
    }

    public static DefaultHttpClient create(int timeoutMilliseconds) {
        return instance.internalCreate(timeoutMilliseconds);
    }

    public static DefaultHttpClient create() {
        return instance.internalCreate(0);
    }

    protected DefaultHttpClient internalCreate(int timeoutMilliseconds) {
        HttpParams httpParameters = new BasicHttpParams();

        if (timeoutMilliseconds > 0) {
            // Set timeouts to wait for connection establishment / receiving data.
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutMilliseconds);
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutMilliseconds);
        }

        // Set the buffer size to avoid OutOfMemoryError exceptions on certain HTC devices.
        // http://stackoverflow.com/questions/5358014/android-httpclient-oom-on-4g-lte-htc-thunderbolt
        HttpConnectionParams.setSocketBufferSize(httpParameters, SOCKET_SIZE);

        return new DefaultHttpClient(httpParameters);
    }
}
