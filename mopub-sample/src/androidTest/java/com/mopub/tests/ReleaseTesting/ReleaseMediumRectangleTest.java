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
public class ReleaseMediumRectangleTest extends MoPubBaseTestCase {

    @Test
    public void release_portraitMediumRectangleHtml_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.HTML.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_landscapeMediumRectangleHtml_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.HTML.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_portraitMediumRectangleImage_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.IMAGE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_landscapeMediumRectangleImage_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.IMAGE.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_PortraitMediumRectangleHtmlVideo_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.VIDEO.getAdName(), ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        hasClickthrough(R.id.banner_mopubview);
    }

    @Test
    public void release_LandscapeMediumRectangleHtmlVideo_shouldLoadCenteredHorizontally_shouldShowMoPubBrowser() {
        isAlignedInLine(MediumRectangleTestAdUnits.VIDEO.getAdName(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hasClickthrough(R.id.banner_mopubview);
    }

    private enum MediumRectangleTestAdUnits {
        HTML(AdLabels.MEDIUM_RECTANGLE_HTML),
        IMAGE(AdLabels.MEDIUM_RECTANGLE_IMAGE),
        VIDEO(AdLabels.MEDIUM_RECTANGLE_HTML_VIDEO);

        private final String label;

        MediumRectangleTestAdUnits(final String adType) {
            this.label = adType;
        }

        public String getAdName() {
            return label;
        }
    }
}
