package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.UrlHandler;
import com.mopub.common.UrlAction;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Iterator;

class ClickDestinationResolutionListener implements UrlResolutionTask.UrlResolutionListener {
    private final Context mContext;
    private final Iterator<String> mUrlIterator;
    private final WeakReference<SpinningProgressView> mSpinningProgressView;

    public ClickDestinationResolutionListener(@NonNull final Context context,
            @NonNull final Iterator<String> urlIterator,
            @NonNull final SpinningProgressView spinningProgressView) {
        mContext = context.getApplicationContext();
        mUrlIterator = urlIterator;
        mSpinningProgressView = new WeakReference<SpinningProgressView>(spinningProgressView);
    }

    /**
     * Called upon user click, after the corresponding UrlResolutionTask has followed all redirects
     * successfully. Attempts to open mopubnativebrowser links in the device browser, deep-links in
     * the corresponding application, and all other links in the MoPub in-app browser. In the first
     * two cases, malformed URLs will try to fallback to the next entry in mUrlIterator, and failing
     * that, will no-op.
     */
    @Override
    public void onSuccess(@NonNull final String resolvedUrl) {
        new UrlHandler.Builder()
                .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_APP_MARKET,
                        UrlAction.OPEN_IN_APP_BROWSER,
                        UrlAction.HANDLE_SHARE_TWEET,
                        UrlAction.FOLLOW_DEEP_LINK)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                        if (mUrlIterator.hasNext()) {
                            UrlResolutionTask.getResolvedUrl(mUrlIterator.next(),
                                    ClickDestinationResolutionListener.this);
                        }
                    }
                })
                .build().handleUrl(mContext, resolvedUrl);
        removeSpinningProgressView();
    }

    @Override
    public void onFailure() {
        MoPubLog.d("Failed to resolve URL for click.");
        removeSpinningProgressView();
    }

    private void removeSpinningProgressView() {
        final SpinningProgressView spinningProgressView = mSpinningProgressView.get();
        if (spinningProgressView != null) {
            spinningProgressView.removeFromRoot();
        }
    }
}
