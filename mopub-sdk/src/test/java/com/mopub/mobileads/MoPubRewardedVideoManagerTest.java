package com.mopub.mobileads;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.HashMap;
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

    public static final String MOPUB_REWARD = "mopub_reward";
    public static final String REWARDED_CURRENCY_NAME = "Coins";
    public static final String REWARDED_CURRENCY_AMOUNT_STR = "15";
    public static final String SINGLE_REWARDED_CURRENCY_JSON =
            "{\"rewards\": [ { \"name\": \"Coins\", \"amount\": 25 } ] }";
    public static final String MULTI_REWARDED_CURRENCIES_JSON =
            "{\n" +
                    "  \"rewards\": [\n" +
                    "    { \"name\": \"Coins\", \"amount\": 8 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 1 },\n" +
                    "    { \"name\": \"Diamonds\", \"amount\": 10 },\n" +
                    "    { \"name\": \"Energy\", \"amount\": 20 }\n" +
                    "  ]\n" +
                    "}\n";
    public static final String TEST_CUSTOM_EVENT_PREF_NAME = "mopubTestCustomEventSettings";
    private static final String CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE = "provided rewarded ad custom data parameter longer than supported";

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    MoPubRewardedVideoListener mockVideoListener;

    private AdRequest.Listener requestListener;
    private AdRequest request;
    private RewardedVideoCompletionRequest rewardedVideoCompletionRequest;
    private Activity mActivity;
    private SharedPreferences mTestCustomEventSharedPrefs;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        MoPubRewardedVideoManager.init(mActivity);
        // The fact that next call fixes issues in multiple tests proves that Robolectric doesn't
        // teardown singletons properly between tests.
        MoPubRewardedVideoManager.updateActivity(mActivity);

        MoPubRewardedVideoManager.setVideoListener(mockVideoListener);

        mTestCustomEventSharedPrefs = SharedPreferencesHelper.getSharedPreferences(
                        mActivity, TEST_CUSTOM_EVENT_PREF_NAME);
        MoPubRewardedVideoManager.setCustomEventSharedPrefs(mTestCustomEventSharedPrefs);

        when(mockRequestQueue.add(any(Request.class))).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Request req = ((Request) invocationOnMock.getArguments()[0]);
                if (req.getClass().equals(AdRequest.class)) {
                    request = (AdRequest) req;
                    requestListener = request.getListener();
                    return null;
                } else if (req.getClass().equals(RewardedVideoCompletionRequest.class)) {
                    rewardedVideoCompletionRequest = (RewardedVideoCompletionRequest) req;
                    return null;
                } else {
                    throw new Exception(String.format("Request object added to RequestQueue can " +
                            "only be of type AdRequest or RewardedVideoCompletionRequest, " +
                            "saw %s instead.", req.getClass()));
                }
            }
        });

        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void tearDown() {
        // Unpause the main looper in case a test terminated while the looper was paused.
        ShadowLooper.unPauseMainLooper();
        MoPubRewardedVideoManager.getRewardedAdData().clear();
        MoPubRewardedVideoManager.getAdRequestStatusMapping().clearMapping();
        mTestCustomEventSharedPrefs.edit().clear().commit();
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
    public void loadVideo_withRequestParameters_shouldGenerateUrlWithKeywords() {
        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", new MoPubRewardedVideoManager.RequestParameters("nonsense;garbage;keywords"));

        verify(mockRequestQueue).add(argThat(new RequestUrlContains(Uri.encode("nonsense;garbage;keywords"))));

        // Finish the request
        requestListener.onErrorResponse(new VolleyError("end test"));
        ShadowLooper.unPauseMainLooper();
    }

    @Test
    public void loadVideo_withCustomerIdInRequestParameters_shouldSetCustomerId() {
        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", new MoPubRewardedVideoManager.RequestParameters("keywords", null, "testCustomerId"));

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

        verify(mockRequestQueue).add(any(AdRequest.class));
    }

    @Test
    public void loadVideo_withCustomEventAlreadyLoaded_shouldNotLoadAnotherVideo() throws Exception {
        final CustomEventRewardedVideo mockCustomEvent = mock(CustomEventRewardedVideo.class);
        MoPubRewardedVideoManager.getRewardedAdData().updateAdUnitCustomEventMapping(
                "testAdUnit", mockCustomEvent, TestCustomEvent.AD_NETWORK_ID);

        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first custom event
        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify the first custom event
        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);
        verify(mockRequestQueue).add(any(Request.class));
        reset(mockVideoListener);

        ShadowLooper.pauseMainLooper();

        // Load the second custom event
        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);

        ShadowLooper.unPauseMainLooper();

        // Verify the first custom event is still available
        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);
        // Make sure the second load does not attempt to load another ad
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void callbackMethods_withNullListener_shouldNotError() {
        // Clients can set RVM null.
        MoPubRewardedVideoManager.setVideoListener(null);

        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        // Triggers a call to MoPubRewardedVideoManager.onRewardedVideoLoadSuccess
        requestListener.onSuccess(testResponse);

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
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setFailoverUrl("fail.url")
                .build();

        MoPubRewardedVideoManager.updateActivity(null);
        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        verify(mockRequestQueue).add(any(AdRequest.class));
        verifyNoMoreInteractions(mockRequestQueue);

        // Clean up the static state we screwed up:
        MoPubRewardedVideoManager.updateActivity(mActivity);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldCallFailCallback() throws Exception {
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setCustomEventClassName("doesn't_Exist")
                .build();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);

        requestListener.onSuccess(testResponse);

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"),
                eq(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void onAdSuccess_noCEFound_shouldLoadFailUrl() {
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setCustomEventClassName("doesn't_Exist")
                .setFailoverUrl("fail.url")
                .build();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);

        assertThat(request.getUrl()).contains("testAdUnit");
        requestListener.onSuccess(testResponse);
        assertThat(request.getUrl()).isEqualTo("fail.url");
        // Clear up the static state :(
        requestListener.onErrorResponse(new VolleyError("reset"));
    }

    @Test
    public void onAdSuccess_shouldInstantiateCustomEvent_shouldLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void onAdSuccess_withLegacyRewardedCurrencyHeaders_shouldMapAdUnitIdToReward_shouldLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedVideoCurrencyName(REWARDED_CURRENCY_NAME)
                .setRewardedVideoCurrencyAmount(REWARDED_CURRENCY_AMOUNT_STR)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the reward is mapped to the adunit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit")).isNotNull();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit").getLabel()).isEqualTo(REWARDED_CURRENCY_NAME);
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit").getAmount()).isEqualTo(Integer.parseInt(REWARDED_CURRENCY_AMOUNT_STR));
        assertThat(rewardedVideoData.getAvailableRewards("testAdUnit")).isEmpty();
    }

    @Test
    public void onAdSuccess_withMultiRewardedCurrenciesJsonHeader_shouldMapAdUnitToAvailableRewards_shouldLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that only available rewards are updated, not the final reward mapped to the adunit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit")).isNull();
        assertThat(rewardedVideoData.getAvailableRewards("testAdUnit").size()).isEqualTo(4);
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Coins", 8)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Diamonds", 1)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Diamonds", 10)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withSingleRewardedCurrencyJsonHeader_shouldMapAdUnitToRewardAndUpdateAvailableRewards_shouldLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(SINGLE_REWARDED_CURRENCY_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the single reward is mapped to the adunit, and it's the only available reward
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit")).isNotNull();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit").getLabel()).isEqualTo("Coins");
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit").getAmount()).isEqualTo(25);
        assertThat(rewardedVideoData.getAvailableRewards("testAdUnit").size()).isEqualTo(1);
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Coins", 25)).isTrue();
    }

    @Test
    public void onAdSuccess_withBothLegacyAndJsonHeaders_shouldIgnoreLegacyHeaders_shouldLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedVideoCurrencyName(REWARDED_CURRENCY_NAME)
                .setRewardedVideoCurrencyAmount(REWARDED_CURRENCY_AMOUNT_STR)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that the legacy headers are ignored, and available rewards are updated from the JSON header
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit")).isNull();
        assertThat(rewardedVideoData.getAvailableRewards("testAdUnit").size()).isEqualTo(4);
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Coins", 8)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Diamonds", 1)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Diamonds", 10)).isTrue();
        assertThat(rewardedVideoData.existsInAvailableRewards("testAdUnit", "Energy", 20)).isTrue();
    }

    @Test
    public void onAdSuccess_withMalformedRewardedCurrenciesJsonHeader_shouldNotUpdateRewardMappings_andNotLoad() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies("not json")
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isFalse();
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"),
                eq(MoPubErrorCode.REWARDED_CURRENCIES_PARSING_ERROR));
        verifyNoMoreInteractions(mockVideoListener);

        // Verify that no reward mappings are updated
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        assertThat(rewardedVideoData.getMoPubReward("testAdUnit")).isNull();
        assertThat(rewardedVideoData.getAvailableRewards("testAdUnit").isEmpty());
    }

    @Test
    public void onAdSuccess_withEmptyServerExtras_shouldStillSaveEmptyMapInSharedPrefs() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestCustomEventSharedPrefs.getAll();
        String testCustomEventClassName = TestCustomEvent.class.getName();

        // Verify that TestCustomEvent has an empty map saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testCustomEventClassName)).isTrue();
        assertThat(networkInitSettings.get(testCustomEventClassName)).isEqualTo("{}");
    }

    @Test
    public void onAdSuccess_withServerExtras_shouldSaveInitParamsInSharedPrefs() {
        Map<String, String> serverExtras = new HashMap<>();
        serverExtras.put("k1", "v1");
        serverExtras.put("k2", "v2");

        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setServerExtras(serverExtras)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

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
    public void onAdSuccess_withNewInitParams_shouldUpdateInitParamsInSharedPrefs() {
        // Put in {"k1":"v1","k2":"v2"} as existing init params.
        mTestCustomEventSharedPrefs.edit().putString(
                TestCustomEvent.class.getName(),
                "{\"k1\":\"v1\",\"k2\":\"v2\"}").commit();

        // New init params are {"k3":"v3"}.
        Map<String, String> serverExtras = new HashMap<>();
        serverExtras.put("k3", "v3");

        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setServerExtras(serverExtras)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        Map<String, ?> networkInitSettings = mTestCustomEventSharedPrefs.getAll();
        String testCustomEventClassName = TestCustomEvent.class.getName();

        // Verify that TestCustomEvent has new init params saved in SharedPrefs.
        assertThat(networkInitSettings.size()).isEqualTo(1);
        assertThat(networkInitSettings.containsKey(testCustomEventClassName)).isTrue();
        assertThat(networkInitSettings.get(testCustomEventClassName)).isEqualTo("{\"k3\":\"v3\"}");
    }

    @Test
    public void onAdSuccess_witNonCustomEventRewardedVideo_shouldNotSaveAnythingInSharedPrefs() {
        Map<String, String> serverExtras = new HashMap<>();
        serverExtras.put("k1", "v1");
        serverExtras.put("k2", "v2");

        // MoPubRewardedVideo does not extend from CustomEventRewardedVideo
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideo")
                .setAdType(AdType.CUSTOM)
                .setServerExtras(serverExtras)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Verify that nothing got saved in SharedPrefs.
        assertThat(mTestCustomEventSharedPrefs.getAll().size()).isEqualTo(0);
    }

    @Test
    public void onAdSuccess_shouldHaveUniqueBroadcastIdsSetForEachCustomEvent() throws Exception {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        // Load the first custom event
        MoPubRewardedVideoManager.loadVideo("testAdUnit1", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Get the first custom event's broadcast id
        TestCustomEvent testCustomEvent1 = (TestCustomEvent)
                MoPubRewardedVideoManager.getRewardedAdData().getCustomEvent("testAdUnit1");
        Long broadcastId1 = (Long) testCustomEvent1.getLocalExtras().get(
                DataKeys.BROADCAST_IDENTIFIER_KEY);
        assertThat(broadcastId1).isNotNull();

        ShadowLooper.pauseMainLooper();

        // Load the second custom event
        MoPubRewardedVideoManager.loadVideo("testAdUnit2", null);
        requestListener.onSuccess(testResponse);

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
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedVideoCurrencyName("currency_name")
                .setRewardedVideoCurrencyAmount("123")
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubReward moPubReward =
                MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward("testAdUnit");
        assertThat(moPubReward.getAmount()).isEqualTo(123);
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
    }
    
    @Test
    public void showVideo_shouldSetHasVideoFalse() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isTrue();
        MoPubRewardedVideoManager.showVideo("testAdUnit");
        verify(mockVideoListener).onRewardedVideoLoadSuccess(eq("testAdUnit"));
        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isFalse();
        verify(mockVideoListener).onRewardedVideoStarted(eq("testAdUnit"));
    }
    
    @Test
    public void showVideo_whenNotHasVideo_shouldFail() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$NoVideoCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"), eq(MoPubErrorCode.NETWORK_NO_FILL));

        assertThat(MoPubRewardedVideoManager.hasVideo("testAdUnit")).isFalse();
        MoPubRewardedVideoManager.showVideo("testAdUnit");
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"), eq(MoPubErrorCode.VIDEO_NOT_AVAILABLE));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenRewardNotSelected_shouldFail() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        // Multiple rewards are available, but a reward is not selected before showing video
        MoPubRewardedVideoManager.showVideo("testAdUnit");
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenValidRewardIsSelected_shouldUpdateRewardMappings() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards("testAdUnit");
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedVideoManager.selectReward("testAdUnit", reward);
                break;
            }
        }

        // AdUnit to MoPubReward mapping
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        MoPubReward moPubReward = rewardedVideoData.getMoPubReward("testAdUnit");
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit");

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedVideoData.getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getLabel()).isEqualTo("Diamonds");
        assertThat(moPubReward.getAmount()).isEqualTo(10);
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenSelectRewardWithWrongAdUnit_shouldFail() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards("testAdUnit");
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select the 10 Diamonds reward, but to a wrong AdUnit
        for (MoPubReward reward : availableRewards) {
            if (reward.getLabel().equals("Diamonds") && reward.getAmount() == 10) {
                MoPubRewardedVideoManager.selectReward("wrongAdUnit", reward);
                break;
            }
        }

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward("testAdUnit")).isNull();

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit");
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withMultiRewardedCurrenciesJsonHeader_whenSelectedRewardIsNotAvailable_shouldFail() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(MULTI_REWARDED_CURRENCIES_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        Set<MoPubReward> availableRewards = MoPubRewardedVideoManager.getAvailableRewards("testAdUnit");
        assertThat(availableRewards.size()).isEqualTo(4);

        // Select a reward that's not in the returned set of available rewards
        MoPubRewardedVideoManager.selectReward("testAdUnit", MoPubReward.success("fake reward", 99));

        // No selected reward is mapped to AdUnit
        assertThat(MoPubRewardedVideoManager.getRewardedAdData().getMoPubReward("testAdUnit")).isNull();

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit");
        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"), eq(MoPubErrorCode.REWARD_NOT_SELECTED));
    }

    @Test
    public void showVideo_withSingleRewardedCurrencyJsonHeader_whenRewardNotSelected_shouldSelectOnlyRewardAutomatically() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedCurrencies(SINGLE_REWARDED_CURRENCY_JSON)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        // There's only one reward in the set of available rewards for this AdUnit
        assertThat(MoPubRewardedVideoManager.getAvailableRewards("testAdUnit").size()).isEqualTo(1);

        // The only reward is automatically mapped to this AdUnit
        RewardedAdData rewardedVideoData = MoPubRewardedVideoManager.getRewardedAdData();
        MoPubReward moPubReward = rewardedVideoData.getMoPubReward("testAdUnit");
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit");

        // CustomEventRewardedAd class to MoPubReward mapping
        moPubReward = rewardedVideoData.getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getLabel()).isEqualTo("Coins");
        assertThat(moPubReward.getAmount()).isEqualTo(25);
    }

    @Test
    public void showVideo_withLegacyRewardedCurrencyHeaders_shouldUpdateLastShownCustomEventRewardMapping() throws Exception {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .setRewardedVideoCurrencyName("currency_name")
                .setRewardedVideoCurrencyAmount("123")
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit");

        MoPubReward moPubReward =
                MoPubRewardedVideoManager.getRewardedAdData().getLastShownMoPubReward(TestCustomEvent.class);
        assertThat(moPubReward.getAmount()).isEqualTo(123);
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
    }

    @Test
    public void showVideo_withCustomDataShorterThanLengthMaximum_shouldNotLogWarning() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit",
                createStringWithLength(MoPubRewardedVideoManager.CUSTOM_DATA_MAX_LENGTH_BYTES - 1));

        for (final ShadowLog.LogItem logItem : ShadowLog.getLogs()) {
            if (logItem.msg.toLowerCase().contains(CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE)) {
                fail(String.format(Locale.US, "Log item '%s' not expected, found.", CUSTOM_DATA_MAX_LENGTH_EXCEEDED_MESSAGE));
            }
        }
    }

    @Test
    public void showVideo_withCustomDataGreaterThanLengthMaximum_shouldLogWarning() {
        AdResponse testResponse = new AdResponse.Builder()
                .setCustomEventClassName("com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setAdType(AdType.CUSTOM)
                .build();

        // Robolectric executes its handlers immediately, so if we want the async behavior we see
        // in an actual app we have to pause the main looper until we're done successfully loading the ad.
        ShadowLooper.pauseMainLooper();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);

        ShadowLooper.unPauseMainLooper();

        MoPubRewardedVideoManager.showVideo("testAdUnit",
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
    public void onAdFailure_shouldCallFailCallback() {
        VolleyError e = new VolleyError("testError!");

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);

        assertThat(request.getUrl()).contains("testAdUnit");
        requestListener.onErrorResponse(e);
        verify(mockVideoListener).onRewardedVideoLoadFailure(anyString(), any(MoPubErrorCode.class));
        verifyNoMoreInteractions(mockVideoListener);
    }

    @Test
    public void chooseReward_shouldReturnMoPubRewardOverNetworkReward() throws Exception {
        MoPubReward moPubReward = MoPubReward.success(MOPUB_REWARD, 123);
        MoPubReward networkReward = MoPubReward.success("network_reward", 456);

        MoPubReward chosenReward =
                MoPubRewardedVideoManager.chooseReward(moPubReward, networkReward);
        assertThat(chosenReward).isEqualTo(moPubReward);
    }

    @Test
    public void chooseReward_withNetworkRewardNotSuccessful_shouldReturnNetworkReward() throws Exception {
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
    public void onRewardedVideoLoadFailure_withExpirationErrorCode_shouldCallFailCallback_shouldNotLoadFailUrl() {
        AdResponse testResponse = new AdResponse.Builder()
                .setAdType(AdType.CUSTOM)
                .setCustomEventClassName(
                        "com.mopub.mobileads.MoPubRewardedVideoManagerTest$TestCustomEvent")
                .setFailoverUrl("fail.url")
                .build();

        MoPubRewardedVideoManager.loadVideo("testAdUnit", null);
        requestListener.onSuccess(testResponse);
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(TestCustomEvent.class,
                TestCustomEvent.AD_NETWORK_ID, MoPubErrorCode.EXPIRED);

        verify(mockVideoListener).onRewardedVideoLoadFailure(eq("testAdUnit"),
                eq(MoPubErrorCode.EXPIRED));
        verifyNoMoreInteractions(mockVideoListener);
        verify(mockRequestQueue).add(any(AdRequest.class));
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

    private static class RequestUrlContains extends ArgumentMatcher<Request> {

        private final String mMustContain;

        RequestUrlContains(String stringToFind) {
            mMustContain = stringToFind;
        }

        @Override
        public boolean matches(final Object argument) {
            return argument instanceof Request
                    && ((Request) argument).getUrl().contains(mMustContain);
        }
    }
}
