package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.VisibleForTesting;

/**
 * A collection of methods to help with detecting clicks, and handling click destination urls
 * for native ads.
 */
public class NativeClickHandler {
    @NonNull private final Context mContext;
    @Nullable private final String mDspCreativeId;

    private boolean mClickInProgress;

    public NativeClickHandler(@NonNull final Context context) {
        this(context, null);
    }

    public NativeClickHandler(@NonNull final Context context, @Nullable final String dspCreativeId) {
        Preconditions.checkNotNull(context);
        mContext = context.getApplicationContext();
        mDspCreativeId = dspCreativeId;
    }

    /**
     * Sets the on click listener on all views in the native ad view hierarchy to invoke
     * {@link ClickInterface#handleClick(View)} when a view in the view hierarchy is clicked.
     *
     * @param view The top view of the native ad view hierarchy
     * @param clickInterface The native ad implementing the click interface
     */
    public void setOnClickListener(@NonNull final View view,
            @NonNull final ClickInterface clickInterface) {
        if (!Preconditions.NoThrow.checkNotNull(view, "Cannot set click listener on a null view")) {
            return;
        }
        if (!Preconditions.NoThrow.checkNotNull(clickInterface,
                "Cannot set click listener with a null ClickInterface")) {
            return;
        }

        setOnClickListener(view, new OnClickListener() {
            @Override
            public void onClick(View v) {
                clickInterface.handleClick(v);
            }
        });
    }

    /**
     * Uses recursion in order to set the on click listener on all views in the view hierarchy.
     * This is necessary since certain views, such as a button, will not forward the click event
     * to their parent.
     *
     * @param view The top view of the native ad view hierarchy
     * @param onClickListener The click listener to be invoked on click
     */
    private void setOnClickListener(@NonNull final View view,
            @Nullable final OnClickListener onClickListener) {
        view.setOnClickListener(onClickListener);
        if ((view instanceof ViewGroup)) {
            ViewGroup viewGroup = (ViewGroup)view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setOnClickListener(viewGroup.getChildAt(i), onClickListener);
            }
        }
    }

    /**
     * Clears the on click listener from all views in the the native ad view hierarchy.
     *
     * @param view The top view of the native ad view hierarchy.
     */
    public void clearOnClickListener(@NonNull final View view) {
        if (!Preconditions.NoThrow.checkNotNull(view, "Cannot clear click listener from a null view")) {
            return;
        }

        setOnClickListener(view, (OnClickListener) null);
    }

    /**
     * Handles resolving and opening the click destination url. A spinning progress view is shown
     * while the click destination is being resolved.
     *
     * @param clickDestinationUrl The click destination url
     * @param view The view on which to display the spinning progress view
     */
    public void openClickDestinationUrl(@NonNull final String clickDestinationUrl,
            @Nullable final View view) {
        openClickDestinationUrl(clickDestinationUrl, view, new SpinningProgressView(mContext));
    }

    @VisibleForTesting
    void openClickDestinationUrl(@NonNull final String clickDestinationUrl,
            @Nullable final View view,
            @NonNull final SpinningProgressView spinningProgressView) {
        // Use NoThrow here because the clickDestinationUrl will be passed in
        // by third party custom event writers
        if (!Preconditions.NoThrow.checkNotNull(clickDestinationUrl,
                "Cannot open a null click destination url")) {
            return;
        }
        Preconditions.checkNotNull(spinningProgressView);

        if (mClickInProgress) {
            return;
        }
        mClickInProgress = true;

        if (view != null) {
            spinningProgressView.addToRoot(view);
        }

        UrlHandler.Builder builder = new UrlHandler.Builder();
        if (!TextUtils.isEmpty(mDspCreativeId)) {
            builder.withDspCreativeId(mDspCreativeId);
        }
        builder.withSupportedUrlActions(
                UrlAction.IGNORE_ABOUT_SCHEME,
                UrlAction.OPEN_NATIVE_BROWSER,
                UrlAction.OPEN_APP_MARKET,
                UrlAction.OPEN_IN_APP_BROWSER,
                UrlAction.HANDLE_SHARE_TWEET,
                UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                UrlAction.FOLLOW_DEEP_LINK)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        removeSpinningProgressView();
                        mClickInProgress = false;
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                        removeSpinningProgressView();
                        mClickInProgress = false;
                    }

                    private void removeSpinningProgressView() {
                        if (view != null) {
                            spinningProgressView.removeFromRoot();
                        }
                    }
                })
                .build().handleUrl(mContext, clickDestinationUrl);
    }
}
