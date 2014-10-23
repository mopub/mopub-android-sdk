package com.mopub.mobileads;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.test.support.TestMraidViewFactory;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.MraidView.MraidListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class MraidActivityTest extends BaseInterstitialActivityTest {

    private MraidView mraidView;
    private CustomEventInterstitial.CustomEventInterstitialListener customEventInterstitialListener;
    private Activity context;

    @Before
    public void setUp() throws Exception {
        super.setup();
        context = new Activity();
        Intent mraidActivityIntent = createMraidActivityIntent(EXPECTED_SOURCE);
        mraidView = TestMraidViewFactory.getSingletonMock();
        resetMockedView(mraidView);
        subject = Robolectric.buildActivity(MraidActivity.class).withIntent(mraidActivityIntent).create().get();
        resetMockedView(mraidView);
        customEventInterstitialListener = mock(CustomEventInterstitial.CustomEventInterstitialListener.class);
    }

    @Test
    public void preRenderHtml_shouldDisablePluginsSetListenersAndLoadHtml() throws Exception {
        MraidActivity.preRenderHtml(null, customEventInterstitialListener, "3:27");

        verify(mraidView).enablePlugins(eq(false));
        verify(mraidView).setMraidListener(any(MraidListener.class));
        verify(mraidView).setWebViewClient(any(WebViewClient.class));
        verify(mraidView).loadHtmlData(eq("3:27"));
    }

    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenMraidListenerOnReady() throws Exception {
        MraidActivity.preRenderHtml(null, customEventInterstitialListener, "");

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidView).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onReady(null);

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialFailed_whenMraidListenerOnFailure() throws Exception {
        MraidActivity.preRenderHtml(null, customEventInterstitialListener, "");

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidView).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onFailure(null);

        verify(customEventInterstitialListener).onInterstitialFailed(null);
    }

    @Test
    public void preRenderHtml_whenWebViewClientShouldOverrideUrlLoading_shouldReturnTrue() throws Exception {
        MraidActivity.preRenderHtml(null, customEventInterstitialListener, "");

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mraidView).setWebViewClient(webViewClientArgumentCaptor.capture());
        WebViewClient webViewClient = webViewClientArgumentCaptor.getValue();

        boolean consumeUrlLoading = webViewClient.shouldOverrideUrlLoading(null, null);

        assertThat(consumeUrlLoading).isTrue();
        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
        verify(customEventInterstitialListener, never()).onInterstitialFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenWebViewClientOnPageFinished() throws Exception {
        MraidActivity.preRenderHtml(null, customEventInterstitialListener, "");

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mraidView).setWebViewClient(webViewClientArgumentCaptor.capture());
        WebViewClient webViewClient = webViewClientArgumentCaptor.getValue();

        webViewClient.onPageFinished(null, null);

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void onCreate_shouldSetContentView() throws Exception {
        subject.onCreate(null);

        assertThat(getContentView(subject).getChildCount()).isEqualTo(3);
    }

    @Test
    public void onCreate_shouldSetupAnMraidView() throws Exception {
        subject.onCreate(null);

        assertThat(getContentView(subject).getChildAt(0)).isSameAs(mraidView);
        verify(mraidView).setMraidListener(any(MraidListener.class));
        verify(mraidView).setOnCloseButtonStateChange(any(MraidView.OnCloseButtonStateChangeListener.class));

        verify(mraidView).loadHtmlData(EXPECTED_SOURCE);
    }

    @Test
    public void onCreate_shouldSetLayoutOfMraidView() throws Exception {
        subject.onCreate(null);

        ArgumentCaptor<RelativeLayout.LayoutParams> captor = ArgumentCaptor.forClass(RelativeLayout.LayoutParams.class);
        verify(mraidView).setLayoutParams(captor.capture());
        RelativeLayout.LayoutParams actualLayoutParams = captor.getValue();

        assertThat(actualLayoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(actualLayoutParams.height).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
    }

    @Test
    public void onCreate_shouldAddCloseEventRegion() throws Exception {
        subject.onCreate(null);

        final Button closeEventRegion = (Button) getContentView(subject).getChildAt(2);
        assertThat(closeEventRegion.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(closeEventRegion).getBackgroundColor()).isEqualTo(Color.TRANSPARENT);
        assertThat(Dips.pixelsToIntDips((float) closeEventRegion.getLayoutParams().width, context)).isEqualTo(50);
        assertThat(Dips.pixelsToIntDips((float) closeEventRegion.getLayoutParams().height, context)).isEqualTo(50);
        assertThat(((RelativeLayout.LayoutParams)closeEventRegion.getLayoutParams()).getRules()[RelativeLayout.ALIGN_PARENT_TOP])
                .isEqualTo(RelativeLayout.TRUE);
        assertThat(((RelativeLayout.LayoutParams)closeEventRegion.getLayoutParams()).getRules()[RelativeLayout.ALIGN_PARENT_RIGHT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void onCreate_atLeastIcs_shouldSetHardwareAcceleratedFlag() throws Exception {
        subject.onCreate(null);

        boolean hardwareAccelerated = shadowOf(subject.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        assertThat(hardwareAccelerated).isTrue();
    }

    @Config(reportSdk = VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void onCreate_beforeIcs_shouldNotSetHardwareAcceleratedFlag() throws Exception {
        subject.onCreate(null);

        boolean hardwareAccelerated = shadowOf(subject.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        assertThat(hardwareAccelerated).isFalse();
    }

    @Test
    public void closeEventRegion_shouldFinishActivityWhenClicked() throws Exception {
        subject.onCreate(null);

        final Button closeEventRegion = (Button) getContentView(subject).getChildAt(2);
        assertThat(closeEventRegion.performClick()).isTrue();
        assertThat(subject.isFinishing()).isTrue();
    }

    @Test
    public void onDestroy_DestroyMraidView() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, subject.getBroadcastIdentifier());
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        subject.onCreate(null);
        subject.onDestroy();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
        verify(mraidView).destroy();
        assertThat(getContentView(subject).getChildCount()).isEqualTo(0);
    }

    @Test
    public void getAdView_shouldSetupOnReadyListener() throws Exception {
        subject.onCreate(null);
        resetMockedView(mraidView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setMraidListener(captor.capture());

        subject.hideInterstitialCloseButton();
        captor.getValue().onReady(null);
        ImageButton closeButton = (ImageButton) getContentView(subject).getChildAt(1);
        assertThat(closeButton).isNotNull();
    }

    @Test
    public void baseMraidListenerOnReady_shouldFireJavascriptWebViewDidAppear() throws Exception {
        subject.onCreate(null);
        resetMockedView(mraidView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onReady(null);

        verify(mraidView).loadUrl(eq("javascript:webviewDidAppear();"));
    }

    @Test
    public void baseMraidListenerOnClose_shouldFireJavascriptWebViewDidClose() throws Exception {
        subject.onCreate(null);
        resetMockedView(mraidView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onClose(null, null);

        verify(mraidView).loadUrl(eq("javascript:webviewDidClose();"));
    }

    @Test
    public void baseMraidListenerOnOpen_shouldBroadcastClickEvent() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        subject.onCreate(null);
        resetMockedView(mraidView);

        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onOpen(null);

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void getAdView_shouldSetupOnCloseButtonStateChangeListener() throws Exception {
        subject.onCreate(null);
        resetMockedView(mraidView);
        ArgumentCaptor<MraidView.OnCloseButtonStateChangeListener> captor = ArgumentCaptor.forClass(MraidView.OnCloseButtonStateChangeListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setOnCloseButtonStateChange(captor.capture());
        MraidView.OnCloseButtonStateChangeListener listener = captor.getValue();

        ANDROID.assertThat(getCloseButton()).isVisible();

        listener.onCloseButtonStateChange(null, false);
        ANDROID.assertThat(getCloseButton()).isNotVisible();

        listener.onCloseButtonStateChange(null, true);
        ANDROID.assertThat(getCloseButton()).isVisible();
    }

    @Test
    public void getAdView_shouldSetupOnCloseListener() throws Exception {
        subject.onCreate(null);
        resetMockedView(mraidView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidView);
        verify(mraidView).setMraidListener(captor.capture());

        captor.getValue().onClose(null, null);

        ANDROID.assertThat(subject).isFinishing();
    }

    @Test
    public void onPause_shouldOnPauseMraidView() throws Exception {
        subject.onCreate(null);
        ((MraidActivity)subject).onPause();

        verify(mraidView).onPause();
    }

    @Test
    public void onResume_shouldResumeMraidView() throws Exception {
        subject.onCreate(null);
        ((MraidActivity)subject).onPause();
        ((MraidActivity)subject).onResume();

        verify(mraidView).onResume();
    }

    private Intent createMraidActivityIntent(String expectedSource) {
        Intent mraidActivityIntent = new Intent();
        mraidActivityIntent.setComponent(new ComponentName("", ""));
        mraidActivityIntent.putExtra(HTML_RESPONSE_BODY_KEY, expectedSource);

        adConfiguration = mock(AdConfiguration.class, withSettings().serializable());
        stub(adConfiguration.getBroadcastIdentifier()).toReturn(testBroadcastIdentifier);
        mraidActivityIntent.putExtra(AD_CONFIGURATION_KEY, adConfiguration);

        return mraidActivityIntent;
    }
}
