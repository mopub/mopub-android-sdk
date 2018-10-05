// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.AdReport;
import com.mopub.mobileads.HtmlBannerWebView;

import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;

public class HtmlBannerWebViewFactory {
    protected static HtmlBannerWebViewFactory instance = new HtmlBannerWebViewFactory();

    @NonNull
    public static HtmlBannerWebView create(
            Context context,
            AdReport adReport,
            CustomEventBannerListener customEventBannerListener,
            String clickthroughUrl) {
        return instance.internalCreate(context, adReport, customEventBannerListener, clickthroughUrl);
    }

    public HtmlBannerWebView internalCreate(
            Context context,
            AdReport adReport,
            CustomEventBannerListener customEventBannerListener,
            String clickthroughUrl) {
        HtmlBannerWebView htmlBannerWebView = new HtmlBannerWebView(context, adReport);
        htmlBannerWebView.init(customEventBannerListener, clickthroughUrl, adReport.getDspCreativeId());
        return htmlBannerWebView;
    }

    @Deprecated // for testing
    public static void setInstance(HtmlBannerWebViewFactory factory) {
        instance = factory;
    }
}

