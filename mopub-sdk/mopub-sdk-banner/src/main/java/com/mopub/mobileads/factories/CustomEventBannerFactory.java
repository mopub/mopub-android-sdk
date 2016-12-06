package com.mopub.mobileads.factories;

import com.mopub.mobileads.CustomEventBanner;

import java.lang.reflect.Constructor;

public class CustomEventBannerFactory {
    private static CustomEventBannerFactory instance = new CustomEventBannerFactory();

    public static CustomEventBanner create(String className) throws Exception {
        return instance.internalCreate(className);
    }

    @Deprecated // for testing
    public static void setInstance(CustomEventBannerFactory factory) {
        instance = factory;
    }

    protected CustomEventBanner internalCreate(String className) throws Exception {
        Class<? extends CustomEventBanner> bannerClass = Class.forName(className)
                .asSubclass(CustomEventBanner.class);
        Constructor<?> bannerConstructor = bannerClass.getDeclaredConstructor((Class[]) null);
        bannerConstructor.setAccessible(true);
        return (CustomEventBanner) bannerConstructor.newInstance();
    }
}
