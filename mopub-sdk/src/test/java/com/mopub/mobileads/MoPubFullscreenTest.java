// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.CacheServiceTest;
import com.mopub.common.DataKeys;
import com.mopub.common.FullAdType;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.test.support.TestVastManagerFactory;
import com.mopub.mobileads.test.support.VastUtils;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.httpclient.FakeHttp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.Constants.FOUR_HOURS_MILLIS;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_SHOW;
import static com.mopub.common.IntentActions.ACTION_REWARDED_AD_COMPLETE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.withSettings;

@RunWith(SdkTestRunner.class)
public class MoPubFullscreenTest {
    private static final String EXPECTED_HTML_DATA = "<html> html </html>";
    private static final String EXPECTED_VAST_DATA = "<VAST></VAST>";
    private static final String DSP_CREATIVE_ID = "dsp_creative_id";
    private static final String AD_UNIT = "ad_unit";
    private static final int REWARDED_DURATION_S = 22;
    private static final String CURRENCY_NAME = "currency_name";
    private static final int CURRENCY_AMOUNT = 10;
    private long broadcastIdentifier;
    private Activity context;
    private AdData adData;

    private MoPubFullscreen subject;

    private VastManager mockVastManager;
    private MoPubAd moPubAd;
    @Mock
    private AdLifecycleListener.LoadListener loadListener;
    @Mock
    private AdLifecycleListener.InteractionListener interactionListener;
    @Mock
    private Handler mockHandler;

    @Before
    public void setUp() throws Exception {
        broadcastIdentifier = 15243;
        mockVastManager = TestVastManagerFactory.getSingletonMock();
        moPubAd = mock(MoPubAd.class);

        final Map<String, String> serverExtras = new HashMap<>();
        serverExtras.put(HTML_RESPONSE_BODY_KEY, EXPECTED_HTML_DATA);
        adData = new AdData.Builder()
                .extras(serverExtras)
                .broadcastIdentifier(broadcastIdentifier)
                .dspCreativeId(DSP_CREATIVE_ID)
                .adType("mraid")
                .build();

        context = Robolectric.buildActivity(Activity.class).create().get();

        subject = new MoPubFullscreen();
        subject.setVastManager(mockVastManager);
        subject.mLoadListener = loadListener;
        subject.mInteractionListener = interactionListener;
    }

    @Test
    public void show_shouldConnectListenerToBroadcastReceiver() throws Exception {
        subject.internalLoad(context, loadListener, adData);
        subject.markReady();
        subject.show();

        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener).onAdShown();
        verify(interactionListener).onAdImpression();
        verifyNoMoreInteractions(interactionListener);
        Mockito.reset(interactionListener);

        intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_CLICK, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener).onAdClicked();
        verifyNoMoreInteractions(interactionListener);
        Mockito.reset(interactionListener);

        intent = getIntentForActionAndIdentifier(ACTION_REWARDED_AD_COMPLETE,
                broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener).onAdComplete(null);
        verifyNoMoreInteractions(interactionListener);
        Mockito.reset(interactionListener);

        intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_DISMISS, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener).onAdDismissed();
    }


    @Test
    public void onInvalidate_shouldDisconnectListenerToBroadcastReceiver() throws Exception {
        subject.internalLoad(context, loadListener, adData);
        subject.onInvalidate();

        Intent intent;
        intent = new Intent(ACTION_FULLSCREEN_SHOW);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener, never()).onAdShown();

        intent = new Intent(ACTION_REWARDED_AD_COMPLETE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener, never()).onAdComplete(null);

        intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_CLICK, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener, never()).onAdClicked();

        intent = new Intent(ACTION_FULLSCREEN_DISMISS);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener, never()).onAdDismissed();
    }

    @Test
    public void show_shouldStartActivityWithIntent() throws Exception {
        subject.internalLoad(context, loadListener, adData);
        subject.setReady(true);

        subject.show();

        ShadowActivity shadowActivity = Shadows.shadowOf(context);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;
        assertThat(intent.getComponent().getClassName())
                .isEqualTo("com.mopub.mobileads.MoPubFullscreenActivity");
        assertThat((Object) intent.getParcelableExtra("com_mopub_ad_data")).isNotNull();
    }

    @Test
    public void load_withDefaultAdData_shouldUseDefaultData() throws Exception {
        subject.internalLoad(context, loadListener, adData);

        AdData adData = subject.mAdData;
        assertThat(adData.getCurrencyAmount()).isEqualTo(0);
        assertThat(adData.getCurrencyName()).isNull();
        assertThat(adData.getRewardedDurationSeconds()).isEqualTo(30);
    }

    @Test
    public void load_withRewardedFields_shouldSetRewardedFields() throws Exception {
        adData.setRewarded(true);
        adData.setRewardedDurationSeconds(REWARDED_DURATION_S);
        adData.setCurrencyName(CURRENCY_NAME);
        adData.setCurrencyAmount(CURRENCY_AMOUNT);
        subject.internalLoad(context, loadListener, adData);

        AdData result = subject.mAdData;
        assertThat(result.isRewarded()).isTrue();
        assertThat(result.getRewardedDurationSeconds()).isEqualTo(REWARDED_DURATION_S);
        assertThat(result.getCurrencyName()).isEqualTo(CURRENCY_NAME);
        assertThat(result.getCurrencyAmount()).isEqualTo(CURRENCY_AMOUNT);
    }

    @Test
    public void preRenderHtml_whenCreatingVideoCache_butItHasInitializationErrors_shouldSignalOnInterstitialFailedOnError() throws Exception {
        // context is null when load is not called, which causes DiskLruCache to not be created

        subject.preRender();

        verify(loadListener).onAdLoadFailed(eq(MoPubErrorCode.VIDEO_CACHE_ERROR));
        verify(mockVastManager, never()).prepareVastVideoConfiguration(anyString(),
                any(VastManager.VastManagerListener.class), any(String.class), any(Context.class));
    }

    @Test
    public void load_shouldInitializeDiskCache() throws Exception {
        FakeHttp.addPendingHttpResponse(mock(HttpResponse.class));

        CacheServiceTest.assertDiskCacheIsUninitialized();
        subject.internalLoad(context, loadListener, adData);
        CacheServiceTest.assertDiskCacheIsEmpty();
    }

    @Test
    public void load_withFullAdTypeVast_shouldCreateVastManagerAndProcessVast() throws Exception {
        loadVast();

        verify(mockVastManager).prepareVastVideoConfiguration(eq(EXPECTED_VAST_DATA),
                eq(subject), eq(DSP_CREATIVE_ID), eq(context));
    }

    @Test
    public void show_shouldStartVideoPlayerActivityWithAllValidTrackers() throws Exception {
        VastCompanionAdConfig vastCompanionAdConfig = mock(VastCompanionAdConfig.class, withSettings().serializable());
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker.Builder
                ("start", 2000).build()));
        vastVideoConfig.addFractionalTrackers(Arrays.asList(
                new VastFractionalProgressTracker.Builder("first", 0.25f).build(),
                new VastFractionalProgressTracker.Builder("mid", 0.5f).build(),
                new VastFractionalProgressTracker.Builder("third", 0.75f).build()));
        vastVideoConfig.addCompleteTrackers(VastUtils.stringsToVastTrackers("complete"));
        vastVideoConfig.addImpressionTrackers(VastUtils.stringsToVastTrackers("imp"));
        vastVideoConfig.setClickThroughUrl("clickThrough");
        vastVideoConfig.addClickTrackers(VastUtils.stringsToVastTrackers("click"));
        vastVideoConfig.setVastCompanionAd(vastCompanionAdConfig, vastCompanionAdConfig);
        loadVast();
        subject.onVastVideoConfigurationPrepared(vastVideoConfig);

        subject.show();

        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubFullscreenActivity.class.getCanonicalName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), Intent.FLAG_ACTIVITY_NEW_TASK)).isTrue();
        AdData adDataFromActivity = intent.getParcelableExtra(DataKeys.AD_DATA_KEY);
        assertThat(adData).isEqualsToByComparingFields(adDataFromActivity);
        assertThat(adDataFromActivity.isRewarded()).isFalse();
    }

    @Test
    public void onInvalidate_shouldCancelVastManager() throws Exception {
        subject.onInvalidate();

        verify(mockVastManager).cancel();
    }

    @Test
    public void onInvalidate_whenVastManagerIsNull_shouldNotBlowUp() throws Exception {
        loadVast();

        subject.setVastManager(null);

        subject.onInvalidate();

        // pass
    }

    @Test
    public void onInvalidate_whenFirstCall_shouldDoNothing() {
        subject.onInvalidate();

        // pass
    }

    @Test
    public void onInvalidate_shouldClearRunnable() throws Exception {
        adData.setRewarded(true);
        subject.internalLoad(context, loadListener, adData);
        subject.setHandler(mockHandler);
        final VastVideoConfig mockVastVideoConfig = mock(VastVideoConfig.class);
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        subject.onInvalidate();

        verify(mockHandler).removeCallbacks(any(Runnable.class));
    }

    @Test
    public void onVastVideoConfigurationPrepared_withVastVideoConfiguration_shouldSignalOnAdLoaded() throws Exception {
        loadVast();

        subject.onVastVideoConfigurationPrepared(mock(VastVideoConfig.class));

        verify(loadListener).onAdLoaded();
    }

    @Test
    public void onVastVideoConfigurationPrepared_withNullVastVideoConfiguration_shouldSignalOnAdFailed() throws Exception {
        loadVast();

        subject.onVastVideoConfigurationPrepared(null);

        verify(loadListener).onAdLoadFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
    }

    @Test
    public void onVastVideoConfigurationPrepared_withProperVastConfig_withRewardedFlag_shouldSetRewardedVideoFlag() throws Exception {
        adData.setRewarded(true);
        subject.internalLoad(context, loadListener, adData);
        final VastVideoConfig mockVastVideoConfig = mock(VastVideoConfig.class);

        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).setRewarded(true);
    }

    @Test
    public void load_withAdUnitId_shouldSetAdNetworkId() throws Exception {
        adData.setAdUnit(AD_UNIT);
        subject.internalLoad(context, loadListener, adData);

        assertThat(subject.getAdNetworkId()).isEqualTo(AD_UNIT);
    }

    @Test
    public void load_withNoAdUnitId_shouldUseDefaultAdNetworkId() throws Exception {
        subject.internalLoad(context, loadListener, adData);

        assertThat(subject.getAdNetworkId()).isEqualTo("com.mopub.mobileads.MoPubFullscreen");
    }

    @Test
    public void show_withVideoNotLoaded_shouldDoNothing() {
        subject.show();

        ShadowActivity shadowActivity = Shadows.shadowOf(context);
        ShadowActivity.IntentForResult intentForResult = shadowActivity.getNextStartedActivityForResult();
        assertThat(intentForResult).isNull();
    }

    @Test
    public void show_whenInvalidated_shouldDoNothing() throws Exception {
        subject.internalLoad(context, loadListener, adData);
        subject.onInvalidate();

        subject.show();

        ShadowActivity shadowActivity = Shadows.shadowOf(context);
        ShadowActivity.IntentForResult intentForResult = shadowActivity.getNextStartedActivityForResult();
        assertThat(intentForResult).isNull();
    }

    @Test
    public void markReady_withRewardedTrue_shouldPostExpirationRunnable() throws Exception {
        adData.setRewarded(true);
        subject.internalLoad(context, loadListener, adData);
        subject.setHandler(mockHandler);

        subject.markReady();

        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
    }

    @Test
    public void markReady_withRewardedFalse_shouldNotPostExpirationRunnable() throws Exception {
        subject.internalLoad(context, loadListener, adData);
        subject.setHandler(mockHandler);

        subject.markReady();

        verifyZeroInteractions(mockHandler);
    }

    @Test
    public void markNotReady_withRewardedTrue_shouldClearRunnable() throws Exception {
        adData.setRewarded(true);
        subject.internalLoad(context, loadListener, adData);
        subject.setHandler(mockHandler);

        subject.markNotReady();

        verify(mockHandler).removeCallbacks(any(Runnable.class));
    }

     @Test
     public void onVastVideoConfigurationPrepared_withRewardedTrue_shouldPostExpirationRunnable() throws Exception {
         VastVideoConfig vastVideoConfig = new VastVideoConfig();
         adData.setRewarded(true);
         loadVast();
         subject.setHandler(mockHandler);

         subject.onVastVideoConfigurationPrepared(vastVideoConfig);

         verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
     }

    private void loadVast() throws Exception {
        adData.setAdPayload(EXPECTED_VAST_DATA);
        adData.setFullAdType(FullAdType.VAST);
        subject.internalLoad(context, loadListener, adData);
    }
}
