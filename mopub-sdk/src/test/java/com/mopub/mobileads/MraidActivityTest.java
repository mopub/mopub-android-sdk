package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidActivityTest {
    static final String EXPECTED_SOURCE = "expected source";
    static final String HTML_DATA = "html_data";

    @Mock MraidWebView mockMraidWebView;
    @Mock MraidBridge mraidBridge;
    @Mock MraidController mraidController;
    @Mock CustomEventInterstitial.CustomEventInterstitialListener
            customEventInterstitialListener;
    @Mock BroadcastReceiver broadcastReceiver;
    @Mock ResponseBodyInterstitial mraidInterstitial;

    Context context;

    // These fields are relics of a previous version of this class (all tests using them have since
    // been deprecated).
    MraidActivity subject;
    ActivityController<MraidActivity> activityController;

    long testBroadcastIdentifier = 2222;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        when(mockMraidWebView.getContext()).thenReturn(context);
    }

    @Test
    public void preRenderHtml_shouldEnableJavascriptCachingForDummyWebView() {
        MraidActivity.preRenderHtml(mraidInterstitial, customEventInterstitialListener, HTML_DATA,
                mockMraidWebView, testBroadcastIdentifier);

        verify(mockMraidWebView).enableJavascriptCaching();
    }

    @Test
    public void preRenderHtml_shouldDisablePluginsForDummyWebView() {
        MraidActivity.preRenderHtml(mraidInterstitial, customEventInterstitialListener, HTML_DATA,
                mockMraidWebView, testBroadcastIdentifier);

        verify(mockMraidWebView).enablePlugins(false);
    }

    @Test
    public void preRenderHtml_shouldLoadHtml() {
        MraidActivity.preRenderHtml(mraidInterstitial, customEventInterstitialListener, HTML_DATA,
                mockMraidWebView, testBroadcastIdentifier);

        verify(mockMraidWebView).loadDataWithBaseURL(
                "http://ads.mopub.com/",
                HTML_DATA,
                "text/html",
                "UTF-8",
                null
        );
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldSetWebViewClient() throws Exception {
        MraidActivity.preRenderHtml(mraidInterstitial, subject, customEventInterstitialListener,
                "3:27", testBroadcastIdentifier);

        verify(mockMraidWebView).enablePlugins(eq(false));
        verify(mraidController).setMraidListener(any(MraidListener.class));
        verify(mockMraidWebView).setWebViewClient(any(WebViewClient.class));
        verify(mraidBridge).setContentHtml(eq("3:27"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenMraidListenerOnReady() throws Exception {
        MraidActivity.preRenderHtml(mraidInterstitial, subject, customEventInterstitialListener, "",
                testBroadcastIdentifier);

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidController).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onLoaded(null);

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialFailed_whenMraidListenerOnFailure() throws Exception {
        MraidActivity.preRenderHtml(mraidInterstitial, subject, customEventInterstitialListener, "",
                testBroadcastIdentifier);

        ArgumentCaptor<MraidListener> mraidListenerArgumentCaptorr = ArgumentCaptor.forClass(MraidListener.class);
        verify(mraidController).setMraidListener(mraidListenerArgumentCaptorr.capture());
        MraidListener mraidListener = mraidListenerArgumentCaptorr.getValue();

        mraidListener.onFailedToLoad();

        verify(customEventInterstitialListener).onInterstitialFailed(null);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_whenWebViewClientShouldOverrideUrlLoading_shouldReturnTrue() throws Exception {
        MraidActivity.preRenderHtml(mraidInterstitial, subject, customEventInterstitialListener, "",
                testBroadcastIdentifier);

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mockMraidWebView).setWebViewClient(webViewClientArgumentCaptor.capture());
        WebViewClient webViewClient = webViewClientArgumentCaptor.getValue();

        boolean consumeUrlLoading = webViewClient.shouldOverrideUrlLoading(null, (String) null);

        assertThat(consumeUrlLoading).isTrue();
        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
        verify(customEventInterstitialListener, never()).onInterstitialFailed(
                any(MoPubErrorCode.class));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void preRenderHtml_shouldCallCustomEventInterstitialOnInterstitialLoaded_whenWebViewClientOnPageFinished() throws Exception {
        MraidActivity.preRenderHtml(mraidInterstitial, subject, customEventInterstitialListener, "",
                testBroadcastIdentifier);

        ArgumentCaptor<WebViewClient> webViewClientArgumentCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(mockMraidWebView).setWebViewClient(webViewClientArgumentCaptor.capture());
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

        assertThat(getContentView().getChildAt(0)).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(any(MraidListener.class));

        verify(mraidBridge).setContentHtml(EXPECTED_SOURCE);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldSetLayoutOfMraidView() throws Exception {
        subject.onCreate(null);

        ArgumentCaptor<FrameLayout.LayoutParams> captor = ArgumentCaptor.forClass(
                FrameLayout.LayoutParams.class);
        verify(mockMraidWebView).setLayoutParams(captor.capture());
        FrameLayout.LayoutParams actualLayoutParams = captor.getValue();

        assertThat(actualLayoutParams.width).isEqualTo(FrameLayout.LayoutParams.MATCH_PARENT);
        assertThat(actualLayoutParams.height).isEqualTo(FrameLayout.LayoutParams.MATCH_PARENT);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onCreate_shouldSetHardwareAcceleratedFlag() throws Exception {
        subject.onCreate(null);

        boolean hardwareAccelerated = Shadows.shadowOf(subject.getWindow()).getFlag(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        assertThat(hardwareAccelerated).isTrue();
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
        verify(mockMraidWebView).destroy();
        assertThat(getContentView().getChildCount()).isEqualTo(0);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void getAdView_shouldSetupOnReadyListener() throws Exception {
        reset(mockMraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        subject.hideInterstitialCloseButton();
        captor.getValue().onLoaded(null);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnReady_shouldFireJavascriptWebViewDidAppear() throws Exception {
        reset(mockMraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onLoaded(null);

        verify(mockMraidWebView).loadUrl(eq("javascript:webviewDidAppear();"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnClose_shouldFireJavascriptWebViewDidClose() throws Exception {
        reset(mockMraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onClose();

        verify(mockMraidWebView).loadUrl(eq("javascript:webviewDidClose();"));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void baseMraidListenerOnOpen_shouldBroadcastClickEvent() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        reset(mockMraidWebView);

        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        MraidListener baseMraidListener = captor.getValue();
        baseMraidListener.onOpen();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void getAdView_shouldSetupOnCloseListener() throws Exception {
        reset(mockMraidWebView);
        ArgumentCaptor<MraidListener> captor = ArgumentCaptor.forClass(MraidListener.class);
        View actualAdView = subject.getAdView();

        assertThat(actualAdView).isSameAs(mockMraidWebView);
        verify(mraidController).setMraidListener(captor.capture());

        captor.getValue().onClose();

        ANDROID.assertThat(subject).isFinishing();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onPause_shouldOnPauseMraidView() throws Exception {
        activityController.pause();

        verify(mockMraidWebView).onPause();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void onResume_shouldResumeMraidView() throws Exception {
        subject.onCreate(null);
        activityController.pause();
        activityController.resume();

        verify(mockMraidWebView).onResume();
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
