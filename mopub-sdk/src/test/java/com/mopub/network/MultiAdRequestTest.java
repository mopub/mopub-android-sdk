// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;


import android.app.Activity;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MultiAdRequestTest {
    private static final String ACCEPT_LANGUAGE = "accept-language";

    @Mock private MultiAdRequest.Listener mockListener;
    @Mock private MultiAdResponse mockAdResponse;

    private MultiAdRequest subject;
    private Activity activity;
    private String adUnitId;
    private JSONObject jsonBody;

/*
    {
        "x-next-url": "fail_url",
        "ad-responses": [
        {
            "content": "content_body",
                "metadata": {
                    "content-type": "text/html; charset=UTF-8",
                    "x-adgroupid": "365cd2475e074026b93da14103a36b97",
                    "x-adtype": "html",
                    "x-backgroundcolor": "",
                    "x-banner-impression-min-ms": "",
                    "x-banner-impression-min-pixels": "",
                    "x-browser-agent": -1,
                    "x-clickthrough": "clickthrough_url",
                    "x-creativeid": "d06f9bde98134f76931cdf04951b60dd",
                    "x-custom-event-class-data": "",
                    "x-custom-event-class-name": "",
                    "x-disable-viewability": 3,
                    "x-dspcreativeid": "",
                    "x-format": "",
                    "x-fulladtype": "",
                    "x-height": 50,
                    "x-imptracker": "imptracker_url",
                    "x-before-load-url": "before_load_url",
                    "x-after-load-url": "after_load_url",
                    "x-interceptlinks": "",
                    "x-nativeparams": "",
                    "x-networktype": "",
                    "x-orientation": "",
                    "x-precacherequired": "",
                    "x-refreshtime": 15,
                    "x-rewarded-currencies": "",
                    "x-rewarded-video-completion-url": "",
                    "x-rewarded-video-currency-amount": -1,
                    "x-rewarded-video-currency-name": "",
                    "x-vastvideoplayer": "",
                    "x-video-trackers": "",
                    "x-video-viewability-trackers": "",
                    "x-width": 320
                }
            }
        ]
    }
*/

    @Before
    public void setup() throws JSONException {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        adUnitId = "testAdUnitId";
        subject = new MultiAdRequest("testUrl", AdFormat.BANNER, adUnitId, activity, mockListener);
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.CONTENT_TYPE.getKey(), "text/html; charset=UTF-8");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "html");
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), "clickthrough_url");
        metadata.put(ResponseHeader.IMPRESSION_URL.getKey(), "imptracker_url");
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), "before_load_url");
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), "after_load_url");
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), 15);
        metadata.put(ResponseHeader.HEIGHT.getKey(), 50);
        metadata.put(ResponseHeader.WIDTH.getKey(), 320);

        JSONObject singleAdResponse = new JSONObject();
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), "content_text");
        singleAdResponse.put(ResponseHeader.METADATA.getKey(), metadata);

        // array of JSON objects AdResponse
        JSONArray adResponses = new JSONArray();
        adResponses.put(singleAdResponse);

        // whole response body
        jsonBody = new JSONObject();
        jsonBody.put(ResponseHeader.FAIL_URL.getKey(), "fail_url");
        jsonBody.put(ResponseHeader.AD_RESPONSES.getKey(), adResponses);

    }

    @After
    public void teardown() {
        // Reset our locale for other tests.
        Locale.setDefault(Locale.US);
        //noinspection deprecation
        MoPub.resetBrowserAgent();
    }

    @Test
    public void deliverResponse_shouldCallListenerOnSuccess() {
        subject.deliverResponse(mockAdResponse);
        verify(mockListener).onSuccessResponse(mockAdResponse);
    }

    @Test
    public void deliverResponse_afterCancel_shouldNotCallListener(){
        subject.cancel();
        subject.deliverResponse(mockAdResponse);
        verify(mockListener, never()).onSuccessResponse(mockAdResponse);
    }

    @Test
    public void parseNetworkResponse_withValidData_shouldReturnNonErrorResponse() {
        NetworkResponse testResponse = new NetworkResponse(jsonBody.toString().getBytes());
        final Response<MultiAdResponse> response = subject.parseNetworkResponse(testResponse);
        assert response != null;
        assertThat(response.error).isNull();
        assertThat(response.result).isNotNull();
        assertThat(response.result.hasNext()).isEqualTo(true);
    }

    @Test
    public void parseNetworkResponse_withInvalidData_shouldReturnErrorResponse() {
        NetworkResponse testResponse = new NetworkResponse("invalid_json".getBytes());
        final Response<MultiAdResponse> response = subject.parseNetworkResponse(testResponse);
        assert response != null;
        assertThat(response.error).isNotNull();
        assertThat(response.result).isNull();
    }

    @Test
    public void equals_shouldReturnTrue(){
        MultiAdRequest that = new MultiAdRequest("testUrl", AdFormat.BANNER, adUnitId, activity, mockListener);
        assert(subject.equals(that));
    }

    @Test
    public void equals_shouldReturnFalse(){
        MultiAdRequest that = new MultiAdRequest("testUrl", AdFormat.INTERSTITIAL, adUnitId, activity, mockListener);
        assert(!subject.equals(that));
    }

    @Test
    public void getHeaders_withDefaultLocale_shouldReturnDefaultLanguageCode() {
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(ACCEPT_LANGUAGE, "en-us");

        assertThat(subject.getHeaders()).isEqualTo(expectedHeaders);
    }

    @Test
    public void getHeaders_withUserPreferredLocale_shouldReturnUserPreferredLanguageCode() {
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(ACCEPT_LANGUAGE, "fr-ca");

        // Assume user-preferred locale is fr_CA
        activity.getResources().getConfiguration().locale = Locale.CANADA_FRENCH;

        assertThat(subject.getHeaders()).isEqualTo(expectedHeaders);
    }

    @Test
    public void getHeaders_withUserPreferredLocaleAsNull_shouldReturnDefaultLanguageCode() {
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(ACCEPT_LANGUAGE, "en-us");

        // Assume user-preferred locale is null
        activity.getResources().getConfiguration().locale = null;

        assertThat(subject.getHeaders()).isEqualTo(expectedHeaders);
    }

    @Test
    public void getHeaders_withUserPreferredLanguageAsEmptyString_shouldReturnDefaultLanguageCode() {
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put(ACCEPT_LANGUAGE, "en-us");

        // Assume user-preferred locale's language code is empty string after trimming
        activity.getResources().getConfiguration().locale = new Locale(" ");

        assertThat(subject.getHeaders()).isEqualTo(expectedHeaders);
    }

    @Test
    public void getHeaders_withLocaleLanguageAsEmptyString_shouldNotAddLanguageHeader() {
        Map<String, String> expectedHeaders = Collections.emptyMap();

        // Assume default locale's language code is empty string
        Locale.setDefault(new Locale(""));

        // Assume user-preferred locale's language code is empty string after trimming
        activity.getResources().getConfiguration().locale = new Locale(" ");

        assertThat(subject.getHeaders()).isEqualTo(expectedHeaders);
    }
}
