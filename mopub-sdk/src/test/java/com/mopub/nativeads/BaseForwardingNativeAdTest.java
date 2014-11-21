package com.mopub.nativeads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class BaseForwardingNativeAdTest {

    private BaseForwardingNativeAd subject;

    @Before
    public void setUp() throws Exception {
        subject = new BaseForwardingNativeAd() {};
    }

    @Test
    public void constructor_shouldInitializeMembers() throws Exception {
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(50);
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(1000);
        assertThat(subject.getImpressionTrackers()).isEmpty();
        assertThat(subject.getExtras()).isEmpty();
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
}
