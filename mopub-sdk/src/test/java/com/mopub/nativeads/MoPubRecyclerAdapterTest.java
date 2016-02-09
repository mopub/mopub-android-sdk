package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.INSERT_AT_END;
import static com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.KEEP_ADS_FIXED;
import static com.mopub.nativeads.MoPubRecyclerAdapter.ContentChangeStrategy.MOVE_ALL_ADS_WITH_CONTENT;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubRecyclerAdapterTest {
    private static final int AD_POSITION_1 = 1;
    private static final int AD_POSITION_7 = 7;

    @Mock MoPubStreamAdPlacer mockStreamAdPlacer;
    @Mock VisibilityTracker mockVisibilityTracker;
    @Mock
    NativeAd mMockNativeAd;
    @Mock
    NativeAd mMockNativeAd2;
    @Mock MoPubAdRenderer mockAdRenderer;
    @Mock ViewGroup mockParent;
    @Mock View mockAdView;
    @Mock RecyclerView.AdapterDataObserver mockObserver;
    @Mock MoPubNativeAdLoadedListener mockAdLoadedListener;
    MoPubRecyclerViewHolder spyViewHolder;
    @Mock TestHolder mockTestHolder;
    TestAdapter originalAdapter;

    MoPubRecyclerAdapter subject;

    @Mock RecyclerView mockRecyclerView;
    @Mock LinearLayoutManager mockLayoutManager;

    @Before
    public void setUp() throws Exception {
        originalAdapter = spy(new TestAdapter());
        subject = new MoPubRecyclerAdapter(mockStreamAdPlacer, originalAdapter, mockVisibilityTracker);

        spyViewHolder = spy(new MoPubRecyclerViewHolder(mockAdView));

        // Reset because the constructor interacts with the stream ad placer, and we don't want
        // to worry about verifying those changes in tests.
        reset(mockStreamAdPlacer);
        reset(originalAdapter);

        // Mock some simple adjustment behavior for tests. This is creating an ad placer that
        // emulates a content item followed by an ad item, then another content item.
        when(mockStreamAdPlacer.getAdData(AD_POSITION_1)).thenReturn(mMockNativeAd);
        when(mockStreamAdPlacer.getAdData(AD_POSITION_7)).thenReturn(mMockNativeAd2);
        when(mockStreamAdPlacer.getAdRendererForViewType(MoPubRecyclerAdapter.NATIVE_AD_VIEW_TYPE_BASE))
                .thenReturn(mockAdRenderer);
        when(mockAdRenderer.createAdView(any(Activity.class), any(ViewGroup.class)))
                .thenReturn(mockAdView);

        when(mockStreamAdPlacer.isAd(anyInt())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                int position = (Integer) invocation.getArguments()[0];
                return position == AD_POSITION_1 || position == AD_POSITION_7;
            }
        });
        when(mockStreamAdPlacer.getOriginalPosition(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                final int adjusted = (Integer) invocation.getArguments()[0];
                int original;
                if (adjusted < AD_POSITION_1) {
                    original = adjusted;
                } else if (adjusted >= AD_POSITION_7) {
                    original = adjusted - 2;
                } else {
                    original = adjusted - 1;
                }
                return original;
            }
        });
        when(mockStreamAdPlacer.getAdjustedPosition(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                final int originalPosition = (Integer) invocation.getArguments()[0];
                int adjusted;
                if (originalPosition < AD_POSITION_1) {
                    adjusted = originalPosition;
                } else if (originalPosition > AD_POSITION_7) {
                    adjusted = originalPosition + 2;
                } else {
                    adjusted = originalPosition + 1;
                }
                return adjusted;
            }
        });
        when(mockStreamAdPlacer.getAdViewType(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                final int originalPosition = (Integer) invocation.getArguments()[0];
                return (originalPosition == AD_POSITION_1 || originalPosition == AD_POSITION_7)
                        ? 1 : MoPubStreamAdPlacer.CONTENT_VIEW_TYPE;
            }
        });

        when(mockStreamAdPlacer.getAdjustedCount(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                final int originalItemCount = (Integer) invocation.getArguments()[0];
                int adjusted;
                if (originalItemCount < AD_POSITION_1) {
                    adjusted = originalItemCount;
                } else if (originalItemCount > AD_POSITION_7) {
                    adjusted = originalItemCount + 2;
                } else {
                    adjusted = originalItemCount + 1;
                }
                return adjusted;
            }
        });
    }

    @Test
    public void computeScrollOffset_withScrollVerticallyNoStackFromEnd_shouldComputeTopOffset() {
        when(mockAdView.getTop()).thenReturn(13);
        when(mockAdView.getBottom()).thenReturn(14);
        when(mockAdView.getLeft()).thenReturn(10);
        when(mockAdView.getRight()).thenReturn(11);
        when(mockLayoutManager.canScrollVertically()).thenReturn(true);
        when(mockLayoutManager.canScrollHorizontally()).thenReturn(false);
        when(mockLayoutManager.getStackFromEnd()).thenReturn(false);

        int offset = MoPubRecyclerAdapter.computeScrollOffset(mockLayoutManager, spyViewHolder);
        assertThat(offset).isEqualTo(13);
    }

    @Test
    public void computeScrollOffset_withScrollVerticallyStackFromEnd_shouldComputeBottomOffset() {
        when(mockAdView.getTop()).thenReturn(13);
        when(mockAdView.getBottom()).thenReturn(14);
        when(mockAdView.getLeft()).thenReturn(10);
        when(mockAdView.getRight()).thenReturn(11);
        when(mockLayoutManager.canScrollVertically()).thenReturn(true);
        when(mockLayoutManager.canScrollHorizontally()).thenReturn(false);
        when(mockLayoutManager.getStackFromEnd()).thenReturn(true);

        int offset = MoPubRecyclerAdapter.computeScrollOffset(mockLayoutManager, spyViewHolder);
        assertThat(offset).isEqualTo(14);
    }

    @Test
    public void computeScrollOffset_withScrollHorizontallyStackFromEnd_shouldComputeLeftOffset() {
        when(mockAdView.getTop()).thenReturn(13);
        when(mockAdView.getBottom()).thenReturn(14);
        when(mockAdView.getLeft()).thenReturn(10);
        when(mockAdView.getRight()).thenReturn(11);
        when(mockLayoutManager.canScrollVertically()).thenReturn(false);
        when(mockLayoutManager.canScrollHorizontally()).thenReturn(true);
        when(mockLayoutManager.getStackFromEnd()).thenReturn(false);

        int offset = MoPubRecyclerAdapter.computeScrollOffset(mockLayoutManager, spyViewHolder);
        assertThat(offset).isEqualTo(10);
    }

    @Test
    public void computeScrollOffset_withScrollHorizontallyNoStackFromEnd_shouldComputeRightOffset() {
        when(mockAdView.getTop()).thenReturn(13);
        when(mockAdView.getBottom()).thenReturn(14);
        when(mockAdView.getLeft()).thenReturn(10);
        when(mockAdView.getRight()).thenReturn(11);

        when(mockLayoutManager.canScrollVertically()).thenReturn(false);
        when(mockLayoutManager.canScrollHorizontally()).thenReturn(true);
        when(mockLayoutManager.getStackFromEnd()).thenReturn(true);

        int offset = MoPubRecyclerAdapter.computeScrollOffset(mockLayoutManager, spyViewHolder);
        assertThat(offset).isEqualTo(11);
    }

    @Test
    public void computeScrollOffset_withCannotScroll_shouldReturnZero() {
        when(mockLayoutManager.canScrollHorizontally()).thenReturn(false);
        when(mockLayoutManager.canScrollVertically()).thenReturn(false);

        int offset = MoPubRecyclerAdapter.computeScrollOffset(mockLayoutManager, spyViewHolder);
        assertThat(offset).isEqualTo(0);
    }

    @Test
    public void registerAdRenderer_shouldCallRegisterAdRendererOnStreamAdPlacer() {
        subject.registerAdRenderer(new MoPubStaticNativeAdRenderer(new ViewBinder.Builder(1).build()));

        final ArgumentCaptor<MoPubAdRenderer> rendererCaptor = ArgumentCaptor.forClass(MoPubAdRenderer.class);
        verify(mockStreamAdPlacer).registerAdRenderer(rendererCaptor.capture());
        MoPubAdRenderer renderer = rendererCaptor.getValue();
        assertThat(renderer).isExactlyInstanceOf(MoPubStaticNativeAdRenderer.class);
    }

    @Test
    public void onCreateViewHolder_whenAdType_shouldInflateAdView() {
        when(mockStreamAdPlacer.getAdRendererForViewType(0)).thenReturn(mockAdRenderer);
        final RecyclerView.ViewHolder result = subject.onCreateViewHolder(mockParent, MoPubRecyclerAdapter.NATIVE_AD_VIEW_TYPE_BASE);

        assertThat(result).isExactlyInstanceOf(MoPubRecyclerViewHolder.class);

        verify(mockStreamAdPlacer).getAdRendererForViewType(0);
        verifyZeroInteractions(originalAdapter);
    }

    @Test
    public void onCreateViewHolder_whenNotAdType_shouldCallOriginalAdapter() {
        when(mockParent.getContext()).thenReturn(mock(Context.class));

        subject.onCreateViewHolder(mockParent, 3);

        verify(originalAdapter).onCreateViewHolder(mockParent, 3);
        verify(mockStreamAdPlacer, never()).getAdRendererForViewType(anyInt());
    }

    @Test
    public void onBindViewHolder_whenAdPosition_shouldGetAndBindAdData() {
        subject.onBindViewHolder(spyViewHolder, AD_POSITION_1);

        verify(mockStreamAdPlacer).bindAdView(mMockNativeAd, mockAdView);
    }

    @Test
    public void onBindViewHolder_whenNotAdPosition_shouldCallOriginalAdapter() {
        subject.onBindViewHolder(mockTestHolder, AD_POSITION_1 + 1);

        // Position should be adjusted.
        verify(originalAdapter).onBindViewHolder(mockTestHolder, AD_POSITION_1);
    }

    @Test
    public void onViewAttached_whenMoPubViewHolder_shouldNotCallOriginalAdapter() {
        subject.onViewAttachedToWindow(spyViewHolder);

        verify(originalAdapter, never()).onViewAttachedToWindow(any(TestHolder.class));
        verifyZeroInteractions(originalAdapter);
    }

    @Test
    public void onViewAttached_whenNotMoPubViewHolder_shouldCallOriginalAdapter() {
        subject.onViewAttachedToWindow(mockTestHolder);

        verify(originalAdapter).onViewAttachedToWindow(mockTestHolder);
    }

    @Test
    public void onViewDetached_whenMoPubViewHolder_shouldNotCallOriginalAdapter() {
        subject.onViewDetachedFromWindow(spyViewHolder);

        verifyZeroInteractions(originalAdapter);
    }

    @Test
    public void onViewDetached_whenNotMoPubViewHolder_shouldCallOriginalAdapter() {
        subject.onViewDetachedFromWindow(mockTestHolder);

        verify(originalAdapter).onViewDetachedFromWindow(mockTestHolder);
    }

    @Test
    public void onFailedToRecycleView_whenMoPubViewHolder_shouldNotCallOriginalAdapter() {
        assertThat(subject.onFailedToRecycleView(spyViewHolder)).isFalse();

        verifyZeroInteractions(originalAdapter);
    }

    @Test
    public void onFailedToRecycleView_whenNotMoPubViewHolder_shouldCallOriginalAdapter() {
        when(originalAdapter.onFailedToRecycleView(mockTestHolder)).thenReturn(true);

        assertThat(subject.onFailedToRecycleView(mockTestHolder)).isTrue();

        verify(originalAdapter).onFailedToRecycleView(mockTestHolder);
    }

    @Test
    public void onViewRecycled_whenMoPubViewHolder_shouldNotCallOriginalAdapter() {
        subject.onViewRecycled(spyViewHolder);

        verifyZeroInteractions(originalAdapter);
    }

    @Test
    public void onViewRecycled_whenNotMoPubViewHolder_shouldCallOriginalAdapter() {
        subject.onViewRecycled(mockTestHolder);

        verify(originalAdapter).onViewRecycled(mockTestHolder);
    }

    @Test
    public void handleAdLoaded_withAndWithoutAdLoadedListener_shouldNotifyInsertToListener() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setAdLoadedListener(mockAdLoadedListener);
        subject.handleAdLoaded(8);

        verify(mockObserver).onItemRangeInserted(8, 1);
        verify(mockAdLoadedListener).onAdLoaded(8);
        reset(mockObserver, mockAdLoadedListener);

        subject.setAdLoadedListener(null);
        subject.handleAdLoaded(8);

        verify(mockObserver).onItemRangeInserted(8, 1);
        verifyZeroInteractions(mockAdLoadedListener);
    }

    @Test
    public void handleAdRemoved_withAndWithoutAdLoadedListener_shouldNotifyDeleteToListener() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setAdLoadedListener(mockAdLoadedListener);
        subject.handleAdRemoved(AD_POSITION_1);

        verify(mockObserver).onItemRangeRemoved(AD_POSITION_1, 1);
        verify(mockAdLoadedListener).onAdRemoved(AD_POSITION_1);
        reset(mockObserver, mockAdLoadedListener);

        subject.setAdLoadedListener(null);
        subject.handleAdRemoved(AD_POSITION_1);

        verify(mockObserver).onItemRangeRemoved(AD_POSITION_1, 1);
        verifyZeroInteractions(mockAdLoadedListener);
    }

    @Test
    public void loadAds_shouldCallLoadOnAdPlacer() {
        subject.loadAds("testId");

        verify(mockStreamAdPlacer).loadAds("testId");
    }

    @Test
    public void loadAds_withParameters_shouldCallLoadOnAdPlacer() {
        final RequestParameters mockRequestParameters = mock(RequestParameters.class);
        subject.loadAds("testId", mockRequestParameters);

        verify(mockStreamAdPlacer).loadAds("testId", mockRequestParameters);
    }

    @Test
    public void isAd_shouldCallIsAdOnAdPlacer() {
        boolean isAd = subject.isAd(4);

        assertThat(isAd).isFalse();
        verify(mockStreamAdPlacer).isAd(4);


        isAd = subject.isAd(AD_POSITION_1);

        assertThat(isAd).isTrue();
        verify(mockStreamAdPlacer).isAd(AD_POSITION_1);
    }

    @Test
    public void getAdjustedPosition_shouldCallAdPlacer() {
        int adjustedPosition = subject.getAdjustedPosition(AD_POSITION_1);

        assertThat(adjustedPosition).isEqualTo(AD_POSITION_1 + 1);
        verify(mockStreamAdPlacer).getAdjustedPosition(AD_POSITION_1);
    }

    @Test
    public void getOriginalPosition_shouldCallAdPlacer() {
        int originalPosition = subject.getOriginalPosition(AD_POSITION_1 + 1);

        assertThat(originalPosition).isEqualTo(AD_POSITION_1);
        verify(mockStreamAdPlacer).getOriginalPosition(AD_POSITION_1 + 1);
    }

    @Test
    public void getItemCount_shouldCallAdPlacer() {
        int itemCount = subject.getItemCount();

        assertThat(itemCount).isEqualTo(20);
        verify(originalAdapter).getItemCount();
        verify(mockStreamAdPlacer).getAdjustedCount(18);
    }

    @Test
    public void setHasStableIds_shouldCallSetHasStableIdsOnOriginal() {
        subject.setHasStableIds(true);

        verify(originalAdapter).setHasStableIds(true);
    }

    @Test
    public void getItemId_hasStableIds_shouldCallOriginalAdapter() {
        subject.setHasStableIds(true);
        when(originalAdapter.getItemId(anyInt())).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(final InvocationOnMock invocation) throws Throwable {
                return Long.valueOf((Integer) invocation.getArguments()[0]);
            }
        });

        long itemId = subject.getItemId(5);

        assertThat(itemId).isEqualTo(4l);
        verify(originalAdapter).getItemId(4); // Adjusted position.
    }

    @Test
    public void getItemIds_hasStableIds_shouldReturnEnoughIds() {
        originalAdapter.setItemCount(5000);

        subject.setHasStableIds(true);

        Set<Long> ids = new HashSet<>(7000, 1.0f);
        for (int position = 0; position < subject.getItemCount(); position++) {
            ids.add(subject.getItemId(position));
        }

        assertThat(ids.size()).isEqualTo(subject.getItemCount());
        // Verify we called exactly the right # of times on the original adapter.
        verify(originalAdapter, times(5000)).getItemId(anyInt());
    }

    @Test
    public void getItemId_DoesNotHaveStableIds_shouldNotCallOriginalAdapter() {
        subject.setHasStableIds(false);

        assertThat(subject.getItemId(5)).isEqualTo(RecyclerView.NO_ID);
        verify(originalAdapter).setHasStableIds(false);
    }

    @Test
    public void onItemRangeInsertedAtEnd_withInsertAtEndStrategy_shouldNotifyDataChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(INSERT_AT_END);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeInsertedAtEnd_withMoveAdsStrategy_shouldNotifyItemRangeInserted() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount, 3);

        verify(mockObserver).onItemRangeInserted(originalItemCount + 2, 3);
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verify(mockStreamAdPlacer, times(3)).insertItem(anyInt());
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeInsertedAtEnd_withKeepAdsPlacedStrategy_shouldNotifyDataChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(KEEP_ADS_FIXED);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeInsertedInMiddle_withInsertAtEndStrategy_shouldNotifyItemRangeInserted() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(INSERT_AT_END);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount - 7, 3);

        verify(mockObserver).onItemRangeInserted(originalItemCount - 5, 3);
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount - 7);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verify(mockStreamAdPlacer, times(3)).insertItem(anyInt());
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeInsertedInMiddle_withMoveAdsStrategy_shouldNotifyItemRangeInserted() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount - 7, 3);

        verify(mockObserver).onItemRangeInserted(originalItemCount - 5, 3);
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount - 7);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verify(mockStreamAdPlacer, times(3)).insertItem(anyInt());
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeInsertedInMiddle_withKeepAdsStrategy_shouldNotifyDataChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(KEEP_ADS_FIXED);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount + 3);
        originalAdapter.notifyItemRangeInserted(originalItemCount - 7, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount - 7);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount + 3);
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeRemovedFromEnd_withInsertAtEndStrategy_shouldNotifyDataChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(INSERT_AT_END);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 4, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount - 4);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    @Test
    public void onItemRangeRemovedFromEnd_withMoveAdsStrategy_shouldNotifyItemRangeRemoved() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 4, 3);

        verify(mockObserver).onItemRangeRemoved(originalItemCount - 2, 3);
        verifyNoMoreInteractions(mockObserver);

        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verify(mockStreamAdPlacer, times(3)).removeItem(originalItemCount - 4);
    }

    @Test
    public void onItemRangeRemovedFromEnd_withMoveAdsStrategyAndItemsSurroundAnAd_shouldNotifyItemRangeRemoved() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT);
        originalAdapter.setItemCount(AD_POSITION_7 + 1);

        final int originalItemCount = AD_POSITION_7 + 1;

        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 4, 3);

        // We remove 3 items + 1 ad
        verify(mockObserver).onItemRangeRemoved(originalItemCount - 4, 4);
        verifyNoMoreInteractions(mockObserver);

        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verify(mockStreamAdPlacer, times(3)).removeItem(originalItemCount - 4);
    }

    @Test
    public void onItemRangeRemovedFromEnd_withKeepAdsStrategy_shouldNotifyDataChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(KEEP_ADS_FIXED);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 4, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);

        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
    }

    @Test
    public void onItemRangeRemovedFromMiddle_withInsertAtEndStrategy_shouldNotifyItemRangeRemoved() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(INSERT_AT_END);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 8, 3);

        verify(mockObserver).onItemRangeRemoved(originalItemCount - 6, 3);
        verifyNoMoreInteractions(mockObserver);

        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verify(mockStreamAdPlacer, times(3)).removeItem(originalItemCount - 8);
    }

    @Test
    public void onItemRangeRemovedFromMiddle_withMoveAdsStrategy_shouldNotifyItemRangeRemoved() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(MOVE_ALL_ADS_WITH_CONTENT);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 8, 3);

        verify(mockObserver).onItemRangeRemoved(originalItemCount - 6, 3);
        verifyNoMoreInteractions(mockObserver);

        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verify(mockStreamAdPlacer, times(3)).removeItem(originalItemCount - 8);
    }

    @Test
    public void onItemRangeRemovedFromMiddle_withKeepAdsStrategy_shouldNotifyChanged() {
        subject.registerAdapterDataObserver(mockObserver);
        subject.setContentChangeStrategy(KEEP_ADS_FIXED);

        final int originalItemCount = originalAdapter.getItemCount();
        originalAdapter.setItemCount(originalItemCount - 3);
        originalAdapter.notifyItemRangeRemoved(originalItemCount - 8, 3);

        verify(mockObserver).onChanged();
        verifyNoMoreInteractions(mockObserver);
        verify(mockStreamAdPlacer).getAdjustedPosition(originalItemCount - 8);
        verify(mockStreamAdPlacer).setItemCount(originalItemCount - 3);
        verifyNoMoreInteractions(mockStreamAdPlacer);
    }

    private class TestAdapter extends RecyclerView.Adapter<TestHolder> {
        private int mItems = 18;

        @Override
        public TestHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            View view = mock(View.class);
            return new TestHolder(view);
        }

        @Override
        public void onBindViewHolder(final TestHolder holder, final int position) {
            // Do nothing
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mItems;
        }

        void setItemCount(int itemCount) {
            mItems = itemCount;
        }
    }

    private class TestHolder extends RecyclerView.ViewHolder {
        public TestHolder(final View itemView) {
            super(itemView);
        }
    }
}
