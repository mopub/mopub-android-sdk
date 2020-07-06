// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.content.Context
import android.os.Bundle
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import com.mopub.common.MoPubBrowser
import com.mopub.common.UrlAction
import com.mopub.common.UrlHandler
import com.mopub.common.UrlHandler.ResultActions
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.common.util.Intents
import com.mopub.exceptions.IntentNotResolvableException
import com.mopub.network.TrackingRequest
import java.io.Serializable

/**
 * The data and event handlers for the icon displayed during a VAST 3.0 video.
 */
@Mockable
class VastIconConfig(
    @Expose @SerializedName(Constants.VAST_WIDTH)
    val width: Int,
    @Expose @SerializedName(Constants.VAST_HEIGHT)
    val height: Int,
    offsetMS: Int?,
    @Expose @SerializedName(Constants.VAST_DURATION_MS)
    val durationMS: Int?,
    @Expose @SerializedName(Constants.VAST_RESOURCE)
    val vastResource: VastResource,
    @Expose @SerializedName(Constants.VAST_TRACKERS_CLICK)
    val clickTrackingUris: List<VastTracker>,
    @Expose @SerializedName(Constants.VAST_URL_CLICKTHROUGH)
    val clickThroughUri: String?,
    @Expose @SerializedName(Constants.VAST_VIDEO_VIEWABILITY_TRACKER)
    val viewTrackingUris: List<VastTracker>
) : Serializable {

    @Expose
    @SerializedName(Constants.VAST_SKIP_OFFSET_MS)
    val offsetMS = offsetMS ?: 0

    /**
     * Called when the icon is displayed during the video. Handles firing the impression trackers.
     *
     * @param context the context.
     * @param contentPlayHead the time into the video.
     * @param assetUri the uri of the video.
     */
    fun handleImpression(
        context: Context,
        contentPlayHead: Int,
        assetUri: String
    ) {
        TrackingRequest.makeVastTrackingHttpRequest(
            viewTrackingUris,
            null,
            contentPlayHead,
            assetUri,
            context
        )
    }

    /**
     * Called when the icon is clicked. Handles forwarding the user to the click through uri.
     *
     * @param context                the context.
     * @param webViewClickThroughUri The click through uri for Javascript, HTML and IFrame resources
     * from the WebView
     */
    fun handleClick(
        context: Context,
        webViewClickThroughUri: String?,
        dspCreativeId: String?
    ) {

        vastResource.getCorrectClickThroughUrl(clickThroughUri, webViewClickThroughUri)
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                // Do URL stuff with `it`
                UrlHandler.Builder()
                    .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_IN_APP_BROWSER
                    )
                    .withResultActions(object : ResultActions {
                        override fun urlHandlingSucceeded(url: String, urlAction: UrlAction) {
                            if (urlAction === UrlAction.OPEN_IN_APP_BROWSER) {
                                val bundle = Bundle()
                                bundle.run {
                                    putString(MoPubBrowser.DESTINATION_URL_KEY, url)
                                    if (!dspCreativeId.isNullOrEmpty()) {
                                        putString(MoPubBrowser.DSP_CREATIVE_ID, dspCreativeId)
                                    }

                                }
                                val clazz = MoPubBrowser::class.java
                                val intent = Intents.getStartActivityIntent(context, clazz, bundle)
                                try {
                                    Intents.startActivity(context, intent)
                                } catch (e: IntentNotResolvableException) {
                                    MoPubLog.log(SdkLogEvent.CUSTOM, e.message)
                                }
                            }
                        }

                        override fun urlHandlingFailed(
                            url: String,
                            lastFailedUrlAction: UrlAction
                        ) {
                        }
                    })
                    .withoutMoPubBrowser()
                    .build()
                    .handleUrl(context, it)
            }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
