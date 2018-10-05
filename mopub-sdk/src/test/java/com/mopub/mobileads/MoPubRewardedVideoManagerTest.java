// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.privacy.SyncRequest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.ResponseHeader;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MultiAdRequest;
import com.mopub.network.MultiAdResponse;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.AuthFailureError;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class
        MoPubRewardedVideoManagerTest {

    private static final String MOPUB_REWARD = "mopub_reward";
    private static final String SINGLE_CURRENCY_NAME = "Coins_old";
    private static final int SINGLE_CURRENCY_AMOUNT = 17;
    private static final String MULTI_CURRENCY_JSON_1 =
            "{\"rewards\": [ { \"name\": \"Coins\", \"amount\": 25 } ] }";
    private static final String MULTI_CURRENCIES_JSON_4 =
            "{\n" +
                    "  \"rewards\": [\n" +
                    "    { \"name\": \"Coins\", \"amount\": 8 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 1 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 10 },\n" +
                    "    { \"name\": \"Energy\", \"amount\": 20 }\n" +
                    "  ]\n" +
                    "}\n";
    private static final String TEST_CUSTOM_EVENT_PREF_NAME = "mopubTestCustomEventSettings";
    private static final String CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE = "provided rewarded ad custom data parameter longer than supported";
    private static final String adUnitId = "testAdUnit";

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    private MoPubRewardedVideoListener mockVideoListener;

    private MultiAdRequest.Listener requestListener;
    private MultiAdRequest request;
    private RewardedVideoCompletionRequest rewardedVideoCompletionRequest;
    private Activity mActivity;
    private SharedPreferences mTestCustomEventSharedPrefs;
    private PersonalInfoManager mockPersonalInfoManager;

    @Before
    public void setup() throws Exception {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();

        new Reflection.MethodBuilder(null, "clearAdvancedBidders")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder("adunit").build(), null);

        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(mActivity, false);
        MoPubRewardedVideoManager.init(mActivity);

        // The fact that next call fixes issues in multiple tests proves that Robolectric doesn't
        // teardown singletons properly between tests.
        MoPubRewardedVideoManager.updateActivity(mActivity);

        MoPubRewardedVideoManager.setVideoListener(mockVideoListener);

        mTestCustomEventSharedPrefs = SharedPreferencesHelper.getSharedPreferences(
                        mActivity, TEST_CUSTOM_EVENT_PREF_NAME);
        MoPubRewardedVideoManager.setCustomEventSharedPrefs(mTestCustomEventSharedPrefs);

        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.UNKNOWN);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockRequestQueue.add(any(Request.class))).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Request req = ((Request) invocationOnMock.getArguments()[0]);
                if (req.getClass().equals(MultiAdRequest.class)) {
                    request = (MultiAdRequest) req;
                    requestListener = request.mListener;
                    return null;
                } else if (req.getClass().equals(RewardedVideoCompletionRequest.class)) {
                    rewardedVideoCompletionRequest = (RewardedVideoCompletionRequest) req;
                    return null;
                } else if(req.getClass().equals(SyncRequest.class)){
                    return null;
                } else if(req.getClass().equals(TrackingRequest.class)){
                    return null;
                } else {
                    throw new Exception(String.format("Request object added to RequestQueue can " +
                            "only be of type MultiAdRequest or RewardedVideoCompletionRequest, " +
                            "saw %s instead.", req.getClass()));
                }
            }
        });

        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void tearDown() throws Exception {
        // Unpause the main looper in case a test terminated while the looper was paused.
        ShadowLooper.unPauseMainLooper();
        MoPubRewardedVideoManager.getRewardedAdData().clear();
        MoPubRewardedVideoManager.getAdRequestStatusMapping().clearMapping();
        mTestCustomEventSharedPrefs.edit().clear().commit();
        MoPubIdentifierTest.clearPreferences(mActivity);
        new Reflection.MethodBuilder(null, "clearAdvancedBidders")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void initNetworks_withEmptySharedPrefs_shouldNotInitAnyNetworks() {
        List<Class<? extends CustomEventRewardedVideo>> networksToInit =
                Arrays.asList(
                        CustomEventRewardedVideo.class,
                        TestCustomEvent.class,
                        NoVideoCustomEvent.class
                );

        List<CustomEventRewardedVideo> initializedNetworksList =
                MoPubRewardedVideoManager.initNetworks(mActivity, networksToInit);

        // Verify that no networks got initialized.
        assertThat(initializedNetworksList.size()).isEqualTo(0);
    }

    @Test
    public void initNetworks_shouldOnlyInitNetworksWithSettingsSavedInSharedPrefs() {
        // Only TestCustomEvent has settings saved in SharedPrefs.
        mTestCustomEventSharedPrefs.edit().putString(
                TestCustomEvent.class.getName(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").commit();

        List<Class<? extends CustomEventRewardedVideo>> networksToInit =
                Arrays.asList(
                        CustomEventRewardedVideo.class,
                        TestCustomEvent.class,
                        NoVideoCustomEvent.class
                );

        List<CustomEventRewardedVideo> networksInitialized =
                MoPubRewardedVideoManager.initNetworks(mActivity, networksToInit);

        // Verify that only TestCustomEvent got initialized.
        assertThat(networksInitialized.size()).isEqualTo(1);
        assertThat(networksInitialized.get(0).getClass().getName())
                .isEqualTo(TestCustomEvent.class.getName());
    }

    @Test
    public void initNetworks_withDuplicatedNetworks_shouldOnlyInitDedupedNetworks() {
        // Only TestCustomEvent has settings saved in SharedPrefs.
        mTestCustomEventSharedPrefs.edit().putString(
                TestCustomEvent.class.getName(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").commit();

        // All networks are duplicated.
        List<Class<? extends CustomEventRewardedVideo>> networksToInit =
                Arrays.asList(
                        CustomEventRewardedVideo.class,
                        TestCustomEvent.class,
                        NoVideoCustomEvent.class,
                        TestCustomEvent.class,
                        NoVideoCustomEvent.class,
                        CustomEventRewardedVideo.class
                );

        List<CustomEventRewardedVideo> networksInitialized =
                MoPubRewardedVideoManager.initNetworks(mActivity, networksToInit);

        // Verify that only TestCustomEvent got initialized, and only once.
        assertThat(networksInitialized.size()).isEqualTo(1);
        assertThat(networksInitialized.get(0).getClass().getName())
                .isEqualTo(TestCustomEvent.class.getName());
    }

    @Test
    public void initNetworks_shouldObeyOrderDuringInit() {
        // Both TestCustomEvent and NoVideoCustomEvent have settings saved in SharedPrefs.
        mTestCustomEventSharedPrefs.edit().putString(
                TestCustomEvent.class.getName(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").commit();
        mTestCustomEventSharedPrefs.edit().putString(
                NoVideoCustomEvent.class.getName(),
                "{\"k3\":\"v3\",\"k4\":\"v4\"}").commit();

        // All networks are duplicated.
        List<Class<? extends CustomEventRewardedVideo>> networksToInit =
                Arrays.asList(
                        NoVideoCustomEvent.class,
                        TestCustomEvent.class,
                        CustomEventRewardedVideo.class,
                        TestCustomEvent.class,
                        CustomEventRewardedVideo.class,
                        NoVideoCustomEvent.class
                );

        List<CustomEventRewardedVideo> networksInitialized =
                MoPubRewardedVideoManager.initNetworks(mActivity, networksToInit);

        // Verify that only NoVideoCustomEvent and TestCustomEvent got initialized,
        // in that order, and each only once.
        assertThat(networksInitialized.size()).isEqualTo(2);
        assertThat(networksInitialized.get(0).getClass().getName())
                .isEqualTo(NoVideoCustomEvent.class.getName());
        assertThat(networksInitialized.get(1).getClass().getName())
                .isEqualTo(TestCustomEvent.class.getName());
    }

    @Test
    public void createRequestParameters_withUserDataKeywordsButNoConsent_shouldNotSetUserDataKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        MoPubRewardedVideoManager.RequestParameters requestParameters = new MoPubRewardedVideoManager.RequestParameters("keywords", "user_data_keywords",null, "testCustomerId");

        assertThat(requestParameters.mKeywords).isEqualTo("keywords");
        assertThat(requestParameters.mUserDataKeywords).isEqualTo(null);
    }

    @Test
    public void createRequestParameters_withUserDataKeywordsWithConsent_shouldSetUserDataKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        MoPubRewardedVideoManager.RequestParameters requestParameters = new MoPubRewardedVideoManager.RequestParameters("keywords", "user_data_keywords", null, "testCustomerId");

        assertThat(requestParameters.mKeywords).isEqualTo("keywords");
        assertThat(requestParameters.mUserDataKeywords).isEqualTo("user_data_keywords");
    }

    @Test
    public void loadVideo_withRequestParameters_shouldGenerateUrlWithKeywords() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", new MoPubRewardedVideoManager.RequestParameters("nonsense;garbage;keywords"));

        verify(mockRequestQueue).add(argThat(new RequestBodyContains("nonsense;garbage;keywords")));

        // Finish the request
        requestListener.onErrorResponse(new VolleyError("end test"));
        ShadowLooper.unPauseMainLooper();
    }

    @Test
    public void loadVideo_withCustomerIdInRequestParameters_shouldSetCustomerId() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", new MoPubRewardedVideoManager.RequestParameters("keywords", "user_data_keywords",null, "testCustomerId"));

        assertThat(MoPubRewardedVideoManager.getRewardedAdData().getCustomerId()).isEqualTo("testCustomerId");

        // Finish the request
        requestListener.onErrorResponse(new VolleyError("end test"));
        ShadowLooper.unPauseMainLooper();
    }

    @Test
    public void loadVideo_withVideoAlreadyShowing_shouldNotLoadVideo() {
        // To simulate that a video is showing
        MoPubRewardedVideoManager.getRewardedAdData().setCurrentlyShowingAdUnitId("testAdUnit");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadVideo_withDifferentVideoAlreadyShowing_shouldLoadVideo() {
        // To simulate that a video is showing
        MoPubRewardedVideoManager.getRewardedAdData().setCurrentlyShowingAdUnitId("testAdUnit");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("anotherTestAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(MultiAdRequest.class));
    }

    @Test
    public void loadVideo_withCustomEventAlreadyLoaded_shouldNotLoadAnotherVideo() throws Exception {
        final CustomEventRewardedVideo mockCustomEvent = mock(CustomEventRewardedVideo.class);
        MoPubRewardedVideoManager.getRewardedAdData().updateAdUnitCustomEventMapping(
                adUnitId, mockCustomEvent, TestCustomEvent.AD_NETWORK_ID);

        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first custom event
        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify the first custom event
        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);
        verify(mockRequestQueue).add(any(Request.class));
        reset(mockVideoListener);

        ShadowLooper.pauseMainLooper();

        // Load the second custom event
        MoPubRewardedVideoManager.loadVideo(adUnitId, null);

        ShadowLooper.unPauseMainLooper();

        // Verify the first custom event is still available
        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);
        // Make sure the second load does not attempt to load another ad
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void callbackMethods_withNullListener_shouldNotError() {
        // Clients can set RVM null.
        MoPubRewardedVideoManager.setVideoListener(null);

        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);

        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        // Triggers a call to MoPubRewardedVideoManager.onRewardedVideoLoadSuccess
        requestListener.onSuccessResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoClicked(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID);
        MoPubRewardedVideoManager.onRewardedVideoStarted(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID);
        MoPubRewardedVideoManager.onRewardedVideoClosed(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID);
        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID,
                MoPubReward.success("test", 111));

        // The test passed because none of the above calls threw an exception even though the listener is null.
    }

    @Test
    public void onAdSuccess_noActivityFound_shouldNotCallFailUrl() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setFailoverUrl("fail.url")
                .build();

        MoPubRewardedVideoManager.updateActivity(null);
        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccessResponse(multiAdResponse);

        verify(mockRequestQueue).add(any(MultiAdRequest.class));
        verifyNoMoreInteractions(mockRequestQueue);

        // Clean up the static state we screwed up:
        MoPubRewardedVideoManager.updateActivity(mActivity);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldCallFailCallback() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "doesn't_Exist");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);

        requestListener.onSuccessResponse(testResponse);

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId),
                eq(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldLoadFailUrl() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail.url");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "doesn't_Exist");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);

        requestListener.onSuccessResponse(testResponse);
        assertThat(request.getUrl()).isEqualTo("fail.url");
        // Clear up the static state :(
        requestListener.onErrorResponse(new VolleyError("reset"));
    }

    @Test
    public void onAdSuccess_shouldInstantiateCustomEvent_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void onAdSuccess_withLegacyRewardedCurrencyHeaders_shouldMapAdUnitIdToReward_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the reward is mapped to the adunit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId)).isNotNull();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId).getLabel()).isEqualTo(SINGLE_CURRENCY_NAME);
        assertThat(rewardedVideoData.getMoPubReward(adUnitId).getAmount()).isEqualTo(SINGLE_CURRENCY_AMOUNT);
        assertThat(rewardedVideoData.getAvailableRewards(adUnitId)).isEmpty();
    }

    @Test
    public void onAdSuccess_withMultiRewardedCurrenciesJsonHeader_shouldMapAdUnitToAvailableRewards_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that only available rewards are updated, not the final reward mapped to the adunit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedVideoData.getAvailableRewards(adUnitId).size()).isEqualTo(4);
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Coins", 8)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Diamonds", 1)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Diamonds", 10)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withSingleRewardedCurrencyJsonHeader_shouldMapAdUnitToRewardAndUpdateAvailableRewards_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the single reward is mapped to the adunit, and it's the only available reward
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId)).isNotNull();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId).getLabel()).isEqualTo("Coins");
        assertThat(rewardedVideoData.getMoPubReward(adUnitId).getAmount()).isEqualTo(15);
        assertThat(rewardedVideoData.getAvailableRewards(adUnitId).size()).isEqualTo(1);
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Coins", 15)).isTrue();
    }

    @Test
    public void onAdSuccess_withBothLegacyAndJsonHeaders_shouldIgnoreLegacyHeaders_shouldLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), MULTI_CURRENCIES_JSON_4);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the legacy headers are ignored, and available rewards are updated from the JSON header
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedVideoData.getAvailableRewards(adUnitId).size()).isEqualTo(4);
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Coins", 8)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Diamonds", 1)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Diamonds", 10)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards(adUnitId, "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withMalformedRewardedCurrenciesJsonHeader_shouldNotUpdateRewardMappings_andNotLoad() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), "not json");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);


        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isFalse();
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId),
                eq(MoPubErrorCode.REWARDED_CURRENCIES_PARSING_ERROR));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that no reward mappings are updated
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward(adUnitId)).isNull();
        assertThat(rewardedVideoData.getAvailableRewards(adUnitId).isEmpty());
    }

    @Test
    public void onAdSuccess_withEmptyServerExtras_shouldStillSaveEmptyMapInSharedPrefs() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestCustomEventSharedPrefs.getAll();
        String testCustomEventClassName = TestCustomEvent.class.getName();

        // Verify that TestCustomEvent has an empty map saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testCustomEventClassName)).isTrue();
        assertThat(networkInitSettings.get(testCustomEventClassName)).isEqualTo("{}");
    }

    @Test
    public void onAdSuccess_withServerExtras_shouldSaveInitParamsInSharedPrefs() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{\"k1\":\"v1\",\"k2\":\"v2\"}");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestCustomEventSharedPrefs.getAll();
        String testCustomEventClassName = TestCustomEvent.class.getName();

        // Verify that TestCustomEvent has init params saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testCustomEventClassName)).isTrue();
        assertThat(networkInitSettings.get(testCustomEventClassName))
                .isEqualTo("{\"k1\":\"v1\",\"k2\":\"v2\"}");
    }

    @Test
    public void onAdSuccess_withNewInitParams_shouldUpdateInitParamsInSharedPrefs() throws JSONException, MoPubNetworkError {
        // Put in {"k1":"v1","k2":"v2"} as existing init params.
        mTestCustomEventSharedPrefs.edit().putString(
                TestCustomEvent.class.getName(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").commit();

        // New init params are {"k3":"v3"}.
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{\"k3\":\"v3\"}");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestCustomEventSharedPrefs.getAll();
        String testCustomEventClassName = TestCustomEvent.class.getName();

        // Verify that TestCustomEvent has new init params saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testCustomEventClassName)).isTrue();
        assertThat(networkInitSettings.get(testCustomEventClassName)).isEqualTo("{\"k3\":\"v3\"}");
    }

    @Test
    public void onAdSuccess_witNonCustomEventRewardedVideo_shouldNotSaveAnythingInSharedPrefs() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideo");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{\"k1\":\"v1\", \"k2\":\"v2\"}");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, "testAdUnit1");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit1", null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify that nothing got saved in SharedPrefs.
        assertThat(mTestCustomEventSharedPrefs.getAll().size()).isEqualTo(0);
    }

    @Test
    public void onAdSuccess_shouldHaveUniqueBroadcastIdsSetForEachCustomEvent() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, "testAdUnit1");

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first custom event
        MoPubRewardedVideoManager.loadVideo("testAdUnit1", null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Get the first custom event's broadcast id
        TestCustomEvent testCustomEvent1 = (TestCustomEvent)
                MoPubRewardedVideoManager.getRewardedAdData().getCustomEvent("testAdUnit1");
        Long broadcastId1 = (Long) testCustomEvent1.getLocalExtras().get(
                DataKeys.BROADCAST_IDENTIFIER_KEY);
        assertThat(broadcastId1).isNotNull();

        ShadowLooper.pauseMainLooper();

        // Load the second custom event
        testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, "testAdUnit2");
        MoPubRewardedVideoManager.loadVideo("testAdUnit2", null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Get the second custom event's broadcast id
        TestCustomEvent testCustomEvent2 = (TestCustomEvent)
                MoPubRewardedVideoManager.getRewardedAdData().getCustomEvent("testAdUnit2");
        Long broadcastId2 = (Long) testCustomEvent2.getLocalExtras().get(
                DataKeys.BROADCAST_IDENTIFIER_KEY);
        assertThat(broadcastId2).isNotNull();

        // Make sure they're different
        assertThat(broadcastId1).isNotEqualTo(broadcastId2);
    }

    @Test
    public void onAdSuccess_shouldUpdateAdUnitRewardMapping() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), SINGLE_CURRENCY_NAME);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), SINGLE_CURRENCY_AMOUNT);
        metadata.remove(ResponseHeader.REWARDED_CURRENCIES.getKey());

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubReward moPubReward =
                MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward(adUnitId);
        assertThat(moPubReward.getAmount()).isEqualTo(SINGLE_CURRENCY_AMOUNT);
        assertThat(moPubReward.getLabel()).isEqualTo(SINGLE_CURRENCY_NAME);
    }

    @Test
    public void onRewardedVideoClosed_shouldSetHasVideoFalse() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        MoPubRewardedVideoManager.showVideo(adUnitId);
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq(adUnitId));
        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isTrue();
        verify(mockVideoListener).onRewardedVideoStarted(eq(adUnitId));
        MoPubRewardedVideoManager.onRewardedVideoClosed(TestCustomEvent.class, adUnitId);
        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isFalse();
    }
    
    @Test
    public void showVideo_whenNotHasVideo_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$NoVideoCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId), eq(MoPubErrorCode.NETWORK_NO_FILL));

        assertThat(MoPubRewardedVideoManager.hasVideo(adUnitId)).isFalse();
        MoPubRewardedVideoManager.showVideo(adUnitId);
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId), eq(MoPubErrorCode.VIDEO_NOT_AVAILABLE));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenRewardNotSelected_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Multiple rewards are available, but a reward is not selected before showing video
        MoPubRewardedVideoManager.showVideo(adUnitId);
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenValidRewardIsSelected_shouldUpdateRewardMappings() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedVideoManager.selectReward(adUnitId, reward);
                break;
            }
        }

        // AdUnit to MoPubReward mapping
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        MoPubReward moPubReward = rewardedVideoData.getMoPubReward(adUnitId);
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId);

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedVideoData.getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenSelectRewardWithWrongAdUnit_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward, but to a wrong AdUnit
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedVideoManager.selectReward("wrongAdUnit", reward);
                break;
            }
        }

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward(adUnitId)).isNull();

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId);
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenSelectedRewardIsNotAvailable_shouldFail() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), new JSONObject(MULTI_CURRENCIES_JSON_4));

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards(adUnitId);
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select a reward that's not in the returned set of available rewards
        MoPubRewardedVideoManager.selectReward(adUnitId, MoPubReward.success("fake reward", 99));

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward(adUnitId)).isNull();

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId);
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withSingleRewardedCurrencyJsonHeader_whenRewardNotSelected_shouldSelectOnlyRewardAutomatically() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), MULTI_CURRENCY_JSON_1);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        // There's only one reward in the set of available rewards for this AdUnit
        assertThat(MoPubRewardedVideoManager.getAvailableRewards(adUnitId).size()).isEqualTo(1);

        // The only reward is automatically mapped to this AdUnit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        MoPubReward moPubReward = rewardedVideoData.getMoPubReward(adUnitId);
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId);

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedVideoData.getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);
    }

    @Test
    public void showVideo_withLegacyRewardedCurrencyHeaders_shouldUpdateLastShownCustomEventRewardMapping() throws Exception {
        JSONObject jsonResponse = createRewardedJsonResponse();
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME.getKey(), "currency_name");
        metadata.put(ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT.getKey(), 123);
        metadata.put(ResponseHeader.REWARDED_CURRENCIES.getKey(), "");

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId);

        MoPubReward moPubReward =
                MoPubRewardedVideoManager.getRewardedAdData().getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getAmount()).isEqualTo(123);
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
    }

    @Test
    public void showVideo_withCustomDataShorterThanLengthMaximum_shouldNotLogWarning() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        when(multiAdResponse.getFailURL()).thenReturn("failUrl");
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId,
                createStringWithLength(MoPubRewardedVideoManager.CUSTOM_DATA_MAX_LENGTH_BYTES - 1));

        for (final ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.msg.toLowerCase().contains(CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE)) {
                fail(String.format(Locale.US, "Log item '%s' not expected, found.", CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE));
            }
        }
    }

    @Test
    public void showVideo_withCustomDataGreaterThanLengthMaximum_shouldLogWarning() {
        MultiAdResponse multiAdResponse = Mockito.mock(MultiAdResponse.class);
        when(multiAdResponse.getFailURL()).thenReturn("failUrl");
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(multiAdResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo(adUnitId,
                createStringWithLength(MoPubRewardedVideoManager.CUSTOM_DATA_MAX_LENGTH_BYTES  + 1));

        for (final ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.msg.toLowerCase().contains(CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE)) {
                // Test passes the first time we see the warning log message
                return;
            }
        }
        fail(String.format(Locale.US, "Expected log item '%s' not found.",
                CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE));
    }

    @Test
    public void onAdFailure_shouldCallFailCallback() throws JSONException {
        VolleyError e = new VolleyError("testError!");

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);

        JSONObject jsonBody = new JSONObject(new String(request.getBody()));
        assertThat(jsonBody.get("id")).isEqualTo(adUnitId);
        requestListener.onErrorResponse(e);
        verify(mockVideoListener).onRewardedVideoLoadFailure(anyString(), any(MoPubErrorCode.class));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void chooseReward_shouldReturnMoPubRewardOverNetworkReward() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        MoPubReward networkReward = MoPubReward.success("network_reward", 456);

        MoPubReward chosenReward =
                MoPubRewardedVideoManager.chooseReward(moPubReward, networkReward);
        assertThat(chosenReward).isEqualTo(moPubReward);
    }

    @Test
    public void chooseReward_withNetworkRewardNotSuccessful_shouldReturnNetworkReward() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        MoPubReward networkReward = MoPubReward.failure();

        MoPubReward chosenReward =
                MoPubRewardedVideoManager.chooseReward(moPubReward, networkReward);
        assertThat(chosenReward).isEqualTo(networkReward);
    }
    
    @Test
    public void onRewardedVideoCompleted_withEmptyServerCompletionUrl_withCurrentlyShowingAdUnitId_shouldNotifyRewardedVideoCompletedForOneAdUnitId() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");
        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit1", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit2", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        // Server completion url empty and custom event has no server reward set

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class, TestCustomEvent.AD_NETWORK_ID,
                moPubReward);
        
        ShadowLooper.unPauseMainLooper();

        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockVideoListener).onRewardedVideoCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1");
    }

    @Test
    public void onRewardedVideoCompleted_withEmptyServerCompletionUrl_withNoCurrentlyShowingAdUnitId_shouldNotifyRewardedVideoCompletedForAllAdUnitIds() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId(null);
        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit1", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit2", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit3", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        // Server completion url empty and custom event has no server reward set

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class, TestCustomEvent.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockVideoListener).onRewardedVideoCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1", "testAdUnit2",
                "testAdUnit3");
    }

    @Test
    public void onRewardedVideoCompleted_withServerCompletionUrl_shouldMakeRewardedVideoCompletionRequest_shouldNotifyRewardedVideoCompleted() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");

        // Set server-side reward, different from moPubReward, and corresponding server completion URL
        rewardedVideoData.updateAdUnitRewardMapping("testAdUnit1", "server-side currency", "777");
        rewardedVideoData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class, TestCustomEvent.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedVideoCompletionRequest.class));
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains("testUrl");
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains("&rcn=server-side%20currency");
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains("&rca=777");
        ArgumentCaptor<Set<String>> rewardedIdsCaptor = ArgumentCaptor.forClass((Class) Set.class);
        verify(mockVideoListener).onRewardedVideoCompleted(rewardedIdsCaptor.capture(),
                eq(moPubReward));
        assertThat(rewardedIdsCaptor.getValue()).containsOnly("testAdUnit1");
    }

    @Test
    public void onRewardedVideoCompleted_shouldMakeRewardedVideoCompletionRequestIncludingClassName() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");

        rewardedVideoData.updateAdUnitCustomEventMapping("testAdUnit1", new TestCustomEvent(),
                TestCustomEvent.AD_NETWORK_ID);
        rewardedVideoData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedVideoCompletionRequest.class));
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains(
                "cec=com.mopub.mobileads.MoPubRewardedVideoManagerTest%24TestCustomEvent");
    }

    @Test
    public void onRewardedVideoCompleted_withCustomData_shouldMakeRewardedVideoCompletionRequestIncludingCustomData() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");

        rewardedVideoData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");
        rewardedVideoData.updateAdUnitToCustomDataMapping("testAdUnit1", "very%=custom@[data]");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedVideoCompletionRequest.class));
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains(
                "&rcd=very%25%3Dcustom%40%5Bdata%5D");
    }

    @Test
    public void onRewardedVideoCompleted_withNullCustomData_shouldMakeRewardedVideoCompletionRequestWithoutCustomData() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");

        rewardedVideoData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");
        rewardedVideoData.updateAdUnitToCustomDataMapping("testAdUnit1", null);

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID, moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedVideoCompletionRequest.class));
        assertThat(rewardedVideoCompletionRequest.getUrl()).doesNotContain("&rcd=");
    }

    @Test
    public void onRewardedVideoCompleted_withServerCompletionUrl_withNullRewardForCurrentlyShowingAdUnitId_shouldMakeRewardedVideoCompletionRequestWithDefaultRewardValues() {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        rewardedVideoData.setCurrentlyShowingAdUnitId("testAdUnit1");

        // Set reward fields to nulls
        rewardedVideoData.updateAdUnitRewardMapping("testAdUnit1", null, null);
        rewardedVideoData.updateAdUnitToServerCompletionUrlMapping("testAdUnit1", "testUrl");

        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.onRewardedVideoCompleted(TestCustomEvent.class, TestCustomEvent.AD_NETWORK_ID,
                moPubReward);

        ShadowLooper.unPauseMainLooper();

        verify(mockRequestQueue).add(any(RewardedVideoCompletionRequest.class));
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains("testUrl");
        // Default reward values
        assertThat(rewardedVideoCompletionRequest.getUrl()).contains("&rcn=&rca=0");
    }

    @Test
    public void onRewardedVideoLoadFailure_withExpirationErrorCode_shouldCallFailCallback_shouldNotLoadFailUrl() throws JSONException, MoPubNetworkError {
        JSONObject jsonResponse = createRewardedJsonResponse();
        jsonResponse.put(ResponseHeader.FAIL_URL.getKey(), "fail_url");
        JSONObject firstResponse = jsonResponse.getJSONArray(ResponseHeader.AD_RESPONSES.getKey()).getJSONObject(0);
        JSONObject metadata = firstResponse.getJSONObject(ResponseHeader.METADATA.getKey());
        metadata.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent");
        metadata.put(ResponseHeader.AD_TYPE.getKey(), AdType.CUSTOM);

        NetworkResponse netResponse = new NetworkResponse(jsonResponse.toString().getBytes());
        MultiAdResponse testResponse = new MultiAdResponse(mActivity, netResponse, AdFormat.REWARDED_VIDEO, adUnitId);

        MoPubRewardedVideoManager.loadVideo(adUnitId, null);
        requestListener.onSuccessResponse(testResponse);
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID, MoPubErrorCode.EXPIRED);

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq(adUnitId),
                eq(MoPubErrorCode.EXPIRED));
        verifyNoMoreInteractions(mockVideoListener);
        verify(mockRequestQueue).add(any(MultiAdRequest.class));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    private String createStringWithLength(int length) {
        if (length < 1) {
            return "";
        }

        char[] chars = new char[length];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public static class TestCustomEvent extends CustomEventRewardedVideo {
        public static final String AD_NETWORK_ID = "id!";

        boolean mPlayable = false;
        private Map<String, Object> mLocalExtras;

        @Nullable
        @Override
        protected LifecycleListener getLifecycleListener() {
            return null;
        }

        @NonNull
        @Override
        protected String getAdNetworkId() {
            return AD_NETWORK_ID;
        }

        @Override
        protected void onInvalidate() {
            mPlayable = false;
        }

        @Override
        protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                @NonNull final Map<String, Object> localExtras,
                @NonNull final Map<String, String> serverExtras) throws Exception {
            return false;
        }

        @Override
        protected void loadWithSdkInitialized(@NonNull final Activity activity,
                @NonNull final Map<String, Object> localExtras,
                @NonNull final Map<String, String> serverExtras) throws Exception {
            // Do nothing because robolectric handlers execute immediately.
            mPlayable = true;
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(TestCustomEvent.class,
                    TestCustomEvent.AD_NETWORK_ID);
            mLocalExtras = localExtras;
        }

        @Override
        protected boolean hasVideoAvailable() {
            return mPlayable;
        }

        @Override
        protected void showVideo() {
            MoPubRewardedVideoManager.onRewardedVideoStarted(TestCustomEvent.class, TestCustomEvent.AD_NETWORK_ID);
        }

        @Nullable
        Map<String, Object> getLocalExtras() {
            return mLocalExtras;
        }
    }

    public static class NoVideoCustomEvent extends TestCustomEvent {
        @Override
        protected void loadWithSdkInitialized(@NonNull final Activity activity,
                @NonNull final Map<String, Object> localExtras,
                @NonNull final Map<String, String> serverExtras) throws Exception {
            mPlayable = false;
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(NoVideoCustomEvent.class, TestCustomEvent.AD_NETWORK_ID, MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private static class RequestBodyContains extends ArgumentMatcher<Request> {

        private final String mMustContain;

        RequestBodyContains(String stringToFind) {
            mMustContain = stringToFind;
        }

        @Override
        public boolean matches(final Object argument) {
            try {
                return argument instanceof Request
                        && new String(((Request) argument).getBody()).contains(mMustContain);
            } catch (AuthFailureError authFailureError) {
                return false;
            }
        }
    }

    private static JSONObject createRewardedJsonResponse() throws JSONException {
        final String jsonString = "{\n" +
                "  \"ad-responses\": [\n" +
                "    {\n" +
                "      \"content\": \"<VAST version=\\\"2.0\\\">\\r\\n  <Ad id=\\\"1\\\">\\r\\n    <InLine>\\r\\n      <AdSystem>MoPub</AdSystem>\\r\\n      <AdTitle>MoPub Video Test Ad</AdTitle>\\r\\n      <Impression>\\r\\n        <![CDATA[https://d30x8mtr3hjnzo.cloudfront.net/client/images/vastimp1x1.png?1519938200329]]>\\r\\n      </Impression>\\r\\n      <Creatives>\\r\\n        <Creative>\\r\\n          <Linear>\\r\\n            <Duration>00:00:30</Duration>\\r\\n            <VideoClicks>\\r\\n              <ClickThrough>\\r\\n                <![CDATA[mopubnativebrowser://navigate?url=http%3A%2F%2Fwww.mopub.com]]>\\r\\n              </ClickThrough>\\r\\n            </VideoClicks>\\r\\n            <MediaFiles>\\r\\n              <MediaFile delivery=\\\"progressive\\\" type=\\\"video/mp4\\\" bitrate=\\\"325\\\" width=\\\"640\\\" height=\\\"360\\\">\\r\\n                <![CDATA[https://d2al1opqne3nsh.cloudfront.net/videos/corgi_30s_640x360_baseline_30.mp4]]>\\r\\n              </MediaFile>\\r\\n            </MediaFiles>\\r\\n          </Linear>\\r\\n        </Creative>\\r\\n        <Creative>\\r\\n          <CompanionAds>\\r\\n            <Companion width=\\\"640\\\" height=\\\"360\\\">\\r\\n              <StaticResource creativeType=\\\"image/jpeg\\\">\\r\\n                <![CDATA[https://d2al1opqne3nsh.cloudfront.net/images/igetbeggin_640x360.jpg]]>\\r\\n              </StaticResource>\\r\\n              <TrackingEvents>\\r\\n                <Tracking event=\\\"creativeView\\\">\\r\\n                  <![CDATA[https://www.mopub.com/?q=companionTracking640x360]]>\\r\\n                </Tracking>\\r\\n              </TrackingEvents>\\r\\n              <CompanionClickThrough>\\r\\n                <![CDATA[https://www.mopub.com/?q=companionClickThrough640x360]]>\\r\\n              </CompanionClickThrough>\\r\\n            </Companion>\\r\\n          </CompanionAds>\\r\\n        </Creative>\\r\\n      </Creatives>\\r\\n    </InLine>\\r\\n  </Ad>\\r\\n</VAST> <MP_TRACKING_URLS>  </MP_TRACKING_URLS> \",\n" +
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
//                "        \"x-clickthrough\": \"http://ads-staging.mopub.com/m/aclk?appid=&cid=4652bd83d89a40c5a4e276dbf101499f&city=San%20Francisco&ckv=2&country_code=US&cppck=E3A19&dev=Android%20SDK%20built%20for%20x86&exclude_adgroups=b4148ea9ed7b4003b9d7c1e61036e0b1&id=920b6145fb1546cf8b5cf2ac34638bb7&is_mraid=0&os=Android&osv=8.0.0&req=5e3d79f17abb48468d95fde17e82f7f6&reqt=1519938200.0&rev=0&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&video_type=\",\n" +
                "        \"x-clickthrough\": \"\",\n" +
                "        \"x-creativeid\": \"4652bd83d89a40c5a4e276dbf101499f\",\n" +
                "        \"x-custom-event-class-data\": \"\",\n" +
                "        \"x-custom-event-class-name\": \"\",\n" +
                "        \"x-disable-viewability\": 3,\n" +
                "        \"x-dspcreativeid\": \"\",\n" +
                "        \"x-format\": \"\",\n" +
                "        \"x-fulladtype\": \"vast\",\n" +
                "        \"x-height\": -1,\n" +
                "        \"x-imptracker\": \"http://ads-staging.mopub.com/m/imp?appid=&cid=4652bd83d89a40c5a4e276dbf101499f&city=San%20Francisco&ckv=2&country_code=US&cppck=6A575&dev=Android%20SDK%20built%20for%20x86&exclude_adgroups=b4148ea9ed7b4003b9d7c1e61036e0b1&id=920b6145fb1546cf8b5cf2ac34638bb7&is_ab=0&is_mraid=0&os=Android&osv=8.0.0&req=5e3d79f17abb48468d95fde17e82f7f6&reqt=1519938200.0&rev=0.000050&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&video_type=\",\n" +
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
//                "  \"x-next-url\": \"http://ads-staging.mopub.com/m/ad?v=6&id=920b6145fb1546cf8b5cf2ac34638bb7&nv=6.1&dn=Google%2CAndroid%20SDK%20built%20for%20x86%2Csdk_gphone_x86&bundle=com.mopub.simpleadsdemo&z=%2B0000&o=p&w=1080&h=1920&sc_a=2.625&mcc=310&mnc=260&iso=us&cn=Android&ct=3&av=4.20.0&udid=ifa%3Abd9022e4-5ced-4af2-8cba-dd15ffa715ee&dnt=0&mr=1&android_perms_ext_storage=0&vv=3&exclude=b4148ea9ed7b4003b9d7c1e61036e0b1&request_id=5e3d79f17abb48468d95fde17e82f7f6&fail=1\"\n" +
                "  \"x-next-url\": \"\"\n" +
                "}";

        return new JSONObject(jsonString);
    }
}
