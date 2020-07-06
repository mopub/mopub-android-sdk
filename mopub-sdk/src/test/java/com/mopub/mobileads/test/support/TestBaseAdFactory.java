package com.mopub.mobileads.test.support;

import androidx.annotation.NonNull;

import com.mopub.mobileads.BaseAd;
import com.mopub.mobileads.factories.BaseAdFactory;

import static org.mockito.Mockito.mock;

public class TestBaseAdFactory extends BaseAdFactory {
    private BaseAd mockBaseAd = mock(BaseAd.class);
    private String className;

    public static BaseAd getSingletonMock() {
        return getTestFactory().mockBaseAd;
    }

    private static TestBaseAdFactory getTestFactory() {
        return ((TestBaseAdFactory) instance);
    }

    @Override
    protected BaseAd internalCreate(@NonNull String className) throws Exception {
        this.className = className;
        return mockBaseAd;
    }

    public static String getLatestClassName() {
        return getTestFactory().className;
    }
}
