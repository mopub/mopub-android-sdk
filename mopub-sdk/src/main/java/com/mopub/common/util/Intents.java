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

import com.mopub.common.MoPubBrowser;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.logging.MoPubLog;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.exceptions.UrlParseException;

import java.util.EnumSet;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

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

    public static boolean canHandleApplicationUrl(final Context context, final Uri uri) {
        return canHandleApplicationUrl(context, uri, true);
    }

    public static boolean canHandleApplicationUrl(final Context context, final Uri uri,
            final boolean logError) {
        // Determine which activities can handle the intent
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        // If there are no relevant activities, don't follow the link
        if (!Intents.deviceCanHandleIntent(context, intent)) {
            if (logError) {
                MoPubLog.w("Could not handle application specific action: " + uri + ". " +
                        "You may be running in the emulator or another device which does not " +
                        "have the required application.");
            }
            return false;
        }

        return true;
    }

    /**
     * Native Browser Scheme URLs provide a means for advertisers to include links that click out to
     * an external browser, rather than the MoPub in-app browser. Properly formatted native browser
     * URLs take the form of "mopubnativebrowser://navigate?url=http%3A%2F%2Fwww.mopub.com".
     *
     * @param uri The Native Browser Scheme URL to open in the external browser.
     * @return An Intent that will open an app-external browser taking the user to a page specified
     * in the query parameter of the passed-in url
     * @throws UrlParseException if the provided url has an invalid format or is non-hierarchical
     */
    public static Intent intentForNativeBrowserScheme(@NonNull final Uri uri)
            throws UrlParseException {
        Preconditions.checkNotNull(uri);

        if (!UrlAction.OPEN_NATIVE_BROWSER.shouldTryHandlingUrl(uri)) {
            throw new UrlParseException("URL does not have mopubnativebrowser:// scheme.");
        }

        if (!"navigate".equals(uri.getHost())) {
            throw new UrlParseException("URL missing 'navigate' host parameter.");
        }

        final String urlToOpenInNativeBrowser;
        try {
            urlToOpenInNativeBrowser = uri.getQueryParameter("url");
        } catch (UnsupportedOperationException e) {
            // Accessing query parameters only makes sense for hierarchical URIs as per:
            // http://developer.android.com/reference/android/net/Uri.html#getQueryParameter(java.lang.String)
            MoPubLog.w("Could not handle url: " + uri);
            throw new UrlParseException("Passed-in URL did not create a hierarchical URI.");
        }

        if (urlToOpenInNativeBrowser == null) {
            throw new UrlParseException("URL missing 'url' query parameter.");
        }

        final Uri intentUri = Uri.parse(urlToOpenInNativeBrowser);
        return new Intent(Intent.ACTION_VIEW, intentUri);
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
            // http://developer.android.com/reference/android/net/Uri.html#getQueryParameter(java.lang.String)
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
            @NonNull Uri uri)
            throws IntentNotResolvableException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        MoPubLog.d("Final URI to show in browser: " + uri);

        final Bundle extras = new Bundle();
        extras.putString(MoPubBrowser.DESTINATION_URL_KEY, uri.toString());
        Intent intent = getStartActivityIntent(context, MoPubBrowser.class, extras);

        String errorMessage = "Could not show MoPubBrowser for url: " + uri + "\n\tPerhaps you " +
                "forgot to declare com.mopub.common.MoPubBrowser in your Android manifest file.";

        launchIntentForUserClick(context, intent, errorMessage);
    }

    public static void launchActionViewIntent(Context context, @NonNull final Uri uri,
            @NonNull final String errorMessage) throws IntentNotResolvableException {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (!(context instanceof Activity)) {
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        }

        launchIntentForUserClick(context, intent, errorMessage);
    }

    public static void launchIntentForUserClick(@NonNull final Context context,
            @NonNull final Intent intent, @Nullable final String errorMessage)
            throws IntentNotResolvableException {
        Preconditions.NoThrow.checkNotNull(context);
        Preconditions.NoThrow.checkNotNull(intent);

        try {
            Intents.startActivity(context, intent);
        } catch (IntentNotResolvableException e) {
            throw new IntentNotResolvableException(errorMessage + "\n" + e.getMessage());
        }
    }

    public static void launchApplicationUrl(@NonNull final Context context,
            @NonNull final Uri uri) throws IntentNotResolvableException {
        if (Intents.canHandleApplicationUrl(context, uri)) {
            final String errorMessage = "Unable to open intent for: " + uri;
            Intents.launchActionViewIntent(context, uri, errorMessage);
        } else {
            throw new IntentNotResolvableException("Could not handle application specific " +
                    "action: " + uri + "\n\tYou may be running in the emulator or another " +
                    "device which does not have the required application.");
        }
    }
}
