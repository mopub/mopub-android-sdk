// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.MoPubShadowConnectivityManager;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.mobileads.test.support.TestBaseAdFactory;
import com.mopub.mobileads.test.support.TestFullscreenAdAdapterFactory;
import com.mopub.mobileads.test.support.ThreadUtils;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.ImpressionData;
import com.mopub.network.ImpressionListener;
import com.mopub.network.ImpressionsEmitter;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MultiAdRequest;
import com.mopub.network.Networking;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class, MoPubShadowConnectivityManager.class})
public class AdViewControllerTest {

    private static final int[] HTML_ERROR_CODES = new int[]{400, 401, 402, 403, 404, 405, 407, 408,
            409, 410, 411, 412, 413, 414, 415, 416, 417, 500, 501, 502, 503, 504, 505};

    private static final String mAdUnitId = "ad_unit_id";

    private AdViewController subject;
    @Mock private MoPubView mockMoPubView;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private ImpressionData mockImpressionData;
    private Reflection.MethodBuilder methodBuilder;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private MoPubShadowConnectivityManager shadowConnectivityManager;

    private AdResponse response;
    private Activity activity;

    private PersonalInfoManager mockPersonalInfoManager;

    @Before
    public void setup() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        Shadows.shadowOf(activity).grantPermissions(android.Manifest.permission.ACCESS_NETWORK_STATE);

