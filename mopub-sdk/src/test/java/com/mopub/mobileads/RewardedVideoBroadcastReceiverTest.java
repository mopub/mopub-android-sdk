package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;

import com.mopub.common.test.support.SdkTestRunner;

import org.fest.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Iterator;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedVideoBroadcastReceiverTest {

    private RewardedVastVideoInterstitial.CustomEventRewardedVideoInterstitialListener customEventRewardedVideoInterstitialListener;
    private RewardedVideoBroadcastReceiver subject;
    private Activity context;
    private long broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        customEventRewardedVideoInterstitialListener = mock(
                RewardedVastVideoInterstitial.CustomEventRewardedVideoInterstitialListener.class);
        broadcastIdentifier = 123456L;
        subject = new RewardedVideoBroadcastReceiver(customEventRewardedVideoInterstitialListener,
                broadcastIdentifier);
        context = new Activity();
    }

    @Test
    public void constructor_shouldSetIntentFilter() throws Exception {
        Set<String> expectedActions = Sets.newLinkedHashSet(
                RewardedVideoBroadcastReceiver.ACTION_REWARDED_VIDEO_COMPLETE
        );

        final IntentFilter intentFilter = subject.getIntentFilter();
        final Iterator<String> actionIterator = intentFilter.actionsIterator();

        assertThat(intentFilter.countActions()).isEqualTo(1);
        while (actionIterator.hasNext()) {
            assertThat(expectedActions.contains(actionIterator.next()));
        }
    }

    @Test
    public void onReceive_withActionRewardedVideoComplete_shouldNotifyListener() {
        final Intent rewardedVideoCompleteIntent = new Intent();
        rewardedVideoCompleteIntent.setAction(
                RewardedVideoBroadcastReceiver.ACTION_REWARDED_VIDEO_COMPLETE);
        rewardedVideoCompleteIntent.putExtra("broadcastIdentifier", broadcastIdentifier);

        subject.onReceive(context, rewardedVideoCompleteIntent);

        verify(customEventRewardedVideoInterstitialListener).onVideoComplete();
    }
}
