package com.mopub.nativeads.events;

/**
 * Created by Shad on 14.10.15.
 */
public interface NativeAdEventsListener {
	void onNativeAdClicked(NativeAdType adType);
	void onNativeAdImpressed(NativeAdType adType);
	void onNativeAdRequested(NativeAdType adType);
	void onNativeAdLoadSuccess(NativeAdType adType);
}
