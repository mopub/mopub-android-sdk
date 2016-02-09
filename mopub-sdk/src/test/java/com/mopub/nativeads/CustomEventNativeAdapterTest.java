package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.test.support.TestCustomEventNativeFactory;
import com.mopub.network.AdResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CustomEventNativeAdapterTest {

    private Activity context;
    private HashMap<String, Object> localExtras;
    private CustomEventNative.CustomEventNativeListener mCustomEventNativeListener;
    private CustomEventNative mCustomEventNative;
    private HashMap<String, String> serverExtras;
    private AdResponse testAdResponse;

    @Before
    public void setUp() throws Exception {
        context = new Activity();

        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put("key", "value");

        testAdResponse = new AdResponse.Builder()
                .setAdType(AdType.STATIC_NATIVE)
                .setCustomEventClassName("com.mopub.nativeads.MoPubCustomEventNative")
                .setClickTrackingUrl("clicktrackingurl")
                .setResponseBody("body")
                .setServerExtras(serverExtras)
                .build();

        mCustomEventNativeListener = mock(CustomEventNative.CustomEventNativeListener.class);

        mCustomEventNative = TestCustomEventNativeFactory.getSingletonMock();
    }

    @Test
    public void loadNativeAd_withValidInput_shouldCallLoadNativeAdOnTheCustomEvent() throws Exception {
        Map<String, Object> expectedLocalExtras = new HashMap<String, Object>();
        expectedLocalExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, "clicktrackingurl");
        CustomEventNativeAdapter.loadNativeAd(context, localExtras, testAdResponse, mCustomEventNativeListener);
        verify(mCustomEventNative).loadNativeAd(context, mCustomEventNativeListener, expectedLocalExtras, serverExtras);
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withInvalidClassName_shouldNotifyListenerOfOnNativeAdFailedAndReturn() throws Exception {
        testAdResponse = testAdResponse.toBuilder()
                .setCustomEventClassName("com.mopub.baaad.invalidinvalid123143")
                .build();

        CustomEventNativeAdapter.loadNativeAd(context, localExtras, testAdResponse, mCustomEventNativeListener);
        verify(mCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
        verify(mCustomEventNative, never()).loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
    }

    @Test
    public void loadNativeAd_withInvalidCustomEventNativeData_shouldNotAddToServerExtras() throws Exception {
        testAdResponse = testAdResponse.toBuilder()
                .setServerExtras(null)
                .build();

        CustomEventNativeAdapter.loadNativeAd(context, localExtras, testAdResponse, mCustomEventNativeListener);
        verify(mCustomEventNative).loadNativeAd(eq(context), eq(mCustomEventNativeListener), eq(localExtras), eq(new HashMap<String, String>()));
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }
}
