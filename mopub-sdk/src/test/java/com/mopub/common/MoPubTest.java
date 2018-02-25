package com.mopub.common;

import android.app.Activity;

import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.MoPubRewardedVideoListener;
import com.mopub.mobileads.MoPubRewardedVideoManager;
import com.mopub.mobileads.MoPubRewardedVideos;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

// If you encounter a VerifyError with PowerMock then you need to set Android Studio to use
// JDK version 7u79 or later. Go to File > Project Structure > [Platform Settings] > SDK to
// change the JDK version.
@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({MoPubRewardedVideoManager.class})
public class MoPubTest {

    private Activity mActivity;
    private MediationSettings[] mMediationSettings;
    private String mAdUnitId;
    private MoPubRewardedVideoListener mockRewardedVideoListener;
    private MoPubRewardedVideoManager.RequestParameters mockRequestParameters;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(Activity.class).create().get();
        mMediationSettings = new MediationSettings[0];
        mAdUnitId = "123";

        mockRewardedVideoListener = mock(MoPubRewardedVideoListener.class);
        mockRequestParameters = mock(MoPubRewardedVideoManager.RequestParameters.class);

        mockStatic(MoPubRewardedVideoManager.class);

        MoPub.resetBrowserAgent();
    }

    @After
    public void tearDown() {
        MoPub.resetBrowserAgent();
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
    public void initializeRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "initializeRewardedVideo", Activity.class, MediationSettings[].class)).isNotNull();
    }

    @Test
    public void initializeRewardedVideo_withValidParameters_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.initializeRewardedVideo(mActivity, mMediationSettings);

        verifyStatic();
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
    public void setRewardedVideoListener_withValidListener_shouldSetMoPubRewardedVideoManagerListener() throws Exception {
        MoPub.setRewardedVideoListener(mockRewardedVideoListener);

        verifyStatic();
        MoPubRewardedVideoManager.setVideoListener(mockRewardedVideoListener);
    }

    @Test
    public void setRewardedVideoListener_withNullListener_shouldCallMoPubRewardedVideoManagerListenerWithNull() throws Exception {
        MoPub.setRewardedVideoListener(null);

        verifyStatic();
        MoPubRewardedVideoManager.setVideoListener(null);
    }

    @Test
    public void loadRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "loadRewardedVideo", String.class,
                MoPubRewardedVideoManager.RequestParameters.class,
                MediationSettings[].class)).isNotNull();
    }

    @Test
    public void loadRewardedVideo_withTwoNonNullParameters_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.loadRewardedVideo(mAdUnitId, mMediationSettings);

        verifyStatic();
        MoPubRewardedVideoManager.loadVideo(mAdUnitId, null, mMediationSettings);
    }

    @Test
    public void loadRewardedVideo_withTwoParameters_withNullMediationSettings_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.loadRewardedVideo(mAdUnitId, (MediationSettings[]) null);

        verifyStatic();
        MoPubRewardedVideoManager.loadVideo(mAdUnitId, null, (MediationSettings[]) null);
    }

    @Test
    public void loadRewardedVideo_withThreeNonNullParameters_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.loadRewardedVideo(mAdUnitId, mockRequestParameters, mMediationSettings);

        verifyStatic();
        MoPubRewardedVideoManager.loadVideo(mAdUnitId, mockRequestParameters, mMediationSettings);
    }

    @Test
    public void loadRewardedVideo_withThreeParameters_withNullRequestParameters_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.loadRewardedVideo(mAdUnitId, null, mMediationSettings);

        verifyStatic();
        MoPubRewardedVideoManager.loadVideo(mAdUnitId, null, mMediationSettings);
    }

    @Test
    public void loadRewardedVideo_withThreeParameters_withNullRequestParameters_withNullMediationSettings_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.loadRewardedVideo(mAdUnitId, null, (MediationSettings[]) null);

        verifyStatic();
        MoPubRewardedVideoManager.loadVideo(mAdUnitId, null, (MediationSettings[]) null);
    }

    @Test
    public void hasRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "hasRewardedVideo", String.class)).isNotNull();
    }

    @Test
    public void hasRewardedVideo_withValidAdUnitId_shouldReturnTrue() throws Exception {
        when(MoPubRewardedVideoManager.hasVideo(mAdUnitId)).thenReturn(true);

        assertThat(MoPub.hasRewardedVideo(mAdUnitId)).isTrue();
    }

    @Test
    public void hasRewardedVideo_withInvalidAdUnitId_shouldReturnFalse() throws Exception {
        assertThat(MoPub.hasRewardedVideo("fakeId")).isFalse();
    }

    @Test
    public void showRewardedVideo_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(MoPubRewardedVideos.class,
                "showRewardedVideo", String.class)).isNotNull();
    }

    @Test
    public void showRewardedVideo_withNonNullAdUnitId_shouldCallMoPubRewardedVideoManager() throws Exception {
        MoPub.showRewardedVideo(mAdUnitId);

        verifyStatic();
        MoPubRewardedVideoManager.showVideo(mAdUnitId);
    }

}
