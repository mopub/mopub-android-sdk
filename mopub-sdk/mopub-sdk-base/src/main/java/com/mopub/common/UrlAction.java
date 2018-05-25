package com.mopub.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.exceptions.UrlParseException;

import java.net.URISyntaxException;
import java.util.List;

import static com.mopub.common.Constants.HTTP;
import static com.mopub.common.Constants.HTTPS;
import static com.mopub.common.MoPub.getBrowserAgent;
import static com.mopub.network.TrackingRequest.makeTrackingHttpRequest;

/**
 * {@code UrlAction} describes the different kinds of actions for URLs that {@link UrlHandler} can
 * potentially perform and how to match against each URL.
 */
public enum UrlAction {
    /**
     * NOTE: The order in which these are defined determines the priority when matching URLs!
     * If a URL matches multiple Url Actions, it will be handled by the one that appears first in
     * this enum (see {@link UrlHandler#handleUrl(Context, String)}).
     *
     * Each UrlAction includes its ordinal in a comment as a reminder of this fact.
     */

    /* 0 */ HANDLE_MOPUB_SCHEME(false) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            return "mopub".equalsIgnoreCase(uri.getScheme());
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            final String host = uri.getHost();
            final UrlHandler.MoPubSchemeListener moPubSchemeListener =
                    urlHandler.getMoPubSchemeListener();

