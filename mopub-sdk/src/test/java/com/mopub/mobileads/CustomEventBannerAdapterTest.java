package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.Map;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CustomEventBannerAdapterTest {
    private CustomEventBannerAdapter subject;
    @Mock
    private MoPubView moPubView;
    @Mock
    private AdReport mockAdReport;
    private static final String CLASS_NAME = "arbitrary_banner_adapter_class_name";
    private static final long BROADCAST_IDENTIFIER = 123;
    private Map<String, String> serverExtras;
    private CustomEventBanner banner;
    private Map<String,Object> localExtras;
    private Map<String,Object> expectedLocalExtras;
    private HashMap<String,String> expectedServerExtras;

    @Before
    public void setUp() throws Exception {

        when(moPubView.getAdTimeoutDelay()).thenReturn(null);
        when(moPubView.getAdWidth()).thenReturn(320);
        when(moPubView.getAdHeight()).thenReturn(50);

        localExtras = new HashMap<String, Object>();
        when(moPubView.getLocalExtras()).thenReturn(localExtras);

        serverExtras = new HashMap<String, String>();
        serverExtras.put("key", "value");
        serverExtras.put("another_key", "another_value");
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        expectedLocalExtras = new HashMap<String, Object>();
        expectedLocalExtras.put(DataKeys.AD_REPORT_KEY, mockAdReport);
        expectedLocalExtras.put("broadcastIdentifier", BROADCAST_IDENTIFIER);
        expectedLocalExtras.put(DataKeys.AD_WIDTH, 320);
        expectedLocalExtras.put(DataKeys.AD_HEIGHT, 50);

        expectedServerExtras = new HashMap<String, String>();

        banner = CustomEventBannerFactory.create(CLASS_NAME);
    }

    @Test
    public void constructor_shouldPopulateLocalExtrasWithAdWidthAndHeight() throws Exception {
        assertThat(localExtras.get("com_mopub_ad_width")).isEqualTo(320);
        assertThat(localExtras.get("com_mopub_ad_height")).isEqualTo(50);
    }

    @Test
    public void timeout_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        subject.loadAd();

        ShadowLooper.idleMainLooper(CustomEventBannerAdapter.DEFAULT_BANNER_TIMEOUT_DELAY - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNegativeAdTimeoutDelay_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        when(moPubView.getAdTimeoutDelay()).thenReturn(-1);

        subject.loadAd();

        ShadowLooper.idleMainLooper(CustomEventBannerAdapter.DEFAULT_BANNER_TIMEOUT_DELAY - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNonNullAdTimeoutDelay_shouldSignalFailureAndInvalidateWithCustomDelay() throws Exception {
       when(moPubView.getAdTimeoutDelay()).thenReturn(77);

        subject.loadAd();

        ShadowLooper.idleMainLooper(77000 - 1);
        verify(moPubView, never()).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(moPubView).loadFailUrl(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }


    @Test
    public void loadAd_shouldPropagateLocationInLocalExtras() throws Exception {
        Location expectedLocation = new Location("");
        expectedLocation.setLongitude(10.0);
        expectedLocation.setLongitude(20.1);

        when(moPubView.getLocation()).thenReturn(expectedLocation);
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, new HashMap<String, String>(), BROADCAST_IDENTIFIER, mockAdReport);
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
    public void loadAd_shouldPropagateServerExtrasToLoadBanner() throws Exception {
        subject.loadAd();

        expectedServerExtras.put("key", "value");
        expectedServerExtras.put("another_key", "another_value");

        verify(banner).loadBanner(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadAd_shouldScheduleTimeout_bannerLoadedAndFailed_shouldCancelTimeout() throws Exception {
        ShadowLooper.pauseMainLooper();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.loadAd();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onBannerLoaded(null);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.loadAd();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onBannerFailed(null);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void loadAd_shouldScheduleTimeoutRunnableBeforeCallingLoadBanner() throws Exception {
        ShadowLooper.pauseMainLooper();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        Answer assertTimeoutRunnableHasStarted = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
                return null;
            }
        };

        // noinspection unchecked
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
        ShadowLooper.pauseMainLooper();

        Answer justCallOnBannerFailed = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
                subject.onBannerFailed(null);
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
                return null;
            }
        };

        // noinspection unchecked
        doAnswer(justCallOnBannerFailed)
                .when(banner)
                .loadBanner(
                        any(Context.class),
                        any(CustomEventBannerListener.class),
                        any(Map.class),
                        any(Map.class)
                );

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
        subject.loadAd();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void onBannerLoaded_shouldSignalMoPubView() throws Exception {
        View view = new View(Robolectric.buildActivity(Activity.class).create().get());
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
        when(moPubView.getAutorefreshEnabled()).thenReturn(true);
        subject.onBannerExpanded();
        reset(moPubView);
        subject.onBannerCollapsed();
        verify(moPubView).setAutorefreshEnabled(eq(true));
        verify(moPubView).adClosed();

        when(moPubView.getAutorefreshEnabled()).thenReturn(false);
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

        // noinspection unchecked
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
}
