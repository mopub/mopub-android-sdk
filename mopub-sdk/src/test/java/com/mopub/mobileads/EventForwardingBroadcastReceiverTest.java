package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.CustomEventInterstitialAdapterFactory;

import org.fest.util.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class EventForwardingBroadcastReceiverTest {

    private CustomEventInterstitialListener customEventInterstitialListener;
    private EventForwardingBroadcastReceiver subject;
    private Activity context;
    private int broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        broadcastIdentifier = 27027027;
        subject = new EventForwardingBroadcastReceiver(customEventInterstitialListener, broadcastIdentifier);
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Ignore("Difficult with the number of test factories and mocking involved.")
    @Test
    public void twoDifferentInterstitials_shouldNotHearEachOthersBroadcasts() throws Exception {
        final MoPubInterstitial interstitialA = new MoPubInterstitial(context, "adunitid");
        final InterstitialAdListener listenerA = mock(InterstitialAdListener.class);
        interstitialA.setInterstitialAdListener(listenerA);

        final MoPubInterstitial interstitialB = new MoPubInterstitial(context, "adunitid");
        final InterstitialAdListener listenerB = mock(InterstitialAdListener.class);
        interstitialB.setInterstitialAdListener(listenerB);

        Map<String, String> serverExtras = new HashMap<String, String>();
        serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, "response");
        final CustomEventInterstitialAdapter customEventInterstitialAdapter =
                CustomEventInterstitialAdapterFactory.create(
                        interstitialA,
                        "com.mopub.mobileads.HtmlInterstitial",
                        serverExtras, broadcastIdentifier, null);


        customEventInterstitialAdapter.loadInterstitial();
        verify(listenerA).onInterstitialLoaded(interstitialA);
        verify(listenerB, never()).onInterstitialLoaded(any(MoPubInterstitial.class));

        interstitialA.onCustomEventInterstitialShown();
        verify(listenerA).onInterstitialLoaded(interstitialA);
        verify(listenerB, never()).onInterstitialShown(any(MoPubInterstitial.class));

        interstitialA.onCustomEventInterstitialClicked();
        verify(listenerA).onInterstitialClicked(interstitialA);
        verify(listenerB, never()).onInterstitialClicked(any(MoPubInterstitial.class));

        interstitialA.onCustomEventInterstitialDismissed();
        verify(listenerA).onInterstitialDismissed(interstitialA);
        verify(listenerB, never()).onInterstitialDismissed(any(MoPubInterstitial.class));
    }

    @Test
    public void constructor_shouldSetIntentFilter() throws Exception {
        Set<String> expectedActions = Sets.newLinkedHashSet(
                ACTION_INTERSTITIAL_FAIL,
                ACTION_INTERSTITIAL_SHOW,
                ACTION_INTERSTITIAL_DISMISS,
                ACTION_INTERSTITIAL_CLICK
        );

        final IntentFilter intentFilter = subject.getIntentFilter();
        final Iterator<String> actionIterator = intentFilter.actionsIterator();

        assertThat(intentFilter.countActions()).isEqualTo(4);
        while (actionIterator.hasNext()) {
            assertThat(expectedActions.contains(actionIterator.next()));
        }
    }

    @Test
    public void onReceive_whenActionInterstitialFail_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(customEventInterstitialListener).onInterstitialClicked();
    }

    @Test
    public void onReceive_whenActionInterstitialShow_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(customEventInterstitialListener).onInterstitialShown();
    }


    @Test
    public void onReceive_whenActionInterstitialDismiss_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(customEventInterstitialListener).onInterstitialDismissed();
    }

    @Test
    public void onReceive_whenActionInterstitialClick_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(customEventInterstitialListener).onInterstitialClicked();
    }

    @Test
    public void onReceive_withIncorrectBroadcastIdentifier_shouldDoNothing() throws Exception {
        long incorrectBroadcastIdentifier = broadcastIdentifier + 1;

        Intent fail = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, incorrectBroadcastIdentifier);
        Intent show = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, incorrectBroadcastIdentifier);
        Intent click = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, incorrectBroadcastIdentifier);
        Intent dismiss = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, incorrectBroadcastIdentifier);

        subject.onReceive(context, fail);
        subject.onReceive(context, show);
        subject.onReceive(context, click);
        subject.onReceive(context, dismiss);

        verifyNoMoreInteractions(customEventInterstitialListener);
    }

    @Test
    public void onReceiver_whenCustomEventInterstitialListenerIsNull_shouldNotBlowUp() throws Exception {
        Intent intent = new Intent(ACTION_INTERSTITIAL_SHOW);

        subject = new EventForwardingBroadcastReceiver(null, broadcastIdentifier);
        subject.onReceive(context, intent);

        // pass
    }

    @Test
    public void register_shouldEnableReceivingBroadcasts() throws Exception {
        subject.register(subject, context);
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialShown();
    }

    @Test
    public void unregister_shouldDisableReceivingBroadcasts() throws Exception {
        subject.register(subject, context);

        subject.unregister(subject);
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialShown();
    }

    @Test
    public void unregister_whenNotRegistered_shouldNotBlowUp() throws Exception {
        subject.unregister(subject);

        // pass
    }

    @Test
    public void unregister_shouldNotLeakTheContext() throws Exception {
        subject.register(subject, context);
        subject.unregister(subject);

        LocalBroadcastManager.getInstance(context).registerReceiver(subject, subject.getIntentFilter());
        subject.unregister(subject);

        // Unregister shouldn't know the context any more and so should not have worked
        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        verify(customEventInterstitialListener).onInterstitialShown();
    }

    public static Intent getIntentForActionAndIdentifier(final String action, final long broadcastIdentifier) {
        final Intent intent = new Intent(action);
        intent.putExtra("broadcastIdentifier", broadcastIdentifier);
        return intent;
    }
}
