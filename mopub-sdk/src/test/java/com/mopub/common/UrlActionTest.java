package com.mopub.common;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.mopub.common.UrlAction.HANDLE_MOPUB_SCHEME;
import static com.mopub.common.UrlAction.IGNORE_ABOUT_SCHEME;
import static com.mopub.common.UrlAction.HANDLE_PHONE_SCHEME;
import static com.mopub.common.UrlAction.OPEN_NATIVE_BROWSER;
import static com.mopub.common.UrlAction.OPEN_APP_MARKET;
import static com.mopub.common.UrlAction.OPEN_IN_APP_BROWSER;
import static com.mopub.common.UrlAction.HANDLE_SHARE_TWEET;
import static com.mopub.common.UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK;
import static com.mopub.common.UrlAction.FOLLOW_DEEP_LINK;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class UrlActionTest {

    @Test
    public void handleMopubScheme_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(HANDLE_MOPUB_SCHEME, "mopub:", true);
        assertUrlActionMatching(HANDLE_MOPUB_SCHEME, "MoPuB:", true);
    }

    @Test
    public void ignoreAboutScheme_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(IGNORE_ABOUT_SCHEME, "about:", true);
        assertUrlActionMatching(IGNORE_ABOUT_SCHEME, "AbOuT:", true);
    }

    @Test
    public void handlePhoneScheme_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "tel:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "TeL:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "voicemail:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "VoIcEmAiL:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "sms:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "SmS:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "mailto:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "MaIlTo:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "geo:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "GeO:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "google.streetview:", true);
        assertUrlActionMatching(HANDLE_PHONE_SCHEME, "GoOgLe.StReEtViEw:", true);
                
    }

    @Test
    public void openNativeBrowser_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(OPEN_NATIVE_BROWSER, "mopubnativebrowser:", true);
        assertUrlActionMatching(OPEN_NATIVE_BROWSER, "MoPuBnAtIvEbRoWsEr:", true);
    }

    @Test
    public void openAppMarket_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(OPEN_APP_MARKET, "https://play.google.com", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "https://PlAy.GoOgLe.CoM", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "https://market.android.com", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "https://MaRkEt.AnDrOiD.CoM", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "market:", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "MaRkEt:", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "play.google.com/", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "PlAy.GoOgLe.CoM/", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "market.android.com/", true);
        assertUrlActionMatching(OPEN_APP_MARKET, "MaRkEt.AnDrOiD.CoM/", true);
    }

    @Test
    public void openInAppBrowser_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(OPEN_IN_APP_BROWSER, "http:", true);
        assertUrlActionMatching(OPEN_IN_APP_BROWSER, "HtTp:", true);
        assertUrlActionMatching(OPEN_IN_APP_BROWSER, "https:", true);
        assertUrlActionMatching(OPEN_IN_APP_BROWSER, "HtTpS:", true);
    }

    @Test
    public void handleShareTweet_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(HANDLE_SHARE_TWEET, "mopubshare://tweet", true);
        assertUrlActionMatching(HANDLE_SHARE_TWEET, "MoPuBsHaRe://tweet", true);
        assertUrlActionMatching(HANDLE_SHARE_TWEET, "mopubshare://TwEeT", true);
    }

    @Test
    public void followDeepLinkWithFallback_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(FOLLOW_DEEP_LINK_WITH_FALLBACK, "deeplink+:", true);
        assertUrlActionMatching(FOLLOW_DEEP_LINK_WITH_FALLBACK, "DeEpLiNk+:", true);
    }

    @Test
    public void followDeepLink_shouldBeCaseInsensitive() throws Exception {
        assertUrlActionMatching(FOLLOW_DEEP_LINK, "myapp://", true);
        assertUrlActionMatching(FOLLOW_DEEP_LINK, "MyApP://", true);
        assertUrlActionMatching(FOLLOW_DEEP_LINK, "myapp://myview", true);
        assertUrlActionMatching(FOLLOW_DEEP_LINK, "myapp://MyView", true);
    }

    private void assertUrlActionMatching(@NonNull final UrlAction action,
            @Nullable final String url, final boolean shouldMatch) {
        assertThat(action.shouldTryHandlingUrl(Uri.parse(url))).isEqualTo(shouldMatch);
    }
}
