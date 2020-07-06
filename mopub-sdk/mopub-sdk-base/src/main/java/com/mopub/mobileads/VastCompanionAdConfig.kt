// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import com.mopub.common.MoPubBrowser
import com.mopub.common.UrlAction
import com.mopub.common.UrlHandler
import com.mopub.common.UrlHandler.ResultActions
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM
import com.mopub.common.util.Intents
import com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest
import java.io.Serializable

@VisibleForTesting
open class VastCompanionAdConfig(
    @Expose @SerializedName(Constants.VAST_WIDTH)
    val width: Int,
    @Expose @SerializedName(Constants.VAST_HEIGHT)
    val height: Int,
    @Expose @SerializedName(Constants.VAST_RESOURCE)
    val vastResource: VastResource,
    @Expose @SerializedName(Constants.VAST_URL_CLICKTHROUGH)
    val clickThroughUrl: String?,
    @Expose @SerializedName(Constants.VAST_TRACKERS_CLICK)
    val clickTrackers: MutableList<VastTracker>,
    @Expose @SerializedName(Constants.VAST_TRACKERS_IMPRESSION)
    val creativeViewTrackers: MutableList<VastTracker>
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 3L
    }

    /**
     * Add click trackers.
     *
     * @param clickTrackers List of URLs to hit
     */
    fun addClickTrackers(clickTrackers: Collection<VastTracker>) {
        this.clickTrackers.addAll(clickTrackers)
    }

    /**
     * Add creativeView trackers that are supposed to be fired when the companion ad is visible.
     *
     * @param creativeViewTrackers List of URLs to hit when this companion is viewed
     */
    fun addCreativeViewTrackers(creativeViewTrackers: Collection<VastTracker>) {
        this.creativeViewTrackers.addAll(creativeViewTrackers)
    }

    fun handleImpression(
        context: Context,
        contentPlayHead: Int
    ) {
        makeVastTrackingHttpRequest(
            creativeViewTrackers,
            null,
            contentPlayHead,
            null,
            context
        )
    }

    open fun handleClick(
        context: Context,
        requestCode: Int,
        webViewClickThroughUrl: String?,
        dspCreativeId: String?
    ) {
        require(context is Activity) { "context must be an activity" }

        vastResource.getCorrectClickThroughUrl(clickThroughUrl, webViewClickThroughUrl)
            ?.takeIf { url -> url.isNotEmpty() }
            ?.let { url ->
                UrlHandler.Builder()
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
                                    if (!dspCreativeId.isNullOrEmpty()) {
                                        putString(MoPubBrowser.DSP_CREATIVE_ID, dspCreativeId)
                                    }
                                }

                                val clazz = MoPubBrowser::class.java
                                val intent = Intents.getStartActivityIntent(context, clazz, bundle)
                                try {
                                    context.startActivityForResult(intent, requestCode)
                                } catch (anfe: ActivityNotFoundException) {
                                    MoPubLog.log(
                                        CUSTOM,
                                        "Activity " + clazz.getName() + " not found. Did you " +
                                                "declare it in your AndroidManifest.xml?"
                                    )
                                }
                            }
                        }

                        override fun urlHandlingFailed(
                            url: String,
                            lastFailedUrlAction: UrlAction
                        ) {
                        }
                    })
                    .withDspCreativeId(dspCreativeId)
                    .withoutMoPubBrowser()
                    .build()
                    .handleUrl(context, url)
            }
    }
}
