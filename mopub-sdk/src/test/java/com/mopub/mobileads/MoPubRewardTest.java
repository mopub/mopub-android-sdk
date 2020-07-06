// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import com.mopub.common.MoPubReward;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class MoPubRewardTest {
    private MoPubReward subject;

    @Test
    public void success_whenRewardNegative_shouldSetRewardToZero() {
        subject = MoPubReward.success( "label1", -100);

        assertThat(subject.getAmount()).isEqualTo(MoPubReward.DEFAULT_REWARD_AMOUNT);
        assertThat(subject.getLabel()).isEqualTo("label1");
        assertThat(subject.isSuccessful()).isTrue();
    }

    @Test
    public void success_withRewardZero_shouldSetRewardToZero() {
        subject = MoPubReward.success( "label2", 0);

        assertThat(subject.getAmount()).isEqualTo(0);
        assertThat(subject.getLabel()).isEqualTo("label2");
        assertThat(subject.isSuccessful()).isTrue();
    }

    @Test
    public void success_withRewardPositive_shouldSetReward() {
        subject = MoPubReward.success( "label3", 7447);

        assertThat(subject.getAmount()).isEqualTo(7447);
        assertThat(subject.getLabel()).isEqualTo("label3");
        assertThat(subject.isSuccessful()).isTrue();
    }

    @Test
    public void fail_shouldSetRewardToDefault() {
        subject = MoPubReward.failure();

        assertThat(subject.getAmount()).isEqualTo(MoPubReward.DEFAULT_REWARD_AMOUNT);
        assertThat(subject.getLabel()).isEqualTo(MoPubReward.NO_REWARD_LABEL);
        assertThat(subject.isSuccessful()).isFalse();
    }
}
