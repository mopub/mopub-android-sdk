// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads.test.support;

import android.support.annotation.NonNull;

import com.mopub.nativeads.CustomEventNative;
import com.mopub.nativeads.factories.CustomEventNativeFactory;

import static org.mockito.Mockito.mock;

public class TestCustomEventNativeFactory extends CustomEventNativeFactory {
    private CustomEventNative instance = mock(CustomEventNative.class);

    public static CustomEventNative getSingletonMock() {
        return getTestFactory().instance;
    }

    private static TestCustomEventNativeFactory getTestFactory() {
        return ((TestCustomEventNativeFactory) CustomEventNativeFactory.instance);
    }

    @Override
    protected CustomEventNative internalCreate(@NonNull final Class<? extends CustomEventNative> nativeClass) {
        return instance;
    }
}
