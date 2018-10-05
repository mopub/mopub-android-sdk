// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.factories.CustomEventInterstitialFactory;

import static org.mockito.Mockito.mock;

public class TestCustomEventInterstitialFactory extends CustomEventInterstitialFactory {
    private CustomEventInterstitial instance = mock(CustomEventInterstitial.class);

    @Override
    protected CustomEventInterstitial internalCreate(String className) {
        return instance;
    }
}
