package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.mobileads.VastManager;

public class VastManagerFactory {
    protected static VastManagerFactory instance = new VastManagerFactory();

    public static VastManager create(final Context context) {
        return instance.internalCreate(context, true);
    }

    public static VastManager create(final Context context, boolean preCacheVideo) {
        return instance.internalCreate(context, preCacheVideo);
    }

    public VastManager internalCreate(final Context context, boolean preCacheVideo) {
        return new VastManager(context, preCacheVideo);
    }

    @Deprecated // for testing
    public static void setInstance(VastManagerFactory factory) {
        instance = factory;
    }
}
