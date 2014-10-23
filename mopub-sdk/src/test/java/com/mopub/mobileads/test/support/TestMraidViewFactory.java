package com.mopub.mobileads.test.support;

import android.content.Context;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.MraidView;
import com.mopub.mobileads.factories.MraidViewFactory;

import static org.mockito.Mockito.mock;

public class TestMraidViewFactory extends MraidViewFactory {
    private final MraidView mockMraidView = mock(MraidView.class);

    public static MraidView getSingletonMock() {
        return getTestFactory().mockMraidView;
    }

    private static TestMraidViewFactory getTestFactory() {
        return (TestMraidViewFactory) instance;
    }

    @Override
    protected MraidView internalCreate(Context context, AdConfiguration adConfiguration) {
        return mockMraidView;
    }

    @Override
    protected MraidView internalCreate(Context context, AdConfiguration adConfiguration, MraidView.ExpansionStyle expansionStyle, MraidView.NativeCloseButtonStyle buttonStyle, MraidView.PlacementType placementType) {
        return mockMraidView;
    }
}
