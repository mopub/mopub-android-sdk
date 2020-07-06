// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.framework.base;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import com.mopub.common.MoPub;
import com.mopub.framework.pages.AdListPage;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.fail;

public class BasePage {
    private static final int DEFAULT_TIMEOUT_SECS = 10;
    private static final int DEFAULT_RETRY_COUNT = 6;
    private static final int SAMPLE_TIME_MS = 300;
    private static final int SAMPLES_PER_SEC = 5;

    protected final String ADD_AD_UNIT_LABEL = "New ad unit";

    public static void pressBack() {
        onView(isRoot()).perform(ViewActions.pressBack());
    }

    /**
     * Finds the Ad unit on the R.id.list and clicks the cell
     *
     * @param AdUnit Name of the Ad Unit
     */
    public static void clickCellOnList(@NonNull final String AdUnit) {
        waitForSdkToInitialize();
        try {
            final DataInteraction adUnit;
            adUnit = onData(hasToString((AdUnit)))
                    .inAdapterView(withId(android.R.id.list));
            adUnit.perform(scrollTo());
            Thread.sleep(SAMPLE_TIME_MS); // Buffer to prevent flaky clicks
            adUnit.perform(click());
        } catch (PerformException | InterruptedException e) {
            fail("Ad Unit not found on list");
        }
    }

    public static void waitForSdkToInitialize() {
        int i = 0;
        try {
            while (i++ < DEFAULT_RETRY_COUNT || !MoPub.isSdkInitialized()) {
                Thread.sleep(SAMPLE_TIME_MS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Thread error on sleep execution");
        }
        if (!MoPub.isSdkInitialized()) {
            fail("SDK failed to initialize");
        }
    }

    public void quickClickElement(@NonNull final ViewInteraction element) {
        element.perform(click());
    }

    public void clickElementWithText(@NonNull final String text, @NonNull final boolean isStrict) {
        final String failMessage = "This element with text '" + text + "' is not present";

        final ViewInteraction element = isStrict ?
                onView(withText(text)) :
                onView(withText(containsString(text)));
        clickElement(element, failMessage);
    }

    public void clickElementWithText(@NonNull final String text) {
        clickElementWithText(text, true);
    }

    public void clickElementWithId(final int id) {
        final ViewInteraction element = onView(withId(id));
        final String failMessage = "This element with id '" + id + "' is not present";

        clickElement(element, failMessage);
    }

    public void clickElement(@NonNull final ViewInteraction element) {
        clickElement(element, null);
    }

    public void clickElement(@NonNull final ViewInteraction element, @NonNull final String failMessage) {
        final String message = (failMessage != null) ?
                failMessage :
                "This element is not present";

        if (waitForElement(element)) {
            element.perform(click());
            return;
        }

        fail(message);
    }

    public void clickElementWithResource(@NonNull final String resName) {
        final ViewInteraction element = onView(withResourceName(resName));
        final String failMessage = "This element with resource name '" + resName + "' is not present";

        clickElement(element, failMessage);
    }

    public boolean waitForElement(@NonNull final ViewInteraction element) {
        return waitForElement(element, DEFAULT_TIMEOUT_SECS);
    }

    public boolean waitForElement(@NonNull final ViewInteraction element, final int timeoutInSeconds) {
        int i = 0;
        while (i++ < timeoutInSeconds * SAMPLES_PER_SEC) {
            try {
                element.check(matches(isEnabled()));
                return true;
            } catch (Throwable e) {
                try {
                    Thread.sleep(SAMPLE_TIME_MS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }

    public AdListPage goToHome() {
        ViewInteraction element = onView(withText(ADD_AD_UNIT_LABEL));

        if (!waitForElement(element, 1)) {
            pressBack();
            goToHome();
        }

        return new AdListPage();
    }

    /**
     * Validates if adFormat is aligned horizontally centered in the middle of onScreen
     *
     * @param adRect   Ad format's Rect()
     * @param onScreen Screen Display Rect()
     * @return boolean validation if Banner is centered
     */
    private boolean isCenteredHorizontally(Rect adRect, Rect onScreen) {
        return Math.abs((adRect.left - onScreen.left) - (onScreen.right - adRect.right)) <= 1;
    }

    /**
     * Validates if adFormat is aligned vertically centered in the middle of onScreen
     *
     * @param adRect   Ad Format Rect()
     * @param onScreen Screen Display Rect()
     * @return boolean validation if  is centered
     */
    private boolean isCenteredVertically(Rect adRect, Rect onScreen) {
        return Math.abs((adRect.top - onScreen.top) - (onScreen.bottom - adRect.bottom)) <= 1;
    }

    public Matcher<View> isCenteredInWindow() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View adFormatView) {
                final Rect screen = new Rect();
                final Rect banner = new Rect();

                adFormatView.getWindowVisibleDisplayFrame(screen);
                adFormatView.getHitRect(banner);

                return isCenteredHorizontally(banner, screen);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is centered on Display Frame");
            }
        };
    }

    public Matcher<View> isInFullscreen() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View adFormatView) {
                final Rect screen = new Rect();
                final Rect fullscreenAd = new Rect();

                adFormatView.getWindowVisibleDisplayFrame(screen);
                adFormatView.getHitRect(fullscreenAd);

                return fullscreenAd.equals(screen);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Is on fullscreen");
            }
        };
    }
}
