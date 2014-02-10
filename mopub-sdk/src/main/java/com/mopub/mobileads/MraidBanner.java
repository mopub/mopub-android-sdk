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

import android.content.Context;
import android.net.Uri;
import com.mopub.mobileads.MraidView.ViewState;
import com.mopub.mobileads.factories.MraidViewFactory;
import com.mopub.mobileads.util.Views;

import java.util.*;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.MRAID_LOAD_ERROR;
import static com.mopub.mobileads.MraidView.MraidListener;

class MraidBanner extends CustomEventBanner {
    private MraidView mMraidView;
    private CustomEventBannerListener mBannerListener;

    @Override
    protected void loadBanner(Context context,
                    CustomEventBannerListener customEventBannerListener,
                    Map<String, Object> localExtras,
                    Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        String htmlData;
        if (extrasAreValid(serverExtras)) {
            htmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
        } else {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        AdConfiguration adConfiguration = AdConfiguration.extractFromMap(localExtras);
        mMraidView = MraidViewFactory.create(context, adConfiguration);
        mMraidView.loadHtmlData(htmlData);
        initMraidListener();
    }

    @Override
    protected void onInvalidate() {
        if (mMraidView != null) {
            resetMraidListener();
            Views.removeFromParent(mMraidView);
            mMraidView.destroy();
        }
    }

    private void onReady() {
        mBannerListener.onBannerLoaded(mMraidView);
    }

    private void onFail() {
        mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
    }

    private void onExpand() {
        mBannerListener.onBannerExpanded();
        mBannerListener.onBannerClicked();
    }

    private void onClose() {
        mBannerListener.onBannerCollapsed();
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }

    private void initMraidListener() {
        mMraidView.setMraidListener(new MraidListener() {
            public void onReady(MraidView view) {
                MraidBanner.this.onReady();
            }
            public void onFailure(MraidView view) {
                onFail();
            }
            public void onExpand(MraidView view) {
                MraidBanner.this.onExpand();
            }
            public void onClose(MraidView view, ViewState newViewState) {
                MraidBanner.this.onClose();
            }
        });
    }

    private void resetMraidListener() {
        mMraidView.setMraidListener(null);
    }
}
