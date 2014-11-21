package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.HtmlInterstitialWebView;

import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

public class HtmlInterstitialWebViewFactory {
    protected static HtmlInterstitialWebViewFactory instance = new HtmlInterstitialWebViewFactory();

    public static HtmlInterstitialWebView create(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            boolean isScrollable,
            String redirectUrl,
            String clickthroughUrl,
            AdConfiguration adConfiguration) {
        return instance.internalCreate(context, customEventInterstitialListener, isScrollable, redirectUrl, clickthroughUrl, adConfiguration);
    }

    public HtmlInterstitialWebView internalCreate(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            boolean isScrollable,
            String redirectUrl,
            String clickthroughUrl,
            AdConfiguration adConfiguration) {
        HtmlInterstitialWebView htmlInterstitialWebView = new HtmlInterstitialWebView(context, adConfiguration);
        htmlInterstitialWebView.init(customEventInterstitialListener, isScrollable, redirectUrl, clickthroughUrl);
        return htmlInterstitialWebView;
    }

    @Deprecated // for testing
    public static void setInstance(HtmlInterstitialWebViewFactory factory) {
        instance = factory;
    }
}
