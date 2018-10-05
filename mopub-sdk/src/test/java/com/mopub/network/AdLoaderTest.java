// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.AdFormat;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdLoaderTest {
    private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
    private static final String IMPTRACKER_URL = "imptracker_url";
    private static final String BEFORE_LOAD_URL = "before_load_url";
    private static final String AFTER_LOAD_URL = "after_load_url";
    private static final String AFTER_LOAD_SUCCESS_URL = "after_load_success_url";
    private static final String AFTER_LOAD_FAIL_URL = "after_load_fail_url";
    private static final int REFRESH_TIME = 15;
    private static final int HEIGHT = 50;
    private static final int WIDTH = 320;

    @Mock
    private AdLoader.Listener mockListener;
    @Mock
    private MultiAdRequest mockMultiAdRequest;
    @Mock
    private MoPubRequestQueue mockRequestQueue;

    private Activity activity;
    private final String adUnitId = "adUnitId";
    private Map<String, String> headers;

    private AdLoader subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        String url = "test-url";
        subject = new AdLoader(url, AdFormat.BANNER, adUnitId, activity, mockListener);
        headers = new HashMap<>();

        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void teardown() {
    }

    @Test
    public void constructor_initialStateValidation() throws NoSuchFieldException, IllegalAccessException {
        assertThat(subject.isFailed()).isFalse();
        assertThat(subject.isRunning()).isFalse();
        assertThat(getPrivateField("mAdListener").get(subject)).isNotNull();
        assertThat(getPrivateField("mContext").get(subject)).isNotNull();
        assertThat(getPrivateField("mOriginalListener").get(subject)).isEqualTo(mockListener);
        assertThat(getPrivateField("mMultiAdRequest").get(subject)).isNotNull();
        assertThat(getPrivateField("mMultiAdResponse").get(subject)).isNull();
        assertThat(getPrivateField("mLastDeliveredResponse").get(subject)).isNull();
        assertThat(getPrivateField("mHandler").get(subject)).isNotNull();
        assertThat(subject.hasMoreAds()).isTrue();
    }

    @Test
    public void hasMoreAds_whenFailed_returnsFalse() throws NoSuchFieldException, IllegalAccessException {
        // set AdLoader.failed=true;
        Field field = getPrivateField("mFailed");
        field.setBoolean(subject, true);

        assertThat(subject.hasMoreAds()).isFalse();
    }

    @Test
    public void hasMoreAds_makesCallsToMultiAdResponse() throws NoSuchFieldException, IllegalAccessException {
        // set private AdLoader.mMultiAdResponse to mocked object
        MultiAdResponse multiAdResponse = mock(MultiAdResponse.class);
        Field fieldResponse = getPrivateField("mMultiAdResponse");
        fieldResponse.set(subject, multiAdResponse);

        subject.hasMoreAds();

        verify(multiAdResponse).hasNext();
        verify(multiAdResponse).isWaterfallFinished();
    }

    @Test
    public void loadNextAd_whenRunning_returnsOldRequest() throws NoSuchFieldException, IllegalAccessException {
        // set AdLoader.running = true;
        Field field = getPrivateField("mRunning");
        field.setBoolean(subject, true);
        // get current subject.multiAdRequest
        Field fieldRequest = getPrivateField("mMultiAdRequest");
        MultiAdRequest origRequest = (MultiAdRequest) fieldRequest.get(subject);

        MultiAdRequest request = (MultiAdRequest) subject.loadNextAd(null);
        assertThat(origRequest == request).isTrue();
    }

    @Test
    public void loadNextAd_whenFailed_callsHandler_OnErrorresponse() throws NoSuchFieldException, IllegalAccessException {
        // set AdLoader.failed=true;
        Field field = getPrivateField("mFailed");
        field.setBoolean(subject, true);

        subject.loadNextAd(null);

        verify(mockListener).onErrorResponse(any(VolleyError.class));
    }

    @Test
    public void deliverError_callsOriginalListenerOnErrorResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // call private method AdLoader.deliverError()
        Method methodDeliverError = getMethod("deliverError", new Class[]{VolleyError.class});
        methodDeliverError.invoke(subject, mock(VolleyError.class));

        verify(mockListener).onErrorResponse(any(VolleyError.class));
    }

    @Test
    public void deliverResponse_callsOriginalListenerOnSuccess() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // call private method AdLoader.deliverResponse
        Method deliverResponse = getMethod("deliverResponse", new Class[]{AdResponse.class});
        deliverResponse.invoke(subject, mock(AdResponse.class));

        verify(mockListener).onSuccess(any(AdResponse.class));
    }

    @Test
    public void fetchAd_addsRequestToQueue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // call private method AdLoader.fetchAd()
        Method fetchMethod = getMethod("fetchAd", new Class[]{MultiAdRequest.class, Context.class});
        Request<?> request = (Request<?>) fetchMethod.invoke(subject, mockMultiAdRequest, activity);

        RequestQueue requestQueue = Networking.getRequestQueue();
        verify(requestQueue).add(request);
    }

    @Test
    public void oneAdResponseWaterfall_shouldSucceed() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject adResponseJson = createAdResponseJson("trackingUrl", "content_data");
        byte[] body = createResponseBody(null, new JSONObject[]{adResponseJson});
        NetworkResponse testResponse = new NetworkResponse(200, body, headers, false);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isFalse();
    }

    @Test
    public void twoAdResponseWaterfall_shouldSucceed() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject adResponseJson1 = createAdResponseJson("trackingUrl1", "content_1");
        JSONObject adResponseJson2 = createAdResponseJson("trackingUrl2", "content_2");
        byte[] body = createResponseBody(null, new JSONObject[]{adResponseJson1, adResponseJson2});
        NetworkResponse testResponse = new NetworkResponse(200, body, headers, false);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener, times(1)).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        subject.loadNextAd(UNSPECIFIED);
        verify(mockListener, times(2)).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isFalse();
    }

    @Test
    public void twoAdResponseWaterfall_validFailUrl_shouldSucceed() throws JSONException, MoPubNetworkError, NoSuchFieldException, IllegalAccessException {
        JSONObject adResponseJson1 = createAdResponseJson("trackingUrl1", "content_1");
        JSONObject adResponseJson2 = createAdResponseJson("trackingUrl2", "content_2");
        byte[] body = createResponseBody("fail_url", new JSONObject[]{adResponseJson1, adResponseJson2});
        NetworkResponse testResponse = new NetworkResponse(200, body, headers, false);
        MultiAdResponse multiAdResponse = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        // set subject MultiAdResponse
        Field field = getPrivateField("mMultiAdResponse");
        field.set(subject, multiAdResponse);

        // validation
        assertThat(subject.hasMoreAds()).isTrue();
        subject.loadNextAd(null);
        verify(mockListener, times(1)).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();

        subject.loadNextAd(UNSPECIFIED);
        verify(mockListener, times(2)).onSuccess(any(AdResponse.class));
        assertThat(subject.hasMoreAds()).isTrue();
    }

    // -----------  utils  -----------
    private static Field getPrivateField(final String name) throws NoSuchFieldException {
        Field declaredField = AdLoader.class.getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static Method getMethod(final String name, Class[] argClasses) throws NoSuchMethodException {
        Method method = AdLoader.class.getDeclaredMethod(name, argClasses);
        method.setAccessible(true);
        return method;
    }

    // ----------- utils -------------
    private static byte[] createResponseBody(String failURL, JSONObject[] adResponses) throws JSONException {
        return createJsonBody(failURL, adResponses).toString().getBytes();
    }

    /**
     * @param failURL     test value for failURL
     * @param adRespArray ad response JSON for single creative
     * @return JSON in the same format like it comes from the server
     * @throws JSONException unlikely to happen
     */
    private static JSONObject createJsonBody(String failURL, JSONObject[] adRespArray) throws JSONException {
        // array of JSON objects AdResponse
        JSONArray adResponses = new JSONArray();
        for (int i = 0; i < adRespArray.length; i++) {
            adResponses.put(i, adRespArray[i]);
        }

        // whole response body
        JSONObject jsonBody = new JSONObject();
        jsonBody.put(ResponseHeader.FAIL_URL.getKey(), failURL);
        jsonBody.put(ResponseHeader.AD_RESPONSES.getKey(), adResponses);
        return jsonBody;
    }

    private static JSONObject createAdResponseJson(final String trackingUrl, final String content) throws JSONException {
        JSONObject jsonAd = new JSONObject();
        jsonAd.put(ResponseHeader.CONTENT.getKey(), content);
        jsonAd.put(ResponseHeader.METADATA.getKey(), createMetadataJson(trackingUrl));
        return jsonAd;
    }

    private static JSONObject createMetadataJson(final String trackingUrl) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.CONTENT_TYPE.getKey(), CONTENT_TYPE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "html");
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), trackingUrl);
        metadata.put(ResponseHeader.IMPRESSION_URL.getKey(), IMPTRACKER_URL);
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URL);
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), AFTER_LOAD_URL);
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), AFTER_LOAD_SUCCESS_URL);
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), AFTER_LOAD_FAIL_URL);
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        metadata.put(ResponseHeader.HEIGHT.getKey(), HEIGHT);
        metadata.put(ResponseHeader.WIDTH.getKey(), WIDTH);
        return metadata;
    }
}
