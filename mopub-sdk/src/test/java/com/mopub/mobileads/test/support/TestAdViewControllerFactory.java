package com.mopub.mobileads.test.support;

import android.content.Context;

import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.factories.AdViewControllerFactory;

import static org.mockito.Mockito.mock;

public class TestAdViewControllerFactory extends AdViewControllerFactory {
    private AdViewController mockAdViewController = mock(AdViewController.class);

    public static AdViewController getSingletonMock() {
        return getTestFactory().mockAdViewController;
    }

    private static TestAdViewControllerFactory getTestFactory() {
        return ((TestAdViewControllerFactory) instance);
    }

    @Override
    protected AdViewController internalCreate(Context context, MoPubView moPubView) {
        return mockAdViewController;
    }
}
