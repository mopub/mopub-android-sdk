package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestDateAndTime;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.*;

import static com.mopub.mobileads.AdViewController.MINIMUM_REFRESH_TIME_MILLISECONDS;
import static com.mopub.common.util.ResponseHeader.AD_TIMEOUT;
import static com.mopub.common.util.ResponseHeader.AD_TYPE;
import static com.mopub.common.util.ResponseHeader.CLICKTHROUGH_URL;
import static com.mopub.common.util.ResponseHeader.DSP_CREATIVE_ID;
import static com.mopub.common.util.ResponseHeader.FAIL_URL;
import static com.mopub.common.util.ResponseHeader.HEIGHT;
import static com.mopub.common.util.ResponseHeader.IMPRESSION_URL;
import static com.mopub.common.util.ResponseHeader.NETWORK_TYPE;
import static com.mopub.common.util.ResponseHeader.REDIRECT_URL;
import static com.mopub.common.util.ResponseHeader.REFRESH_TIME;
import static com.mopub.common.util.ResponseHeader.WIDTH;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class AdConfigurationTest {
    private AdConfiguration subject;
    private Context context;
    private TestHttpResponseWithHeaders httpResponse;

    @Before
    public void setUp() throws Exception {
        context = new Activity();

        subject = new AdConfiguration(context);

        httpResponse = new TestHttpResponseWithHeaders(200, "I ain't got no-body");
    }

    @Test
    public void constructor_shouldSetDefaults() throws Exception {
        assertThat(subject.getAdUnitId()).isNull();
        assertThat(subject.getResponseString()).isNull();
        assertThat(subject.getAdType()).isNull();
        assertThat(subject.getNetworkType()).isNull();
        assertThat(subject.getRedirectUrl()).isNull();
        assertThat(subject.getClickthroughUrl()).isNull();
        assertThat(subject.getImpressionUrl()).isNull();
        assertThat(subject.getTimeStamp()).isEqualTo(TestDateAndTime.now().getTime());
        assertThat(subject.getWidth()).isEqualTo(0);
        assertThat(subject.getHeight()).isEqualTo(0);
        assertThat(subject.getAdTimeoutDelay()).isNull();
        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(60000);
        assertThat(subject.getFailUrl()).isNull();
        assertThat(subject.getDspCreativeId()).isNull();
    }

    @Test
    public void constructor_shouldSetHashedUdid() throws Exception {
        // this is sha1 of null
        assertThat(subject.getHashedUdid()).isEqualTo("da39a3ee5e6b4b0d3255bfef95601890afd80709");
    }

    @Test
    public void constructor_withNullContext_shouldNotSetHashedUdid() throws Exception {
        subject = new AdConfiguration(null);

        assertThat(subject.getHashedUdid()).isNull();
    }

    @Test
    public void constructor_shouldSetUserAgent() throws Exception {
        assertThat(subject.getUserAgent()).isEqualTo("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
    }

    @Test
    public void constructor_withNullContext_shouldSetUserAgent() throws Exception {
        subject = new AdConfiguration(null);

        assertThat(subject.getUserAgent()).isNull();
    }

    @Test
    public void constructor_shouldSetDeviceLocale() throws Exception {
        Robolectric.getShadowApplication().getResources().getConfiguration().locale = Locale.FRANCE;

        subject = new AdConfiguration(context);

        assertThat(subject.getDeviceLocale()).isEqualTo("fr_FR");
    }

    @Test
    public void constructor_withNullContext_shouldNotSetDeviceLocale() throws Exception {
        Robolectric.getShadowApplication().getResources().getConfiguration().locale = Locale.FRANCE;

        subject = new AdConfiguration(null);

        assertThat(subject.getDeviceLocale()).isNull();
    }

    @Test
    public void constructor_shouldSetDeviceModelAndPlatformVersionAndSdkVersion() throws Exception {
        assertThat(subject.getDeviceModel()).isNotNull();
        assertThat(subject.getPlatformVersion()).isEqualTo(Build.VERSION.SDK_INT);
        assertThat(subject.getSdkVersion()).isEqualTo(MoPub.SDK_VERSION);
    }

    @Test
    public void constructor_shouldSetBroadcastIdentifier() throws Exception {
        assertThat(subject.getBroadcastIdentifier()).isGreaterThan(0);
    }

    @Test
    public void addHttpResponse_shouldSetFields() throws Exception {
        Date now = new Date();
        TestDateAndTime.getInstance().setNow(now);

        httpResponse.addHeader(AD_TYPE.getKey(), "this is an ad type");
        httpResponse.addHeader(NETWORK_TYPE.getKey(), "network type!");
        httpResponse.addHeader(REDIRECT_URL.getKey(), "redirect url");
        httpResponse.addHeader(CLICKTHROUGH_URL.getKey(), "clickthrough url");
        httpResponse.addHeader(FAIL_URL.getKey(), "fail url");
        httpResponse.addHeader(IMPRESSION_URL.getKey(), "impression url");
        httpResponse.addHeader(WIDTH.getKey(), "320  ");
        httpResponse.addHeader(HEIGHT.getKey(), "  50");
        httpResponse.addHeader(AD_TIMEOUT.getKey(), "  12  ");
        httpResponse.addHeader(REFRESH_TIME.getKey(), "70");
        httpResponse.addHeader(DSP_CREATIVE_ID.getKey(), "1534363");

        subject.addHttpResponse(httpResponse);

        assertThat(subject.getAdType()).isEqualTo("this is an ad type");
        assertThat(subject.getNetworkType()).isEqualTo("network type!");
        assertThat(subject.getRedirectUrl()).isEqualTo("redirect url");
        assertThat(subject.getClickthroughUrl()).isEqualTo("clickthrough url");
        assertThat(subject.getFailUrl()).isEqualTo("fail url");
        assertThat(subject.getImpressionUrl()).isEqualTo("impression url");
        assertThat(subject.getTimeStamp()).isEqualTo(now.getTime());
        assertThat(subject.getWidth()).isEqualTo(320);
        assertThat(subject.getHeight()).isEqualTo(50);
        assertThat(subject.getAdTimeoutDelay()).isEqualTo(12);
        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(70000);
        assertThat(subject.getDspCreativeId()).isEqualTo("1534363");
    }

    @Test
    public void addHttpResponse_withMissingWidthHeader_shouldSetWidthTo0() throws Exception {
        httpResponse.addHeader(HEIGHT.getKey(), "25");

        subject.addHttpResponse(httpResponse);

        assertThat(subject.getWidth()).isEqualTo(0);
    }

    @Test
    public void addHttpResponse_withMissingHeightHeader_shouldSetHeightTo0() throws Exception {
        subject.addHttpResponse(httpResponse);

        assertThat(subject.getHeight()).isEqualTo(0);
    }

    @Test
    public void addHttpResponse_withFloatTimeoutDelay_shouldTruncateTimeoutDelay() throws Exception {
        httpResponse.addHeader("X-AdTimeout", "3.14");
        subject.addHttpResponse(httpResponse);
        assertThat(subject.getAdTimeoutDelay()).isEqualTo(3);

        httpResponse = new TestHttpResponseWithHeaders(200, "I ain't got no-body");
        httpResponse.addHeader("X-AdTimeout", "-3.14");
        subject.addHttpResponse(httpResponse);
        assertThat(subject.getAdTimeoutDelay()).isEqualTo(-3);
    }

    @Test
    public void addHttpResponse_withInvalidTimeoutDelay_shouldSetAdTimeoutDelayToNull() throws Exception {
        // no X-AdTimeout header
        subject.addHttpResponse(httpResponse);
        assertThat(subject.getAdTimeoutDelay()).isNull();

        httpResponse = new TestHttpResponseWithHeaders(200, "I ain't got no-body");
        httpResponse.addHeader("X-AdTimeout", "not a number, i promise");
        subject.addHttpResponse(httpResponse);
        assertThat(subject.getAdTimeoutDelay()).isNull();
    }

    @Test
    public void addHttpResponse_shouldSetRefreshTimeToMinimumOf10Seconds() throws Exception {
        httpResponse.addHeader("X-Refreshtime", "0");

        subject.addHttpResponse(httpResponse);
        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(MINIMUM_REFRESH_TIME_MILLISECONDS);
    }

    @Test
    public void addHttpResponse_whenRefreshTimeNotSpecified_shouldResetRefreshTimeTo0Seconds() throws Exception {
        httpResponse.addHeader("X-Refreshtime", "5");
        subject.addHttpResponse(httpResponse);

        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(MINIMUM_REFRESH_TIME_MILLISECONDS);
        httpResponse = new TestHttpResponseWithHeaders(200, "I ain't got no-body");
        // no X-Refreshtime header
        subject.addHttpResponse(httpResponse);

        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(0);
    }

    @Test
    public void cleanup_shouldClearAllFields() throws Exception {
        Date now = new Date();
        TestDateAndTime.getInstance().setNow(now);

        httpResponse.addHeader(AD_TYPE.getKey(), "this is an ad type");
        httpResponse.addHeader(NETWORK_TYPE.getKey(), "network type!");
        httpResponse.addHeader(REDIRECT_URL.getKey(), "redirect url");
        httpResponse.addHeader(CLICKTHROUGH_URL.getKey(), "clickthrough url");
        httpResponse.addHeader(FAIL_URL.getKey(), "fail url");
        httpResponse.addHeader(IMPRESSION_URL.getKey(), "impression url");
        httpResponse.addHeader(WIDTH.getKey(), "320  ");
        httpResponse.addHeader(HEIGHT.getKey(), "  50");
        httpResponse.addHeader(AD_TIMEOUT.getKey(), "  12  ");
        httpResponse.addHeader(REFRESH_TIME.getKey(), "70");
        httpResponse.addHeader(DSP_CREATIVE_ID.getKey(), "1534363");

        subject.addHttpResponse(httpResponse);
        subject.cleanup();

        assertThat(subject.getBroadcastIdentifier()).isEqualTo(0);
        assertThat(subject.getAdUnitId()).isNull();
        assertThat(subject.getResponseString()).isNull();
        assertThat(subject.getAdType()).isNull();
        assertThat(subject.getNetworkType()).isNull();
        assertThat(subject.getRedirectUrl()).isNull();
        assertThat(subject.getClickthroughUrl()).isNull();
        assertThat(subject.getImpressionUrl()).isNull();
        assertThat(subject.getTimeStamp()).isEqualTo(TestDateAndTime.now().getTime());
        assertThat(subject.getWidth()).isEqualTo(0);
        assertThat(subject.getHeight()).isEqualTo(0);
        assertThat(subject.getAdTimeoutDelay()).isNull();
        assertThat(subject.getRefreshTimeMilliseconds()).isEqualTo(60000);
        assertThat(subject.getFailUrl()).isNull();
        assertThat(subject.getDspCreativeId()).isNull();
    }

    @Test
    public void extractFromMap_shouldReturnValidAdConfiguration() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AdFetcher.AD_CONFIGURATION_KEY, subject);

        AdConfiguration returnValue = AdConfiguration.extractFromMap(map);

        assertThat(returnValue).isEqualTo(subject);
    }

    @Test
    public void extractFromMap_withNullMap_shouldReturnNull() throws Exception {
        AdConfiguration returnValue = AdConfiguration.extractFromMap(null);

        assertThat(returnValue).isEqualTo(null);
    }

    @Test
    public void extractFromMap_withNonAdConfigurationObjectInMap_shouldReturnNull() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(AdFetcher.AD_CONFIGURATION_KEY, "not_an_ad_configuration");

        AdConfiguration returnValue = AdConfiguration.extractFromMap(map);

        assertThat(returnValue).isEqualTo(null);
    }
}
