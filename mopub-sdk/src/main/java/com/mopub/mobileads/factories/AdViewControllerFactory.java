package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubView;

public class AdViewControllerFactory {
    protected static AdViewControllerFactory instance = new AdViewControllerFactory();

    @Deprecated // for testing
    public static void setInstance(AdViewControllerFactory factory) {
        instance = factory;
    }

    public static AdViewController create(Context context, MoPubView moPubView) {
        return instance.internalCreate(context, moPubView);
    }

    protected AdViewController internalCreate(Context context, MoPubView moPubView) {
        return new AdViewController(context, moPubView);
    }
}
