package com.mopub.common;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

import java.util.EnumSet;


/**
 * {@code UrlHandler} facilitates handling user clicks on different URLs, allowing configuration
 * for which kinds of URLs to handle and then responding accordingly for a given URL.
 *
 * This class is designed to be instantiated for a single use by immediately calling its {@link
 * #handleUrl(Context, String)} method upon constructing it.
 */
public class UrlHandler {

    /**
     * {@code ClickListener} defines the methods that {@link UrlHandler} calls when handling a
     * certain click succeeds or fails.
     */
    public interface ResultActions {
        /**
         * Called if the URL matched a supported {@link UrlAction} and was resolvable. Will be
         * called at most 1 times and is mutually exclusive with
         * {@link ResultActions#urlHandlingFailed(String, UrlAction)}.
         */
        void urlHandlingSucceeded(@NonNull final String url, @NonNull final UrlAction urlAction);

        /**
         * Called with {@link UrlAction#NOOP} if the URL did not match any supported
         * {@link UrlAction}s; or, called with the last matching {@link UrlAction} if URL was
         * unresolvable. Will be called at most 1 times and is mutually exclusive with
         * {@link ResultActions#urlHandlingSucceeded(String, UrlAction)}.
         */
        void urlHandlingFailed(@NonNull final String url,
                @NonNull final UrlAction lastFailedUrlAction);
    }

    /**
     * {@code MoPubSchemeListener} defines the methods that {@link UrlHandler} calls when handling
     * {@code HANDLE_MOPUB_SCHEME} URLs.
     */
    public interface MoPubSchemeListener {
        void onFinishLoad();
        void onClose();
        void onFailLoad();
    }

    /**
     * {@code Builder} provides an API to configure an immutable {@link UrlHandler} and create it.
     */
    public static class Builder {
        @NonNull
        private EnumSet<UrlAction> supportedUrlActions = EnumSet.of(UrlAction.NOOP);
        @NonNull
        private ResultActions resultActions = EMPTY_CLICK_LISTENER;
        @NonNull
        private MoPubSchemeListener moPubSchemeListener = EMPTY_MOPUB_SCHEME_LISTENER;
        private boolean skipShowMoPubBrowser = false;

        /**
         * Sets the {@link UrlAction}s to support in the {@code UrlHandler} to build.
         *
         * @param first A {@code UrlAction} for the {@code UrlHandler} to support.
         * @param others An arbitrary number of {@code UrlAction}s for the {@code UrlHandler} to
         * support.
         * @return A {@link com.mopub.common.UrlHandler.Builder} with the desired supported
         * {@code UrlAction}s added.
         */
        public Builder withSupportedUrlActions(@NonNull final UrlAction first,
                @Nullable final UrlAction... others) {
            this.supportedUrlActions = EnumSet.of(first, others);
            return this;
        }

        /**
         * Sets the {@link ResultActions} for the {@code UrlHandler} to
         * build.
         *
         * @param resultActions A {@code ClickListener} for the {@code UrlHandler}.
         * @return A {@link com.mopub.common.UrlHandler.Builder} with the desired
         * {@code ClickListener} added.
         */
        public Builder withResultActions(@NonNull final ResultActions resultActions) {
            this.resultActions = resultActions;
            return this;
        }

        /**
         * Sets the {@link com.mopub.common.UrlHandler.MoPubSchemeListener} for the
         * {@code UrlHandler} to build.
         *
         * @param moPubSchemeListener A {@code MoPubSchemeListener} for the {@code UrlHandler}.
         * @return A {@link com.mopub.common.UrlHandler.Builder} with the desired
         * {@code MoPubSchemeListener} added.
         */
        public Builder withMoPubSchemeListener(
                @NonNull final MoPubSchemeListener moPubSchemeListener) {
            this.moPubSchemeListener = moPubSchemeListener;
            return this;
        }

        /**
         * If called, will avoid starting a {@link MoPubBrowser} activity where applicable.
         * (see {@link Intents#showMoPubBrowserForUrl(Context, Uri)})
         *
         * @return A {@link com.mopub.common.UrlHandler.Builder} that will skip starting a
         * {@code MoPubBrowser}.
         */
        public Builder withoutMoPubBrowser() {
            this.skipShowMoPubBrowser = true;
            return this;
        }

