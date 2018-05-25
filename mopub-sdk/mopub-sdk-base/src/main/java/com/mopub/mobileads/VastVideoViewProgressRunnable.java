package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.ExternalViewabilitySession.VideoEvent;
import com.mopub.common.Preconditions;
import com.mopub.mobileads.VastTracker.MessageType;
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

        // Keep track of quartile measurement for ExternalViewabilitySessions
        final List<VastFractionalProgressTracker> trackers =
                new ArrayList<VastFractionalProgressTracker>();
        trackers.add(new VastFractionalProgressTracker(MessageType.QUARTILE_EVENT,
                VideoEvent.AD_STARTED.name(), 0f));
        trackers.add(new VastFractionalProgressTracker(MessageType.QUARTILE_EVENT,
                VideoEvent.AD_IMPRESSED.name(), 0f));
        trackers.add(new VastFractionalProgressTracker(MessageType.QUARTILE_EVENT,
                VideoEvent.AD_VIDEO_FIRST_QUARTILE.name(), 0.25f));
        trackers.add(new VastFractionalProgressTracker(MessageType.QUARTILE_EVENT,
                VideoEvent.AD_VIDEO_MIDPOINT.name(), 0.5f));
        trackers.add(new VastFractionalProgressTracker(MessageType.QUARTILE_EVENT,
                VideoEvent.AD_VIDEO_THIRD_QUARTILE.name(), 0.75f));
        mVastVideoConfig.addFractionalTrackers(trackers);
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
                    if (tracker.getMessageType() == MessageType.TRACKING_URL) {
                        trackUrls.add(tracker.getContent());
                    } else if (tracker.getMessageType() == MessageType.QUARTILE_EVENT) {
                        mVideoViewController.handleViewabilityQuartileEvent(tracker.getContent());
                    }
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
