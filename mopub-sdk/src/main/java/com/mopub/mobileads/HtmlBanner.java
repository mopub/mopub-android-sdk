/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;

import com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;

import java.util.*;

import static com.mopub.mobileads.AdFetcher.CLICKTHROUGH_URL_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.AdFetcher.REDIRECT_URL_KEY;
import static com.mopub.mobileads.AdFetcher.SCROLLABLE_KEY;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class HtmlBanner extends CustomEventBanner {

    private HtmlBannerWebView mHtmlBannerWebView;
    private Activity mActivity;

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
        if (Looper.myLooper() == Looper.getMainLooper()) {
        	mHtmlBannerWebView = HtmlBannerWebViewFactory.create(context, customEventBannerListener, isScrollable, redirectUrl, clickthroughUrl, adConfiguration);
            AdViewController.setShouldHonorServerDimensions(mHtmlBannerWebView);
            mHtmlBannerWebView.loadHtmlResponse(htmlData);
        } else {
        	final Context pContext = context;
            final CustomEventBannerListener pCustomEventBannerListener = customEventBannerListener;
            final Boolean pIsScrollable = isScrollable;
            final String pRedirectUrl = redirectUrl;
            final String pClickthroughUrl = clickthroughUrl;
            final AdConfiguration pAdConfiguration = adConfiguration;
            final String pHtmlData = htmlData;
            mActivity = (Activity)context; 
            mActivity.runOnUiThread(new Runnable() {
    			@Override
    			public void run() {
    				mHtmlBannerWebView = HtmlBannerWebViewFactory.create(pContext, pCustomEventBannerListener, pIsScrollable, pRedirectUrl, pClickthroughUrl, pAdConfiguration);
    		        AdViewController.setShouldHonorServerDimensions(mHtmlBannerWebView);
    		        mHtmlBannerWebView.loadHtmlResponse(pHtmlData);
    			}
    		});
        }
    }

    @Override
    protected void onInvalidate() {
        if (mHtmlBannerWebView != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
        		mHtmlBannerWebView.destroy();
        	} else {
        		mActivity.runOnUiThread(new Runnable() {
    				@Override
    				public void run() {
    					mHtmlBannerWebView.destroy();
    				}
    			});
        	}
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
