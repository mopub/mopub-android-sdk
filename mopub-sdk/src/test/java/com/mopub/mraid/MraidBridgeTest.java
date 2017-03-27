package com.mopub.mraid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mraid.MraidBridge.MraidBridgeListener;
import com.mopub.mraid.MraidBridge.MraidWebView;
import com.mopub.mraid.MraidNativeCommandHandler.MraidCommandFailureListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
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
@Config(constants = BuildConfig.class)
public class MraidBridgeTest {
    @Mock MraidNativeCommandHandler mockNativeCommandHandler;
    @Mock MraidBridgeListener mockBridgeListener;
    @Mock AdReport mockAdReport;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MraidWebView mockBannerWebView;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MraidWebView mockInterstitialWebView;
    @Mock WebSettings mockWebSettings;
    @Captor ArgumentCaptor<WebViewClient> bannerWebViewClientCaptor;

    private Activity activity;
    private MraidBridge subjectBanner;
    private MraidBridge subjectInterstitial;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        subjectBanner = new MraidBridge(mockAdReport, PlacementType.INLINE, mockNativeCommandHandler);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);

        subjectInterstitial = new MraidBridge(mockAdReport, PlacementType.INTERSTITIAL, mockNativeCommandHandler);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void attachView_atLeastJellyBeanMr1_withInterstitial_shouldAutoPlayVideo() {
        when(mockInterstitialWebView.getSettings()).thenReturn(mockWebSettings);

        subjectInterstitial.attachView(mockInterstitialWebView);

        verify(mockWebSettings).setMediaPlaybackRequiresUserGesture(false);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void attachView_atLeastJellyBeanMr1_withInline_shouldNotAutoPlayVideo() {
        when(mockBannerWebView.getSettings()).thenReturn(mockWebSettings);

        subjectBanner.attachView(mockBannerWebView);

        verify(mockWebSettings, never()).setMediaPlaybackRequiresUserGesture(anyBoolean());
    }

    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void attachView_belowJellyBeanMr1_withInterstitial_shouldNotAutoPlayVideo() {
        when(mockInterstitialWebView.getSettings()).thenReturn(mockWebSettings);

        subjectInterstitial.attachView(mockInterstitialWebView);

        // Disregard setting of javascript
        verify(mockWebSettings).setJavaScriptEnabled(anyBoolean());
        // Ensure mockWebSettings.setMediaPlaybackRequiresUserGesture is never called
        verifyNoMoreInteractions(mockWebSettings);
    }

    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void attachView_belowJellyBeanMr1_withInline_shouldNotAutoPlayVideo() {
        when(mockBannerWebView.getSettings()).thenReturn(mockWebSettings);

        subjectBanner.attachView(mockBannerWebView);

        // Disregard setting of javascript
        verify(mockWebSettings).setJavaScriptEnabled(anyBoolean());
        // Ensure mockWebSettings.setMediaPlaybackRequiresUserGesture is never called
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
    public void handleShouldOverrideUrl_smsUrl_clicked_shouldStartActivity() {
        attachWebViews();
        subjectBanner.setClicked(true);
        reset(mockBannerWebView);
        when(mockBannerWebView.getContext()).thenReturn(activity);

        boolean result = subjectBanner.handleShouldOverrideUrl("sms://123456789");

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedIntent.getComponent()).isNull();
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_normalUrl_shouldReturnFalse() {
        attachWebViews();
        boolean result = subjectBanner.handleShouldOverrideUrl("https://www.mopub.com");

        assertThat(result).isFalse();
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner = new MraidBridge(mockAdReport, PlacementType.INLINE);
        subjectBanner.attachView(mockBannerWebView);
        subjectBanner.setClicked(false);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    public void runCommand_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_interstitial_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectInterstitial.setClicked(false);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "https://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_interstitial_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        attachWebViews();
        subjectInterstitial.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("url", "https://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_close_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        Map<String, String> params = new HashMap<String, String>();
        
        subjectBanner.runCommand(MraidJavascriptCommand.CLOSE, params);

        verify(mockBridgeListener).onClose();
    }

    @Test
    public void runCommand_expand_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        verify(mockBridgeListener).onExpand(null, true);
    }

    @Test
    public void runCommand_expand_withUrl_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("url", "https://valid-url");
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onExpand(
                uriCaptor.capture(), eq(true));
        assertThat(uriCaptor.getValue().toString()).isEqualTo("https://valid-url");
    }

    @Test
    public void runCommand_playVideo_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onPlayVideo(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString()).isEqualTo("https://valid-url");
    }

    @Test
    public void runCommand_storePicture_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "https://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.STORE_PICTURE, params);

        verify(mockNativeCommandHandler).storePicture(any(Context.class), eq("https://valid-url"),
                any(MraidCommandFailureListener.class));
    }

    @Test
    public void runCommand_createCalendarEvent_shouldCallListener()
            throws MraidCommandException {
        attachWebViews();
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("eventName", "Dinner at my house");

        subjectBanner.runCommand(MraidJavascriptCommand.CREATE_CALENDAR_EVENT, params);

        verify(mockNativeCommandHandler).createCalendarEvent(any(Context.class),
                anyMapOf(String.class, String.class));
    }

    private void attachWebViews() {
        subjectBanner.attachView(mockBannerWebView);
        subjectInterstitial.attachView(mockInterstitialWebView);

        verify(mockBannerWebView).setWebViewClient(bannerWebViewClientCaptor.capture());
        reset(mockBannerWebView);
    }
}
