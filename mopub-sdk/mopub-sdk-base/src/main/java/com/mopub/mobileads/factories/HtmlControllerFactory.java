// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.HtmlController;

public class HtmlControllerFactory {
    protected static HtmlControllerFactory instance = new HtmlControllerFactory();

    @VisibleForTesting
    public static void setInstance(HtmlControllerFactory factory) {
        instance = factory;
    }

    public static HtmlController create(@NonNull final Context context,
                                        @Nullable final String dspCreativeId,
                                        @Nullable final String clickthroughUrl) {
        return instance.internalCreate(context, dspCreativeId, clickthroughUrl);
    }

    protected HtmlController internalCreate(@NonNull final Context context,
                                            @Nullable final String dspCreativeId,
                                            @Nullable final String clickthroughUrl) {
        return new HtmlController(context, dspCreativeId, clickthroughUrl);
    }
}
