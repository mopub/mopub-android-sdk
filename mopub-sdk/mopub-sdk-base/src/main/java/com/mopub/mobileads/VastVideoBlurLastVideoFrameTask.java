package com.mopub.mobileads;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ImageUtils;
import com.mopub.mobileads.resource.DrawableConstants;

public class VastVideoBlurLastVideoFrameTask extends AsyncTask<String, Void, Boolean> {

    private static final int MICROSECONDS_PER_MILLISECOND = 1000;

    /**
     * Gets a frame this many microseconds from the end of the video.
     */
    private static final int OFFSET_IN_MICROSECONDS = 200000;

    @NonNull private final MediaMetadataRetriever mMediaMetadataRetriever;
    @NonNull private final ImageView mBlurredLastVideoFrameImageView;
    private int mVideoDuration;
    @Nullable private Bitmap mLastVideoFrame;
    @Nullable private Bitmap mBlurredLastVideoFrame;

    public VastVideoBlurLastVideoFrameTask(
            @NonNull final MediaMetadataRetriever mediaMetadataRetriever,
            @NonNull final ImageView blurredLastVideoFrameImageView, int videoDuration) {
        mMediaMetadataRetriever = mediaMetadataRetriever;
        mBlurredLastVideoFrameImageView = blurredLastVideoFrameImageView;
        mVideoDuration = videoDuration;
    }

    @Override
    protected Boolean doInBackground(String... videoPaths) {
        if (videoPaths == null || videoPaths.length == 0 || videoPaths[0] == null) {
            return false;
        }

        try {
            final String videoPath = videoPaths[0];

            mMediaMetadataRetriever.setDataSource(videoPath);

            // This actually gets a frame just before the video ends. If we try to get a frame
            // that's actually past the end of the video or before 0, this will pick some
            // arbitrary frame.
            mLastVideoFrame = mMediaMetadataRetriever.getFrameAtTime(
                    mVideoDuration * MICROSECONDS_PER_MILLISECOND - OFFSET_IN_MICROSECONDS,
                    MediaMetadataRetriever.OPTION_CLOSEST);

            if (mLastVideoFrame == null) {
                return false;
            }

            mBlurredLastVideoFrame = ImageUtils.applyFastGaussianBlurToBitmap(
                    mLastVideoFrame, 4);

            return true;
        } catch (Exception e) {
            MoPubLog.d("Failed to blur last video frame", e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (isCancelled()) {
            onCancelled();
            return;
        }

        if (success != null && success) {
            mBlurredLastVideoFrameImageView.setImageBitmap(mBlurredLastVideoFrame);
            mBlurredLastVideoFrameImageView.setImageAlpha(DrawableConstants.BlurredLastVideoFrame
                    .ALPHA);
        }
    }

    @Override
    protected void onCancelled() {
        MoPubLog.d("VastVideoBlurLastVideoFrameTask was cancelled.");
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    Bitmap getBlurredLastVideoFrame() {
        return mBlurredLastVideoFrame;
    }
}
