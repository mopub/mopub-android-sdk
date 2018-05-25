package com.mopub.nativeads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeAdViewHelperTest {
    private Activity activity;
    @Mock private View mockView;
    @Mock private ViewGroup mockViewGroup;
    @Mock private NativeAd mMockNativeAd1;
    @Mock private NativeAd mMockNativeAd2;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        when(mMockNativeAd1.createAdView(any(Activity.class), any(ViewGroup.class)))
                .thenReturn(mockView);
        when(mMockNativeAd2.createAdView(any(Activity.class), any(ViewGroup.class)))
                .thenReturn(mockView);
        when(mMockNativeAd1.isDestroyed()).thenReturn(false);
        when(mMockNativeAd2.isDestroyed()).thenReturn(false);
    }

    @Test
    public void getAdView_shouldRenderView() throws Exception {
        NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd1);

        verify(mMockNativeAd1).createAdView(activity, mockViewGroup);
        verify(mMockNativeAd1).renderAdView(mockView);
    }

    @Test
    public void getAdView_withDestroyedNativeAd_shouldReturnEmptyAndGoneConvertView() throws Exception {
        when(mMockNativeAd1.isDestroyed()).thenReturn(true);

        View view = NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd1);

        assertThat(view).isNotEqualTo(mockView);
        assertThat(view.getTag()).isEqualTo(NativeAdViewHelper.ViewType.EMPTY);
        assertThat(view.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void getAdView_shouldClearPreviousNativeAd() throws Exception {
        NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd1);

        // Second call should clear the first NativeAd
        NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd2);
        verify(mMockNativeAd1).clear(mockView);

        // Third call should clear the second NativeAd
        NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd1);
        verify(mMockNativeAd2).clear(mockView);
    }

    @Test
    public void getAdView_shouldPrepareNativeAd() throws Exception {
        NativeAdViewHelper.getAdView(mockView, mockViewGroup, activity, mMockNativeAd1);

        verify(mMockNativeAd1).prepare(mockView);
    }
}
