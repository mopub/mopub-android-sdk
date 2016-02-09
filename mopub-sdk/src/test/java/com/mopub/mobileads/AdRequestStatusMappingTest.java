package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdRequestStatusMappingTest {
    private AdRequestStatusMapping subject;
    private String key1;

    @Before
    public void setUp() {
        subject = new AdRequestStatusMapping();
        key1 = "adUnitId1";
    }

    @Test
    public void markFail_shouldNullOutAllValues() {
        subject.markFail(key1);

        assertThat(subject.getFailoverUrl(key1)).isNull();
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();

        assertThat(subject.canPlay(key1)).isFalse();
        assertThat(subject.isLoading(key1)).isFalse();
    }

    @Test
    public void markLoading_shouldNotUpdateUrls_shouldSetIsLoadingTrue() {
        subject.markLoading(key1);

        assertThat(subject.getFailoverUrl(key1)).isNull();
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();

        assertThat(subject.canPlay(key1)).isFalse();
        assertThat(subject.isLoading(key1)).isTrue();
    }

    @Test
    public void markLoaded_shouldUpdateUrls_shouldSetCanPlayTrue() {
        subject.markLoaded(key1, "fail", "imp", "click");

        assertThat(subject.getFailoverUrl(key1)).isEqualTo("fail");
        assertThat(subject.getImpressionTrackerUrlString(key1)).isEqualTo("imp");
        assertThat(subject.getClickTrackerUrlString(key1)).isEqualTo("click");

        assertThat(subject.canPlay(key1)).isTrue();
        assertThat(subject.isLoading(key1)).isFalse();
    }

    @Test
    public void markLoaded_withNullValues_shouldUpdateUrls_shouldSetCanPlayTrue() {
        subject.markLoaded(key1, null, null, null);

        assertThat(subject.getFailoverUrl(key1)).isNull();
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();

        assertThat(subject.canPlay(key1)).isTrue();
        assertThat(subject.isLoading(key1)).isFalse();
    }

    @Test
    public void markPlayed_afterLoaded_shouldKeepExistingUrls_shouldSetCanPlayFalse() {
        subject.markLoaded(key1, "fail", "imp", "click");
        subject.markPlayed(key1);

        assertThat(subject.getFailoverUrl(key1)).isEqualTo("fail");
        assertThat(subject.getImpressionTrackerUrlString(key1)).isEqualTo("imp");
        assertThat(subject.getClickTrackerUrlString(key1)).isEqualTo("click");

        assertThat(subject.canPlay(key1)).isFalse();
        assertThat(subject.isLoading(key1)).isFalse();
    }

    @Test
    public void markPlayed_beforeLoaded_shouldSetUrlsNull_shouldSetCanPlayFalse() {
        subject.markPlayed(key1);

        assertThat(subject.getFailoverUrl(key1)).isNull();
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();

        assertThat(subject.canPlay(key1)).isFalse();
        assertThat(subject.isLoading(key1)).isFalse();
    }

    @Test
    public void clearImpression_shouldResetImpressionUrl() {
        subject.markLoaded(key1, "fail", "imp", "click");
        subject.clearImpressionUrl(key1);

        assertThat(subject.getFailoverUrl(key1)).isEqualTo("fail");
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isEqualTo("click");
    }

    @Test
    public void clearclick_shouldResetClickurl() {
        subject.markLoaded(key1, "fail", "imp", "click");
        subject.clearClickUrl(key1);

        assertThat(subject.getFailoverUrl(key1)).isEqualTo("fail");
        assertThat(subject.getImpressionTrackerUrlString(key1)).isEqualTo("imp");
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();
    }

    @Test
    public void allAccessors_withInvalidKey_shouldReturnDefaultsAndNotThrowExceptions() {
        assertThat(subject.getFailoverUrl(key1)).isNull();
        assertThat(subject.getImpressionTrackerUrlString(key1)).isNull();
        assertThat(subject.getClickTrackerUrlString(key1)).isNull();

        assertThat(subject.canPlay(key1)).isFalse();
        assertThat(subject.isLoading(key1)).isFalse();

        subject.clearImpressionUrl(key1);
        subject.clearClickUrl(key1);
    }
}