            if ("finishLoad".equalsIgnoreCase(host)) {
                moPubSchemeListener.onFinishLoad();
            } else if ("close".equalsIgnoreCase(host)) {
                moPubSchemeListener.onClose();
            } else if ("failLoad".equalsIgnoreCase(host)) {
                moPubSchemeListener.onFailLoad();
            } else {
                throw new IntentNotResolvableException("Could not handle MoPub Scheme url: " + uri);
            }
        }
    },

    /* 1 */ IGNORE_ABOUT_SCHEME(false) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            return "about".equalsIgnoreCase(uri.getScheme());
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            MoPubLog.d("Link to about page ignored.");
        }
    },

    /* 2 */ HANDLE_PHONE_SCHEME(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            final String scheme = uri.getScheme();
            return "tel".equalsIgnoreCase(scheme) || "voicemail".equalsIgnoreCase(scheme)
                    || "sms".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)
                    || "geo".equalsIgnoreCase(scheme)
                    || "google.streetview".equalsIgnoreCase(scheme);
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            final String errorMessage = "Could not handle intent with URI: " + uri + "\n\tIs " +
                    "this intent supported on your phone?";
            Intents.launchActionViewIntent(context, uri, errorMessage);
        }
    },

    /* 3 */ OPEN_NATIVE_BROWSER(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            final String scheme = uri.getScheme();

            if (HTTP.equalsIgnoreCase(scheme) || HTTPS.equalsIgnoreCase(scheme)) {
                return getBrowserAgent() == BrowserAgent.NATIVE;
            }

            return "mopubnativebrowser".equalsIgnoreCase(scheme);
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            final String errorMessage = "Unable to load mopub native browser url: " + uri;
            try {
                final Intent intent = Intents.intentForNativeBrowserScheme(uri);
                Intents.launchIntentForUserClick(context, intent, errorMessage);
            } catch (UrlParseException e) {
                throw new IntentNotResolvableException(errorMessage + "\n\t" + e.getMessage());
            }
        }
    },

    /* 4 */ OPEN_APP_MARKET(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            final String scheme = uri.getScheme();
            final String host = uri.getHost();

            return "play.google.com".equalsIgnoreCase(host)
                    || "market.android.com".equalsIgnoreCase(host)
                    || "market".equalsIgnoreCase(scheme)
                    || uri.toString().toLowerCase().startsWith("play.google.com/")
                    || uri.toString().toLowerCase().startsWith("market.android.com/");
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            Intents.launchApplicationUrl(context, uri);
        }
    },

    /* 5 */ OPEN_IN_APP_BROWSER(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            final String scheme = uri.getScheme();
            return (HTTP.equalsIgnoreCase(scheme) || HTTPS.equalsIgnoreCase(scheme));
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            if (!urlHandler.shouldSkipShowMoPubBrowser()) {
                Intents.showMoPubBrowserForUrl(context, uri, creativeId);
            }
        }
    },

    /**
     * This handles tweet sharing via the chooser dialog.
     * See {@link Intents#intentForShareTweet(Uri)} for more details.
     */
    /* 6 */ HANDLE_SHARE_TWEET(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            Preconditions.checkNotNull(uri);
            return "mopubshare".equalsIgnoreCase(uri.getScheme())
                    && "tweet".equalsIgnoreCase(uri.getHost());
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(uri);

            final String chooserText = "Share via";
            final String errorMessage = "Could not handle share tweet intent with URI " + uri;
            try {
                final Intent shareTweetIntent = Intents.intentForShareTweet(uri);
                final Intent chooserIntent = Intent.createChooser(shareTweetIntent, chooserText);
                Intents.launchIntentForUserClick(context, chooserIntent, errorMessage);
            } catch (UrlParseException e) {
                throw new IntentNotResolvableException(errorMessage + "\n\t" + e.getMessage());
            }
        }
    },

    /* 7 */ FOLLOW_DEEP_LINK_WITH_FALLBACK(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            return "deeplink+".equalsIgnoreCase(uri.getScheme());
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {

            // 1. Parse the URL as a valid deeplink+
            if (!"navigate".equalsIgnoreCase(uri.getHost())) {
                throw new IntentNotResolvableException("Deeplink+ URL did not have 'navigate' as" +
                        " the host.");
            }

            final String primaryUrl;
            final List<String> primaryTrackingUrls;
            final String fallbackUrl;
            final List<String> fallbackTrackingUrls;
            try {
                primaryUrl = uri.getQueryParameter("primaryUrl");
                primaryTrackingUrls = uri.getQueryParameters("primaryTrackingUrl");
                fallbackUrl = uri.getQueryParameter("fallbackUrl");
                fallbackTrackingUrls = uri.getQueryParameters("fallbackTrackingUrl");
            } catch (UnsupportedOperationException e) {
                // If the URL is not hierarchical, getQueryParameter[s] will throw
                // UnsupportedOperationException (see https://developer.android.com/reference/android/net/Uri.html#getQueryParameter(java.lang.String)
                throw new IntentNotResolvableException("Deeplink+ URL was not a hierarchical" +
                        " URI.");
            }

            if (primaryUrl == null) {
                throw new IntentNotResolvableException("Deeplink+ did not have 'primaryUrl' query" +
                        " param.");
            }

            final Uri primaryUri = Uri.parse(primaryUrl);
            if (shouldTryHandlingUrl(primaryUri)) {
                // Nested Deeplink+ URLs are not allowed
                throw new IntentNotResolvableException("Deeplink+ had another Deeplink+ as the " +
                        "'primaryUrl'.");
            }

            // 2. Attempt to handle the primary URL
            try {
                Intents.launchApplicationUrl(context, primaryUri);
                makeTrackingHttpRequest(primaryTrackingUrls, context);
                return;
            } catch (IntentNotResolvableException e) {
                // Primary URL failed; proceed to attempt fallback URL
            }

            // 3. Attempt to handle the fallback URL
            if (fallbackUrl == null) {
                throw new IntentNotResolvableException("Unable to handle 'primaryUrl' for " +
                        "Deeplink+ and 'fallbackUrl' was missing.");
            }

            if (shouldTryHandlingUrl(Uri.parse(fallbackUrl))) {
                // Nested Deeplink+ URLs are not allowed
                throw new IntentNotResolvableException("Deeplink+ URL had another Deeplink+ " +
                        "URL as the 'fallbackUrl'.");
            }

            // UrlAction.handleUrl already verified this comes from a user interaction
            final boolean fromUserInteraction = true;
            urlHandler.handleUrl(context, fallbackUrl, true, fallbackTrackingUrls);
        }
    },

    /* 8 */ FOLLOW_DEEP_LINK(true) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            final String scheme = uri.getScheme();
            return !TextUtils.isEmpty(scheme);
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable String creativeId)
                throws IntentNotResolvableException {
            if (Constants.INTENT_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                try {
                    final Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                    Intents.launchApplicationIntent(context, intent);
                } catch (URISyntaxException e) {
                    throw new IntentNotResolvableException("Intent uri had invalid syntax: "
                            + uri.toString());
                }
            } else {
                Intents.launchApplicationUrl(context, uri);
            }
        }
    },

    /* This is essentially an "unspecified" value for UrlAction. */
    NOOP(false) {
        @Override
        public boolean shouldTryHandlingUrl(@NonNull final Uri uri) {
            return false;
        }

        @Override
        protected void performAction(
                @NonNull final Context context, @NonNull final Uri uri,
                @NonNull final UrlHandler urlHandler,
                @Nullable final String creativeId)
                throws IntentNotResolvableException { }
    };

    public void handleUrl(
            UrlHandler urlHandler,
            @NonNull final Context context,
            @NonNull final Uri destinationUri,
            final boolean fromUserInteraction,
            @Nullable String creativeId)
            throws IntentNotResolvableException {
        MoPubLog.d("Ad event URL: " + destinationUri);
        if (mRequiresUserInteraction && !fromUserInteraction) {
            throw new IntentNotResolvableException("Attempted to handle action without user " +
                    "interaction.");
        } else {
            performAction(context, destinationUri, urlHandler, creativeId);
        }
    }

    private final boolean mRequiresUserInteraction;

    UrlAction(boolean requiresUserInteraction) {
        mRequiresUserInteraction = requiresUserInteraction;
    }

    public abstract boolean shouldTryHandlingUrl(@NonNull final Uri uri);

    protected abstract void performAction(
            @NonNull final Context context, @NonNull final Uri uri,
            @NonNull final UrlHandler urlHandler,
            @Nullable String creativeId)
            throws IntentNotResolvableException;
}
