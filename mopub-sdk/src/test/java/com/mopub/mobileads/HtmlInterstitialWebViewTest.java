package com.mopub.mobileads;

import android.app.Activity;
import android.webkit.WebViewClient;

import com.mopub.common.AdReport;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.HtmlInterstitialWebView.HtmlInterstitialWebViewListener;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HtmlInterstitialWebViewTest {

    @Mock AdReport mockAdReport;
    private HtmlInterstitialWebView subject;
    private CustomEventInterstitialListener customEventInterstitialListener;
    private String clickthroughUrl;
    private boolean isScrollable;
    private String redirectUrl;
    private String dspCreativeId;

    @Before
    public void setUp() throws Exception {
        subject = new HtmlInterstitialWebView(Robolectric.buildActivity(Activity.class).create().get(),
                mockAdReport);
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        isScrollable = false;
        clickthroughUrl = "clickthroughUrl";
        redirectUrl = "redirectUrl";
        dspCreativeId = "dspCreativeId";
    }

    @Test
    public void init_shouldSetupWebViewClient() throws Exception {
        subject.init(customEventInterstitialListener, false, clickthroughUrl, redirectUrl, dspCreativeId);
        WebViewClient webViewClient = Shadows.shadowOf(subject).getWebViewClient();
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
}
