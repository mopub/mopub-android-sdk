package com.mopub.mobileads;

import android.app.Activity;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.webkit.WebViewClient;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.HtmlInterstitialWebView.HtmlInterstitialWebViewListener;
import static com.mopub.mobileads.HtmlInterstitialWebView.MOPUB_JS_INTERFACE_NAME;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.common.util.VersionCode.GINGERBREAD_MR1;
import static com.mopub.common.util.VersionCode.HONEYCOMB;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class HtmlInterstitialWebViewTest {

    private HtmlInterstitialWebView subject;
    private CustomEventInterstitialListener customEventInterstitialListener;
    private String clickthroughUrl;
    private boolean isScrollable;
    private String redirectUrl;
    private AdConfiguration adConfiguration;

    @Before
    public void setUp() throws Exception {
        adConfiguration = mock(AdConfiguration.class);
        subject = new HtmlInterstitialWebView(new Activity(), adConfiguration);
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        isScrollable = false;
        clickthroughUrl = "clickthroughUrl";
        redirectUrl = "redirectUrl";
    }

    @Test
    public void init_shouldSetupWebViewClient() throws Exception {
        subject.init(customEventInterstitialListener, false, clickthroughUrl, redirectUrl);
        WebViewClient webViewClient = shadowOf(subject).getWebViewClient();
        assertThat(webViewClient).isNotNull();
        assertThat(webViewClient).isInstanceOf(HtmlWebViewClient.class);
    }

    @Test
    public void htmlBannerWebViewListener_shouldForwardCalls() throws Exception {
        HtmlInterstitialWebViewListener listenerSubject = new HtmlInterstitialWebViewListener(customEventInterstitialListener);

        listenerSubject.onLoaded(subject);

        listenerSubject.onFailed(NETWORK_INVALID_STATE);
        verify(customEventInterstitialListener).onInterstitialFailed(eq(NETWORK_INVALID_STATE));

        listenerSubject.onClicked();
        verify(customEventInterstitialListener).onInterstitialClicked();
    }

    @Test
    public void init_shouldAddJavascriptInterface() throws Exception {
        subject.init(customEventInterstitialListener, isScrollable, clickthroughUrl, redirectUrl);

        Object javascriptInterface = shadowOf(subject).getJavascriptInterface(MOPUB_JS_INTERFACE_NAME);
        assertThat(javascriptInterface).isNotNull();

        Method fireFinishLoad = javascriptInterface.getClass().getDeclaredMethod("fireFinishLoad");
        Robolectric.pauseMainLooper();
        boolean returnValue = (Boolean) fireFinishLoad.invoke(javascriptInterface);
        assertThat(returnValue).isTrue();
        verify(customEventInterstitialListener, never()).onInterstitialShown();

        Robolectric.unPauseMainLooper();
        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Config(reportSdk = VERSION_CODES.HONEYCOMB)
    @Test
    public void destroy_atLeastHoneycomb_shouldRemoveJavascriptInterface() {
        HtmlInterstitialWebView spySubject = spy(subject);

        spySubject.destroy();

        verify(spySubject).removeJavascriptInterface(MOPUB_JS_INTERFACE_NAME);
    }

    @Config(reportSdk = VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void destroy_beforeHoneycomb_shouldNotRemoveJavascriptInterface() {
        HtmlInterstitialWebView spySubject = spy(subject);

        spySubject.destroy();

        verify(spySubject, never()).removeJavascriptInterface(MOPUB_JS_INTERFACE_NAME);
    }

    @Test
    public void destroy_shouldPreventJavascriptInterfaceFromNotifyingListener() throws Exception{
        subject.init(customEventInterstitialListener, isScrollable, clickthroughUrl, redirectUrl);

        Object javascriptInterface = shadowOf(subject).getJavascriptInterface(MOPUB_JS_INTERFACE_NAME);
        assertThat(javascriptInterface).isNotNull();

        subject.setIsDestroyed(true);

        Method fireFinishLoad = javascriptInterface.getClass().getDeclaredMethod("fireFinishLoad");
        fireFinishLoad.invoke(javascriptInterface);

        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
    }
}
