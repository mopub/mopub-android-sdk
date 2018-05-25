package com.mopub.common.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Constants;
import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.logging.MoPubLog;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.exceptions.UrlParseException;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.mopub.common.MoPub.getBrowserAgent;

public class Intents {

    private Intents() {}

    public static void startActivity(@NonNull final Context context, @NonNull final Intent intent)
            throws IntentNotResolvableException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(intent);

        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            throw new IntentNotResolvableException(e);
        }
    }

    /**
     * Adding FLAG_ACTIVITY_NEW_TASK with startActivityForResult will always result in a
     * RESULT_CANCELED, so don't use it for Activity contexts.
     */
    public static Intent getStartActivityIntent(@NonNull final Context context,
            @NonNull final Class clazz, @Nullable final Bundle extras) {
        final Intent intent = new Intent(context, clazz);

        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }

    public static boolean deviceCanHandleIntent(@NonNull final Context context,
            @NonNull final Intent intent) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            return !activities.isEmpty();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Native Browser Scheme URLs provide a means for advertisers to include links that click out to
     * an external browser, rather than the MoPub in-app browser. Properly formatted native browser
     * URLs take the form of "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.mopub.com".
     *
     * @param uri The Native Browser Scheme URL to open in the external browser.
     * @return An Intent that will open an app-external browser taking the user to a page specified
     * in the query parameter of the passed-in url.
     * @throws UrlParseException if the provided url has an invalid format or is non-hierarchical.
     */
    public static Intent intentForNativeBrowserScheme(@NonNull final Uri uri)
            throws UrlParseException {
        Preconditions.checkNotNull(uri);

        if (!UrlAction.OPEN_NATIVE_BROWSER.shouldTryHandlingUrl(uri)) {
            String supportedSchemes = "mopubnativebrowser://";
            if (getBrowserAgent() == BrowserAgent.NATIVE) {
                supportedSchemes += ", http://, or https://";
            }
            throw new UrlParseException("URI does not have " + supportedSchemes + " scheme.");
        }

        if ("mopubnativebrowser".equalsIgnoreCase(uri.getScheme())) {
            final Uri intentUri = parseMoPubNativeBrowserUri(uri);
            return new Intent(Intent.ACTION_VIEW, intentUri);
        }

        if (getBrowserAgent() == BrowserAgent.NATIVE) {
            return new Intent(Intent.ACTION_VIEW, uri);
        }

        // Should never get here
        throw new UrlParseException("Invalid URI: " + uri.toString());
    }

    private static Uri parseMoPubNativeBrowserUri(@NonNull final Uri uri)
            throws UrlParseException {
        Preconditions.checkNotNull(uri);

        if (!"navigate".equals(uri.getHost())) {
            throw new UrlParseException("URL missing 'navigate' host parameter.");
        }

        final String urlToOpenInNativeBrowser;
        try {
            urlToOpenInNativeBrowser = uri.getQueryParameter("url");
        } catch (UnsupportedOperationException e) {
            // Accessing query parameters only makes sense for hierarchical URIs as per:
            // https://developer.android.com/reference/android/net/Uri.html#getQueryParameter(java.lang.String)
            MoPubLog.w("Could not handle url: " + uri);
            throw new UrlParseException("Passed-in URL did not create a hierarchical URI.");
        }

        if (urlToOpenInNativeBrowser == null) {
            throw new UrlParseException("URL missing 'url' query parameter.");
        }

        return Uri.parse(urlToOpenInNativeBrowser);
    }

    /**
     * Share Tweet URLs provide a means for advertisers on Twitter to include tweet links
     * promoting their products that can be shared via supporting apps on the device.  Any
     * app with a filter that matches ACTION_SEND and MIME type text/plain is capable of sharing
     * the tweet link.
     *
     * Properly formatted share tweet URLs take the form of
     * "mopubshare://tweet?screen_name=<SCREEN_NAME>&tweet_id=<TWEET_ID>"
     *
     * Both screen_name and tweet_id are required query parameters.  This method does not verify
     * that their values are existent and valid on Twitter, but UrlParseException would be thrown
     * if either is missing or empty.
     *
     * Example user flow:
     * Upon clicking "mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802" in an ad,
     * a chooser dialog with message "Share via" pops up listing existing apps on the device
     * capable of sharing this tweet link.  After the user chooses an app to share the tweet,
     * the message “Check out @SpaceX's Tweet: https://twitter.com/SpaceX/status/596026229536460802”
     * is ready to be shared in the chosen app.
     *
     * @param uri The Share Tweet URL indicating the tweet to share
     * @return An ACTION_SEND intent that will be wrapped in a chooser intent
     * @throws UrlParseException if the provided url has an invalid format or is non-hierarchical
     */
    public static Intent intentForShareTweet(@NonNull final Uri uri)
            throws UrlParseException {
        if (!UrlAction.HANDLE_SHARE_TWEET.shouldTryHandlingUrl(uri)) {
            throw new UrlParseException("URL does not have mopubshare://tweet? format.");
        }

        final String screenName;
        final String tweetId;

        try {
            screenName = uri.getQueryParameter("screen_name");
            tweetId = uri.getQueryParameter("tweet_id");
        } catch (UnsupportedOperationException e) {
            // Accessing query parameters only makes sense for hierarchical URIs as per:
            // https://developer.android.com/reference/android/net/Uri.html#getQueryParameter(java.lang.String)
            MoPubLog.w("Could not handle url: " + uri);
            throw new UrlParseException("Passed-in URL did not create a hierarchical URI.");
        }

        // If either query parameter is null or empty, throw UrlParseException
        if (TextUtils.isEmpty(screenName)) {
            throw new UrlParseException("URL missing non-empty 'screen_name' query parameter.");
        }
        if (TextUtils.isEmpty(tweetId)) {
            throw new UrlParseException("URL missing non-empty 'tweet_id' query parameter.");
        }

        // Derive the tweet link on Twitter
        final String tweetUrl = String.format("https://twitter.com/%s/status/%s", screenName, tweetId);

        // Compose the share message
        final String shareMessage = String.format("Check out @%s's Tweet: %s", screenName, tweetUrl);

        // Construct share intent with the shareMessage in subject and text
        Intent shareTweetIntent = new Intent(Intent.ACTION_SEND);
        shareTweetIntent.setType("text/plain");
        shareTweetIntent.putExtra(Intent.EXTRA_SUBJECT, shareMessage);
        shareTweetIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        return shareTweetIntent;
    }

    /**
     * Launches a {@link MoPubBrowser} activity with the desired URL.
     * @param context The activity context.
     * @param uri The URL to load in the started {@link MoPubBrowser} activity.
     */
    public static void showMoPubBrowserForUrl(@NonNull final Context context,
            @NonNull Uri uri,
            @Nullable String dspCreativeId)
            throws IntentNotResolvableException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        MoPubLog.d("Final URI to show in browser: " + uri);

        final Bundle extras = new Bundle();
        extras.putString(MoPubBrowser.DESTINATION_URL_KEY, uri.toString());
        if (!TextUtils.isEmpty(dspCreativeId)) {
            extras.putString(MoPubBrowser.DSP_CREATIVE_ID, dspCreativeId);
        }
        Intent intent = getStartActivityIntent(context, MoPubBrowser.class, extras);

        String errorMessage = "Could not show MoPubBrowser for url: " + uri + "\n\tPerhaps you " +
                "forgot to declare com.mopub.common.MoPubBrowser in your Android manifest file.";

        launchIntentForUserClick(context, intent, errorMessage);
    }

    public static void launchIntentForUserClick(@NonNull final Context context,
            @NonNull final Intent intent, @Nullable final String errorMessage)
            throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(intent);

        try {
            startActivity(context, intent);
        } catch (IntentNotResolvableException e) {
            throw new IntentNotResolvableException(errorMessage + "\n" + e.getMessage());
        }
    }

    public static void launchApplicationUrl(@NonNull final Context context,
            @NonNull final Uri uri) throws IntentNotResolvableException {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        if (deviceCanHandleIntent(context, intent)) {
            launchApplicationIntent(context, intent);
        } else {
            // Deeplink+ needs this exception to know primaryUrl failed and then attempt fallbackUrl
            // See UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK
            throw new IntentNotResolvableException("Could not handle application specific " +
                    "action: " + uri + "\n\tYou may be running in the emulator or another " +
                    "device which does not have the required application.");
        }
    }

    public static void launchApplicationIntent(@NonNull final Context context,
            @NonNull final Intent intent) throws IntentNotResolvableException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(intent);

        if (deviceCanHandleIntent(context, intent)) {
            final String errorMessage = "Unable to open intent: " + intent;
            if (!(context instanceof Activity)) {
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            }
            launchIntentForUserClick(context, intent, errorMessage);
        } else {
            final String fallbackUrl = intent.getStringExtra("browser_fallback_url");
            if (TextUtils.isEmpty(fallbackUrl)) {
                if (!"market".equalsIgnoreCase(intent.getScheme())) {
                    launchApplicationUrl(context, getPlayStoreUri(intent));
                } else {
                    throw new IntentNotResolvableException("Device could not handle neither " +
                            "intent nor market url.\nIntent: " + intent.toString());
                }
            } else {
                final Uri fallbackUri = Uri.parse(fallbackUrl);
                final String fallbackScheme = fallbackUri.getScheme();
                if (Constants.HTTP.equalsIgnoreCase(fallbackScheme)
                        || Constants.HTTPS.equalsIgnoreCase(fallbackScheme)) {
                    showMoPubBrowserForUrl(context, fallbackUri, null);
                } else {
                    launchApplicationUrl(context, fallbackUri);
                }
            }
        }
    }

    @NonNull
    public static Uri getPlayStoreUri(@NonNull final Intent intent) {
        Preconditions.checkNotNull(intent);

        return Uri.parse("market://details?id=" + intent.getPackage());
    }

    public static void launchActionViewIntent(@NonNull final Context context,
            @NonNull final Uri uri,
            @Nullable final String errorMessage) throws IntentNotResolvableException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }
        launchIntentForUserClick(context, intent, errorMessage);
    }

    /**
     * @deprecated as of 4.7.0. Use {@link #deviceCanHandleIntent(Context, Intent)}
     */
    @Deprecated
    public static boolean canHandleApplicationUrl(final Context context, final Uri uri) {
        return false;
    }

    /**
     * @deprecated as of 4.7.0. Use {@link #deviceCanHandleIntent(Context, Intent)}
     */
    @Deprecated
    public static boolean canHandleApplicationUrl(final Context context, final Uri uri,
            final boolean logError) {
        return false;
    }
}
