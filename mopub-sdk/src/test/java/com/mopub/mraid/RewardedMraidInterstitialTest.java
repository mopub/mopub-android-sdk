package com.mopub.mraid;

import android.app.Activity;
import android.content.Intent;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.ResponseBodyInterstitialTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REWARDED_AD_DURATION_KEY;
import static com.mopub.common.DataKeys.SHOULD_REWARD_ON_CLICK_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.common.IntentActions.ACTION_REWARDED_PLAYABLE_COMPLETE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedMraidInterstitialTest extends ResponseBodyInterstitialTest {
    private static final String EXPECTED_HTML_DATA = "<html></html>";
    private static final int EXPECTED_REWARDED_DURATION_SECONDS = 15;
    private static final Boolean EXPECTED_SHOULD_REWARD_ON_CLICK = true;
    private long broadcastIdentifier;

    @Mock RewardedMraidInterstitial.RewardedMraidInterstitialListener customEventInterstitialListener;

    private Map<String, Object> localExtras;
    private Map<String, String> serverExtras;
    private Activity context;

    @Before
    public void setUp() throws Exception {
        broadcastIdentifier = 4321;

        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put(HTML_RESPONSE_BODY_KEY, EXPECTED_HTML_DATA);
        localExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        localExtras.put(REWARDED_AD_DURATION_KEY, EXPECTED_REWARDED_DURATION_SECONDS);
        localExtras.put(SHOULD_REWARD_ON_CLICK_KEY, EXPECTED_SHOULD_REWARD_ON_CLICK);

        context = Robolectric.buildActivity(Activity.class).create().get();

        subject = new RewardedMraidInterstitial();
    }

    @Test
    public void loadInterstitial_withLocalExtras_shouldSetRewardedDuration_shouldSetShouldRewardOnClick() {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras,
                serverExtras);

        assertThat(((RewardedMraidInterstitial) subject).getRewardedDuration()).isEqualTo(
                EXPECTED_REWARDED_DURATION_SECONDS);
        assertThat(((RewardedMraidInterstitial) subject).isShouldRewardOnClick()).isEqualTo(
                EXPECTED_SHOULD_REWARD_ON_CLICK);
    }

    @Test
    public void loadInterstitial_withLocalExtrasNotComplete_shouldUseDefaultRewardedDurationAndShouldRewardOnClick() {
        localExtras.remove(REWARDED_AD_DURATION_KEY);
        localExtras.remove(SHOULD_REWARD_ON_CLICK_KEY);
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras,
                serverExtras);

        assertThat(((RewardedMraidInterstitial) subject).getRewardedDuration()).isEqualTo(
                RewardedMraidController.DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS);
        assertThat(((RewardedMraidInterstitial) subject).isShouldRewardOnClick()).isEqualTo(
                RewardedMraidController.DEFAULT_PLAYABLE_SHOULD_REWARD_ON_CLICK);

    }

    @Test
    public void loadInterstitial_shouldConnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras,
                serverExtras);

        Intent intent =
                getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialShown();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialClicked();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialDismissed();

        intent = getIntentForActionAndIdentifier(ACTION_REWARDED_PLAYABLE_COMPLETE,
                broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onMraidComplete();
    }

    @Test
    public void showInterstitial_shouldStartActivityWithIntent() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras,
                serverExtras);
        subject.showInterstitial();

        ShadowActivity shadowActivity = Shadows.shadowOf(context);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent().getClassName())
                .isEqualTo("com.mopub.mobileads.RewardedMraidActivity");
        assertThat(intent.getExtras().get(HTML_RESPONSE_BODY_KEY)).isEqualTo(EXPECTED_HTML_DATA);
        assertThat(intent.getExtras().get(REWARDED_AD_DURATION_KEY)).isEqualTo(
                EXPECTED_REWARDED_DURATION_SECONDS);
        assertThat(intent.getExtras().get(SHOULD_REWARD_ON_CLICK_KEY)).isEqualTo(
                EXPECTED_SHOULD_REWARD_ON_CLICK);
    }

    @Test
    public void onInvalidate_shouldDisconnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras,
                serverExtras);
        subject.onInvalidate();

        Intent intent;
        intent = new Intent(ACTION_INTERSTITIAL_SHOW);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialShown();

        intent = new Intent(ACTION_INTERSTITIAL_DISMISS);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialDismissed();

        intent = new Intent(ACTION_REWARDED_PLAYABLE_COMPLETE);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onMraidComplete();
    }
}
