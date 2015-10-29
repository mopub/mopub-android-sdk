package com.mopub.nativeads.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shad on 14.10.15.
 */
public class NativeAdEventsObserver {
	private static NativeAdEventsObserver instance;

	public static NativeAdEventsObserver instance() {
		if (instance == null) {
			synchronized (NativeAdEventsObserver.class) {
				if (instance == null) {
					instance = new NativeAdEventsObserver();
				}
			}
		}
		return instance;
	}

	private final List<NativeAdEventsListener> listeners;

	public NativeAdEventsObserver() {
		listeners = new ArrayList<>(4);
	}

	public void addListener(NativeAdEventsListener listener) {
		listeners.add(listener);
	}

	public void removeListener(NativeAdEventsListener listener) {
		listeners.remove(listener);
	}

	public void onAdClicked(NativeAdType adType) {
		for (NativeAdEventsListener listener : listeners) {
			listener.onNativeAdClicked(adType);
		}
	}

	public void onAdImpressed(NativeAdType adType) {
		for (NativeAdEventsListener listener : listeners) {
			listener.onNativeAdImpressed(adType);
		}
	}
}
