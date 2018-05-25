package com.mopub.nativeads;

import android.os.Handler;
import android.os.SystemClock;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.ArrayList;

import static com.mopub.nativeads.NativeAdSource.AdSourceListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeAdSourceTest {
    private NativeAdSource subject;
    private ArrayList<TimestampWrapper<NativeAd>> nativeAdCache;
    private RequestParameters requestParameters;
    private int defaultRetryTime;
    private int maxRetryTime;
    private int maxRetries;

    @Mock private AdSourceListener mockAdSourceListener;
    @Mock private MoPubNative mockMoPubNative;
    @Mock private NativeAd mMockNativeAd;
    @Mock private Handler mockReplenishCacheHandler;
    @Mock private AdRendererRegistry mockAdRendererRegistry;
    @Mock private MoPubStaticNativeAdRenderer mockRenderer;

    @Before
    public void setUp() {
        nativeAdCache = new ArrayList<TimestampWrapper<NativeAd>>(2);
        subject = new NativeAdSource(nativeAdCache, mockReplenishCacheHandler, mockAdRendererRegistry);
        subject.setAdSourceListener(mockAdSourceListener);

        requestParameters = new RequestParameters.Builder().build();

        defaultRetryTime = 1000;
        maxRetryTime = 5*60*1000;
        maxRetries = 5;

        // XXX We need this to ensure that our SystemClock starts
        ShadowSystemClock.uptimeMillis();

        ArrayList<MoPubAdRenderer> moPubAdRenderers = new ArrayList<MoPubAdRenderer>();
        moPubAdRenderers.add(mockRenderer);
        when(mockAdRendererRegistry.getRendererIterable()).thenReturn(moPubAdRenderers);
    }

    @Test
    public void constructor_shouldInitializeCorrectly() {
        assertThat(subject.mRequestInFlight).isFalse();
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);
    }

    @Test
    public void getAdRendererCount_shouldCallAdRendererRegistryGetAdRendererCount() throws Exception {
        when(mockAdRendererRegistry.getAdRendererCount()).thenReturn(123);

        assertThat(subject.getAdRendererCount()).isEqualTo(123);

        verify(mockAdRendererRegistry).getAdRendererCount();
    }

    @Test
    public void getViewTypeForAd_shouldCallAdRendererRegistryGetViewTypeForAd() throws Exception {
        NativeAd nativeAd = mock(NativeAd.class);
        when(mockAdRendererRegistry.getViewTypeForAd(nativeAd)).thenReturn(123);

        assertThat(subject.getViewTypeForAd(nativeAd)).isEqualTo(123);

        verify(mockAdRendererRegistry).getViewTypeForAd(nativeAd);
    }

    @Test
    public void registerAdRenderer_shouldRegisterAdRendererWithRegistryAndMoPubNative() throws Exception {
        subject.setMoPubNative(mockMoPubNative);
        subject.registerAdRenderer(mockRenderer);

        verify(mockAdRendererRegistry).registerAdRenderer(mockRenderer);
        verify(mockMoPubNative).registerAdRenderer(mockRenderer);
    }

    @Test
    public void getAdRendererForViewType_shouldCallAdRendererRegistryGetRendererForViewType() throws Exception {
        when(mockAdRendererRegistry.getRendererForViewType(123)).thenReturn(mockRenderer);

        assertThat(subject.getAdRendererForViewType(123)).isEqualTo(mockRenderer);

        verify(mockAdRendererRegistry).getRendererForViewType(123);
    }

    @Test
    public void loadAds_shouldReplenishCache() {
        subject.loadAds(requestParameters, mockMoPubNative);
        assertThat(subject.mRequestInFlight).isTrue();
        verify(mockMoPubNative).makeRequest(requestParameters, 0);
    }

    @Test
    public void loadAds_shouldReregisterAdRenderersWithNewMoPubNative() throws Exception {
        subject.loadAds(mock(RequestParameters.class), mockMoPubNative);

        verify(mockMoPubNative).registerAdRenderer(mockRenderer);
    }

    @Test
    public void loadAds_shouldClearNativeAdSource() {
        subject.setMoPubNative(mockMoPubNative);
        TimestampWrapper<NativeAd> timestampWrapper =
                new TimestampWrapper<NativeAd>(mock(NativeAd.class));
        nativeAdCache.add(timestampWrapper);
        subject.mRequestInFlight = true;
        subject.mSequenceNumber = 5;
        subject.mCurrentRetries = maxRetries;

        subject.loadAds(requestParameters, mockMoPubNative);

        verify(timestampWrapper.mInstance).destroy();
        assertThat(nativeAdCache).isEmpty();
        verify(mockMoPubNative).destroy();
        verify(mockReplenishCacheHandler).removeMessages(0);
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.mCurrentRetries).isEqualTo(0);
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);

        // new request has been kicked off
        assertThat(subject.mRequestInFlight).isTrue();
    }

    @Test
    public void loadAds_shouldDestroyPreviousMoPubNativeInstance() {
        subject.loadAds(requestParameters, mockMoPubNative);
        verify(mockMoPubNative, never()).destroy();

        subject.loadAds(requestParameters, mockMoPubNative);
        verify(mockMoPubNative).destroy();
    }

    @Test
    public void clear_shouldDestroyMoPubNative_shouldClearNativeAdCache_shouldRemovePollHandlerMessages_shouldResetSequenceNumber_shouldResetRequestInFlight_shouldResetRetryTime() {
        subject.setMoPubNative(mockMoPubNative);
        TimestampWrapper<NativeAd> timestampWrapper = new TimestampWrapper<NativeAd>(mock(NativeAd.class));
        nativeAdCache.add(timestampWrapper);
        subject.mRequestInFlight = true;
        subject.mSequenceNumber = 5;
        subject.mCurrentRetries = maxRetries;

        subject.clear();

        verify(timestampWrapper.mInstance).destroy();
        assertThat(nativeAdCache).isEmpty();
        verify(mockMoPubNative).destroy();
        verify(mockReplenishCacheHandler).removeMessages(0);
        assertThat(subject.mRequestInFlight).isFalse();
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);
    }

    @Test
    public void dequeueAd_withNonStaleAd_shouldReturnNativeAd() {
        subject.setMoPubNative(mockMoPubNative);
        nativeAdCache.add(new TimestampWrapper<NativeAd>(mMockNativeAd));

        assertThat(subject.dequeueAd()).isEqualTo(mMockNativeAd);
        assertThat(nativeAdCache).isEmpty();
    }

    @Test
    public void dequeueAd_withStaleAd_shouldReturnNativeAd() {
        subject.setMoPubNative(mockMoPubNative);

        TimestampWrapper<NativeAd> timestampWrapper = new TimestampWrapper<NativeAd>(
                mMockNativeAd);
        timestampWrapper.mCreatedTimestamp = SystemClock.uptimeMillis() - (4*60*60*1000+1);
        nativeAdCache.add(timestampWrapper);

        assertThat(subject.dequeueAd()).isNull();
        assertThat(nativeAdCache).isEmpty();
    }

    @Test
    public void dequeueAd_noRequestInFlight_shouldReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(new TimestampWrapper<NativeAd>(mMockNativeAd));

        assertThat(subject.dequeueAd()).isEqualTo(mMockNativeAd);

        assertThat(nativeAdCache).isEmpty();
        verify(mockReplenishCacheHandler).post(any(Runnable.class));
    }

    @Test
    public void dequeueAd_requestInFlight_shouldNotReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(new TimestampWrapper<NativeAd>(mMockNativeAd));

        subject.mRequestInFlight = true;
        assertThat(subject.dequeueAd()).isEqualTo(mMockNativeAd);

        assertThat(nativeAdCache).isEmpty();
        verify(mockReplenishCacheHandler, never()).post(any(Runnable.class));
    }

    @Test
    public void updateRetryTime_shouldUpdateRetryTimeUntilAt10Minutes() {
        int retryTime = 0;
        while (subject.mCurrentRetries < maxRetries) {
            subject.updateRetryTime();
            retryTime = subject.getRetryTime();
        }

        assertThat(retryTime).isEqualTo(maxRetryTime);

        // assert it won't change anymore
        subject.updateRetryTime();
        assertThat(retryTime).isEqualTo(subject.getRetryTime());
    }

    @Test
    public void resetRetryTime_shouldSetRetryTimeTo1Second() {
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);

        subject.updateRetryTime();
        assertThat(subject.getRetryTime()).isGreaterThan(defaultRetryTime);

        subject.resetRetryTime();
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);
    }

    @Test
    public void replenishCache_shouldLoadNativeAd_shouldMarkRequestInFlight() {
        subject.setMoPubNative(mockMoPubNative);

        subject.replenishCache();

        verify(mockMoPubNative).makeRequest(any(RequestParameters.class), eq(0));
        assertThat(subject.mRequestInFlight).isTrue();
    }

    @Test
    public void replenishCache_withRequestInFlight_shouldNotLoadNativeAd() {
        subject.mRequestInFlight = true;
        subject.setMoPubNative(mockMoPubNative);

        subject.replenishCache();

        verify(mockMoPubNative, never()).makeRequest(requestParameters, 0);
        assertThat(subject.mRequestInFlight).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void replenishCache_withCacheSizeAtLimit_shouldNotLoadNativeAd() {
        // Default cache size may change in the future and this test will have to be updated
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));

        subject.setMoPubNative(mockMoPubNative);

        subject.replenishCache();

        verify(mockMoPubNative, never()).makeRequest(any(RequestParameters.class), any(Integer.class));
        assertThat(subject.mRequestInFlight).isFalse();
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_shouldAddToCache() {
        subject.setMoPubNative(mockMoPubNative);
        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(nativeAdCache).hasSize(1);
        assertThat(nativeAdCache.get(0).mInstance).isEqualTo(mMockNativeAd);
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withEmptyCache_shouldCallOnAdsAvailable() {
        subject.setMoPubNative(mockMoPubNative);

        assertThat(nativeAdCache).isEmpty();
        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(nativeAdCache).hasSize(1);
        verify(mockAdSourceListener).onAdsAvailable();
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withNonEmptyCache_shouldNotCallOnAdsAvailable() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(mock(TimestampWrapper.class));
        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(nativeAdCache).hasSize(2);
        verify(mockAdSourceListener, never()).onAdsAvailable();
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_shouldIncrementSequenceNumber_shouldResetRetryTime() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mCurrentRetries = maxRetries;
        subject.mSequenceNumber = 5;

        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);
        assertThat(subject.mSequenceNumber).isEqualTo(6);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withFullCache_shouldResetRequestInFlight() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mRequestInFlight = true;

        // fill cache
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));

        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withCacheFilled_shouldNotReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mRequestInFlight = true;

        subject.getMoPubNativeNetworkListener().onNativeLoad(mMockNativeAd);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
    }

    @Test
    public void
    moPubNativeNetworkListener_onNativeFail_shouldResetInFlight_shouldUpdateRetryTime_shouldPostDelayedRunnable() {
        subject.mRequestInFlight = true;

        subject.getMoPubNativeNetworkListener().onNativeFail(NativeErrorCode.UNSPECIFIED);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
        assertThat(subject.mRetryInFlight).isEqualTo(true);
        assertThat(subject.getRetryTime()).isGreaterThan(defaultRetryTime);
        verify(mockReplenishCacheHandler).postDelayed(any(Runnable.class), eq((long)subject.getRetryTime()));
    }

    @Test
    public void
    moPubNativeNetworkListener_onNativeFail_maxRetryTime_shouldResetInflight_shouldResetRetryTime_shouldNotPostDelayedRunnable() {
        subject.mRequestInFlight = true;
        subject.mCurrentRetries = maxRetries;

        subject.getMoPubNativeNetworkListener().onNativeFail(NativeErrorCode.UNSPECIFIED);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
        assertThat(subject.mRetryInFlight).isEqualTo(false);
        assertThat(subject.getRetryTime()).isEqualTo(defaultRetryTime);
        verify(mockReplenishCacheHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }
}
