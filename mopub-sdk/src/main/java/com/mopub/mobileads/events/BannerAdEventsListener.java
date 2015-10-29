package com.mopub.mobileads.events;

/**
 * Created by Shad on 14.10.15.
 */
public interface BannerAdEventsListener {
	void onBannerAdLeftApplication(BannerAdType adType);

	void onBannerAdLoaded(BannerAdType adType,String pubId);

	void onBannerAdFailedToLoad(BannerAdType adType);
}
