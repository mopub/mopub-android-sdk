package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import com.mopub.common.GpsHelper;
import com.mopub.common.GpsHelperTest;
import com.mopub.common.MoPub;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.factories.HttpClientFactory;
import com.mopub.mobileads.test.support.TestAdFetcherFactory;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import com.mopub.mobileads.test.support.ThreadUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;

import java.lang.reflect.InvocationTargetException;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.mobileads.AdViewController.DEFAULT_REFRESH_TIME_MILLISECONDS;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NO_FILL;
import static com.mopub.mobileads.test.support.ThreadUtils.NETWORK_DELAY;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.application;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class AdViewControllerTest {
    private AdViewController subject;
    private MoPubView moPubView;
    private HttpResponse response;
    private HttpClient httpClient;
    private AdFetcher adFetcher;
    private MethodBuilder methodBuilder;
    private Activity context;

    @Before
    public void setup() {
        context = new Activity();
        shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);

        moPubView = mock(MoPubView.class);
        stub(moPubView.getContext()).toReturn(context);

        httpClient = HttpClientFactory.create();

        subject = new AdViewController(context, moPubView);
        response = new TestHttpResponseWithHeaders(200, "I ain't got no-body");
        adFetcher = TestAdFetcherFactory.getSingletonMock();
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        reset(methodBuilder);
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void scheduleRefreshTimerIfEnabled_shouldCancelOldRefreshAndScheduleANewOne() throws Exception {
        response.addHeader("X-Refreshtime", "30");
        subject.configureUsingHttpResponse(response);
        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleRefreshIfAutorefreshIsOff() throws Exception {
        response.addHeader("X-Refreshtime", "30");
        subject.configureUsingHttpResponse(response);

        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.forceSetAutorefreshEnabled(false);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimer_whenAdViewControllerNotConfiguredByResponse_shouldHaveDefaultRefreshTime() throws Exception {
        Robolectric.pauseMainLooper();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Robolectric.idleMainLooper(DEFAULT_REFRESH_TIME_MILLISECONDS - 1);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Robolectric.idleMainLooper(1);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void scheduleRefreshTimer_shouldNotScheduleRefreshIfRefreshTimeIsZero() throws Exception {
//        response.addHeader("X-Refreshtime", "0");
        subject.configureUsingHttpResponse(response);
        Robolectric.pauseMainLooper();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.scheduleRefreshTimerIfEnabled();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }
    
    @Test
    public void forceSetAutoRefreshEnabled_shouldSetAutoRefreshSetting() throws Exception {
        assertThat(subject.getAutorefreshEnabled()).isTrue();

        subject.forceSetAutorefreshEnabled(false);
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.forceSetAutorefreshEnabled(true);
        assertThat(subject.getAutorefreshEnabled()).isTrue();
    }

    @Test
    public void pauseRefresh_shouldDisableAutorefresh() throws Exception {
        assertThat(subject.getAutorefreshEnabled()).isTrue();

        subject.pauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();
    }

    @Test
    public void unpauseRefresh_afterUnpauseRefresh_shouldEnableRefresh() throws Exception {
        subject.pauseRefresh();
        
        subject.unpauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isTrue();
    }

    @Test
    public void pauseAndUnpauseRefresh_withRefreshForceDisabled_shouldAlwaysHaveRefreshFalse() throws Exception {
        subject.forceSetAutorefreshEnabled(false);
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.pauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();

        subject.unpauseRefresh();
        assertThat(subject.getAutorefreshEnabled()).isFalse();
    }

    @Test
    public void enablingAutoRefresh_afterLoadAd_shouldScheduleNewRefreshTimer() throws Exception {
        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.loadAd();
        adViewControllerSpy.forceSetAutorefreshEnabled(true);
        verify(adViewControllerSpy).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void enablingAutoRefresh_withoutCallingLoadAd_shouldNotScheduleNewRefreshTimer() throws Exception {
        final AdViewController adViewControllerSpy = spy(subject);

        adViewControllerSpy.forceSetAutorefreshEnabled(true);
        verify(adViewControllerSpy, never()).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void disablingAutoRefresh_shouldCancelRefreshTimers() throws Exception {
        response.addHeader("X-Refreshtime", "30");
        subject.configureUsingHttpResponse(response);

        Robolectric.pauseMainLooper();

        subject.loadAd();
        subject.forceSetAutorefreshEnabled(true);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.forceSetAutorefreshEnabled(false);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void trackImpression_shouldHttpGetTheImpressionUrl() throws Exception {
        response.addHeader("X-Imptracker", "http://trackingUrl");
        subject.configureUsingHttpResponse(response);
        String expectedUserAgent = new WebView(context).getSettings().getUserAgentString();
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();
        fakeHttpLayer.addPendingHttpResponse(200, "");

        assertThat(expectedUserAgent).isNotNull();

        subject.trackImpression();
        ThreadUtils.pause(NETWORK_DELAY); // does this make the test flaky?

        HttpRequest request = fakeHttpLayer.getLastSentHttpRequestInfo().getHttpRequest();
        assertThat(request.getFirstHeader("User-Agent").getValue()).isEqualTo(expectedUserAgent);
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://trackingUrl");

        ClientConnectionManager connectionManager = httpClient.getConnectionManager();
        try {
            new MethodBuilder(connectionManager, "assertStillUp").setAccessible().execute();
            fail("should have thrown an exception");
        } catch (InvocationTargetException expected) {
            assertThat(expected.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    // this test for impressionUrl is unnecessary (since we're catching the NullPointerException)
    @Test
    public void trackImpression_shouldDoNothingIfImpressionUrlNotSpecified() throws Exception {
        subject.configureUsingHttpResponse(response);
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();
        fakeHttpLayer.addPendingHttpResponse(200, "");

        subject.trackImpression();
        Thread.sleep(300); // does this make the test flaky?

        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
    }

    @Test
    public void registerClick_shouldHttpGetTheClickthroughUrl() throws Exception {
        response.addHeader("X-Clickthrough", "http://clickUrl");
        subject.configureUsingHttpResponse(response);
        String expectedUserAgent = new WebView(context).getSettings().getUserAgentString();
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();
        fakeHttpLayer.addPendingHttpResponse(200, "");

        assertThat(expectedUserAgent).isNotNull();

        subject.registerClick();
        Thread.sleep(200); // does this make the test flaky?

        HttpRequest request = fakeHttpLayer.getLastSentHttpRequestInfo().getHttpRequest();
        assertThat(request.getFirstHeader("User-Agent").getValue()).isEqualTo(expectedUserAgent);
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://clickUrl");

        ClientConnectionManager connectionManager = httpClient.getConnectionManager();
        try {
            new MethodBuilder(connectionManager, "assertStillUp").setAccessible().execute();
            fail("should have thrown an exception");
        } catch (InvocationTargetException expected) {
            assertThat(expected.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    // this test for clickthroughUrl is unnecessary (since we're catching the NullPointerException)
    @Test
    public void trackImpression_shouldDoNothingIfClickthroughUrlNotSpecified() throws Exception {
        subject.configureUsingHttpResponse(response);
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();
        fakeHttpLayer.addPendingHttpResponse(200, "");

        subject.registerClick();
        Thread.sleep(50); // does this make the test flaky?

        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
    }

    @Test
    public void generateAdUrl_shouldIncludeMinFields() throws Exception {
        String expectedAdUrl = "http://ads.mopub.com/m/ad" +
                "?v=6" +
                "&nv=" + MoPub.SDK_VERSION +
                "&dn=" + Build.MANUFACTURER +
                "%2C" + Build.MODEL +
                "%2C" + Build.PRODUCT +
                "&udid=sha%3A" +
                "&z=-0700" +
                "&o=u" +
                "&sc_a=1.0" +
                "&mr=1" +
                "&ct=3" +
                "&av=1.0" +
                "&android_perms_ext_storage=0";

        String adUrl = subject.generateAdUrl();

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void loadAd_shouldNotLoadUrlIfAdUnitIdIsNull() throws Exception {
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();

        subject.loadAd();

        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
    }

    @Test
    public void loadAd_shouldScheduleRefreshIfNoNetworkConnectivity() throws Exception {
        FakeHttpLayer fakeHttpLayer = Robolectric.getFakeHttpLayer();
        Robolectric.pauseMainLooper();
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(connectivityManager.getActiveNetworkInfo()).setConnectionStatus(false);
        response.addHeader("X-Refreshtime", "30");
        subject.configureUsingHttpResponse(response);
        subject.setAdUnitId("adUnitId");

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.loadAd();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
    }

    @Test
    public void loadAd_whenGooglePlayServicesIsLinkedAndAdInfoIsNotCached_shouldCacheAdInfoBeforeFetchingAd() throws Exception {
        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().commit();
        GpsHelperTest.verifyCleanSharedPreferences(context);

        GpsHelper.setClassNamesForTesting();
        GpsHelperTest.TestAdInfo adInfo = new GpsHelperTest.TestAdInfo();

        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE,
                adInfo,
                adInfo.ADVERTISING_ID,
                adInfo.LIMIT_AD_TRACKING_ENABLED
        );

        final AdViewController.AdViewControllerGpsHelperListener mockAdViewControllerGpsHelperListener
                = mock(AdViewController.AdViewControllerGpsHelperListener.class);
        subject.setGpsHelperListener(mockAdViewControllerGpsHelperListener);
        subject.setAdUnitId("adUnitId");
        subject.setLocation(new Location(""));
        subject.loadAd();
        Thread.sleep(500);

        verify(mockAdViewControllerGpsHelperListener).onFetchAdInfoCompleted();
        GpsHelperTest.verifySharedPreferences(context, adInfo);
    }

    @Test
    public void loadAd_whenGooglePlayServicesIsNotLinked_shouldFetchAdFast() throws Exception {
        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().commit();
        GpsHelperTest.verifyCleanSharedPreferences(context);

        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        // return error code so it fails
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE + 1);

        final AdViewController.AdViewControllerGpsHelperListener mockAdViewControllerGpsHelperListener
                = mock(AdViewController.AdViewControllerGpsHelperListener.class);
        subject.setGpsHelperListener(mockAdViewControllerGpsHelperListener);
        subject.setAdUnitId("adUnitId");
        subject.setLocation(new Location(""));
        subject.loadAd();
        // no need to sleep since it run the callback without an async task

        verify(mockAdViewControllerGpsHelperListener).onFetchAdInfoCompleted();
        GpsHelperTest.verifyCleanSharedPreferences(context);
    }

    @Test
    public void loadAd_whenGooglePlayServicesIsLinkedAndAdInfoIsCached_shouldFetchAdFast() throws Exception {
        GpsHelperTest.TestAdInfo adInfo = new GpsHelperTest.TestAdInfo();
        GpsHelperTest.populateAndVerifySharedPreferences(context, adInfo);
        GpsHelper.setClassNamesForTesting();

        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE
        );

        final AdViewController.AdViewControllerGpsHelperListener mockAdViewControllerGpsHelperListener
                = mock(AdViewController.AdViewControllerGpsHelperListener.class);
        subject.setGpsHelperListener(mockAdViewControllerGpsHelperListener);
        subject.setAdUnitId("adUnitId");
        subject.setLocation(new Location(""));
        subject.loadAd();
        // no need to sleep since it run the callback without an async task

        verify(mockAdViewControllerGpsHelperListener).onFetchAdInfoCompleted();
        GpsHelperTest.verifySharedPreferences(context, adInfo);
    }

    @Test
    public void loadNonJavascript_shouldFetchAd() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);

        verify(adFetcher).fetchAdForUrl(eq(url));
    }

    @Test
    public void loadNonJavascript_whenAlreadyLoading_shouldNotFetchAd() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);
        reset(adFetcher);
        subject.loadNonJavascript(url);

        verify(adFetcher, never()).fetchAdForUrl(anyString());
    }

    @Test
    public void loadNonJavascript_shouldClearTheFailUrl() throws Exception {
        subject.setFailUrl("blarg:");
        subject.loadNonJavascript("http://www.goodness.com");
        reset(adFetcher);
        subject.loadFailUrl(null);

        verify(adFetcher, never()).fetchAdForUrl(anyString());
        verify(moPubView).adFailed(eq(NO_FILL));
    }

    @Test
    public void loadNonJavascript_shouldAcceptNullParameter() throws Exception {
        subject.loadNonJavascript(null);
        // pass
    }

    @Test
    public void reload_shouldReuseOldUrl() throws Exception {
        String url = "http://www.guy.com";
        subject.loadNonJavascript(url);
        subject.setNotLoading();
        reset(adFetcher);
        subject.reload();

        verify(adFetcher).fetchAdForUrl(eq(url));
    }

    @Test
    public void loadFailUrl_shouldLoadFailUrl() throws Exception {
        String failUrl = "http://www.bad.man";
        subject.setFailUrl(failUrl);
        subject.loadFailUrl(INTERNAL_ERROR);

        verify(adFetcher).fetchAdForUrl(eq(failUrl));
        verify(moPubView, never()).adFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void loadFailUrl_shouldAcceptNullErrorCode() throws Exception {
        subject.loadFailUrl(null);
        // pass
    }

    @Test
    public void loadFailUrl_whenFailUrlIsNull_shouldCallAdDidFail() throws Exception {
        subject.setFailUrl(null);
        subject.loadFailUrl(INTERNAL_ERROR);

        verify(moPubView).adFailed(eq(NO_FILL));
        verify(adFetcher, never()).fetchAdForUrl(anyString());
    }

    @Test
    public void setAdContentView_whenCalledFromWrongUiThread_shouldStillSetContentView() throws Exception {
        response.addHeader("X-Width", "320");
        response.addHeader("X-Height", "50");
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.configureUsingHttpResponse(response);

        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(100);
        Robolectric.runUiThreadTasks();

        verify(moPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(moPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenCalledAfterCleanUp_shouldNotRemoveViewsAndAddView() throws Exception {
        response.addHeader("X-Width", "320");
        response.addHeader("X-Height", "50");
        final View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.configureUsingHttpResponse(response);

        subject.cleanup();
        new Thread(new Runnable() {
            @Override
            public void run() {
                subject.setAdContentView(view);
            }
        }).start();
        ThreadUtils.pause(10);
        Robolectric.runUiThreadTasks();

        verify(moPubView, never()).removeAllViews();
        verify(moPubView, never()).addView(any(View.class), any(FrameLayout.LayoutParams.class));
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndHasDimensions_shouldSizeAndCenterView() throws Exception {
        response.addHeader("X-Width", "320");
        response.addHeader("X-Height", "50");
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.configureUsingHttpResponse(response);

        subject.setAdContentView(view);

        verify(moPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(moPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenHonorServerDimensionsAndDoesntHaveDimensions_shouldWrapAndCenterView() throws Exception {
        View view = mock(View.class);
        AdViewController.setShouldHonorServerDimensions(view);
        subject.configureUsingHttpResponse(response);

        subject.setAdContentView(view);

        verify(moPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(moPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void setAdContentView_whenNotServerDimensions_shouldWrapAndCenterView() throws Exception {
        response.addHeader("X-Width", "320");
        response.addHeader("X-Height", "50");
        subject.configureUsingHttpResponse(response);
        View view = mock(View.class);

        subject.setAdContentView(view);

        verify(moPubView).removeAllViews();
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(moPubView).addView(eq(view), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.height).isEqualTo(FrameLayout.LayoutParams.WRAP_CONTENT);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }
}
