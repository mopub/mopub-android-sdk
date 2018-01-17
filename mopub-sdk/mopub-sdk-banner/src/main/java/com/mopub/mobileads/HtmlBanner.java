package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class HtmlBanner extends CustomEventBanner {
    private HtmlBannerWebView mHtmlBannerWebView;
    @Nullable private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;

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

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(activity);
            mExternalViewabilitySessionManager.createDisplaySession(activity, mHtmlBannerWebView);
        } else {
            MoPubLog.d("Unable to start viewability session for HTML banner: Context provided was not an Activity.");
        }

        mHtmlBannerWebView.loadHtmlResponse(htmlData);
    }

    @Override
    protected void onInvalidate() {
        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }

        if (mHtmlBannerWebView != null) {
            mHtmlBannerWebView.destroy();
        }
    }

    @Override
    protected void trackMpxAndThirdPartyImpressions() {
        mHtmlBannerWebView.loadUrl(WEB_VIEW_DID_APPEAR.getUrl());
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(DataKeys.HTML_RESPONSE_BODY_KEY);
    }
}
