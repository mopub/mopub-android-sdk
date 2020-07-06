// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mopub.framework.models.AdLabels;
import com.mopub.framework.pages.AdDetailPage;
import com.mopub.framework.pages.AdListPage;
import com.mopub.framework.pages.AdListPage.AdUnitType;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BannerAdTests extends MoPubBaseTestCase {

    // Test Variables
    private static final AdUnitType AD_TYPE = AdUnitType.BANNER;
    private static final String TITLE = AdLabels.BANNER;

    /*
     * Verify that the Banner Ad is successfully loaded and displayed on
     * the app.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubBannerSample_shouldLoadMoPubBanner() {
        final AdListPage adListPage = new AdListPage();
        final AdDetailPage adDetailPage = adListPage.clickCell(TITLE);

        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));

        assertTrue(adDetailPage.waitForElement(bannerElement));
    }

    /*
     * Verify that the Banner Ad fails to load on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubBannerSample_shouldNotLoadMoPubBanner() {
        final String fakeAdUnit = "abc";
        final String adUnitTitle = "Banner Automation Test";

        final AdListPage adListPage = new AdListPage();
        adListPage.addAdUnit(AD_TYPE, fakeAdUnit, adUnitTitle);

        final AdDetailPage adDetailPage = adListPage.clickCell(adUnitTitle);

        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));

        assertTrue(!adDetailPage.waitForElement(bannerElement));

        // Clean Up
        adListPage.deleteAdUnit(adUnitTitle);
    }

    /*
     * Verify that the user is correctly navigated to
     * Banner Ad's url on click.
     */
    @Test
    public void adsDetailsPage_withClickOnMoPubBannerAd_shouldShowMoPubBrowser() {
        final AdListPage adListPage = new AdListPage();
        final AdDetailPage adDetailPage = adListPage.clickCell(TITLE);

        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));
        adDetailPage.clickElement(bannerElement);

        final ViewInteraction browserLinkElement = onView(withText(WEB_PAGE_LINK));

        assertTrue(adDetailPage.waitForElement(browserLinkElement));
    }
}
