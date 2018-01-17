package com.mopub.nativeads;

import android.app.Activity;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubAdAdapterTest {
    private static final int AD_POSITION = 1;

    @Mock
    private MoPubStreamAdPlacer mockStreamAdPlacer;
    @Mock
    private NativeAdData mockNativeAdData;
    @Mock
    private View mockAdView;
    @Mock
    private VisibilityTracker mockVisibilityTracker;
    @Mock
    private MoPubNativeAdLoadedListener mockAdLoadedListener;
    @Mock
    private DataSetObserver mockDataSetObserver;
    @Mock
    private RequestParameters mockRequestParameters;
    @Mock
    private ViewBinder mockViewBinder;
    @Mock
    private MoPubAdRenderer mockAdRenderer;
    @Mock
    private ListView mockListView;
    @Mock
    private OnItemClickListener mockOnItemClickListener;
    @Mock
    private OnItemLongClickListener mockOnItemLongClickListener;
    @Mock
    private OnItemSelectedListener mockOnItemSelectedListener;
    @Mock
    private View mockItemView;

    private long originalItemId = 0;
    private boolean originalHasStableIds = false;

    private int originalItemViewType = 0;
    private int originalViewTypeCount = 1;
    private boolean originalItemsAreEnabled = false;
    private ArrayAdapter<String> originalAdapter;
    private MoPubAdAdapter subject;

    @Before
    public void setup() {
        // Set up original adapter with 2 items
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        originalAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1) {
            @Override
            public boolean isEnabled(final int position) {
                return originalItemsAreEnabled;
            }

            @Override
            public long getItemId(final int position) {
                return originalItemId;
            }

            @Override
            public boolean hasStableIds() {
                return originalHasStableIds;
            }

            @Override
            public int getItemViewType(final int position) {
                return originalItemViewType;
            }

            @Override
            public int getViewTypeCount() {
                return originalViewTypeCount;
            }
        };
        originalAdapter.add("ITEM 1");
        originalAdapter.add("ITEM 2");

        subject = new MoPubAdAdapter(mockStreamAdPlacer, originalAdapter, mockVisibilityTracker);

        // Reset because the constructor interacts with the stream ad placer, and we don't want
        // to worry about verifying those changes in tests.
        reset(mockStreamAdPlacer);

        // Mock some simple adjustment behavior for tests. This is creating an ad placer that
        // emulates a content item followed by an ad item, then another content item.
        when(mockStreamAdPlacer.getAdData(AD_POSITION)).thenReturn(mockNativeAdData);
        when(mockStreamAdPlacer.getAdView(eq(AD_POSITION), any(View.class), any(ViewGroup.class))).thenReturn(mockAdView);
        when(mockStreamAdPlacer.isAd(anyInt())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(final InvocationOnMock invocation) throws Throwable {
                int position = (Integer)invocation.getArguments()[0];
                return position == AD_POSITION;
            }
        });
        when(mockStreamAdPlacer.getOriginalPosition(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                int originalPosition = (Integer)invocation.getArguments()[0];
                return originalPosition < AD_POSITION ? originalPosition : originalPosition - 1;
            }
        });
        when(mockStreamAdPlacer.getAdViewType(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                int originalPosition = (Integer)invocation.getArguments()[0];
                return originalPosition == AD_POSITION ? 1 : MoPubStreamAdPlacer.CONTENT_VIEW_TYPE;
            }
        });
        when(mockStreamAdPlacer.getAdjustedPosition(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                int originalPosition = (Integer)invocation.getArguments()[0];
                return originalPosition < AD_POSITION ? originalPosition : originalPosition + 1;
            }
        });
        when(mockStreamAdPlacer.getAdjustedCount(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable {
                int originalCount = (Integer)invocation.getArguments()[0];
                return originalCount < AD_POSITION ? originalCount : originalCount + 1;
            }
        });
    }

    @Test
    public void originalAdapterChange_shouldNotifyDataSetChanged() {
        subject.registerDataSetObserver(mockDataSetObserver);

        originalAdapter.notifyDataSetChanged();

        verify(mockDataSetObserver).onChanged();
    }

    @Test
    public void originalAdapterInvalidated_shouldNotifyDataSetInvalidated() {
        subject.registerDataSetObserver(mockDataSetObserver);

        originalAdapter.notifyDataSetInvalidated();

        verify(mockDataSetObserver).onInvalidated();
    }

    @Test
    public void registerAdRenderer_shouldCallRegisterAdRendererOnAdPlacer() {
        subject.registerAdRenderer(mockAdRenderer);

        verify(mockStreamAdPlacer).registerAdRenderer(mockAdRenderer);
    }

    @Test
    public void registerAdRenderer_withNull_shouldNotCallAdPlacer() {
        subject.registerAdRenderer(null);

        verify(mockStreamAdPlacer, never()).registerAdRenderer(any(MoPubAdRenderer.class));
    }

    @Test
    public void setAdLoadedListener_handleAdLoaded_shouldCallCallback_shouldCallObserver() {
        subject.setAdLoadedListener(mockAdLoadedListener);
        subject.registerDataSetObserver(mockDataSetObserver);

        subject.handleAdLoaded(8);

        verify(mockAdLoadedListener).onAdLoaded(8);
        verify(mockDataSetObserver).onChanged();
    }

    @Test
    public void setAdLoadedListener_handleAdRemoved_shouldCallCallback_shouldCallObserver() {
        subject.setAdLoadedListener(mockAdLoadedListener);
        subject.registerDataSetObserver(mockDataSetObserver);

        subject.handleAdRemoved(10);

        verify(mockAdLoadedListener).onAdRemoved(10);
        verify(mockDataSetObserver).onChanged();
    }

    @Test
    public void loadAds_shouldCallLoadAdsOnAdPlacer() {
        subject.loadAds("AD_UNIT_ID");

        verify(mockStreamAdPlacer).loadAds("AD_UNIT_ID");

        subject.loadAds("AD_UNIT_ID", mockRequestParameters);

        verify(mockStreamAdPlacer).loadAds("AD_UNIT_ID", mockRequestParameters);
    }

    @Test
    public void isAd_shouldCallIsAdOnAdPlacer() {
        boolean isAd = subject.isAd(AD_POSITION);

        assertThat(isAd).isTrue();

        isAd = subject.isAd(AD_POSITION + 1);

        assertThat(isAd).isFalse();

        verify(mockStreamAdPlacer, times(2)).isAd(anyInt());
    }

    @Test
    public void clearAds_shouldCallClearAdsOnAdPlacer() {
        subject.clearAds();

        verify(mockStreamAdPlacer).clearAds();
    }

    @Test
    public void destroy_shouldDestroyStreamAdPlacer_shouldDestroyVisibilityTracker() {
        subject.destroy();

        verify(mockStreamAdPlacer).destroy();
        verify(mockVisibilityTracker).destroy();
    }

    @Test
    public void isEnabled_adPosition_shouldReturnTrue() {
        boolean isEnabled = subject.isEnabled(AD_POSITION);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isEnabled_withNonAdPosition_shouldUseOriginalAdapter() {
        originalItemsAreEnabled = false;
        boolean isEnabled = subject.isEnabled(AD_POSITION + 1);

        assertThat(isEnabled).isFalse();

        originalItemsAreEnabled = true;
        isEnabled = subject.isEnabled(AD_POSITION + 1);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void getItem_withAdPosition_shouldReturnAd_shouldGetAdDataOnCallAdPlacer() {
        Object actualItem = subject.getItem(AD_POSITION);

        assertThat(actualItem).isEqualTo(mockNativeAdData);

        verify(mockStreamAdPlacer).getAdData(AD_POSITION);
    }

    @Test
    public void getItem_withNonAdPosition_shouldCallGetOriginalPositionOnAdPlacer() {
        Object actualItem = subject.getItem(AD_POSITION + 1);

        assertThat(actualItem).isNotEqualTo(mockNativeAdData);

        verify(mockStreamAdPlacer).getOriginalPosition(AD_POSITION + 1);
    }

    @Test
    public void getCount_shouldCallGetAdjustedCountOnAdPlacer() {
        int actualCount = subject.getCount();

        assertThat(actualCount).isEqualTo(3);

        verify(mockStreamAdPlacer).getAdjustedCount(anyInt());
    }

    @Test
    public void getItemId_withAdPosition_shouldBeNegative() {
        long itemId = subject.getItemId(AD_POSITION);

        assertThat(itemId).isLessThan(0);
    }

    @Test
    public void getItemId_withNonAdPosition_shouldUseOriginalAdapterId() {
        originalItemId = 42;
        long itemId = subject.getItemId(AD_POSITION + 1);

        assertThat(itemId).isEqualTo(42);
    }

    @Test
    public void hasStableIds_shouldUseOriginalAdapterValue() {
        originalHasStableIds = false;
        boolean hasStableIds = subject.hasStableIds();

        assertThat(hasStableIds).isFalse();

        originalHasStableIds = true;
        hasStableIds = subject.hasStableIds();

        assertThat(hasStableIds).isTrue();
    }

    @Test
    public void getView_withAdPosition_shouldReturnAdView_shouldTrackVisibility() {
        View view = subject.getView(AD_POSITION, null, null);

        assertThat(view).isEqualTo(mockAdView);

        verify(mockVisibilityTracker).addView(eq(mockAdView), anyInt(),
                Matchers.isNull(Integer.class));
    }

    @Test
    public void getView_withNonAdPosition_shouldOriginalAdapterView_shouldTrackVisibility() {
        View view = subject.getView(AD_POSITION + 1, null, null);

        assertThat(view).isNotEqualTo(mockAdView);

        verify(mockVisibilityTracker).addView(any(View.class), anyInt(),
                Matchers.isNull(Integer.class));
    }

    @Test
    public void getItemViewType_withAdPosition_shouldReturnOneGreaterThanViewType() {
        originalItemViewType = 0;

        int itemViewType = subject.getItemViewType(AD_POSITION);
        assertThat(itemViewType).isEqualTo(originalItemViewType + 1);
    }

    @Test
    public void getItemViewType_withNonAdPosition_shouldUseOriginalAdapterId() {
        originalItemViewType = 0;

        int itemViewType = subject.getItemViewType(AD_POSITION + 1);
        assertThat(itemViewType).isEqualTo(originalItemViewType);
    }

    @Test
    public void getViewTypeCount_shouldReturnOriginalViewTypeCountPlusOne() {
        originalViewTypeCount = 1;

        int viewTypeCount = subject.getViewTypeCount();
        assertThat(viewTypeCount).isEqualTo(1);

        originalViewTypeCount = 2;

        viewTypeCount = subject.getViewTypeCount();
        assertThat(viewTypeCount).isEqualTo(2);
    }

    @Test
    public void isEmpty_shouldUseOriginalAdapterValue() {
        boolean isEmpty = subject.isEmpty();

        assertThat(isEmpty).isFalse();

        originalAdapter.clear();

        isEmpty = subject.isEmpty();

        assertThat(isEmpty).isTrue();
    }

    @Test
    public void getOriginalPosition_shouldCallStreamAdPlacer() {
        subject.getOriginalPosition(5);

        verify(mockStreamAdPlacer).getOriginalPosition(5);
    }

    @Test
    public void getAdjustedPosition_shouldCallStreamAdPlacer() {
        subject.getAdjustedPosition(5);

        verify(mockStreamAdPlacer).getAdjustedPosition(5);
    }

    @Test
    public void insertItem_shouldCallInsertItemOnStreamAdPlacer() {
        subject.insertItem(5);

        verify(mockStreamAdPlacer).insertItem(5);
    }

    @Test
    public void removeItem_shouldCallRemoveItemOnStreamAdPlacer() {
        subject.removeItem(5);

        verify(mockStreamAdPlacer).removeItem(5);
    }

    @Test
    public void setOnItemClickListener_withAdPosition_shouldNotCallListener() {
        subject.setOnClickListener(mockListView, mockOnItemClickListener);

        ArgumentCaptor<OnItemClickListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemClickListener.class);
        verify(mockListView).setOnItemClickListener(listenerCaptor.capture());

        OnItemClickListener listener = listenerCaptor.getValue();
        listener.onItemClick(mockListView, mockItemView, AD_POSITION, 0);

        verify(mockOnItemClickListener, never()).onItemClick(
                any(AdapterView.class), any(View.class), anyInt(), anyInt());
    }

    @Test
    public void setOnItemClickListener_withNonAdPosition_shouldCallListener() {
        subject.setOnClickListener(mockListView, mockOnItemClickListener);

        ArgumentCaptor<OnItemClickListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemClickListener.class);
        verify(mockListView).setOnItemClickListener(listenerCaptor.capture());

        OnItemClickListener listener = listenerCaptor.getValue();
        listener.onItemClick(mockListView, mockItemView, AD_POSITION + 1, 0);

        verify(mockOnItemClickListener).onItemClick(
                mockListView, mockItemView, AD_POSITION, 0);
    }

    @Test
    public void setOnItemLongClickListener_withAdPosition_shouldNotCallListener() {
        subject.setOnItemLongClickListener(mockListView, mockOnItemLongClickListener);

        ArgumentCaptor<OnItemLongClickListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemLongClickListener.class);
        verify(mockListView).setOnItemLongClickListener(listenerCaptor.capture());

        OnItemLongClickListener listener = listenerCaptor.getValue();
        listener.onItemLongClick(mockListView, mockItemView, AD_POSITION, 0);

        verify(mockOnItemLongClickListener, never()).onItemLongClick(
                any(AdapterView.class), any(View.class), anyInt(), anyInt());
    }

    @Test
    public void setOnItemLongClickListener_withNonAdPosition_shouldCallListener() {
        subject.setOnItemLongClickListener(mockListView, mockOnItemLongClickListener);

        ArgumentCaptor<OnItemLongClickListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemLongClickListener.class);
        verify(mockListView).setOnItemLongClickListener(listenerCaptor.capture());

        OnItemLongClickListener listener = listenerCaptor.getValue();
        listener.onItemLongClick(mockListView, mockItemView, AD_POSITION + 1, 0);

        verify(mockOnItemLongClickListener).onItemLongClick(
                mockListView, mockItemView, AD_POSITION, 0);
    }

    @Test
    public void setOnItemSelectedListener_withAdPosition_shouldNotCallListener() {
        subject.setOnItemSelectedListener(mockListView, mockOnItemSelectedListener);

        ArgumentCaptor<OnItemSelectedListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemSelectedListener.class);
        verify(mockListView).setOnItemSelectedListener(listenerCaptor.capture());

        OnItemSelectedListener listener = listenerCaptor.getValue();
        listener.onItemSelected(mockListView, mockItemView, AD_POSITION, 0);

        verify(mockOnItemSelectedListener, never()).onItemSelected(
                any(AdapterView.class), any(View.class), anyInt(), anyInt());
    }

    @Test
    public void setOnItemSelectedListener_withNonAdPosition_shouldCallListener() {
        subject.setOnItemSelectedListener(mockListView, mockOnItemSelectedListener);

        ArgumentCaptor<OnItemSelectedListener> listenerCaptor =
                ArgumentCaptor.forClass(OnItemSelectedListener.class);
        verify(mockListView).setOnItemSelectedListener(listenerCaptor.capture());

        OnItemSelectedListener listener = listenerCaptor.getValue();
        listener.onItemSelected(mockListView, mockItemView, AD_POSITION + 1, 0);

        verify(mockOnItemSelectedListener).onItemSelected(
                mockListView, mockItemView, AD_POSITION, 0);
    }

    @Test
    public void setSelection_shouldCallSetSelectionOnListView() {
        subject.setSelection(mockListView, AD_POSITION);

        // Since the original position is the ad position, the adjusted position is 1 higher
        verify(mockListView).setSelection(AD_POSITION + 1);
    }

    @Test
    public void smoothScrollToPosition_shouldCallSmoothScrollToPositionOnListView() {
        subject.smoothScrollToPosition(mockListView, AD_POSITION);

        // Since the original position is the ad position, the adjusted position is 1 higher
        verify(mockListView).smoothScrollToPosition(AD_POSITION + 1);
    }

    @Test
    public void refreshAds_shouldLoadAdsOnAdPlacer() {
        when(mockListView.getAdapter()).thenReturn(subject);

        subject.refreshAds(mockListView, "AD_UNIT_ID", mockRequestParameters);

        verify(mockStreamAdPlacer).loadAds("AD_UNIT_ID", mockRequestParameters);
    }
}
