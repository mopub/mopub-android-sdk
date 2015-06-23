package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static com.mopub.common.UrlAction.FOLLOW_DEEP_LINK;
import static com.mopub.common.UrlAction.HANDLE_MOPUB_SCHEME;
import static com.mopub.common.UrlAction.HANDLE_PHONE_SCHEME;
import static com.mopub.common.UrlAction.HANDLE_SHARE_TWEET;
import static com.mopub.common.UrlAction.IGNORE_ABOUT_SCHEME;
import static com.mopub.common.UrlAction.NOOP;
import static com.mopub.common.UrlAction.OPEN_IN_APP_BROWSER;
import static com.mopub.common.UrlAction.OPEN_NATIVE_BROWSER;
import static com.mopub.common.UrlAction.OPEN_APP_MARKET;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
public class UrlHandlerTest {
    private Context context;
    @Mock private UrlHandler.ResultActions mockResultActions;
    @Mock private UrlHandler.MoPubSchemeListener mockMoPubSchemeListener;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get().getApplicationContext();
    }

    @Test
    public void urlHandler_withoutMoPubBrowser_shouldCallOnClickSuccessButNotStartActivity() {
        final String url = "http://some_url";

        new UrlHandler.Builder()
                .withSupportedUrlActions(OPEN_IN_APP_BROWSER)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .withoutMoPubBrowser()
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeFinishLoad_shouldCallOnFinishLoad() {
        final String url = "mopub://finishLoad";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockMoPubSchemeListener).onFinishLoad();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeClose_shouldCallOnClose() {
        final String url = "mopub://close";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockMoPubSchemeListener).onClose();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeFailLoad_shouldCallOnFailLoad() {
        final String url = "mopub://failLoad";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockMoPubSchemeListener).onFailLoad();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingAboutSchemeUrl_shouldIgnoreClick() {
        final String url = "about:blank";
        new UrlHandler.Builder()
                .withSupportedUrlActions(
                        HANDLE_MOPUB_SCHEME,
                        IGNORE_ABOUT_SCHEME,
                        HANDLE_PHONE_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeTelUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("tel:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeVoicemailUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("voicemail:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeSMSUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("sms:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeMailToUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("mailto:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeGeoUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("geo:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeStreetViewUrl_shouldCallOnClickSuccess() {
        assertPhoneSchemeCallback("google.streetview:");
    }

    @Test
    public void urlHandler_withMatchingPhoneSchemeUrl_shouldStartActivity() {
        final String url = "tel:1234567890";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(url));
    }

    @Test
    public void urlHandler_withValidNativeBrowserUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String urlToLoad = "some_url";
        final String url = "mopubnativebrowser://navigate?url=" + urlToLoad;

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_NATIVE_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(urlToLoad));
    }

    @Test
    public void urlHandler_withMatchingInAppBrowserHttpUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String url = "http://some_url";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
    }

    @Test
    public void urlHandler_withMatchingInAppBrowserHttpsUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String url = "https://some_url";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
    }

    @Test
    public void urlHandler_withMatchingShareUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String shareTweetUrl = "mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .build().handleUrl(context, shareTweetUrl);

        verify(mockResultActions).urlHandlingSucceeded(shareTweetUrl, HANDLE_SHARE_TWEET);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_CHOOSER);
    }

    @Test
    public void urlHandler_withMatchingDeepLinkUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String deepLinkUrl = "appscheme://host";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(deepLinkUrl)), new ResolveInfo());

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, deepLinkUrl);

        verify(mockResultActions).urlHandlingSucceeded(deepLinkUrl, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
        final Intent startedActivity = Robolectric.getShadowApplication().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(deepLinkUrl));
    }

    @Test
    public void urlHandler_withDualMatchingUnresolvableUrlActions_shouldCallOnClickFailOnLastMatchedAction() {
        final String url = "mopub://invalid";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withShareTweetAndDeepLink_shouldCallOnClickFailOnLastMatchedDeepLink() {
        final String url = "mopubshare://invalid";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET, FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withNoConfiguration_shouldDoNothing() {
        new UrlHandler.Builder().build().handleUrl(context, "");

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutDestinationUrl_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, "");

        verify(mockResultActions).urlHandlingFailed("", NOOP);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutSupportedUrlActions_shouldNotError() {
        new UrlHandler.Builder()
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, "about:blank");

        verify(mockResultActions).urlHandlingFailed("about:blank", NOOP);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutClickListener_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, "about:blank");

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutMoPubSchemeListener_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .build().handleUrl(context, "about:blank");

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutMoPubBrowser_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .withoutMoPubBrowser()
                .build().handleUrl(context, "about:blank");

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutMatchingAboutSchemeUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(IGNORE_ABOUT_SCHEME);
    }

    @Test
    public void urlHandler_withoutMatchingMoPubSchemeUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(HANDLE_MOPUB_SCHEME);
    }

    @Test
    public void urlHandler_withoutMatchingDeepLinkUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(FOLLOW_DEEP_LINK);
    }

    @Test
    public void urlHandler_withoutMatchingInAppBrowserUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(OPEN_IN_APP_BROWSER);
    }

    @Test
    public void urlHandler_withoutMatchingPhoneSchemeUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(HANDLE_PHONE_SCHEME);
    }

    @Test
    public void urlHandler_withoutMatchingNativeBrowserUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(OPEN_NATIVE_BROWSER);
    }

    @Test
    public void urlHandler_withoutMatchingShareTweetUrl_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingUrl(HANDLE_SHARE_TWEET);
    }

    /**
     * For the next few unit tests urlHandler_withoutMatching[some]UrlAction_shouldCallOnClickFail,
     * do not include FOLLOW_DEEP_LINK, since it would be a catch-all and trigger urlHandlingSucceeded.
     */

    @Test
    public void urlHandler_withoutMatchingAboutSchemeUrlAction_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingSupportedUrlAction("about:blank", HANDLE_MOPUB_SCHEME,
                OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingMoPubSchemeUrlAction_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingSupportedUrlAction("mopub://close", IGNORE_ABOUT_SCHEME,
                OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingDeepLinkUrlAction_shouldCallUrlHandlingFailed() {
        final String deepLinkUrl = "appscheme://host";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(deepLinkUrl)), new ResolveInfo());
        assertCallbackWithoutMatchingSupportedUrlAction(deepLinkUrl, IGNORE_ABOUT_SCHEME,
                HANDLE_MOPUB_SCHEME, OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER,
                HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingInAppBrowserUrlAction_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingSupportedUrlAction("https://some_url", IGNORE_ABOUT_SCHEME,
                HANDLE_MOPUB_SCHEME, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingPhoneSchemeUrlAction_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingSupportedUrlAction("tel:1234567890", IGNORE_ABOUT_SCHEME,
                HANDLE_MOPUB_SCHEME, OPEN_IN_APP_BROWSER, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingNativeBrowserUrlAction_shouldCallUrlHandlingFailed() {
        assertCallbackWithoutMatchingSupportedUrlAction("mopubnativebrowser://navigate?url=some_url",
                IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME,
                HANDLE_SHARE_TWEET);
    }

    @Test
    public void urlHandler_withoutMatchingShareTweetUrlAction_shouldCallUrlHandlingFailed() {
        final String shareTweetUrl = "mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802";
        assertCallbackWithoutMatchingSupportedUrlAction(shareTweetUrl, HANDLE_MOPUB_SCHEME,
                IGNORE_ABOUT_SCHEME, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, OPEN_APP_MARKET,
                OPEN_IN_APP_BROWSER);
    }

    @Test
    public void urlHandler_withNullDestinationURL_shouldDoNothing() {
        final String nullUrl = null;
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, nullUrl);

        verify(mockResultActions).urlHandlingFailed(nullUrl, NOOP);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withEmptyDestinationURL_shouldDoNothing() {
        final String emptyUrl = "";
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, emptyUrl);

        verify(mockResultActions).urlHandlingFailed(emptyUrl, NOOP);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withInvalidDestinationURL_shouldDoNothing() {
        final String invalidUrl = "some_invalid_url";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, invalidUrl);

        verify(mockResultActions).urlHandlingFailed(invalidUrl, NOOP);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeWithoutMoPubSchemeListener_shouldDoNothing() {
        final String url = "mopub://finishLoad";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .build().handleUrl(context, url);

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withInvalidMoPubSchemeCustom_shouldNotError() {
        final String url = "mopub://custom?INVALID";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, HANDLE_MOPUB_SCHEME);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withInvalidNativeBrowserUrl_shouldCallUrlHandlingFailed() {
        final String url = "mopubnativebrowser://INVALID";

        new UrlHandler.Builder()
                .withSupportedUrlActions(OPEN_NATIVE_BROWSER)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, OPEN_NATIVE_BROWSER);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withInvalidHostInShareTweetUrl_shouldCallUrlHandlingFailed() {
        final String url = "mopubshare://invalid";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, NOOP);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withMissingQueryParametersInShareTweetUrl_shouldCallUrlHandlingFailed() {
        final String url = "mopubshare://tweet?x=1&y=2";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, HANDLE_SHARE_TWEET);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withEmptyQueryParametersInShareTweetUrl_shouldCallUrlHandlingFailed() {
        final String url = "mopubshare://tweet?screen_name=&tweet_id=";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingFailed(url, HANDLE_SHARE_TWEET);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withMatchingUnresolvableDeepLinkUrl_shouldCallUrlHandlingFailed() {
        final String deepLinkUrl = "appscheme://host";
        // The following code would make this url resolvable, so avoiding it to test for an
        // unresolvable url (yet included for documentation purposes).
        //  Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
        //          Uri.parse(deepLinkUrl)), new ResolveInfo());

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, deepLinkUrl);

        verify(mockResultActions).urlHandlingFailed(deepLinkUrl, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    private void assertPhoneSchemeCallback(@NonNull final String url) {
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_PHONE_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);

        verify(mockResultActions).urlHandlingSucceeded(url, HANDLE_PHONE_SCHEME);
        verifyNoMoreCallbacks();
    }

    private void assertCallbackWithoutMatchingUrl(@NonNull final UrlAction urlAction) {
        final String url = "non://matching_url";
        UrlAction expectedFailUrlAction = NOOP;

        if (urlAction.equals(FOLLOW_DEEP_LINK)) {
            expectedFailUrlAction = FOLLOW_DEEP_LINK;
        }

        new UrlHandler.Builder()
                .withSupportedUrlActions(urlAction)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);
        verify(mockResultActions).urlHandlingFailed(url, expectedFailUrlAction);
        verifyNoMoreCallbacks();
    }

    private void assertCallbackWithoutMatchingSupportedUrlAction(@NonNull final String url,
            @NonNull final UrlAction... otherTypes) {
        new UrlHandler.Builder()
                .withSupportedUrlActions(UrlAction.NOOP, otherTypes)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url);
        verify(mockResultActions).urlHandlingFailed(url, NOOP);
        verifyNoMoreCallbacks();
    }

    private void verifyNoMoreCallbacks() {
        verifyNoMoreInteractions(mockResultActions);
        verifyNoMoreInteractions(mockMoPubSchemeListener);
    }

    private void verifyNoStartedActivity() {
        assertThat(Robolectric.getShadowApplication().peekNextStartedActivity()).isNull();
    }
}
