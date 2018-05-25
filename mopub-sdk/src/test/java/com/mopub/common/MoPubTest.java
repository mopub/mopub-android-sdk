package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.privacy.SyncRequest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.MoPubRewardedVideos;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

// If you encounter a VerifyError with PowerMock then you need to set Android Studio to use
// JDK version 7u79 or later. Go to File > Project Structure > [Platform Settings] > SDK to
// change the JDK version.
@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.mopub.network.CustomSSLSocketFactory" })
@PrepareForTest({MoPubRewardedVideoManager.class})
public class MoPubTest {

    public static final String INIT_ADUNIT = "b195f8dd8ded45fe847ad89ed1d016da";

    private Activity mActivity;
    private MediationSettings[] mMediationSettings;
    private String mAdUnitId;
    private MoPubRewardedVideoListener mockRewardedVideoListener;
    private MoPubRewardedVideoManager.RequestParameters mockRequestParameters;
    private SdkInitializationListener mockInitializationListener;
    private MoPubRequestQueue mockRequestQueue;
    private SyncRequest.Listener syncListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mMediationSettings = new MediationSettings[0];
        mAdUnitId = "123";

        mockRewardedVideoListener = mock(MoPubRewardedVideoListener.class);
        mockRequestParameters = mock(MoPubRewardedVideoManager.RequestParameters.class);
        mockInitializationListener = org.mockito.Mockito.mock(SdkInitializationListener.class);
        mockRequestQueue = org.mockito.Mockito.mock(MoPubRequestQueue.class);
        Networking.setRequestQueueForTesting(mockRequestQueue);
        when(mockRequestQueue.add(any(Request.class))).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Request req = ((Request) invocationOnMock.getArguments()[0]);
                if (req.getClass().equals(SyncRequest.class)) {
                    syncListener = (SyncRequest.Listener) req.getErrorListener();
                    syncListener.onErrorResponse(new VolleyError());
                    return null;
                } else if (req.getClass().equals(TrackingRequest.class)) {
                    return null;
                } else {
                    throw new Exception(String.format("Request object added to RequestQueue can " +
                            "only be of type SyncRequest, " + "saw %s instead.", req.getClass()));
                }
            }
        });

        mockStatic(MoPubRewardedVideoManager.class);

        MoPub.resetBrowserAgent();
    }

    @After
    public void tearDown() throws Exception {
        MoPub.clearAdvancedBidders();
        MoPub.resetBrowserAgent();
        ClientMetadata.clearForTesting();
    }

    @Test
    public void setBrowserAgent_withDefaultValue_shouldNotChangeBrowserAgent_shouldSetOverriddenFlag() {
        MoPub.setBrowserAgent(BrowserAgent.IN_APP);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(BrowserAgent.IN_APP);
        assertThat(MoPub.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test
    public void setBrowserAgent_withNonDefaultValue_shouldChangeBrowserAgent_shouldSetOverriddenFlag() {
        MoPub.setBrowserAgent(BrowserAgent.NATIVE);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(BrowserAgent.NATIVE);
        assertThat(MoPub.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test
    public void setBrowserAgentFromAdServer_whenNotAlreadyOverriddenByClient_shouldSetBrowserAgentFromAdServer() {
        MoPub.setBrowserAgentFromAdServer(BrowserAgent.NATIVE);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(BrowserAgent.NATIVE);
        assertThat(MoPub.isBrowserAgentOverriddenByClient()).isFalse();
    }

    @Test
    public void setBrowserAgentFromAdServer_whenAlreadyOverriddenByClient_shouldNotChangeBrowserAgent() {
        MoPub.setBrowserAgent(BrowserAgent.NATIVE);
        MoPub.setBrowserAgentFromAdServer(BrowserAgent.IN_APP);
        assertThat(MoPub.getBrowserAgent()).isEqualTo(BrowserAgent.NATIVE);
        assertThat(MoPub.isBrowserAgentOverriddenByClient()).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void setBrowserAgent_withNullValue_shouldThrowException() {
        MoPub.setBrowserAgent(null);
    }

    @Test(expected = NullPointerException.class)
    public void setBrowserAgentFromAdServer_withNullValue_shouldThrowException() {
        MoPub.setBrowserAgentFromAdServer(null);
    }

    @Test
    public void initializeSdk_withRewardedVideo_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).build(),
                mockInitializationListener);

        org.mockito.Mockito.verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedVideo_withMediationSettings_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(mMediationSettings).build(),
                mockInitializationListener);

        org.mockito.Mockito.verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedVideo_withNetworksToInit_shouldCallMoPubRewardedVideoManager() throws Exception {
        List<String> stringClassList = new ArrayList<>();
        // This class does not extend from CustomEventRewardedVideo
        stringClassList.add("com.mopub.common.MoPubTest");
        // This class is one that works.
        stringClassList.add("com.mopub.common.MoPubTest$TestCustomEventRewardedVideo");
        // Not a real class, so not added to the list.
        stringClassList.add("not.a.real.Classname");
        // This class is two subclasses from CustomEventRewardedVideo, but it should still be added.
        stringClassList.add("com.mopub.common.MoPubTest$TestInheritedCustomEventRewardedVideo");

        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT)
                        .withNetworksToInit(stringClassList)
                        .withMediationSettings(mMediationSettings)
                        .build(),
                mockInitializationListener);

        org.mockito.Mockito.verify(mockInitializationListener).onInitializationFinished();
        List<Class<? extends CustomEventRewardedVideo>> classList = new ArrayList<>();
        classList.add(TestCustomEventRewardedVideo.class);
        classList.add(TestInheritedCustomEventRewardedVideo.class);
        verifyStatic();
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
        verifyStatic();
        MoPubRewardedVideoManager.initNetworks(mActivity, classList);
    }

    @Test
    public void initializeSdk_withRewardedVideo_withoutActivity_shouldNotCallMoPubRewardedVideoManager() throws Exception {
        // Since we can't verifyStatic with 0 times, we expect this to call the rewarded video
        // manager exactly twice instead of three times since one of the times is with the
        // application context instead of the activity context.
        MoPub.initializeSdk(mActivity.getApplication(),
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(
                        mMediationSettings).build(), mockInitializationListener);

        verifyStatic(times(2));
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void updateActivity_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideoManager.class,
                "updateActivity", Activity.class)).isNotNull();
    }

    @Test
    public void updateActivity_withValidActivity_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.updateActivity(mActivity);

        verifyStatic();
        MoPubRewardedVideoManager.updateActivity(mActivity);
    }

    @Test
    public void setRewardedVideoListener_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "setRewardedVideoListener", MoPubRewardedVideoListener.class)).isNotNull();
    }

    @Test
    public void loadRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "loadRewardedVideo", String.class,
                MoPubRewardedVideoManager.RequestParameters.class,
                MediationSettings[].class)).isNotNull();
    }

    @Test
    public void hasRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "hasRewardedVideo", String.class)).isNotNull();
    }

    @Test
    public void initializeSdk_withOneAdvancedBidder_shouldSetAdvancedBiddingTokens() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                INIT_ADUNIT).withAdvancedBidder(
                        AdvancedBidderTestClass.class).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");
    }

    @Test
    public void initializeSdk_withMultipleInitializations_shouldSetAdvancedBiddingTokensOnce() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder
                (INIT_ADUNIT).withAdvancedBidder(
                        AdvancedBidderTestClass.class).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");

        // Attempting to initialize twice
        sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT)
                .withAdvancedBidder(SecondAdvancedBidderTestClass.class).build();
        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        // This should not do anything, and getAdvancedBiddingTokensJson() should return the
        // original Advanced Bidder.
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");
    }

    @Test
    public void initializeSdk_withCallbackSet_shouldCallCallback() throws Exception {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).build(), mockInitializationListener);

        org.mockito.Mockito.verify(mockInitializationListener).onInitializationFinished();
    }

    private static class AdvancedBidderTestClass implements MoPubAdvancedBidder {

        @Override
        public String getToken(final Context context) {
            return "AdvancedBidderTestClassToken";
        }

        @Override
        public String getCreativeNetworkName() {
            return "AdvancedBidderTestClassName";
        }
    }

    private static class SecondAdvancedBidderTestClass implements MoPubAdvancedBidder {

        @Override
        public String getToken(final Context context) {
            return "SecondAdvancedBidderTestClassToken";
        }

        @Override
        public String getCreativeNetworkName() {
            return "SecondAdvancedBidderTestClassName";
        }
    }

    private static class TestCustomEventRewardedVideo extends CustomEventRewardedVideo {

        public TestCustomEventRewardedVideo(String param) {

        }
        @Nullable
        @Override
        protected LifecycleListener getLifecycleListener() {
            return null;
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
        }

        @NonNull
        @Override
        protected String getAdNetworkId() {
            return "";
        }

        @Override
        protected void onInvalidate() {
        }

        @Override
        protected boolean hasVideoAvailable() {
            return false;
        }

        @Override
        protected void showVideo() {
        }
    }

    private static class TestInheritedCustomEventRewardedVideo extends TestCustomEventRewardedVideo {
        public TestInheritedCustomEventRewardedVideo(String param) {
            super(param);
        }
    }
}
