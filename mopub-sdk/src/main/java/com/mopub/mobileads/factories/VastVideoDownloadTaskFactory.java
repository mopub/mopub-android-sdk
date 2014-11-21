package com.mopub.mobileads.factories;

import com.mopub.mobileads.VastVideoDownloadTask;

public class VastVideoDownloadTaskFactory {
    private static VastVideoDownloadTaskFactory instance = new VastVideoDownloadTaskFactory();

    @Deprecated // for testing
    public static void setInstance(VastVideoDownloadTaskFactory factory){
        instance = factory;
    }

    public static VastVideoDownloadTask create(VastVideoDownloadTask.VastVideoDownloadTaskListener vastVideoDownloadTaskListener) {
        return instance.internalCreate(vastVideoDownloadTaskListener);
    }

    protected VastVideoDownloadTask internalCreate(VastVideoDownloadTask.VastVideoDownloadTaskListener vastVideoDownloadTaskListener) {
        return new VastVideoDownloadTask(vastVideoDownloadTaskListener);
    }
}
