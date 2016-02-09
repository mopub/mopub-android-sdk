package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.MoPubView;

public class MoPubViewFactory {
    protected static MoPubViewFactory instance = new MoPubViewFactory();

    @VisibleForTesting
    @Deprecated
    public static void setInstance(MoPubViewFactory factory) {
        instance = factory;
    }

    public static MoPubView create(Context context) {
        return instance.internalCreate(context);
    }

    protected MoPubView internalCreate(Context context) {
        return new MoPubView(context);
    }
}
