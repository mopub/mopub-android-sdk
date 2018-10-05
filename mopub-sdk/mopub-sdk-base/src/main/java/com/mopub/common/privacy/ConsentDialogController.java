// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;

public class ConsentDialogController implements ConsentDialogRequest.Listener {
    @NonNull
    private final Context mAppContext;

    @Nullable private String mHtmlBody;
    @Nullable private ConsentDialogListener mExtListener;
    volatile boolean mReady;
    volatile boolean mRequestInFlight;
    private final Handler mHandler;

    ConsentDialogController(@NonNull final Context appContext) {
        Preconditions.checkNotNull(appContext);

        mAppContext = appContext.getApplicationContext();
        mHandler = new Handler();
    }

    @Override
    public void onSuccess(final ConsentDialogResponse response) {
        mRequestInFlight = false;
        mHtmlBody = response.getHtml();
        if (TextUtils.isEmpty(mHtmlBody)) {
            mReady = false;
            if (mExtListener != null) {
                mExtListener.onConsentDialogLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
            }
            return;
        }

        mReady = true;
        if (mExtListener != null) {
            mExtListener.onConsentDialogLoaded();
        }
    }

    @Override
    public void onErrorResponse(final VolleyError volleyError) {
        final ConsentDialogListener loadListener = mExtListener;
        resetState();

        if (loadListener == null) {
            return;
        }

        if (volleyError instanceof MoPubNetworkError) {
            switch(((MoPubNetworkError) volleyError).getReason()) {
                case BAD_BODY:
                    loadListener.onConsentDialogLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
                    return;
                default:
                    break;
            }
        }

        loadListener.onConsentDialogLoadFailed(MoPubErrorCode.UNSPECIFIED);
    }

    synchronized void loadConsentDialog(@Nullable final ConsentDialogListener listener,
            @Nullable final Boolean gdprApplies,
            @NonNull final PersonalInfoData personalInfoData) {
        Preconditions.checkNotNull(personalInfoData);

        if (mReady) {
            if (listener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onConsentDialogLoaded();
                    }
                });
            }
            return;
        } else if (mRequestInFlight) {
            MoPubLog.d("Already making a consent dialog load request.");
            return;
        }

        mExtListener = listener;
        mRequestInFlight = true;

        ConsentDialogRequest consentDialogRequest = new ConsentDialogRequest(mAppContext,
                new ConsentDialogUrlGenerator(mAppContext, personalInfoData.getAdUnitId(),
                        personalInfoData.getConsentStatus().getValue())
                        .withGdprApplies(gdprApplies)
                        .withConsentedPrivacyPolicyVersion(personalInfoData.getConsentedPrivacyPolicyVersion())
                        .withConsentedVendorListVersion(personalInfoData.getConsentedVendorListVersion())
                        .withForceGdprApplies(personalInfoData.isForceGdprApplies())
                        .generateUrlString(Constants.HOST), this);
        Networking.getRequestQueue(mAppContext).add(consentDialogRequest);
    }

    boolean showConsentDialog() {
        if (!mReady || TextUtils.isEmpty(mHtmlBody)) {
            return false;
        }

        ConsentDialogActivity.start(mAppContext, mHtmlBody);
        resetState();
        return true;
    }

    boolean isReady() {
        return mReady;
    }

    private void resetState() {
        mRequestInFlight = false;
        mReady = false;
        mExtListener = null;
        mHtmlBody = null;
    }
}
