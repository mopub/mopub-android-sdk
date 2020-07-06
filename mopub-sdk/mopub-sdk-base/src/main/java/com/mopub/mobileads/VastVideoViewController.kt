// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity.CENTER
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.CENTER_IN_PARENT
import android.widget.RelativeLayout.TRUE
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.SessionPlayer
import androidx.media2.common.SessionPlayer.*
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import androidx.media2.player.PlaybackParams
import androidx.media2.widget.VideoView
import com.mopub.common.DataKeys
import com.mopub.common.ExternalViewabilitySession.VideoEvent
import com.mopub.common.ExternalViewabilitySessionManager
import com.mopub.common.IntentActions
import com.mopub.common.MoPubBrowser.MOPUB_BROWSER_REQUEST_CODE
import com.mopub.common.VisibleForTesting
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.common.util.AsyncTasks
import com.mopub.common.util.Dips
import com.mopub.mobileads.factories.MediaPlayerFactory
import com.mopub.mobileads.factories.VideoViewFactory
import com.mopub.mobileads.resource.DrawableConstants.PrivacyInfoIcon.LEFT_MARGIN_DIPS
import com.mopub.mobileads.resource.DrawableConstants.PrivacyInfoIcon.TOP_MARGIN_DIPS
import com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest

@Mockable
class VastVideoViewController(
    val activity: Activity,
    val extras: Bundle,
    val savedInstanceState: Bundle?,
    broadcastIdentifier: Long,
    baseListener: BaseVideoViewControllerListener
) : BaseVideoViewController(activity, broadcastIdentifier, baseListener) {

    companion object {
        const val VAST_VIDEO_CONFIG = "vast_video_config"
        const val CURRENT_POSITION = "current_position"
        const val RESUMED_VAST_CONFIG = "resumed_vast_config"
        const val WEBVIEW_PADDING = 16
        const val DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON = 5 * 1000
        const val MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON = 16 * 1000

        private const val VIDEO_PROGRESS_TIMER_CHECKER_DELAY: Long = 50
        private const val VIDEO_COUNTDOWN_UPDATE_INTERVAL: Long = 250
        private const val SEEKER_POSITION_NOT_INITIALIZED = -1
    }

    private val videoView: VideoView

    val mediaPlayer = MediaPlayerFactory.create(context)
    val playerCallback = PlayerCallback()

    private var seekerPositionOnPause = SEEKER_POSITION_NOT_INITIALIZED
    private var vastCompanionAdConfig: VastCompanionAdConfig? = null
    @VisibleForTesting
    val vastVideoConfig: VastVideoConfig
    @VisibleForTesting
    val vastIconConfig: VastIconConfig?
    private val externalViewabilitySessionManager = ExternalViewabilitySessionManager(activity)
    @VisibleForTesting
    val blurredLastVideoFrameImageView: ImageView
    @VisibleForTesting
    val landscapeCompanionAdView: View
    @VisibleForTesting
    val portraitCompanionAdView: View
    @VisibleForTesting
    val iconView: View

    private val progressCheckerRunnable: VastVideoViewProgressRunnable
    private val countdownRunnable: VastVideoViewCountdownRunnable
    @VisibleForTesting
    val clickThroughListener: OnTouchListener

    @VisibleForTesting
    lateinit var topGradientStripWidget: VastVideoGradientStripWidget
    @VisibleForTesting
    lateinit var bottomGradientStripWidget: VastVideoGradientStripWidget
    @VisibleForTesting
    lateinit var progressBarWidget: VastVideoProgressBarWidget
    @VisibleForTesting
    lateinit var radialCountdownWidget: RadialCountdownWidget
    @VisibleForTesting
    val ctaButtonWidget: VastVideoCtaButtonWidget
    @VisibleForTesting
    lateinit var closeButtonWidget: VastVideoCloseButtonWidget

    @VisibleForTesting
    var blurLastVideoFrameTask: VastVideoBlurLastVideoFrameTask? = null
    @VisibleForTesting
    var mediaMetadataRetriever: MediaMetadataRetriever? = MediaMetadataRetriever()

    @VisibleForTesting
    var isComplete: Boolean = false
    @VisibleForTesting
    var shouldAllowClose: Boolean = false
    private var isInClickExperiment: Boolean = false
    @VisibleForTesting
    var showCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON
    @VisibleForTesting
    var isCalibrationDone: Boolean = false
    @VisibleForTesting
    var isClosing: Boolean = false

    var videoError: Boolean = false
    val networkMediaFileUrl get() = vastVideoConfig.networkMediaFileUrl

    init {
        val resumed =
            (savedInstanceState?.getSerializable(RESUMED_VAST_CONFIG) as? VastVideoConfig)

        val adData = extras.getParcelable<AdData>(DataKeys.AD_DATA_KEY)
        vastVideoConfig = resumed
            ?: requireNotNull(
                VastVideoConfig.fromVastVideoConfigString(
                    requireNotNull(
                        requireNotNull(adData) {
                            "AdData is invalid"
                        }.vastVideoConfigString
                    ) {
                        "VastVideoConfigByteArray is null"
                    })) {
                "VastVideoConfig is invalid"
            }


        seekerPositionOnPause = resumed?.let {
            savedInstanceState?.getInt(CURRENT_POSITION, SEEKER_POSITION_NOT_INITIALIZED)
        } ?: SEEKER_POSITION_NOT_INITIALIZED

        requireNotNull(vastVideoConfig.diskMediaFileUrl) {
            "VastVideoConfig does not have a video disk path"
        }

        vastCompanionAdConfig = vastVideoConfig.getVastCompanionAd(
            activity.resources.configuration.orientation
        )
        vastIconConfig = vastVideoConfig.vastIconConfig
        isInClickExperiment = vastVideoConfig.enableClickExperiment


        clickThroughListener = OnTouchListener { _, event ->
            if (event.action == ACTION_UP && (shouldAllowClose || isInClickExperiment)) {
                externalViewabilitySessionManager.recordVideoEvent(
                    VideoEvent.AD_CLICK_THRU,
                    getCurrentPosition()
                )
                isClosing = !isInClickExperiment || isComplete
                broadcastAction(IntentActions.ACTION_FULLSCREEN_CLICK)
                vastVideoConfig.handleClickForResult(activity,
                    getDuration().takeIf { isComplete } ?: getCurrentPosition(),
                    MOPUB_BROWSER_REQUEST_CODE)
            }
            true
        }

        layout.setBackgroundColor(Color.BLACK)

        blurredLastVideoFrameImageView = ImageView(context).also {
            it.visibility = INVISIBLE
            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            layout.addView(it, layoutParams)
            it.setOnTouchListener(clickThroughListener)
        }

        // Video view
        videoView = createVideoView(activity, VISIBLE)
        videoView.requestFocus()

        externalViewabilitySessionManager.createVideoSession(activity, videoView, vastVideoConfig)
        externalViewabilitySessionManager.registerVideoObstruction(blurredLastVideoFrameImageView)

        landscapeCompanionAdView =
            vastVideoConfig.createCompanionAdView(ORIENTATION_LANDSCAPE, INVISIBLE)
        portraitCompanionAdView =
            vastVideoConfig.createCompanionAdView(ORIENTATION_PORTRAIT, INVISIBLE)

        // Top transparent gradient strip overlaying top of screen
        val hasCompanionAd = vastCompanionAdConfig != null
        topGradientStripWidget = VastVideoGradientStripWidget(
            context,
            GradientDrawable.Orientation.TOP_BOTTOM,
            hasCompanionAd,
            VISIBLE,
            RelativeLayout.ALIGN_TOP,
            layout.id
        ).also {
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)
        }

        // Progress bar overlaying bottom of video view
        progressBarWidget = VastVideoProgressBarWidget(context).also {
            it.setAnchorId(videoView.id)
            it.visibility = INVISIBLE
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)
        }

        // Bottom transparent gradient strip above progress bar
        bottomGradientStripWidget = VastVideoGradientStripWidget(
            context,
            GradientDrawable.Orientation.BOTTOM_TOP,
            hasCompanionAd,
            GONE,
            RelativeLayout.ABOVE,
            progressBarWidget.id
        ).also {
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)
        }

        // Radial countdown timer snapped to top-right corner of screen
        radialCountdownWidget = RadialCountdownWidget(context).also {
            it.visibility = INVISIBLE
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)
        }

        iconView = vastIconConfig?.let { iconConfig ->
            VastWebView.createView(context, iconConfig.vastResource).also {
                it.vastWebViewClickListener = VastWebView.VastWebViewClickListener {
                    makeVastTrackingHttpRequest(
                        iconConfig.clickTrackingUris,
                        null,
                        getCurrentPosition(),
                        networkMediaFileUrl,
                        context
                    )
                    vastIconConfig?.handleClick(context, null, vastVideoConfig.dspCreativeId)
                }
                it.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        url: String
                    ): Boolean {
                        vastIconConfig?.handleClick(
                            context,
                            url,
                            vastVideoConfig.dspCreativeId
                        )
                        return true
                    }
                }
                it.visibility = INVISIBLE
                val layoutParams = vastIconConfig?.let {
                    iconConfig
                    RelativeLayout.LayoutParams(
                        Dips.asIntPixels(iconConfig.width.toFloat(), context),
                        Dips.asIntPixels(iconConfig.height.toFloat(), context)
                    )
                }
                val leftMargin = Dips.dipsToIntPixels(LEFT_MARGIN_DIPS.toFloat(), context)
                val topMargin = Dips.dipsToIntPixels(TOP_MARGIN_DIPS.toFloat(), context)
                layoutParams?.setMargins(leftMargin, topMargin, 0, 0)

                layout.addView(it, layoutParams)
                externalViewabilitySessionManager.registerVideoObstruction(it)
            }
        } ?: View(context)

        ctaButtonWidget = VastVideoCtaButtonWidget(
            context,
            videoView.id,
            hasCompanionAd,
            !vastVideoConfig.clickThroughUrl.isNullOrEmpty()
        ).also {
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)
            vastVideoConfig.customCtaText?.let { ctaText ->
                it.updateCtaText(ctaText)
            }
            it.setOnTouchListener(clickThroughListener)
        }

        closeButtonWidget = VastVideoCloseButtonWidget(context).also {
            it.visibility = GONE
            layout.addView(it)
            externalViewabilitySessionManager.registerVideoObstruction(it)

            it.setOnTouchListenerToContent(OnTouchListener { _, event ->
                if (event.action != ACTION_UP) {
                    return@OnTouchListener true
                }

                isClosing = !isInClickExperiment || isComplete
                handleExitTrackers()
                baseVideoViewControllerListener.onFinish()
                return@OnTouchListener true
            })
            vastVideoConfig.customSkipText?.let { skipText ->
                it.updateCloseButtonText(skipText)
            }
            vastVideoConfig.customCloseIconUrl?.let { closeIcon ->
                it.updateCloseButtonIcon(closeIcon)
            }
        }

        if (isInClickExperiment && !vastVideoConfig.isRewarded) {
            // This makes the CTA button clickable immediately
            ctaButtonWidget.notifyVideoSkippable()
        }

        val mainHandler = Handler(Looper.getMainLooper())
        progressCheckerRunnable = VastVideoViewProgressRunnable(
            this,
            vastVideoConfig,
            mainHandler
        )
        countdownRunnable = VastVideoViewCountdownRunnable(this, mainHandler)
    }

    private fun adjustSkipOffset() {
        val videoDuration = getDuration()
        // If this is a rewarded video, never allow it to be skippable.
        if (vastVideoConfig.isRewarded) {
            showCloseButtonDelay = videoDuration
            return
        }
        // Default behavior: video is non-skippable if duration < 16 seconds
        if (videoDuration < MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON) {
            showCloseButtonDelay = videoDuration
        }
        // Override if skipoffset attribute is specified in VAST
        try {
            vastVideoConfig.getSkipOffsetMillis(videoDuration)?.let {
                showCloseButtonDelay = it
            }
        } catch (e: NumberFormatException) {
            MoPubLog.log(CUSTOM, "Failed to parse skipoffset ${vastVideoConfig.skipOffset}")
        }
    }

    private fun createVideoView(context: Context, initialVisibility: Int): VideoView {
        val tempVideoView = VideoViewFactory.create(context)
        val executor = ContextCompat.getMainExecutor(context)

        val playbackParams = PlaybackParams.Builder()
            .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
            .setSpeed(1.0f)
            .build()
        mediaPlayer.playbackParams = playbackParams
        val audioAttrs = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
            .build()
        mediaPlayer.setAudioAttributes(audioAttrs)
        mediaPlayer.registerPlayerCallback(executor, playerCallback)
        tempVideoView.removeView(tempVideoView.mediaControlView)
        tempVideoView.id = generateViewId()
        tempVideoView.setPlayer(mediaPlayer)
        tempVideoView.setOnTouchListener(clickThroughListener)

        mediaPlayer.run {
            setMediaItem(
                UriMediaItem.Builder(Uri.parse(vastVideoConfig.diskMediaFileUrl)).build()
            )
            prepare().addListener(
                Runnable {
                    // Called when media source is ready for playback
                    // The VideoView duration defaults to -1 when the video is not prepared or playing;
                    // Therefore set it here so that we have access to it at all times
                    externalViewabilitySessionManager.onVideoPrepared(layout, duration.toInt())
                    adjustSkipOffset()
                    mediaPlayer.playerVolume = 1.0f

                    if (vastCompanionAdConfig == null) {
                        vastVideoConfig.diskMediaFileUrl?.let {
                            prepareBlurredLastVideoFrame(blurredLastVideoFrameImageView, it)
                        }
                    }
                    progressBarWidget.calibrateAndMakeVisible(
                        duration.toInt(),
                        showCloseButtonDelay
                    )
                    radialCountdownWidget.calibrateAndMakeVisible(showCloseButtonDelay)
                    isCalibrationDone = true
                },
                executor
            )

        }

        return tempVideoView
    }

    fun VastVideoConfig.createCompanionAdView(
        orientation: Int,
        initialVisibility: Int
    ): View {
        return this.getVastCompanionAd(orientation)?.let { companionConfig ->
            val relativeLayout = RelativeLayout(context)
            val layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            relativeLayout.gravity = CENTER
            layout.addView(relativeLayout, layoutParams)
            externalViewabilitySessionManager.registerVideoObstruction(relativeLayout)

            companionConfig.createWebView().also { wv ->
                wv.visibility = initialVisibility
                val wvLayout = RelativeLayout.LayoutParams(
                    Dips.dipsToIntPixels(
                        (companionConfig.width + WEBVIEW_PADDING).toFloat(),
                        context
                    ),
                    Dips.dipsToIntPixels(
                        (companionConfig.height + WEBVIEW_PADDING).toFloat(),
                        context
                    )
                )
                wvLayout.addRule(CENTER_IN_PARENT, TRUE)
                relativeLayout.addView(wv, wvLayout)
                externalViewabilitySessionManager.registerVideoObstruction(wv)
            }
        } ?: View(context).also { it.visibility = INVISIBLE }
    }

    fun VastCompanionAdConfig.createWebView(): VastWebView {
        return VastWebView.createView(context, vastResource).also {
            it.vastWebViewClickListener = VastWebView.VastWebViewClickListener {
                broadcastAction(IntentActions.ACTION_FULLSCREEN_CLICK)
                isClosing = true
                // We only should track and handleClick for non-null, non-empty clickthrough urls
                if (!this.clickThroughUrl.isNullOrEmpty()) {
                    makeVastTrackingHttpRequest(
                        this.clickTrackers,
                        null,
                        getCurrentPosition(),
                        null,
                        context
                    )
                    this.handleClick(
                        context,
                        MOPUB_BROWSER_REQUEST_CODE,
                        null,
                        vastVideoConfig.dspCreativeId
                    )
                }
            }
            it.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    vastCompanionAdConfig?.handleClick(
                        context,
                        MOPUB_BROWSER_REQUEST_CODE,
                        url,
                        vastVideoConfig.dspCreativeId
                    )
                    return true
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        vastVideoConfig.handleImpression(context, getCurrentPosition())
    }

    override fun onResume() {
        startRunnables()

        if (seekerPositionOnPause > 0) {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_PLAYING,
                seekerPositionOnPause
            )
            mediaPlayer.seekTo(seekerPositionOnPause.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_LOADED,
                getDuration()
            )
            if (!isComplete) {
                mediaPlayer.play()
            }
        }

        if (seekerPositionOnPause != SEEKER_POSITION_NOT_INITIALIZED && !isComplete) {
            vastVideoConfig.handleResume(context, seekerPositionOnPause)
        }
    }

    @SuppressLint("RestrictedApi", "VisibleForTests")
    override fun onPause() {
        stopRunnables()
        seekerPositionOnPause = getCurrentPosition()
        mediaPlayer.pause()
        // To address a bug where a video may resume after a transient audio loss, we close the
        // focus handler here in onPause() as we manage play and pause manually
        mediaPlayer.audioFocusHandler.close()

        if (!isClosing) {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_PAUSED,
                getCurrentPosition()
            )
            vastVideoConfig.handlePause(context, seekerPositionOnPause)
        }
    }

    override fun onDestroy() {
        stopRunnables()
        externalViewabilitySessionManager.recordVideoEvent(
            VideoEvent.AD_STOPPED,
            getCurrentPosition()
        )
        externalViewabilitySessionManager.endVideoSession()
        broadcastAction(IntentActions.ACTION_FULLSCREEN_DISMISS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_POSITION, seekerPositionOnPause)
        outState.putSerializable(RESUMED_VAST_CONFIG, vastVideoConfig)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        vastCompanionAdConfig = context.resources.configuration.orientation.let {
            var companionAdConfig: VastCompanionAdConfig? = null
            if (landscapeCompanionAdView.visibility == VISIBLE
                || portraitCompanionAdView.visibility == VISIBLE
            ) {
                companionAdConfig = vastVideoConfig.getVastCompanionAd(it)
                if (it == ORIENTATION_PORTRAIT) {
                    landscapeCompanionAdView.visibility = INVISIBLE
                    portraitCompanionAdView.visibility = VISIBLE
                } else {
                    landscapeCompanionAdView.visibility = VISIBLE
                    portraitCompanionAdView.visibility = INVISIBLE
                }
            }
            companionAdConfig?.also { config ->
                config.handleImpression(context, getDuration())
            }
        }
    }

    override fun getVideoView(): View {
        return videoView
    }

    override fun onBackPressed() {
        handleExitTrackers()
    }

    // Enable the device's back button when the video close button has been displayed
    override fun backButtonEnabled(): Boolean {
        return shouldAllowClose
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (isClosing
            && requestCode == MOPUB_BROWSER_REQUEST_CODE
            && resultCode == Activity.RESULT_OK
        ) {
            baseVideoViewControllerListener.onFinish()
        }
    }

    internal fun handleViewabilityQuartileEvent(enumValue: String) {
        VideoEvent.valueOf(enumValue)?.let {
            externalViewabilitySessionManager.recordVideoEvent(it, getCurrentPosition())
        }
    }

    fun getDuration(): Int {
        return mediaPlayer.duration.toInt()
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer.currentPosition.toInt()
    }

    fun updateCountdown(forceCloseable: Boolean = false) {
        if (isCalibrationDone) {
            radialCountdownWidget.updateCountdownProgress(
                showCloseButtonDelay,
                getCurrentPosition()
            )
        }

        if (forceCloseable || getCurrentPosition() >= showCloseButtonDelay) {
            radialCountdownWidget.visibility = GONE
            closeButtonWidget.visibility = VISIBLE
            shouldAllowClose = true

            if (isInClickExperiment || !vastVideoConfig.isRewarded) {
                // This makes the CTA button clickable - both rewarded and non-rewarded
                ctaButtonWidget.notifyVideoSkippable()
            }
        }
    }

    fun updateProgressBar() {
        progressBarWidget.updateProgress(getCurrentPosition())
    }

    /**
     * Displays and impresses the icon if the current position of the video is greater than the
     * offset of the icon. Once the current position is greater than the offset plus duration, the
     * icon is then hidden again.
     *
     * @param currentPosition the current position of the video in milliseconds.
     */
    fun handleIconDisplay(currentPosition: Int) {
        val offsetMs = vastIconConfig?.offsetMS ?: return
        if (currentPosition < offsetMs) {
            return
        }

        iconView.visibility = VISIBLE
        networkMediaFileUrl?.let {
            vastIconConfig?.handleImpression(context, currentPosition, it)
        }

        val durationMS = vastIconConfig?.durationMS ?: return

        if (currentPosition >= offsetMs + durationMS) {
            iconView.visibility = GONE
        }
    }

    fun prepareBlurredLastVideoFrame(
        blurredLastVideoFrameImageView: ImageView,
        diskMediaFileUrl: String
    ) {
        mediaMetadataRetriever?.let { it ->
            blurLastVideoFrameTask = VastVideoBlurLastVideoFrameTask(
                it,
                blurredLastVideoFrameImageView,
                getDuration()
            ).also { task ->
                AsyncTasks.safeExecuteOnExecutor(
                    task,
                    diskMediaFileUrl
                )
            }
        }
    }

    private fun handleExitTrackers() {
        val currentPosition: Int = getCurrentPosition()
        val duration: Int = getDuration()
        if (isComplete || currentPosition >= duration) {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_COMPLETE,
                duration
            )
            vastVideoConfig.handleComplete(context, duration)
        } else {
            externalViewabilitySessionManager.recordVideoEvent(
                VideoEvent.AD_COMPLETE,
                currentPosition
            )
            vastVideoConfig.handleSkip(context, currentPosition)
        }

        vastVideoConfig.handleClose(context, duration)
    }

    private fun startRunnables() {
        progressCheckerRunnable.startRepeating(VIDEO_PROGRESS_TIMER_CHECKER_DELAY)
        countdownRunnable.startRepeating(VIDEO_COUNTDOWN_UPDATE_INTERVAL)
    }

    private fun stopRunnables() {
        progressCheckerRunnable.stop()
        countdownRunnable.stop()
        blurLastVideoFrameTask?.run {
            if (status != AsyncTask.Status.FINISHED) {
                cancel(true)
            }
        }
    }

    inner class PlayerCallback : MediaPlayer.PlayerCallback() {
        var complete = false
        override fun onPlayerStateChanged(player: SessionPlayer, playerState: Int) {
            super.onPlayerStateChanged(player, playerState)

            when (playerState) {
                PLAYER_STATE_ERROR -> {
                    externalViewabilitySessionManager.recordVideoEvent(
                        VideoEvent.RECORD_AD_ERROR,
                        getCurrentPosition()
                    )
                    stopRunnables()
                    updateCountdown(true)
                    videoError(false)
                    videoError = true
                    vastVideoConfig.handleError(
                        context,
                        VastErrorCode.GENERAL_LINEAR_AD_ERROR, getCurrentPosition()
                    )
                }
                else -> {
                    MoPubLog.log(
                        CUSTOM,
                        "Player state changed to ${playerStateToString(playerState)}"
                    )
                }
            }
        }

        override fun onPlaybackCompleted(player: SessionPlayer) {
            stopRunnables()
            updateCountdown()
            isComplete = true
            videoCompleted(false)
            if (vastVideoConfig.isRewarded) {
                broadcastAction(IntentActions.ACTION_REWARDED_AD_COMPLETE)
            }

            if (!videoError && vastVideoConfig.remainingProgressTrackerCount == 0) {
                externalViewabilitySessionManager.recordVideoEvent(
                    VideoEvent.AD_COMPLETE,
                    getCurrentPosition()
                )
                vastVideoConfig.handleComplete(context, getCurrentPosition())
            }

            videoView.visibility = INVISIBLE
            progressBarWidget.visibility = GONE
            iconView.visibility = GONE

            topGradientStripWidget.notifyVideoComplete()
            bottomGradientStripWidget.notifyVideoComplete()
            ctaButtonWidget.notifyVideoComplete()
            closeButtonWidget.notifyVideoComplete()

            // Show companion ad if available
            vastCompanionAdConfig?.let {
                val orientation = context.resources.configuration.orientation
                if (orientation == ORIENTATION_PORTRAIT) {
                    portraitCompanionAdView.visibility = VISIBLE
                } else {
                    landscapeCompanionAdView.visibility = VISIBLE
                }
                it.handleImpression(context, getDuration())
            } ?: if (blurredLastVideoFrameImageView.drawable != null) {
                // If there is no companion ad, show blurred last video frame with dark overlay
                blurredLastVideoFrameImageView.visibility = VISIBLE
            }
        }

        override fun onSeekCompleted(player: SessionPlayer, position: Long) {
            mediaPlayer.play()
        }

        private fun playerStateToString(state: Int): String {
            return when (state) {
                PLAYER_STATE_IDLE -> "PLAYER_STATE_IDLE"
                PLAYER_STATE_PAUSED -> "PLAYER_STATE_PAUSED"
                PLAYER_STATE_PLAYING -> "PLAYER_STATE_PLAYING"
                PLAYER_STATE_ERROR -> "PLAYER_STATE_ERROR"
                else -> "UNKNOWN"
            }
        }
    }
}
