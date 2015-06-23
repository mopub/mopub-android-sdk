package com.mopub.nativeads;

import android.app.Activity;
import android.graphics.Bitmap;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.nativeads.test.support.MoPubShadowBitmap;
import com.mopub.nativeads.test.support.MoPubShadowDisplay;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.NativeResponse.Parameter;
import static com.mopub.nativeads.NativeResponse.Parameter.requiredKeys;
import static com.mopub.volley.toolbox.ImageLoader.ImageListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(shadows={MoPubShadowBitmap.class, MoPubShadowDisplay.class})
public class MoPubForwardingNativeAdTest {

    private JSONObject fakeJsonObject;
    private MoPubCustomEventNative.MoPubForwardingNativeAd subject;
    private Activity context;

    @Mock
    private CustomEventNative.CustomEventNativeListener mockCustomEventNativeListener;
    @Mock
    private MoPubRequestQueue mockRequestQueue;
    @Mock
    private MaxWidthImageLoader mockImageLoader;
    @Mock
    private ImageLoader.ImageContainer mockImageContainer;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        fakeJsonObject = new JSONObject();
        fakeJsonObject.put("imptracker", new JSONArray("[\"url1\", \"url2\"]"));
        fakeJsonObject.put("clktracker", "expected clicktracker");
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        stub(mockImageContainer.getBitmap()).toReturn(mock(Bitmap.class));
    }

    @Test
    public void parameter_requiredKeys_shouldOnlyContainTheRequiredKeys() throws Exception {
        final HashSet<String> expectedKeys = new HashSet<String>();
        expectedKeys.add("imptracker");
        expectedKeys.add("clktracker");

        assertThat(requiredKeys).isEqualTo(expectedKeys);
    }

    @Test
    public void parameter_fromString_shouldReturnParameterOnMatch() throws Exception {
        final Parameter parameter = Parameter.from("title");

        assertThat(parameter).isEqualTo(Parameter.TITLE);
    }

    @Test
    public void parameter_fromString_shouldReturnNullOnIllegalKey() throws Exception {
        final Parameter parameter = Parameter.from("random gibberish");

        assertThat(parameter).isNull();
    }

    @Test
    public void loadAd_whenMissingRequiredKeys_shouldThrowIllegalArgumentException() throws Exception {
        fakeJsonObject.remove("imptracker");

        try {
            subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
            subject.loadAd();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void loadAd_whenRequiredKeyOfWrongType_shouldThrowIllegalArgumentException() throws Exception {
        fakeJsonObject.put("imptracker", 12345);

        try {
            subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
            subject.loadAd();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void loadAd_shouldSetRequiredExpectedFields() throws Exception {
        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();
        assertThat(subject.getImpressionTrackers()).containsOnly("url1", "url2");
    }

    @Test
    public void loadAd_shouldSetOptionalExpectedFields() throws Exception {
        fakeJsonObject.put("title", "expected title");
        fakeJsonObject.put("text", "expected text");
        fakeJsonObject.put("mainimage", "expected mainimage");
        fakeJsonObject.put("iconimage", "expected iconimage");

        fakeJsonObject.put("clk", "expected clk");

        fakeJsonObject.put("fallback", "expected fallback");
        fakeJsonObject.put("ctatext", "expected ctatext");
        fakeJsonObject.put("starrating", 5.0);

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getTitle()).isEqualTo("expected title");
        assertThat(subject.getText()).isEqualTo("expected text");
        assertThat(subject.getMainImageUrl()).isEqualTo("expected mainimage");
        assertThat(subject.getIconImageUrl()).isEqualTo("expected iconimage");

        assertThat(subject.getClickDestinationUrl()).isEqualTo("expected clk");

//        assertThat(subject.getFallback()).isEqualTo("expected fallback");
        assertThat(subject.getImpressionTrackers()).containsOnly("url1", "url2");
        assertThat(subject.getCallToAction()).isEqualTo("expected ctatext");
        assertThat(subject.getStarRating()).isEqualTo(5.0);
    }

    @Test
    public void loadAd_withIntegerStarRating_shouldSetStarRating() throws Exception {
        fakeJsonObject.put("starrating", 3);

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getStarRating()).isEqualTo(3.0);
    }

    @Test
    public void loadAd_withStringStarRating_shouldSetStarRating() throws Exception {
        fakeJsonObject.put("starrating", "2.3");

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getStarRating()).isEqualTo(2.3);
    }

    @Test
    public void loadAd_withInvalidStringStarRating_shouldNotSetStarRating() throws Exception {
        fakeJsonObject.put("starrating", "this is not a number");

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getStarRating()).isNull();
    }

    @Test
    public void loadAd_withInvalidlyTypedStarRating_shouldNotSetStarRating() throws Exception {
        fakeJsonObject.put("starrating", new Activity());

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getStarRating()).isNull();
    }

    @Test
    public void loadAd_whenImpressionTrackersContainsNonStrings_willCoerceToString() throws Exception {
        /**
         * At this level of abstraction, we don't actually care that these Strings resolve to valid
         * URLs. We just want to ensure that loadAd does not throw an exception.
         */
        final JSONArray impressionTrackers = new JSONArray();
        impressionTrackers.put("url1");
        impressionTrackers.put(JSONObject.NULL);
        impressionTrackers.put(2.12);
        fakeJsonObject.put("imptracker", impressionTrackers);

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getImpressionTrackers()).containsOnly("url1", "null", "2.12");
    }

    @Test
    public void loadAd_shouldSetExtraFields() throws Exception {
        List<Object> array = new ArrayList<Object>();
        array.add("index1");
        array.add(-10);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("one", "a");
        map.put("two", "b");

        fakeJsonObject.put("key1", "yay json");
        fakeJsonObject.put("key2", 5);
        fakeJsonObject.put("key3", new JSONArray(array));
        fakeJsonObject.put("key4", new JSONObject(map));

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getExtra("key1")).isEqualTo("yay json");
        assertThat(subject.getExtra("key2")).isEqualTo(5);
        assertThat((JSONArray) subject.getExtra("key3")).isEqualsToByComparingFields(new JSONArray(array));
        assertThat((JSONObject) subject.getExtra("key4")).isEqualsToByComparingFields(new JSONObject(map));
    }

    @Test
    public void loadAd_shouldDownloadImages_shouldNotifyListenerOfOnNativeAdLoaded() throws Exception {
        fakeJsonObject.put("mainimage", "mainimageurl");
        fakeJsonObject.put("iconimage", "iconimageurl");
        fakeJsonObject.put("extraimage", "extraimageurl");

        when(mockImageLoader.get(anyString(), any(ImageListener.class)))
                .then(new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        ImageListener listener = ((ImageListener) invocationOnMock.getArguments()[1]);
                        listener.onResponse(mockImageContainer, false);
                        return null;
                    }
                });

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        verify(mockImageLoader).get(eq("mainimageurl"), any(ImageListener.class));
        verify(mockImageLoader).get(eq("iconimageurl"), any(ImageListener.class));
        verify(mockImageLoader).get(eq("extraimageurl"), any(ImageListener.class));

        verify(mockCustomEventNativeListener).onNativeAdLoaded(subject);
        verify(mockCustomEventNativeListener, never()).onNativeAdFailed(any(NativeErrorCode.class));
    }

    @Test
    public void loadAd_withFailedImageDownload_shouldNotDownloadImages_shouldNotifyListenerOfOnNativeAdFailed() throws Exception {
        fakeJsonObject.put("mainimage", "mainimageurl");
        fakeJsonObject.put("iconimage", "iconimageurl");
        fakeJsonObject.put("extraimage", "extraimageurl");

        when(mockImageLoader.get(anyString(), any(ImageListener.class)))
                .then(new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        ImageListener listener = ((ImageListener) invocationOnMock.getArguments()[1]);
                        listener.onErrorResponse(new VolleyError());
                        return null;
                    }
                });

        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(subject);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.IMAGE_DOWNLOAD_FAILURE);
    }

    @Test
    public void getExtrasImageUrls_whenExtrasContainsImages_shouldReturnImageUrls() throws Exception {
        // getExtrasImageUrls requires the key to end with a case-insensitive "image" to be counted as an image
        fakeJsonObject.put("test_image", "image_url_1");
        fakeJsonObject.put("filler", "ignored");
        fakeJsonObject.put("otherIMAGE", "image_url_2");
        fakeJsonObject.put("more filler", "ignored");
        fakeJsonObject.put("lastimage", "image_url_3");
        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getExtrasImageUrls()).containsOnly("image_url_1", "image_url_2", "image_url_3");
    }


    @Test
    public void getExtrasImageUrls_whenExtrasDoesNotContainImageKeys_shouldReturnEmptyList() throws Exception {
        // getExtrasImageUrls requires the key to end with a case-insensitive "image" to be counted as an image
        fakeJsonObject.put("imageAtFront", "ignored");
        fakeJsonObject.put("middle_image_in_key", "ignored");
        fakeJsonObject.put("other", "ignored");
        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getExtrasImageUrls()).isEmpty();
    }

    @Test
    public void getAllImageUrls_withExtraImagesAndMainAndIconImages_shouldReturnAllUrls() throws Exception {
        fakeJsonObject.put("mainimage", "mainImageUrl");
        fakeJsonObject.put("iconimage", "iconImageUrl");
        fakeJsonObject.put("extraimage", "extraImageUrl");
        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getAllImageUrls()).containsOnly(
                "mainImageUrl",
                "iconImageUrl",
                "extraImageUrl"
        );
    }

    @Test
    public void getAllImageUrls_withOnlyExtrasImages_shouldNotIncludeMainOrIconImages() throws Exception {
        fakeJsonObject.put("extra1_image", "expected extra1_image");
        fakeJsonObject.put("extra2_image", "expected extra2_image");
        subject = new MoPubCustomEventNative.MoPubForwardingNativeAd(context, fakeJsonObject, mockCustomEventNativeListener);
        subject.loadAd();

        assertThat(subject.getAllImageUrls()).containsOnly("expected extra1_image", "expected extra2_image");
    }
}
