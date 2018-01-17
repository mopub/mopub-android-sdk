package com.mopub.mobileads;

import android.app.Activity;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.RewardedMraidInterstitial;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubRewardedPlayableTest {
    private Activity activity;
    private MoPubRewardedPlayable subject;

    @Mock private RewardedMraidInterstitial mockRewardedMraidInterstitial;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        MoPubRewardedVideoManager.init(activity);

        subject = new MoPubRewardedPlayable();
    }

    @Test
    public void onInvalidate_withRewardedMraidActivity_shouldInvalidateRewardedMraidActivity() {
        subject.setRewardedMraidInterstitial(mockRewardedMraidInterstitial);

        subject.onInvalidate();

        verify(mockRewardedMraidInterstitial).onInvalidate();
    }

    @Test
    public void onInvalidate_withNullRewardedMraidActivity_shouldNotInvalidateRewardedMraidActivity() {
        subject.setRewardedMraidInterstitial(null);

        subject.onInvalidate();

        verifyZeroInteractions(mockRewardedMraidInterstitial);
    }

    @Test
    public void loadWithSdkInitialized_withCorrectLocalExtras_shouldLoadVastVideoInterstitial() throws Exception {
        subject.setRewardedMraidInterstitial(mockRewardedMraidInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        final Map<String, String> serverExtras = new HashMap<String, String>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "currencyName");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "10");
        localExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, "body");
        localExtras.put(DataKeys.REWARDED_AD_DURATION_KEY, "30");
        localExtras.put(DataKeys.SHOULD_REWARD_ON_CLICK_KEY, "0");

        subject.loadWithSdkInitialized(activity, localExtras, serverExtras);

        verify(mockRewardedMraidInterstitial).loadInterstitial(eq(activity),
                any(CustomEventInterstitial.CustomEventInterstitialListener.class), eq(localExtras),
                eq(serverExtras));
        verifyNoMoreInteractions(mockRewardedMraidInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("currencyName");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(10);
    }

    @Test
    public void loadWithSdkInitialized_withAdUnitId_shouldSetAdNetworkId() throws Exception {
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        localExtras.put(DataKeys.AD_UNIT_ID_KEY, "adUnit");

        subject.loadWithSdkInitialized(activity, localExtras, new HashMap<String, String>());

        assertThat(subject.getAdNetworkId()).isEqualTo("adUnit");
    }

    @Test
    public void loadWithSdkInitialized_withNoAdUnitId_shouldUseDefaultAdNetworkId() throws Exception {
        subject.loadWithSdkInitialized(activity, new HashMap<String, Object>(),
                new HashMap<String, String>());

        assertThat(subject.getAdNetworkId()).isEqualTo(
                MoPubRewardedPlayable.MOPUB_REWARDED_PLAYABLE_ID);
    }

    @Test
    public void show_withMraidLoaded_shouldShowRewardedMraidInterstitial() {
        subject.setRewardedMraidInterstitial(mockRewardedMraidInterstitial);
        subject.setIsLoaded(true);

        subject.show();

        verify(mockRewardedMraidInterstitial).showInterstitial();
        verifyNoMoreInteractions(mockRewardedMraidInterstitial);
    }

    @Test
    public void show_withVideoNotLoaded_shouldDoNothing() {
        subject.setRewardedMraidInterstitial(mockRewardedMraidInterstitial);
        subject.setIsLoaded(false);

        subject.show();

        verifyZeroInteractions(mockRewardedMraidInterstitial);
    }

    @Test
    public void show_whenInvalidated_shouldDoNothing() {
        subject.setRewardedMraidInterstitial(mockRewardedMraidInterstitial);
        subject.setIsLoaded(true);
        subject.onInvalidate();

        subject.show();

        verify(mockRewardedMraidInterstitial).onInvalidate();
        verifyNoMoreInteractions(mockRewardedMraidInterstitial);
        assertThat(subject.getRewardedMraidInterstitial()).isNull();
    }
}
