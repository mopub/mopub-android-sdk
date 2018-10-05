// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.AdReport;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;

import static org.mockito.Mockito.mock;

public class TestMraidControllerFactory extends MraidControllerFactory {
    private MraidController mockMraidController = mock(MraidController.class);

    public static MraidController getSingletonMock() {
        return getTestFactory().mockMraidController;
    }

    private static TestMraidControllerFactory getTestFactory() {
        return ((TestMraidControllerFactory) MraidControllerFactory.instance);
    }

    @Override
    protected MraidController internalCreate(@NonNull final Context context,
            @NonNull AdReport adReport,
            @NonNull final PlacementType placementType) {
        return mockMraidController;
    }
}
