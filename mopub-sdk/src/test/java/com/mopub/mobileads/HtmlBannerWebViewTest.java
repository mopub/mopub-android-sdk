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

import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HtmlBannerWebViewTest {
    private HtmlBannerWebView subject;
    @Mock
    private AdReport mockAdReport;
    private CustomEventBannerListener customEventBannerListener;
    private String clickthroughUrl;
    private String redirectUrl;
    private String dspCreativeId;

    @Before
    public void setup() throws Exception {
        subject = new HtmlBannerWebView(Robolectric.buildActivity(Activity.class).create().get(),
                mockAdReport);
        customEventBannerListener = mock(CustomEventBannerListener.class);
        clickthroughUrl = "clickthroughUrl";
        redirectUrl = "redirectUrl";
        dspCreativeId = "dspCreativeId";
    }

    @Test
    public void init_shouldSetupWebViewClient() throws Exception {
        subject.init(customEventBannerListener, false, clickthroughUrl, redirectUrl, dspCreativeId);
        WebViewClient webViewClient = Shadows.shadowOf(subject).getWebViewClient();
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
