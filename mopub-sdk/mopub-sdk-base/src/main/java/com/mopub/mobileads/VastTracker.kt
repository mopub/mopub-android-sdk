// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import java.io.Serializable

open class VastTracker(
    @Expose @SerializedName(Constants.VAST_TRACKER_CONTENT) val content: String,
    @Expose @SerializedName(Constants.VAST_TRACKER_MESSAGE_TYPE) val messageType: MessageType,
    @Expose @SerializedName(Constants.VAST_TRACKER_REPEATABLE) val isRepeatable: Boolean) : Serializable {

    data class Builder(private val content: String) {
        private var messageType: MessageType = MessageType.TRACKING_URL
        private var isRepeatable: Boolean = false

        fun messageType(messageType: MessageType) = apply { this.messageType = messageType }
        fun isRepeatable(isRepeatable: Boolean) = apply { this.isRepeatable = isRepeatable }
        fun build() = VastTracker(content, messageType, isRepeatable)
    }

    companion object {
        private const val serialVersionUID: Long = 3L
    }

    var isTracked: Boolean = false
        private set

    enum class MessageType {
        TRACKING_URL,
        QUARTILE_EVENT;
    }

    fun setTracked() {
        isTracked = true
    }
}
