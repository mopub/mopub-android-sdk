package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdTypeTranslatorTest {
    private String customEventName;
    private MoPubView moPubView;
    private MoPubInterstitial.MoPubInterstitialView moPubInterstitialView;
    HashMap<String, String> headers;

    @Before
    public void setUp() throws Exception {
        moPubView = mock(MoPubView.class);
        moPubInterstitialView = mock(MoPubInterstitial.MoPubInterstitialView.class);

        Context context = Robolectric.buildActivity(Activity.class).create().get();
        stub(moPubView.getContext()).toReturn(context);
        stub(moPubInterstitialView.getContext()).toReturn(context);

        headers = new HashMap<String, String>();
    }

    @Test
    public void getCustomEventName_shouldBeGoogleBanner() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, "admob_native", null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.GooglePlayServicesBanner");
    }

    @Test
    public void getCustomEventName_shouldBeGoogleInterstitial() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, "interstitial", "admob_full", headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.GooglePlayServicesInterstitial");
    }

    @Test
    public void getCustomEventName_shouldBeMillenialBanner() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, "millennial_native", null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MillennialBanner");
    }

    @Test
    public void getCustomEventName_shouldBeMillennialIntersitial() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.INTERSTITIAL, "interstitial", "millennial_full", headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MillennialInterstitial");
    }

    @Test
    public void getCustomEventName_shouldBeMraidBanner() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, AdType.MRAID, null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mraid.MraidBanner");
    }

    @Test
    public void getCustomEventName_shouldBeMraidInterstitial() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.INTERSTITIAL, AdType.MRAID, null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mraid.MraidInterstitial");
    }

    @Test
    public void getCustomEventName_shouldBeHtmlBanner() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, "html", null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.HtmlBanner");
    }

    @Test
    public void getCustomEventName_shouldBeHtmlInterstitial() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.INTERSTITIAL, "html", null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.HtmlInterstitial");
    }

    @Test
    public void getCustomEventName_shouldBeVastInterstitial() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.INTERSTITIAL, "interstitial", "vast", headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.VastVideoInterstitial");
    }

    @Test
    public void getCustomEventName_shouldBeCustomClassName() {
        headers.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.example.CustomClass");
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, AdType.CUSTOM, null, headers);

        assertThat(customEventName).isEqualTo("com.example.CustomClass");
    }

    @Test
    public void getCustomEventName_whenNameNotInHeaders_shouldBeNull() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, AdType.CUSTOM, null, headers);

        assertThat(customEventName).isNull();
    }

    @Test
    public void getCustomEventName_withNativeFormat_shouldBeMoPubNative() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.NATIVE, AdType.STATIC_NATIVE, null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.nativeads.MoPubCustomEventNative");
    }

    @Test
    public void getCustomEventName_whenInvalidAdTypeAndInvalidFullAdType_shouldReturnNull() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.BANNER, "garbage", "garbage",
                headers);
        assertThat(customEventName).isNull();
    }

    @Test
    public void getCustomEventName_withRewardedVideoFormat_shouldBeMoPubRewardedVideo() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.REWARDED_VIDEO,
                AdType.REWARDED_VIDEO, null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MoPubRewardedVideo");
    }

    @Test
    public void getCustomEventName_withRewardedPlayableFormat_shouldBeMoPubRewardedPlayable() {
        customEventName = AdTypeTranslator.getCustomEventName(AdFormat.INTERSTITIAL,
                AdType.REWARDED_PLAYABLE, null, headers);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MoPubRewardedPlayable");
    }

    @Test
    public void isMoPubSpecific_withMoPubInterstitialClassNames_shouldBeTrue() {
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mraid.MraidInterstitial")).isTrue();
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mobileads.HtmlInterstitial")).isTrue();
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mobileads.VastVideoInterstitial")).isTrue();
    }

    @Test
    public void isMoPubSpecific_withMoPubRewardedClassNames_shouldBeTrue() {
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mobileads.MoPubRewardedVideo")).isTrue();
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mobileads.MoPubRewardedPlayable")).isTrue();
    }

    @Test
    public void isMoPubSpecific_withNonMoPubClassNames_shouldBeFalse() {
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.mopub.mobileads.GooglePlayServicesBanner")).isFalse();
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific("com.whatever.ads.SomeRandomAdFormat")).isFalse();
        assertThat(AdTypeTranslator.CustomEventType
                .isMoPubSpecific(null)).isFalse();
    }
}
