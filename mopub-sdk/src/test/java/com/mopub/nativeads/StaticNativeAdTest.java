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
public class StaticNativeAdTest {

    private StaticNativeAd subject;

    @Before
    public void setUp() throws Exception {
        subject = new StaticNativeAd() {};

        subject.setTitle("title");
        subject.setText("text");
        subject.setMainImageUrl("mainImageUrl");
        subject.setIconImageUrl("iconImageUrl");
        subject.setClickDestinationUrl("clickDestinationUrl");
        subject.setCallToAction("callToAction");
        subject.setStarRating(5.0);
        subject.setPrivacyInformationIconClickThroughUrl("privacyInformationIconClickThroughUrl");
        subject.setPrivacyInformationIconImageUrl("privacyInformationIconImageUrl");
        subject.addExtra("extra", "extraValue");
        subject.addExtra("extraImage", "extraImageUrl");
        subject.addImpressionTracker("impressionUrl");
        subject.setImpressionMinTimeViewed(500);
        subject.setImpressionMinPercentageViewed(10);
    }

    @Test
    public void getters_shouldReturnCorrectValues() throws Exception {
        assertThat(subject.getTitle()).isEqualTo("title");
        assertThat(subject.getText()).isEqualTo("text");
        assertThat(subject.getMainImageUrl()).isEqualTo("mainImageUrl");
        assertThat(subject.getIconImageUrl()).isEqualTo("iconImageUrl");
        assertThat(subject.getClickDestinationUrl()).isEqualTo("clickDestinationUrl");
        assertThat(subject.getCallToAction()).isEqualTo("callToAction");
        assertThat(subject.getStarRating()).isEqualTo(5.0);
        assertThat(subject.getPrivacyInformationIconClickThroughUrl()).isEqualTo(
                "privacyInformationIconClickThroughUrl");
        assertThat(subject.getPrivacyInformationIconImageUrl()).isEqualTo
                ("privacyInformationIconImageUrl");
        assertThat(subject.getExtra("extra")).isEqualTo("extraValue");
        assertThat(subject.getExtra("extraImage")).isEqualTo("extraImageUrl");
        assertThat(subject.getExtras()).hasSize(2);
        assertThat(subject.getImpressionTrackers()).containsOnly("impressionUrl");
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(500);
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(10);
    }

    @Test
    public void setImpressionMinTimeViewed_whenTimeIsGreaterThan0_shouldSetTime() throws Exception {
        subject.setImpressionMinTimeViewed(250);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(250);
    }

    @Test
    public void setImpressionMinTimeViewed_whenTimeIsLessThan0_shouldNotSetTime() throws Exception {
        subject.setImpressionMinTimeViewed(250);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(250);

        subject.setImpressionMinTimeViewed(-1);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(250);
    }

    @Test
    public void setImpressionMinTimeViewed_whenTimeIs0_shouldNotSetTime() throws Exception {
        subject.setImpressionMinTimeViewed(250);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(250);

        subject.setImpressionMinTimeViewed(0);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(250);
    }

    @Test
    public void setImpressionMinPercentageViewed_whenPercentIsLessThan0_shouldNotSetPercentageViewed() {
        subject.setImpressionMinPercentageViewed(-1);
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(10);
    }

    @Test
    public void setImpressionMinPercentageViewed_whenPercentIs0_shouldSetPercentageViewed() {
        subject.setImpressionMinPercentageViewed(0);
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(0);
    }

    @Test
    public void setImpressionMinPercentageViewed_whenPercentIsGreaterThan100_shouldNotSetPercentageViewed() {
        subject.setImpressionMinPercentageViewed(101);
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(10);
    }

    @Test
    public void setImpressionMinPercentageViewed_whenPercentIsNormal_shouldSetPercentageViewed() {
        subject.setImpressionMinPercentageViewed(35);
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(35);
    }

    @Test
    public void setStarRating_withinValidRange_shouldSetStarRating() throws Exception {
        subject.setStarRating(0.0);
        assertThat(subject.getStarRating()).isEqualTo(0.0);

        subject.setStarRating(5.0);
        assertThat(subject.getStarRating()).isEqualTo(5.0);

        subject.setStarRating(2.5);
        assertThat(subject.getStarRating()).isEqualTo(2.5);
    }

    @Test
    public void setStarRating_withNull_shouldSetStarRatingToNull() throws Exception {
        // Setting star rating to 0 before each case, so we can detect when it gets set to null
        final double initialStarRating = 0.0;

        subject.setStarRating(initialStarRating);
        subject.setStarRating(null);
        assertThat(subject.getStarRating()).isEqualTo(null);
    }

    @Test
    public void setStarRating_withNanOrInf_shouldNotSetStarRating() throws Exception {
        // First, set star rating to a valid value
        final double initialStarRating = 3.75;
        subject.setStarRating(initialStarRating);

        subject.setStarRating(Double.NaN);
        assertThat(subject.getStarRating()).isEqualTo(initialStarRating);

        subject.setStarRating(Double.POSITIVE_INFINITY);
        assertThat(subject.getStarRating()).isEqualTo(initialStarRating);

        subject.setStarRating(Double.NEGATIVE_INFINITY);
        assertThat(subject.getStarRating()).isEqualTo(initialStarRating);
    }

    @Test
    public void setStarRating_withValuesOutsideOfValidRange_shouldNotSetStarRating() throws Exception {
        // First, set star rating to a valid value
        final double initialStarRating = 4.9;
        subject.setStarRating(initialStarRating);

        subject.setStarRating(5.0001);
        assertThat(subject.getStarRating()).isEqualTo(initialStarRating);

        subject.setStarRating(-0.001);
        assertThat(subject.getStarRating()).isEqualTo(initialStarRating);
    }

    @Test
    public void isImpressionRecorded_withRecordedImpression_shouldReturnTrue() throws Exception {
        assertThat(subject.isImpressionRecorded()).isFalse();

        subject.setImpressionRecorded();

        assertThat(subject.isImpressionRecorded()).isTrue();
    }
}
