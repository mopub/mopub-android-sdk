// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

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
