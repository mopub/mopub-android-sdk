package com.mopub.nativeads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.BaseNativeAd.NativeEventListener;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.nativeads.NativeAd.MoPubNativeEventListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeAdTest {

    private NativeAd subject;
    private Activity activity;

    @Mock private View mockView;
    @Mock private ViewGroup mockParent;
    @Mock private MoPubStaticNativeAdRenderer mockRenderer;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private MoPubNativeEventListener mockEventListener;
    @Mock private BaseNativeAd mockBaseNativeAd;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        Networking.setRequestQueueForTesting(mockRequestQueue);

        Set<String> impUrls = new HashSet<String>();
        impUrls.add("impUrl");
        when(mockBaseNativeAd.getImpressionTrackers()).thenReturn(impUrls);

        Set<String> clkUrls = new HashSet<String>();
        clkUrls.add("clkUrl");
        when(mockBaseNativeAd.getClickTrackers()).thenReturn(clkUrls);

        subject = new NativeAd(activity,
                "moPubImpressionTrackerUrl",
                "moPubClickTrackerUrl",
                "adunit_id",
                mockBaseNativeAd,
                mockRenderer
        );
        subject.setMoPubNativeEventListener(mockEventListener);
    }

    @Test
    public void constructor_shouldSetNativeEventListener() {
        reset(mockBaseNativeAd);
        subject = new NativeAd(activity, "moPubImpressionTrackerUrl", "moPubClickTrackerUrl",
                "adunit_id", mockBaseNativeAd, mockRenderer);
        verify(mockBaseNativeAd).setNativeEventListener(any(NativeEventListener.class));
    }

    @Test
    public void constructor_shouldMergeMoPubClickTrackerWithBaseNativeAdClickTrackers() {
        reset(mockRequestQueue);
        subject = new NativeAd(activity, "", "moPubClickTrackerUrl", "", mockBaseNativeAd,
                mockRenderer);

        subject.handleClick(null);

        verify(mockRequestQueue).add(argThat(isUrl("moPubClickTrackerUrl")));
        verify(mockRequestQueue).add(argThat(isUrl("clkUrl")));
    }

    @Test
    public void getAdUnitId_shouldReturnAdUnitId() {
        assertThat(subject.getAdUnitId()).isEqualTo("adunit_id");
    }

    @Test
    public void isDestroyed_withNativeAdDestroyed_shouldReturnTrue() {
        assertThat(subject.isDestroyed()).isFalse();

        subject.destroy();

        assertThat(subject.isDestroyed()).isTrue();
    }

    @Test
    public void createAdView_shouldCallCreateAdViewOnRenderer() {
        View newView = mock(View.class);
        when(mockRenderer.createAdView(activity, mockParent))
                .thenReturn(newView);

        View view = subject.createAdView(activity, mockParent);

        verify(mockRenderer).createAdView(activity, mockParent);
        assertThat(view).isEqualTo(newView);
    }

    @Test
    public void renderAdView_shouldCallRenderAdViewOnRenderer() {
        subject.createAdView(activity, mockParent);

        verify(mockRenderer).createAdView(activity, mockParent);
    }

    @Test
    public void prepare_shouldCallPrepareOnBaseNativeAd() {
        subject.prepare(mockView);

        verify(mockBaseNativeAd).prepare(mockView);
    }

    @Test
    public void prepare_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.prepare(mockView);

        verify(mockBaseNativeAd, never()).prepare(mockView);
    }

    @Test
    public void clear_shouldCallClearOnBaseNativeAd() {
        subject.clear(mockView);
        verify(mockBaseNativeAd).clear(mockView);
    }

    @Test
    public void destroy_shouldCallIntoBaseNativeAdOnce() {
        subject.destroy();
        verify(mockBaseNativeAd).destroy();

        reset(mockBaseNativeAd);

        subject.destroy();
        verifyZeroInteractions(mockBaseNativeAd);
    }

    @Test
    public void recordImpression_shouldRecordImpressionsOnce() {
        subject.recordImpression(mockView);
        verify(mockRequestQueue).add(argThat(isUrl("moPubImpressionTrackerUrl")));
        verify(mockRequestQueue).add(argThat(isUrl("impUrl")));
        verify(mockEventListener).onImpression(mockView);

        // reset state
        reset(mockRequestQueue);

        // verify impression tracking doesn't fire again
        subject.recordImpression(mockView);
        verifyZeroInteractions(mockRequestQueue);
        verifyZeroInteractions(mockEventListener);
    }

    @Test
    public void recordImpression_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.recordImpression(mockView);
        verifyZeroInteractions(mockRequestQueue);
        verifyZeroInteractions(mockEventListener);
    }

    @Test
    public void handleClick_shouldTrackClicksOnce() {
        subject.handleClick(mockView);
        verify(mockRequestQueue).add(argThat(isUrl("moPubClickTrackerUrl")));
        verify(mockRequestQueue).add(argThat(isUrl("clkUrl")));
        verify(mockEventListener).onClick(mockView);

        // reset state
        reset(mockRequestQueue);

        // verify click tracking doesn't fire again
        subject.handleClick(mockView);
        verifyZeroInteractions(mockRequestQueue);
        verifyZeroInteractions(mockEventListener);
    }

    @Test
    public void handleClick_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.handleClick(mockView);
        verifyZeroInteractions(mockRequestQueue);
        verifyZeroInteractions(mockEventListener);
    }
}
