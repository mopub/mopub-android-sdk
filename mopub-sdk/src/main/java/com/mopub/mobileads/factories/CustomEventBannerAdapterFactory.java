package com.mopub.mobileads.factories;

import com.mopub.mobileads.CustomEventBannerAdapter;
import com.mopub.mobileads.MoPubView;

public class CustomEventBannerAdapterFactory {
    protected static CustomEventBannerAdapterFactory instance = new CustomEventBannerAdapterFactory();

    @Deprecated // for testing
    public static void setInstance(CustomEventBannerAdapterFactory factory) {
        instance = factory;
    }

    public static CustomEventBannerAdapter create(MoPubView moPubView, String className, String classData) {
        return instance.internalCreate(moPubView, className, classData);
    }

    protected CustomEventBannerAdapter internalCreate(MoPubView moPubView, String className, String classData) {
        return new CustomEventBannerAdapter(moPubView, className, classData);
    }
}
