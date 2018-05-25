package com.mopub.nativeads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VideoNativeAdTest {

    private VideoNativeAd subject;

    @Before
    public void setUp() {
        subject = new VideoNativeAd() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) { }

            @Override
            public void onError(Exception e) { }
        };

        subject.setTitle("title");
        subject.setText("text");
        subject.setMainImageUrl("mainImageUrl");
        subject.setIconImageUrl("iconImageUrl");
        subject.setClickDestinationUrl("clickDestinationUrl");
        subject.setVastVideo("vastVideo");
        subject.setCallToAction("callToAction");
        subject.setPrivacyInformationIconClickThroughUrl("privacyInformationIconClickThroughUrl");
        subject.setPrivacyInformationIconImageUrl("privacyInformationIconImageUrl");
        subject.addExtra("extra", "extraValue");
        subject.addExtra("extraImage", "extraImageUrl");
        subject.addImpressionTracker("impressionUrl");
    }

    @Test
    public void getters_shouldReturnCorrectValues() {
        assertThat(subject.getTitle()).isEqualTo("title");
        assertThat(subject.getText()).isEqualTo("text");
        assertThat(subject.getMainImageUrl()).isEqualTo("mainImageUrl");
        assertThat(subject.getIconImageUrl()).isEqualTo("iconImageUrl");
        assertThat(subject.getClickDestinationUrl()).isEqualTo("clickDestinationUrl");
        assertThat(subject.getCallToAction()).isEqualTo("callToAction");
        assertThat(subject.getPrivacyInformationIconClickThroughUrl()).isEqualTo(
                "privacyInformationIconClickThroughUrl");
        assertThat(subject.getPrivacyInformationIconImageUrl()).isEqualTo
                ("privacyInformationIconImageUrl");
        assertThat(subject.getExtra("extra")).isEqualTo("extraValue");
        assertThat(subject.getExtra("extraImage")).isEqualTo("extraImageUrl");
        assertThat(subject.getExtras()).hasSize(2);
        assertThat(subject.getImpressionTrackers()).containsOnly("impressionUrl");
    }
}
