package com.mopub.nativeads.factories;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.nativeads.CustomEventNative;
import com.mopub.nativeads.MoPubCustomEventNative;

import java.lang.reflect.Constructor;

public class CustomEventNativeFactory {
    protected static CustomEventNativeFactory instance = new CustomEventNativeFactory();

    public static CustomEventNative create(@Nullable final String className) throws Exception {
        if (className != null) {
            final Class<? extends CustomEventNative> nativeClass = Class.forName(className)
                    .asSubclass(CustomEventNative.class);
            return instance.internalCreate(nativeClass);
        } else {
            return new MoPubCustomEventNative();
        }
    }

    @Deprecated // for testing
    public static void setInstance(
            @NonNull final CustomEventNativeFactory customEventNativeFactory) {
        Preconditions.checkNotNull(customEventNativeFactory);

        instance = customEventNativeFactory;
    }

    @NonNull
    protected CustomEventNative internalCreate(
            @NonNull final Class<? extends CustomEventNative> nativeClass) throws Exception {
        Preconditions.checkNotNull(nativeClass);

        final Constructor<?> nativeConstructor = nativeClass.getDeclaredConstructor((Class[]) null);
        nativeConstructor.setAccessible(true);
        return (CustomEventNative) nativeConstructor.newInstance();
    }
}
