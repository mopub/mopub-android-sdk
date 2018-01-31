package com.mopub.mobileads;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.VideoView;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;

/**
 * Custom VideoView dedicated for VAST videos. This primarily deals with the blurring of the last
 * frame when there's no companion ad and retrying the video.
 */
public class VastVideoView extends VideoView {

    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;

    @Nullable private VastVideoBlurLastVideoFrameTask mBlurLastVideoFrameTask;
    @Nullable private MediaMetadataRetriever mMediaMetadataRetriever;

    public VastVideoView(@NonNull final Context context) {
        super(context);
        Preconditions.checkNotNull(context, "context cannot be null");
        mMediaMetadataRetriever = new MediaMetadataRetriever();
    }

    /**
     * Launches an async task to blur the last frame of the video. If the API of the device is not
     * high enough, this does nothing.
     *
     * @param blurredLastVideoFrameImageView The view will get populated with the image when the
     *                                       async task is finished.
     */
    public void prepareBlurredLastVideoFrame(
            @NonNull final ImageView blurredLastVideoFrameImageView,
            @NonNull final String diskMediaFileUrl) {
        if (mMediaMetadataRetriever != null) {
            mBlurLastVideoFrameTask = new VastVideoBlurLastVideoFrameTask(mMediaMetadataRetriever,
                    blurredLastVideoFrameImageView, getDuration());

            try {
                AsyncTasks.safeExecuteOnExecutor(
                        mBlurLastVideoFrameTask,
                        diskMediaFileUrl
                );
            } catch (Exception e) {
                MoPubLog.d("Failed to blur last video frame", e);
            }
        }
    }

    /**
     * Called when the activity enclosing this view is destroyed. We do not want to continue this
     * task when the activity expecting the result no longer exists.
     */
    public void onDestroy() {
        if (mBlurLastVideoFrameTask != null &&
                mBlurLastVideoFrameTask.getStatus() != AsyncTask.Status.FINISHED) {
            mBlurLastVideoFrameTask.cancel(true);
        }
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setMediaMetadataRetriever(@NonNull MediaMetadataRetriever mediaMetadataRetriever) {
        mMediaMetadataRetriever = mediaMetadataRetriever;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    @Nullable
    VastVideoBlurLastVideoFrameTask getBlurLastVideoFrameTask() {
        return mBlurLastVideoFrameTask;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setBlurLastVideoFrameTask(@NonNull VastVideoBlurLastVideoFrameTask blurLastVideoFrameTask) {
        mBlurLastVideoFrameTask = blurLastVideoFrameTask;
    }

}
