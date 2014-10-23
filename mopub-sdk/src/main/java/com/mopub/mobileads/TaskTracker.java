package com.mopub.mobileads;

public class TaskTracker {
    private long mCurrentTaskId = -1l;
    private long mLastCompletedTaskId;

    public long getCurrentTaskId() {
        return mCurrentTaskId;
    }

    public void newTaskStarted() {
        mCurrentTaskId++;
    }

    public void markTaskCompleted(long taskId) {
        if (taskId > mLastCompletedTaskId) {
            mLastCompletedTaskId = taskId;
        }
    }

    public boolean isMostCurrentTask(long taskId) {
        return taskId >= mLastCompletedTaskId;
    }
}
