// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import androidx.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestAdViewControllerFactory;
import com.mopub.mobileads.test.support.TestFullscreenAdAdapterFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mopub.common.Constants.FOUR_HOURS_MILLIS;
import static com.mopub.mobileads.MoPubErrorCode.CANCELLED;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.DESTROYED;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.IDLE;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.LOADING;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.READY;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.SHOWING;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class MoPubInterstitialTest {

    private static final String KEYWORDS_VALUE = "expected_keywords";
    private static final String AD_UNIT_ID_VALUE = "expected_adunitid";
    private static final String SOURCE_VALUE = "expected_source";
    private static final String CLICKTHROUGH_URL_VALUE = "expected_clickthrough_url";
    private Activity activity;
    private MoPubInterstitial subject;
    private Map<String, String> serverExtras;
    private FullscreenAdAdapter fullscreenAdapter;
    private MoPubInterstitial.InterstitialAdListener interstitialAdListener;
    private AdLifecycleListener.InteractionListener interactionListener;
    private AdViewController adViewController;
    private String customEventClassName;
    @Mock private Handler mockHandler;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new MoPubInterstitial(activity, AD_UNIT_ID_VALUE);
        interstitialAdListener = mock(MoPubInterstitial.InterstitialAdListener.class);
        interactionListener = mock(AdLifecycleListener.InteractionListener.class);
        subject.setInterstitialAdListener(interstitialAdListener);
        subject.setHandler(mockHandler);
        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);

        customEventClassName = "class name";
        serverExtras = new HashMap<String, String>();
        serverExtras.put("testExtra", "class data");

        fullscreenAdapter = TestFullscreenAdAdapterFactory.getSingletonMock();
        reset(fullscreenAdapter);
        when(fullscreenAdapter.isAutomaticImpressionAndClickTrackingEnabled())
                .thenReturn(true);
        adViewController = TestAdViewControllerFactory.getSingletonMock();
        subject.setAdViewController(adViewController);
    }

    @Test
    public void forceRefresh_shouldResetInterstitialViewAndMarkNotDestroyed() throws Exception {
        subject.onAdLoaded();
        subject.setCurrentInterstitialState(READY);
        subject.forceRefresh();

        assertThat(subject.isReady()).isFalse();
        assertThat(subject.isDestroyed()).isFalse();
        verify(adViewController).forceRefresh();
    }

    @Test
    public void setUserDataKeywordsTest() throws Exception {
        String userDataKeywords = "these_are_user_data_keywords";

        subject.setUserDataKeywords(userDataKeywords);
        verify(adViewController).setUserDataKeywords(eq(userDataKeywords));
    }

    @Test
    public void setKeywords_withNonEmptyKeywords_shouldsetKeywordsOnInterstitialView() throws Exception {
        String keywords = "these_are_keywords";

        subject.setKeywords(keywords);

        verify(adViewController).setKeywords(eq(keywords));
    }

    @Test
    public void getKeywordsTest_shouldCallGetKeywordsOnInterstitialView() throws Exception {
        subject.getKeywords();

        verify(adViewController).getKeywords();
    }

    @Test
    public void setTestingTest() throws Exception {
        subject.setTesting(true);
        verify(adViewController).setTesting(eq(true));
    }

    @Test
    public void getInterstitialAdListenerTest() throws Exception {
        interstitialAdListener = mock(MoPubInterstitial.InterstitialAdListener.class);
        subject.setInterstitialAdListener(interstitialAdListener);
        assertThat(subject.getInterstitialAdListener()).isSameAs(interstitialAdListener);
    }

    @Test
    public void getTestingTest() throws Exception {
        subject.getTesting();
        verify(adViewController).getTesting();
    }

    @Test
    public void setLocalExtrasTest() throws Exception {
        Map<String,Object> localExtras = new HashMap<String, Object>();
        localExtras.put("guy", new Activity());
        localExtras.put("other guy", new BigDecimal(27f));

        subject.setLocalExtras(localExtras);
        verify(adViewController).setLocalExtras(eq(localExtras));
    }


    @Test
    public void onLoaded_withoutLoad_shouldNotNotifyListener() throws Exception {
        subject.onAdLoaded();
        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 1);

        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim())
                .isEqualTo("[com.mopub.mobileads.MoPubInterstitial][attemptStateTransition] Ad Log " +
                        "- Attempted transition from IDLE to READY failed due to no known load call.");

        verify(adViewController, never()).onAdImpression();
    }

    @Test
    public void onLoaded_withLoad_shouldNotifyListener() throws Exception {
        subject.load();
        subject.onAdLoaded();
        verify(interstitialAdListener).onInterstitialLoaded(eq(subject));

        verify(adViewController, never()).onAdImpression();
    }

    @Test
    public void onLoaded_whenInterstitialAdListenerIsNull_shouldNotNotifyListenerOrTrackImpression() throws Exception {
        subject.setInterstitialAdListener(null);

        subject.onAdLoaded();

        verify(adViewController, never()).onAdImpression();
        verify(interstitialAdListener, never()).onInterstitialLoaded(eq(subject));
    }

    @Test
    public void onFailed_shouldLoadFailUrl() throws Exception {
        subject.onAdFailed(INTERNAL_ERROR);

        verify(adViewController).loadFailUrl(INTERNAL_ERROR);
    }

    @Test
    public void onShown_shouldTrackImpressionAndNotifyListener() throws Exception {
        subject.onAdShown();

        verify(interstitialAdListener).onInterstitialShown(eq(subject));
    }

    @Test
    public void onShown_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);
        subject.onAdShown();
        verify(interstitialAdListener, never()).onInterstitialShown(eq(subject));
    }

    @Test
    public void onClicked_shouldRegisterClickAndNotifyListener() throws Exception {
        subject.onAdClicked();

        verify(adViewController).registerClick();
        verify(interstitialAdListener).onInterstitialClicked(eq(subject));
    }

    @Test
    public void onClicked_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);

        subject.onAdClicked();

        verify(interstitialAdListener, never()).onInterstitialClicked(eq(subject));
    }

    @Test
    public void onImpression_whenAutomaticImpressionTrackingIsEnabled_shouldDoNothing() {
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        when(fullscreenAdapter.isAutomaticImpressionAndClickTrackingEnabled())
                .thenReturn(true);

        subject.onAdImpression();

        verify(adViewController, never()).onAdImpression();
    }

    @Test
    public void onDismissed_shouldNotifyListener() throws Exception {
        subject.onAdDismissed();

        verify(interstitialAdListener).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void onDismissed_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);
        subject.onAdDismissed();
        verify(interstitialAdListener, never()).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialLoadedNotification() throws Exception {
        subject.destroy();

        subject.onAdLoaded();

        verify(interstitialAdListener, never()).onInterstitialLoaded(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialFailedNotification() throws Exception {
        subject.destroy();

        subject.onAdFailed(UNSPECIFIED);

        verify(adViewController, never()).loadFailUrl(UNSPECIFIED);
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialClickedFromRegisteringClick() throws Exception {
        subject.destroy();

        subject.onAdClicked();

        verify(adViewController, never()).registerClick();
    }

    @Test
    public void destroy_shouldPreventOnCustomEventShownNotification() throws Exception {
        subject.destroy();

        subject.onAdShown();

        verify(interstitialAdListener, never()).onInterstitialShown(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialDismissedNotification() throws Exception {
        subject.destroy();

        subject.onAdDismissed();

        verify(interstitialAdListener, never()).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void newlyCreated_shouldNotBeReadyAndNotShow() throws Exception {
        assertShowsFullscreenAd(false);
    }

    @Test
    public void failingCustomEventInterstitial_shouldNotBecomeReadyToShowCustomEventAd() throws Exception {
        subject.onAdLoaded();
        subject.onAdFailed(CANCELLED);

        assertShowsFullscreenAd(false);
    }

    @Test
    public void dismissingCustomEventInterstitial_shouldNotBecomeReadyToShowCustomEventAd() throws Exception {
        subject.onAdLoaded();
        subject.onAdDismissed();

        assertShowsFullscreenAd(false);
    }

    @Test
    public void adFailed_shouldNotifyInterstitialAdListener() throws Exception {
        subject.onAdFailed(CANCELLED);

        verify(interstitialAdListener).onInterstitialFailed(eq(subject), eq(CANCELLED));
    }

    @Test
    public void attemptStateTransition_withIdleStartState() {
        /**
         * IDLE can go to LOADING when load or forceRefresh is called. IDLE can also go to
         * DESTROYED if the interstitial view is destroyed.
         */

        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(IDLE);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verify(fullscreenAdapter).invalidate();
        verify(adViewController).loadAd();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verify(fullscreenAdapter).invalidate();
        verify(adViewController).forceRefresh();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withLoadingStartState() {
        /**
         * LOADING can go to IDLE if a force refresh happens. LOADING can also go into IDLE if an
         * ad failed to load. LOADING should go to READY when the interstitial is done loading.
         * LOADING can go to DESTROYED if the interstitial view is destroyed.
         */

        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(LOADING);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        when(adViewController.getBaseAdClassName())
                .thenReturn(AdTypeTranslator.BaseAdType.MOPUB_FULLSCREEN.toString());
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withReadyStartState() {
        /**
         * This state should succeed for going to SHOWING. It is also possible to force refresh from
         * here into IDLE. Also, READY can go into DESTROYED.
         */

        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(READY);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);
        verify(interstitialAdListener).onInterstitialLoaded(subject);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);
        verify(interstitialAdListener).onInterstitialLoaded(subject);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        reset(mockHandler);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        reset(mockHandler);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withShowingStartState() {
        /**
         * When the interstitial is dismissed, this should transition to IDLE. Otherwise, block
         * other transitions except to DESTROYED. You cannot force refresh while an interstitial
         * is showing.
         */
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(SHOWING);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(fullscreenAdapter).invalidate();
    }
    @Test
    public void attemptStateTransition_withDestroyedStartState() {
        // All state transitions should fail if starting from a destroyed state
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(DESTROYED);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(fullscreenAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
    }

    @Test
    public void attemptStateTransition_withLoadingStartState_withReadyEndState_withMoPubCustomEvent_shouldExpireAd() {
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(LOADING);
        when(adViewController.getBaseAdClassName())
                .thenReturn(AdTypeTranslator.BaseAdType.MOPUB_FULLSCREEN.toString());
        subject.attemptStateTransition(READY, false);
        reset(mockHandler);

        resetMoPubInterstitial(LOADING);
        when(adViewController.getBaseAdClassName())
                .thenReturn(AdTypeTranslator.BaseAdType.MOPUB_FULLSCREEN.toString());
        subject.attemptStateTransition(READY, false);
        reset(mockHandler);

        resetMoPubInterstitial(LOADING);
        when(adViewController.getBaseAdClassName())
                .thenReturn(AdTypeTranslator.BaseAdType.MOPUB_FULLSCREEN.toString());
        subject.attemptStateTransition(READY, false);
    }

    @Test
    public void attemptStateTransition_withLoadingStartState_withReadyEndState_withNonMoPubCustomEvent_shouldNotExpireAd() {
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(LOADING);
        when(adViewController.getBaseAdClassName()).thenReturn("thirdPartyAd");
        subject.attemptStateTransition(READY, false);
        verifyZeroInteractions(mockHandler);
    }

    private void assertShowsFullscreenAd(boolean shouldBeReady) {
        subject.load();

        assertThat(subject.isReady()).isEqualTo(shouldBeReady);
        assertThat(subject.show()).isEqualTo(shouldBeReady);

        if (shouldBeReady) {
            verify(fullscreenAdapter).show(subject);
        } else {
            verify(fullscreenAdapter, never()).show(subject);
        }
    }

    private void resetMoPubInterstitial(
            @NonNull final MoPubInterstitial.InterstitialState interstitialState) {
        reset(fullscreenAdapter, interstitialAdListener);
        subject.setFullscreenAdAdapter(fullscreenAdapter);
        subject.setCurrentInterstitialState(interstitialState);
    }
}
