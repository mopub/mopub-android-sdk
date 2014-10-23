package com.mopub.mobileads.util;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mopub.common.util.ResponseHeader.AD_TIMEOUT;
import static com.mopub.common.util.ResponseHeader.SCROLLABLE;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class HttpResponsesTest {
    private TestHttpResponseWithHeaders response;

    @Before
    public void setup() {
        response = new TestHttpResponseWithHeaders(200, "all is well");
    }

    @Test
    public void extractBooleanHeader_whenValueIsZero_shouldReturnFalse() throws Exception {
        response.addHeader(SCROLLABLE.getKey(), "0");
        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, false)).isFalse();

        response.addHeader(SCROLLABLE.getKey(), "0");
        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, true)).isFalse();
    }

    @Test
    public void extractBooleanHeader_whenValueIsOne_shouldReturnTrue() throws Exception {
        response.addHeader(SCROLLABLE.getKey(), "1");
        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, false)).isTrue();

        response.addHeader(SCROLLABLE.getKey(), "1");
        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, true)).isTrue();
    }

    @Test
    public void extractBooleanHeader_shouldReturnDefaultValue() throws Exception {
        // no header added to response

        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, false)).isFalse();
        assertThat(HttpResponses.extractBooleanHeader(response, SCROLLABLE, true)).isTrue();
    }

    @Test
    public void extractIntegerHeader_shouldReturnIntegerValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "10");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(10);

        response.addHeader(AD_TIMEOUT.getKey(), "0");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(0);

        response.addHeader(AD_TIMEOUT.getKey(), "-2");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(-2);
    }

    @Test
    public void extractIntegerHeader_withDoubleValue_shouldTruncateValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "3.14");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(3);

        response.addHeader(AD_TIMEOUT.getKey(), "-3.14");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(-3);
    }

    @Test
    public void extractIntegerHeader_whenNoHeaderPresent_shouldReturnNull() throws Exception {
        // no header added to response
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isNull();

        response.addHeader(AD_TIMEOUT.getKey(), null);
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isNull();
    }

    @Test
    public void extractIntegerHeader_withNonsenseStringValue_shouldReturnNull() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "llama!!guy");
        assertThat(HttpResponses.extractIntegerHeader(response, AD_TIMEOUT)).isNull();
    }

    @Test
    public void extractIntHeader_withInvalidHeader_shouldUseDefaultValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "5");
        assertThat(HttpResponses.extractIntHeader(response, AD_TIMEOUT, 10)).isEqualTo(5);

        response.addHeader(AD_TIMEOUT.getKey(), "five!");
        assertThat(HttpResponses.extractIntHeader(response, AD_TIMEOUT, 10)).isEqualTo(10);
    }
}
