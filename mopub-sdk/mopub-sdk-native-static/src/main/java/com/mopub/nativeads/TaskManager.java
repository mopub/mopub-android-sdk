package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.Preconditions.NoThrow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract class TaskManager<T> {
    @NonNull protected final TaskManagerListener<T> mImageTaskManagerListener;
    protected final int mSize;
    @NonNull protected final Map<String, T> mResults;

    @NonNull protected final AtomicInteger mCompletedCount;
    @NonNull protected final AtomicBoolean mFailed;

    interface TaskManagerListener<T> {
        void onSuccess(@NonNull final Map<String, T> images);
        void onFail();
    }

    TaskManager(@NonNull final List<String> urls,
            @NonNull final TaskManagerListener<T> imageTaskManagerListener)
            throws IllegalArgumentException {
        Preconditions.checkNotNull(urls, "Urls list cannot be null");
        Preconditions.checkNotNull(imageTaskManagerListener, "ImageTaskManagerListener cannot be null");
        Preconditions.checkState(!urls.contains(null), "Urls list cannot contain null");

        mSize = urls.size();

        mImageTaskManagerListener = imageTaskManagerListener;
        mCompletedCount = new AtomicInteger(0);
        mFailed = new AtomicBoolean(false);
        mResults = Collections.synchronizedMap(new HashMap<String, T>(mSize));
    }

    abstract void execute();
}