        MoPub.initializeSdk(activity, new SdkConfiguration.Builder("adunit").build(), null);
        Reflection.getPrivateField(MoPub.class, "sSdkInitialized").setBoolean(null, true);

        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.UNKNOWN);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockMoPubView.getAdFormat()).thenReturn(AdFormat.BANNER);
        when(mockMoPubView.getContext()).thenReturn(activity);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        subject = new AdViewController(activity, mockMoPubView);

        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        reset(methodBuilder);
        response = new AdResponse.Builder()
                .setAdUnitId(mAdUnitId)
                .setBaseAdClassName("customEvent")
                .setClickTrackingUrl("clickUrl")
                .setImpressionTrackingUrls(Arrays.asList("impressionUrl1", "impressionUrl2"))
                .setImpressionData(mockImpressionData)
                .setDimensions(320, 50)
                .setAdType("html")
                .setFailoverUrl("failUrl")
                .setResponseBody("testResponseBody")
                .setServerExtras(Collections.<String, String>emptyMap())
                .build();
        shadowTelephonyManager = (MoPubShadowTelephonyManager) Shadows.shadowOf((TelephonyManager) RuntimeEnvironment.application.getSystemService(Context.TELEPHONY_SERVICE));
        shadowConnectivityManager = (MoPubShadowConnectivityManager) Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void cleanup_shouldNotHoldMoPubAdOrUrlGenerator() {
        subject.cleanup();

        assertThat(subject.getMoPubAd()).isNull();
        assertThat(subject.generateAdUrl()).isNull();
    }

    @Test
    public void cleanup_shouldCallInvalidateAdapter() {
        final AdViewController subjectSpy = spy(subject);

        subjectSpy.cleanup();

        verify(subjectSpy).invalidateAdapter();
    }

    @Test
    public void forceRefresh_shouldCallInvalidateAdapter() {
        final AdViewController subjectSpy = spy(subject);

        subjectSpy.forceRefresh();

        verify(subjectSpy).invalidateAdapter();
    }

    @Test
    public void invalidateAdapter_withNonNullAdapter_shouldCallAdapterInvalidate() {
        final AdViewController subjectSpy = spy(subject);
        final AdAdapter mockAdAdapter = mock(AdAdapter.class);
        when(subjectSpy.getAdAdapter()).thenReturn(mockAdAdapter);

        subjectSpy.invalidateAdapter();

        verify(mockAdAdapter).invalidate();
    }

    @Test
    public void setUserDataKeywords_shouldNotSetKeywordIfNoUserConsent() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        subject.setUserDataKeywords("user_data_keywords");

        assertThat(subject.getUserDataKeywords()).isNull();
    }

    @Test
    public void setUserDataKeywords_shouldSetUserDataKeywordsIfUserConsent() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        subject.setUserDataKeywords("user_data_keywords");

        assertThat(subject.getUserDataKeywords()).isEqualTo("user_data_keywords");
    }


    @Test
    public void generateAdUrl_shouldNotSetUserDataKeywordsIfNoUserConsent() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        subject.setAdUnitId("abc123");
        subject.setKeywords("keywords");
        subject.setUserDataKeywords("user_data_keywords");
        subject.setLocation(new Location(""));
        WebViewAdUrlGenerator mUrlGenerator = new WebViewAdUrlGenerator(subject.getContext());

        final String adUrl = subject.generateAdUrl();
        assertThat(getParameterFromRequestUrl(adUrl, "q")).isEqualTo("keywords");
        assertThat(getParameterFromRequestUrl(adUrl, "user_data_keyword_q")).isEqualTo("");
    }

    @Test
    public void generateAdUrl_shouldSetUserDataKeywordsIfUserConsent() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(
                ConsentStatus.EXPLICIT_YES);

        subject.setAdUnitId("abc123");
        subject.setKeywords("keywords");
        subject.setUserDataKeywords("user_data_keywords");
        subject.setLocation(new Location(""));
        WebViewAdUrlGenerator mUrlGenerator = new WebViewAdUrlGenerator(subject.getContext());

        final String adUrl = subject.generateAdUrl();
        assertThat(getParameterFromRequestUrl(adUrl, "q")).isEqualTo("keywords");
        assertThat(getParameterFromRequestUrl(adUrl, "user_data_q")).isEqualTo("user_data_keywords");
    }

    @Test
    public void generateAdUrl_withoutSetRequestedAdSize_shouldSetRequestedAdSizeToZeroZero() {
        subject.setAdUnitId("abc123");
        subject.setKeywords("keywords");
        subject.setUserDataKeywords("user_data_keywords");
        subject.setLocation(new Location(""));
        WebViewAdUrlGenerator mUrlGenerator = new WebViewAdUrlGenerator(subject.getContext());

        final String adUrl = subject.generateAdUrl();
        assertThat(getParameterFromRequestUrl(adUrl, "cw")).isEqualTo("0");
        assertThat(getParameterFromRequestUrl(adUrl, "ch")).isEqualTo("0");
    }

    @Test
    public void generateAdUrl_withSetRequestedAdSize_shouldSetRequestedAdSize() {
        subject.setAdUnitId("abc123");
        subject.setKeywords("keywords");
        subject.setUserDataKeywords("user_data_keywords");
        subject.setLocation(new Location(""));
        subject.setRequestedAdSize(new Point(120, 240));
        WebViewAdUrlGenerator mUrlGenerator = new WebViewAdUrlGenerator(subject.getContext());

        final String adUrl = subject.generateAdUrl();
        assertThat(getParameterFromRequestUrl(adUrl, "cw")).isEqualTo("120");
        assertThat(getParameterFromRequestUrl(adUrl, "ch")).isEqualTo("240");
    }

    @Test
    public void adDidFail_shouldScheduleRefreshTimer_shouldCallMoPubViewAdFailed() {
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.setAdUnitId("abc123");
        subject.adDidFail(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
        verify(mockMoPubView).onAdLoadFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }

    @Test
    public void adDidFail_withNullMoPubView_shouldNotScheduleRefreshTimer_shouldNotCallMoPubViewAdFailed() {
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        // This sets the MoPubView to null
        subject.cleanup();
        subject.adDidFail(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
        verify(mockMoPubView, never()).onAdFailed(any(MoPubErrorCode.class));
    }


    @Test
    public void scheduleRefreshTimer_shouldNotScheduleIfRefreshTimeIsNull() {
        response = response.toBuilder().setRefreshTimeMilliseconds(null).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleIfRefreshTimeIsZero() {
        response = response.toBuilder().setRefreshTimeMilliseconds(0).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimerIfEnabled_shouldCancelOldRefreshAndScheduleANewOne() {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleRefreshIfAutoRefreshIsOff() {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);

        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        subject.setShouldAllowAutoRefresh(false);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimer_whenAdViewControllerNotConfiguredByResponse_shouldHaveDefaultRefreshTime() {
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        ShadowLooper.idleMainLooper(AdViewController.DEFAULT_REFRESH_TIME_MILLISECONDS - 1);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        ShadowLooper.idleMainLooper(1);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void setShouldAllowAutoRefresh_shouldSetCurrentAutoRefreshStatus() {
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();

        subject.setShouldAllowAutoRefresh(false);
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();

        subject.setShouldAllowAutoRefresh(true);
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();
    }

    @Test
    public void pauseRefresh_shouldDisableAutoRefresh() {
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();

        subject.pauseRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();
    }

    @Test
    public void resumeRefresh_afterPauseRefresh_shouldEnableRefresh() {
        subject.pauseRefresh();

        subject.resumeRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();
    }

    @Test
    public void pauseAndResumeRefresh_withShouldAllowAutoRefreshFalse_shouldAlwaysHaveRefreshFalse() {
        subject.setShouldAllowAutoRefresh(false);
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();

        subject.pauseRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();

        subject.resumeRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();
    }

    @Test
    public void multiplePausesBeforeResumeRefresh_shouldEnableAutoRefresh() {
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();

        subject.pauseRefresh();
        subject.pauseRefresh();
        subject.resumeRefresh();

        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();
    }

    @Test
    public void enablingAutoRefresh_afterLoadAd_shouldScheduleNewRefreshTimer() {

        final AdViewController spyAdViewController = spy(subject);

        spyAdViewController.loadAd();
        spyAdViewController.setShouldAllowAutoRefresh(true);
        verify(spyAdViewController).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void enablingAutoRefresh_withoutCallingLoadAd_shouldNotScheduleNewRefreshTimer() {
        final AdViewController spyAdViewController = spy(subject);

        spyAdViewController.setShouldAllowAutoRefresh(true);
        verify(spyAdViewController, never()).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void disablingAutoRefresh_shouldCancelRefreshTimers() {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();

        subject.loadAd();
        subject.setShouldAllowAutoRefresh(true);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        subject.setShouldAllowAutoRefresh(false);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
    }

    @Test
    public void trackImpression_shouldAddToRequestQueue() {
        subject.onAdLoadSuccess(response);
        subject.onAdImpression();

        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl2")));
    }

    @Test
    public void trackImpression_noAdResponse_shouldNotAddToQueue() {
        subject.onAdImpression();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void trackImpression_shouldCallImpressonDataListener() {
        ImpressionListener impressionListener = mock(ImpressionListener.class);
        ImpressionsEmitter.addListener(impressionListener);
        subject.onAdLoadSuccess(response);
        subject.setAdUnitId(mAdUnitId);

        subject.onAdImpression();

        verify(impressionListener).onImpression(response.getAdUnitId(), response.getImpressionData());
        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl2")));
    }

    @Test
    public void registerClick_shouldHttpGetTheClickthroughUrl() {
        subject.onAdLoadSuccess(response);

        subject.registerClick();
        verify(mockRequestQueue).add(argThat(isUrl("clickUrl")));
    }

    @Test
    public void registerClick_NoAdResponse_shouldNotAddToQueue() {
        subject.registerClick();
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void fetchAd_withNullMoPubView_shouldNotMakeRequest() {
        subject.cleanup();
        subject.fetchAd("adUrl", null);
        verify(mockRequestQueue, never()).add(any(MultiAdRequest.class));
    }

    @Test
    public void loadAd_shouldNotLoadWithoutConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Shadows.shadowOf(connectivityManager.getActiveNetworkInfo()).setConnectionStatus(false);
        subject.setAdUnitId("adunit");

        subject.loadAd();
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_shouldNotLoadUrlIfAdUnitIdIsNull() {
        // mAdUnitId is null at initialization
        subject.loadAd();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_withNullAdUnitId_shouldCallAdDidFail_withMissingAdUnitIdError() {
        final AdViewController spyAdViewController = spy(subject);
        // mAdUnitId is null at initialization
        spyAdViewController.loadAd();

        verify(spyAdViewController, atLeastOnce()).adDidFail(any(MoPubErrorCode.class));
    }

    @Test
    public void loadAd_withEmptyAdUnitId_shouldCallAdDidFail_withMissingAdUnitIdError() {
        final AdViewController spyAdViewController = spy(subject);
        spyAdViewController.setAdUnitId("");
        spyAdViewController.loadAd();

        verify(spyAdViewController, atLeastOnce()).adDidFail(MoPubErrorCode.MISSING_AD_UNIT_ID);
    }

    @Test
    public void loadAd_withoutNetworkConnection_shouldCallAdDidFail_withNoConnectionError() {
        ConnectivityManager connectivityManager = (ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Shadows.shadowOf(connectivityManager.getActiveNetworkInfo()).setConnectionStatus(false);

        final AdViewController spyAdViewController = spy(subject);

        spyAdViewController.setAdUnitId("abc123");
        spyAdViewController.loadAd();

        verify(spyAdViewController, atLeastOnce()).adDidFail(MoPubErrorCode.NO_CONNECTION);
    }

    @Test
    public void loadNonJavascript_shouldFetchAd() {
        String url = "https://www.guy.com";
        reset(mockRequestQueue);
        subject.loadNonJavascript(url, null);

        verify(mockRequestQueue).add(argThat(isUrl(url)));
    }

    @Test
    public void loadNonJavascript_whenAlreadyLoading_shouldNotFetchAd() {
        String url = "https://www.guy.com";
        subject.loadNonJavascript(url, null);
        reset(mockRequestQueue);
        subject.loadNonJavascript(url, null);

        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void loadNonJavascript_shouldAcceptNullParameter() {
        subject.loadNonJavascript(null, null);
        // pass
    }

    @Test
    public void loadFailUrl_shouldLoadFailUrl() {
        subject.mAdLoader = new AdLoader("failUrl", AdFormat.BANNER, "adUnitId", activity, mock(AdLoader.Listener.class));

        subject.onAdLoadSuccess(response);
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockRequestQueue).add(argThat(isUrl("failUrl")));
        verify(mockMoPubView, never()).onAdFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void loadFailUrl_shouldAcceptNullErrorCode() {
        subject.loadFailUrl(null);
        // pass
    }

    @Test
    public void loadFailUrl_whenFailUrlIsNull_shouldCallAdDidFail() {
        subject.setAdUnitId("abc123");
        response.toBuilder().setFailoverUrl(null).build();
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockMoPubView).onAdLoadFailed(eq(MoPubErrorCode.NO_FILL));
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void setAdContentView_whenCalledFromWrongUiThread_shouldStillSetContentView() {
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(100);
        ShadowLooper.runUiThreadTasks();

        verify((MoPubView) mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify((MoPubView) mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenCalledAfterCleanUp_shouldNotRemoveViewsAndAddView() {
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.cleanup();
        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(10);
        ShadowLooper.runUiThreadTasks();

        verify((MoPubView) mockMoPubView, never()).removeAllViews();
        verify((MoPubView) mockMoPubView, never()).addView(any(View.class), any(FrameLayout.LayoutParams.class));
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndHasDimensions_shouldSizeAndCenterView() {
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify((MoPubView) mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify((MoPubView) mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndDoesntHaveDimensions_shouldWrapAndCenterView() {
        response = response.toBuilder().setDimensions(null, null).build();
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify((MoPubView) mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify((MoPubView) mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenNotServerDimensions_shouldWrapAndCenterView() {
        subject.onAdLoadSuccess(response);
        View view = mock(View.class);

        subject.setAdContentView(view);

        verify((MoPubView) mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify((MoPubView) mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void onAdLoadSuccess_withResponseContainingRefreshTime_shouldSetNewRefreshTime() {
        assertThat(subject.getRefreshTimeMillis()).isEqualTo(60000);

        response = response.toBuilder().setRefreshTimeMilliseconds(100000).build();
        subject.onAdLoadSuccess(response);

        assertThat(subject.getRefreshTimeMillis()).isEqualTo(100000);
    }

    @Test
    public void onAdLoadSuccess_withResponseNotContainingRefreshTime_shoulSetRefreshTimeToNull() {
        response = response.toBuilder().setRefreshTimeMilliseconds(null).build();
        subject.onAdLoadSuccess(response);

        assertThat(subject.getRefreshTimeMillis()).isNull();
    }

    @Test
    public void onAdLoadError_withMoPubNetworkErrorIncludingRefreshTime_shouldSetNewRefreshTime() {
        subject.setRefreshTimeMillis(54321);

        subject.onAdLoadError(
                new MoPubNetworkError(
                        "network error with specified refresh time",
                        MoPubNetworkError.Reason.NO_FILL,
                        1000)
        );

        assertThat(subject.getRefreshTimeMillis()).isEqualTo(1000);
    }

    @Test
    public void onAdLoadError_withMoPubNetworkErrorNotIncludingRefreshTime_shouldNotModifyRefreshTime() {
        subject.setRefreshTimeMillis(12345);

        subject.onAdLoadError(
                new MoPubNetworkError(
                        "network error that does not specify refresh time",
                        MoPubNetworkError.Reason.UNSPECIFIED)
        );

        assertThat(subject.getRefreshTimeMillis()).isEqualTo(12345);
    }

    @Test
    public void onAdLoadError_withVolleyErrorThatIsNotAnInstanceOfMoPubNetworkError_shouldNotModifyRefreshTime() {
        subject.onAdLoadError(new VolleyError("message"));

        assertThat(subject.getRefreshTimeMillis()).isEqualTo(60000);
    }

    @Test
    public void onAdLoadError_withErrorReasonWarmingUp_shouldReturnErrorCodeWarmup_shouldCallMoPubViewAdFailed() {
        final VolleyError expectedInternalError = new MoPubNetworkError(
                MoPubNetworkError.Reason.WARMING_UP);

        subject.setAdUnitId("abc123");
        subject.onAdLoadError(expectedInternalError);

        verify((MoPubView) mockMoPubView).onAdLoadFailed(MoPubErrorCode.WARMUP);
    }

    @Test
    public void onAdLoadError_whenNoNetworkConnection_shouldReturnErrorCodeNoConnection_shouldCallMoPubViewAdFailed() {
        subject.setAdUnitId("abc123");
        subject.onAdLoadError(new NoConnectionError());

        // DeviceUtils#isNetworkAvailable conveniently returns false due to
        // not having the network permission.
        verify(mockMoPubView).onAdLoadFailed(MoPubErrorCode.NO_CONNECTION);
    }

    @Test
    public void onAdLoadError_withInvalidServerResponse_shouldReturnErrorCodeServerError_shouldCallMoPubViewAdFailed_shouldIncrementBackoffPower() {
        subject.setAdUnitId("abc123");
        for (int htmlErrorCode : HTML_ERROR_CODES) {
            final int oldBackoffPower = subject.mBackoffPower;
            final NetworkResponse errorNetworkResponse = new NetworkResponse(htmlErrorCode, null,
                    null, true, 0);
            final VolleyError volleyError = new VolleyError(errorNetworkResponse);

            subject.onAdLoadError(volleyError);

            assertThat(subject.mBackoffPower).isEqualTo(oldBackoffPower + 1);
        }
        verify(mockMoPubView, times(HTML_ERROR_CODES.length)).onAdLoadFailed(MoPubErrorCode.SERVER_ERROR);
    }

    @Test
    public void loadBaseAd_withNullMoPubAd_shouldCallLoadFailUrl() {
        subject.setAdResponse(response);
        subject.setMoPubAd(null);
        AdViewController subjectSpy = spy(subject);
        subjectSpy.loadBaseAd();
        verify(subjectSpy, times(1)).loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);
    }

    @Test
    public void loadBaseAd_withNullBaseAdClassName_shouldCallLoadFailUrl() {
        response = response.toBuilder().setBaseAdClassName(null).build();
        subject.setAdResponse(response);
        AdViewController subjectSpy = spy(subject);
        subjectSpy.loadBaseAd();
        verify(subjectSpy, times(1)).loadFailUrl(MoPubErrorCode.ADAPTER_NOT_FOUND);
    }

    @Test
    public void loadBaseAd_withEmptyBaseAdClassName_shouldCallLoadFailUrl() {
        response = response.toBuilder().setBaseAdClassName("").build();
        subject.setAdResponse(response);
        AdViewController subjectSpy = spy(subject);
        subjectSpy.loadBaseAd();
        verify(subjectSpy, times(1)).loadFailUrl(MoPubErrorCode.ADAPTER_NOT_FOUND);
    }

    @Test
    public void loadBaseAd_withBannerAdFormat_shouldUseInlineAdAdapter_shouldUseBaseClassName_shouldCallAdapterLoad() {
        when(mockMoPubView.getAdFormat()).thenReturn(AdFormat.BANNER);

        final String baseClassName = "mopub_base_class_inline";
        response = response.toBuilder().setBaseAdClassName(baseClassName).build();
        subject.setAdResponse(response);
        subject.loadBaseAd();

        final AdAdapter adAdapter = subject.getAdAdapter();

        assertThat(adAdapter instanceof InlineAdAdapter);
        assertThat(TestBaseAdFactory.getLatestClassName()).isEqualTo(baseClassName);
        assertThat(adAdapter).isNotNull();
    }

    @Test
    public void loadBaseAd_withInterstitialFormat_shouldUseFullscreenAdAdapter_shouldUseBaseClassName_shouldCallAdapterLoad() {
        when(mockMoPubView.getAdFormat()).thenReturn(AdFormat.INTERSTITIAL);

        final String baseClassName = "mopub_base_class_fullscreen";
        response = response.toBuilder().setBaseAdClassName(baseClassName).build();
        subject.setAdResponse(response);
        subject.loadBaseAd();

        final AdAdapter adAdapter = subject.getAdAdapter();

        assertThat(adAdapter instanceof FullscreenAdAdapter);
        assertThat(TestBaseAdFactory.getLatestClassName()).isEqualTo(baseClassName);
        assertThat(adAdapter).isNotNull();
    }

    @Test
    public void getErrorCodeFromVolleyError_whenNoConnection_shouldReturnErrorCodeNoConnection() {
        final VolleyError noConnectionError = new NoConnectionError();

        // DeviceUtils#isNetworkAvailable conveniently returns false due to
        // not having the internet permission.
        final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                noConnectionError, activity);

        assertThat(errorCode).isEqualTo(MoPubErrorCode.NO_CONNECTION);
    }

    @Test
    public void getErrorCodeFromVolleyError_withNullResponse_whenConnectionValid_shouldReturnErrorCodeUnspecified() {
        final VolleyError noConnectionError = new NoConnectionError();

        Shadows.shadowOf(activity).grantPermissions(Manifest.permission.INTERNET);
        final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                noConnectionError, activity);

        assertThat(errorCode).isEqualTo(MoPubErrorCode.UNSPECIFIED);
    }

    @Test
    public void getErrorCodeFromVolleyError_withInvalidServerResponse_shouldReturnErrorCodeServerError() {
        for (int htmlErrorCode : HTML_ERROR_CODES) {
            final NetworkResponse errorNetworkResponse = new NetworkResponse(htmlErrorCode, null,
                    null, true, 0);
            final VolleyError volleyError = new VolleyError(errorNetworkResponse);

            final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                    volleyError, activity);

            assertThat(errorCode).isEqualTo(MoPubErrorCode.SERVER_ERROR);
        }
    }

    @Test
    public void getErrorCodeFromVolleyError_withErrorReasonWarmingUp_shouldReturnErrorCodeWarmingUp() {
        final VolleyError networkError = new MoPubNetworkError(MoPubNetworkError.Reason.WARMING_UP);

        final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                networkError, activity);

        assertThat(errorCode).isEqualTo(MoPubErrorCode.WARMUP);
    }

    @Test
    public void getErrorCodeFromVolleyError_withErrorReasonNoFill_shouldReturnErrorCodeNoFill() {
        final VolleyError networkError = new MoPubNetworkError(MoPubNetworkError.Reason.NO_FILL);

        final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                networkError, activity);

        assertThat(errorCode).isEqualTo(MoPubErrorCode.NO_FILL);
    }

    @Test
    public void getErrorCodeFromVolleyError_withErrorReasonBadHeaderData_shouldReturnErrorCodeUnspecified() {
        final VolleyError networkError = new MoPubNetworkError(
                MoPubNetworkError.Reason.BAD_HEADER_DATA);

        final MoPubErrorCode errorCode = AdViewController.getErrorCodeFromVolleyError(
                networkError, activity);

        assertThat(errorCode).isEqualTo(MoPubErrorCode.UNSPECIFIED);
    }

    private String getParameterFromRequestUrl(String requestString, String key) {
        Uri requestUri = Uri.parse(requestString);
        String parameter = requestUri.getQueryParameter(key);

        if (TextUtils.isEmpty(parameter)) {
            return "";
        }

        return parameter;
    }
}
