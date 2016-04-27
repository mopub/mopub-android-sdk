package com.mopub.mobileads;

import com.mopub.common.MoPubReward;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedVideoDataTest {

    private RewardedVideoData subject;

    @Before
    public void setup() {
        subject = new RewardedVideoData();
    }

    @Test
    public void updateAdUnitRewardMapping_shouldMapAdUnitIdToReward() throws Exception {
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", "123");
        MoPubReward moPubReward = subject.getMoPubReward("mopub_id");
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
        assertThat(moPubReward.getAmount()).isEqualTo(123);
    }

    @Test
    public void updateAdUnitRewardMapping_withNullCurrencyName_shouldRemoveExistingAdUnitMapping_shouldNotMapAdUnitIdToReward() throws Exception {
        // Insert initial value to be removed with next call
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", "123");
        MoPubReward moPubReward = subject.getMoPubReward("mopub_id");
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
        assertThat(moPubReward.getAmount()).isEqualTo(123);

        subject.updateAdUnitRewardMapping("mopub_id", null, "123");
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test
    public void updateAdUnitRewardMapping_withNullCurrencyName_shouldNotMapAdUnitIdToReward() throws Exception {
        // Insert initial value to be removed with next call
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", "123");
        MoPubReward moPubReward = subject.getMoPubReward("mopub_id");
        assertThat(moPubReward.getLabel()).isEqualTo("currency_name");
        assertThat(moPubReward.getAmount()).isEqualTo(123);

        subject.updateAdUnitRewardMapping("mopub_id", null, "123");
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test
    public void updateAdUnitRewardMapping_withNullCurrencyAmount_shouldNotMapAdUnitIdToReward() throws Exception {
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", null);
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test
    public void updateAdUnitRewardMapping_withNonNumberCurrencyAmount_shouldNotMapAdUnitIdToReward() throws Exception {
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", "abc");
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test
    public void updateAdUnitRewardMapping_withCurrencyAmountLessThanZero_shouldNotMapAdUnitIdToReward() throws Exception {
        subject.updateAdUnitRewardMapping("mopub_id", "currency_name", "-1");
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }
}
