package com.mopub.mobileads.test.support;

import com.mopub.mobileads.CustomEventInterstitialAdapter;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.factories.CustomEventInterstitialAdapterFactory;

import static org.mockito.Mockito.mock;

public class TestCustomEventInterstitialAdapterFactory extends CustomEventInterstitialAdapterFactory{
    private CustomEventInterstitialAdapter mockCustomEventInterstitalAdapter = mock(CustomEventInterstitialAdapter.class);
    private MoPubInterstitial latestMoPubInterstitial;
    private String latestClassName;
    private String latestClassData;

    public static CustomEventInterstitialAdapter getSingletonMock() {
        return getTestFactory().mockCustomEventInterstitalAdapter;
    }

    private static TestCustomEventInterstitialAdapterFactory getTestFactory() {
        return ((TestCustomEventInterstitialAdapterFactory)instance);
    }

    public static MoPubInterstitial getLatestMoPubInterstitial() {
        return getTestFactory().latestMoPubInterstitial;
    }

    public static String getLatestClassName() {
        return getTestFactory().latestClassName;
    }

    public static String getLatestClassData() {
        return getTestFactory().latestClassData;
    }

    @Override
    protected CustomEventInterstitialAdapter internalCreate(MoPubInterstitial moPubInterstitial, String className, String classData) {
        latestMoPubInterstitial = moPubInterstitial;
        latestClassName = className;
        latestClassData = classData;
        return mockCustomEventInterstitalAdapter;
    }
}
