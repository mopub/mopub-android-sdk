// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.mopub.mobileads.AdData;
import com.mopub.mobileads.AdAdapter;
import com.mopub.mobileads.AdLifecycleListener;
import com.mopub.mobileads.FullscreenAdAdapter;
import com.mopub.mobileads.MoPubAd;

public class FullscreenAdAdapterFactory {
    protected static FullscreenAdAdapterFactory instance = new FullscreenAdAdapterFactory();

    @Deprecated
    @VisibleForTesting
    public static void setInstance(FullscreenAdAdapterFactory factory) {
        instance = factory;
    }

    public static FullscreenAdAdapter create(Context context,
                                             String className,
                                             AdData adData) throws AdAdapter.BaseAdNotFoundException {
        return instance.internalCreate(context, className, adData);
    }

    protected FullscreenAdAdapter internalCreate(Context context,
                                                 String className,
                                                 AdData adData) throws AdAdapter.BaseAdNotFoundException {
        return new FullscreenAdAdapter(context, className, adData);
    }
}
