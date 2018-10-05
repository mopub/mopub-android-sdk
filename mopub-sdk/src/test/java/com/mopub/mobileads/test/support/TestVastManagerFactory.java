// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.content.Context;

import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.mobileads.VastManager;

import static org.mockito.Mockito.mock;

public class TestVastManagerFactory extends VastManagerFactory {
    private VastManager mockVastManager = mock(VastManager.class);

    public static VastManager getSingletonMock() {
        return getTestFactory().mockVastManager;
    }

    private static TestVastManagerFactory getTestFactory() {
        return (TestVastManagerFactory) instance;
    }

    @Override
    public VastManager internalCreate(final Context context, final boolean preCacheVideo) {
        return getTestFactory().mockVastManager;
    }
}