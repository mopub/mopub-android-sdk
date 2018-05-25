package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestAdViewControllerFactory;
import com.mopub.mobileads.test.support.TestCustomEventInterstitialAdapterFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.Constants.FOUR_HOURS_MILLIS;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_DATA;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubInterstitialTest {

    private static final String KEYWORDS_VALUE = "expected_keywords";
    private static final String AD_UNIT_ID_VALUE = "expected_adunitid";
    private static final String SOURCE_VALUE = "expected_source";
    private static final String CLICKTHROUGH_URL_VALUE = "expected_clickthrough_url";
    private Activity activity;
    private MoPubInterstitial subject;
    private Map<String, String> serverExtras;
    private CustomEventInterstitialAdapter customEventInterstitialAdapter;
    private MoPubInterstitial.InterstitialAdListener interstitialAdListener;
    private MoPubInterstitial.MoPubInterstitialView interstitialView;
    private AdViewController adViewController;
    private String customEventClassName;
    @Mock private Handler mockHandler;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new MoPubInterstitial(activity, AD_UNIT_ID_VALUE);
        interstitialAdListener = mock(MoPubInterstitial.InterstitialAdListener.class);
        subject.setInterstitialAdListener(interstitialAdListener);
        subject.setHandler(mockHandler);

        interstitialView = mock(MoPubInterstitial.MoPubInterstitialView.class);

        customEventClassName = "class name";
        serverExtras = new HashMap<String, String>();
        serverExtras.put("testExtra", "class data");

        customEventInterstitialAdapter = TestCustomEventInterstitialAdapterFactory.getSingletonMock();
        reset(customEventInterstitialAdapter);
        adViewController = TestAdViewControllerFactory.getSingletonMock();
    }

    @Test
    public void forceRefresh_shouldResetInterstitialViewAndMarkNotDestroyed() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.onCustomEventInterstitialLoaded();
        subject.setCurrentInterstitialState(READY);
        subject.forceRefresh();

        assertThat(subject.isReady()).isFalse();
        assertThat(subject.isDestroyed()).isFalse();
        verify(interstitialView).forceRefresh();
    }

    @Test
    public void setUserDataKeywordsTest() throws Exception {
        subject.setInterstitialView(interstitialView);
        String userDataKeywords = "these_are_user_data_keywords";

        subject.setUserDataKeywords(userDataKeywords);
        verify(interstitialView).setUserDataKeywords(eq(userDataKeywords));
    }

    @Test
    public void getUserDataKeywords() throws Exception {
        subject.setInterstitialView(interstitialView);

        subject.getUserDataKeywords();
        verify(interstitialView).getUserDataKeywords();
    }

    @Test
    public void setKeywords_withNonEmptyKeywords_shouldsetKeywordsOnInterstitialView() throws Exception {
        subject.setInterstitialView(interstitialView);
        String keywords = "these_are_keywords";

        subject.setKeywords(keywords);

        verify(interstitialView).setKeywords(eq(keywords));
    }

    @Test
    public void getKeywordsTest_shouldCallGetKeywordsOnInterstitialView() throws Exception {
        subject.setInterstitialView(interstitialView);

        subject.getKeywords();

        verify(interstitialView).getKeywords();
    }

    @Test
    public void setTestingTest() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.setTesting(true);
        verify(interstitialView).setTesting(eq(true));
    }

    @Test
    public void getInterstitialAdListenerTest() throws Exception {
        interstitialAdListener = mock(MoPubInterstitial.InterstitialAdListener.class);
        subject.setInterstitialAdListener(interstitialAdListener);
        assertThat(subject.getInterstitialAdListener()).isSameAs(interstitialAdListener);
    }

    @Test
    public void getTestingTest() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.getTesting();
        verify(interstitialView).getTesting();
    }

    @Test
    public void setLocalExtrasTest() throws Exception {
        subject.setInterstitialView(interstitialView);

        Map<String,Object> localExtras = new HashMap<String, Object>();
        localExtras.put("guy", new Activity());
        localExtras.put("other guy", new BigDecimal(27f));

        subject.setLocalExtras(localExtras);
        verify(interstitialView).setLocalExtras(eq(localExtras));
    }

    @Test
    public void loadCustomEvent_shouldCreateAndLoadCustomEventInterstitialAdapter() throws Exception {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);
        moPubInterstitialView.loadCustomEvent(customEventClassName, serverExtras);

        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestMoPubInterstitial()).isSameAs(subject);
        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestClassName()).isEqualTo("class name");
        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestServerExtras().get("testExtra")).isEqualTo("class data");
    }

    @Test
    public void onCustomEventInterstitialLoaded_shouldNotifyListener() throws Exception {
        subject.setInterstitialView(interstitialView);

        subject.onCustomEventInterstitialLoaded();
        verify(interstitialAdListener).onInterstitialLoaded(eq(subject));

        verify(interstitialView, never()).trackImpression();
    }

    @Test
    public void onCustomEventInterstitialLoaded_whenInterstitialAdListenerIsNull_shouldNotNotifyListenerOrTrackImpression() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.setInterstitialAdListener(null);

        subject.onCustomEventInterstitialLoaded();

        verify(interstitialView, never()).trackImpression();
        verify(interstitialAdListener, never()).onInterstitialLoaded(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialFailed_shouldLoadFailUrl() throws Exception {
        subject.setInterstitialView(interstitialView);

        subject.onCustomEventInterstitialFailed(INTERNAL_ERROR);

        verify(interstitialView).loadFailUrl(INTERNAL_ERROR);
    }

    @Test
    public void onCustomEventInterstitialShown_shouldTrackImpressionAndNotifyListener() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.onCustomEventInterstitialShown();

        verify(interstitialView).trackImpression();
        verify(interstitialAdListener).onInterstitialShown(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialShown_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);
        subject.onCustomEventInterstitialShown();
        verify(interstitialAdListener, never()).onInterstitialShown(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialClicked_shouldRegisterClickAndNotifyListener() throws Exception {
        subject.setInterstitialView(interstitialView);

        subject.onCustomEventInterstitialClicked();

        verify(interstitialView).registerClick();
        verify(interstitialAdListener).onInterstitialClicked(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialClicked_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);

        subject.onCustomEventInterstitialClicked();

        verify(interstitialAdListener, never()).onInterstitialClicked(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialDismissed_shouldNotifyListener() throws Exception {
        subject.onCustomEventInterstitialDismissed();

        verify(interstitialAdListener).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void onCustomEventInterstitialDismissed_whenInterstitialAdListenerIsNull_shouldNotNotifyListener() throws Exception {
        subject.setInterstitialAdListener(null);
        subject.onCustomEventInterstitialDismissed();
        verify(interstitialAdListener, never()).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialLoadedNotification() throws Exception {
        subject.destroy();

        subject.onCustomEventInterstitialLoaded();

        verify(interstitialAdListener, never()).onInterstitialLoaded(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialFailedNotification() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.destroy();

        subject.onCustomEventInterstitialFailed(UNSPECIFIED);

        verify(interstitialView, never()).loadFailUrl(UNSPECIFIED);
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialClickedFromRegisteringClick() throws Exception {
        subject.setInterstitialView(interstitialView);
        subject.destroy();

        subject.onCustomEventInterstitialClicked();

        verify(interstitialView, never()).registerClick();
    }

    @Test
    public void destroy_shouldPreventOnCustomEventShownNotification() throws Exception {
        subject.destroy();

        subject.onCustomEventInterstitialShown();

        verify(interstitialAdListener, never()).onInterstitialShown(eq(subject));
    }

    @Test
    public void destroy_shouldPreventOnCustomEventInterstitialDismissedNotification() throws Exception {
        subject.destroy();

        subject.onCustomEventInterstitialDismissed();

        verify(interstitialAdListener, never()).onInterstitialDismissed(eq(subject));
    }

    @Test
    public void newlyCreated_shouldNotBeReadyAndNotShow() throws Exception {
        assertShowsCustomEventInterstitial(false);
    }

    @Test
    public void loadingCustomEventInterstitial_shouldBecomeReadyToShowCustomEventAd() throws Exception {
        subject.load();
        subject.onCustomEventInterstitialLoaded();

        assertShowsCustomEventInterstitial(true);
    }

    @Ignore("pending")
    @Test
    public void dismissingHtmlInterstitial_shouldNotBecomeReadyToShowHtmlAd() throws Exception {
//        EventForwardingBroadcastReceiver broadcastReceiver = new EventForwardingBroadcastReceiver(subject.mInterstitialAdListener);
//
//        subject.onCustomEventInterstitialLoaded();
//        broadcastReceiver.onHtmlInterstitialDismissed();
//
//        assertShowsCustomEventInterstitial(false);
    }

    @Test
    public void failingCustomEventInterstitial_shouldNotBecomeReadyToShowCustomEventAd() throws Exception {
        subject.onCustomEventInterstitialLoaded();
        subject.onCustomEventInterstitialFailed(CANCELLED);

        assertShowsCustomEventInterstitial(false);
    }

    @Test
    public void dismissingCustomEventInterstitial_shouldNotBecomeReadyToShowCustomEventAd() throws Exception {
        subject.onCustomEventInterstitialLoaded();
        subject.onCustomEventInterstitialDismissed();

        assertShowsCustomEventInterstitial(false);
    }

    @Test
    public void loadCustomEvent_shouldInitializeCustomEventInterstitialAdapter() throws Exception {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);

        serverExtras.put("testExtra", "data");
        moPubInterstitialView.loadCustomEvent("name", serverExtras);

        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestMoPubInterstitial()).isEqualTo(subject);
        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestClassName()).isEqualTo("name");
        assertThat(TestCustomEventInterstitialAdapterFactory.getLatestServerExtras().get("testExtra")).isEqualTo("data");

        verify(customEventInterstitialAdapter).setAdapterListener(eq(subject));
        verify(customEventInterstitialAdapter).loadInterstitial();
    }

    @Test
    public void loadCustomEvent_whenParamsMapIsNull_shouldCallLoadFailUrl() throws Exception {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);

        moPubInterstitialView.loadCustomEvent(null, null);

        verify(adViewController).loadFailUrl(eq(ADAPTER_NOT_FOUND));
        verify(customEventInterstitialAdapter, never()).invalidate();
        verify(customEventInterstitialAdapter, never()).loadInterstitial();
    }

    @Test
    public void adFailed_shouldNotifyInterstitialAdListener() throws Exception {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);
        moPubInterstitialView.adFailed(CANCELLED);

        verify(interstitialAdListener).onInterstitialFailed(eq(subject), eq(CANCELLED));
    }

    @Test
    public void attemptStateTransition_withIdleStartState() {
        /**
         * IDLE can go to LOADING when load or forceRefresh is called. IDLE can also go to
         * DESTROYED if the interstitial view is destroyed.
         */

        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(IDLE);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verify(customEventInterstitialAdapter).invalidate();
        verify(interstitialView).loadAd();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verify(customEventInterstitialAdapter).invalidate();
        verify(interstitialView).forceRefresh();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(IDLE);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withLoadingStartState() {
        /**
         * LOADING can go to IDLE if a force refresh happens. LOADING can also go into IDLE if an
         * ad failed to load. LOADING should go to READY when the interstitial is done loading.
         * LOADING can go to DESTROYED if the interstitial view is destroyed.
         */

        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(LOADING);
        subject.setInterstitialView(interstitialView);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(LOADING);
        when(interstitialView.getCustomEventClassName())
                .thenReturn(AdTypeTranslator.CustomEventType.HTML_INTERSTITIAL.toString());
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);
        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(LOADING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(LOADING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withReadyStartState() {
        /**
         * This state should succeed for going to SHOWING. It is also possible to force refresh from
         * here into IDLE. Also, READY can go into DESTROYED.
         */

        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(READY);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);
        verify(interstitialAdListener).onInterstitialLoaded(subject);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);
        verify(interstitialAdListener).onInterstitialLoaded(subject);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(READY);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verify(customEventInterstitialAdapter).showInterstitial();
        verify(mockHandler).removeCallbacks(any(Runnable.class));
        reset(mockHandler);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verify(customEventInterstitialAdapter).showInterstitial();
        verify(mockHandler).removeCallbacks(any(Runnable.class));
        reset(mockHandler);

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();
        verify(mockHandler).removeCallbacks(any(Runnable.class));

        resetMoPubInterstitial(READY);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();
    }

    @Test
    public void attemptStateTransition_withShowingStartState() {
        /**
         * When the interstitial is dismissed, this should transition to IDLE. Otherwise, block
         * other transitions except to DESTROYED. You cannot force refresh while an interstitial
         * is showing.
         */
        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(SHOWING);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(IDLE);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(SHOWING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(SHOWING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(SHOWING);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();

        resetMoPubInterstitial(SHOWING);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isTrue();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verify(customEventInterstitialAdapter).invalidate();
    }
    @Test
    public void attemptStateTransition_withDestroyedStartState() {
        // All state transitions should fail if starting from a destroyed state
        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(DESTROYED);
        boolean stateDidChange = subject.attemptStateTransition(IDLE, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(IDLE, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(READY, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(READY, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(LOADING, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(DESTROYED, false);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
        verifyZeroInteractions(customEventInterstitialAdapter);

        resetMoPubInterstitial(DESTROYED);
        stateDidChange = subject.attemptStateTransition(DESTROYED, true);
        assertThat(stateDidChange).isFalse();
        assertThat(subject.getCurrentInterstitialState()).isEqualTo(DESTROYED);
    }

    @Test
    public void attemptStateTransition_withLoadingStartState_withReadyEndState_withMoPubCustomEvent_shouldExpireAd() {
        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(LOADING);
        subject.setInterstitialView(interstitialView);

        when(interstitialView.getCustomEventClassName())
                .thenReturn(AdTypeTranslator.CustomEventType.MRAID_INTERSTITIAL.toString());
        subject.attemptStateTransition(READY, false);
        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
        reset(mockHandler);

        resetMoPubInterstitial(LOADING);
        when(interstitialView.getCustomEventClassName())
                .thenReturn(AdTypeTranslator.CustomEventType.HTML_INTERSTITIAL.toString());
        subject.attemptStateTransition(READY, false);
        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
        reset(mockHandler);

        resetMoPubInterstitial(LOADING);
        when(interstitialView.getCustomEventClassName())
                .thenReturn(AdTypeTranslator.CustomEventType.VAST_VIDEO_INTERSTITIAL.toString());
        subject.attemptStateTransition(READY, false);
        verify(mockHandler).postDelayed(any(Runnable.class), eq((long) FOUR_HOURS_MILLIS));
    }

    @Test
    public void attemptStateTransition_withLoadingStartState_withReadyEndState_withNonMoPubCustomEvent_shouldNotExpireAd() {
        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setCurrentInterstitialState(LOADING);
        subject.setInterstitialView(interstitialView);

        when(interstitialView.getCustomEventClassName()).thenReturn("thirdPartyAd");
        subject.attemptStateTransition(READY, false);
        verifyZeroInteractions(mockHandler);
    }

    private void loadCustomEvent() {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);

        serverExtras.put(CUSTOM_EVENT_DATA.getKey(), "data");
        moPubInterstitialView.loadCustomEvent("name", serverExtras);
    }

    private void assertShowsCustomEventInterstitial(boolean shouldBeReady) {
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = subject.new MoPubInterstitialView(activity);
        moPubInterstitialView.loadCustomEvent(customEventClassName, serverExtras);

        assertThat(subject.isReady()).isEqualTo(shouldBeReady);
        assertThat(subject.show()).isEqualTo(shouldBeReady);

        if (shouldBeReady) {
            verify(customEventInterstitialAdapter).showInterstitial();
        } else {
            verify(customEventInterstitialAdapter, never()).showInterstitial();
        }
    }

    private void resetMoPubInterstitial(
            @NonNull final MoPubInterstitial.InterstitialState interstitialState) {
        reset(customEventInterstitialAdapter, interstitialAdListener, interstitialView);
        subject.setCustomEventInterstitialAdapter(customEventInterstitialAdapter);
        subject.setInterstitialView(interstitialView);
        subject.setCurrentInterstitialState(interstitialState);
    }
}
