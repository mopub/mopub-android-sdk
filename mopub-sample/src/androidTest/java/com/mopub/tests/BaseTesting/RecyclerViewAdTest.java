// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.BaseTesting;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.mopub.framework.models.AdLabels;
import com.mopub.framework.pages.AdDetailPage;
import com.mopub.simpleadsdemo.R;
import com.mopub.tests.base.MoPubBaseTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewAdTest extends MoPubBaseTestCase {

    // Test Variables
    private static final String TITLE = AdLabels.RECYCLER_VIEW;

    /*
     * Verify that the Recycler View Ad loads & shows on the app.
     */
    @Test
    public void adsListPage_withClickOnMoPubRecyclerViewSample_shouldLoadMoPubRecyclerViewAd() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        final AdDetailPage adDetailPage = new AdDetailPage();

        final ViewInteraction element = onView(allOf(withId(R.id.native_main_image)));

        assertTrue(adDetailPage.waitForElement(element));
    }


    /*
     * Verify that the Recycler View Ad loads & shows on the app.
     */
    @Test
    public void adsDetailsPage_withClickOnLoadAdButton_shouldLoadMoPubRecyclerViewAd() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        final AdDetailPage adDetailPage = new AdDetailPage();

        onView(withId(R.id.load_button)).perform(click()); //reload and show ad on button click

        final ViewInteraction element = onView(allOf(withId(R.id.native_main_image)));

        assertTrue(adDetailPage.waitForElement(element));
    }

    /*
     * Verify that the user is correctly navigated to MoPub browser.
     */
    @Test
    public void adsDetailsPage_withClickOnAd_shouldShowMoPubBrowser() {
        onData(hasToString(startsWith(TITLE)))
                .inAdapterView(withId(android.R.id.list))
                .perform(click());

        final AdDetailPage adDetailPage = new AdDetailPage();

        ViewInteraction nativeMainImageElement = onView(withId(R.id.native_main_image)); //show MoPub browser on Ad click
        adDetailPage.clickElement(nativeMainImageElement);

        final ViewInteraction browserLinkElement = onView(withText(WEB_PAGE_LINK));

        assertTrue(adDetailPage.waitForElement(browserLinkElement));
    }
}
