package com.mopub.mobileads.factories;

import com.mopub.mobileads.AdFetchTask;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.TaskTracker;

public class AdFetchTaskFactory {
    protected static AdFetchTaskFactory instance = new AdFetchTaskFactory();

    @Deprecated // for testing
    public static void setInstance(AdFetchTaskFactory factory) {
        instance = factory;
    }

    public static AdFetchTask create(TaskTracker taskTracker, AdViewController adViewController, String userAgent, int timeoutMilliseconds) {
        return instance.internalCreate(taskTracker, adViewController, userAgent, timeoutMilliseconds);
    }

    protected AdFetchTask internalCreate(TaskTracker taskTracker, AdViewController adViewController, String userAgent, int timeoutMilliseconds) {
        return new AdFetchTask(taskTracker, adViewController, userAgent, timeoutMilliseconds);
    }
}
