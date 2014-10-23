package com.mopub.mobileads.test.support;

import com.mopub.mobileads.factories.HttpClientFactory;

import org.apache.http.impl.client.DefaultHttpClient;

public class TestHttpClientFactory extends HttpClientFactory {
    private DefaultHttpClient instance = new DefaultHttpClient();

    @Override
    protected DefaultHttpClient internalCreate(int timeout) {
        return instance;
    }
}
