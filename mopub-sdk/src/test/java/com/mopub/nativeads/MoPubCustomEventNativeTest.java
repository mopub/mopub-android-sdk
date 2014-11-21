package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubCustomEventNativeTest {

    private MoPubCustomEventNative subject;
    private Activity context;
    private HashMap<String, Object> localExtras;
    private CustomEventNativeListener mCustomEventNativeListener;
    private HashMap<String, String> serverExtras;
    private JSONObject fakeJsonObject;

    @Before
    public void setUp() throws Exception {
        subject = new MoPubCustomEventNative();
        context = new Activity();

        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();

        fakeJsonObject = new JSONObject();
        fakeJsonObject.put("imptracker", new JSONArray("[\"url1\", \"url2\"]"));
        fakeJsonObject.put("clktracker", "expected clicktracker");
        fakeJsonObject.put("mainimage", "mainimageurl");
        fakeJsonObject.put("iconimage", "iconimageurl");
        fakeJsonObject.put("extraimage", "extraimageurl");

        serverExtras.put(CustomEventNativeAdapter.RESPONSE_BODY_KEY, fakeJsonObject.toString());

        mCustomEventNativeListener = mock(CustomEventNativeListener.class);
    }

    @After
    public void tearDown() throws Exception {
        reset(mCustomEventNativeListener);
    }

    @Test
    public void loadNativeAd_withInvalidResponseBody_shouldNotifyListenerOfOnNativeAdFailed() throws Exception {
        serverExtras.put(CustomEventNativeAdapter.RESPONSE_BODY_KEY, "{ \"bad json");

        subject.loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(MoPubCustomEventNative.MoPubForwardingNativeAd.class));
        verify(mCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.INVALID_JSON);
    }

    @Test
    public void loadNativeAd_withNullResponseBody_shouldNotifyListenerOfOnNativeAdFailed() throws Exception {
        serverExtras.put(CustomEventNativeAdapter.RESPONSE_BODY_KEY, null);

        subject.loadNativeAd(context, mCustomEventNativeListener, localExtras, serverExtras);
        verify(mCustomEventNativeListener, never()).onNativeAdLoaded(any(MoPubCustomEventNative.MoPubForwardingNativeAd.class));
        verify(mCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
    }
}
