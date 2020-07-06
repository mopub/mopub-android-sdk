// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants

open class VideoViewabilityTracker(
    @Expose @SerializedName(Constants.VAST_TRACKER_PLAYTIME_MS) val viewablePlaytimeMS: Int,
    @Expose @SerializedName(Constants.VAST_TRACKER_PERCENT_VIEWABLE) val percentViewable: Int,
    content: String,
    messageType: MessageType,
    isRepeatable: Boolean
) : VastTracker(content, messageType, isRepeatable) {

    data class Builder(
        private val content: String,
        val viewablePlaytimeMS: Int,
        val percentViewable: Int
    ) {
        private var messageType: MessageType = MessageType.TRACKING_URL
        private var isRepeatable: Boolean = false

        fun messageType(messageType: MessageType) = apply { this.messageType = messageType }
        fun isRepeatable(isRepeatable: Boolean) = apply { this.isRepeatable = isRepeatable }
        fun build() = VideoViewabilityTracker(
            viewablePlaytimeMS,
            percentViewable,
            content,
            messageType,
            isRepeatable
        )
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
