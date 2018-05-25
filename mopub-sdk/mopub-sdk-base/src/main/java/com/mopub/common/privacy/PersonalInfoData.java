package com.mopub.common.privacy;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Preconditions;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.VisibleForTesting;

import java.util.Locale;

class PersonalInfoData implements ConsentData {

    private static final String PERSONAL_INFO_DATA_SHARED_PREFS = "com.mopub.privacy";
    private static final String PERSONAL_INFO_PREFIX = "info/";
    private static final String AD_UNIT_ID_SP_KEY = PERSONAL_INFO_PREFIX + "adunit";
    private static final String CONSENT_STATUS_SP_KEY = PERSONAL_INFO_PREFIX + "consent_status";
    private static final String LAST_SUCCESSFULLY_SYNCED_CONSENT_STATUS_SP_KEY = PERSONAL_INFO_PREFIX + "last_successfully_synced_consent_status";
    private static final String IS_WHITELISTED_SP_KEY = PERSONAL_INFO_PREFIX + "is_whitelisted";
    private static final String CURRENT_VENDOR_LIST_VERSION_SP_KEY = PERSONAL_INFO_PREFIX + "current_vendor_list_version";
    private static final String CURRENT_VENDOR_LIST_LINK_SP_KEY = PERSONAL_INFO_PREFIX + "current_vendor_list_link";
    private static final String CURRENT_PRIVACY_POLICY_VERSION_SP_KEY = PERSONAL_INFO_PREFIX + "current_privacy_policy_version";
    private static final String CURRENT_PRIVACY_POLICY_LINK_SP_KEY = PERSONAL_INFO_PREFIX + "current_privacy_policy_link";
    private static final String CURRENT_VENDOR_LIST_IAB_FORMAT_SP_KEY = PERSONAL_INFO_PREFIX + "current_vendor_list_iab_format";
    private static final String CURRENT_VENDOR_LIST_IAB_HASH_SP_KEY = PERSONAL_INFO_PREFIX + "current_vendor_list_iab_hash";
    private static final String CONSENTED_VENDOR_LIST_VERSION_SP_KEY = PERSONAL_INFO_PREFIX + "consented_vendor_list_version";
    private static final String CONSENTED_PRIVACY_POLICY_VERSION_SP_KEY = PERSONAL_INFO_PREFIX + "consented_privacy_policy_version";
    private static final String CONSENTED_VENDOR_LIST_IAB_FORMAT_SP_KEY = PERSONAL_INFO_PREFIX + "consented_vendor_list_iab_format";
    private static final String EXTRAS_SP_KEY = PERSONAL_INFO_PREFIX + "extras";
    private static final String CONSENT_CHANGE_REASON_SP_KEY = PERSONAL_INFO_PREFIX + "consent_change_reason";
    private static final String REACQUIRE_CONSENT_SP_KEY = PERSONAL_INFO_PREFIX + "reacquire_consent";
    private static final String GDPR_APPLIES_SP_KEY = PERSONAL_INFO_PREFIX + "gdpr_applies";
    private static final String UDID_SP_KEY = PERSONAL_INFO_PREFIX + "udid";
    private static final String LAST_CHANGED_MS_SP_KEY = PERSONAL_INFO_PREFIX + "last_changed_ms";
    private static final String CONSENT_STATUS_BEFORE_DNT_SP_KEY = PERSONAL_INFO_PREFIX + "consent_status_before_dnt";

    /**
     * If this is found in a url, replace it with the device default language.
     */
    private static final String LANGUAGE_MACRO_KEY = "%%LANGUAGE%%";

    @NonNull private final Context mAppContext;

    // Values that are locally generated
    @NonNull private String mAdUnitId;
    @NonNull private ConsentStatus mConsentStatus;
    @Nullable private ConsentStatus mLastSuccessfullySyncedConsentStatus;
    @Nullable private String mConsentChangeReason;
    @Nullable private String mUdid;
    @Nullable private String mLastChangedMs;
    @Nullable private ConsentStatus mConsentStatusBeforeDnt;

    // From server
    private boolean mIsWhitelisted;
    @Nullable private String mCurrentVendorListVersion;
    @Nullable private String mCurrentVendorListLink;
    @Nullable private String mCurrentPrivacyPolicyVersion;
    @Nullable private String mCurrentPrivacyPolicyLink;
    @Nullable private String mCurrentVendorListIabFormat;
    @Nullable private String mCurrentVendorListIabHash;
    @Nullable private String mConsentedVendorListVersion;
    @Nullable private String mConsentedPrivacyPolicyVersion;
    @Nullable private String mConsentedVendorListIabFormat;
    @Nullable private String mExtras;
    private boolean mReacquireConsent;
    @Nullable private Boolean mGdprApplies;

