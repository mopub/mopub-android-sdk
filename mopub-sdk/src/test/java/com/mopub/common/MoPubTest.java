// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.privacy.SyncRequest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
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
import org.robolectric.android.util.concurrent.RoboExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

// If you encounter a VerifyError with PowerMock then you need to set Android Studio to use
// JDK version 7u79 or later. Go to File > Project Structure > [Platform Settings] > SDK to
// change the JDK version.
@RunWith(SdkTestRunner.class)
@Config(sdk = 21)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*", "com.mopub.network.CustomSSLSocketFactory" })
@PrepareForTest({MoPubRewardedVideoManager.class})
public class MoPubTest {

    public static final String INIT_ADUNIT = "b195f8dd8ded45fe847ad89ed1d016da";

    private Activity mActivity;
    private MediationSettings[] mMediationSettings;
    private SdkInitializationListener mockInitializationListener;
    private MoPubRequestQueue mockRequestQueue;
    private SyncRequest.Listener syncListener;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mMediationSettings = new MediationSettings[0];

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
        AsyncTasks.setExecutor(new RoboExecutorService());
    }

    @After
    public void tearDown() throws Exception {
        MoPub.resetMoPub();
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
    public void initializeSdk_withRewardedVideo_shouldCallMoPubRewardedVideoManager() {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).build(),
                mockInitializationListener);

        ShadowLooper.runUiThreadTasks();
        verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedVideo_withMediationSettings_shouldCallMoPubRewardedVideoManager() {
        MoPub.initializeSdk(mActivity,
                new SdkConfiguration.Builder(INIT_ADUNIT).withMediationSettings(mMediationSettings).build(),
                mockInitializationListener);

        ShadowLooper.runUiThreadTasks();
        verify(mockInitializationListener).onInitializationFinished();
        verifyStatic();
        MoPubRewardedVideoManager.init(mActivity, mMediationSettings);
    }

    @Test
    public void initializeSdk_withRewardedVideo_withoutActivity_shouldNotCallMoPubRewardedVideoManager() {
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
        verify(mockInitializationListener);
    }

    @Test
    public void updateActivity_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideoManager.class,
                "updateActivity", Activity.class)).isNotNull();
    }

    @Test
    public void updateActivity_withValidActivity_shouldCallMoPubRewardedVideoManager() {
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
    public void initializeSdk_withOneAdvancedBidder_shouldSetAdvancedBiddingTokens() {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                INIT_ADUNIT).withAdditionalNetwork(
                AdapterConfigurationTestClass.class.getName()).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        ShadowLooper.runUiThreadTasks();
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");
    }

    @Test
    public void initializeSdk_withMultipleInitializations_shouldSetAdvancedBiddingTokensOnce() {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder
                (INIT_ADUNIT).withAdditionalNetwork(
                AdapterConfigurationTestClass.class.getName()).build();

        MoPub.initializeSdk(mActivity, sdkConfiguration, null);

        ShadowLooper.runUiThreadTasks();
        assertThat(MoPub.getAdvancedBiddingTokensJson(mActivity)).isEqualTo(
                "{\"AdvancedBidderTestClassName\":{\"token\":\"AdvancedBidderTestClassToken\"}}");

        // Attempting to initialize twice
        sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT)
                .withAdditionalNetwork(SecondAdapterConfigurationTestClass.class.getName()).build();
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
        ShadowLooper.runUiThreadTasks();

        verify(mockInitializationListener).onInitializationFinished();
    }

    @Test
    public void initializeSdk_withNoLegitimateInterestAllowedValue_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedFalse() throws Exception {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isFalse();
    }

    @Test
    public void initializeSdk_withLegitimateInterestAllowedFalse_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedFalse() throws Exception {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).withLegitimateInterestAllowed(false).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isFalse();
    }

    @Test
    public void initializeSdk_withLegitimateInterestAllowedTrue_shouldCallPersonalInfoManagerSetAllowLegitimateInterest_withLegitimateInterestAllowedTrue() throws Exception {
        MoPub.initializeSdk(mActivity, new SdkConfiguration.Builder(
                INIT_ADUNIT).withLegitimateInterestAllowed(true).build(), null);
        ShadowLooper.runUiThreadTasks();

        final boolean actual = MoPub.shouldAllowLegitimateInterest();

        assertThat(actual).isTrue();
    }

    private static class AdapterConfigurationTestClass extends BaseAdapterConfiguration {
        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "AdvancedBidderTestClassToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "AdvancedBidderTestClassName";
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {

        }
    }

    private static class SecondAdapterConfigurationTestClass implements AdapterConfiguration {
        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "SecondAdvancedBidderTestClassToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "SecondAdvancedBidderTestClassName";
        }

        @Nullable
        @Override
        public Map<String, String> getMoPubRequestOptions() {
            return null;
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {
        }

        @Override
        public void setCachedInitializationParameters(@NonNull final Context context,
                @Nullable final Map<String, String> configuration) {
        }

        @NonNull
        @Override
        public Map<String, String> getCachedInitializationParameters(
                @NonNull final Context context) {
            return new HashMap<>();
        }

        @Override
        public void setMoPubRequestOptions(
                @Nullable final Map<String, String> moPubRequestOptions) {
        }
    }
}
