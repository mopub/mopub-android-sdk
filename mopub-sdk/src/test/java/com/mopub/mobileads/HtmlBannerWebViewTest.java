package com.mopub.mobileads;

import android.app.Activity;
import android.webkit.WebViewClient;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class HtmlBannerWebViewTest {

    private AdConfiguration adConfiguration;
    private HtmlBannerWebView subject;
    private CustomEventBannerListener customEventBannerListener;
    private String clickthroughUrl;
    private String redirectUrl;

    @Before
    public void setup() throws Exception {
        adConfiguration = mock(AdConfiguration.class);
        subject = new HtmlBannerWebView(new Activity(), adConfiguration);
        customEventBannerListener = mock(CustomEventBannerListener.class);
        clickthroughUrl = "clickthroughUrl";
        redirectUrl = "redirectUrl";
    }

    @Test
    public void init_shouldSetupWebViewClient() throws Exception {
        subject.init(customEventBannerListener, false, clickthroughUrl, redirectUrl);
        WebViewClient webViewClient = shadowOf(subject).getWebViewClient();
        assertThat(webViewClient).isNotNull();
        assertThat(webViewClient).isInstanceOf(HtmlWebViewClient.class);
    }

    @Test
    public void htmlBannerWebViewListener_shouldForwardCalls() throws Exception {
        HtmlBannerWebView.HtmlBannerWebViewListener listenerSubject = new HtmlBannerWebView.HtmlBannerWebViewListener(customEventBannerListener);

        listenerSubject.onClicked();
        verify(customEventBannerListener).onBannerClicked();

        listenerSubject.onLoaded(subject);
        verify(customEventBannerListener).onBannerLoaded(eq(subject));

        listenerSubject.onCollapsed();
        verify(customEventBannerListener).onBannerCollapsed();

        listenerSubject.onFailed(NETWORK_INVALID_STATE);
        verify(customEventBannerListener).onBannerFailed(eq(NETWORK_INVALID_STATE));
    }
}
