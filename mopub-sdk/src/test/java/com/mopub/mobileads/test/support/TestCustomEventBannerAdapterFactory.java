// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.support.annotation.NonNull;

import com.mopub.common.AdReport;
import com.mopub.mobileads.CustomEventBannerAdapter;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.factories.CustomEventBannerAdapterFactory;

import java.util.Map;

import static org.mockito.Mockito.mock;

public class TestCustomEventBannerAdapterFactory extends CustomEventBannerAdapterFactory {
    private CustomEventBannerAdapter mockCustomEventBannerAdapter = mock(CustomEventBannerAdapter.class);
    private MoPubView moPubView;
    private String className;
    private Map<String, String> classData;

    public static CustomEventBannerAdapter getSingletonMock() {
        return getTestFactory().mockCustomEventBannerAdapter;
    }

    private static TestCustomEventBannerAdapterFactory getTestFactory() {
        return ((TestCustomEventBannerAdapterFactory) instance);
    }

    @Override
    protected CustomEventBannerAdapter internalCreate(@NonNull final MoPubView moPubView,
            @NonNull final String className,
            @NonNull final Map<String, String> serverExtras,
            final long broadcastIdentifier,
            @NonNull final AdReport adReport) {
        this.moPubView = moPubView;
        this.className = className;
        this.classData = serverExtras;
        return mockCustomEventBannerAdapter;
    }

    public static MoPubView getLatestMoPubView() {
        return getTestFactory().moPubView;
    }

    public static String getLatestClassName() {
        return getTestFactory().className;
    }

    public static Map<String, String> getLatestClassData() {
        return getTestFactory().classData;
    }
}
