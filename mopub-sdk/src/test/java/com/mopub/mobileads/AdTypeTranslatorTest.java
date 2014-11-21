package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
public class AdTypeTranslatorTest {
    private String customEventName;
    private MoPubView moPubView;
    private MoPubInterstitial.MoPubInterstitialView moPubInterstitialView;
    private Context context;

    @Before
    public void setUp() throws Exception {
        moPubView = mock(MoPubView.class);
        moPubInterstitialView = mock(MoPubInterstitial.MoPubInterstitialView.class);

        context = new Activity();
        stub(moPubView.getContext()).toReturn(context);
        stub(moPubInterstitialView.getContext()).toReturn(context);
    }

    @Test
    public void getAdMobBannerReturnsGooglePlayServicesBanner() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubView, "admob_native", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.GooglePlayServicesBanner");
    }

    @Test
    public void getAdMobInterstitialReturnsGooglePlayServicesInterstitial() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubInterstitialView, "interstitial", "admob_full");

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.GooglePlayServicesInterstitial");
    }

    @Test
    public void getMillennialBanner() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubView, "millennial_native", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MillennialBanner");
    }

    @Test
    public void getMillennnialInterstitial() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubInterstitialView, "interstitial", "millennial_full");

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MillennialInterstitial");
    }

    @Test
    public void getMraidBanner() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubView, "mraid", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MraidBanner");
    }

    @Test
    public void getMraidInterstitial() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubInterstitialView, "mraid", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.MraidInterstitial");
    }

    @Test
    public void getHtmlBanner() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubView, "html", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.HtmlBanner");
    }

    @Test
    public void getHtmlInterstitial() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubInterstitialView, "html", null);

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.HtmlInterstitial");
    }

    @Test
    public void getVastInterstitial() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(moPubInterstitialView, "interstitial", "vast");

        assertThat(customEventName).isEqualTo("com.mopub.mobileads.VastVideoInterstitial");
    }

    @Test
    public void getCustomEventNameForAdType_whenSendingNonsense_shouldReturnNull() throws Exception {
        customEventName = AdTypeTranslator.getCustomEventNameForAdType(null, null, null);

        assertThat(customEventName).isNull();
    }
}
