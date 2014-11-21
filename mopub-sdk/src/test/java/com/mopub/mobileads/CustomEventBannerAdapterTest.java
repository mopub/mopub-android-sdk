package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
public class CustomEventBannerAdapterTest {
    private CustomEventBannerAdapter subject;
    private MoPubView moPubView;
    private static final String CLASS_NAME = "arbitrary_banner_adapter_class_name";
    private static final String JSON_PARAMS = "{\"key\":\"value\",\"a different key\":\"a different value\"}";
    private CustomEventBanner banner;
    private Map<String,Object> expectedLocalExtras;
    private HashMap<String,String> expectedServerExtras;

    @Before
    public void setUp() throws Exception {
        moPubView = mock(MoPubView.class);
        stub(moPubView.getAdTimeoutDelay()).toReturn(null);

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, JSON_PARAMS);

        expectedLocalExtras = new HashMap<String, Object>();
        expectedServerExtras = new HashMap<String, String>();

        banner = CustomEventBannerFactory.create(CLASS_NAME);
    }

    @Test
    public void timeout_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        subject.loadAd();

        Robolectric.idleMainLooper(CustomEventBannerAdapter.DEFAULT_BANNER_TIMEOUT_DELAY - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        Robolectric.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNegativeAdTimeoutDelay_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        stub(moPubView.getAdTimeoutDelay()).toReturn(-1);

        subject.loadAd();

        Robolectric.idleMainLooper(CustomEventBannerAdapter.DEFAULT_BANNER_TIMEOUT_DELAY - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        Robolectric.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNonNullAdTimeoutDelay_shouldSignalFailureAndInvalidateWithCustomDelay() throws Exception {
        stub(moPubView.getAdTimeoutDelay()).toReturn(77);

        subject.loadAd();

        Robolectric.idleMainLooper(77000 - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        Robolectric.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void loadAd_shouldHaveEmptyServerExtrasOnInvalidJsonParams() throws Exception {
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, "{this is terrible JSON");
        subject.loadAd();

        verify(banner).loadBanner(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadAd_shouldPropagateLocationInLocalExtras() throws Exception {
        Location expectedLocation = new Location("");
        expectedLocation.setLongitude(10.0);
        expectedLocation.setLongitude(20.1);

        stub(moPubView.getLocation()).toReturn(expectedLocation);
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, null);
        subject.loadAd();

        expectedLocalExtras.put("location", moPubView.getLocation());

        verify(banner).loadBanner(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadAd_shouldPropagateJsonParamsInServerExtras() throws Exception {
        subject.loadAd();

        expectedServerExtras.put("key", "value");
        expectedServerExtras.put("a different key", "a different value");

        verify(banner).loadBanner(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadAd_shouldScheduleTimeout_bannerLoadedAndFailed_shouldCancelTimeout() throws Exception {
        Robolectric.pauseMainLooper();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.loadAd();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.onBannerLoaded(null);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        subject.loadAd();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        subject.onBannerFailed(null);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void loadAd_shouldScheduleTimeoutRunnableBeforeCallingLoadBanner() throws Exception {
        Robolectric.pauseMainLooper();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);

        Answer assertTimeoutRunnableHasStarted = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
                return null;
            }
        };

        doAnswer(assertTimeoutRunnableHasStarted)
                .when(banner)
                .loadBanner(
                        any(Context.class),
                        any(CustomEventBannerListener.class),
                        any(Map.class),
                        any(Map.class)
                );

        subject.loadAd();
    }


    @Test
    public void loadAd_whenCallingOnBannerFailed_shouldCancelExistingTimeoutRunnable() throws Exception {
        Robolectric.pauseMainLooper();

        Answer justCallOnBannerFailed = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
                subject.onBannerFailed(null);
                assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
                return null;
            }
        };

        doAnswer(justCallOnBannerFailed)
                .when(banner)
                .loadBanner(
                        any(Context.class),
                        any(CustomEventBanner.CustomEventBannerListener.class),
                        any(Map.class),
                        any(Map.class)
                );

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
        subject.loadAd();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(0);
    }

    @Test
    public void onBannerLoaded_shouldSignalMoPubView() throws Exception {
        View view = new View(new Activity());
        subject.onBannerLoaded(view);
        
        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(view));
        verify(moPubView).trackNativeImpression();
    }

    @Test
    public void onBannerLoaded_whenViewIsHtmlBannerWebView_shouldNotTrackImpression() throws Exception {
        View mockHtmlBannerWebView = mock(HtmlBannerWebView.class);
        subject.onBannerLoaded(mockHtmlBannerWebView);

        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(mockHtmlBannerWebView));
        verify(moPubView, never()).trackNativeImpression();
    }

    @Test
    public void onBannerFailed_shouldLoadFailUrl() throws Exception {
        subject.onBannerFailed(ADAPTER_CONFIGURATION_ERROR);

        verify(moPubView).loadFailUrl(eq(ADAPTER_CONFIGURATION_ERROR));
    }

    @Test
    public void onBannerFailed_whenErrorCodeIsNull_shouldPassUnspecifiedError() throws Exception {
        subject.onBannerFailed(null);

        verify(moPubView).loadFailUrl(eq(UNSPECIFIED));
    }

    @Test
    public void onBannerExpanded_shouldPauseRefreshAndCallAdPresentOverlay() throws Exception {
        subject.onBannerExpanded();

        verify(moPubView).setAutorefreshEnabled(eq(false));
        verify(moPubView).adPresentedOverlay();
    }

    @Test
    public void onBannerCollapsed_shouldRestoreRefreshSettingAndCallAdClosed() throws Exception {
        stub(moPubView.getAutorefreshEnabled()).toReturn(true);
        subject.onBannerExpanded();
        reset(moPubView);
        subject.onBannerCollapsed();
        verify(moPubView).setAutorefreshEnabled(eq(true));
        verify(moPubView).adClosed();

        stub(moPubView.getAutorefreshEnabled()).toReturn(false);
        subject.onBannerExpanded();
        reset(moPubView);
        subject.onBannerCollapsed();
        verify(moPubView).setAutorefreshEnabled(eq(false));
        verify(moPubView).adClosed();
    }

    @Test
    public void onBannerClicked_shouldRegisterClick() throws Exception {
        subject.onBannerClicked();

        verify(moPubView).registerClick();
    }

    @Test
    public void onLeaveApplication_shouldRegisterClick() throws Exception {
        subject.onLeaveApplication();

        verify(moPubView).registerClick();
    }

    @Test
    public void invalidate_shouldCauseLoadAdToDoNothing() throws Exception {
        subject.invalidate();

        subject.loadAd();

        verify(banner, never()).loadBanner(
                any(Context.class),
                any(CustomEventBannerListener.class),
                any(Map.class),
                any(Map.class)
        );
    }

    @Test
    public void invalidate_shouldCauseBannerListenerMethodsToDoNothing() throws Exception {
        subject.invalidate();

        subject.onBannerLoaded(null);
        subject.onBannerFailed(null);
        subject.onBannerExpanded();
        subject.onBannerCollapsed();
        subject.onBannerClicked();
        subject.onLeaveApplication();

        verify(moPubView, never()).nativeAdLoaded();
        verify(moPubView, never()).setAdContentView(any(View.class));
        verify(moPubView, never()).trackNativeImpression();
        verify(moPubView, never()).loadFailUrl(any(MoPubErrorCode.class));
        verify(moPubView, never()).setAutorefreshEnabled(any(boolean.class));
        verify(moPubView, never()).adClosed();
        verify(moPubView, never()).registerClick();
    }

    @Test
    public void init_whenPassedHtmlData_shouldPutItInLocalExtras() throws Exception {
        String expectedHtmlData = "expected html data";
        expectedServerExtras.put(HTML_RESPONSE_BODY_KEY, expectedHtmlData);
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, "{\"Html-Response-Body\":\"expected html data\"}");
        subject.loadAd();

        verify(banner).loadBanner(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }
}
