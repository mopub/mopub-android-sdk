// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import static com.mopub.mobileads.MoPubView.BannerAdListener;

public class DefaultBannerAdListener implements BannerAdListener {
    @Override public void onBannerLoaded(MoPubView banner) { }
    @Override public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) { }
    @Override public void onBannerClicked(MoPubView banner) { }
    @Override public void onBannerExpanded(MoPubView banner) { }
    @Override public void onBannerCollapsed(MoPubView banner) { }
}
