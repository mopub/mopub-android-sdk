// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import androidx.annotation.VisibleForTesting;

import com.mopub.mobileads.BaseAd;

import java.lang.reflect.Constructor;

public class BaseAdFactory {
    protected static BaseAdFactory instance = new BaseAdFactory();

    @Deprecated
    @VisibleForTesting
    public static void setInstance(BaseAdFactory factory) {
        instance = factory;
    }

    public static BaseAd create(String className) throws Exception {
        return instance.internalCreate(className);
    }

    protected BaseAd internalCreate(String className) throws Exception {
        Class<? extends BaseAd> baseAdClass = Class.forName(className)
                .asSubclass(BaseAd.class);
        Constructor<?> baseAdConstructor = baseAdClass.getDeclaredConstructor((Class[]) null);
        baseAdConstructor.setAccessible(true);
        return (BaseAd) baseAdConstructor.newInstance();
    }
}
