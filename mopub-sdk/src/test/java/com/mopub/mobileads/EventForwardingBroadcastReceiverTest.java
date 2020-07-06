// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;

import org.fest.util.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_CLICK;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_FAIL;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_SHOW;
import static com.mopub.common.IntentActions.ACTION_REWARDED_AD_COMPLETE;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
public class EventForwardingBroadcastReceiverTest {

    private AdLifecycleListener.InteractionListener interactionListener;
    private EventForwardingBroadcastReceiver subject;
    private Activity context;
    private int broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        interactionListener = mock(AdLifecycleListener.InteractionListener.class);
        broadcastIdentifier = 27027027;
        subject = new EventForwardingBroadcastReceiver(interactionListener, broadcastIdentifier);
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
        final AdData adData = new AdData.Builder()
                .extras(serverExtras)
                .broadcastIdentifier(broadcastIdentifier)
                .build();

        final FullscreenAdAdapter fullscreenAdAdapter = new FullscreenAdAdapter(context,
                "com.mopub.mobileads.MoPubFullscreen",
                adData);


        fullscreenAdAdapter.load(mock(AdLifecycleListener.LoadListener.class));
        verify(listenerA).onInterstitialLoaded(interstitialA);
        verify(listenerB, never()).onInterstitialLoaded(any(MoPubInterstitial.class));

        interstitialA.onAdShown();
        verify(listenerA).onInterstitialLoaded(interstitialA);
        verify(listenerB, never()).onInterstitialShown(any(MoPubInterstitial.class));

        interstitialA.onAdClicked();
        verify(listenerA).onInterstitialClicked(interstitialA);
        verify(listenerB, never()).onInterstitialClicked(any(MoPubInterstitial.class));

        interstitialA.onAdDismissed();
        verify(listenerA).onInterstitialDismissed(interstitialA);
        verify(listenerB, never()).onInterstitialDismissed(any(MoPubInterstitial.class));
    }

    @Test
    public void constructor_shouldSetIntentFilter() throws Exception {
        Set<String> expectedActions = Sets.newLinkedHashSet(
                ACTION_FULLSCREEN_FAIL,
                ACTION_FULLSCREEN_SHOW,
                ACTION_FULLSCREEN_DISMISS,
                ACTION_FULLSCREEN_CLICK,
                ACTION_REWARDED_AD_COMPLETE
        );

        final IntentFilter intentFilter = subject.getIntentFilter();
        final Iterator<String> actionIterator = intentFilter.actionsIterator();

        assertThat(intentFilter.countActions()).isEqualTo(5);
        while (actionIterator.hasNext()) {
            assertThat(expectedActions.contains(actionIterator.next()));
        }
    }

    @Test
    public void onReceive_whenActionInterstitialFail_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_CLICK, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(interactionListener).onAdClicked();
    }

    @Test
    public void onReceive_whenActionInterstitialShow_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(interactionListener).onAdShown();
    }


    @Test
    public void onReceive_whenActionInterstitialDismiss_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_DISMISS, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(interactionListener).onAdDismissed();
    }

    @Test
    public void onReceive_whenActionInterstitialClick_shouldNotifyListener() throws Exception {
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_CLICK, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(interactionListener).onAdClicked();
    }


    @Test
    public void onReceive_withActionRewardedAdComplete_shouldNotifyListener() {
        Intent intent = getIntentForActionAndIdentifier(ACTION_REWARDED_AD_COMPLETE, broadcastIdentifier);

        subject.onReceive(context, intent);

        verify(interactionListener).onAdComplete(any());
    }

    @Test
    public void onReceive_withIncorrectBroadcastIdentifier_shouldDoNothing() throws Exception {
        long incorrectBroadcastIdentifier = broadcastIdentifier + 1;

        Intent fail = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_FAIL, incorrectBroadcastIdentifier);
        Intent show = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, incorrectBroadcastIdentifier);
        Intent click = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_CLICK, incorrectBroadcastIdentifier);
        Intent complete = getIntentForActionAndIdentifier(ACTION_REWARDED_AD_COMPLETE, incorrectBroadcastIdentifier);
        Intent dismiss = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_DISMISS, incorrectBroadcastIdentifier);

        subject.onReceive(context, fail);
        subject.onReceive(context, show);
        subject.onReceive(context, click);
        subject.onReceive(context, complete);
        subject.onReceive(context, dismiss);

        verifyNoMoreInteractions(interactionListener);
    }

    @Test
    public void onReceive_whenFullscreenAdListenerIsNull_shouldNotBlowUp() throws Exception {
        Intent intent = new Intent(ACTION_FULLSCREEN_SHOW);

        subject = new EventForwardingBroadcastReceiver(null, broadcastIdentifier);
        subject.onReceive(context, intent);

        // pass
    }

    @Test
    public void register_shouldEnableReceivingBroadcasts() throws Exception {
        subject.register(subject, context);
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener).onAdShown();
    }

    @Test
    public void unregister_shouldDisableReceivingBroadcasts() throws Exception {
        subject.register(subject, context);

        subject.unregister(subject);
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(interactionListener, never()).onAdShown();
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
        Intent intent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, broadcastIdentifier);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        verify(interactionListener).onAdShown();
    }

    public static Intent getIntentForActionAndIdentifier(final String action, final long broadcastIdentifier) {
        final Intent intent = new Intent(action);
        intent.putExtra("broadcastIdentifier", broadcastIdentifier);
        return intent;
    }
}
