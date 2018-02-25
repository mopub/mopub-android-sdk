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
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        expectedLocalExtras = new HashMap<String, Object>();
        expectedLocalExtras.put(DataKeys.AD_REPORT_KEY, mockAdReport);
        expectedLocalExtras.put("broadcastIdentifier", BROADCAST_IDENTIFIER);
        expectedLocalExtras.put(DataKeys.AD_WIDTH, 320);
        expectedLocalExtras.put(DataKeys.AD_HEIGHT, 50);
        expectedLocalExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, false);

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
        serverExtras.put("key", "value");
        serverExtras.put("another_key", "another_value");
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

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
    public void loadAd_withVisibilityImpressionTrackingEnabled_shouldPropagateVisibilityImpressionTrackingEnabledFlagInLocalExtras() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");
        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        subject.loadAd();

        expectedLocalExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, true);
        expectedServerExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        expectedServerExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");
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
    public void onBannerLoaded_whenViewIsHtmlBannerWebView_shouldNotTrackImpression() throws Exception {
        View mockHtmlBannerWebView = mock(HtmlBannerWebView.class);
        subject.onBannerLoaded(mockHtmlBannerWebView);

        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(mockHtmlBannerWebView));
        verify(moPubView, never()).trackNativeImpression();

        // Since there are no visibility imp tracking headers, imp tracking should not be enabled.
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
        assertThat(subject.getVisibilityTracker()).isNull();
    }

    @Test
    public void onBannerLoaded_whenViewIsNotHtmlBannerWebView_shouldSignalMoPubView() throws Exception {
        View view = new View(Robolectric.buildActivity(Activity.class).create().get());
        subject.onBannerLoaded(view);

        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(view));
        verify(moPubView).trackNativeImpression();

        // Since there are no visibility imp tracking headers, imp tracking should not be enabled.
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
        assertThat(subject.getVisibilityTracker()).isNull();
    }

    @Test
    public void onBannerLoaded_whenViewIsHtmlBannerWebView_withVisibilityImpressionTrackingEnabled_shouldSetUpVisibilityTrackerWithListener_shouldNotTrackNativeImpressionImmediately() {
        View mockHtmlBannerWebView = mock(HtmlBannerWebView.class);
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);
        subject.onBannerLoaded(mockHtmlBannerWebView);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(1);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(0);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isTrue();
        assertThat(subject.getVisibilityTracker()).isNotNull();
        assertThat(subject.getVisibilityTracker().getBannerVisibilityTrackerListener()).isNotNull();
        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(mockHtmlBannerWebView));
        verify(moPubView, never()).trackNativeImpression();
    }

    @Test
    public void onBannerLoaded_whenViewIsNotHtmlBannerWebView_withVisibilityImpressionTrackingEnabled_shouldSetUpVisibilityTrackerWithListener_shouldNotTrackNativeImpressionImmediately() {
        View view = new View(Robolectric.buildActivity(Activity.class).create().get());
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);
        subject.onBannerLoaded(view);

        // When visibility impression tracking is enabled, regardless of whether the banner view is
        // HtmlBannerWebView or not, the behavior should be the same.
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(1);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(0);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isTrue();
        assertThat(subject.getVisibilityTracker()).isNotNull();
        assertThat(subject.getVisibilityTracker().getBannerVisibilityTrackerListener()).isNotNull();
        verify(moPubView).nativeAdLoaded();
        verify(moPubView).setAdContentView(eq(view));
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

    @Test
    public void parseBannerImpressionTrackingHeaders_whenMissingInServerExtras_shouldUseDefaultValues_shouldNotEnableVisibilityImpressionTracking() {
        // If headers are missing, use default values
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withBothValuesNonInteger_shouldUseDefaultValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, null);

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        // Both header values must be Integers in order to be parsed
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNonIntegerMinVisibleDipsValue_shouldUseDefaultValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, null);
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        // Both header values must be Integers in order to be parsed
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNonIntegerMinVisibleMsValue_shouldUseDefaultValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        // Both header values must be Integers in order to be parsed
        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.MIN_VALUE);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withBothValuesValid_shouldParseValues_shouldEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(1);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(0);
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isTrue();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withBothValuesInvalid_shouldParseValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "0");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "-1");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(0);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(-1);

        // ImpressionMinVisibleDips must be > 0 AND ImpressionMinVisibleMs must be >= 0 in order to
        // enable viewable impression tracking
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withInvalidMinVisibleDipsValue_shouldParseValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "0");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "0");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(0);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(0);

        // ImpressionMinVisibleDips must be > 0 in order to enable viewable impression tracking
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withInvalidMinVisibleMsValue_shouldParseValues_shouldNotEnableVisibilityImpressionTracking() {
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS, "1");
        serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS, "-1");

        subject = new CustomEventBannerAdapter(moPubView, CLASS_NAME, serverExtras, BROADCAST_IDENTIFIER, mockAdReport);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(1);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(-1);

        // ImpressionMinVisibleMs must be >= 0 in order to enable viewable impression tracking
        assertThat(subject.isVisibilityImpressionTrackingEnabled()).isFalse();
    }
}
