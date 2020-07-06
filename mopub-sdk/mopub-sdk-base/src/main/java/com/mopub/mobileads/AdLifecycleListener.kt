// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads

import com.mopub.common.MoPubReward

interface AdLifecycleListener {

    interface LoadListener {
        /**
         * This method must be called on the implementing class when an ad is successfully loaded.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        fun onAdLoaded()

        /**
         * This method must be called on the implementing class when an ad fails to load.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        fun onAdLoadFailed(errorCode: MoPubErrorCode)
    }

    interface InlineInteractionListener {

        /*
        Inline specific
         */

        /**
         * This method must be called on the implementing class when an ad's refresh is resumed.
         * This method is optional.
         */
        @JvmDefault
        fun onAdResumeAutoRefresh() {
        }

        /**
         * This method must be called on the implementing class when an ad's refresh is paused.
         * This method is optional.
         */
        @JvmDefault
        fun onAdPauseAutoRefresh() {
        }

        /**
         * This method must be called on the implementing class when an ad is expanded.
         * This method is optional.
         */
        @JvmDefault
        fun onAdExpanded() {
        }

        /**
         * This method must be called on the implementing class when an ad is collapsed.
         * This method is optional.
         */
        @JvmDefault
        fun onAdCollapsed() {
        }

    }

    interface FullscreenInteractionListener {

        /*
        Fullscreen specific
         */

        /**
         * This method must be called on the implementing class when an ad is dismissed.
         * This method is optional.
         */
        @JvmDefault
        fun onAdDismissed() {
        }

        /**
         * This method must be called on the implementing class when an ad has completed its minimum
         * required viewing, such as for giving a reward in a rewarded ad. Otherwise, this method
         * is optional.
         */
        @JvmDefault
        fun onAdComplete(moPubReward: MoPubReward?) {
        }

    }

    interface InteractionListener : InlineInteractionListener, FullscreenInteractionListener {

        /**
         * This method must be called on the implementing class when an ad fails at any point it is
         * not loading.
         * This method is optional.
         */
        fun onAdFailed(errorCode: MoPubErrorCode)

        /**
         * This method must be called on the implementing class when an ad is displayed.
         * This method is optional. However, if you call this method, you should ensure that
         * onAdDismissed is called at a later time.
         */
        fun onAdShown()

        /**
         * This method must be called on the implementing class when a user taps on an ad.
         * This method is optional.
         */
        fun onAdClicked()

        /**
         * This method must be called on the implementing class when an impression happens if you
         * set automatic impression and click tracking to false. Otherwise, this method is optional.
         */
        fun onAdImpression()
    }
}
