// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.base.BuildConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.LogLevel;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.INIT_FAILED;

/**
 * Data object holding any SDK initialization parameters.
 */
public class SdkConfiguration {

    /**
     * Any ad unit that your app uses.
     */
    @NonNull private final String mAdUnitId;

    /**
     * List of the class names of adapter configurations to initialize.
     */
    @NonNull private final Set<String> mAdapterConfigurationClasses;

    /**
     * Used for rewarded video initialization. This holds each base ad's unique settings.
     */
    @NonNull private final MediationSettings[] mMediationSettings;

    /**
     * Adapter configuration options used in initialization of networks. This is keyed on the
     * {@link AdapterConfiguration} class and the values are maps of initialization parameters.
     */
    @NonNull private final Map<String, Map<String, String>> mMediatedNetworkConfigurations;

    /**
     * Adapter configuration options passed to the adserver. This is keyed on the
     * {@link AdapterConfiguration} class and the values are maps of request options.
     */
    @NonNull private final Map<String, Map<String, String>> mMoPubRequestOptions;

    /**
     * The log level that will be used to determine which events are printed or thrown out.
     */
    @NonNull private final LogLevel mLogLevel;

    /**
     * Whether or not legitimate interest is allowed for the collection of personally identifiable information.
     */
    private final boolean mLegitimateInterestAllowed;

    /**
     * Holds data for SDK initialization. Do not call this constructor directly; use the Builder.
     */
    private SdkConfiguration(@NonNull final String adUnitId,
            @NonNull final Set<String> adapterConfigurationClasses,
            @NonNull final MediationSettings[] mediationSettings,
            @NonNull final LogLevel logLevel,
            @NonNull final Map<String, Map<String, String>> mediatedNetworkConfigurations,
            @NonNull final Map<String, Map<String, String>> moPubRequestOptions,
            final boolean legitimateInterestAllowed) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(adapterConfigurationClasses);
        Preconditions.checkNotNull(mediatedNetworkConfigurations);
        Preconditions.checkNotNull(moPubRequestOptions);

