package com.mopub.mobileads.test.support;

import com.mopub.mobileads.CustomEventBannerAdapter;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.factories.CustomEventBannerAdapterFactory;

import static org.mockito.Mockito.mock;

public class TestCustomEventBannerAdapterFactory extends CustomEventBannerAdapterFactory {
    private CustomEventBannerAdapter mockCustomEventBannerAdapter = mock(CustomEventBannerAdapter.class);
    private MoPubView moPubView;
    private String className;
    private String classData;

    public static CustomEventBannerAdapter getSingletonMock() {
        return getTestFactory().mockCustomEventBannerAdapter;
    }

    private static TestCustomEventBannerAdapterFactory getTestFactory() {
        return ((TestCustomEventBannerAdapterFactory) instance);
    }

    @Override
    protected CustomEventBannerAdapter internalCreate(MoPubView moPubView, String className, String classData) {
        this.moPubView = moPubView;
        this.className = className;
        this.classData = classData;
        return mockCustomEventBannerAdapter;
    }

    public static MoPubView getLatestMoPubView() {
        return getTestFactory().moPubView;
    }

    public static String getLatestClassName() {
        return getTestFactory().className;
    }

    public static String getLatestClassData() {
        return getTestFactory().classData;
    }
}