        /**
         * Creates an immutable {@link UrlHandler} with the desired configuration, according to the
         * other {@link com.mopub.common.UrlHandler.Builder} methods called before.
         *
         * @return An immutable {@code UrlHandler} with the desired configuration.
         */
        public UrlHandler build() {
            return new UrlHandler(supportedUrlActions, resultActions, moPubSchemeListener,
                    skipShowMoPubBrowser);
        }
    }

    private static final ResultActions EMPTY_CLICK_LISTENER = new ResultActions() {
        @Override
        public void urlHandlingSucceeded(@NonNull String url, @NonNull UrlAction urlAction) { }
        @Override
        public void urlHandlingFailed(@NonNull String url, @NonNull UrlAction lastFailedUrlAction) { }
    };

    private static final MoPubSchemeListener EMPTY_MOPUB_SCHEME_LISTENER =
            new MoPubSchemeListener() {
        @Override public void onFinishLoad() { }

        @Override public void onClose() { }

        @Override public void onFailLoad() { }
    };

    @NonNull
    private EnumSet<UrlAction> mSupportedUrlActions;
    @NonNull
    private ResultActions mResultActions;
    @NonNull
    private MoPubSchemeListener mMoPubSchemeListener;
    private boolean mSkipShowMoPubBrowser;

    /**
     * Do not instantiate UrlHandler directly; use {@link com.mopub.common.UrlHandler.Builder}
     * instead.
     */
    private UrlHandler(
            @NonNull final EnumSet<UrlAction> supportedUrlActions,
            @NonNull final ResultActions resultActions,
            @NonNull final MoPubSchemeListener moPubSchemeListener,
            final boolean skipShowMoPubBrowser) {
        mResultActions = resultActions;
        mMoPubSchemeListener = moPubSchemeListener;
        mSkipShowMoPubBrowser = skipShowMoPubBrowser;
        mSupportedUrlActions = supportedUrlActions;
    }

    /**
     * Performs the actual click handling by verifying that the {@code destinationUrl} is one of
     * the configured supported {@link UrlAction}s and then handling it accordingly.
     *
     * @param context The activity context.
     * @param destinationUrl The URL to handle.
     */
    public void handleUrl(@NonNull final Context context, @NonNull final String destinationUrl) {
        handleUrl(context, destinationUrl, true);
    }

    /**
     * Performs the actual click handling by verifying that the {@code destinationUrl} is one of
     * the configured supported {@link UrlAction}s and then handling it accordingly.
     *
     * @param context The activity context.
     * @param destinationUrl The URL to handle.
     * @param fromUserInteraction Whether this handling was triggered from a user interaction.
     */
    public void handleUrl(@NonNull final Context context, @NonNull final String destinationUrl,
            final boolean fromUserInteraction) {
        UrlAction lastFailedUrlAction = UrlAction.NOOP;

        if (TextUtils.isEmpty(destinationUrl)) {
            MoPubLog.d("Attempted to handle empty url.");
        } else {
            final Uri destinationUri = Uri.parse(destinationUrl);
            for (final UrlAction urlAction : mSupportedUrlActions) {
                if (urlAction.shouldTryHandlingUrl(destinationUri)) {
                    try {
                        urlAction.handleUrl(context, destinationUri, fromUserInteraction,
                                mSkipShowMoPubBrowser, mMoPubSchemeListener);
                        if (!UrlAction.IGNORE_ABOUT_SCHEME.equals(urlAction) &&
                                !UrlAction.HANDLE_MOPUB_SCHEME.equals(urlAction)) {
                            mResultActions.urlHandlingSucceeded(destinationUri.toString(),
                                    urlAction);
                        }
                        return;
                    } catch (IntentNotResolvableException e) {
                        MoPubLog.d(e.getMessage(), e);
                        lastFailedUrlAction = urlAction;
                        // continue trying to match...
                    }
                }
            }
            MoPubLog.d("Link ignored. Unable to handle url: " + destinationUrl);
        }

        mResultActions.urlHandlingFailed(destinationUrl, lastFailedUrlAction);
    }
}
