package com.mopub.mobileads;

import com.mopub.common.MoPubReward;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedAdDataTest {

    private RewardedAdData subject;

    @Before
    public void setup() {
        subject = new RewardedAdData();
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

    @Test
    public void addAvailableReward_shouldAddRewardToSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        Set<MoPubReward> availableRewards = subject.getAvailableRewards("mopub_id");
        assertThat(availableRewards.size()).isEqualTo(1);
        assertThat(subject.existsInAvailableRewards("mopub_id", "currency1", 123)).isTrue();

        // add another reward for the same adunit
        subject.addAvailableReward("mopub_id", "currency2", "321");
        assertThat(availableRewards.size()).isEqualTo(2);
        assertThat(subject.existsInAvailableRewards("mopub_id", "currency1", 123)).isTrue();
        assertThat(subject.existsInAvailableRewards("mopub_id", "currency2", 321)).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void addAvailableReward_withNullAdUnitId_shouldThrowNPE() throws Exception {
        subject.addAvailableReward(null, "currency_name", "123");
    }

    @Test
    public void addAvailableReward_withNullCurrencyName_shouldNotAddRewardToSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id", null, "123");
        assertThat(subject.getAvailableRewards("mopub_id")).isEmpty();
    }

    @Test
    public void addAvailableReward_withNullCurrencyAmount_shouldNotAddRewardToSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id", "currency_name", null);
        assertThat(subject.getAvailableRewards("mopub_id")).isEmpty();
    }

    @Test
    public void addAvailableReward_withNonNumberCurrencyAmount_shouldNotAddRewardToSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id", "currency_name", "abc");
        assertThat(subject.getAvailableRewards("mopub_id")).isEmpty();
    }

    @Test
    public void addAvailableReward_withCurrencyAmountLessThanZero_shouldNotAddRewardToSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id", "currency_name", "-1");
        assertThat(subject.getAvailableRewards("mopub_id")).isEmpty();
    }

    @Test
    public void getAvailableRewards_shouldReturnSetOfAvailableRewards() throws Exception {
        subject.addAvailableReward("mopub_id_A", "currency1", "123");
        subject.addAvailableReward("mopub_id_A", "currency2", "321");
        subject.addAvailableReward("mopub_id_B", "currency3", "456");

        assertThat(subject.getAvailableRewards("mopub_id_A").size()).isEqualTo(2);
        assertThat(subject.getAvailableRewards("mopub_id_B").size()).isEqualTo(1);

        assertThat(subject.existsInAvailableRewards("mopub_id_A", "currency1", 123)).isTrue();
        assertThat(subject.existsInAvailableRewards("mopub_id_A", "currency1", 123)).isTrue();
        assertThat(subject.existsInAvailableRewards("mopub_id_B", "currency3", 456)).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void getAvailableRewards_withNullAdUnitId_shouldThrowNPE() throws Exception {
        subject.addAvailableReward("mopub_id", "currency_name", "123");
        subject.getAvailableRewards(null);
    }

    @Test
    public void getAvailableRewards_whenAdUnitDoesNotHaveAvailableRewards_shouldReturnEmptySet() throws Exception {
        subject.addAvailableReward("mopub_id", "currency_name", "123");
        assertThat(subject.getAvailableRewards("foo_id")).isEmpty();
    }

    @Test
    public void selectReward_shouldMapAdUnitIdToReward() throws Exception {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        subject.addAvailableReward("mopub_id", "currency2", "321");

        // Select the currency2 reward
        for (MoPubReward reward : subject.getAvailableRewards("mopub_id")) {
            if (reward.getLabel().equals("currency2") && reward.getAmount() == 321) {
                subject.selectReward("mopub_id", reward);
                break;
            }
        }

        MoPubReward selectedReward = subject.getMoPubReward("mopub_id");
        assertThat(selectedReward.getLabel()).isEqualTo("currency2");
        assertThat(selectedReward.getAmount()).isEqualTo(321);
    }

    @Test(expected = NullPointerException.class)
    public void selectReward_withNullAdUnitId_shouldThrowNPE() throws Exception {
        subject.selectReward(null, MoPubReward.success("currency_name", 123));
    }

    @Test(expected = NullPointerException.class)
    public void selectReward_withNullSelectedReward_shouldThrowNPE() throws Exception {
        subject.selectReward("mopub_id", null);
    }

    @Test
    public void selectReward_whenAdUnitDoesNotHaveAvailableRewards_shouldNotSelectReward() throws Exception {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        MoPubReward reward = subject.getAvailableRewards("mopub_id").toArray(new MoPubReward[1])[0];
        subject.selectReward("foo_id", reward);

        assertThat(subject.getMoPubReward("mopub_id")).isNull();
        assertThat(subject.getMoPubReward("foo_id")).isNull();
    }

    @Test
    public void selectReward_whenSelectedRewardNotAmongAvailableRewards_shouldNotSelectReward() throws Exception {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        subject.selectReward("mopub_id", MoPubReward.success("currency2", 321));

        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test
    public void resetAvailableRewards_shouldClearAvailableRewardsMap() {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        subject.addAvailableReward("mopub_id", "currency2", "321");
        assertThat(subject.getAvailableRewards("mopub_id").size()).isEqualTo(2);

        subject.resetAvailableRewards("mopub_id");
        assertThat(subject.getAvailableRewards("mopub_id")).isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void resetAvailableRewards_withNullAdUnitId_shouldThrowNPE() throws Exception {
        subject.resetAvailableRewards(null);
    }

    @Test
    public void resetSelectedReward_shouldClearRewardPreviouslySelectedForAdUnit() {
        subject.addAvailableReward("mopub_id", "currency1", "123");
        MoPubReward reward = subject.getAvailableRewards("mopub_id").toArray(new MoPubReward[1])[0];
        subject.selectReward("mopub_id", reward);

        MoPubReward selectedReward = subject.getMoPubReward("mopub_id");
        assertThat(selectedReward.getLabel()).isEqualTo("currency1");
        assertThat(selectedReward.getAmount()).isEqualTo(123);

        // Reset reward previously selected for AdUnit "mopub_id"
        subject.resetSelectedReward("mopub_id");
        assertThat(subject.getMoPubReward("mopub_id")).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void resetSelectedReward_withNullAdUnitId_shouldThrowNPE() throws Exception {
        subject.resetSelectedReward(null);
    }
}
