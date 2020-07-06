// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * These are the default adapter configurations automatically initialized by the SDK.
 */
public enum DefaultAdapterClasses {
    AD_COLONY_ADAPTER_CONFIGURATION("com.mopub.mobileads.AdColonyAdapterConfiguration"),
    APPLOVIN_ADAPTER_CONFIGURATION("com.mopub.mobileads.AppLovinAdapterConfiguration"),
    CHARTBOOST_ADAPTER_CONFIGURATION("com.mopub.mobileads.ChartboostAdapterConfiguration"),
    FACEBOOK_ADAPTER_CONFIGURATION("com.mopub.mobileads.FacebookAdapterConfiguration"),
    IRON_SOURCE_ADAPTER_CONFIGURATION("com.mopub.mobileads.IronSourceAdapterConfiguration"),
    GOOGLE_PLAY_SERVICES_ADAPTER_CONFIGURATION("com.mopub.mobileads.GooglePlayServicesAdapterConfiguration"),
    TAPJOY_ADAPTER_CONFIGURATION("com.mopub.mobileads.TapjoyAdapterConfiguration"),
    UNITY_ADS_ADAPTER_CONFIGURATION("com.mopub.mobileads.UnityAdsAdapterConfiguration"),
    VERIZON_ADAPTER_CONFIGURATION("com.mopub.mobileads.VerizonAdapterConfiguration"),
    VUNGLE_ADAPTER_CONFIGURATION("com.mopub.mobileads.VungleAdapterConfiguration"),
    MINTEGRAL_ADAPTER_CONFIGURATION("com.mopub.mobileads.MintegralAdapterConfiguration");
;
    private final String mClassName;

    DefaultAdapterClasses(@NonNull final String className) {
        mClassName = className;
    }

    public static Set<String> getClassNamesSet() {
        final Set<String> adapterConfigurations = new HashSet<>();
        for (final DefaultAdapterClasses adapterConfiguration : DefaultAdapterClasses.values()) {
            adapterConfigurations.add(adapterConfiguration.mClassName);
        }
        return adapterConfigurations;
    }
}
