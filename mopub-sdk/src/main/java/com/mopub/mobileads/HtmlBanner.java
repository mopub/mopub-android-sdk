package com.mopub.mobileads;

import android.content.Context;
import android.net.Uri;

import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.CLICKTHROUGH_URL_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.AdFetcher.REDIRECT_URL_KEY;
import static com.mopub.mobileads.AdFetcher.SCROLLABLE_KEY;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class HtmlBanner extends CustomEventBanner {

    private HtmlBannerWebView mHtmlBannerWebView;

    @Override
    protected void loadBanner(
            Context context,
            CustomEventBannerListener customEventBannerListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {

        String htmlData;
        String redirectUrl;
        String clickthroughUrl;
        Boolean isScrollable;
        if (extrasAreValid(serverExtras)) {
            htmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
            redirectUrl = serverExtras.get(REDIRECT_URL_KEY);
            clickthroughUrl = serverExtras.get(CLICKTHROUGH_URL_KEY);
            isScrollable = Boolean.valueOf(serverExtras.get(SCROLLABLE_KEY));
        } else {
            customEventBannerListener.onBannerFailed(NETWORK_INVALID_STATE);
            return;
        }

        AdConfiguration adConfiguration = AdConfiguration.extractFromMap(localExtras);
        mHtmlBannerWebView = HtmlBannerWebViewFactory.create(context, customEventBannerListener, isScrollable, redirectUrl, clickthroughUrl, adConfiguration);
        AdViewController.setShouldHonorServerDimensions(mHtmlBannerWebView);
        mHtmlBannerWebView.loadHtmlResponse(htmlData);
    }

    @Override
    protected void onInvalidate() {
        if (mHtmlBannerWebView != null) {
            mHtmlBannerWebView.destroy();
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
