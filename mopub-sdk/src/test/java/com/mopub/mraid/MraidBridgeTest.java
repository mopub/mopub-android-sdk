// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.app.Activity;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import com.mopub.common.CloseableLayout;
import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mraid.MraidBridge.MraidBridgeListener;
import com.mopub.mraid.MraidBridge.MraidWebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class MraidBridgeTest {
    @Mock
    private MraidNativeCommandHandler mockNativeCommandHandler;
    @Mock
    private MraidBridgeListener mockBridgeListener;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MraidWebView mockBannerWebView;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MraidWebView mockInterstitialWebView;
    @Mock
    private WebSettings mockWebSettings;
    @Mock
    private RenderProcessGoneDetail mockRenderProcessGoneDetail;
    @Captor
    private ArgumentCaptor<WebViewClient> bannerWebViewClientCaptor;

    private Activity activity;
    private MraidBridge subjectBanner;
    private MraidBridge subjectInterstitial;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, false);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);

        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, false);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
    }

    @Test
    public void attachView_withInterstitial_shouldAutoPlayVideo() {
        when(mockInterstitialWebView.getSettings()).thenReturn(mockWebSettings);

        subjectInterstitial.attachView(mockInterstitialWebView);

        verify(mockWebSettings).setMediaPlaybackRequiresUserGesture(false);
    }

    @Test
    public void attachView_withInline_shouldNotAutoPlayVideo() {
        when(mockBannerWebView.getSettings()).thenReturn(mockWebSettings);

        subjectBanner.attachView(mockBannerWebView);

        verify(mockWebSettings, never()).setMediaPlaybackRequiresUserGesture(anyBoolean());
        // Disregard setting of javascript
        verify(mockWebSettings).setJavaScriptEnabled(anyBoolean());
        // Ensure mockWebSettings.setMediaPlaybackRequiresUserGesture or anything else is never called
        verifyNoMoreInteractions(mockWebSettings);
    }

    @Test
    public void attachView_thenDetach_shouldSetMRaidWebView_thenShouldClear() {
        attachWebViews();
        assertThat(subjectBanner.getMraidWebView()).isEqualTo(mockBannerWebView);

        subjectBanner.detach();
        assertThat(subjectBanner.getMraidWebView()).isNull();
    }

    @Test
    public void attachView_thenOnPageFinished_shouldFireReady() {
        attachWebViews();
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url");

        verify(mockBridgeListener).onPageLoaded();
    }

    @Test
    public void attachView_thenOnPageFinished_twice_shouldNotFireReadySecondTime() {
        attachWebViews();
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url");
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url2");

        verify(mockBridgeListener, times(1)).onPageLoaded();
    }

    @Test
    public void attachView_thenSetContentHtml_shouldCallLoadDataWithBaseURL() {
        attachWebViews();
        subjectBanner.setContentHtml("test-html");

        verify(mockBannerWebView).loadDataWithBaseURL(
                "http://" + Constants.HOST + "/", "test-html", "text/html", "UTF-8", null);
    }

    @Test
    public void handleShouldOverrideUrl_invalidUrl_shouldFireErrorEvent() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("bad bad bad");

        verify(mockBannerWebView).loadUrl(startsWith(
                "javascript:window.mraidbridge.notifyErrorEvent"));
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mopubNonFailLoadUrl_shouldNeverLoadUrl_shouldReturnTrue() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("mopub://special-mopub-command");

        verify(mockBannerWebView, never()).loadUrl(anyString());
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mopubFailLoadUrl_whenBanner_shouldNotifyListenerOfOnPageFailedToLoad_shouldReturnTrue() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("mopub://failLoad");

        verify(mockBridgeListener).onPageFailedToLoad();
        verify(mockBannerWebView, never()).loadUrl(anyString());
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mopubFailLoadUrl_whenInterstitial_shouldNotNotifyListenerOfOnPageFailedToLoad_shouldReturnTrue() {
        attachWebViews();
        boolean result = subjectInterstitial.handleShouldOverrideUrl("mopub://failLoad");

        verify(mockBridgeListener, never()).onPageFailedToLoad();
        verify(mockBannerWebView, never()).loadUrl(anyString());
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mraidUrl_invalid_shouldFireErrorEvent_shouldReturnTrue() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("mraid://bad-command");

        verify(mockBannerWebView).loadUrl(startsWith(
                "javascript:window.mraidbridge.notifyErrorEvent"));
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_smsUrl_notClicked_shouldReturnFalse() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("sms://123456789");

        assertThat(result).isFalse();
    }

    @Test
    public void handleShouldOverrideUrl_validUrl_clicked_shoulReturnTrue() throws URISyntaxException {
        attachWebViews();
        subjectBanner.setClicked(true);
        reset(mockBannerWebView);
        when(mockBannerWebView.getContext()).thenReturn(activity);

        boolean result = subjectBanner.handleShouldOverrideUrl("sms://123456789");
        verify(mockBridgeListener).onOpen(new URI("sms://123456789"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("tel:123456");
        verify(mockBridgeListener).onOpen(new URI("tel:123456"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("http://www.mopub.com/");
        verify(mockBridgeListener).onOpen(new URI("http://www.mopub.com/"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("mopubnativebrowser://navigate?url=https://www.mopub.com");
        verify(mockBridgeListener).onOpen(new URI("mopubnativebrowser://navigate?url=https://www.mopub.com"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("market://details?id=com.fitbit.FitbitMobile");
        verify(mockBridgeListener).onOpen(new URI("market://details?id=com.fitbit.FitbitMobile"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("deeplink+://navigate?primaryUrl=twitter://timeline");
        verify(mockBridgeListener).onOpen(new URI("deeplink+://navigate?primaryUrl=twitter://timeline"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("geo:37.7767486,-122.416749?q=37.7767486%2C-122.416749");
        verify(mockBridgeListener).onOpen(new URI("geo:37.7767486,-122.416749?q=37.7767486%2C-122.416749"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end");
        verify(mockBridgeListener).onOpen(new URI("intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"));
        assertThat(result).isTrue();

        reset(mockBridgeListener);
        result = subjectBanner.handleShouldOverrideUrl("mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802");
        verify(mockBridgeListener).onOpen(new URI("mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802"));
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_normalUrl_shouldReturnFalse() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("https://www.mopub.com");

        assertThat(result).isFalse();
    }

    @Test
    public void handleRenderProcessGone_shouldDetach_shouldNotifyMraidBridgeListener() {
        subjectBanner.handleRenderProcessGone(mockRenderProcessGoneDetail);
        verify(mockBridgeListener).onRenderProcessGone(any(MoPubErrorCode.class));
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner = new MraidBridge(PlacementType.INLINE, false);
        subjectBanner.attachView(mockBannerWebView);
        subjectBanner.setClicked(false);
        Map<String, String> params = new HashMap<>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    @Test
    public void runCommand_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_interstitial_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectInterstitial.setClicked(false);
        Map<String, String> params = new HashMap<>();
        params.put("uri", "https://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_interstitial_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectInterstitial.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("url", "https://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_close_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        Map<String, String> params = new HashMap<>();
        
        subjectBanner.runCommand(MraidJavascriptCommand.CLOSE, params);

        verify(mockBridgeListener).onClose();
    }

    @Test
    public void runCommand_useCustomClose_withShouldUseCustomCloseTrue_shouldPassTrue()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, true);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, true);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.USE_CUSTOM_CLOSE, params);

        verify(mockBridgeListener).onUseCustomClose( true);
    }

    @Test
    public void runCommand_useCustomClose_withShouldUseCustomCloseFalse_shouldPassFalse()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, true);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, true);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("shouldUseCustomClose", "false");

        subjectBanner.runCommand(MraidJavascriptCommand.USE_CUSTOM_CLOSE, params);

        verify(mockBridgeListener).onUseCustomClose( false);
    }

    @Test
    public void runCommand_useCustomClose_withShouldUseCustomCloseTrue_withAllowCustomCloseFalse_shouldPassFalse()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.USE_CUSTOM_CLOSE, params);

        verify(mockBridgeListener).onUseCustomClose( false);
    }

    @Test
    public void runCommand_resize_withAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, true);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, true);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("width", "50");
        params.put("height", "60");
        params.put("offsetX", "70");
        params.put("offsetY", "80");

        subjectBanner.runCommand(MraidJavascriptCommand.RESIZE, params);

        verify(mockBridgeListener).onResize(anyInt(), anyInt(), anyInt(), anyInt(), any(CloseableLayout.ClosePosition.class), anyBoolean());
        verify(mockBridgeListener).onUseCustomClose(true);
    }

    @Test
    public void runCommand_resize_withoutAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("width", "50");
        params.put("height", "60");
        params.put("offsetX", "70");
        params.put("offsetY", "80");

        subjectBanner.runCommand(MraidJavascriptCommand.RESIZE, params);

        verify(mockBridgeListener).onResize(anyInt(), anyInt(), anyInt(), anyInt(), any(CloseableLayout.ClosePosition.class), anyBoolean());
        verify(mockBridgeListener).onUseCustomClose(false);
    }

    @Test
    public void runCommand_expand_withAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, true);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, true);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        verify(mockBridgeListener).onExpand(null, true);
    }

    @Test
    public void runCommand_expand_withoutAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        verify(mockBridgeListener).onExpand(null, false);
    }

    @Test
    public void runCommand_expand_withUrl_withAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(PlacementType.INLINE, mockNativeCommandHandler, true);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial = new MraidBridge(PlacementType.INTERSTITIAL, mockNativeCommandHandler, true);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("url", "https://valid-url");
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onExpand(
                uriCaptor.capture(), eq(true));
        assertThat(uriCaptor.getValue().toString()).isEqualTo("https://valid-url");
    }

    @Test
    public void runCommand_expand_withUrl_withoutAllowCustomClose_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("url", "https://valid-url");
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onExpand(
                uriCaptor.capture(), eq(false));
        assertThat(uriCaptor.getValue().toString()).isEqualTo("https://valid-url");
    }

    @Test
    public void runCommand_playVideo_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onPlayVideo(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString()).isEqualTo("https://valid-url");
    }

    private void attachWebViews() {
        subjectBanner.attachView(mockBannerWebView);
        subjectInterstitial.attachView(mockInterstitialWebView);

        verify(mockBannerWebView).setWebViewClient(bannerWebViewClientCaptor.capture());
        reset(mockBannerWebView);
    }
}
