package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;

import java.io.Serializable;
import java.util.List;

import static com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest;

public class VastCompanionAdConfig implements Serializable {
    private static final long serialVersionUID = 0L;

    private final int mWidth;
    private final int mHeight;
    @NonNull private final VastResource mVastResource;
    @Nullable private final String mClickThroughUrl;
    @NonNull private final List<VastTracker> mClickTrackers;
    @NonNull private final List<VastTracker> mCreativeViewTrackers;

    public VastCompanionAdConfig(
            int width,
            int height,
            @NonNull VastResource vastResource,
            @Nullable String clickThroughUrl,
            @NonNull List<VastTracker> clickTrackers,
            @NonNull List<VastTracker> creativeViewTrackers) {
        Preconditions.checkNotNull(vastResource);
        Preconditions.checkNotNull(clickTrackers, "clickTrackers cannot be null");
        Preconditions.checkNotNull(creativeViewTrackers, "creativeViewTrackers cannot be null");

        mWidth = width;
        mHeight = height;
        mVastResource = vastResource;
        mClickThroughUrl = clickThroughUrl;
        mClickTrackers = clickTrackers;
        mCreativeViewTrackers = creativeViewTrackers;
    }

    /**
     * Add click trackers.
     *
     * @param clickTrackers List of URLs to hit
     */
    public void addClickTrackers(@NonNull final List<VastTracker> clickTrackers) {
        Preconditions.checkNotNull(clickTrackers, "clickTrackers cannot be null");
        mClickTrackers.addAll(clickTrackers);
    }

    /**
     * Add creativeView trackers that are supposed to be fired when the companion ad is visible.
     *
     * @param creativeViewTrackers List of URLs to hit when this companion is viewed
     */
    public void addCreativeViewTrackers(@NonNull final List<VastTracker> creativeViewTrackers) {
        Preconditions.checkNotNull(creativeViewTrackers, "creativeViewTrackers cannot be null");
        mCreativeViewTrackers.addAll(creativeViewTrackers);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @NonNull
    public VastResource getVastResource() {
        return mVastResource;
    }

    @Nullable
    public String getClickThroughUrl() {
        return mClickThroughUrl;
    }

    @NonNull
    public List<VastTracker> getClickTrackers() {
        return mClickTrackers;
    }

    @NonNull
    public List<VastTracker> getCreativeViewTrackers() {
        return mCreativeViewTrackers;
    }

    /**
     * Called when the companion ad is displayed after the video. Handles firing the impression
     * trackers.
     *
     * @param context         the context.
     * @param contentPlayHead the time into the video. (should be equal to the duration)
     */
    void handleImpression(@NonNull Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context);

        makeVastTrackingHttpRequest(
                mCreativeViewTrackers,
                null,
                contentPlayHead,
                null,
                context
        );
    }

    /**
     * Called when the companion ad is clicked. Handles forwarding the user to the specified click
     * through uri.
     *
     * @param context                the context. Has to be an activity context.
     * @param requestCode            The code that identifies what kind of activity request is going
     *                               to be made
     * @param webViewClickThroughUrl The clickthrough url from the webview that should override the
     *                               one set in the companion ad if the resource is Javascript,
     *                               HTML, or an IFrame.
     */
    void handleClick(@NonNull final Context context, final int requestCode,
            @Nullable final String webViewClickThroughUrl) {
        Preconditions.checkNotNull(context);
        Preconditions.checkArgument(context instanceof Activity, "context must be an activity");

        final String correctClickThroughUrl = mVastResource.getCorrectClickThroughUrl(
                mClickThroughUrl, webViewClickThroughUrl);

        if (TextUtils.isEmpty(correctClickThroughUrl)) {
            return;
        }

        new UrlHandler.Builder()
                .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_APP_MARKET,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_IN_APP_BROWSER,
                        UrlAction.HANDLE_SHARE_TWEET,
                        UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                        UrlAction.FOLLOW_DEEP_LINK)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (urlAction == UrlAction.OPEN_IN_APP_BROWSER) {
                            Bundle bundle = new Bundle();
                            bundle.putString(MoPubBrowser.DESTINATION_URL_KEY,
                                    url);

                            final Class clazz = MoPubBrowser.class;
                            final Intent intent = Intents.getStartActivityIntent(
                                    context, clazz, bundle);
                            try {
                                ((Activity) context).startActivityForResult(intent, requestCode);
                            } catch (ActivityNotFoundException e) {
                                MoPubLog.d("Activity " + clazz.getName() + " not found. Did you " +
                                        "declare it in your AndroidManifest.xml?");
                            }
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withoutMoPubBrowser()
                .build().handleUrl(context, correctClickThroughUrl);
    }
}
