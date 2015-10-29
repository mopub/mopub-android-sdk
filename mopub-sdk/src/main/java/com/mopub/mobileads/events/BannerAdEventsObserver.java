package com.mopub.mobileads.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shad on 14.10.15.
 */
public class BannerAdEventsObserver {
	private static BannerAdEventsObserver instance;

	public static BannerAdEventsObserver instance() {
		if (instance == null) {
			synchronized (BannerAdEventsObserver.class) {
				if (instance == null) {
					instance = new BannerAdEventsObserver();
				}
			}
		}
		return instance;
	}

	private final List<BannerAdEventsListener> listeners;

	public BannerAdEventsObserver() {
		listeners = new ArrayList<>(4);
	}

	public void addListener(BannerAdEventsListener listener) {
		listeners.add(listener);
	}

	public void removeListener(BannerAdEventsListener listener) {
		listeners.remove(listener);
	}

	public void onAdLeftApplication(BannerAdType adType) {
		for (BannerAdEventsListener listener : listeners) {
			listener.onBannerAdLeftApplication(adType);
		}
	}

	public void onAdLoaded(BannerAdType adType, String pubId) {
		for (BannerAdEventsListener listener : listeners) {
			listener.onBannerAdLoaded(adType, pubId);
		}
	}

	public void onAdFailedToLoad(BannerAdType adType) {
		for (BannerAdEventsListener listener : listeners) {
			listener.onBannerAdFailedToLoad(adType);
		}
	}
}
