package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.MoPubCustomEventVideoNative.VideoResponseHeaders;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.Networking;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubCustomEventVideoNativeTest {

    private MoPubCustomEventVideoNative subject;
    private Activity context;
    private HashMap<String, Object> localExtras;
    private HashMap<String, String> serverExtras;
    private JSONObject jsonObject;

    @Mock private CustomEventNative.CustomEventNativeListener mockCustomEventNativeListener;

    @Before
    public void setUp() throws Exception {
        subject = new MoPubCustomEventVideoNative();
        context = Robolectric.buildActivity(Activity.class).create().get();

        localExtras = new HashMap<String, Object>();
        jsonObject = new JSONObject();
        jsonObject.put("imptracker", new JSONArray("[\"url1\", \"url2\"]"));
        jsonObject.put("clktracker", "expected clicktracker");
        jsonObject.put("mainimage", "mainimageurl");
        jsonObject.put("iconimage", "iconimageurl");
        jsonObject.put("extraimage", "extraimageurl");
        localExtras.put(DataKeys.JSON_BODY_KEY, jsonObject);
        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, "clicktrackingurl");

        serverExtras = new HashMap<String, String>();
        serverExtras.put("Play-Visible-Percent", "10");
        serverExtras.put("Pause-Visible-Percent", "5");
        serverExtras.put("Impression-Min-Visible-Percent", "15");
        serverExtras.put("Impression-Visible-Ms", "100");
        serverExtras.put("Max-Buffer-Ms", "20");
        serverExtras.put("Video-Trackers", "{" +
                "urls: [" +
                    "\"http://mopub.com/%%VIDEO_EVENT%%/foo\"," +
                    "\"http://mopub.com/%%VIDEO_EVENT%%/bar\"" +
                "]," +
                "events: [" +
                    "\"start\"," +
                    "\"firstQuartile\"," +
                    "\"midpoint\"," +
                    "\"thirdQuartile\"," +
                    "\"complete\"," +
                    "\"companionAdView\"," +
                    "\"companionAdClick\"" +
                "]" +
            "}");

    }

    @After
    public void tearDown() {
        Networking.setImageLoaderForTesting(null);
    }

    @Test
    public void loadNativeAd_withJsonNotInstanceOfJSONObject_shouldNotifyListenerOnNativeAdFailed() {
        localExtras.put("com_mopub_native_json", "");
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);

        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withInvalidHeaders_shouldNotifyListenerOnNativeAdFailed() {
        serverExtras.put("Play-Visible-Percent", "not_a_number");
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);

        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withMissingRequiredKeys_shouldNotifyListenerOnNativeAdFailed() {
        jsonObject.remove("imptracker");
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);

        reset(mockCustomEventNativeListener);
        jsonObject.remove("clktracker");
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withMissingClickTracker_shouldNotifyListenerOnNativeAdFailed() {
        localExtras.remove(DataKeys.CLICK_TRACKING_URL_KEY);
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withNullClickTracker_shouldNotifyListenerOnNativeAdFailed() {
        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, null);
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withEmptyClickTracker_shouldNotifyListenerOnNativeAdFailed() {
        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, "");
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        verify(mockCustomEventNativeListener, never()).onNativeAdLoaded(any(BaseNativeAd.class));
    }

    @Test
    public void loadNativeAd_withAllRequirementsMet_shouldNotNotifyListenerFailed_shouldNotThrowException() {
        Networking.setImageLoaderForTesting(mock(MaxWidthImageLoader.class));
        subject.loadNativeAd(context, mockCustomEventNativeListener, localExtras, serverExtras);

        verifyNoMoreInteractions(mockCustomEventNativeListener);
    }

    @Test
    public void VideoResponseHeaders_constructor_withValidNumberString_shouldInitializeVariablesCorrectly() {
        VideoResponseHeaders videoResponseHeaders = new VideoResponseHeaders(serverExtras);

        assertThat(videoResponseHeaders.hasValidHeaders()).isTrue();
        assertThat(videoResponseHeaders.getPlayVisiblePercent()).isEqualTo(10);
        assertThat(videoResponseHeaders.getPauseVisiblePercent()).isEqualTo(5);
        assertThat(videoResponseHeaders.getImpressionMinVisiblePercent()).isEqualTo(15);
        assertThat(videoResponseHeaders.getImpressionVisibleMs()).isEqualTo(100);
        assertThat(videoResponseHeaders.getMaxBufferMs()).isEqualTo(20);
    }

    @Test
    public void VideoResponseHeaders_constructor_withValidJson_shouldSetVideoTrackersToJsonObject() throws Exception {
        VideoResponseHeaders videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        JSONObject expectedVideoTrackers = new JSONObject("{" +
                "urls: [" +
                    "\"http://mopub.com/%%VIDEO_EVENT%%/foo\"," +
                    "\"http://mopub.com/%%VIDEO_EVENT%%/bar\"" +
                "]," +
                "events: [" +
                    "\"start\"," +
                    "\"firstQuartile\"," +
                    "\"midpoint\"," +
                    "\"thirdQuartile\"," +
                    "\"complete\"," +
                    "\"companionAdView\"," +
                    "\"companionAdClick\"" +
                "]" +
            "}");

        assertThat(videoResponseHeaders.getVideoTrackers().toString())
                .isEqualTo(expectedVideoTrackers.toString());
    }

    @Test
    public void VideoResponseHeaders_constructor_withInvalidNumberString_withInvalidJson_shouldSetHeadersAreValidToFalse() {
        serverExtras.put("Play-Visible-Percent", "not_a_number");
        VideoResponseHeaders videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isFalse();

        serverExtras.put("Play-Visible-Percent", "10");
        serverExtras.put("Pause-Visible-Percent", "not_a_number");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isFalse();

        serverExtras.put("Pause-Visible-Percent", "5");
        serverExtras.put("Impression-Min-Visible-Percent", "not_a_number");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isFalse();

        serverExtras.put("Impression-Min-Visible-Percent", "15");
        serverExtras.put("Impression-Visible-Ms", "not_a_number");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isFalse();

        serverExtras.put("Impression-Visible-Ms", "100");
        serverExtras.put("Max-Buffer-Ms", "not_a_number");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isFalse();

        serverExtras.put("Max-Buffer-Ms", "20");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        assertThat(videoResponseHeaders.hasValidHeaders()).isTrue();
    }

    @Test
    public void VideoResponseHeaders_constructor_withInvalidJson_shouldSetVideoTrackersToNull() throws Exception {
        serverExtras.put("Video-Trackers", "not_a_json_object");
        VideoResponseHeaders videoResponseHeaders = new VideoResponseHeaders(serverExtras);

        assertThat(videoResponseHeaders.getVideoTrackers()).isNull();
    }
}
