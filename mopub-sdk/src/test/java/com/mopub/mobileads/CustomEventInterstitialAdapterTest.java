package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.CustomEventInterstitialFactory;

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
import java.util.TreeMap;

import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CustomEventInterstitialAdapterTest {
    private static long BROADCAST_IDENTIFER = 123;
    private CustomEventInterstitialAdapter subject;
    @Mock
    private MoPubInterstitial mockMoPubInterstitial;
    private CustomEventInterstitial interstitial;
    private Map<String, Object> expectedLocalExtras;
    private HashMap<String, String> expectedServerExtras;
    @Mock
    private AdViewController mockAdViewController;
    @Mock
    private AdReport mockAdReport;
    private MoPubInterstitial.MoPubInterstitialView moPubInterstitialView;
    private static final String CLASS_NAME = "arbitrary_interstitial_adapter_class_name";
    private Map<String, String> serverExtras;
    private CustomEventInterstitialAdapter.CustomEventInterstitialAdapterListener interstitialAdapterListener;

    @Before
    public void setUp() throws Exception {

        stub(mockMoPubInterstitial.getAdTimeoutDelay()).toReturn(null);
        moPubInterstitialView = mock(MoPubInterstitial.MoPubInterstitialView.class);
        stub(moPubInterstitialView.getAdViewController()).toReturn(mockAdViewController);
        stub(mockAdViewController.getAdReport()).toReturn(mockAdReport);
        stub(mockMoPubInterstitial.getMoPubInterstitialView()).toReturn(moPubInterstitialView);

        serverExtras = new HashMap<String, String>();
        serverExtras.put("key", "value");

        subject = new CustomEventInterstitialAdapter(mockMoPubInterstitial, CLASS_NAME, serverExtras, BROADCAST_IDENTIFER, mockAdViewController.getAdReport());

        expectedLocalExtras = new HashMap<String, Object>();
        expectedServerExtras = new HashMap<String, String>();

        interstitial = CustomEventInterstitialFactory.create(CLASS_NAME);

        interstitialAdapterListener = mock(CustomEventInterstitialAdapter.CustomEventInterstitialAdapterListener.class);
        subject.setAdapterListener(interstitialAdapterListener);
    }

    @Test
    public void constructor_withInvalidClassName_shouldCallOnCustomEventInterstitialFailed() throws Exception {
        // Remove testing mock and use the real thing
        CustomEventInterstitialFactory.setInstance(new CustomEventInterstitialFactory());

        new CustomEventInterstitialAdapter(mockMoPubInterstitial, "bad_class_name_11i234jb", new TreeMap<String, String>(), BROADCAST_IDENTIFER, mockAdViewController.getAdReport());
        verify(mockMoPubInterstitial).onCustomEventInterstitialFailed(ADAPTER_NOT_FOUND);
    }

    @Test
    public void timeout_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        subject.loadInterstitial();
        ShadowLooper.idleMainLooper(CustomEventInterstitialAdapter.DEFAULT_INTERSTITIAL_TIMEOUT_DELAY - 1);
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(interstitialAdapterListener).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNegativeAdTimeoutDelay_shouldSignalFailureAndInvalidateWithDefaultDelay() throws Exception {
        stub(mockMoPubInterstitial.getAdTimeoutDelay()).toReturn(-1);

        subject.loadInterstitial();
        ShadowLooper.idleMainLooper(CustomEventInterstitialAdapter.DEFAULT_INTERSTITIAL_TIMEOUT_DELAY - 1);
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(interstitialAdapterListener).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void timeout_withNonNullAdTimeoutDelay_shouldSignalFailureAndInvalidateWithCustomDelay() throws Exception {
        stub(mockMoPubInterstitial.getAdTimeoutDelay()).toReturn(77);

        subject.loadInterstitial();
        ShadowLooper.idleMainLooper(77000 - 1);
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isFalse();

        ShadowLooper.idleMainLooper(1);
        verify(interstitialAdapterListener).onCustomEventInterstitialFailed(eq(NETWORK_TIMEOUT));
        assertThat(subject.isInvalidated()).isTrue();
    }

    @Test
    public void loadInterstitial_shouldPropagateLocationInLocalExtras() throws Exception {
        Location expectedLocation = new Location("");
        expectedLocation.setLongitude(10.0);
        expectedLocation.setLongitude(20.1);
        stub(mockMoPubInterstitial.getLocation()).toReturn(expectedLocation);
        subject = new CustomEventInterstitialAdapter(mockMoPubInterstitial, CLASS_NAME, new HashMap<String, String>(), BROADCAST_IDENTIFER, mockAdViewController.getAdReport());
        subject.loadInterstitial();

        expectedLocalExtras.put("broadcastIdentifier", BROADCAST_IDENTIFER);
        expectedLocalExtras.put(DataKeys.AD_REPORT_KEY, mockAdReport);
        expectedLocalExtras.put("location", mockMoPubInterstitial.getLocation());

        verify(interstitial).loadInterstitial(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadInterstitial_shouldPropagateServerExtrasToInterstitial() throws Exception {
        subject.loadInterstitial();
        expectedLocalExtras.put("broadcastIdentifier", BROADCAST_IDENTIFER);
        expectedLocalExtras.put(DataKeys.AD_REPORT_KEY, mockAdReport);
        expectedServerExtras.put("key", "value");

        verify(interstitial).loadInterstitial(
                any(Context.class),
                eq(subject),
                eq(expectedLocalExtras),
                eq(expectedServerExtras)
        );
    }

    @Test
    public void loadInterstitial_shouldScheduleTimeout_interstitialLoadedAndFailed_shouldCancelTimeout() throws Exception {
        ShadowLooper.pauseMainLooper();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.loadInterstitial();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onInterstitialLoaded();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);

        subject.loadInterstitial();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);

        subject.onInterstitialFailed(null);
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void loadInterstitial_shouldScheduleTimeoutRunnableBeforeCallingLoadInterstitial() throws Exception {
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
                .when(interstitial)
                .loadInterstitial(
                        any(Context.class),
                        any(CustomEventInterstitialListener.class),
                        any(Map.class),
                        any(Map.class)
                );

        subject.loadInterstitial();
    }

    @Test
    public void loadInterstitial_whenCallingOnInterstitialFailed_shouldCancelExistingTimeoutRunnable() throws Exception {
        ShadowLooper.pauseMainLooper();

        Answer justCallOnInterstitialFailed = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(1);
                subject.onInterstitialFailed(null);
                assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
                return null;
            }
        };

        // noinspection unchecked
        doAnswer(justCallOnInterstitialFailed)
                .when(interstitial)
                .loadInterstitial(
                        any(Context.class),
                        any(CustomEventInterstitialListener.class),
                        any(Map.class),
                        any(Map.class)
                );

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
        subject.loadInterstitial();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void showInterstitial_shouldCallCustomEventInterstitialShowInterstitial() throws Exception {
        subject.showInterstitial();

        verify(interstitial).showInterstitial();
    }

    @Test
    public void onInterstitialLoaded_shouldSignalAdapterListener() throws Exception {
        subject.onInterstitialLoaded();

        verify(interstitialAdapterListener).onCustomEventInterstitialLoaded();
    }

    @Test
    public void onInterstitialFailed_shouldLoadFailUrl() throws Exception {
        subject.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);

        verify(interstitialAdapterListener).onCustomEventInterstitialFailed(eq(ADAPTER_CONFIGURATION_ERROR));
    }

    @Test
    public void onInterstitialFailed_whenErrorCodeIsNull_shouldPassUnspecifiedError() throws Exception {
        subject.onInterstitialFailed(null);

        verify(interstitialAdapterListener).onCustomEventInterstitialFailed(eq(UNSPECIFIED));
    }

    @Test
    public void onInterstitialShown_shouldSignalAdapterListener() throws Exception {
        subject.onInterstitialShown();

        verify(interstitialAdapterListener).onCustomEventInterstitialShown();
    }

    @Test
    public void onInterstitialClicked_shouldSignalAdapterListener() throws Exception {
        subject.onInterstitialClicked();

        verify(interstitialAdapterListener).onCustomEventInterstitialClicked();
    }

    @Test
    public void onLeaveApplication_shouldSignalAdapterListener() throws Exception {
        subject.onLeaveApplication();

        verify(interstitialAdapterListener).onCustomEventInterstitialClicked();
    }

    @Test
    public void onInterstitialDismissed_shouldSignalAdapterListener() throws Exception {
        subject.onInterstitialDismissed();

        verify(interstitialAdapterListener).onCustomEventInterstitialDismissed();
    }

    @Test
    public void invalidate_shouldCauseLoadInterstitialToDoNothing() throws Exception {
        subject.invalidate();

        subject.loadInterstitial();

        // noinspection unchecked
        verify(interstitial, never()).loadInterstitial(
                any(Context.class),
                any(CustomEventInterstitialListener.class),
                any(Map.class),
                any(Map.class)
        );
    }

    @Test
    public void invalidate_shouldCauseShowInterstitialToDoNothing() throws Exception {
        subject.invalidate();

        subject.showInterstitial();

        verify(interstitial, never()).showInterstitial();
    }

    @Test
    public void invalidate_shouldCauseInterstitialListenerMethodsToDoNothing() throws Exception {
        subject.invalidate();

        subject.onInterstitialLoaded();
        subject.onInterstitialFailed(null);
        subject.onInterstitialShown();
        subject.onInterstitialClicked();
        subject.onLeaveApplication();
        subject.onInterstitialDismissed();

        verify(interstitialAdapterListener, never()).onCustomEventInterstitialLoaded();
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialFailed(any(MoPubErrorCode.class));
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialShown();
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialClicked();
        verify(interstitialAdapterListener, never()).onCustomEventInterstitialDismissed();
    }
}
