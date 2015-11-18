package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdRendererRegistryTest {

    private AdRendererRegistry subject;
    private NativeAd mNativeAd;
    private Context context;

    @Mock
    MoPubStaticNativeAdRenderer mockRenderer;
    @Mock
    BaseNativeAd mockNativeAd;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new AdRendererRegistry();
        mNativeAd = new NativeAd(context, "impression", "click", "adunit",
                mock(BaseNativeAd.class), mockRenderer);
        when(mockRenderer.supports(mockNativeAd)).thenReturn(true);
    }

    @Test
    public void registerAdRenderer_shouldAddRendererToRegistry() {
        subject.registerAdRenderer(mockRenderer);
        assertThat(subject.getRendererIterable()).containsOnly(mockRenderer);
    }

    @Test
    public void getRendererCount_shouldReturnCount() {
        subject.registerAdRenderer(mockRenderer);
        assertThat(subject.getAdRendererCount()).isEqualTo(1);
    }

    @Test
    public void getViewTypeForAd_() {
        subject.registerAdRenderer(mockRenderer);
        assertThat(subject.getAdRendererCount()).isEqualTo(1);
    }

    @Test
    public void getViewTypeForAd_shouldReturnIndexPlusOneOfMatchedRenderer() {
        subject.registerAdRenderer(mock(MoPubStaticNativeAdRenderer.class));
        subject.registerAdRenderer(mockRenderer);

        assertThat(subject.getViewTypeForAd(mNativeAd)).isEqualTo(2);
    }

    @Test
    public void getViewTypeForAd_withNoMatchingRednerer_shouldReturn0() {
        subject.registerAdRenderer(mock(MoPubStaticNativeAdRenderer.class));

        assertThat(subject.getViewTypeForAd(mNativeAd)).isEqualTo(0);
    }

    @Test
    public void getRendererForAd_shouldReturnRendererSupportingNativeAd() {
        subject.registerAdRenderer(mockRenderer);
        subject.registerAdRenderer(mock(MoPubStaticNativeAdRenderer.class));

        assertThat(subject.getRendererForAd(mockNativeAd)).isEqualTo(mockRenderer);
    }

    @Test
    public void getRendererForAd_withNoSupportingRenderer_shouldReturnNull() {
        subject.registerAdRenderer(mock(MoPubStaticNativeAdRenderer.class));

        assertThat(subject.getRendererForAd(mockNativeAd)).isEqualTo(null);
    }

    @Test
    public void getRendererForViewType_shouldReturnRendererSupportingNativeAd() {
    }

    @Test
    public void getRendererForViewType_withNoSupportingRenderer_shouldReturnNull() {
    }
}