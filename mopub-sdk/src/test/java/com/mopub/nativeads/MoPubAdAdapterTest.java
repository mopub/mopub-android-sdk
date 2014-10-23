package com.mopub.nativeads;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.leq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(manifest=Config.NONE)
@RunWith(SdkTestRunner.class)
public class MoPubAdAdapterTest {

    @Mock
    public BaseAdapter mockOriginalAdapter;
    @Mock
    public MoPubStreamAdPlacer mockStreamAdPlacer;
    @Mock
    public Object mockItem;
    @Mock
    public NativeAdData mockAd;
    @Mock
    public View mockAdView;
    @Mock
    public View mockItemView;
    @Mock
    public VisibilityTracker mockVisibilityTracker;
    @Mock
    public MoPubNativeAdLoadedListener mockAdLoadedListener;
    @Mock
    public DataSetObserver mockDataSetObserver;

    public MoPubAdAdapter subject;

    private static final int AD_POSITION = 4;

    @Before
    public void setup() {

        // Mock setup code.
        when(mockOriginalAdapter.getViewTypeCount()).thenReturn(1);
        when(mockOriginalAdapter.getCount()).thenReturn(30);
        when(mockOriginalAdapter.getItem(leq(29))).thenReturn(mockItem);
        when(mockOriginalAdapter.getView(anyInt(), any(View.class), any(ViewGroup.class))).thenReturn(mockItemView);

        when(mockStreamAdPlacer.getAdData(AD_POSITION)).thenReturn(mockAd);
        when(mockStreamAdPlacer.getAdView(eq(AD_POSITION), any(View.class), any(ViewGroup.class))).thenReturn(mockAdView);

        // Mock some adjustment behavior for tests.
        when(mockStreamAdPlacer.getOriginalPosition(eq(1))).thenReturn(1);
        when(mockStreamAdPlacer.getOriginalPosition(eq(8))).thenReturn(7);
        when(mockStreamAdPlacer.getAdjustedCount(eq(30))).thenReturn(31);

        subject = new MoPubAdAdapter(mockStreamAdPlacer, mockOriginalAdapter, mockVisibilityTracker);
    }

    @Test
    public void getItem_shouldCallAdPlacer() throws Exception {
        assertThat(subject.getItem(AD_POSITION)).isEqualTo(mockAd);
        verify(mockStreamAdPlacer, never()).getOriginalPosition(AD_POSITION);
        verify(mockStreamAdPlacer).getAdData(AD_POSITION);
    }

    @Test
    public void getItem_shouldCallOriginalAdapter() throws Exception {
        assertThat(subject.getItem(1)).isEqualTo(mockItem);
        verify(mockStreamAdPlacer).getOriginalPosition(1);
        verify(mockOriginalAdapter).getItem(1);

        assertThat(subject.getItem(8)).isEqualTo(mockItem);
        verify(mockStreamAdPlacer).getOriginalPosition(8);
        verify(mockOriginalAdapter).getItem(7);
    }

    @Test
    public void getCount_shouldCallAdPlacer() throws Exception {
        assertThat(subject.getCount()).isEqualTo(31);
        verify(mockStreamAdPlacer).getAdjustedCount(30);
    }

    @Test
    public void getItemIdForAd_shouldBeNegative() throws Exception {
        assertThat(subject.getItemId(AD_POSITION)).isLessThan(0);
    }
    
    @Test
    public void destroy_shouldDestroyStreamAdPlacer_shouldDestroyVisibilityTracker() {
        subject.destroy();
        verify(mockStreamAdPlacer).destroy();
        verify(mockVisibilityTracker).destroy();
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
    public void insertItem_shouldCallInsertItemOnStreamAdPlacer() throws Exception {
        subject.insertItem(5);
        verify(mockStreamAdPlacer).insertItem(5);
    }

    @Test
    public void removeItem_shouldCallRemoveItemOnStreamAdPlacer() throws Exception {
        subject.removeItem(5);
        verify(mockStreamAdPlacer).removeItem(5);
    }

    @Test
    public void getOriginalPosition_shouldCallGetOriginalPositionOnStreamAdPlacer() throws Exception {
        subject.getOriginalPosition(5);
        verify(mockStreamAdPlacer).getOriginalPosition(5);
    }
}