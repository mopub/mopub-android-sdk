// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.view.View;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.BaseAdFactory;
import com.mopub.mobileads.test.support.TestBaseAdFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class InlineAdAdapterTest {
    private static final int DEFAULT_TIMEOUT_DELAY = Constants.THIRTY_SECONDS_MILLIS;
    private static final String CLASS_NAME = "arbitrary_inline_adapter_class_name";

    private static long BROADCAST_IDENTIFER = 123;

    private Activity context;
    @Mock
    private MoPubAd moPubAd;
    private InlineAdAdapter subject;
    private HashMap<String, String> expectedServerExtras;
    @Mock
    private AdViewController mockAdViewController;
    private AdLifecycleListener.LoadListener loadListener;
    private AdLifecycleListener.InteractionListener interactionListener;
    private AdData adData;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();

        moPubAd = mock(MoPubView.class);
        when(moPubAd.getAdViewController()).thenReturn(mockAdViewController);

        Map<String, String> extras = new HashMap<String, String>();
        extras.put("key", "value");
        adData = new AdData.Builder()
                .extras(extras)
                .broadcastIdentifier(BROADCAST_IDENTIFER)
                .timeoutDelayMillis(DEFAULT_TIMEOUT_DELAY)
                .build();

        subject = new InlineAdAdapter(context, CLASS_NAME, adData);

        loadListener = mock(AdLifecycleListener.LoadListener.class);
        subject.setLoadListener(loadListener);
        interactionListener = mock(AdLifecycleListener.InteractionListener.class);
        subject.setInteractionListener(interactionListener);

        when(subject.isAutomaticImpressionAndClickTrackingEnabled()).thenReturn(true);
    }

    @Test(expected = AdAdapter.BaseAdNotFoundException.class)
    public void constructor_withInvalidClassName_shouldThrowBaseAdNotFoundException()
            throws AdAdapter.BaseAdNotFoundException {
        BaseAdFactory.setInstance(new BaseAdFactory());
        new InlineAdAdapter(context, "not_a_real_class", adData);
    }

    @Test
    public void timeout_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        subject.load(loadListener);
        ShadowLooper.idleMainLooper(DEFAULT_TIMEOUT_DELAY - 1);
        verify(loadListener, never()).onAdLoadFailed(NETWORK_TIMEOUT);
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(loadListener).onAdLoadFailed(NETWORK_TIMEOUT);
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNonNullAdTimeoutDelay_shouldSignalFailureAndInvalidateWithCustomDelay() {
        adData.setTimeoutDelayMillis(77000);

        subject.load(loadListener);
        ShadowLooper.idleMainLooper(77000 - 1);
        verify(loadListener, never()).onAdLoadFailed(NETWORK_TIMEOUT);
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(loadListener).onAdLoadFailed(NETWORK_TIMEOUT);
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void load_shouldPropagateAdDataToBaseAd() throws Exception {
        subject = new InlineAdAdapter(context, "com.mopub.mobileads.MoPubInline", adData);

        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        subject.load(loadListener);

        verify(baseAd).internalLoad(
                context,
                loadListener,
                adData
        );
    }

    @Test
    public void load_shouldScheduleTimeout_loadedAndFailed_shouldCancelTimeout() throws Exception {
        ShadowLooper.pauseMainLooper();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.load(loadListener);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onAdLoaded();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.load(loadListener);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onAdFailed(UNSPECIFIED);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void load_shouldScheduleTimeoutRunnableBeforeCallingLoad() throws Exception {
        ShadowLooper.pauseMainLooper();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        Answer assertTimeoutRunnableHasStarted = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
                return null;
            }
        };

        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        // noinspection unchecked
        doAnswer(assertTimeoutRunnableHasStarted)
                .when(baseAd)
                .internalLoad(
                        context,
                        loadListener,
                        adData
                );

        subject.load(loadListener);
    }

    @Test
    public void load_whenCallingOnFailed_shouldCancelExistingTimeoutRunnable() throws Exception {
        ShadowLooper.pauseMainLooper();

        Answer justCallOnFailed = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
                subject.onAdFailed(null);
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
                return null;
            }
        };

        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        // noinspection unchecked
        doAnswer(justCallOnFailed)
                .when(baseAd)
                .internalLoad(
                        context,
                        loadListener,
                        adData
                );

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
        subject.load(loadListener);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void show_shouldCallMoPubAdSetContentView_withBaseAdGetAdView() throws Exception {
        final BaseAd baseAd = TestBaseAdFactory.getSingletonMock();
        when(baseAd.getAdView()).thenReturn(mock(View.class));

        subject.show(moPubAd);

        final View adView = baseAd.getAdView();

        verify(moPubAd).setAdContentView(adView);
    }

    @Test
    public void onLoaded_shouldSignalAdapterListener() throws Exception {
        subject.onAdLoaded();

        verify(loadListener).onAdLoaded();
    }

    @Test
    public void onLoadFailed_shouldLoadFailUrl() throws Exception {
        subject.onAdLoadFailed(ADAPTER_CONFIGURATION_ERROR);

        verify(loadListener).onAdLoadFailed(eq(ADAPTER_CONFIGURATION_ERROR));
    }

    @Test
    public void onFailed_shouldLoadFailUrl() throws Exception {
        subject.onAdFailed(ADAPTER_CONFIGURATION_ERROR);

        verify(interactionListener).onAdFailed(eq(ADAPTER_CONFIGURATION_ERROR));
    }

    @Test
    public void onFailed_whenErrorCodeIsNull_shouldPassUnspecifiedError() throws Exception {
        subject.onAdFailed(UNSPECIFIED);

        verify(interactionListener).onAdFailed(eq(UNSPECIFIED));
    }

    @Test
    public void onShown_shouldSignalAdapterListener() throws Exception {
        subject.onAdShown();

        verify(interactionListener).onAdShown();
    }

    @Test
    public void onClicked_shouldSignalAdapterListener() throws Exception {
        subject.onAdClicked();

        verify(interactionListener).onAdClicked();
    }

    @Test
    public void onImpression_whenAutomaticImpressionAndClickTrackingFalse_shouldSignalAdapterListener() {
        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();
        when(baseAd.isAutomaticImpressionAndClickTrackingEnabled()).thenReturn(false);

        subject.onAdImpression();

        verify(interactionListener).onAdImpression();
    }

    @Test
    public void onImpression_whenAutomaticImpressionAndClickTrackingTrue_shouldSignalAdapterListener() {
        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();
        baseAd.setAutomaticImpressionAndClickTracking(true);
        subject.onAdImpression();

        verify(interactionListener, never()).onAdImpression();
    }

    @Test
    public void invalidate_shouldCauseShowToDoNothing() throws Exception {
        BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        subject.invalidate();

        subject.show(moPubAd);

        verify(baseAd, never()).internalShow(interactionListener);
    }

    @Test
    public void invalidate_shouldMakeBaseAdNull_shouldMakeListenerNull() throws Exception {
        subject.invalidate();

        assertThat(subject.mBaseAd).isNull();
        assertThat(subject.mLoadListener).isNull();
    }

    @Test
    public void invalidate_shouldCauseListenerMethodsToDoNothing() throws Exception {
        subject.invalidate();

        subject.onAdLoaded();
        subject.onAdFailed(UNSPECIFIED);
        subject.onAdShown();
        subject.onAdClicked();
        subject.onAdDismissed();

        verify(loadListener, never()).onAdLoaded();
        verify(loadListener, never()).onAdLoadFailed(isNotNull(MoPubErrorCode.class));
        verify(interactionListener, never()).onAdShown();
        verify(interactionListener, never()).onAdClicked();
        verify(interactionListener, never()).onAdDismissed();
    }

    @Test
    public void doInvalidate_withNonNullBaseAd_shouldCallBaseAdOnInvalidate() throws Exception {
        final BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        assertThat(subject.mBaseAd).isNotNull();

        subject.doInvalidate();

        verify(baseAd).onInvalidate();
    }

    @Test
    public void doInvalidate_withNullBaseAd_shouldNotCallBaseAdOnInvalidate() throws Exception {
        final BaseAd baseAd = TestBaseAdFactory.getSingletonMock();

        subject.mBaseAd = null;
        assertThat(subject.mBaseAd).isNull();

        subject.doInvalidate();

        verify(baseAd, never()).onInvalidate();
    }

    @Test
    public void doInvalidate_withNonNullBaseAd_withNonNullVisibilityTracker_shouldCallVisibilityTrackerDestroy() throws Exception {
        final BaseAd baseAd = TestBaseAdFactory.getSingletonMock();
        final InlineVisibilityTracker mockInlineVisibilityTracker = mock(InlineVisibilityTracker.class);

        subject.mVisibilityTracker = mockInlineVisibilityTracker;

        assertThat(subject.mBaseAd).isNotNull();
        assertThat(subject.mVisibilityTracker).isEqualTo(mockInlineVisibilityTracker);

        subject.doInvalidate();

        verify(mockInlineVisibilityTracker).destroy();
    }

    @Test
    public void doInvalidate_withNullBaseAd_withNonNullVisibilityTracker_shouldCallVisibilityTrackerDestroy() throws Exception {
        final BaseAd baseAd = TestBaseAdFactory.getSingletonMock();
        final InlineVisibilityTracker mockInlineVisibilityTracker = mock(InlineVisibilityTracker.class);

        subject.mVisibilityTracker = mockInlineVisibilityTracker;

        subject.mBaseAd = null;
        assertThat(subject.mBaseAd).isNull();
        assertThat(subject.mVisibilityTracker).isEqualTo(mockInlineVisibilityTracker);

        subject.doInvalidate();

        verify(baseAd, never()).onInvalidate();
        verify(mockInlineVisibilityTracker).destroy();
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNullImpressionMinVisibleDips_withNonNullNonEmptyImpressionMinVisibleMs_shouldNotSetImpressionMinVisibleDips() throws Exception {
        final String nullImpressionMinVisibleDips = null;
        final String nonNullNonEmptyImpressionMinVisibleMs = "1";

        final int originalImpressionMinVisibleDips = 999;
        final int originalImpressionMinVisibleMs = 888;

        subject.setImpressionMinVisibleDips(originalImpressionMinVisibleDips);
        subject.setImpressionMinVisibleMs(originalImpressionMinVisibleMs);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);

        subject.mAdData.setImpressionMinVisibleDips(nullImpressionMinVisibleDips);
        subject.mAdData.setImpressionMinVisibleMs(nonNullNonEmptyImpressionMinVisibleMs);

        subject.parseBannerImpressionTrackingHeaders();

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withEmptyImpressionMinVisibleDips_withNonNullNonEmptyImpressionMinVisibleMs_shouldNotSetImpressionMinVisibleDips() throws Exception {
        final String emptyImpressionMinVisibleDips = "";
        final String nonNullNonEmptyImpressionMinVisibleMs = "1";

        final int originalImpressionMinVisibleDips = 999;
        final int originalImpressionMinVisibleMs = 888;

        subject.setImpressionMinVisibleDips(originalImpressionMinVisibleDips);
        subject.setImpressionMinVisibleMs(originalImpressionMinVisibleMs);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);

        subject.mAdData.setImpressionMinVisibleDips(emptyImpressionMinVisibleDips);
        subject.mAdData.setImpressionMinVisibleMs(nonNullNonEmptyImpressionMinVisibleMs);

        subject.parseBannerImpressionTrackingHeaders();

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNonNullNonEmptyImpressionMinVisibleDips_withNullImpressionMinVisibleMs_shouldNotSetImpressionMinVisibleDips() throws Exception {
        final String nonNullNonEmptyImpressionMinVisibleDips = "2";
        final String nullImpressionMinVisibleMs = null;

        final int originalImpressionMinVisibleDips = 999;
        final int originalImpressionMinVisibleMs = 888;

        subject.setImpressionMinVisibleDips(originalImpressionMinVisibleDips);
        subject.setImpressionMinVisibleMs(originalImpressionMinVisibleMs);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);

        subject.mAdData.setImpressionMinVisibleDips(nonNullNonEmptyImpressionMinVisibleDips);
        subject.mAdData.setImpressionMinVisibleMs(nullImpressionMinVisibleMs);

        subject.parseBannerImpressionTrackingHeaders();

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNonNullNonEmptyImpressionMinVisibleDips_withEmptyImpressionMinVisibleMs_shouldNotSetImpressionMinVisibleDips() throws Exception {
        final String nonNullNonEmptyImpressionMinVisibleDips = "2";
        final String emptyImpressionMinVisibleMs = "";

        final int originalImpressionMinVisibleDips = 999;
        final int originalImpressionMinVisibleMs = 888;

        subject.setImpressionMinVisibleDips(originalImpressionMinVisibleDips);
        subject.setImpressionMinVisibleMs(originalImpressionMinVisibleMs);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);

        subject.mAdData.setImpressionMinVisibleDips(nonNullNonEmptyImpressionMinVisibleDips);
        subject.mAdData.setImpressionMinVisibleMs(emptyImpressionMinVisibleMs);

        subject.parseBannerImpressionTrackingHeaders();

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);
    }

    @Test
    public void parseBannerImpressionTrackingHeaders_withNonNullNonEmptyImpressionMinVisibleDips_withNonNullNonEmptyImpressionMinVisibleMs_shouldNotSetImpressionMinVisibleDips() throws Exception {
        final String nonNullNonEmptyImpressionMinVisibleDips = "3";
        final String nonNullNonEmptyImpressionMinVisibleMs = "4";

        final int originalImpressionMinVisibleDips = 999;
        final int originalImpressionMinVisibleMs = 888;

        subject.setImpressionMinVisibleDips(originalImpressionMinVisibleDips);
        subject.setImpressionMinVisibleMs(originalImpressionMinVisibleMs);

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(originalImpressionMinVisibleDips);
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(originalImpressionMinVisibleMs);

        subject.mAdData.setImpressionMinVisibleDips(nonNullNonEmptyImpressionMinVisibleDips);
        subject.mAdData.setImpressionMinVisibleMs(nonNullNonEmptyImpressionMinVisibleMs);

        subject.parseBannerImpressionTrackingHeaders();

        assertThat(subject.getImpressionMinVisibleDips()).isEqualTo(Integer.parseInt(nonNullNonEmptyImpressionMinVisibleDips));
        assertThat(subject.getImpressionMinVisibleMs()).isEqualTo(Integer.parseInt(nonNullNonEmptyImpressionMinVisibleMs));
    }
}
