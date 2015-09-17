package com.mopub.common;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.event.BaseEvent;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

import java.util.EnumSet;

import static com.mopub.common.UrlResolutionTask.UrlResolutionListener;
import static com.mopub.network.TrackingRequest.makeTrackingHttpRequest;

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
         * @return A {@link Builder} with the desired supported {@code UrlAction}s added.
         */
        public Builder withSupportedUrlActions(@NonNull final UrlAction first,
                @Nullable final UrlAction... others) {
            this.supportedUrlActions = EnumSet.of(first, others);
            return this;
        }

        /**
         * Sets the {@link UrlAction}s to support in the {@code UrlHandler} to build.
         *
         * @param supportedUrlActions An {@code EnumSet} of {@code UrlAction}s for the
         * {@code UrlHandler} to support.
         * @return A {@link Builder} with the desired supported {@code UrlAction}s added.
         */
        public Builder withSupportedUrlActions(
                @NonNull final EnumSet<UrlAction> supportedUrlActions) {
            this.supportedUrlActions = EnumSet.copyOf(supportedUrlActions);
            return this;
        }
        
        /**
         * Sets the {@link ResultActions} for the {@code UrlHandler} to
         * build.
         *
         * @param resultActions A {@code ClickListener} for the {@code UrlHandler}.
         * @return A {@link Builder} with the desired {@code ClickListener} added.
         */
        public Builder withResultActions(@NonNull final ResultActions resultActions) {
            this.resultActions = resultActions;
            return this;
        }

        /**
         * Sets the {@link MoPubSchemeListener} for the {@code UrlHandler} to build.
         *
         * @param moPubSchemeListener A {@code MoPubSchemeListener} for the {@code UrlHandler}.
         * @return A {@link Builder} with the desired {@code MoPubSchemeListener} added.
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
         * @return A {@link Builder} that will skip starting a {@code MoPubBrowser}.
         */
        public Builder withoutMoPubBrowser() {
            this.skipShowMoPubBrowser = true;
            return this;
        }

        /**
         * Creates an immutable {@link UrlHandler} with the desired configuration, according to the
         * other {@link Builder} methods called before.
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
    private boolean mAlreadySucceeded;
    private boolean mTaskPending;

    /**
     * Do not instantiate UrlHandler directly; use {@link Builder} instead.
     */
    private UrlHandler(
            @NonNull final EnumSet<UrlAction> supportedUrlActions,
            @NonNull final ResultActions resultActions,
            @NonNull final MoPubSchemeListener moPubSchemeListener,
            final boolean skipShowMoPubBrowser) {
        mSupportedUrlActions = EnumSet.copyOf(supportedUrlActions);
        mResultActions = resultActions;
        mMoPubSchemeListener = moPubSchemeListener;
        mSkipShowMoPubBrowser = skipShowMoPubBrowser;
        mAlreadySucceeded = false;
        mTaskPending = false;
    }

    @NonNull
    EnumSet<UrlAction> getSupportedUrlActions() {
        return EnumSet.copyOf(mSupportedUrlActions);
    }

    @NonNull
    ResultActions getResultActions() {
        return mResultActions;
    }

    @NonNull
    MoPubSchemeListener getMoPubSchemeListener() {
        return mMoPubSchemeListener;
    }

    boolean shouldSkipShowMoPubBrowser() {
        return mSkipShowMoPubBrowser;
    }

    /**
     * Performs the actual click handling by verifying that the {@code destinationUrl} is one of
     * the configured supported {@link UrlAction}s and then handling it accordingly.
     *
     * @param context The activity context.
     * @param destinationUrl The URL to handle.
     */
    public void handleUrl(@NonNull final Context context, @NonNull final String destinationUrl) {
        Preconditions.checkNotNull(context);

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
        Preconditions.checkNotNull(context);

        handleUrl(context, destinationUrl, fromUserInteraction, null);
    }

    /**
     * Follows any redirects from {@code destinationUrl} and then handles the URL accordingly.
     *
     * @param context The activity context.
     * @param destinationUrl The URL to handle.
     * @param fromUserInteraction Whether this handling was triggered from a user interaction.
     * @param trackingUrls Optional tracking URLs to trigger on success
     */
    public void handleUrl(@NonNull final Context context, @NonNull final String destinationUrl,
            final boolean fromUserInteraction, @Nullable final Iterable<String> trackingUrls) {
        Preconditions.checkNotNull(context);

        if (TextUtils.isEmpty(destinationUrl)) {
            failUrlHandling(destinationUrl, null, "Attempted to handle empty url.", null);
            return;
        }

        final UrlResolutionListener urlResolutionListener = new UrlResolutionListener() {
            @Override
            public void onSuccess(@NonNull final String resolvedUrl) {
                mTaskPending = false;
                handleResolvedUrl(context, resolvedUrl, fromUserInteraction, trackingUrls);
            }

            @Override
            public void onFailure(@NonNull final String message,
                    @Nullable final Throwable throwable) {
                mTaskPending = false;
                failUrlHandling(destinationUrl, null, message, throwable);

            }

        };

        UrlResolutionTask.getResolvedUrl(destinationUrl, urlResolutionListener);
        mTaskPending = true;
    }

    /**
     * Performs the actual url handling by verifying that the {@code destinationUrl} is one of
     * the configured supported {@link UrlAction}s and then handling it accordingly.
     *
     * @param context The activity context.
     * @param url The URL to handle.
     * @param fromUserInteraction Whether this handling was triggered from a user interaction.
     * @param trackingUrls Optional tracking URLs to trigger on success
     * @return true if the given URL was successfully handled; false otherwise
     */
    public boolean handleResolvedUrl(@NonNull final Context context,
            @NonNull final String url, final boolean fromUserInteraction,
            @Nullable Iterable<String> trackingUrls) {
        if (TextUtils.isEmpty(url)) {
            failUrlHandling(url, null, "Attempted to handle empty url.", null);
            return false;
        }

        UrlAction lastFailedUrlAction = UrlAction.NOOP;
        final Uri destinationUri = Uri.parse(url);

        for (final UrlAction urlAction : mSupportedUrlActions) {
            if (urlAction.shouldTryHandlingUrl(destinationUri)) {
                try {
                    urlAction.handleUrl(UrlHandler.this, context, destinationUri,
                            fromUserInteraction);
                    if (!mAlreadySucceeded && !mTaskPending
                            && !UrlAction.IGNORE_ABOUT_SCHEME.equals(urlAction)
                            && !UrlAction.HANDLE_MOPUB_SCHEME.equals(urlAction)) {
                        makeTrackingHttpRequest(trackingUrls, context,
                                BaseEvent.Name.CLICK_REQUEST);
                        mResultActions.urlHandlingSucceeded(destinationUri.toString(),
                                urlAction);
                        mAlreadySucceeded = true;
                    }
                    return true;
                } catch (IntentNotResolvableException e) {
                    MoPubLog.d(e.getMessage(), e);
                    lastFailedUrlAction = urlAction;
                    // continue trying to match...
                }
            }
        }
        failUrlHandling(url, lastFailedUrlAction, "Link ignored. Unable to handle url: " + url, null);
        return false;
    }

    private void failUrlHandling(@Nullable final String url, @Nullable UrlAction urlAction,
            @NonNull final String message, @Nullable final Throwable throwable) {
        Preconditions.checkNotNull(message);

        if (urlAction == null) {
            urlAction = UrlAction.NOOP;
        }

        MoPubLog.d(message, throwable);
        mResultActions.urlHandlingFailed(url, urlAction);
    }

}
