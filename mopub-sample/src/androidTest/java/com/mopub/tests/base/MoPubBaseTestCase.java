// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.tests.base;

import androidx.annotation.NonNull;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.rule.IntentsTestRule;

import com.mopub.framework.pages.AdDetailPage;
import com.mopub.simpleadsdemo.MoPubSampleActivity;
import com.mopub.simpleadsdemo.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.mopub.framework.base.BasePage.clickCellOnList;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

public class MoPubBaseTestCase {

    protected static final String WEB_PAGE_LINK = "https://www.mopub.com/en/click-test";
    private static final String RELEASE_WEB_PAGE_LINK = "https://www.mopub.com/en";
    @Rule
    public IntentsTestRule<MoPubSampleActivity> mActivityRule =
            new IntentsTestRule<MoPubSampleActivity>(MoPubSampleActivity.class);
    protected AdDetailPage adDetailPage;

    @Before
    public void setUp() {
        adDetailPage = new AdDetailPage();
    }

    @After
    public void tearDown() {
    }

    /**
     * Verify the navigation and loading of the given adUnit.
     * Validates if it is horizontally aligned in the center of the screen for the given orientation.
     *
     * @param adUnit      Name of the adUnit to validate.
     * @param orientation ActivityInfo orientation constant
     */
    protected void isAlignedInLine(@NonNull String adUnit, int orientation) {
        clickCellOnList(adUnit);

        // Press Load button
        adDetailPage.clickElementWithId(R.id.load_button);

        final ViewInteraction bannerElement = onView(allOf(withId(R.id.banner_mopubview),
                hasChildCount(1)));

        assertTrue("Ad banner failed to load", adDetailPage.waitForElement(bannerElement));

        mActivityRule.getActivity().setRequestedOrientation(orientation);

        // Assert if the bannerElement is centered in portrait
        bannerElement.check(matches(adDetailPage.isCenteredInWindow()));
    }

    /**
     * Verify the navigation and loading of the given adUnit.
     *
     * @param adUnit      Name of the adUnit to validate.
     * @param orientation ActivityInfo orientation constant
     */
    protected void showsInFullscreen(@NonNull String adUnit, int orientation) {
        clickCellOnList(adUnit);
        mActivityRule.getActivity().setRequestedOrientation(orientation);
        // Click Load
        onView(withId(R.id.load_button)).perform(click());

        ViewInteraction showButtonElement = onView((withId(R.id.show_button))); //show ad on click
        adDetailPage.clickElement(showButtonElement);

        final ViewInteraction element = onView(allOf(withId(android.R.id.content)));
        element.check(matches(adDetailPage.isInFullscreen()));
    }

    /**
     * Verify clicking on the Ad shows the clickthrough url in the MoPubBrowser.
     *
     * @param id element to validate clickthrough
     */
    protected void hasClickthrough(final int id) {
        final ViewInteraction bannerElement = onView(allOf(withId(id),
                hasChildCount(1)));
        // Check for web page loads correctly
        adDetailPage.clickElement(bannerElement);
        final ViewInteraction browserLinkElement = onView(withText(RELEASE_WEB_PAGE_LINK));

        assertTrue("Browser Link not found", adDetailPage.waitForElement(browserLinkElement));
    }

}
