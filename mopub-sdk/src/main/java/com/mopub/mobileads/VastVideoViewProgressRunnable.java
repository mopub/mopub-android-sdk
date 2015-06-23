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
    public VastVideoViewProgressRunnable(@NonNull VastVideoViewController videoViewController,
            @NonNull Handler handler) {
        super(handler);

        Preconditions.checkNotNull(videoViewController);
        mVideoViewController = videoViewController;
    }

    @Override
    public void doWork() {
        int videoLength = mVideoViewController.getDuration();
        int currentPosition = mVideoViewController.getCurrentPosition();

        if (videoLength > 0) {
            final List<VastTracker> trackersToTrack =
                    mVideoViewController.getUntriggeredTrackersBefore(currentPosition, videoLength);
            if (!trackersToTrack.isEmpty()) {
                final List<String> trackUrls = new ArrayList<String>();
                for (VastTracker tracker : trackersToTrack) {
                    trackUrls.add(tracker.getTrackingUrl());
                    tracker.setTracked();
                }
                TrackingRequest.makeTrackingHttpRequest(trackUrls, mVideoViewController.getContext());
            }

        }
    }
}
