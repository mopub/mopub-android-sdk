package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.net.URISyntaxException;

import static com.mopub.common.UrlAction.FOLLOW_DEEP_LINK;
import static com.mopub.common.UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK;
import static com.mopub.common.UrlAction.HANDLE_MOPUB_SCHEME;
import static com.mopub.common.UrlAction.HANDLE_PHONE_SCHEME;
import static com.mopub.common.UrlAction.HANDLE_SHARE_TWEET;
import static com.mopub.common.UrlAction.IGNORE_ABOUT_SCHEME;
import static com.mopub.common.UrlAction.NOOP;
import static com.mopub.common.UrlAction.OPEN_APP_MARKET;
import static com.mopub.common.UrlAction.OPEN_IN_APP_BROWSER;
import static com.mopub.common.UrlAction.OPEN_NATIVE_BROWSER;
import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class UrlHandlerTest {
    private Context context;
    @Mock private UrlHandler.ResultActions mockResultActions;
    @Mock private UrlHandler.MoPubSchemeListener mockMoPubSchemeListener;
    @Mock private MoPubRequestQueue mockRequestQueue;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get().getApplicationContext();

        // This url will be attempted when and intent:// url is not resolvable AND the app package
        // is missing (see Intents.launchApplicationIntent). In this case, we want the url to be
        // resolvable so the tests behave as a real device and actually attempt to open it.
        // This discrepancy between devices and test environment/emulators is was led to the
        // regression of ADF-2291.
        makeDeeplinkResolvable("market://details?id=null");
    }

    @Test
    public void urlHandler_withoutMoPubBrowser_shouldCallOnClickSuccessButNotStartActivity() {
        final String url = "https://www.mopub.com/";

        new UrlHandler.Builder()
                .withSupportedUrlActions(OPEN_IN_APP_BROWSER)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .withoutMoPubBrowser()
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeFinishLoad_shouldCallOnFinishLoad() {
        final String url = "mopub://finishLoad";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockMoPubSchemeListener).onFinishLoad();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeUppercasedFinishLoad_shouldCallOnFinishLoad() {
        final String url = "mopub://FiNiShLoAd";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockMoPubSchemeListener).onClose();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeUppercasedClose_shouldCallOnClose() {
        final String url = "mopub://ClOsE";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockMoPubSchemeListener).onFailLoad();
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withMatchingMoPubSchemeUppercasedFailLoad_shouldCallOnFailLoad() {
        final String url = "mopub://FaIlLoAd";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(url));
    }

    @Test
    public void urlHandler_withValidNativeBrowserUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String urlToLoad = "https://www.mopub.com/";
        final String url = "mopubnativebrowser://navigate?url=" + urlToLoad;

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_NATIVE_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(urlToLoad));
    }

    @Test
    public void urlHandler_withMatchingInAppBrowserHttpUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String url = "https://some_url";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
    }

    @Test
    public void urlHandler_withMatchingInAppBrowserHttpsUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String url = "https://www.mopub.com/";

        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME, HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK,
                        OPEN_IN_APP_BROWSER, HANDLE_PHONE_SCHEME, OPEN_NATIVE_BROWSER, HANDLE_SHARE_TWEET)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingSucceeded(url, OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
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
                .build().handleResolvedUrl(context, shareTweetUrl, true, null);

        verify(mockResultActions).urlHandlingSucceeded(shareTweetUrl, HANDLE_SHARE_TWEET);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_CHOOSER);
    }

    @Test
    public void urlHandler_withMatchingDeepLinkUrl_shouldCallOnClickSuccess_shouldStartActivity() {
        final String deepLinkUrl = "appscheme://host";
        makeDeeplinkResolvable(deepLinkUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, deepLinkUrl, true, null);

        verify(mockResultActions).urlHandlingSucceeded(deepLinkUrl, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(deepLinkUrl));
    }

    @Test
    public void urlHandler_withMatchingIntentUrl_shouldCallOnClickSuccess_shouldStartActivity() throws URISyntaxException {
        final String appPackage = "com.google.zxing.client.android";
        final String intentUrl = "intent://scan/#Intent;scheme=zxing;package=" + appPackage
                + ";end";
        makeIntentUrlResolvable(intentUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, intentUrl, true, null);

        verify(mockResultActions).urlHandlingSucceeded(intentUrl, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void urlHandler_withMatchingDeeplinkPlus_shouldCallOnClickSuccess_shouldStartActivity() {
        final String primaryUrl = "twitter://timeline";
        final String deeplinkPlusUrl = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl);
        makeDeeplinkResolvable("twitter://timeline");

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, deeplinkPlusUrl, true, null);

        verify(mockResultActions).urlHandlingSucceeded(deeplinkPlusUrl, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData()).isEqualTo(Uri.parse(primaryUrl));
    }

    @Test
    public void urlHandler_withMatchingUnresolvableDeeplinkPlus_withResolvableFallback_shouldResolveRedirects_shouldCallOnClickSuccess_shouldStartActivity() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "https://www.twitter.com";
        final String fallbackUrlAfterRedirects = "https://twitter.com/";
        final String deeplinkPlusUrl = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK, OPEN_IN_APP_BROWSER)
                .withResultActions(mockResultActions)
                .build().handleUrl(context, deeplinkPlusUrl);

        Robolectric.getBackgroundThreadScheduler().advanceBy(0);
        verify(mockResultActions).urlHandlingSucceeded(fallbackUrlAfterRedirects,
                OPEN_IN_APP_BROWSER);
        verifyNoMoreCallbacks();
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY))
                .isEqualTo(fallbackUrlAfterRedirects);
    }

    @Test
    public void urlHandler_withMatchingUnresolvableDeeplinkPlus_withUnresolvableFallback_shouldDoNothing() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "unresolvableUrl";
        final String deeplinkPlusUrl = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK, FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, deeplinkPlusUrl, true, null);

        verify(mockResultActions).urlHandlingFailed(fallbackUrl, NOOP);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withDeeplinkPlus_shouldTriggerPrimaryTracker() {
        final String primaryUrl = "twitter://timeline";
        final String primaryTracker = "https://ads.twitter.com/tracking?pubId=1234&userId=5678";
        final String fallbackUrl = "https://twitter.com";
        final String fallbackTracker =
                "https://ads.twitter.com/fallbackTracking?pubId=1234&userId=5678";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&primaryTrackingUrl=" + Uri.encode(primaryTracker)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl)
                + "&fallbackTrackingUrl=" + Uri.encode(fallbackTracker);
        makeDeeplinkResolvable(primaryUrl);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockRequestQueue).add(argThat(isUrl(primaryTracker)));
        verify(mockRequestQueue, never()).add(argThat(isUrl(fallbackTracker)));
    }

    @Test
    public void urlHandler_withDeeplinkPlus_shouldTriggerMultiplePrimaryTrackers() {
        final String primaryUrl = "twitter://timeline";
        final String primaryTracker1 = "https://ads.twitter.com/tracking?pubId=1234&userId=5678";
        final String primaryTracker2 = "https://ads.mopub.com/tracking?pubId=4321&userId=8765";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&primaryTrackingUrl=" + Uri.encode(primaryTracker1)
                + "&primaryTrackingUrl=" + Uri.encode(primaryTracker2);
        makeDeeplinkResolvable(primaryUrl);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockRequestQueue).add(argThat(isUrl(primaryTracker1)));
        verify(mockRequestQueue).add(argThat(isUrl(primaryTracker2)));
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withResolvableFallback_shouldTriggerFallbackTracker() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "https://twitter.com";
        final String primaryTracker = "https://ads.twitter.com/tracking?pubId=1234&userId=5678";
        final String fallbackTracker =
                "https://ads.twitter.com/fallbackTracking?pubId=1234&userId=5678";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&primaryTrackingUrl=" + Uri.encode(primaryTracker)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl)
                + "&fallbackTrackingUrl=" + Uri.encode(fallbackTracker);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK, OPEN_IN_APP_BROWSER)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockRequestQueue).add(argThat(isUrl(fallbackTracker)));
        verify(mockRequestQueue, never()).add(argThat(isUrl(primaryTracker)));
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withResolvableFallback_shouldTriggerMultiplePrimaryTrackers() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "https://twitter.com";
        final String fallbackTracker1 = "https://ads.twitter.com/tracking?pubId=1234&userId=5678";
        final String fallbackTracker2 = "https://ads.mopub.com/tracking?pubId=4321&userId=8765";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl)
                + "&fallbackTrackingUrl=" + Uri.encode(fallbackTracker1)
                + "&fallbackTrackingUrl=" + Uri.encode(fallbackTracker2);
        Networking.setRequestQueueForTesting(mockRequestQueue);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK, OPEN_IN_APP_BROWSER)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockRequestQueue).add(argThat(isUrl(fallbackTracker1)));
        verify(mockRequestQueue).add(argThat(isUrl(fallbackTracker2)));
    }

    @Test
    public void urlHandler_withdDeeplinkPlus_withUppercasedNavigate_shouldBeHandled() {
        final String primaryUrl = "twitter://timeline";
        final String url = "deeplink+://NaViGaTe?primaryUrl=" + Uri.encode(primaryUrl);
        makeDeeplinkResolvable(primaryUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingSucceeded(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
    }

    @Test
    public void urlHandler_withoutMatchingDeeplinkPlus_shouldDoNothing() {
        final String url = "NOTdeeplink+://navigate?primaryUrl=twitter%3A%2F%2Ftimeline";

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, NOOP);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withoutNavigate_shouldDoNothing() {
        final String url = "deeplink+://NOTnavigate?primaryUrl=twitter%3A%2F%2Ftimeline";

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withNestedDeeplinkPlus_shouldDoNothing() {
        final String deeplink = "deeplink+://navigate?primaryUrl=twitter%3A%2F%2Ftimeline";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(deeplink);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withDeeplinkPlusAsFallback_shouldDoNothing() {
        final String deeplink = "deeplink+://navigate?primaryUrl=twitter%3A%2F%2Ftimeline";
        final String url = "deeplink+://navigate?primaryUrl=missingApp%3A%2F%2FsomePath"
                + "&fallbackUrl=" + Uri.encode(deeplink);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withInvalidPrimaryUrl_shouldDoNothing() {
        final String url = "deeplink+://navigate?primaryUrl=INVALID";

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withDeeplinkPlus_withDecodedPrimaryUrl_shouldDoNothing() {
        final String url = "deeplink+://navigate?primaryUrl=twitter://timeline";

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK_WITH_FALLBACK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK_WITH_FALLBACK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withDualMatchingUnresolvableUrlActions_shouldCallOnClickFailOnLastMatchedAction() {
        final String url = "mopub://invalid";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME, FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withShareTweetAndDeepLink_shouldCallOnClickFailOnLastMatchedDeepLink() {
        final String url = "mopubshare://invalid";

        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_SHARE_TWEET, FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withNoConfiguration_shouldDoNothing() {
        new UrlHandler.Builder().build().handleResolvedUrl(context, "", true, null);

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutDestinationUrl_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, "", true, null);

        verify(mockResultActions).urlHandlingFailed("", NOOP);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutSupportedUrlActions_shouldNotError() {
        new UrlHandler.Builder()
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, "about:blank", true, null);

        verify(mockResultActions).urlHandlingFailed("about:blank", NOOP);
        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutClickListener_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, "about:blank", true, null);

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutMoPubSchemeListener_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .build().handleResolvedUrl(context, "about:blank", true, null);

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withoutMoPubBrowser_shouldNotError() {
        new UrlHandler.Builder()
                .withSupportedUrlActions(IGNORE_ABOUT_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .withoutMoPubBrowser()
                .build().handleResolvedUrl(context, "about:blank", true, null);

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
        makeDeeplinkResolvable(deepLinkUrl);
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
                .build().handleResolvedUrl(context, nullUrl, true, null);

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
                .build().handleResolvedUrl(context, emptyUrl, true, null);

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
                .build().handleResolvedUrl(context, invalidUrl, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

        verifyNoMoreCallbacks();
    }

    @Test
    public void urlHandler_withInvalidMoPubSchemeCustom_shouldNotError() {
        final String url = "mopub://custom?INVALID";
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_MOPUB_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);

        verify(mockResultActions).urlHandlingFailed(url, HANDLE_SHARE_TWEET);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    @Test
    public void urlHandler_withMatchingUnresolvableDeepLinkUrl_shouldCallUrlHandlingFailed() {
        final String deepLinkUrl = "appscheme://host";
        // The following code would make this url resolvable, so avoiding it to test for an
        // unresolvable url (yet included for documentation purposes).
        //makeDeeplinkResolvable(deepLinkUrl);

        new UrlHandler.Builder()
                .withSupportedUrlActions(FOLLOW_DEEP_LINK)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, deepLinkUrl, true, null);

        verify(mockResultActions).urlHandlingFailed(deepLinkUrl, FOLLOW_DEEP_LINK);
        verifyNoMoreCallbacks();
        verifyNoStartedActivity();
    }

    private void assertPhoneSchemeCallback(@NonNull final String url) {
        new UrlHandler.Builder()
                .withSupportedUrlActions(HANDLE_PHONE_SCHEME)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);

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
                .build().handleResolvedUrl(context, url, true, null);
        verify(mockResultActions).urlHandlingFailed(url, expectedFailUrlAction);
        verifyNoMoreCallbacks();
    }

    private void assertCallbackWithoutMatchingSupportedUrlAction(@NonNull final String url,
            @NonNull final UrlAction... otherTypes) {
        new UrlHandler.Builder()
                .withSupportedUrlActions(NOOP, otherTypes)
                .withResultActions(mockResultActions)
                .withMoPubSchemeListener(mockMoPubSchemeListener)
                .build().handleResolvedUrl(context, url, true, null);
        verify(mockResultActions).urlHandlingFailed(url, NOOP);
        verifyNoMoreCallbacks();
    }

    private void verifyNoMoreCallbacks() {
        verifyNoMoreInteractions(mockResultActions);
        verifyNoMoreInteractions(mockMoPubSchemeListener);
    }

    private void verifyNoStartedActivity() {
        assertThat(ShadowApplication.getInstance().peekNextStartedActivity()).isNull();
    }

    private void makeDeeplinkResolvable(String deeplink) {
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(deeplink)), new ResolveInfo());
    }

    private void makeIntentUrlResolvable(String intentUrl) throws URISyntaxException {
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(
                Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME), new ResolveInfo());
    }
}
