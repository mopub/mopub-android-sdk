// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import com.mopub.mobileads.CustomEventInterstitial;

import java.lang.reflect.Constructor;

public class CustomEventInterstitialFactory {
    private static CustomEventInterstitialFactory instance = new CustomEventInterstitialFactory();

    public static CustomEventInterstitial create(String className) throws Exception {
        return instance.internalCreate(className);
    }

    @Deprecated // for testing
    public static void setInstance(CustomEventInterstitialFactory factory) {
        instance = factory;
    }

    protected CustomEventInterstitial internalCreate(String className) throws Exception {
        Class<? extends CustomEventInterstitial> interstitialClass = Class.forName(className)
                .asSubclass(CustomEventInterstitial.class);
        Constructor<?> interstitialConstructor = interstitialClass.getDeclaredConstructor((Class[]) null);
        interstitialConstructor.setAccessible(true);
        return (CustomEventInterstitial) interstitialConstructor.newInstance();
    }
}
