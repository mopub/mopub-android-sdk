package com.mopub.mobileads.test.support;

import com.mopub.mobileads.VastVideoDownloadTask;
import com.mopub.mobileads.factories.VastVideoDownloadTaskFactory;

import static org.mockito.Mockito.mock;

public class TestVastVideoDownloadTaskFactory extends VastVideoDownloadTaskFactory {
    private static VastVideoDownloadTask singletonMock = mock(VastVideoDownloadTask.class);

    public static VastVideoDownloadTask getSingletonMock() {
        return singletonMock;
    }

    @Override
    protected VastVideoDownloadTask internalCreate(VastVideoDownloadTask.VastVideoDownloadTaskListener vastVideoDownloadTaskListener) {
        return singletonMock;
    }
}
