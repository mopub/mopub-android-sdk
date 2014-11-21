package com.mopub.common.util.test.support;

import com.mopub.common.factories.MethodBuilderFactory;

import static com.mopub.common.util.Reflection.MethodBuilder;
import static org.mockito.Mockito.mock;

public class TestMethodBuilderFactory extends MethodBuilderFactory {
    private MethodBuilder mockMethodBuilder = mock(MethodBuilder.class);

    public static MethodBuilder getSingletonMock() {
        return getTestFactory().mockMethodBuilder;
    }

    private static TestMethodBuilderFactory getTestFactory() {
        return ((TestMethodBuilderFactory) MethodBuilderFactory.instance);
    }

    @Override
    public MethodBuilder internalCreate(Object object, String methodName) {
        return mockMethodBuilder;
    }
}

