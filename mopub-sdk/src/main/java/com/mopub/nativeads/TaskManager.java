package com.mopub.nativeads;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract class TaskManager<T> {
    protected final TaskManagerListener<T> mImageTaskManagerListener;
    protected final int mSize;
    protected final Map<String, T> mResults;

    protected final AtomicInteger mCompletedCount;
    protected final AtomicBoolean mFailed;

    interface TaskManagerListener<T> {
        void onSuccess(final Map<String, T> images);
        void onFail();
    }

    TaskManager(final List<String> urls, final TaskManagerListener<T> imageTaskManagerListener)
            throws IllegalArgumentException {
        if (urls == null) {
            throw new IllegalArgumentException("Urls list cannot be null");
        } else if (urls.contains(null)) {
            throw new IllegalArgumentException("Urls list cannot contain null");
        } else if (imageTaskManagerListener == null) {
            throw new IllegalArgumentException("ImageTaskManagerListener cannot be null");
        }

        mSize = urls.size();

        mImageTaskManagerListener = imageTaskManagerListener;
        mCompletedCount = new AtomicInteger(0);
        mFailed = new AtomicBoolean(false);
        mResults = Collections.synchronizedMap(new HashMap<String, T>(mSize));
    }

    abstract void execute();
}

