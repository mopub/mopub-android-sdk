package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.JsonUtils;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mopub.common.util.ResponseHeader.AD_TYPE;
import static com.mopub.common.util.ResponseHeader.CLICKTHROUGH_URL;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_DATA;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_NAME;
import static com.mopub.common.util.ResponseHeader.CUSTOM_SELECTOR;
import static com.mopub.common.util.ResponseHeader.NATIVE_PARAMS;
import static com.mopub.common.util.ResponseHeader.REDIRECT_URL;
import static com.mopub.common.util.ResponseHeader.SCROLLABLE;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.GOOGLE_PLAY_SERVICES_BANNER;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.HTML_BANNER;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.HTML_INTERSTITIAL;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.MRAID_BANNER;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.MRAID_INTERSTITIAL;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
public class AdLoadTaskTest {

    private AdViewController adViewController;
    private HttpResponse response;
    private String standardExpectedJson;

    @Before
    public void setup() {
        adViewController = mock(AdViewController.class);
        AdConfiguration adConfiguration = mock(AdConfiguration.class);
        stub(adViewController.getAdConfiguration()).toReturn(adConfiguration);
        response = new TestHttpResponseWithHeaders(200, "");
        standardExpectedJson = "{\"Scrollable\":\"false\",\"Redirect-Url\":\"redirect\",\"Clickthrough-Url\":\"clickthrough\",\"Html-Response-Body\":\"%3Chtml%3E%3C%2Fhtml%3E\"}";
    }

    @Test
    public void fromHttpResponse_whenCustomEvent_shouldGetNameAndData() throws Exception {
        String expectedCustomData = "Custom data";
        response.addHeader(AD_TYPE.getKey(), "custom");
        String expectedCustomEventName = "custom event name";
        response.addHeader(CUSTOM_EVENT_NAME.getKey(), expectedCustomEventName);
        response.addHeader(CUSTOM_EVENT_DATA.getKey(), expectedCustomData);

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(expectedCustomEventName);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey())).isEqualTo(expectedCustomData);
    }

    @Test
    public void fromHttpResponse_whenNoCustomEventName_shouldCreateLegacyCustomEventAdLoadTaskWithAHeader() throws Exception {
        String expectedCustomData = "Custom data";
        String expectedHeaderValue = "some stuff";
        response.addHeader(AD_TYPE.getKey(), "custom");
        response.addHeader(CUSTOM_EVENT_DATA.getKey(), expectedCustomData);
        response.addHeader(CUSTOM_SELECTOR.getKey(), expectedHeaderValue);

        AdLoadTask.LegacyCustomEventAdLoadTask customEventTask = (AdLoadTask.LegacyCustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        Header taskHeader = customEventTask.getHeader();
        assertThat(taskHeader).isNotNull();
        assertThat(taskHeader.getName()).isEqualTo(CUSTOM_SELECTOR.getKey());
        assertThat(taskHeader.getValue()).isEqualTo(expectedHeaderValue);
    }

    @Test
    public void fromHttpResponse_whenMraidBanner_shouldCreateAnEncodedJsonString() throws Exception {
        String htmlData = "<html></html>";
        response = new TestHttpResponseWithHeaders(200, htmlData);
        addExpectedResponseHeaders("mraid");

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(MRAID_BANNER.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, standardExpectedJson);
    }

    @Test
    public void fromHttpResponse_whenMraidInterstitial_shouldCreateAnEncodedJsonString() throws Exception {
        String htmlData = "<html></html>";
        response = new TestHttpResponseWithHeaders(200, htmlData);
        addExpectedResponseHeaders("mraid");
        stub(adViewController.getMoPubView()).toReturn(mock(MoPubInterstitial.MoPubInterstitialView.class));

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(MRAID_INTERSTITIAL.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, standardExpectedJson);
    }

    @Test
    public void fromHttpResponse_whenCustomEventDelegate_shouldConvertAdMobToCustomEvent() throws Exception {
        String expectedNativeParams = "{\"this is a json\":\"map\",\"whee\":\"look at me\"}";
        response.addHeader(AD_TYPE.getKey(), "admob_native");
        response.addHeader(NATIVE_PARAMS.getKey(), expectedNativeParams);

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(GOOGLE_PLAY_SERVICES_BANNER.toString());

        String actualNativeParams = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualNativeParams, expectedNativeParams);
    }

    @Test
    public void fromHttpResponse_whenHtmlBanner_shouldConvertToCustomEventBanner() throws Exception {
        String htmlData = "<html></html>";
        response = new TestHttpResponseWithHeaders(200, htmlData);
        addExpectedResponseHeaders("html");

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(HTML_BANNER.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, standardExpectedJson);
    }

    @Test
    public void fromHttpResponse_whenHtmlInterstitial_shouldConvertToCustomEventInterstitial() throws Exception {
        String htmlData = "<html></html>";
        response = new TestHttpResponseWithHeaders(200, htmlData);
        addExpectedResponseHeaders("html");
        stub(adViewController.getMoPubView()).toReturn(mock(MoPubInterstitial.MoPubInterstitialView.class));

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(HTML_INTERSTITIAL.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, standardExpectedJson);
    }

    @Test
    public void fromHttpResponse_whenEntityIsNull_shouldCreateMinimumJsonString() throws Exception {
        String htmlData = "<html></html>";
        String expectedJson = "{\"Scrollable\":\"false\",\"Html-Response-Body\":\"\"}";
        response = new TestHttpResponseWithHeaders(200, htmlData) {
            @Override
            public HttpEntity getEntity() {
                return null;
            }
        };
        response.addHeader(AD_TYPE.getKey(), "html");

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(HTML_BANNER.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, expectedJson);
    }

    @Test
    public void fromHttpResponse_whenScrollableIsOne_shouldBeReflectedInJson() throws Exception {
        String expectedJson = "{\"Scrollable\":\"true\",\"Html-Response-Body\":\"\"}";
        response.addHeader(SCROLLABLE.getKey(), "1");
        response.addHeader(AD_TYPE.getKey(), "html");


        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(HTML_BANNER.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, expectedJson);
    }

    @Test
    public void fromHttpResponse_whenScrollableIsNotSpecified_shouldDefaultToFalseInJson() throws Exception {
        String expectedJson = "{\"Scrollable\":\"false\",\"Html-Response-Body\":\"\"}";
        response.addHeader(AD_TYPE.getKey(), "html");

        AdLoadTask.CustomEventAdLoadTask customEventTask = (AdLoadTask.CustomEventAdLoadTask) AdLoadTask.fromHttpResponse(response, adViewController);
        assertThat(customEventTask.getParamsMap().get(CUSTOM_EVENT_NAME.getKey())).isEqualTo(HTML_BANNER.toString());

        String actualJsonData = customEventTask.getParamsMap().get(CUSTOM_EVENT_DATA.getKey());
        JsonUtils.assertJsonStringMapsEqual(actualJsonData, expectedJson);
    }

    private void addExpectedResponseHeaders(String adType) {
        response.addHeader(SCROLLABLE.getKey(), "0");
        response.addHeader(AD_TYPE.getKey(), adType);
        response.addHeader(REDIRECT_URL.getKey(), "redirect");
        response.addHeader(CLICKTHROUGH_URL.getKey(), "clickthrough");
    }
}
