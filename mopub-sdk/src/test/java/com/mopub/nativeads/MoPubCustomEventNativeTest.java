package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.MoPubCustomEventNative.MoPubStaticNativeAd;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubCustomEventNativeTest {

    private MoPubCustomEventNative subject;
    private Activity context;
    private HashMap<String, Object> localExtras;
    private HashMap<String, String> serverExtras;
    private JSONObject fakeJsonObject;

    @Mock private CustomEventNativeListener mockCustomEventNativeListener;

    @Before
    public void setUp() throws Exception {
        subject = new MoPubCustomEventNative();
        context = Robolectric.buildActivity(Activity.class).create().get();

        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();

        fakeJsonObject = new JSONObject();
        fakeJsonObject.put("imptracker", new JSONArray("[\"url1\", \"url2\"]"));
        fakeJsonObject.put("clktracker", "expected clicktracker");
        fakeJsonObject.put("mainimage", "mainimageurl");
        fakeJsonObject.put("iconimage", "iconimageurl");
        fakeJsonObject.put("extraimage", "extraimageurl");

        localExtras.put(DataKeys.JSON_BODY_KEY, fakeJsonObject);
    }

    @Test
    public void loadNativeAd_withNullResponseBody_shouldNotifyListenerOfOnNativeAdFailed() {
        localExtras.remove(DataKeys.JSON_BODY_KEY);

        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener, never())
                .onNativeAdLoaded(any(MoPubStaticNativeAd.class));
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
    }
}
