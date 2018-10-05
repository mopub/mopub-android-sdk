// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;

import static com.mopub.common.Constants.GDPR_CONSENT_HANDLER;

public class ConsentDialogUrlGenerator extends BaseUrlGenerator {
    /**
     * Current device default language.
     */
    private static final String LANGUAGE_KEY = "language";

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mAdUnitId;
    @NonNull
    private final String mCurrentConsentStatus;
    @Nullable
    private Boolean mGdprApplies;
    private boolean mForceGdprApplies;
    @Nullable
    private String mConsentedVendorListVersion;
    @Nullable
    private String mConsentedPrivacyPolicyVersion;

    ConsentDialogUrlGenerator(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull final String currentConsentStatus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(currentConsentStatus);

        mContext = context.getApplicationContext();
        mAdUnitId = adUnitId;
        mCurrentConsentStatus = currentConsentStatus;
    }

    protected ConsentDialogUrlGenerator withGdprApplies(@Nullable final Boolean gdprApplies) {
        mGdprApplies = gdprApplies;
        return this;
    }

    protected ConsentDialogUrlGenerator withForceGdprApplies(final boolean forceGdprApplies) {
        mForceGdprApplies = forceGdprApplies;
        return this;
    }

    protected ConsentDialogUrlGenerator withConsentedVendorListVersion(
            @Nullable final String consentedVendorListVersion) {
        mConsentedVendorListVersion = consentedVendorListVersion;
        return this;
    }

    protected ConsentDialogUrlGenerator withConsentedPrivacyPolicyVersion(
            @Nullable final String consentedPrivacyPolicyVersion) {
        mConsentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
        return this;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, GDPR_CONSENT_HANDLER);

        addParam(AD_UNIT_ID_KEY, mAdUnitId);
        addParam(CURRENT_CONSENT_STATUS_KEY, mCurrentConsentStatus);
        addParam(SDK_VERSION_KEY, MoPub.SDK_VERSION);
        addParam(LANGUAGE_KEY, ClientMetadata.getCurrentLanguage(mContext));
        addParam(GDPR_APPLIES, mGdprApplies);
        addParam(FORCE_GDPR_APPLIES, mForceGdprApplies);
        addParam(CONSENTED_VENDOR_LIST_VERSION_KEY, mConsentedVendorListVersion);
        addParam(CONSENTED_PRIVACY_POLICY_VERSION_KEY, mConsentedPrivacyPolicyVersion);
        addParam(BUNDLE_ID_KEY, ClientMetadata.getInstance(mContext).getAppPackageName());

        return getFinalUrlString();
    }
}
