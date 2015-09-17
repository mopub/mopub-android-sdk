package com.mopub.mobileads;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.VideoView;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Streams;

import java.io.File;
import java.io.FileInputStream;

/**
 * Custom VideoView dedicated for VAST videos. This primarily deals with the blurring of the last
 * frame when there's no companion ad and retrying the video.
 */
public class VastVideoView extends VideoView {

    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;

    @Nullable private VastVideoBlurLastVideoFrameTask mBlurLastVideoFrameTask;
    @Nullable private MediaMetadataRetriever mMediaMetadataRetriever;
    private int mVideoRetries;

    public VastVideoView(@NonNull final Context context) {
        super(context);
        Preconditions.checkNotNull(context, "context cannot be null");
        mMediaMetadataRetriever = createMediaMetadataRetriever();
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

    boolean retryMediaPlayer(final MediaPlayer mediaPlayer, final int what, final int extra,
            @NonNull final String diskMediaFileUrl) {
        // XXX
        // VideoView has a bug in versions lower than Jelly Bean, Api Level 16, Android 4.1
        // For api < 16, VideoView is not able to read files written to disk since it reads them in
        // a Context different from the Application and therefore does not have correct permission.
        // To solve this problem we obtain the video file descriptor ourselves with valid permissions
        // and pass it to the underlying MediaPlayer in VideoView.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                && what == MediaPlayer.MEDIA_ERROR_UNKNOWN
                && extra == VIDEO_VIEW_FILE_PERMISSION_ERROR
                && mVideoRetries < MAX_VIDEO_RETRIES) {

            FileInputStream inputStream = null;
            try {
                mediaPlayer.reset();
                final File file = new File(diskMediaFileUrl);
                inputStream = new FileInputStream(file);
                mediaPlayer.setDataSource(inputStream.getFD());

                // XXX
                // VideoView has a callback registered with the MediaPlayer to set a flag when the
                // media file has been prepared. Start also sets a flag in VideoView indicating the
                // desired state is to play the video. Therefore, whichever method finishes last
                // will check both flags and begin playing the video.
                mediaPlayer.prepareAsync();
                start();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                Streams.closeStream(inputStream);
                mVideoRetries++;
            }
        }
        return false;
    }

    /**
     * Called when the activity enclosing this view is resumed.
     */
    public void onResume() {
        // When resuming, VideoView needs to reinitialize its MediaPlayer with the video path
        // and therefore reset the count to zero, to let it retry on error
        mVideoRetries = 0;
    }

    @VisibleForTesting
    @Nullable
    MediaMetadataRetriever createMediaMetadataRetriever() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            return new MediaMetadataRetriever();
        }

        return null;
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

    // for testing
    @Deprecated
    @VisibleForTesting
    int getVideoRetries() {
        return mVideoRetries;
    }
}
