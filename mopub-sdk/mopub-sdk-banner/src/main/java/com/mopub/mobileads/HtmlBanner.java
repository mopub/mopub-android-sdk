package com.mopub.mobileads;

import android.content.Context;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
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
        AdReport adReport;
        if (extrasAreValid(serverExtras)) {
            htmlData = serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY);
            redirectUrl = serverExtras.get(DataKeys.REDIRECT_URL_KEY);
            clickthroughUrl = serverExtras.get(DataKeys.CLICKTHROUGH_URL_KEY);
            isScrollable = Boolean.valueOf(serverExtras.get(DataKeys.SCROLLABLE_KEY));
            try {
                adReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            } catch (ClassCastException e) {
                MoPubLog.e("LocalExtras contained an incorrect type.");
                customEventBannerListener.onBannerFailed(INTERNAL_ERROR);
                return;
            }
        } else {
            customEventBannerListener.onBannerFailed(NETWORK_INVALID_STATE);
            return;
        }

        mHtmlBannerWebView = HtmlBannerWebViewFactory.create(context, adReport, customEventBannerListener, isScrollable, redirectUrl, clickthroughUrl);
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
        return serverExtras.containsKey(DataKeys.HTML_RESPONSE_BODY_KEY);
    }
}
