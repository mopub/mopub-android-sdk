package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.network.TrackingRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A runnable that is used to measure video progress and track video progress events for video ads.
 *
 */
public class VastVideoViewProgressRunnable extends RepeatingHandlerRunnable {

    @NonNull private final VastVideoViewController mVideoViewController;
    @NonNull private final VastVideoConfig mVastVideoConfig;

    public VastVideoViewProgressRunnable(@NonNull VastVideoViewController videoViewController,
            @NonNull final VastVideoConfig vastVideoConfig,
            @NonNull Handler handler) {
        super(handler);

        Preconditions.checkNotNull(videoViewController);
        Preconditions.checkNotNull(vastVideoConfig);
        mVideoViewController = videoViewController;
        mVastVideoConfig = vastVideoConfig;
    }

    @Override
    public void doWork() {
        int videoLength = mVideoViewController.getDuration();
        int currentPosition = mVideoViewController.getCurrentPosition();

        mVideoViewController.updateProgressBar();

        if (videoLength > 0) {
            final List<VastTracker> trackersToTrack =
                    mVastVideoConfig.getUntriggeredTrackersBefore(currentPosition, videoLength);
            if (!trackersToTrack.isEmpty()) {
                final List<String> trackUrls = new ArrayList<String>();
                for (VastTracker tracker : trackersToTrack) {
                    trackUrls.add(tracker.getTrackingUrl());
                    tracker.setTracked();
                }
                TrackingRequest.makeTrackingHttpRequest(
                        new VastMacroHelper(trackUrls)
                                .withAssetUri(mVideoViewController.getNetworkMediaFileUrl())
                                .withContentPlayHead(currentPosition)
                                .getUris(),
                        mVideoViewController.getContext());
            }

            mVideoViewController.handleIconDisplay(currentPosition);
        }
    }
}
