package com.mopub.common.privacy;

import android.support.annotation.NonNull;

import com.mopub.mobileads.MoPubErrorCode;

/**
 * Use this interface to listen to a successful or failed consent dialog load request.
 */
public interface ConsentDialogListener {
    /**
     * Called when the consent dialog successfully loads.
     */
    void onConsentDialogLoaded();

    /**
     * Called when the consent dialog fails to load.
     *
     * @param moPubErrorCode The reason why the dialog failed to load.
     */
    void onConsentDialogLoadFailed(@NonNull final MoPubErrorCode moPubErrorCode);
}
