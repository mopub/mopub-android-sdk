// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import android.graphics.Point
import android.location.Location
import android.view.View
import com.mopub.common.AdFormat
import com.mopub.common.MoPub
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.AdLogEvent
import com.mopub.mobileads.AdLifecycleListener.LoadListener
import com.mopub.mobileads.AdLifecycleListener.InteractionListener
import java.util.*

interface MoPubAd : LoadListener, InteractionListener {

    fun resolveAdSize(): Point
    fun getAdFormat(): AdFormat
    fun getAdViewController(): AdViewController?
    fun setAdViewController(adViewController: AdViewController)

    @JvmDefault
    fun loadAd() {
        getAdViewController()?.run {
            setRequestedAdSize(resolveAdSize())
            loadAd()
        }
    }

    @JvmDefault
    fun setAdContentView(view: View) {
        getAdViewController()?.run { setAdContentView(view) }
    }

    @JvmDefault
    fun setAdUnitId(adUnitId: String) {
        getAdViewController()?.let { it.adUnitId = adUnitId }
    }

    @JvmDefault
    fun getAdUnitId(): String? {
        return getAdViewController()?.adUnitId
    }

    @JvmDefault
    fun setKeywords(keywords: String?) {
        getAdViewController()?.let { it.keywords = keywords }
    }

    @JvmDefault
    fun getKeywords(): String? {
        return getAdViewController()?.keywords
    }

    @JvmDefault
    fun setUserDataKeywords(userDataKeywords: String?) {
        getAdViewController()?.let { it.userDataKeywords = userDataKeywords }
    }

    @JvmDefault
    fun getUserDataKeywords(): String? {
        return if (MoPub.canCollectPersonalInformation()) {
            getAdViewController()?.userDataKeywords
        } else null
    }

    @JvmDefault
    fun getAdWidth(): Int {
        return getAdViewController()?.adWidth ?: 0
    }

    @JvmDefault
    fun getAdHeight(): Int {
        return getAdViewController()?.adHeight ?: 0
    }

    @JvmDefault
    fun loadFailUrl(errorCode: MoPubErrorCode): Boolean {
        return getAdViewController()?.loadFailUrl(errorCode) ?: false
    }

    @JvmDefault
    fun pauseAutoRefresh() {
        getAdViewController()?.pauseRefresh()
    }

    @JvmDefault
    fun resumeAutoRefresh() {
        getAdViewController()?.resumeRefresh()
    }

    @JvmDefault
    fun getLocalExtras(): Map<String, Any> {
        return getAdViewController()?.localExtras ?: TreeMap()
    }

    @JvmDefault
    fun setLocalExtras(localExtras: Map<String, Any>) {
        getAdViewController()?.let { it.localExtras = localExtras }
    }

    /**
     * @return current SDK location value
     */
    @Deprecated("As of 5.12.0, will be removed in the future.")
    @JvmDefault
    fun getLocation(): Location? {
        return getAdViewController()?.location
    }
}