    PersonalInfoData(@NonNull final Context context, @NonNull final String adUnitId) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adUnitId);

        mAppContext = context.getApplicationContext();
        mConsentStatus = ConsentStatus.UNKNOWN;
        getStateFromDisk();
        mAdUnitId = adUnitId;
    }

    private void getStateFromDisk() {
        final SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(
                mAppContext, PERSONAL_INFO_DATA_SHARED_PREFS);
        mAdUnitId = sharedPreferences.getString(AD_UNIT_ID_SP_KEY, "");
        mConsentStatus = ConsentStatus.fromString(sharedPreferences.getString(
                CONSENT_STATUS_SP_KEY, ConsentStatus.UNKNOWN.name()));
        final String lastSuccessfullySyncedConsentStatusString = sharedPreferences.getString(
                LAST_SUCCESSFULLY_SYNCED_CONSENT_STATUS_SP_KEY, null);
        if (TextUtils.isEmpty(lastSuccessfullySyncedConsentStatusString)) {
            mLastSuccessfullySyncedConsentStatus = null;
        } else {
            mLastSuccessfullySyncedConsentStatus =
                    ConsentStatus.fromString(lastSuccessfullySyncedConsentStatusString);
        }
        mIsWhitelisted = sharedPreferences.getBoolean(IS_WHITELISTED_SP_KEY, false);
        mCurrentVendorListVersion = sharedPreferences.getString(CURRENT_VENDOR_LIST_VERSION_SP_KEY,
                null);
        mCurrentVendorListLink = sharedPreferences.getString(CURRENT_VENDOR_LIST_LINK_SP_KEY, null);
        mCurrentPrivacyPolicyVersion = sharedPreferences.getString(
                CURRENT_PRIVACY_POLICY_VERSION_SP_KEY, null);
        mCurrentPrivacyPolicyLink = sharedPreferences.getString(CURRENT_PRIVACY_POLICY_LINK_SP_KEY,
                null);
        mCurrentVendorListIabFormat = sharedPreferences.getString(
                CURRENT_VENDOR_LIST_IAB_FORMAT_SP_KEY, null);
        mCurrentVendorListIabHash = sharedPreferences.getString(CURRENT_VENDOR_LIST_IAB_HASH_SP_KEY,
                null);
        mConsentedVendorListVersion = sharedPreferences.getString(
                CONSENTED_VENDOR_LIST_VERSION_SP_KEY, null);
        mConsentedPrivacyPolicyVersion = sharedPreferences.getString(
                CONSENTED_PRIVACY_POLICY_VERSION_SP_KEY, null);
        mConsentedVendorListIabFormat = sharedPreferences.getString(
                CONSENTED_VENDOR_LIST_IAB_FORMAT_SP_KEY, null);
        mExtras = sharedPreferences.getString(EXTRAS_SP_KEY, null);
        mConsentChangeReason = sharedPreferences.getString(CONSENT_CHANGE_REASON_SP_KEY, null);
        mReacquireConsent = sharedPreferences.getBoolean(REACQUIRE_CONSENT_SP_KEY, false);
        final String gdprAppliesString = sharedPreferences.getString(GDPR_APPLIES_SP_KEY, null);
        if (TextUtils.isEmpty(gdprAppliesString)) {
            mGdprApplies = null;
        } else {
            mGdprApplies = Boolean.parseBoolean(gdprAppliesString);
        }
        mUdid = sharedPreferences.getString(UDID_SP_KEY, null);
        mLastChangedMs = sharedPreferences.getString(LAST_CHANGED_MS_SP_KEY, null);
        final String consentStatusBeforeDnt = sharedPreferences.getString(
                CONSENT_STATUS_BEFORE_DNT_SP_KEY, null);
        if (TextUtils.isEmpty(consentStatusBeforeDnt)) {
            mConsentStatusBeforeDnt = null;
        } else {
            mConsentStatusBeforeDnt = ConsentStatus.fromString(consentStatusBeforeDnt);
        }
    }

    void writeToDisk() {
        final SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(
                mAppContext, PERSONAL_INFO_DATA_SHARED_PREFS).edit();
        editor.putString(AD_UNIT_ID_SP_KEY, mAdUnitId);
        editor.putString(CONSENT_STATUS_SP_KEY, mConsentStatus.name());
        editor.putString(LAST_SUCCESSFULLY_SYNCED_CONSENT_STATUS_SP_KEY,
                mLastSuccessfullySyncedConsentStatus == null ? null : mLastSuccessfullySyncedConsentStatus.name());
        editor.putBoolean(IS_WHITELISTED_SP_KEY, mIsWhitelisted);
        editor.putString(CURRENT_VENDOR_LIST_VERSION_SP_KEY, mCurrentVendorListVersion);
        editor.putString(CURRENT_VENDOR_LIST_LINK_SP_KEY, mCurrentVendorListLink);
        editor.putString(CURRENT_PRIVACY_POLICY_VERSION_SP_KEY, mCurrentPrivacyPolicyVersion);
        editor.putString(CURRENT_PRIVACY_POLICY_LINK_SP_KEY, mCurrentPrivacyPolicyLink);
        editor.putString(CURRENT_VENDOR_LIST_IAB_FORMAT_SP_KEY, mCurrentVendorListIabFormat);
        editor.putString(CURRENT_VENDOR_LIST_IAB_HASH_SP_KEY, mCurrentVendorListIabHash);
        editor.putString(CONSENTED_VENDOR_LIST_VERSION_SP_KEY, mConsentedVendorListVersion);
        editor.putString(CONSENTED_PRIVACY_POLICY_VERSION_SP_KEY, mConsentedPrivacyPolicyVersion);
        editor.putString(CONSENTED_VENDOR_LIST_IAB_FORMAT_SP_KEY, mConsentedVendorListIabFormat);
        editor.putString(EXTRAS_SP_KEY, mExtras);
        editor.putString(CONSENT_CHANGE_REASON_SP_KEY, mConsentChangeReason);
        editor.putBoolean(REACQUIRE_CONSENT_SP_KEY, mReacquireConsent);
        editor.putString(GDPR_APPLIES_SP_KEY,
                mGdprApplies == null ? null : mGdprApplies.toString());
        editor.putString(UDID_SP_KEY, mUdid);
        editor.putString(LAST_CHANGED_MS_SP_KEY, mLastChangedMs);
        editor.putString(CONSENT_STATUS_BEFORE_DNT_SP_KEY,
                mConsentStatusBeforeDnt == null ? null : mConsentStatusBeforeDnt.name());
        editor.apply();
    }

    @NonNull
    String getAdUnitId() {
        return mAdUnitId;
    }

    @NonNull
    ConsentStatus getConsentStatus() {
        return mConsentStatus;
    }

    void setConsentStatus(@NonNull final ConsentStatus consentStatus) {
        mConsentStatus = consentStatus;
    }

    @Nullable
    ConsentStatus getLastSuccessfullySyncedConsentStatus() {
        return mLastSuccessfullySyncedConsentStatus;
    }

    void setLastSuccessfullySyncedConsentStatus(
            @Nullable final ConsentStatus lastSuccessfullySyncedConsentStatus) {
        mLastSuccessfullySyncedConsentStatus = lastSuccessfullySyncedConsentStatus;
    }

    boolean isWhitelisted() {
        return mIsWhitelisted;
    }

    void setWhitelisted(boolean whitelisted) {
        mIsWhitelisted = whitelisted;
    }

    @Nullable
    public String getCurrentVendorListVersion() {
        return mCurrentVendorListVersion;
    }

    void setCurrentVendorListVersion(@Nullable final String currentVendorListVersion) {
        mCurrentVendorListVersion = currentVendorListVersion;
    }

    @NonNull
    public String getCurrentVendorListLink() {
        return getCurrentVendorListLink(null);
    }

    @NonNull
    public String getCurrentVendorListLink(@Nullable final String language) {
        return replaceLanguageMacro(mCurrentVendorListLink, mAppContext,
                language);
    }

    void setCurrentVendorListLink(@Nullable final String currentVendorListLink) {
        mCurrentVendorListLink = currentVendorListLink;
    }

    @Nullable
    public String getCurrentPrivacyPolicyVersion() {
        return mCurrentPrivacyPolicyVersion;
    }

    void setCurrentPrivacyPolicyVersion(@Nullable final String currentPrivacyPolicyVersion) {
        mCurrentPrivacyPolicyVersion = currentPrivacyPolicyVersion;
    }

    @NonNull
    public String getCurrentPrivacyPolicyLink() {
        return getCurrentPrivacyPolicyLink(null);
    }

    @NonNull
    public String getCurrentPrivacyPolicyLink(@Nullable final String language) {
        return replaceLanguageMacro(mCurrentPrivacyPolicyLink, mAppContext,
                language);
    }

    void setCurrentPrivacyPolicyLink(@Nullable final String currentPrivacyPolicyLink) {
        mCurrentPrivacyPolicyLink = currentPrivacyPolicyLink;
    }

    @Nullable
    public String getCurrentVendorListIabFormat() {
        return mCurrentVendorListIabFormat;
    }

    void setCurrentVendorListIabFormat(@Nullable final String currentVendorListIabFormat) {
        mCurrentVendorListIabFormat = currentVendorListIabFormat;
    }

    @Nullable
    String getCurrentVendorListIabHash() {
        return mCurrentVendorListIabHash;
    }

    void setCurrentVendorListIabHash(@Nullable final String currentVendorListIabHash) {
        mCurrentVendorListIabHash = currentVendorListIabHash;
    }

    @Nullable
    public String getConsentedVendorListVersion() {
        return mConsentedVendorListVersion;
    }

    void setConsentedVendorListVersion(@Nullable final String consentedVendorListVersion) {
        mConsentedVendorListVersion = consentedVendorListVersion;
    }

    @Nullable
    public String getConsentedPrivacyPolicyVersion() {
        return mConsentedPrivacyPolicyVersion;
    }

    void setConsentedPrivacyPolicyVersion(
            @Nullable final String consentedPrivacyPolicyVersion) {
        mConsentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
    }

    @Nullable
    public String getConsentedVendorListIabFormat() {
        return mConsentedVendorListIabFormat;
    }

    void setConsentedVendorListIabFormat(
            @Nullable final String consentedVendorListIabFormat) {
        mConsentedVendorListIabFormat = consentedVendorListIabFormat;
    }

    @Nullable
    public String getExtras() {
        return mExtras;
    }

    public void setExtras(@Nullable final String extras) {
        mExtras = extras;
    }

    @Nullable
    String getConsentChangeReason() {
        return mConsentChangeReason;
    }

    void setConsentChangeReason(@Nullable final String consentChangeReason) {
        mConsentChangeReason = consentChangeReason;
    }

    boolean shouldReacquireConsent() {
        return mReacquireConsent;
    }

    void setShouldReacquireConsent(final boolean reacquireConsent) {
        mReacquireConsent = reacquireConsent;
    }

    @Nullable
    Boolean getGdprApplies() {
        return mGdprApplies;
    }

    void setGdprApplies(final boolean gdprApplies) {
        mGdprApplies = gdprApplies;
    }

    @Nullable
    String getUdid() {
        return mUdid;
    }

    void setUdid(@Nullable final String udid) {
        mUdid = udid;
    }

    @Nullable
    String getLastChangedMs() {
        return mLastChangedMs;
    }

    void setLastChangedMs(@Nullable final String lastChangedMs) {
        mLastChangedMs = lastChangedMs;
    }

    @Nullable
    ConsentStatus getConsentStatusBeforeDnt() {
        return mConsentStatusBeforeDnt;
    }

    void setConsentStatusBeforeDnt(@Nullable final ConsentStatus consentStatusBeforeDnt) {
        mConsentStatusBeforeDnt = consentStatusBeforeDnt;
    }

    @VisibleForTesting
    @NonNull
    static String replaceLanguageMacro(@Nullable final String input,
            @NonNull final Context context, @Nullable final String language) {
        Preconditions.checkNotNull(context);

        if (TextUtils.isEmpty(input)) {
            return "";
        }

        return input.replaceAll(LANGUAGE_MACRO_KEY, validateLanguage(context, language));
    }

    /**
     * Returns a valid 2-character ISO 639-1 language. This will return the default language of the
     * device if language is empty or not an ISO 639-1 language.
     *
     * @param context  Context to get Locale.
     * @param language Desired language.
     */
    @NonNull
    private static String validateLanguage(@NonNull final Context context,
            @Nullable final String language) {
        Preconditions.checkNotNull(context);

        for (final String isoLanguage : Locale.getISOLanguages()) {
            if (isoLanguage != null && isoLanguage.equals(language)) {
                return language;
            }
        }
        return ClientMetadata.getCurrentLanguage(context);
    }

}
