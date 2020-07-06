// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.ReleaseTesting;

import android.content.pm.ActivityInfo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.mopub.framework.models.AdLabels;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReleaseBannerTest extends MoPubBaseTestCase {

    @Test
    public void release_portraitBannerImage_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(BannerTestAdUnits.IMAGE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_landscapeBannerImage_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(BannerTestAdUnits.IMAGE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_portraitBannerHTML_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(BannerTestAdUnits.HTML.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_landscapeBannerHTML_shouldLoadBannerCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(BannerTestAdUnits.HTML.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(R.id.banner_mopubview);
    }

    private enum BannerTestAdUnits {
        HTML(AdLabels.BANNER_HTML),
        IMAGE(AdLabels.BANNER_IMAGE);

        private final String label;

        BannerTestAdUnits(final String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
