// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.os.Handler
import com.mopub.common.ExternalViewabilitySession.VideoEvent
import com.mopub.network.TrackingRequest
import java.util.*

/**
 * A runnable that is used to measure video progress and track video progress events for video ads.
 *
 */
@Mockable
class VastVideoViewProgressRunnable(
    private val videoViewController: VastVideoViewController,
    private val vastVideoConfig: VastVideoConfig,
    handler: Handler
) : RepeatingHandlerRunnable(handler) {

    override fun doWork() {
        val videoLength = videoViewController.getDuration()
        val currentPosition = videoViewController.getCurrentPosition()
        videoViewController.updateProgressBar()

        if (videoLength <= 0) {
            return
        }

        vastVideoConfig.getUntriggeredTrackersBefore(currentPosition, videoLength).mapNotNull {
            it.setTracked()
            when (it.messageType) {
                VastTracker.MessageType.TRACKING_URL -> {
                    it.content
                }
                VastTracker.MessageType.QUARTILE_EVENT -> {
                    videoViewController.handleViewabilityQuartileEvent(it.content)
                    null
                }
            }
        }.takeIf { it.isNotEmpty() }?.also {
            TrackingRequest.makeTrackingHttpRequest(
                VastMacroHelper(it)
                    .withAssetUri(videoViewController.networkMediaFileUrl)
                    .withContentPlayHead(currentPosition)
                    .uris,
                videoViewController.context
            )
        }

        videoViewController.handleIconDisplay(currentPosition)
    }

    init {
        // Keep track of quartile measurement for ExternalViewabilitySessions
        val trackers: MutableList<VastFractionalProgressTracker> =
            ArrayList()
        trackers.add(
            VastFractionalProgressTracker.Builder(
                VideoEvent.AD_STARTED.name,
                0f
            ).messageType(VastTracker.MessageType.QUARTILE_EVENT).build()
        )
        trackers.add(
            VastFractionalProgressTracker.Builder(
                VideoEvent.AD_IMPRESSED.name,
                0f
            ).messageType(VastTracker.MessageType.QUARTILE_EVENT).build()
        )
        trackers.add(
            VastFractionalProgressTracker.Builder(
                VideoEvent.AD_VIDEO_FIRST_QUARTILE.name,
                0.25f
            ).messageType(VastTracker.MessageType.QUARTILE_EVENT).build()
        )
        trackers.add(
            VastFractionalProgressTracker.Builder(
                VideoEvent.AD_VIDEO_MIDPOINT.name,
                0.5f
            ).messageType(VastTracker.MessageType.QUARTILE_EVENT).build()
        )
        trackers.add(
            VastFractionalProgressTracker.Builder(
                VideoEvent.AD_VIDEO_THIRD_QUARTILE.name,
                0.75f
            ).messageType(VastTracker.MessageType.QUARTILE_EVENT).build()
        )
        vastVideoConfig.addFractionalTrackers(trackers)
    }
}