        mAdUnitId = adUnitId;
        mAdapterConfigurationClasses = adapterConfigurationClasses;
        mMediationSettings = mediationSettings;
        mLogLevel = logLevel;
        mMediatedNetworkConfigurations = mediatedNetworkConfigurations;
        mMoPubRequestOptions = moPubRequestOptions;
        mLegitimateInterestAllowed = legitimateInterestAllowed;
    }

    @NonNull
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @NonNull
    public Set<String> getAdapterConfigurationClasses() {
        return Collections.unmodifiableSet(mAdapterConfigurationClasses);
    }

    @NonNull
    public MediationSettings[] getMediationSettings() {
        return Arrays.copyOf(mMediationSettings, mMediationSettings.length);
    }

    @NonNull
    LogLevel getLogLevel() {
        return mLogLevel;
    }

    public Map<String, Map<String, String>> getMediatedNetworkConfigurations() {
        return Collections.unmodifiableMap(mMediatedNetworkConfigurations);
    }

    @NonNull
    public Map<String, Map<String, String>> getMoPubRequestOptions() {
        return Collections.unmodifiableMap(mMoPubRequestOptions);
    }

    public boolean getLegitimateInterestAllowed() {
        return mLegitimateInterestAllowed;
    }

    public static class Builder {
        @NonNull private String adUnitId;
        @NonNull private final Set<String> adapterConfigurations;
        @NonNull private MediationSettings[] mediationSettings;
        @NonNull private LogLevel logLevel = LogLevel.NONE;
        @NonNull private final Map<String, Map<String, String>> mediatedNetworkConfigurations;
        @NonNull private final Map<String, Map<String, String>> moPubRequestOptions;
        private boolean legitimateInterestAllowed;

        /**
         * Use this builder instead of creating a new SdkConfiguration. This Builder needs any ad
         * unit that is used by this app.
         *
         * @param adUnitId Any ad unit id used by this app. This cannot be empty.
         */
        public Builder(@NonNull final String adUnitId) {
            if (TextUtils.isEmpty(adUnitId)) {
                final IllegalArgumentException iae = new
                        IllegalArgumentException("Ad unit cannot be empty at initialization");
                MoPubLog.setLogLevel(MoPubLog.getLogLevel());
                MoPubLog.log(INIT_FAILED, "Pass in an ad unit used by this app", iae);
                if (BuildConfig.DEBUG) {
                    throw iae;
                }
            }

            this.adUnitId = adUnitId;
            adapterConfigurations = DefaultAdapterClasses.getClassNamesSet();
            mediationSettings = new MediationSettings[0];
            mediatedNetworkConfigurations = new HashMap<>();
            moPubRequestOptions = new HashMap<>();
            legitimateInterestAllowed = false;
        }

        /**
         * Specifies an additional custom adapter configuration to attempt to initialize. MoPub
         * automatically adds MoPub-supported networks' adapter configurations.
         *
         * @param adapterConfigurationClass {@link Class#getName()} of an adapter configuration
         *                                  class. This should not be the simple name or the
         *                                  canonical name.
         * @return The builder.
         */
        public Builder withAdditionalNetwork(@NonNull final String adapterConfigurationClass) {
            Preconditions.checkNotNull(adapterConfigurationClass);

            adapterConfigurations.add(adapterConfigurationClass);
            return this;
        }

        /**
         * Adds mediation settings for rewarded video base ads.
         *
         * @param mediationSettings Array of mediation settings. Can be empty but not null.
         * @return The builder.
         */
        public Builder withMediationSettings(@NonNull MediationSettings... mediationSettings) {
            Preconditions.checkNotNull(mediationSettings);

            this.mediationSettings = mediationSettings;
            return this;
        }

        /**
         * Adds a log level to be used by MoPubLog.
         *
         * @param logLevel A MoPubLog.LogLevel. Cannot be null.
         * @return The builder.
         */
        public Builder withLogLevel(@NonNull LogLevel logLevel) {
            Preconditions.checkNotNull(logLevel);

            this.logLevel = logLevel;
            return this;
        }

        /**
         * Adds a single mediated network configuration keyed by the AdapterConfiguration class.
         * This is used by ad networks' initialization.
         *
         * @param adapterConfigurationClass    The class name to key on.
         * @param mediatedNetworkConfiguration A Map of network configurations.
         * @return The builder.
         */
        public Builder withMediatedNetworkConfiguration(
                @NonNull final String adapterConfigurationClass,
                @NonNull final Map<String, String> mediatedNetworkConfiguration) {
            Preconditions.checkNotNull(adapterConfigurationClass);
            Preconditions.checkNotNull(mediatedNetworkConfiguration);

            mediatedNetworkConfigurations.put(adapterConfigurationClass,
                    mediatedNetworkConfiguration);
            return this;
        }

        /**
         * Adds a single MopubRequestOption keyed by the AdapterConfiguration class.
         *
         * @param adapterConfigurationClass The class name to key on.
         * @param mopubRequestOptions       A Map of options.
         * @return The builder.
         */
        public Builder withMoPubRequestOptions(
                @NonNull final String adapterConfigurationClass,
                @NonNull final Map<String, String> mopubRequestOptions) {
            Preconditions.checkNotNull(adapterConfigurationClass);
            Preconditions.checkNotNull(mopubRequestOptions);

            this.moPubRequestOptions.put(adapterConfigurationClass, mopubRequestOptions);
            return this;
        }

        /**
         * Sets whether or not legitimate interest is allowed for the collection of personally identifiable information.
         * This API can be used if you want to allow supported SDK networks to collect user information on the basis of legitimate interest.
         *
         * @param legitimateInterestAllowed should be true if legitimate interest is allowed. False if it isn't allowed.
         * @return The builder.
         */
        public Builder withLegitimateInterestAllowed(final boolean legitimateInterestAllowed) {
            this.legitimateInterestAllowed = legitimateInterestAllowed;
            return this;
        }

        public SdkConfiguration build() {
            return new SdkConfiguration(adUnitId, adapterConfigurations, mediationSettings,
                    logLevel, mediatedNetworkConfigurations, moPubRequestOptions, legitimateInterestAllowed);
        }
    }
}
