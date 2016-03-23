package com.mopub.mobileads.events;

/**
 * Created by Shad on 14.10.15.
 */
public enum BannerAdType {
	AdMob("AdMob"),
	Mopub("Mopub"),
	Facebook("Facebook"),
	Millennial("Millenial"),
	Inneractive("Inneractive"),
	Amazon("Amazon"),
	Flurry("Flurry");

	private String name;

	BannerAdType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static BannerAdType fromAdapterClass(String className) {
		if (className == null) {
			return Mopub;
		}
		switch (className) {
			case "com.mopub.mobileads.AdMobGeneric": {
				return AdMob;
			}
			case "com.mopub.mobileads.AdMobWCPMFloor": {
				return AdMob;
			}
			case "com.mopub.mobileads.AmazonBanner": {
				return Amazon;
			}
			case "com.mopub.mobileads.FacebookBanner": {
				return Facebook;
			}
			case "com.mopub.mobileads.GooglePlayServicesBanner": {
				return AdMob;
			}
			case "com.mopub.mobileads.InneractiveBanner": {
				return Inneractive;
			}
			case "com.mopub.mobileads.MillennialBanner": {
				return Millennial;
			}
			case "com.mopub.mobileads.FlurryCustomEventBanner": {
				return Flurry;
			}
			default: {
				return Mopub;
			}
		}
	}
}
