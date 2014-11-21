package com.mopub.nativeads;

import android.os.Handler;
import android.os.SystemClock;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

@RunWith(SdkTestRunner.class)
public class NativeAdSourceTest {
    private NativeAdSource subject;
    private ArrayList<TimestampWrapper<NativeResponse>> nativeAdCache;
    private RequestParameters requestParameters;
    private int defaultRetryTime;
    private int maxRetryTime;

    @Mock private AdSourceListener mockAdSourceListener;
    @Mock private MoPubNative mockMoPubNative;
    @Mock private NativeResponse mockNativeResponse;
    @Mock private Handler mockReplenishCacheHandler;

    @Before
    public void setUp() {
        nativeAdCache = new ArrayList<TimestampWrapper<NativeResponse>>(2);
        subject = new NativeAdSource(nativeAdCache, mockReplenishCacheHandler);
        subject.setAdSourceListener(mockAdSourceListener);

        requestParameters = new RequestParameters.Builder().build();

        defaultRetryTime = 1000;
        maxRetryTime = 5*60*1000;

        // XXX We need this to ensure that our SystemClock starts
        ShadowSystemClock.uptimeMillis();
    }

    @Test
    public void constructor_shouldInitializeCorrectly() {
        assertThat(subject.mRequestInFlight).isFalse();
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);
    }

    @Test
    public void loadAds_shouldReplenishCache() {
        subject.loadAds(requestParameters, mockMoPubNative);
        assertThat(subject.mRequestInFlight).isTrue();
        verify(mockMoPubNative).makeRequest(requestParameters, 0);
    }

    @Test
    public void loadAds_shouldClearNativeAdSource() {
        subject.setMoPubNative(mockMoPubNative);
        TimestampWrapper<NativeResponse> timestampWrapper =
                new TimestampWrapper<NativeResponse>(mock(NativeResponse.class));
        nativeAdCache.add(timestampWrapper);
        subject.mRequestInFlight = true;
        subject.mSequenceNumber = 5;
        subject.mRetryTimeMilliseconds = maxRetryTime;

        subject.loadAds(requestParameters, mockMoPubNative);

        verify(timestampWrapper.mInstance).destroy();
        assertThat(nativeAdCache).isEmpty();
        verify(mockMoPubNative).destroy();
        verify(mockReplenishCacheHandler).removeMessages(0);
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);

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
        TimestampWrapper<NativeResponse> timestampWrapper = new TimestampWrapper<NativeResponse>(mock(NativeResponse.class));
        nativeAdCache.add(timestampWrapper);
        subject.mRequestInFlight = true;
        subject.mSequenceNumber = 5;
        subject.mRetryTimeMilliseconds = maxRetryTime;

        subject.clear();

        verify(timestampWrapper.mInstance).destroy();
        assertThat(nativeAdCache).isEmpty();
        verify(mockMoPubNative).destroy();
        verify(mockReplenishCacheHandler).removeMessages(0);
        assertThat(subject.mRequestInFlight).isFalse();
        assertThat(subject.mSequenceNumber).isEqualTo(0);
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);
    }

    @Test
    public void dequeueAd_withNonStaleResponse_shouldReturnNativeResponse() {
        subject.setMoPubNative(mockMoPubNative);
        nativeAdCache.add(new TimestampWrapper<NativeResponse>(mockNativeResponse));

        assertThat(subject.dequeueAd()).isEqualTo(mockNativeResponse);
        assertThat(nativeAdCache).isEmpty();
    }

    @Test
    public void dequeueAd_withStaleResponse_shouldReturnNativeResponse() {
        subject.setMoPubNative(mockMoPubNative);

        TimestampWrapper<NativeResponse> timestampWrapper = new TimestampWrapper<NativeResponse>(
                mockNativeResponse);
        timestampWrapper.mCreatedTimestamp = SystemClock.uptimeMillis() - (15*60*1000+1);
        nativeAdCache.add(timestampWrapper);

        assertThat(subject.dequeueAd()).isNull();
        assertThat(nativeAdCache).isEmpty();
    }

    @Test
    public void dequeueAd_noRequestInFlight_shouldReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(new TimestampWrapper<NativeResponse>(mockNativeResponse));

        assertThat(subject.dequeueAd()).isEqualTo(mockNativeResponse);

        assertThat(nativeAdCache).isEmpty();
        verify(mockReplenishCacheHandler).post(any(Runnable.class));
    }

    @Test
    public void dequeueAd_requestInFlight_shouldNotReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(new TimestampWrapper<NativeResponse>(mockNativeResponse));

        subject.mRequestInFlight = true;
        assertThat(subject.dequeueAd()).isEqualTo(mockNativeResponse);

        assertThat(nativeAdCache).isEmpty();
        verify(mockReplenishCacheHandler, never()).post(any(Runnable.class));
    }

    @Test
    public void updateRetryTime_shouldUpdateRetryTimeUntilAt10Minutes() {
        int retryTime = 0;
        while (subject.mRetryTimeMilliseconds < maxRetryTime) {
            subject.updateRetryTime();
            retryTime = subject.mRetryTimeMilliseconds;
        }

        assertThat(retryTime).isEqualTo(maxRetryTime);

        // assert it won't change anymore
        subject.updateRetryTime();
        assertThat(retryTime).isEqualTo(subject.mRetryTimeMilliseconds);
    }

    @Test
    public void resetRetryTime_shouldSetRetryTimeTo1Second() {
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);

        subject.updateRetryTime();
        assertThat(subject.mRetryTimeMilliseconds).isGreaterThan(defaultRetryTime);

        subject.resetRetryTime();
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);
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
        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(nativeAdCache).hasSize(1);
        assertThat(nativeAdCache.get(0).mInstance).isEqualTo(mockNativeResponse);
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withEmptyCache_shouldCallOnAdsAvailable() {
        subject.setMoPubNative(mockMoPubNative);

        assertThat(nativeAdCache).isEmpty();
        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(nativeAdCache).hasSize(1);
        verify(mockAdSourceListener).onAdsAvailable();
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withNonEmptyCache_shouldNotCallOnAdsAvailable() {
        subject.setMoPubNative(mockMoPubNative);

        nativeAdCache.add(mock(TimestampWrapper.class));
        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(nativeAdCache).hasSize(2);
        verify(mockAdSourceListener, never()).onAdsAvailable();
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_shouldIncrementSequenceNumber_shouldResetRetryTime() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mRetryTimeMilliseconds = maxRetryTime;
        subject.mSequenceNumber = 5;

        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);
        assertThat(subject.mSequenceNumber).isEqualTo(6);
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withFullCache_shouldResetRequestInFlight() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mRequestInFlight = true;

        // fill cache
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));
        nativeAdCache.add(mock(TimestampWrapper.class));

        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
    }

    @Test
    public void moPubNativeNetworkListener_onNativeLoad_withNonFullCache_shouldReplenishCache() {
        subject.setMoPubNative(mockMoPubNative);

        subject.mRequestInFlight = true;

        subject.getMoPubNativeNetworkListener().onNativeLoad(mockNativeResponse);

        assertThat(subject.mRequestInFlight).isEqualTo(true);
        verify(mockMoPubNative).makeRequest(any(RequestParameters.class), eq(1));
    }

    @Test
    public void
    moPubNativeNetworkListener_onNativeFail_shouldResetInFlight_shouldUpdateRetryTime_shouldPostDelayedRunnable() {
        subject.mRequestInFlight = true;
        subject.mRetryTimeMilliseconds = defaultRetryTime;

        subject.getMoPubNativeNetworkListener().onNativeFail(NativeErrorCode.UNSPECIFIED);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
        assertThat(subject.mRetryInFlight).isEqualTo(true);
        assertThat(subject.mRetryTimeMilliseconds).isGreaterThan(defaultRetryTime);
        verify(mockReplenishCacheHandler).postDelayed(any(Runnable.class), eq((long)subject.mRetryTimeMilliseconds));
    }

    @Test
    public void
    moPubNativeNetworkListener_onNativeFail_maxRetryTime_shouldResetInflight_shouldResetRetryTime_shouldNotPostDelayedRunnable() {
        subject.mRequestInFlight = true;
        subject.mRetryTimeMilliseconds = maxRetryTime;

        subject.getMoPubNativeNetworkListener().onNativeFail(NativeErrorCode.UNSPECIFIED);

        assertThat(subject.mRequestInFlight).isEqualTo(false);
        assertThat(subject.mRetryInFlight).isEqualTo(false);
        assertThat(subject.mRetryTimeMilliseconds).isEqualTo(defaultRetryTime);
        verify(mockReplenishCacheHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }
}
