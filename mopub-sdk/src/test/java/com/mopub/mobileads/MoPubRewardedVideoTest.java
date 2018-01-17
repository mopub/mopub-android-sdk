package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.Constants.FOUR_HOURS_MILLIS;
import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubRewardedVideoTest {

    private Activity activity;
    private MoPubRewardedVideo subject;
    private MoPubRewardedAd.MoPubRewardedAdListener listener;

    @Mock private RewardedVastVideoInterstitial mockRewardedVastVideoInterstitial;
    @Mock private Handler mockHandler;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        MoPubRewardedVideoManager.init(activity);

        subject = new MoPubRewardedVideo();
    }

    @Test
    public void onInvalidate_withVastVideoInterstitial_shouldInvalidateVastVideoInterstitial() {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);

        subject.onInvalidate();

        verify(mockRewardedVastVideoInterstitial).onInvalidate();
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
    }

    @Test
    public void onInvalidate_withNullVastVideoInterstitial_shouldNotInvalidateVastVideoInterstitial() {
        subject.setRewardedVastVideoInterstitial(null);

        subject.onInvalidate();

        verifyZeroInteractions(mockRewardedVastVideoInterstitial);
    }

    @Test
    public void loadWithSdkInitialized_withLocalExtrasIncomplete_shouldLoadVastVideoInterstitial() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        subject.loadWithSdkInitialized(activity, new HashMap<String, Object>(),
                new HashMap<String, String>());

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity), any(
                        CustomEventInterstitial.CustomEventInterstitialListener.class),
                eq(new HashMap<String, Object>()),
                eq(new HashMap<String, String>()));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(0);
    }

    @Test
    public void loadWithSdkInitialized_withRewardedVideoCurrencyNameIncorrectType_shouldLoadVastVideoInterstitial_shouldSetCurrencyNameToEmptyString() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, new Object());
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "10");

        subject.loadWithSdkInitialized(activity, localExtras, new HashMap<String, String>());

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity), any(
                        CustomEventInterstitial.CustomEventInterstitialListener.class),
                eq(localExtras),
                eq(new HashMap<String, String>()));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(10);
    }

    @Test
    public void loadWithSdkInitialized_withRewardedVideoCurrencyAmountIncorrectType_shouldLoadVastVideoInterstitial_shouldSetCurrencyAmountToZero() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "currencyName");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, new Object());

        subject.loadWithSdkInitialized(activity, localExtras, new HashMap<String, String>());

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity), any(
                        CustomEventInterstitial.CustomEventInterstitialListener.class),
                eq(localExtras),
                eq(new HashMap<String, String>()));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("currencyName");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(0);
    }

    @Test
    public void loadWithSdkInitialized_withRewardedVideoCurrencyAmountNotInteger_shouldLoadVastVideoInterstitial_shouldSetCurrencyAmountToZero() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "currencyName");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "foo");

        subject.loadWithSdkInitialized(activity, localExtras, new HashMap<String, String>());

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity), any(
                        CustomEventInterstitial.CustomEventInterstitialListener.class),
                eq(localExtras),
                eq(new HashMap<String, String>()));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("currencyName");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(0);
    }

    @Test
    public void loadWithSdkInitialized_withRewardedVideoCurrencyAmountNegative_shouldLoadVastVideoInterstitial_shouldSetCurrencyAmountToZero() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "currencyName");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "-42");

        subject.loadWithSdkInitialized(activity, localExtras, new HashMap<String, String>());

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity), any(
                        CustomEventInterstitial.CustomEventInterstitialListener.class),
                eq(localExtras),
                eq(new HashMap<String, String>()));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("currencyName");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(0);
    }

    @Test
    public void loadWithSdkInitialized_withCorrectLocalExtras_shouldLoadVastVideoInterstitial() throws Exception {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        final Map<String, String> serverExtras = new HashMap<String, String>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "currencyName");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "10");

        subject.loadWithSdkInitialized(activity, localExtras, serverExtras);

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity),
                any(CustomEventInterstitial.CustomEventInterstitialListener.class), eq(localExtras),
                eq(serverExtras));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("currencyName");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(10);
    }

    @Test
    public void loadWithSdkInitialized_withEmptyCurrencyName_withNegativeCurrencyAmount_shouldLoadVastVideoInterstitial_shouldNotChangeCurrencyName_shouldSetCurrencyAmountToZero() throws Exception {
        // We pass whatever was sent to this custom event to the app as long as it exists, but
        // if the currency value is negative, set it to 0
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        final Map<String, Object> localExtras = new HashMap<String, Object>();
        final Map<String, String> serverExtras = new HashMap<String, String>();
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY, "");
        localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY, "-10");

        subject.loadWithSdkInitialized(activity, localExtras, serverExtras);

        verify(mockRewardedVastVideoInterstitial).loadInterstitial(eq(activity),
                any(CustomEventInterstitial.CustomEventInterstitialListener.class), eq(localExtras),
                eq(serverExtras));
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedAdCurrencyName()).isEqualTo("");
        assertThat(subject.getRewardedAdCurrencyAmount()).isEqualTo(0);
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

        assertThat(subject.getAdNetworkId()).isEqualTo(MoPubRewardedVideo.MOPUB_REWARDED_VIDEO_ID);
    }

    @Test
    public void show_withVideoLoaded_shouldShowVastVideoInterstitial() {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        subject.setIsLoaded(true);

        subject.show();

        verify(mockRewardedVastVideoInterstitial).showInterstitial();
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
    }

    @Test
    public void show_withVideoNotLoaded_shouldDoNothing() {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        subject.setIsLoaded(false);

        subject.show();

        verifyZeroInteractions(mockRewardedVastVideoInterstitial);
    }

    @Test
    public void show_whenInvalidated_shouldDoNothing() {
        subject.setRewardedVastVideoInterstitial(mockRewardedVastVideoInterstitial);
        subject.setIsLoaded(true);
        subject.onInvalidate();

        subject.show();

        verify(mockRewardedVastVideoInterstitial).onInvalidate();
        verifyNoMoreInteractions(mockRewardedVastVideoInterstitial);
        assertThat(subject.getRewardedVastVideoInterstitial()).isNull();
    }

    @Test
    public void moPubRewardedAdListener_onInterstitialLoaded_withMoPubRewardedVideo_shouldPostExpirationRunnable() {
        listener = subject.createListener(MoPubRewardedVideo.class);
        listener.setHandler(mockHandler);

        listener.onInterstitialLoaded();

        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
    }

    @Test
    public void moPubRewardedAdListener_onInterstitialLoaded_withMoPubRewardedPlayable_shouldPostExpirationRunnable() {
        listener = subject.createListener(MoPubRewardedPlayable.class);
        listener.setHandler(mockHandler);

        listener.onInterstitialLoaded();

        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
    }

    @Test
    public void moPubRewardedAdListener_onInterstitialLoaded_withOtherCustomEvents_shouldNotPostExpirationRunnable() {
        listener = subject.createListener(MoPubRewardedAd.class);
        listener.setHandler(mockHandler);

        listener.onInterstitialLoaded();

        verifyNoMoreInteractions(mockHandler);
    }

    @Test
    public void moPubRewardedAdListener_onInterstitialFailed_shouldRemoveExpirationRunnable() {
        listener = subject.createListener(MoPubRewardedVideo.class);
        listener.setHandler(mockHandler);

        listener.onInterstitialFailed(EXPIRED);

        verify(mockHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void moPubRewardedAdListener_onInterstitialShown_shouldRemoveExpirationRunnable() {
        listener = subject.createListener(MoPubRewardedVideo.class);
        listener.setHandler(mockHandler);

        listener.onInterstitialShown();

        verify(mockHandler).removeCallbacks(any(Runnable.class));
    }
}
