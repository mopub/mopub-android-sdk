package com.mopub.common;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class DownloadResponseTest {

    DownloadResponse subject;
    TestHttpResponseWithHeaders testHttpResponse;

    @Before
    public void setup() throws Exception {
        testHttpResponse = new TestHttpResponseWithHeaders(200, "abcde".getBytes());
        testHttpResponse.addHeader(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "testCustomEvent");
        testHttpResponse.addHeader(ResponseHeader.CLICK_TRACKING_URL.getKey().toLowerCase(Locale.US), "https://example.com/");
        testHttpResponse.addHeader(ResponseHeader.FAIL_URL.getKey().toUpperCase(Locale.US), "https://mopub.com/");
        subject = new DownloadResponse(testHttpResponse);
    }

    @Test
    public void constructor_withNullHttpEntity_shouldNotThrowNullPointerException() throws Exception {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getEntity()).thenReturn(null);
        when(mockHttpResponse.getStatusLine()).thenReturn(testHttpResponse.new TestStatusLine());

        DownloadResponse downloadResponse = new DownloadResponse(mockHttpResponse);
        assertThat(downloadResponse.getContentLength()).isEqualTo(0);
        assertThat(downloadResponse.getByteArray()).isEmpty();
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
        assertEquals("https://example.com/", subject.getFirstHeader(ResponseHeader.CLICK_TRACKING_URL));
        assertEquals("https://mopub.com/", subject.getFirstHeader(ResponseHeader.FAIL_URL));
        assertNull(subject.getFirstHeader(ResponseHeader.CUSTOM_EVENT_DATA));
    }
}
