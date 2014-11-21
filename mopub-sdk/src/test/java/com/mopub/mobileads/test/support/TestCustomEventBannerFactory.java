package com.mopub.mobileads.test.support;

import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import static org.mockito.Mockito.mock;

public class TestCustomEventBannerFactory extends CustomEventBannerFactory{
    private CustomEventBanner instance = mock(CustomEventBanner.class);

    @Override
    protected CustomEventBanner internalCreate(String className) {
        return instance;
    }
}
