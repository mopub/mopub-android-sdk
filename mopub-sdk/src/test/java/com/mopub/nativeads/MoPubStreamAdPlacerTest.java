package com.mopub.nativeads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.PositioningSource.PositioningListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubStreamAdPlacerTest {
    private Activity activity;

    MoPubClientPositioning positioning;

    @Mock
    PositioningSource mockPositioningSource;
    @Mock
    NativeAdSource mockAdSource;
    @Mock
    MoPubStaticNativeAdRenderer mockAdRenderer;
    @Mock
    MoPubNativeAdLoadedListener mockAdLoadedListener;
    @Mock
    ImpressionTracker mockImpressionTracker;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    NativeAd mStubNativeAd;

    private MoPubStreamAdPlacer subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        positioning = MoPubNativeAdPositioning.clientPositioning()
                .enableRepeatingPositions(2);

        subject = new MoPubStreamAdPlacer(activity, mockAdSource, mockPositioningSource);
        subject.registerAdRenderer(mockAdRenderer);
        subject.setAdLoadedListener(mockAdLoadedListener);
    }

    @Test
    public void isAd_initialState_hasNoAds() {
        checkAdPositions();
    }

    @Test
    public void isAd_loadPositions_withoutLoadingAds_hasNoAds() {
        subject.handlePositioningLoad(positioning);
        checkAdPositions();
    }

    @Test
    public void isAd_loadAds_withoutLoadingPositions_hasNoAds() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.handleAdsAvailable();
        subject.setItemCount(4);
        checkAdPositions();
    }

    @Test
    public void isAd_loadAds_thenLoadPositions_hasAds() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");

        subject.handleAdsAvailable();
        subject.handlePositioningLoad(positioning);
        subject.setItemCount(4);
        checkAdPositions(1, 3, 5);
    }

    @Test
    public void isAd_loadPositions_thenLoadAds_hasAds() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");

        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);
        checkAdPositions(1, 3, 5);
    }

    @Test
    public void getAdViewTypeCount_shouldAdSourceCallGetAdRendererCount() throws Exception {
        subject.getAdViewTypeCount();
        verify(mockAdSource).getAdRendererCount();
    }

    @Test
    public void getOriginalPosition_adjustsPositions() {
        assertThat(subject.getOriginalPosition(0)).isEqualTo(0);
        assertThat(subject.getOriginalPosition(1)).isEqualTo(1);
        assertThat(subject.getOriginalPosition(2)).isEqualTo(2);
        assertThat(subject.getOriginalPosition(3)).isEqualTo(3);
        assertThat(subject.getOriginalPosition(4)).isEqualTo(4);

        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        assertThat(subject.getOriginalPosition(0)).isEqualTo(0);
        assertThat(subject.getOriginalPosition(1)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalPosition(2)).isEqualTo(1);
        assertThat(subject.getOriginalPosition(3)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalPosition(4)).isEqualTo(2);
        assertThat(subject.getOriginalPosition(5)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalPosition(6)).isEqualTo(3);
        assertThat(subject.getOriginalPosition(7)).isEqualTo(4);
        assertThat(subject.getOriginalPosition(8)).isEqualTo(5);
        assertThat(subject.getOriginalPosition(9)).isEqualTo(6);
    }

    @Test
    public void getAdjustedPosition_adjustsPositions() {
        assertThat(subject.getAdjustedPosition(0)).isEqualTo(0);
        assertThat(subject.getAdjustedPosition(1)).isEqualTo(1);
        assertThat(subject.getAdjustedPosition(2)).isEqualTo(2);
        assertThat(subject.getAdjustedPosition(3)).isEqualTo(3);
        assertThat(subject.getAdjustedPosition(4)).isEqualTo(4);

        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        assertThat(subject.getAdjustedPosition(0)).isEqualTo(0);
        assertThat(subject.getAdjustedPosition(1)).isEqualTo(2);
        assertThat(subject.getAdjustedPosition(2)).isEqualTo(4);
        assertThat(subject.getAdjustedPosition(3)).isEqualTo(6);
        assertThat(subject.getAdjustedPosition(4)).isEqualTo(7);
        assertThat(subject.getAdjustedPosition(5)).isEqualTo(8);
        assertThat(subject.getAdjustedPosition(6)).isEqualTo(9);
        assertThat(subject.getAdjustedPosition(7)).isEqualTo(10);
        assertThat(subject.getAdjustedPosition(8)).isEqualTo(11);
        assertThat(subject.getAdjustedPosition(9)).isEqualTo(12);
    }

    @Test
    public void getOriginalCount_adjustsPositions() {
        assertThat(subject.getOriginalCount(0)).isEqualTo(0);
        assertThat(subject.getOriginalCount(1)).isEqualTo(1);
        assertThat(subject.getOriginalCount(2)).isEqualTo(2);
        assertThat(subject.getOriginalCount(3)).isEqualTo(3);
        assertThat(subject.getOriginalCount(4)).isEqualTo(4);

        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        assertThat(subject.getOriginalCount(0)).isEqualTo(0);
        assertThat(subject.getOriginalCount(1)).isEqualTo(1);
        assertThat(subject.getOriginalCount(2)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalCount(3)).isEqualTo(2);
        assertThat(subject.getOriginalCount(4)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalCount(5)).isEqualTo(3);
        assertThat(subject.getOriginalCount(6)).isEqualTo(PlacementData.NOT_FOUND);
        assertThat(subject.getOriginalCount(7)).isEqualTo(4);
        assertThat(subject.getOriginalCount(8)).isEqualTo(5);
        assertThat(subject.getOriginalCount(9)).isEqualTo(6);
    }

    @Test
    public void getAdjustedCount_adjustsPositions() {
        assertThat(subject.getAdjustedCount(0)).isEqualTo(0);
        assertThat(subject.getAdjustedCount(1)).isEqualTo(1);
        assertThat(subject.getAdjustedCount(2)).isEqualTo(2);
        assertThat(subject.getAdjustedCount(3)).isEqualTo(3);
        assertThat(subject.getAdjustedCount(4)).isEqualTo(4);

        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        assertThat(subject.getAdjustedCount(0)).isEqualTo(0);
        assertThat(subject.getAdjustedCount(1)).isEqualTo(1);
        assertThat(subject.getAdjustedCount(2)).isEqualTo(3);
        assertThat(subject.getAdjustedCount(3)).isEqualTo(5);
        assertThat(subject.getAdjustedCount(4)).isEqualTo(7);
        assertThat(subject.getAdjustedCount(5)).isEqualTo(8);
        assertThat(subject.getAdjustedCount(6)).isEqualTo(9);
        assertThat(subject.getAdjustedCount(7)).isEqualTo(10);
        assertThat(subject.getAdjustedCount(8)).isEqualTo(11);
        assertThat(subject.getAdjustedCount(9)).isEqualTo(12);
    }

    @Test
    public void placeAds_shouldCallListener() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        verify(mockAdLoadedListener, never()).onAdLoaded(0);
        verify(mockAdLoadedListener).onAdLoaded(1);
        verify(mockAdLoadedListener, never()).onAdLoaded(2);
        verify(mockAdLoadedListener).onAdLoaded(3);
        verify(mockAdLoadedListener, never()).onAdLoaded(4);
        verify(mockAdLoadedListener).onAdLoaded(5);
        verify(mockAdLoadedListener, never()).onAdLoaded(6);
        verify(mockAdLoadedListener, never()).onAdLoaded(7);
    }

    @Test
    public void placeAdsInRange_shouldPlaceAfter() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();

        Robolectric.getForegroundThreadScheduler().pause();
        subject.setItemCount(100);
        subject.placeAdsInRange(50, 50);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        assertThat(subject.isAd(48)).isFalse();
        assertThat(subject.isAd(49)).isFalse();
        assertThat(subject.isAd(50)).isTrue();
        assertThat(subject.isAd(51)).isFalse();
        assertThat(subject.isAd(52)).isTrue();
        assertThat(subject.isAd(53)).isFalse();
        assertThat(subject.isAd(54)).isTrue();
    }

    @Test
    public void placeAdsInRange_shouldCallListener() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();

        Robolectric.getForegroundThreadScheduler().pause();
        subject.setItemCount(100);
        subject.placeAdsInRange(50, 54);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        verify(mockAdLoadedListener).onAdLoaded(50);
        verify(mockAdLoadedListener, never()).onAdLoaded(51);
        verify(mockAdLoadedListener).onAdLoaded(52);
        verify(mockAdLoadedListener, never()).onAdLoaded(53);
        verify(mockAdLoadedListener).onAdLoaded(54);
        verify(mockAdLoadedListener, never()).onAdLoaded(55);
        verify(mockAdLoadedListener).onAdLoaded(56);
    }

    @Test
    public void placeAdsInRange_aboveItemCount_shouldNotInsert() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();

        Robolectric.getForegroundThreadScheduler().pause();
        subject.setItemCount(0);
        subject.placeAdsInRange(50, 54);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        verify(mockAdLoadedListener, never()).onAdLoaded(50);
    }

    @Test
    public void getAdView_withNoAds_returnsNull() {
        assertThat(subject.getAdView(1, null, null)).isNull();
    }

    @Test
    public void loadAds_shouldClearAds_afterFirstAdLoads() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(2);
        subject.placeAdsInRange(0, 1);

        subject.loadAds("test-ad-unit-id");

        // Ad should still exist until a new ad is available
        checkAdPositions(1);
        verify(mockAdLoadedListener, never()).onAdRemoved(anyInt());

        // Once an ad is available, it should be immediately removed and replaced
        subject.handleAdsAvailable();
        subject.handlePositioningLoad(positioning);
        verify(mockAdLoadedListener).onAdRemoved(1);
        verify(mockAdLoadedListener, times(2)).onAdLoaded(1);
        checkAdPositions(1);
    }

    @Test
    public void loadAds_withFailingPositioningSource_shouldNotLoadAds() {
        ArgumentCaptor<PositioningListener> listenerCaptor =
                ArgumentCaptor.forClass(PositioningListener.class);

        subject.registerAdRenderer(mockAdRenderer);
        when(mockAdSource.getAdRendererCount()).thenReturn(1);
        subject.loadAds("test-ad-unit-id");

        verify(mockPositioningSource).loadPositions(
                eq("test-ad-unit-id"), listenerCaptor.capture());
        listenerCaptor.getValue().onFailed();
        verify(mockAdLoadedListener, never()).onAdLoaded(anyInt());
    }

    @Test
    public void destroy_shouldClearAdSource_shouldDestroyImpressionTracker_shouldDestroyNativeAd() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);

        subject = new MoPubStreamAdPlacer(activity, mockAdSource, mockPositioningSource);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(2);  // Places 1 ad

        subject.destroy();

        verify(mockAdSource).clear();
        verify(mStubNativeAd).destroy();
    }

    @Test
    public void getAdView_withNullConvertView_callsRenderer_addsToImpressionTracker() {
        View view = new View(activity);
        when(mStubNativeAd.createAdView(any(Activity.class), any(ViewGroup.class))).thenReturn(view);
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(100);

        assertThat(subject.getAdView(1, null, null)).isEqualTo(view);

        verify(mStubNativeAd).createAdView(eq(activity), any(ViewGroup.class));
        verify(mStubNativeAd).renderAdView(view);
    }

    @Test
    public void getAdView_withConvertView_shouldCallRenderer() {
        View convertView = new View(activity);
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(4);

        assertThat(subject.getAdView(1, convertView, null)).isEqualTo(convertView);
        verify(mStubNativeAd, never()).createAdView(any(Activity.class), any(ViewGroup.class));
        verify(mStubNativeAd).renderAdView(convertView);
    }

    @Test
    public void getAdView_shouldClearPreviousNativeAd() throws Exception {
        NativeAd mockNativeAd = mock(NativeAd.class);
        View mockView = mock(View.class);
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd, mockNativeAd, mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(100);

        subject.getAdView(1, mockView, null);

        // Second call should clear the first NativeAd
        subject.getAdView(3, mockView, null);
        verify(mStubNativeAd).clear(mockView);

        // Third call should clear the second NativeAd
        subject.getAdView(5, mockView, null);
        verify(mockNativeAd).clear(mockView);
    }

    @Test
    public void getAdView_shouldPrepareNativeAd() throws Exception {
        View mockView = mock(View.class);
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(100);

        subject.getAdView(1, mockView, null);

        verify(mStubNativeAd).prepare(mockView);
    }

    @Test
    public void destroy_shouldClearAdSource_shouldResetPlacementData() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);

        subject = new MoPubStreamAdPlacer(activity, mockAdSource, mockPositioningSource);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        subject.handleAdsAvailable();
        subject.setItemCount(2);  // Places 1 ad

        subject.destroy();

        verify(mockAdSource).clear();
        verify(mStubNativeAd).destroy();
    }

    @Test
    public void modifyClientPositioning_afterConstructingAdPlacer_shouldNotModifyAdPositions() {
        when(mockAdSource.dequeueAd()).thenReturn(mStubNativeAd);
        subject.registerAdRenderer(mockAdRenderer);
        subject.loadAds("test-ad-unit-id");
        subject.handlePositioningLoad(positioning);
        positioning.enableRepeatingPositions(5);

        subject.handleAdsAvailable();
        subject.setItemCount(4);
        checkAdPositions(1, 3, 5);
    }

    void checkAdPositions(Integer... positions) {
        List<Integer> expected = Arrays.asList(positions);
        List<Integer> actual = new ArrayList<Integer>();
        for (int i = 0; i < 20; i++) {
            if (subject.isAd(i)) {
                actual.add(i);
                assertThat(subject.getAdData(i)).isNotNull();
            } else {
                assertThat(subject.getAdData(i)).isNull();
            }
        }

        assertThat(actual).isEqualTo(expected);
    }
}
