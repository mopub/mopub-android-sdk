package com.mopub.network;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.mopub.common.util.ResponseHeader.AD_TIMEOUT;
import static com.mopub.common.util.ResponseHeader.SCROLLABLE;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HeaderUtilsTest {
    private TestHttpResponseWithHeaders response;

    @Before
    public void setup() {
        response = new TestHttpResponseWithHeaders(200, "all is well");
    }

    @Test
    public void extractBooleanHeader_whenValueIsZero_shouldReturnFalse() throws Exception {
        response.addHeader(SCROLLABLE.getKey(), "0");
        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, false)).isFalse();

        response.addHeader(SCROLLABLE.getKey(), "0");
        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, true)).isFalse();
    }

    @Test
    public void extractBooleanHeader_whenValueIsOne_shouldReturnTrue() throws Exception {
        response.addHeader(SCROLLABLE.getKey(), "1");
        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, false)).isTrue();

        response.addHeader(SCROLLABLE.getKey(), "1");
        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, true)).isTrue();
    }

    @Test
    public void extractBooleanHeader_shouldReturnDefaultValue() throws Exception {
        // no header added to response

        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, false)).isFalse();
        assertThat(HeaderUtils.extractBooleanHeader(response, SCROLLABLE, true)).isTrue();
    }

    @Test
    public void extractIntegerHeader_shouldReturnIntegerValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "10");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(10);

        response.addHeader(AD_TIMEOUT.getKey(), "0");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(0);

        response.addHeader(AD_TIMEOUT.getKey(), "-2");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(-2);
    }

    @Test
    public void extractIntegerHeader_withDoubleValue_shouldTruncateValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "3.14");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(3);

        response.addHeader(AD_TIMEOUT.getKey(), "-3.14");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isEqualTo(-3);
    }

    @Test
    public void extractIntegerHeader_whenNoHeaderPresent_shouldReturnNull() throws Exception {
        // no header added to response
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isNull();

        response.addHeader(AD_TIMEOUT.getKey(), null);
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isNull();
    }

    @Test
    public void extractIntegerHeader_withNonsenseStringValue_shouldReturnNull() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "llama!!guy");
        assertThat(HeaderUtils.extractIntegerHeader(response, AD_TIMEOUT)).isNull();
    }

    @Test
    public void extractIntHeader_withInvalidHeader_shouldUseDefaultValue() throws Exception {
        response.addHeader(AD_TIMEOUT.getKey(), "5");
        assertThat(HeaderUtils.extractIntHeader(response, AD_TIMEOUT, 10)).isEqualTo(5);

        response.addHeader(AD_TIMEOUT.getKey(), "five!");
        assertThat(HeaderUtils.extractIntHeader(response, AD_TIMEOUT, 10)).isEqualTo(10);
    }
}
