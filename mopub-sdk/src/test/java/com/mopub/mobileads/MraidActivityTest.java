package com.mopub.mobileads;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.MraidBridge;
import com.mopub.mraid.MraidBridge.MraidWebView;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.MraidController.MraidListener;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidActivityTest {
    static final String EXPECTED_SOURCE = "expected source";

    @Mock MraidWebView mraidWebView;
    @Mock MraidBridge mraidBridge;
    @Mock MraidController mraidController;
    @Mock CustomEventInterstitial.CustomEventInterstitialListener
            customEventInterstitialListener;
    @Mock BroadcastReceiver broadcastReceiver;

    long testBroadcastIdentifier = 2222;

    ActivityController<TestMraidActivity> activityController;
    TestMraidActivity subject;

    // Make a concrete version of the abstract class for testing purposes.
    private static class TestMraidActivity extends MraidActivity {
        View mraidWebView;

        @Override
        public View getAdView() {
            return mraidWebView;
        }
    }

    @Before
    public void setUp() throws Exception {
        activityController = Robolectric.buildActivity(TestMraidActivity.class).start();
        subject = activityController.get();
        subject.mraidWebView = mraidWebView;
        activityController.create();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldDisablePluginsSetListenersAndLoadHtml() throws Exception {
        MraidActivity.preRenderHtml(subject, customEventInterstitialListener, "3:27");

        verify(mraidWebView).enablePlugins(eq(false));
        verify(mraidController).setMraidListener(any(MraidListener.class));
        verify(mraidWebView).setWebViewClient(any(WebViewClient.class));
        verify(mraidBridge).setContentHtml(eq("3:27"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenMraidListenerOnReady() throws Exception {
        MraidActivity.preRenderHtml(subject, customEventInterstitialListener, "");

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidController).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onLoaded(null);

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialFailed_whenMraidListenerOnFailure() throws Exception {
        MraidActivity.preRenderHtml(subject, customEventInterstitialListener, "");

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidController).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onFailedToLoad();

        verify(customEventInterstitialListener).onInterstitialFailed(null);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_whenWebViewClientShouldOverrideUrlLoading_shouldReturnTrue() throws Exception {
        MraidActivity.preRenderHtml(subject, customEventInterstitialListener, "");

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mraidWebView).setWebViewClient(webViewClientArgumentCaptor.capture());
        WebViewClient webViewClient = webViewClientArgumentCaptor.getValue();

        boolean consumeUrlLoading = webViewClient.shouldOverrideUrlLoading(null, null);

        assertThat(consumeUrlLoading).isTrue();
        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
        verify(customEventInterstitialListener, never()).onInterstitialFailed(
                any(MoPubErrorCode.class));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenWebViewClientOnPageFinished() throws Exception {
        MraidActivity.preRenderHtml(subject, customEventInterstitialListener, "");

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mraidWebView).setWebViewClient(webViewClientArgumentCaptor.capture());
        WebViewClient webViewClient = webViewClientArgumentCaptor.getValue();

        webViewClient.onPageFinished(null, null);

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldSetContentView() throws Exception {
        subject.onCreate(null);

        assertThat(getContentView().getChildCount()).isEqualTo(1);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldSetupAnMraidView() throws Exception {
        subject.onCreate(null);

        assertThat(getContentView().getChildAt(0)).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(any(MraidListener.class));

        verify(mraidBridge).setContentHtml(EXPECTED_SOURCE);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldSetLayoutOfMraidView() throws Exception {
        subject.onCreate(null);

        ArgumentCaptor<FrameLayout.LayoutParams> captor = ArgumentCaptor.forClass(
                FrameLayout.LayoutParams.class);
        verify(mraidWebView).setLayoutParams(captor.capture());
        FrameLayout.LayoutParams actualLayoutParams = captor.getValue();

        assertThat(actualLayoutParams.width).isEqualTo(FrameLayout.LayoutParams.MATCH_PARENT);
        assertThat(actualLayoutParams.height).isEqualTo(FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Config(sdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_atLeastIcs_shouldSetHardwareAcceleratedFlag() throws Exception {
        subject.onCreate(null);

        boolean hardwareAccelerated = Shadows.shadowOf(subject.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        assertThat(hardwareAccelerated).isTrue();
    }

    @Config(sdk = VERSION_CODES.HONEYCOMB_MR2)
    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_beforeIcs_shouldNotSetHardwareAcceleratedFlag() throws Exception {
        subject.onCreate(null);

        boolean hardwareAccelerated = Shadows.shadowOf(subject.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        assertThat(hardwareAccelerated).isFalse();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onDestroy_DestroyMraidView() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, subject.getBroadcastIdentifier());
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        subject.onDestroy();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
        verify(mraidWebView).destroy();
        assertThat(getContentView().getChildCount()).isEqualTo(0);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void getAdView_shouldSetupOnReadyListener() throws Exception {
        reset(mraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        subject.hideInterstitialCloseButton();
        captor.getValue().onLoaded(null);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnReady_shouldFireJavascriptWebViewDidAppear() throws Exception {
        reset(mraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onLoaded(null);

        verify(mraidWebView).loadUrl(eq("javascript:webviewDidAppear();"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnClose_shouldFireJavascriptWebViewDidClose() throws Exception {
        reset(mraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onClose();

        verify(mraidWebView).loadUrl(eq("javascript:webviewDidClose();"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnOpen_shouldBroadcastClickEvent() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        reset(mraidWebView);

        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onOpen();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void getAdView_shouldSetupOnCloseListener() throws Exception {
        reset(mraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        captor.getValue().onClose();

        ANDROID.assertThat(subject).isFinishing();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onPause_shouldOnPauseMraidView() throws Exception {
        activityController.pause();

        verify(mraidWebView).onPause();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onResume_shouldResumeMraidView() throws Exception {
        subject.onCreate(null);
        Shadows.shadowOf(subject).pauseAndThenResume();

        verify(mraidWebView).onResume();
    }

    private Intent createMraidActivityIntent(String expectedSource) {
        Intent mraidActivityIntent = new Intent();
        mraidActivityIntent.setComponent(new ComponentName("", ""));
        mraidActivityIntent.putExtra(HTML_RESPONSE_BODY_KEY, expectedSource);

        mraidActivityIntent.putExtra(BROADCAST_IDENTIFIER_KEY, testBroadcastIdentifier);

        return mraidActivityIntent;
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        subject.onDestroy();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    private FrameLayout getContentView() {
        return (FrameLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }
}
