// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.factories;

import static com.mopub.common.util.Reflection.MethodBuilder;

public class MethodBuilderFactory {
    protected static MethodBuilderFactory instance = new MethodBuilderFactory();

    @Deprecated // for testing
    public static void setInstance(MethodBuilderFactory factory) {
        instance = factory;
    }

    public static MethodBuilder create(Object object, String methodName) {
        return instance.internalCreate(object, methodName);
    }

    protected MethodBuilder internalCreate(Object object, String methodName) {
        return new MethodBuilder(object, methodName);
    }
}

