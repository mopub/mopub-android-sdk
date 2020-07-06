// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mopub.mobileads.AdAdapter;
import com.mopub.mobileads.AdData;
import com.mopub.mobileads.FullscreenAdAdapter;
import com.mopub.mobileads.InlineAdAdapter;
import com.mopub.mobileads.factories.FullscreenAdAdapterFactory;

import static org.mockito.Mockito.mock;

public class TestFullscreenAdAdapterFactory extends FullscreenAdAdapterFactory {
    private FullscreenAdAdapter mockFullscreenAdAdapter = mock(FullscreenAdAdapter.class);
    private String className;
    private AdData adData;

    public static FullscreenAdAdapter getSingletonMock() {
        return getTestFactory().mockFullscreenAdAdapter;
    }

    private static TestFullscreenAdAdapterFactory getTestFactory() {
        return ((TestFullscreenAdAdapterFactory) instance);
    }

    @Override
    protected FullscreenAdAdapter internalCreate(@NonNull Context context,
                                             @NonNull String className,
                                             @NonNull AdData adData) throws AdAdapter.BaseAdNotFoundException {
        this.className = className;
        this.adData = adData;
        return mockFullscreenAdAdapter;
    }

    public static String getLatestClassName() {
        return getTestFactory().className;
    }

    public static AdData getLatestAdData() {
        return getTestFactory().adData;
    }
}
