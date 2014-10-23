package com.mopub.common;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SdkTestRunner.class)
public class DownloadResponseTest {

    DownloadResponse subject;
    TestHttpResponseWithHeaders mockHttpResponse;

    @Before
    public void setup() throws Exception {
        mockHttpResponse = new TestHttpResponseWithHeaders(200, "abcde".getBytes());
        mockHttpResponse.addHeader(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "testCustomEvent");
        mockHttpResponse.addHeader(ResponseHeader.CLICKTHROUGH_URL.getKey().toLowerCase(Locale.US), "http://example.com/");
        mockHttpResponse.addHeader(ResponseHeader.FAIL_URL.getKey().toUpperCase(Locale.US), "http://mopub.com/");
        subject = new DownloadResponse(mockHttpResponse);
    }

    @Test
    public void testGetByteArray() throws Exception {
        assertArrayEquals("abcde".getBytes(), subject.getByteArray());
    }

    @Test
    public void testGetStatusCode() throws Exception {
        assertEquals(200, subject.getStatusCode());
    }

    @Test
    public void testGetContentLength() throws Exception {
        assertEquals("abcde".getBytes().length, subject.getContentLength());
    }

    @Test
    public void testGetFirstHeader_caseInsensitive() throws Exception {
        assertEquals("testCustomEvent", subject.getFirstHeader(ResponseHeader.CUSTOM_EVENT_NAME));
        assertEquals("http://example.com/", subject.getFirstHeader(ResponseHeader.CLICKTHROUGH_URL));
        assertEquals("http://mopub.com/", subject.getFirstHeader(ResponseHeader.FAIL_URL));
        assertNull(subject.getFirstHeader(ResponseHeader.CUSTOM_EVENT_DATA));
    }
}
