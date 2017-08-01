package com.mopub.mobileads;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.AdFormat;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.ThreadUtils;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
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

import java.util.Collections;
import java.util.Map;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdViewControllerTest {

    private static final int[] HTML_ERROR_CODES = new int[]{400, 401, 402, 403, 404, 405, 407, 408,
            409, 410, 411, 412, 413, 414, 415, 416, 417, 500, 501, 502, 503, 504, 505};

    private AdViewController subject;
    @Mock private MoPubView mockMoPubView;
    @Mock private MoPubRequestQueue mockRequestQueue;
    private Reflection.MethodBuilder methodBuilder;

    private AdResponse response;
    private Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        Shadows.shadowOf(activity).grantPermissions(android.Manifest.permission.ACCESS_NETWORK_STATE);

        when(mockMoPubView.getAdFormat()).thenReturn(AdFormat.BANNER);
        when(mockMoPubView.getContext()).thenReturn(activity);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        subject = new AdViewController(activity, mockMoPubView);

        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        reset(methodBuilder);
        response = new AdResponse.Builder()
                .setCustomEventClassName("customEvent")
                .setClickTrackingUrl("clickUrl")
                .setImpressionTrackingUrl("impressionUrl")
                .setRedirectUrl("redirectUrl")
                .setScrollable(false)
                .setDimensions(320, 50)
                .setAdType("html")
                .setFailoverUrl("failUrl")
                .setResponseBody("testResponseBody")
                .setServerExtras(Collections.<String, String>emptyMap())
                .build();
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void cleanup_shouldNotHoldViewOrUrlGenerator() {
        subject.cleanup();

        assertThat(subject.getMoPubView()).isNull();
        assertThat(subject.generateAdUrl()).isNull();
    }

    @Test
    public void adDidFail_shouldScheduleRefreshTimer_shouldCallMoPubViewAdFailed() throws Exception {
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.adDidFail(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
        verify(mockMoPubView).adFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
    }

    @Test
    public void adDidFail_withNullMoPubView_shouldNotScheduleRefreshTimer_shouldNotCallMoPubViewAdFailed() throws Exception {
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        // This sets the MoPubView to null
        subject.cleanup();
        subject.adDidFail(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
        verify(mockMoPubView, never()).adFailed(any(MoPubErrorCode.class));
    }


    @Test
    public void scheduleRefreshTimer_shouldNotScheduleIfRefreshTimeIsNull() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(null).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleIfRefreshTimeIsZero() {
        response = response.toBuilder().setRefreshTimeMilliseconds(0).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimerIfEnabled_shouldCancelOldRefreshAndScheduleANewOne() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleRefreshIfAutoRefreshIsOff() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);

        ShadowLooper.pauseMainLooper();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.setShouldAllowAutoRefresh(false);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimer_whenAdViewControllerNotConfiguredByResponse_shouldHaveDefaultRefreshTime() throws Exception {
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
    public void setShouldAllowAutoRefresh_shouldSetCurrentAutoRefreshStatus() throws Exception {
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();

        subject.setShouldAllowAutoRefresh(false);
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();

        subject.setShouldAllowAutoRefresh(true);
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();
    }

    @Test
    public void pauseRefresh_shouldDisableAutoRefresh() throws Exception {
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();

        subject.pauseRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isFalse();
    }

    @Test
    public void resumeRefresh_afterPauseRefresh_shouldEnableRefresh() throws Exception {
        subject.pauseRefresh();

        subject.resumeRefresh();
        assertThat(subject.getCurrentAutoRefreshStatus()).isTrue();
    }

    @Test
    public void pauseAndResumeRefresh_withShouldAllowAutoRefreshFalse_shouldAlwaysHaveRefreshFalse() throws Exception {
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
    public void enablingAutoRefresh_afterLoadAd_shouldScheduleNewRefreshTimer() throws Exception {

        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.loadAd();
        adViewControllerSpy.setShouldAllowAutoRefresh(true);
        verify(adViewControllerSpy).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void enablingAutoRefresh_withoutCallingLoadAd_shouldNotScheduleNewRefreshTimer() throws Exception {
        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.setShouldAllowAutoRefresh(true);
        verify(adViewControllerSpy, never()).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void disablingAutoRefresh_shouldCancelRefreshTimers() throws Exception {
        response = response.toBuilder().setRefreshTimeMilliseconds(30).build();
        subject.onAdLoadSuccess(response);
        ShadowLooper.pauseMainLooper();

        subject.loadAd();
        subject.setShouldAllowAutoRefresh(true);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.setShouldAllowAutoRefresh(false);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void trackImpression_shouldAddToRequestQueue() throws Exception {
        subject.onAdLoadSuccess(response);
        subject.trackImpression();

        verify(mockRequestQueue).add(argThat(isUrl("impressionUrl")));
    }

    @Test
    public void trackImpression_noAdResponse_shouldNotAddToQueue() {
        subject.trackImpression();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void registerClick_shouldHttpGetTheClickthroughUrl() throws Exception {
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
    public void fetchAd_withNullMoPubView_shouldNotMakeRequest() throws Exception {
        subject.cleanup();
        subject.fetchAd("adUrl");
        verify(mockRequestQueue, never()).add(any(AdRequest.class));
    }

    @Test
    public void loadAd_shouldNotLoadWithoutConnectivity() throws Exception {
        ConnectivityManager connectivityManager = (ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Shadows.shadowOf(connectivityManager.getActiveNetworkInfo()).setConnectionStatus(false);
        subject.setAdUnitId("adunit");

        subject.loadAd();
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadAd_shouldNotLoadUrlIfAdUnitIdIsNull() throws Exception {
        subject.loadAd();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void loadNonJavascript_shouldFetchAd() throws Exception {
        String url = "https://www.guy.com";
        subject.loadNonJavascript(url);

        verify(mockRequestQueue).add(argThat(isUrl(url)));
    }

    @Test
    public void loadNonJavascript_whenAlreadyLoading_shouldNotFetchAd() throws Exception {
        String url = "https://www.guy.com";
        subject.loadNonJavascript(url);
        reset(mockRequestQueue);
        subject.loadNonJavascript(url);

        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void loadNonJavascript_shouldAcceptNullParameter() throws Exception {
        subject.loadNonJavascript(null);
        // pass
    }

    @Test
    public void reload_shouldReuseOldUrl() throws Exception {
        String url = "https://www.guy.com";
        subject.loadNonJavascript(url);
        subject.setNotLoading();
        reset(mockRequestQueue);
        subject.reload();

        verify(mockRequestQueue).add(argThat(isUrl(url)));
    }

    @Test
    public void loadFailUrl_shouldLoadFailUrl() throws Exception {
        subject.onAdLoadSuccess(response);
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockRequestQueue).add(argThat(isUrl("failUrl")));
        verify(mockMoPubView, never()).adFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void loadFailUrl_shouldAcceptNullErrorCode() throws Exception {
        subject.loadFailUrl(null);
        // pass
    }

    @Test
    public void loadFailUrl_whenFailUrlIsNull_shouldCallAdDidFail() throws Exception {
        response.toBuilder().setFailoverUrl(null).build();
        subject.loadFailUrl(MoPubErrorCode.INTERNAL_ERROR);

        verify(mockMoPubView).adFailed(eq(MoPubErrorCode.NO_FILL));
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void setAdContentView_whenCalledFromWrongUiThread_shouldStillSetContentView() throws Exception {
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

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenCalledAfterCleanUp_shouldNotRemoveViewsAndAddView() throws Exception {
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

        verify(mockMoPubView, never()).removeAllViews();
        verify(mockMoPubView, never()).addView(any(View.class), any(FrameLayout.LayoutParams.class));
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndHasDimensions_shouldSizeAndCenterView() throws Exception {
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndDoesntHaveDimensions_shouldWrapAndCenterView() throws Exception {
        response = response.toBuilder().setDimensions(null, null).build();
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.onAdLoadSuccess(response);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenNotServerDimensions_shouldWrapAndCenterView() throws Exception {
        subject.onAdLoadSuccess(response);
        View view = mock(View.class);

        subject.setAdContentView(view);

        verify(mockMoPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(mockMoPubView).addView(eq(view), layoutParamsCaptor.capture());
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

        subject.onAdLoadError(expectedInternalError);

        verify(mockMoPubView).adFailed(MoPubErrorCode.WARMUP);
    }

    @Test
    public void onAdLoadError_whenNoNetworkConnection_shouldReturnErrorCodeNoConnection_shouldCallMoPubViewAdFailed() {
        subject.onAdLoadError(new NoConnectionError());

        // DeviceUtils#isNetworkAvailable conveniently returns false due to
        // not having the network permission.
        verify(mockMoPubView).adFailed(MoPubErrorCode.NO_CONNECTION);
    }

    @Test
    public void onAdLoadError_withInvalidServerResponse_shouldReturnErrorCodeServerError_shouldCallMoPubViewAdFailed_shouldIncrementBackoffPower() {
        for (int htmlErrorCode : HTML_ERROR_CODES) {
            final int oldBackoffPower = subject.mBackoffPower;
            final NetworkResponse errorNetworkResponse = new NetworkResponse(htmlErrorCode, null,
                    null, true, 0);
            final VolleyError volleyError = new VolleyError(errorNetworkResponse);

            subject.onAdLoadError(volleyError);

            assertThat(subject.mBackoffPower).isEqualTo(oldBackoffPower + 1);
        }
        verify(mockMoPubView, times(HTML_ERROR_CODES.length)).adFailed(MoPubErrorCode.SERVER_ERROR);
    }

    @Test
    public void loadCustomEvent_shouldCallMoPubViewLoadCustomEvent() throws Exception {
        Map serverExtras = mock(Map.class);
        String customEventClassName = "customEventClassName";
        subject.loadCustomEvent(mockMoPubView, customEventClassName, serverExtras);

        verify(mockMoPubView).loadCustomEvent(customEventClassName, serverExtras);
    }

    @Test
    public void loadCustomEvent_withNullMoPubView_shouldNotCallMoPubViewLoadCustomEvent() throws Exception {
        Map serverExtras = mock(Map.class);
        String customEventClassName = "customEventClassName";
        subject.loadCustomEvent(null, customEventClassName, serverExtras);

        verify(mockMoPubView, never()).loadCustomEvent(anyString(), anyMap());
    }

    @Test
    public void loadCustomEvent_withNullCustomEventClassName_shouldCallMoPubViewLoadCustomEvent() throws Exception {
        Map serverExtras = mock(Map.class);
        subject.loadCustomEvent(mockMoPubView, null, serverExtras);

        verify(mockMoPubView).loadCustomEvent(null, serverExtras);
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
}
