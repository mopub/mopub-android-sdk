// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;

import com.mopub.common.AdFormat;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MultiAdRequest;
import com.mopub.network.MultiAdResponse;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdLoaderRewardedVideoTest {

    @Mock
    private AdLoader.Listener mockListener;
    @Mock
    private MoPubRequestQueue mockRequestQueue;

    private Activity activity;
    private final String adUnitId = "adUnitId";

    private AdLoaderRewardedVideo subject;
    private TrackingRequest request;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        String url = "test-url";
        subject = new AdLoaderRewardedVideo(url, AdFormat.REWARDED_VIDEO, adUnitId, activity, mockListener);

        when(mockRequestQueue.add(any(Request.class))).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Request req = ((Request) invocationOnMock.getArguments()[0]);
                if (req.getClass().equals(TrackingRequest.class)) {
                    request = (TrackingRequest) req;
                    return null;
                } else if (req.getClass().equals(MultiAdRequest.class)) {
                    // ignore it
                    return null;
                } else {
                    throw new Exception(String.format("Request object added to RequestQueue can " +
                            "only be of type TrackingRequest, saw %s instead.", req.getClass()));
                }
            }
        });

        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void teardown() {
    }

    @Test
    public void loadNextAd_withNoAdResponse_shouldReturnAllEmptyOrNulls() {
        // validation for basic AdLoader
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);

        //validation for all functions in AdLoaderRewardedVideo
        assertThat(subject.getFailurl()).isNull();
        assertThat(subject.getClickUrl()).isNull();
        assertThat(subject.getImpressionUrls()).isEmpty();
        assertThat(subject.getLastDeliveredResponse()).isNull();
    }

    @Test
    public void oneAdResponse_shouldSucceed() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject serverJson = createAdResponseJson();
        JSONObject firstAdJson = serverJson.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject firstAdMetadata = firstAdJson.getJSONObject(ResponseHeader.METADATA.getKey());
        byte[] body = serverJson.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation for basic AdLoader
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        //validation for all functions in AdLoaderRewardedVideo
        assertThat(subject.getFailurl()).isEqualTo(serverJson.getString(ResponseHeader.FAIL_URL.getKey()));
        assertThat(subject.getClickUrl()).isEqualTo(firstAdMetadata.getString(ResponseHeader.CLICK_TRACKING_URL.getKey()));
        assertThat(subject.getImpressionUrls().get(0)).isEqualTo(firstAdMetadata.getString(ResponseHeader.IMPRESSION_URL.getKey()));
        assertThat(subject.getLastDeliveredResponse()).isNotNull();
    }

    @Test
    public void trackImpression_shouldMakeTrackingRequest() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject serverJson = createAdResponseJson();
        byte[] body = serverJson.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation for basic AdLoader
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        // call tracking
        subject.trackImpression(activity);
        //validation for impression tracking request

        assertThat(request).isNotNull();
        assertThat(request.getUrl()).isEqualTo("impression_tracking_url");
    }

    @Test
    public void trackImpression_withImpressionTrackingUrlsList_shouldIgnoreSingleImpressionUrl_shouldFireListOfImpressionUrls() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        final JSONObject serverJson = createAdResponseJson();
        serverJson.getJSONArray("ad-responses").getJSONObject(0).getJSONObject("metadata").put(
                "imptrackers", new JSONArray().put("imp1").put("imp2"));
        byte[] body = serverJson.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation for basic AdLoader
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        // call tracking
        subject.trackImpression(activity);
        //validation for impression tracking request

        assertThat(request).isNotNull();
        assertThat(request.getUrl()).isEqualTo("imp2");
    }

    @Test
    public void trackClick_shouldMakeClickTrackingRequest() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject serverJson = createAdResponseJson();
        byte[] body = serverJson.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation for basic AdLoader
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        // call tracking
        subject.trackClick(activity);

        //validation for 'click' tracking request
        assertThat(request).isNotNull();
        assertThat(request.getUrl()).isEqualTo("click-url");
    }

    // -----------  utils  -----------
    private static Field getPrivateField(final String name) throws NoSuchFieldException {
        Field declaredField = AdLoader.class.getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static JSONObject createAdResponseJson() throws JSONException {
        final String jsonString = "{\n" +
                "  \"ad-responses\": [\n" +
                "    {\n" +
                "      \"content\": \"ad-content-text\",\n" +
                "      \"metadata\": {\n" +
                "        \"content-type\": \"text/html; charset=UTF-8\",\n" +
                "        \"x-ad-timeout-ms\": 0,\n" +
                "        \"x-adgroupid\": \"b4148ea9ed7b4003b9d7c1e61036e0b1\",\n" +
                "        \"x-adtype\": \"rewarded_video\",\n" +
                "        \"x-backgroundcolor\": \"\",\n" +
                "        \"x-banner-impression-min-ms\": \"\",\n" +
                "        \"x-banner-impression-min-pixels\": \"\",\n" +
                "        \"x-before-load-url\": \"\",\n" +
                "        \"x-browser-agent\": -1,\n" +
                "        \"x-clickthrough\": \"click-url\",\n" +
                "        \"x-creativeid\": \"4652bd83d89a40c5a4e276dbf101499f\",\n" +
                "        \"x-custom-event-class-data\": \"\",\n" +
                "        \"x-custom-event-class-name\": \"\",\n" +
                "        \"x-disable-viewability\": 3,\n" +
                "        \"x-dspcreativeid\": \"\",\n" +
                "        \"x-format\": \"\",\n" +
                "        \"x-fulladtype\": \"vast\",\n" +
                "        \"x-height\": -1,\n" +
                "        \"x-imptracker\": \"impression_tracking_url\",\n" +
                "        \"x-interceptlinks\": \"\",\n" +
                "        \"x-nativeparams\": \"\",\n" +
                "        \"x-networktype\": \"\",\n" +
                "        \"x-orientation\": \"l\",\n" +
                "        \"x-precacherequired\": \"1\",\n" +
                "        \"x-refreshtime\": 30,\n" +
                "        \"x-rewarded-currencies\": {\n" +
                "          \"rewards\": [ { \"name\": \"Coins\", \"amount\": 15 } ]\n" +
                "        },\n" +
                "        \"x-rewarded-video-completion-url\": \"\",\n" +
                "        \"x-rewarded-video-currency-amount\": 10,\n" +
                "        \"x-rewarded-video-currency-name\": \"Coins\",\n" +
                "        \"x-vastvideoplayer\": \"\",\n" +
                "        \"x-video-trackers\": \"\",\n" +
                "        \"x-video-viewability-trackers\": \"\",\n" +
                "        \"x-width\": -1\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"x-next-url\": \"next-url\"\n" +
                "}";

        return new JSONObject(jsonString);
    }
}
