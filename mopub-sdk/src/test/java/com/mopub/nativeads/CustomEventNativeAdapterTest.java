package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.DownloadResponse;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.nativeads.test.support.TestCustomEventNativeFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class CustomEventNativeAdapterTest {

    private Activity context;
    private DownloadResponse downloadResponse;
    private HashMap<String, Object> localExtras;
    private CustomEventNative.CustomEventNativeListener mCustomEventNativeListener;
    private CustomEventNative mCustomEventNative;
    private HashMap<String, String> serverExtras;
    private TestHttpResponseWithHeaders testHttpResponseWithHeaders;

    @Before
    public void setUp() throws Exception {
        context = new Activity();

        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put("key", "value");
        serverExtras.put(CustomEventNativeAdapter.RESPONSE_BODY_KEY, "body");

        testHttpResponseWithHeaders = new TestHttpResponseWithHeaders(200, "body");
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{ \"key\" : \"value\" }");
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.nativeads.MoPubCustomEventNative");
        downloadResponse = new DownloadResponse(testHttpResponseWithHeaders);

        mCustomEventNativeListener = mock(CustomEventNative.CustomEventNativeListener.class);

        mCustomEventNative = TestCustomEventNativeFactory.getSingletonMock();
    }

    @Test
    public void loadNativeAd_withValidInput_shouldCallLoadNativeAdOnTheCustomEvent() throws Exception {
        CustomEventNativeAdapter.loadNativeAd(context, localExtras, downloadResponse, mCustomEventNativeListener);
        verify(mCustomEventNative).loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(NativeAdInterface.class));
    }

    @Test
    public void loadNativeAd_withInvalidClassName_shouldNotifyListenerOfOnNativeAdFailedAndReturn() throws Exception {
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.mopub.baaad.invalidinvalid123143");
        downloadResponse = new DownloadResponse(testHttpResponseWithHeaders);

        CustomEventNativeAdapter.loadNativeAd(context, localExtras, downloadResponse, mCustomEventNativeListener);
        verify(mCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(NativeAdInterface.class));
        verify(mCustomEventNative, never()).loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
    }

    @Test
    public void loadNativeAd_withInvalidCustomEventNativeData_shouldNotAddToServerExtras() throws Exception {
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CUSTOM_EVENT_DATA.getKey(), "{ \"bad json");
        downloadResponse = new DownloadResponse(testHttpResponseWithHeaders);
        serverExtras.remove("key");

        CustomEventNativeAdapter.loadNativeAd(context, localExtras, downloadResponse, mCustomEventNativeListener);
        verify(mCustomEventNative).loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
        verify(mCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(NativeAdInterface.class));
    }
}
