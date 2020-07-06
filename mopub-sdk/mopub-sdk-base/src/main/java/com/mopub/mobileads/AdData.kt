// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.os.Parcelable
import com.mopub.common.Constants
import com.mopub.common.CreativeOrientation
import kotlinx.android.parcel.Parcelize
import java.util.TreeMap
import kotlin.collections.HashMap

@Parcelize
data class AdData(
    var vastVideoConfigString: String?,
    var orientation: CreativeOrientation?,
    var broadcastIdentifier: Long,
    var timeoutDelayMillis: Int,
    var impressionMinVisibleDips: String?,
    var impressionMinVisibleMs: String?,
    var dspCreativeId: String?,
    var adPayload: String,
    var extras: Map<String, String>,
    var isRewarded: Boolean,
    var rewardedDurationSeconds: Int,
    var currencyName: String?,
    var currencyAmount: Int,
    var adWidth: Int?,
    var adHeight: Int?,
    var adUnit: String?,
    var adType: String?,
    var fullAdType: String?,
    var customerId: String?,
    var allowCustomClose: Boolean
) : Parcelable {
    companion object {
        const val MILLIS_IN_SECOND: Int = 1_000

        /**
         * If a duration is not specified, this duration is used. 30 seconds is also the maximum
         * amount of time that we currently allow rewarded ads to be not closeable.
         */
        const val DEFAULT_DURATION_FOR_CLOSE_BUTTON_SECONDS: Int = 30
        const val DEFAULT_DURATION_FOR_CLOSE_BUTTON_MILLIS: Int =
            DEFAULT_DURATION_FOR_CLOSE_BUTTON_SECONDS * MILLIS_IN_SECOND
        const val COUNTDOWN_UPDATE_INTERVAL_MILLIS: Long = 250L
        const val DEFAULT_INLINE_TIMEOUT_DELAY = Constants.TEN_SECONDS_MILLIS
        const val DEFAULT_FULLSCREEN_TIMEOUT_DELAY = Constants.THIRTY_SECONDS_MILLIS
        const val DEFAULT_UNSPECIFIED_TIMEOUT_DELAY = Constants.THIRTY_SECONDS_MILLIS
    }

    private constructor(builder: Builder) : this(
        builder.vastVideoConfigString,
        builder.orientation,
        builder.broadcastIdentifier,
        builder.timeoutDelayMillis,
        builder.impressionMinVisibleDips,
        builder.impressionMinVisibleMs,
        builder.dspCreativeId,
        builder.adPayload,
        builder.extras,
        builder.isRewarded,
        builder.rewardedDurationSeconds,
        builder.currencyName,
        builder.currencyAmount,
        builder.adWidth,
        builder.adHeight,
        builder.adUnit,
        builder.adType,
        builder.fullAdType,
        builder.customerId,
        builder.allowCustomClose
    )

    class Builder() {
        var vastVideoConfigString: String? = null
            private set
        var orientation: CreativeOrientation? = null
            private set
        var broadcastIdentifier: Long = 0
            private set
        var timeoutDelayMillis: Int = DEFAULT_UNSPECIFIED_TIMEOUT_DELAY
            private set
        var impressionMinVisibleDips: String? = null
            private set
        var impressionMinVisibleMs: String? = null
            private set
        var dspCreativeId: String? = ""
            private set
        var adPayload: String = ""
            private set
        var extras: Map<String, String> = HashMap()
            private set
        var isRewarded: Boolean = false
            private set
        var rewardedDurationSeconds: Int = DEFAULT_DURATION_FOR_CLOSE_BUTTON_SECONDS
            private set
        var currencyName: String? = null
            private set
        var currencyAmount: Int = 0
            private set
        var adWidth: Int? = null
            private set
        var adHeight: Int? = null
            private set
        var adUnit: String? = null
            private set
        var adType: String? = null
            private set
        var fullAdType: String? = null
            private set
        var customerId: String? = null
            private set
        var allowCustomClose: Boolean = false
            private set

        fun vastVideoConfig(vastVideoConfigString: String?) =
            apply { this.vastVideoConfigString = vastVideoConfigString }

        fun orientation(orientation: CreativeOrientation?) =
            apply { this.orientation = orientation }

        fun broadcastIdentifier(broadcastIdentifier: Long) =
            apply { this.broadcastIdentifier = broadcastIdentifier }

        fun timeoutDelayMillis(timeoutDelayMillis: Int) =
            apply { this.timeoutDelayMillis = timeoutDelayMillis }

        fun impressionMinVisibleDips(impressionMinVisibleDips: String?) = apply {
            this.impressionMinVisibleDips = impressionMinVisibleDips
        }

        fun impressionMinVisibleMs(impressionMinVisibleMs: String?) = apply {
            this.impressionMinVisibleMs = impressionMinVisibleMs
        }

        fun dspCreativeId(dspCreativeId: String?) = apply { this.dspCreativeId = dspCreativeId }

        fun adPayload(adPayload: String) = apply { this.adPayload = adPayload }

        fun extras(extras: Map<String, String>) =
            apply { this.extras = TreeMap(extras) }

        fun isRewarded(isRewarded: Boolean) = apply { this.isRewarded = isRewarded }

        fun rewardedDurationSeconds(rewardedDurationSeconds: Int) =
            apply { this.rewardedDurationSeconds = rewardedDurationSeconds }

        fun currencyName(currencyName: String?) = apply { this.currencyName = currencyName }

        fun currencyAmount(currencyAmount: Int) = apply { this.currencyAmount = currencyAmount }

        fun adWidth(adWidth: Int?) = apply { this.adWidth = adWidth }

        fun adHeight(adHeight: Int?) = apply { this.adHeight = adHeight }

        fun adUnit(adUnit: String?) = apply { this.adUnit = adUnit }

        fun adType(adType: String?) = apply { this.adType = adType }

        fun fullAdType(fullAdType: String?) = apply { this.fullAdType = fullAdType }

        fun customerId(customerId: String?) = apply { this.customerId = customerId }

        fun allowCustomClose(allowCustomClose: Boolean) =
            apply { this.allowCustomClose = allowCustomClose }

        fun build() = AdData(this)

        fun fromAdData(adData: AdData) = apply {
            this.vastVideoConfigString = adData.vastVideoConfigString
            this.orientation = adData.orientation
            this.broadcastIdentifier = adData.broadcastIdentifier
            this.timeoutDelayMillis = adData.timeoutDelayMillis
            this.impressionMinVisibleDips = adData.impressionMinVisibleDips
            this.impressionMinVisibleMs = adData.impressionMinVisibleMs
            this.dspCreativeId = adData.dspCreativeId
            this.adPayload = adData.adPayload
            this.extras = adData.extras
            this.isRewarded = adData.isRewarded
            this.rewardedDurationSeconds = adData.rewardedDurationSeconds
            this.currencyName = adData.currencyName
            this.currencyAmount = adData.currencyAmount
            this.adWidth = adData.adWidth
            this.adHeight = adData.adHeight
            this.adUnit = adData.adUnit
            this.adType = adData.adType
            this.fullAdType = adData.fullAdType
            this.customerId = customerId
            this.allowCustomClose = allowCustomClose
        }
    }
}
