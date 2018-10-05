// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

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
                    "x-after-load-success-url": "after_load_success_url",
                    "x-after-load-fail-url": "after_load_fail_url",
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

import android.app.Activity;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.HtmlBanner;
import com.mopub.mobileads.MoPubRewardedVideo;
import com.mopub.nativeads.MoPubCustomEventNative;
import com.mopub.nativeads.MoPubCustomEventVideoNative;
import com.mopub.network.MultiAdResponse.ServerOverrideListener;
import com.mopub.volley.NetworkResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MultiAdResponseTest {
    private static final String CONTENT_TYPE = "text/html; charset=UTF-8";
    private static final String FAIL_URL = "fail_url";
    private static final String CLICKTTRACKING_URL = "clickthrough_url";
    private static final String IMPTRACKER_URL = "imptracker_url";
    private static final JSONArray IMPTRACKER_URLS = new JSONArray().put("imptracker_url1").put(
            "imptracker_url2");
    private static final List<String> IMPTRACKER_URLS_LIST =
            Arrays.asList("imptracker_url1", "imptracker_url2");
    private static final String BEFORE_LOAD_URL = "before_load_url";
    private static final String AFTER_LOAD_URL = "after_load_url";
    private static final JSONArray AFTER_LOAD_URLS = new JSONArray().put("after_load_url1").put(
            "after_load_url2");
    private static final List<String> AFTER_LOAD_URLS_LIST =
            Arrays.asList("after_load_url1", "after_load_url2");

    private static final String AFTER_LOAD_SUCCESS_URL = "after_load_success_url";
    private static final JSONArray AFTER_LOAD_SUCCESS_URLS = new JSONArray().put("after_load_success_url1").put(
            "after_load_success_url2");
    private static final List<String> AFTER_LOAD_SUCCESS_URLS_LIST =
            Arrays.asList("after_load_success_url1", "after_load_success_url2");

    private static final String AFTER_LOAD_FAIL_URL = "after_load_fail_url";
    private static final JSONArray AFTER_LOAD_FAIL_URLS = new JSONArray().put("after_load_fail_url1").put(
            "after_load_fail_url2");
    private static final List<String> AFTER_LOAD_FAIL_URLS_LIST =
            Arrays.asList("after_load_fail_url1", "after_load_fail_url2");

    private static final String ADM_VALUE = "adm_value";
    private static final String REQUEST_ID_VALUE = "request_id_value";
    private static final int REFRESH_TIME = 15;
    private static final int HEIGHT = 50;
    private static final int WIDTH = 320;


    private Activity activity;
    private String adUnitId;
    private JSONObject singleAdResponse;

    @Before
    public void setup() throws JSONException {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        adUnitId = "testAdUnitId";

        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.CONTENT_TYPE.getKey(), CONTENT_TYPE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "html");
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), CLICKTTRACKING_URL);
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), IMPTRACKER_URLS);
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URL);
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), AFTER_LOAD_URLS);
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), AFTER_LOAD_SUCCESS_URLS);
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), AFTER_LOAD_FAIL_URLS);
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        metadata.put(ResponseHeader.HEIGHT.getKey(), HEIGHT);
        metadata.put(ResponseHeader.WIDTH.getKey(), WIDTH);

        singleAdResponse = new JSONObject();
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), "content_text");
        singleAdResponse.put(ResponseHeader.METADATA.getKey(), metadata);
    }

    @After
    public void teardown() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void constructor_withSingleAdResponse_shouldSucceed() throws Exception {
        byte[] body = createResponseBody(FAIL_URL, singleAdResponse);
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.next()).isNotNull();
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
    }

    @Test
    public void constructor_withForceGdprSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.FORCE_GDPR_APPLIES.getKey(), 1);

        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onForceGdprApplies();

        assertTrue(subject.hasNext());
        assertNotNull(subject.next());
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
    }

    @Test
    public void constructor_withForceExplicitNoSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.FORCE_EXPLICIT_NO.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onForceExplicitNo("change_reason");
    }

    @Test
    public void constructor_withInvalidateConsentSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.INVALIDATE_CONSENT.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onInvalidateConsent("change_reason");
    }

    @Test
    public void constructor_withReaquireConsentSet_shouldCallListener() throws Exception {
        ServerOverrideListener mockOverrideListener = mock(ServerOverrideListener.class);
        MultiAdResponse.setServerOverrideListener(mockOverrideListener);

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.put(ResponseHeader.REACQUIRE_CONSENT.getKey(), 1);
        jsonObject.put(ResponseHeader.CONSENT_CHANGE_REASON.getKey(), "change_reason");

        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        verify(mockOverrideListener).onReacquireConsent("change_reason");
    }

    @Test(expected = JSONException.class)
    public void constructor_NonJsonBodyShouldThrowException() throws Exception {
        NetworkResponse testResponse = new NetworkResponse("abc".getBytes());
        new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
    }

    @Test
    public void constructor_withResponseClear_shouldThrowNoFill() throws JSONException {
        JSONObject jsonClear = createClearAdResponse();
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        NetworkResponse testResponse = new NetworkResponse(body);

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withResponseClear_withNoRefreshTime_shouldThrowNoFill() throws JSONException {
        JSONObject jsonClear = createClearAdResponse();
        jsonClear.getJSONObject(ResponseHeader.METADATA.getKey()).remove(ResponseHeader.REFRESH_TIME.getKey());
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        NetworkResponse testResponse = new NetworkResponse(body);

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertNull(error.getRefreshTimeMillis());
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withResponseWarmup_shouldThrowException() throws JSONException {
        JSONObject jsonClear = createWarmupAdResponse();
        byte[] body = createResponseBody(FAIL_URL, jsonClear);
        NetworkResponse testResponse = new NetworkResponse(body);

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.WARMING_UP);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withEmptyResponseArray_shouldThrowError_shouldUseDefaultTimeout() throws JSONException {
        byte[] body = createResponseBody(FAIL_URL, null);
        NetworkResponse testResponse = new NetworkResponse(body);

        try {
            new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                MoPubNetworkError error = (MoPubNetworkError) ex;
                assertThat(error.getReason()).isEqualTo(MoPubNetworkError.Reason.NO_FILL);
                assertThat(error.getRefreshTimeMillis()).isEqualTo(Constants.THIRTY_SECONDS_MILLIS);
                return; // success
            }
        }
        assert false;
    }

    @Test
    public void constructor_withTwoAdResponses_withNonEmptyFailUrl_fullTest() throws MoPubNetworkError, JSONException {
        JSONObject secondResponse = new JSONObject(singleAdResponse.toString());
        JSONObject metadata = secondResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), CLICKTTRACKING_URL+"_2");
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), new JSONArray().put("imptracker_url3"));
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URL+"_2");
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_success_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_fail_2"));

        JSONObject jsonObject = createJsonBody(FAIL_URL, singleAdResponse);
        jsonObject.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).put(secondResponse);
        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);

        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
        AdResponse first = subject.next();
        assertNotNull(first);
        assertThat(first.getAdType()).isEqualTo("html");
        assertThat(first.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(first.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(first.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(first.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(first.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(first.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertTrue(subject.hasNext());
        AdResponse second = subject.next();
        assertThat(second.getAdType()).isEqualTo("html");
        assertThat(second.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL+"_2");
        assertThat(second.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL+"_2");
        assertThat(second.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_2"));
        assertThat(second.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_success_2"));
        assertThat(second.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_fail_2"));
        assertThat(second.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList("imptracker_url3"));
        assertThat(subject.getFailURL()).isEqualTo(FAIL_URL);
        assertFalse(subject.hasNext());
        assertFalse(subject.isWaterfallFinished());
    }

    @Test
    public void constructor_withTwoAdResponses_withEmptyFailUrl_fullTest() throws MoPubNetworkError, JSONException {
        JSONObject secondResponse = new JSONObject(singleAdResponse.toString());
        JSONObject metadata = secondResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CLICK_TRACKING_URL.getKey(), CLICKTTRACKING_URL+"_2");
        metadata.put(ResponseHeader.IMPRESSION_URLS.getKey(), new JSONArray().put("imptracker_url3"));
        metadata.put(ResponseHeader.BEFORE_LOAD_URL.getKey(), BEFORE_LOAD_URL+"_2");
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_success_2"));
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), new JSONArray().put(AFTER_LOAD_URL+"_fail_2"));

        JSONObject jsonObject = createJsonBody("", singleAdResponse);
        jsonObject.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).put(secondResponse);
        byte[] body = jsonObject.toString().getBytes();
        NetworkResponse testResponse = new NetworkResponse(body);

        MultiAdResponse subject = new MultiAdResponse(activity, testResponse, AdFormat.BANNER, adUnitId);

        assertThat(subject.hasNext()).isTrue();
        assertThat(subject.getFailURL()).isEqualTo("");
        AdResponse first = subject.next();
        assertNotNull(first);
        assertThat(first.getAdType()).isEqualTo("html");
        assertThat(first.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(first.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(first.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(first.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(first.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(first.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertTrue(subject.hasNext());
        AdResponse second = subject.next();
        assertThat(second.getAdType()).isEqualTo("html");
        assertThat(second.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL+"_2");
        assertThat(second.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL+"_2");
        assertThat(second.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_2"));
        assertThat(second.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_success_2"));
        assertThat(second.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_fail_2"));
        assertThat(second.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList("imptracker_url3"));
        assertThat(subject.getFailURL()).isEqualTo("");
        assertFalse(subject.hasNext());
        assertTrue(subject.isWaterfallFinished());
    }

    @Test
    public void parseNetworkResponse_forBanner_withoutImpTrackingHeaders_shouldSucceed() throws MoPubNetworkError, JSONException {
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(HtmlBanner.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS)).isEmpty();
        assertThat(serverExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS)).isEmpty();
    }

    @Test
    public void parseNetworkResponse_forBanner_withImpTrackingHeaders_shouldSucceed() throws MoPubNetworkError, JSONException {
        // add impression tracking values
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_DIPS.getKey(), 1);
        metadata.put(ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_MS.getKey(), 2);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(HtmlBanner.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS)).isEqualTo("1");
        assertThat(serverExtras.get(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS)).isEqualTo("2");
    }

    @Test
    public void parseNetworkResponse_forBanner_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                FAIL_URL);

        assertThat(subject.getAdType()).isEqualTo(AdType.HTML);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(HtmlBanner.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY)).isEqualToIgnoringCase("content_text");
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test(expected = MoPubNetworkError.class)
    public void parseNetworkResponse_forNatvieStatic_withInvalidContent_throwsException() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);
    }

    @Test
    public void parseNetworkResponse_forNatvieStatic_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT.getKey(), 33);
        metadata.put(ResponseHeader.IMPRESSION_VISIBLE_MS.getKey(), 900);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT)).isEqualTo("33");
        assertThat(serverExtras.get(DataKeys.IMPRESSION_VISIBLE_MS)).isEqualTo("900");
    }

    @Test
    public void parseNetworkResponse_forNatvieStatic_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualTo(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_forNatvieVideo_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT.getKey(), 33);
        metadata.put(ResponseHeader.IMPRESSION_VISIBLE_MS.getKey(), 900);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.VIDEO_NATIVE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.VIDEO_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventVideoNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT)).isEqualTo("33");
        assertThat(serverExtras.get(DataKeys.IMPRESSION_VISIBLE_MS)).isEqualTo("900");
    }

    @Test
    public void parseNetworkResponse_forNatvieVideo_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.VIDEO_NATIVE);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.VIDEO_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventVideoNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_forRewardedVideo_withAdvancedBidding_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.REWARDED_VIDEO);
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        final String rewardedCurrenciesJson = "{\"rewards\": ["
                + "{\"name\": \"Coins\", \"amount\": 8},"
                + "{\"name\": \"Diamonds\", \"amount\": 1},"
                + "{\"name\": \"Diamonds\", \"amount\": 10 },"
                + "{\"name\": \"Energy\", \"amount\": 20}"
                + "]}";
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), rewardedCurrenciesJson);

        metadata.put(ResponseHeader.REWARDED_VIDEO_COMPLETION_URL.getKey(),
                "http://completionUrl");
        metadata.put(ResponseHeader.REWARDED_DURATION.getKey(), "15000");
        metadata.put(ResponseHeader.SHOULD_REWARD_ON_CLICK.getKey(), "1");

        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.REWARDED_VIDEO);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getHeight()).isEqualTo(HEIGHT);
        assertThat(subject.getWidth()).isEqualTo(WIDTH);
        assertThat(subject.getStringBody()).isEqualTo("content_text");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubRewardedVideo.class.getName());
        assertThat(subject.getRewardedCurrencies()).isEqualTo(rewardedCurrenciesJson);
        assertThat(subject.getRewardedVideoCompletionUrl()).isEqualTo(
                "http://completionUrl");
        assertThat(subject.getRewardedDuration()).isEqualTo(15000);
        assertThat(subject.shouldRewardOnClick()).isTrue();
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualToIgnoringCase(CLICKTTRACKING_URL);
        assertNull(serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY));
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }


    @Test
    public void parseNetworkResponse_withInAppBrowserAgent_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), MoPub.BrowserAgent.IN_APP.ordinal());
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.IN_APP);
    }

    @Test
    public void parseNetworkResponse_withNativeBrowserAgent_shouldSucceed() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), MoPub.BrowserAgent.NATIVE.ordinal());
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.NATIVE);
    }

    @Test
    public void parseNetworkResponse_withNullBrowserAgent_shouldDefaultToInApp() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.BROWSER_AGENT.getKey(), null);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.IN_APP);
    }

    @Test
    public void parseNetworkResponse_withUndefinedBrowserAgent_shouldDefaultToInApp() throws MoPubNetworkError, JSONException {
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getBrowserAgent()).isEqualTo(MoPub.BrowserAgent.IN_APP);
    }

    @Test
    public void parseNetworkResponse_withoutRefreshTime_shouldNotIncludeRefreshTime() throws MoPubNetworkError, JSONException {
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.remove(ResponseHeader.REFRESH_TIME.getKey());
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertNull(subject.getRefreshTimeMillis());
    }

    @Test
    public void parseNetworkResponse_withOnlyLegacyImpressionTracker_shouldPopulateImpressionTrackersList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.remove(ResponseHeader.IMPRESSION_URLS.getKey());
        metadata.put(ResponseHeader.IMPRESSION_URL.getKey(), IMPTRACKER_URL);
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(Collections.singletonList(IMPTRACKER_URL));
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualTo(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadUrl_shouldPopulateAfterLoadUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_URL.getKey(), AFTER_LOAD_URL+"_1");
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_URL+"_1"));
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualTo(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadSuccessUrl_shouldPopulateAfterLoadSuccessUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_SUCCESS_URL.getKey(), AFTER_LOAD_SUCCESS_URL+"_1");
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_SUCCESS_URL+"_1"));
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(AFTER_LOAD_FAIL_URLS_LIST);
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualTo(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }

    @Test
    public void parseNetworkResponse_withSingleAfterLoadFailUrl_shouldPopulateAfterLoadFailUrlsList() throws MoPubNetworkError, JSONException {
        singleAdResponse.put(ResponseHeader.CONTENT.getKey(), new JSONObject());
        JSONObject metadata = (JSONObject) singleAdResponse.get(ResponseHeader.METADATA.getKey());
        metadata.put(DataKeys.ADM_KEY, ADM_VALUE);
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.STATIC_NATIVE);
        metadata.put(ResponseHeader.AFTER_LOAD_FAIL_URL.getKey(), AFTER_LOAD_FAIL_URL+"_1");
        NetworkResponse networkResponse = new NetworkResponse(singleAdResponse.toString().getBytes());

        AdResponse subject = MultiAdResponse.parseSingleAdResponse(activity.getApplicationContext(),
                networkResponse,
                singleAdResponse,
                adUnitId,
                AdFormat.BANNER,
                REQUEST_ID_VALUE);

        assertThat(subject.getAdType()).isEqualTo(AdType.STATIC_NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo(adUnitId);
        assertThat(subject.getClickTrackingUrl()).isEqualTo(CLICKTTRACKING_URL);
        assertThat(subject.getImpressionTrackingUrls()).isEqualTo(IMPTRACKER_URLS_LIST);
        assertThat(subject.getBeforeLoadUrl()).isEqualTo(BEFORE_LOAD_URL);
        assertThat(subject.getAfterLoadUrls()).isEqualTo(AFTER_LOAD_URLS_LIST);
        assertThat(subject.getAfterLoadSuccessUrls()).isEqualTo(AFTER_LOAD_SUCCESS_URLS_LIST);
        assertThat(subject.getAfterLoadFailUrls()).isEqualTo(Collections.singletonList(AFTER_LOAD_FAIL_URL+"_1"));
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(REFRESH_TIME * 1000);
        assertThat(subject.getStringBody()).isEqualTo("{}");
        assertThat(subject.getCustomEventClassName()).isEqualTo(MoPubCustomEventNative.class.getName());
        final Map<String, String> serverExtras = subject.getServerExtras();
        assertNotNull(serverExtras);
        assertThat(serverExtras).isNotEmpty();
        assertThat(serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY)).isEqualTo(CLICKTTRACKING_URL);
        assertThat(serverExtras.get(DataKeys.ADM_KEY)).isEqualTo(ADM_VALUE);
    }


    // Utility functions
    private static byte[] createResponseBody(String failURL, JSONObject adResponse) throws JSONException {
        return createJsonBody(failURL, adResponse).toString().getBytes();
    }

    /**
     * @param failURL    test value for failURL
     * @param adResponse ad response JSON for single creative
     * @return JSONObject in the same format like it comes from the server
     * @throws JSONException unlikely to happen
     */
    private static JSONObject createJsonBody(String failURL, JSONObject adResponse) throws JSONException {
        // array of JSON objects AdResponse
        JSONArray adResponses = new JSONArray();
        if (adResponse != null)
            adResponses.put(adResponse);

        // whole response body
        JSONObject jsonBody = new JSONObject();
        jsonBody.put(ResponseHeader.FAIL_URL.getKey(), failURL);
        jsonBody.put(ResponseHeader.AD_RESPONSES.getKey(), adResponses);
        return jsonBody;
    }

    //    {
    //        "metadata": {
    //          "x-adtype": "clear",
    //          "x-backfill": "clear",
    //          "x-refreshtime": 30
    //        }
    //    }
    private static JSONObject createClearAdResponse() throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "clear");
        metadata.put(ResponseHeader.BACKFILL.getKey(), "clear");
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ResponseHeader.METADATA.getKey(), metadata);
        return jsonObject;
    }

    //    {
    //        "metadata": {
    //          "x-adtype": "clear",
    //          "x-backfill": "clear",
    //          "x-refreshtime": 30
    //          "x-warmup": "1",
    //        }
    //    }
    private static JSONObject createWarmupAdResponse() throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(ResponseHeader.AD_TYPE.getKey(), "clear");
        metadata.put(ResponseHeader.BACKFILL.getKey(), "clear");
        metadata.put(ResponseHeader.REFRESH_TIME.getKey(), REFRESH_TIME);
        metadata.put(ResponseHeader.WARMUP.getKey(), 1);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ResponseHeader.METADATA.getKey(), metadata);
        return jsonObject;
    }

}
