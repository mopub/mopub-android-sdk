// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.mopub.common.Constants
import java.util.regex.Pattern

open class VastAbsoluteProgressTracker(
    @Expose @SerializedName(Constants.VAST_TRACKER_TRACKING_MS) val trackingMilliseconds: Int,
    content: String,
    messageType: MessageType,
    isRepeatable: Boolean)
    : VastTracker(content, messageType, isRepeatable), Comparable<VastAbsoluteProgressTracker> {

    data class Builder(private val content: String,
                       private val trackingMilliseconds: Int) {
        private var messageType: MessageType = MessageType.TRACKING_URL
        private var isRepeatable: Boolean = false

        fun messageType(messageType: MessageType) = apply { this.messageType = messageType }
        fun isRepeatable(isRepeatable: Boolean) = apply { this.isRepeatable = isRepeatable }
        fun build() = VastAbsoluteProgressTracker(trackingMilliseconds, content, messageType, isRepeatable)
    }

    companion object {
        private const val serialVersionUID: Long = 1L
        private val absolutePattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}(.\\d{3})?")

        fun isAbsoluteTracker(progressValue: String?): Boolean {
            return (!progressValue.isNullOrEmpty()
                    && absolutePattern.matcher(progressValue).matches())
        }

        fun parseAbsoluteOffset(progressValue: String?): Int? {
            return progressValue
                ?.split(":")
                ?.takeIf {
                    it.size == 3
                }?.let {
                    (it[0].toInt() * 60 * 60 * 1000 // Hours
                    +it[1].toInt() * 60 * 1000 // Minutes
                    +(it[2].toFloat() * 1000).toInt())
                }
        }
    }

    override fun compareTo(other: VastAbsoluteProgressTracker): Int {
        return this.trackingMilliseconds.compareTo(other.trackingMilliseconds)
    }

    override fun toString(): String {
        return "${trackingMilliseconds}ms: $content"
    }

}
