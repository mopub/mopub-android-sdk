package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.robolectric.Robolectric.shadowOf_;

@RunWith(SdkTestRunner.class)
public class MraidInterstitialTest extends ResponseBodyInterstitialTest {
    private CustomEventInterstitialListener customEventInterstitialListener;
    private Map<String,Object> localExtras;
    private Map<String,String> serverExtras;
    private Context context;
    private static final String INPUT_HTML_DATA = "%3Chtml%3E%3C%2Fhtml%3E";
    private static final String EXPECTED_HTML_DATA = "<html></html>";
    private long broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        subject = new MraidInterstitial();
        context = new Activity();
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put(HTML_RESPONSE_BODY_KEY, INPUT_HTML_DATA);

        broadcastIdentifier = 2222;
        final AdConfiguration adConfiguration = mock(AdConfiguration.class, withSettings().serializable());
        stub(adConfiguration.getBroadcastIdentifier()).toReturn(broadcastIdentifier);
        localExtras.put(AD_CONFIGURATION_KEY, adConfiguration);
    }

    @Test
    public void loadInterstitial_withMalformedServerExtras_shouldNotifyInterstitialFailed() throws Exception {
        serverExtras.remove(HTML_RESPONSE_BODY_KEY);
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        verify(customEventInterstitialListener).onInterstitialFailed(NETWORK_INVALID_STATE);
        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
    }

    @Test
    public void loadInterstitial_shouldNotifyInterstitialLoaded() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

//        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void loadInterstitial_shouldConnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialShown();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialClicked();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialDismissed();
    }

    @Test
    public void showInterstitial_shouldStartActivityWithIntent() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.showInterstitial();

        ShadowActivity shadowActivity = shadowOf_(context);
        Intent intent = shadowActivity.getNextStartedActivityForResult().intent;

        assertThat(intent.getComponent().getPackageName()).isEqualTo("com.mopub.mobileads");
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.mobileads.MraidActivity");
        assertThat(intent.getExtras().get(HTML_RESPONSE_BODY_KEY)).isEqualTo(EXPECTED_HTML_DATA);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
    }

    @Test
    public void onInvalidate_shouldDisconnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.onInvalidate();

        Intent intent;
        intent = new Intent(ACTION_INTERSTITIAL_SHOW);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialShown();

        intent = new Intent(ACTION_INTERSTITIAL_DISMISS);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialDismissed();
    }
}
