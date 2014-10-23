package com.mopub.mobileads.test.support;

import com.mopub.mobileads.AdFetchTask;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.TaskTracker;
import com.mopub.mobileads.factories.AdFetchTaskFactory;

import static org.mockito.Mockito.mock;

public class TestAdFetchTaskFactory extends AdFetchTaskFactory {
    private AdFetchTask mockAdFetchTask = mock(AdFetchTask.class);

    public static AdFetchTask getSingletonMock() {
        return getTestFactory().mockAdFetchTask;
    }

    private static TestAdFetchTaskFactory getTestFactory() {
        return ((TestAdFetchTaskFactory) AdFetchTaskFactory.instance);
    }

    @Override
    protected AdFetchTask internalCreate(TaskTracker taskTracker, AdViewController adViewController, String userAgent, int timeoutMilliseconds) {
        return mockAdFetchTask;
    }
}
