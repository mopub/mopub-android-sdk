package com.mopub.mobileads.test.support;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.robolectric.shadows.httpclient.TestHttpResponse;

import java.util.HashMap;
import java.util.Map;

public class TestHttpResponseWithHeaders extends TestHttpResponse {
    private Map<String, Header> headers;

    public TestHttpResponseWithHeaders(int statusCode, String responseBody) {
        super(statusCode, responseBody);
        headers = new HashMap<String, Header>();
    }

    public TestHttpResponseWithHeaders(int statusCode, byte[] responseBody) {
        super(statusCode, responseBody);
        headers = new HashMap<String, Header>();
    }

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, new BasicHeader(name, value));
    }

    @Override
    public Header getFirstHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return headers.values().toArray(new Header[headers.size()]);
    }
}
