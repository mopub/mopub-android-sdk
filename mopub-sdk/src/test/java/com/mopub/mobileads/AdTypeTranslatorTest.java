// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class AdTypeTranslatorTest {
    private String baseAdClassName;
    JSONObject headers;

    @Before
    public void setUp() throws Exception {
        Map<String, String> stringHeaders = new HashMap<>();
        headers = new JSONObject(stringHeaders);
    }

    @Test
    public void getBaseAdClassName_shouldBeGoogleBanner() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, "admob_native", null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.GooglePlayServicesBanner");
    }

    @Test
    public void getBaseAdClassName_shouldBeGoogleInterstitial() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, "interstitial", "admob_full", headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.GooglePlayServicesInterstitial");
    }

    @Test
    public void getBaseAdClassName_forMraid_shouldBeMraidBanner() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, AdType.MRAID, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubInline");
    }

    @Test
    public void getBaseAdClassName_forInterstitial_shouldBeMoPubFullscreen() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.INTERSTITIAL, AdType.MRAID, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void getBaseAdClassName_shouldBeMoPubInline() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, "html", null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubInline");
    }

    @Test
    public void getBaseAdClassName_forHtml_shouldBeMoPubFullscreen() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.INTERSTITIAL, "html", null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void getBaseAdClassName_shouldBeVastInterstitial() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.INTERSTITIAL, "interstitial", "vast", headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void getBaseAdClassName_shouldBeCustomClassName() throws JSONException {
        headers.put(ResponseHeader.CUSTOM_EVENT_NAME.getKey(), "com.example.CustomClass");
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, AdType.CUSTOM, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.example.CustomClass");
    }

    @Test
    public void getBaseAdClassName_whenNameNotInHeaders_shouldBeEmpty() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, AdType.CUSTOM, null, headers);

        assertThat(baseAdClassName).isEmpty();
    }

    @Test
    public void getBaseAdClassName_withNativeFormat_shouldBeMoPubNative() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.NATIVE, AdType.STATIC_NATIVE, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.nativeads.MoPubCustomEventNative");
    }

    @Test
    public void getBaseAdClassName_withNativeVideoFormat_shouldBeMoPubNativeVideo() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.NATIVE, AdType.VIDEO_NATIVE, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.nativeads.MoPubCustomEventVideoNative");
    }

    @Test
    public void getBaseAdClassName_whenInvalidAdTypeAndInvalidFullAdType_shouldReturnNull() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.BANNER, "garbage", "garbage",
                headers);
        assertThat(baseAdClassName).isNull();
    }

    @Test
    public void getBaseAdClassName_withRewardedVideoFormat_shouldBeMoPubFullscreen() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.REWARDED_VIDEO,
                AdType.REWARDED_VIDEO, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void getBaseAdClassName_withRewardedPlayableFormat_shouldBeMoPubFullscreen() {
        baseAdClassName = AdTypeTranslator.getBaseAdClassName(AdFormat.INTERSTITIAL,
                AdType.REWARDED_PLAYABLE, null, headers);

        assertThat(baseAdClassName).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void isMoPubSpecific_withMoPubInlineClassNames_shouldBeTrue() {
        assertThat(AdTypeTranslator.BaseAdType
                .isMoPubSpecific("com.mopub.mobileads.MoPubInline")).isTrue();
    }

    @Test
    public void isMoPubSpecific_withMoPubFullscreenClassNames_shouldBeTrue() {
        assertThat(AdTypeTranslator.BaseAdType
                .isMoPubSpecific("com.mopub.mobileads.MoPubFullscreen")).isTrue();
    }

    @Test
    public void isMoPubSpecific_withNonMoPubClassNames_shouldBeFalse() {
        assertThat(AdTypeTranslator.BaseAdType
                .isMoPubSpecific("com.mopub.mobileads.GooglePlayServicesBanner")).isFalse();
        assertThat(AdTypeTranslator.BaseAdType
                .isMoPubSpecific("com.whatever.ads.SomeRandomAdFormat")).isFalse();
        assertThat(AdTypeTranslator.BaseAdType
                .isMoPubSpecific(null)).isFalse();
    }
}
