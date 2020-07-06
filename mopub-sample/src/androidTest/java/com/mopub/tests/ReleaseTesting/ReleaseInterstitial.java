// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.ReleaseTesting;

import android.content.pm.ActivityInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.framework.models.AdLabels;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReleaseInterstitial extends MoPubBaseTestCase {

    @Test
    public void release_landscapeInterstitialHtml_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.HTML_LANDSCAPE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(android.R.id.content);
    }

    @Test
    public void release_portraitInterstitialHtml_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.HTML_PORTRAIT.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(android.R.id.content);

    }

    @Test
    public void release_portraitInterstitialImage_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.IMAGE_PORTRAIT.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(android.R.id.content);
    }

    @Test
    public void release_landscapeInterstitialImage_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.IMAGE_LANDSCAPE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(android.R.id.content);
    }

    @Test
    public void release_landscapeInterstitialVideo_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.VIDEO_LANDSCAPE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(android.R.id.content);
    }

    @Test
    public void release_portraitInterstitialVideo_shouldLoad_shouldShowMoPubBrowser() {
        showsInFullscreen(InterstitialTestAdUnits.VIDEO_PORTRAIT.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(android.R.id.content);
    }

    private enum InterstitialTestAdUnits {
        IMAGE_PORTRAIT(AdLabels.INTERSTITIAL_IMAGE_PORTRAIT),
        IMAGE_LANDSCAPE(AdLabels.INTERSTITIAL_IMAGE_LANDSCAPE),
        HTML_PORTRAIT(AdLabels.INTERSTITIAL_HTML_PORTRAIT),
        HTML_LANDSCAPE(AdLabels.INTERSTITIAL_HTML_LANDSCAPE),
        VIDEO_PORTRAIT(AdLabels.INTERSTITIAL_VIDEO_PORTRAIT),
        VIDEO_LANDSCAPE(AdLabels.INTERSTITIAL_VIDEO_LANDSCAPE);

        private final String label;

        InterstitialTestAdUnits(String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
