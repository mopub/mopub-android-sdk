package com.mopub.mobileads.test.support;

import android.content.Context;
import android.webkit.WebSettings;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.HtmlBannerWebView;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class TestHtmlBannerWebViewFactory extends HtmlBannerWebViewFactory {
    private HtmlBannerWebView mockHtmlBannerWebView = mock(HtmlBannerWebView.class);
    private CustomEventBannerListener latestListener;
    private boolean latestIsScrollable;
    private String latestRedirectUrl;
    private String latestClickthroughUrl;

    public TestHtmlBannerWebViewFactory() {
        WebSettings webSettings = mock(WebSettings.class);
        stub(mockHtmlBannerWebView.getSettings()).toReturn(webSettings);
        stub(webSettings.getUserAgentString()).toReturn("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
    }

    public static HtmlBannerWebView getSingletonMock() {
        return getTestFactory().mockHtmlBannerWebView;
    }

    private static TestHtmlBannerWebViewFactory getTestFactory() {
        return (TestHtmlBannerWebViewFactory) instance;
    }

    @Override
    public HtmlBannerWebView internalCreate(
            Context context, CustomEventBannerListener
            customEventBannerListener,
            boolean isScrollable,
            String redirectUrl,
            String clickthroughUrl, AdConfiguration adConfiguration) {
        latestListener = customEventBannerListener;
        latestIsScrollable = isScrollable;
        latestRedirectUrl = redirectUrl;
        latestClickthroughUrl = clickthroughUrl;
        return mockHtmlBannerWebView;
    }

    public static CustomEventBannerListener getLatestListener() {
        return getTestFactory().latestListener;
    }

    public static boolean getLatestIsScrollable() {
        return getTestFactory().latestIsScrollable;
    }

    public static String getLatestRedirectUrl() {
        return getTestFactory().latestRedirectUrl;
    }

    public static String getLatestClickthroughUrl() {
        return getTestFactory().latestClickthroughUrl;
    }
}
