// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.mopub.common.Constants
import com.mopub.common.MoPubBrowser
import com.mopub.common.UrlAction
import com.mopub.common.UrlHandler
import com.mopub.common.UrlHandler.ResultActions
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.common.logging.MoPubLog.log
import com.mopub.common.util.Intents
import com.mopub.exceptions.IntentNotResolvableException
import com.mopub.mobileads.VideoTrackingEvent.*
import com.mopub.mobileads.VideoTrackingEvent.Companion.fromString
import com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.Serializable
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min

@Mockable
class VastVideoConfig : Serializable {

    companion object {
        private const val serialVersionUID: Long = 3L

        @Throws(IOException::class, ClassNotFoundException::class)
        fun fromVastVideoConfigString(input: String): VastVideoConfig {
            val gson = GsonBuilder()
                .registerTypeAdapterFactory(VastVideoConfigTypeAdapterFactory())
                .create()
            return gson.fromJson(input, VastVideoConfig::class.java)
        }
    }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_IMPRESSION)
    private val _impressionTrackers = mutableListOf<VastTracker>()
    val impressionTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_impressionTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_PAUSE)
    private val _pauseTrackers = mutableListOf<VastTracker>()
    val pauseTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_pauseTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_RESUME)
    private val _resumeTrackers = mutableListOf<VastTracker>()
    val resumeTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_resumeTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_COMPLETE)
    private val _completeTrackers = mutableListOf<VastTracker>()
    val completeTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_completeTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_CLOSE)
    private val _closeTrackers = mutableListOf<VastTracker>()
    val closeTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_closeTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_SKIP)
    private val _skipTrackers = mutableListOf<VastTracker>()
    val skipTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_skipTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_CLICK)
    private val _clickTrackers = mutableListOf<VastTracker>()
    val clickTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_clickTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_ERROR)
    private val _errorTrackers = mutableListOf<VastTracker>()
    val errorTrackers: ArrayList<VastTracker>
        get() {
            return ArrayList(_errorTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_FRACTIONAL)
    private val _fractionalTrackers = mutableListOf<VastFractionalProgressTracker>()
    val fractionalTrackers: ArrayList<VastFractionalProgressTracker>
        get() {
            return ArrayList(_fractionalTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_TRACKERS_ABSOLUTE)
    private val _absoluteTrackers = mutableListOf<VastAbsoluteProgressTracker>()
    val absoluteTrackers: ArrayList<VastAbsoluteProgressTracker>
        get() {
            return ArrayList(_absoluteTrackers)
        }

    // Viewability
    @Expose
    @SerializedName(Constants.VAST_EXTERNAL_VIEWABILITY_TRACKERS)
    private val _externalViewabilityTrackers = mutableMapOf<String, String>()
    val externalViewabilityTrackers: Map<String, String>
        get() {
            return HashMap(_externalViewabilityTrackers)
        }

    @Expose
    @SerializedName(Constants.VAST_AVID_JAVASCRIPT_RESOURCES)
    private val _avidJavascriptResources = mutableSetOf<String>()
    val avidJavascriptResources: Set<String>
        get() {
            return HashSet(_avidJavascriptResources)
        }

    @Expose
    @SerializedName(Constants.VAST_MOAT_IMPRESSION_PIXELS)
    private val _moatImpressionPixels = mutableSetOf<String>()
    val moatImpressionPixels: Set<String>
        get() {
            return HashSet(_moatImpressionPixels)
        }

    @Expose
    @SerializedName(Constants.VAST_URL_CLICKTHROUGH)
    var clickThroughUrl: String? = null
    @Expose
    @SerializedName(Constants.VAST_URL_NETWORK_MEDIA_FILE)
    var networkMediaFileUrl: String? = null
    @Expose
    @SerializedName(Constants.VAST_URL_DISK_MEDIA_FILE)
    var diskMediaFileUrl: String? = null
    @Expose
    @SerializedName(Constants.VAST_SKIP_OFFSET)
    var skipOffset: String? = null

    @Expose
    @SerializedName(Constants.VAST_COMPANION_AD_LANDSCAPE)
    private var landscapeVastCompanionAdConfig: VastCompanionAdConfig? = null
    @Expose
    @SerializedName(Constants.VAST_COMPANION_AD_PORTRAIT)
    private var portraitVastCompanionAdConfig: VastCompanionAdConfig? = null

    @Expose
    @SerializedName(Constants.VAST_ICON_CONFIG)
    var vastIconConfig: VastIconConfig? = null

    @Expose
    @SerializedName(Constants.VAST_IS_REWARDED)
    var isRewarded: Boolean = false

    @Expose
    @SerializedName(Constants.VAST_ENABLE_CLICK_EXP)
    var enableClickExperiment: Boolean = false

    // Custom extensions
    @Expose
    @SerializedName(Constants.VAST_CUSTOM_TEXT_CTA)
    var customCtaText: String? = null
        set(value) {
            field = value ?: field
        }

    @Expose
    @SerializedName(Constants.VAST_CUSTOM_TEXT_SKIP)
    var customSkipText: String? = null
        set(value) {
            field = value ?: field
        }

    @Expose
    @SerializedName(Constants.VAST_CUSTOM_CLOSE_ICON_URL)
    var customCloseIconUrl: String? = null
        set(value) {
            field = value ?: field
        }

    @Expose
    @SerializedName(Constants.VAST_VIDEO_VIEWABILITY_TRACKER)
    var videoViewabilityTracker: VideoViewabilityTracker? = null
        set(value) {
            field = value ?: field
        }

    // MoPub-specific metadata
    @Expose
    @SerializedName(Constants.VAST_DSP_CREATIVE_ID)
    var dspCreativeId: String? = null
        set(value) {
            field = value ?: field
        }
    @Expose
    @SerializedName(Constants.VAST_PRIVACY_ICON_IMAGE_URL)
    var privacyInformationIconImageUrl: String? = null
        set(value) {
            field = value ?: field
        }
    @Expose
    @SerializedName(Constants.VAST_PRIVACY_ICON_CLICK_URL)
    var privacyInformationIconClickthroughUrl: String? = null

    /**
     * Setters
     */
    fun addImpressionTrackers(impressionTrackers: List<VastTracker>) {
        _impressionTrackers.addAll(impressionTrackers)
    }

    fun addResumeTrackers(resumeTrackers: List<VastTracker>) {
        _resumeTrackers.addAll(resumeTrackers)
    }

    fun addFractionalTrackers(fractionalTrackers: List<VastFractionalProgressTracker>) {
        _fractionalTrackers.addAll(fractionalTrackers)
        _fractionalTrackers.sort()
    }

    fun addAbsoluteTrackers(absoluteTrackers: List<VastAbsoluteProgressTracker>) {
        _absoluteTrackers.addAll(absoluteTrackers)
        _absoluteTrackers.sort()
    }

    fun addCompleteTrackers(completeTrackers: List<VastTracker>) {
        _completeTrackers.addAll(completeTrackers)
    }

    fun addPauseTrackers(pauseTrackers: List<VastTracker>) {
        _pauseTrackers.addAll(pauseTrackers)
    }

    fun addCloseTrackers(closeTrackers: List<VastTracker>) {
        _closeTrackers.addAll(closeTrackers)
    }

    fun addSkipTrackers(skipTrackers: List<VastTracker>) {
        _skipTrackers.addAll(skipTrackers)
    }

    fun addClickTrackers(clickTrackers: List<VastTracker>) {
        _clickTrackers.addAll(clickTrackers)
    }

    fun addErrorTrackers(errorTrackers: List<VastTracker>) {
        _errorTrackers.addAll(errorTrackers)
    }

    fun addExternalViewabilityTrackers(
        externalViewabilityTrackers: Map<String, String>?
    ) {
        externalViewabilityTrackers?.let { _externalViewabilityTrackers.putAll(it) }
    }

    fun addAvidJavascriptResources(javascriptResources: Set<String>?) {
        javascriptResources?.let { _avidJavascriptResources.addAll(it) }
    }

    fun addMoatImpressionPixels(impressionPixels: Set<String>?) {
        impressionPixels?.let { _moatImpressionPixels.addAll(it) }
    }

    /**
     * Adds internal video trackers from a JSONObject in the form:
     * {
     * urls: [ "...%%VIDEO_EVENT%%...", ... ],
     * events: [ "companionAdView", ... ]
     * }
     *
     * Each event adds a corresponding tracker type with all the listed urls, with %%VIDEO_EVENT%%
     * replaced with the event name. The currently supported trackers and their mappings are:
     * > start: addAbsoluteTrackers(url, 0)
     * > firstQuartile: addFractionalTrackers(url, 0.25f)
     * > midpoint: addFractionalTrackers(url, 0.5f)
     * > thirdQuartile: addFractionalTrackers(url, 0.75f)
     * > complete: addCompleteTrackers(url)
     * > companionAdView: VastCompanionAdConfig.addCreativeViewTrackers
     * > companionAdClick: VastCompanionAdConfig.addClickTrackers
     *
     * @param videoTrackers A JSONObject with the urls and events to track
     */
    fun addVideoTrackers(videoTrackers: JSONObject?) {
        if (videoTrackers == null) {
            return
        }
        val urls =
            videoTrackers.optJSONArray(Constants.VIDEO_TRACKING_URLS_KEY)
        val events =
            videoTrackers.optJSONArray(Constants.VIDEO_TRACKING_EVENTS_KEY)
        if (urls == null || events == null) {
            return
        }

        for (i in 0 until events.length()) { // JSONArray isn't Iterable -_-)
            val eventName = events.optString(i)
            val urlsForEvent = hydrateUrls(eventName, urls)
            val event = fromString(eventName)
            if (eventName == null || urlsForEvent == null) {
                continue
            }
            when (event) {
                START -> addStartTrackersForUrls(urlsForEvent)
                FIRST_QUARTILE, MIDPOINT, THIRD_QUARTILE ->
                    addFractionalTrackersForUrls(urlsForEvent, event.toFloat())
                COMPLETE -> addCompleteTrackersForUrls(urlsForEvent)
                COMPANION_AD_VIEW -> addCompanionAdViewTrackersForUrls(urlsForEvent)
                COMPANION_AD_CLICK -> addCompanionAdClickTrackersForUrls(urlsForEvent)
                else -> log(CUSTOM, "Encountered unknown video tracking event: $eventName")
            }
        }
    }

    fun setVastCompanionAd(
        landscapeVastCompanionAdConfig: VastCompanionAdConfig?,
        portraitVastCompanionAdConfig: VastCompanionAdConfig?
    ) {
        this.landscapeVastCompanionAdConfig = landscapeVastCompanionAdConfig
        this.portraitVastCompanionAdConfig = portraitVastCompanionAdConfig
    }

    fun getVastCompanionAd(orientation: Int): VastCompanionAdConfig? {
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> portraitVastCompanionAdConfig
            Configuration.ORIENTATION_LANDSCAPE -> landscapeVastCompanionAdConfig
            else -> landscapeVastCompanionAdConfig
        }
    }

    /**
     * Returns whether or not there is a companion ad set. There must be both a landscape and a
     * portrait companion ad set for this to be true.
     *
     * @return true if both the landscape and portrait companion ads are set, false otherwise.
     */
    fun hasCompanionAd(): Boolean {
        return landscapeVastCompanionAdConfig != null && portraitVastCompanionAdConfig != null
    }

    /**
     * Called when the video starts playing.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handleImpression(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _impressionTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when the video is clicked. Handles forwarding the user to the specified click through
     * url, calling [Activity.onActivityResult] when done.
     *
     * @param activity        Used to call startActivityForResult with provided requestCode.
     * @param contentPlayHead Current video playback time when clicked.
     * @param requestCode     Code that identifies what kind of activity request is going to be
     * made.
     */
    fun handleClickForResult(
        activity: Activity, contentPlayHead: Int,
        requestCode: Int
    ) {
        handleClick(activity, contentPlayHead, requestCode)
    }

    /**
     * Called when the video is clicked. Handles forwarding the user to the specified click through
     * url. Does not provide any feedback when opened activity is finished.
     *
     * @param context         Used to call startActivity.
     * @param contentPlayHead Current video playback time when clicked.
     */
    fun handleClickWithoutResult(
        context: Context,
        contentPlayHead: Int
    ) {
        handleClick(context.applicationContext, contentPlayHead, null)
    }

    /**
     * Called when the video is clicked. Handles forwarding the user to the specified click through
     * url.
     *
     * @param context         If an Activity context, used to call startActivityForResult with
     * provided requestCode; otherwise, used to call startActivity.
     * @param contentPlayHead Current video playback time when clicked.
     * @param requestCode     Required when the context is an Activity; code that identifies what
     * kind of activity request is going to be made.
     */
    private fun handleClick(context: Context, contentPlayHead: Int, requestCode: Int?) {
        makeVastTrackingHttpRequest(
            _clickTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
        if (clickThroughUrl.isNullOrEmpty()) {
            return
        }

        val urlHandler = UrlHandler.Builder()
            .withDspCreativeId(dspCreativeId)
            .withoutMoPubBrowser()
            .withSupportedUrlActions(
                UrlAction.IGNORE_ABOUT_SCHEME,
                UrlAction.OPEN_APP_MARKET,
                UrlAction.OPEN_NATIVE_BROWSER,
                UrlAction.OPEN_IN_APP_BROWSER,
                UrlAction.HANDLE_SHARE_TWEET,
                UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                UrlAction.FOLLOW_DEEP_LINK
            )
            .withResultActions(object : ResultActions {
                override fun urlHandlingSucceeded(url: String, urlAction: UrlAction) {
                    if (urlAction == UrlAction.OPEN_IN_APP_BROWSER) {
                        val bundle = Bundle()
                        bundle.run {
                            putString(MoPubBrowser.DESTINATION_URL_KEY, url)
                            putString(MoPubBrowser.DSP_CREATIVE_ID, dspCreativeId)
                        }
                        val clazz = MoPubBrowser::class.java
                        val intent = Intents.getStartActivityIntent(
                            context, clazz, bundle
                        )
                        try {
                            if (context is Activity) {
                                requireNotNull(requestCode) {
                                    "Activity context requires a requestCode"
                                }
                                context.startActivityForResult(intent, requestCode)
                            } else {
                                Intents.startActivity(context, intent)
                            }
                        } catch (e: ActivityNotFoundException) {
                            log(
                                CUSTOM, "Activity ${clazz.name} not found. Did you declare " +
                                        "it in your AndroidManifest.xml?"
                            )
                        } catch (e: IntentNotResolvableException) {
                            log(
                                CUSTOM, "Activity ${clazz.name} not found. Did you declare " +
                                        "it in your AndroidManifest.xml?"
                            )
                        }
                    }
                }

                override fun urlHandlingFailed(url: String, lastFailedUrlAction: UrlAction) {}
            })
            .build()

        clickThroughUrl?.let { urlHandler.handleUrl(context, it) }
    }

    /**
     * Called when the video is not finished and is resumed from the middle of the video.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handleResume(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _resumeTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when the video is not finished and is paused.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handlePause(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _pauseTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when the video is closed.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handleClose(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _closeTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when the video is skipped.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handleSkip(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _skipTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when the video is played completely without skipping.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time (should be duration of video).
     */
    fun handleComplete(context: Context, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _completeTrackers,
            null,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Called when there is a problem with the video. Refer to the possible [VastErrorCode]s
     * for a list of problems.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    fun handleError(context: Context, errorCode: VastErrorCode?, contentPlayHead: Int) {
        makeVastTrackingHttpRequest(
            _errorTrackers,
            errorCode,
            contentPlayHead,
            networkMediaFileUrl,
            context
        )
    }

    /**
     * Returns untriggered VAST progress trackers with a progress before the provided position.
     *
     * @param currentPositionMillis the current video position in milliseconds.
     * @param videoLengthMillis the total video length.
     */
    fun getUntriggeredTrackersBefore(
        currentPositionMillis: Int,
        videoLengthMillis: Int
    ): List<VastTracker> {
        if (videoLengthMillis <= 0 || currentPositionMillis < 0) {
            return emptyList()
        }

        return ArrayList<VastTracker>().also {
            val absoluteTest = VastAbsoluteProgressTracker.Builder(
                "", currentPositionMillis
            ).build()

            _absoluteTrackers.forEach { tracker ->
                if (tracker <= absoluteTest && !tracker.isTracked) {
                    it.add(tracker)
                }
            }

            val fractionalTest = VastFractionalProgressTracker.Builder(
                "",
                currentPositionMillis / videoLengthMillis.toFloat()
            ).build()

            _fractionalTrackers.forEach { tracker ->
                if (tracker <= fractionalTest && !tracker.isTracked) {
                    it.add(tracker)
                }
            }
        }
    }

    /**
     * Returns the number of untriggered progress trackers.
     *
     * @return Integer count >= 0 of the remaining progress trackers.
     */
    val remainingProgressTrackerCount: Int
        get() = getUntriggeredTrackersBefore(Int.MAX_VALUE, Int.MAX_VALUE).size

    /**
     * Gets the skip offset in milliseconds. If the skip offset would be past the video duration,
     * this returns the video duration. Returns null when the skip offset is not set or cannot be parsed.
     *
     * @param videoDuration Used to calculate percentage based offsets.
     * @return The skip offset in milliseconds. Can return null.
     */
    @Throws(NumberFormatException::class)
    fun getSkipOffsetMillis(videoDuration: Int): Int? {
        return skipOffset?.let {
            when {
                VastAbsoluteProgressTracker.isAbsoluteTracker(it) ->
                    VastAbsoluteProgressTracker.parseAbsoluteOffset(it)
                VastFractionalProgressTracker.isPercentageTracker(it) ->
                    VastFractionalProgressTracker.parsePercentageOffset(it, videoDuration)
                else -> {
                    log(CUSTOM, "Invalid VAST skipoffset format: $it")
                    null
                }
            }?.let { skipMs ->
                min(skipMs, videoDuration)
            }
        }
    }

    /**
     * Converts this VastVideoConfig to a String (for parcelization).
     *
     * @return a String representing this VastVideoConfig.
     */
    fun toJsonString(): String {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(VastVideoConfigTypeAdapterFactory())
            .create()
        return gson.toJson(this@VastVideoConfig)
    }

    private fun hydrateUrls(event: String?, urls: JSONArray): List<String>? {
        if (event == null) {
            return null
        }
        val hydratedUrls: MutableList<String> = ArrayList()
        for (i in 0 until urls.length()) {
            val url = urls.optString(i) ?: continue
            hydratedUrls.add(
                url.replace(Constants.VIDEO_TRACKING_URL_MACRO, event)
            )
        }
        return hydratedUrls
    }

    private fun createVastTrackersForUrls(urls: List<String>): List<VastTracker> {
        return urls.map {
            VastTracker.Builder(it).build()
        }
    }

    private fun addCompleteTrackersForUrls(urls: List<String>) {
        addCompleteTrackers(createVastTrackersForUrls(urls))
    }

    private fun addStartTrackersForUrls(urls: List<String>) {
        addAbsoluteTrackers(urls.map {
            VastAbsoluteProgressTracker.Builder(it, 0).build()
        })
    }

    private fun addFractionalTrackersForUrls(urls: List<String>, fraction: Float) {
        addFractionalTrackers(urls.map {
            VastFractionalProgressTracker.Builder(it, fraction).build()
        })
    }

    private fun addCompanionAdViewTrackersForUrls(urls: List<String>) {
        val companionAdViewTrackers = createVastTrackersForUrls(urls)
        landscapeVastCompanionAdConfig?.addCreativeViewTrackers(companionAdViewTrackers)
        portraitVastCompanionAdConfig?.addCreativeViewTrackers(companionAdViewTrackers)
    }

    private fun addCompanionAdClickTrackersForUrls(urls: List<String>) {
        val companionAdClickTrackers = createVastTrackersForUrls(urls)
        landscapeVastCompanionAdConfig?.addClickTrackers(companionAdClickTrackers)
        portraitVastCompanionAdConfig?.addClickTrackers(companionAdClickTrackers)
    }

    private class VastVideoConfigTypeAdapterFactory : TypeAdapterFactory {
        override fun <T : Any?> create(gson: Gson?, typeToken: TypeToken<T>?): TypeAdapter<T>? {
            return if (typeToken == null || !Class::class.java.isAssignableFrom(typeToken.rawType)) {
                null
            } else VastVideoConfig.VastVideoConfigTypeAdapter() as TypeAdapter<T>
        }
    }

    class VastVideoConfigTypeAdapter : TypeAdapter<Class<*>?>() {
        @Throws(IOException::class)
        override fun write(jsonWriter: JsonWriter?, clazz: Class<*>?) {
            when {
                jsonWriter == null -> return
                clazz == null -> {
                    jsonWriter.nullValue()
                    return
                }
                else -> jsonWriter.value(clazz.name)
            }
        }

        @Throws(IOException::class)
        override fun read(jsonReader: JsonReader?): Class<*>? {
            return when {
                jsonReader == null -> null
                jsonReader.peek() == JsonToken.NULL -> {
                    jsonReader.nextNull()
                    null
                }
                else -> try {
                    Class.forName(jsonReader.nextString())
                } catch (exception: ClassNotFoundException) {
                    throw IOException(exception)
                    null
                }
            }
        }
    }

}
