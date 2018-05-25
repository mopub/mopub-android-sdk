package com.mopub.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.util.MoPubCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Data object holding any SDK initialization parameters.
 */
public class SdkConfiguration {

    /**
     * Any ad unit that your app uses.
     */
    @NonNull private final String mAdUnitId;

    /**
     * List of the class names of advanced bidders to initialize.
     */
    @NonNull private final List<Class<? extends MoPubAdvancedBidder>> mAdvancedBidders;

    /**
     * Used for rewarded video initialization. This holds each custom event's unique settings.
     */
    @NonNull private final MediationSettings[] mMediationSettings;

    /**
     * List of class names of rewarded video custom events to initialize. These classes must
     * extend CustomEventRewardedVideo.
     */
    @Nullable private final List<String> mNetworksToInit;

    /**
     * Holds data for SDK initialization. Do not call this constructor directly; use the Builder.
     */
    private SdkConfiguration(@NonNull final String adUnitId,
            @NonNull final List<Class<? extends MoPubAdvancedBidder>> advancedBidders,
            @NonNull final MediationSettings[] mediationSettings,
            @Nullable final List<String> networksToInit) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(advancedBidders);

        mAdUnitId = adUnitId;
        mAdvancedBidders = advancedBidders;
        mMediationSettings = mediationSettings;
        mNetworksToInit = networksToInit;
    }

    @NonNull
    public String getAdUnitId() {
        return mAdUnitId;
    }

    @NonNull
    public List<Class<? extends MoPubAdvancedBidder>> getAdvancedBidders() {
        return Collections.unmodifiableList(mAdvancedBidders);
    }

    @NonNull
    public MediationSettings[] getMediationSettings() {
        return Arrays.copyOf(mMediationSettings, mMediationSettings.length);
    }

    @Nullable
    public List<String> getNetworksToInit() {
        if (mNetworksToInit == null) {
            return null;
        }
        return Collections.unmodifiableList(mNetworksToInit);
    }

    public static class Builder {
        @NonNull private String adUnitId;
        @NonNull private final List<Class<? extends MoPubAdvancedBidder>> advancedBidders;
        @NonNull private MediationSettings[] mediationSettings;
        @Nullable private List<String> networksToInit;

        /**
         * Use this builder instead of creating a new SdkConfiguration. This Builder needs any ad
         * unit that is used by this app.
         *
         * @param adUnitId Any ad unit id used by this app. This cannot be null.
         */
        public Builder(@NonNull final String adUnitId) {
            this.adUnitId = adUnitId;
            advancedBidders = new ArrayList<Class<? extends MoPubAdvancedBidder>>();
            mediationSettings = new MediationSettings[0];
        }

        /**
         * Adds a single advanced bidder class to be initialized.
         *
         * @param advancedBidder The advanced bidder class. Cannot be null.
         * @return The builder.
         */
        public Builder withAdvancedBidder(
                @NonNull final Class<? extends MoPubAdvancedBidder> advancedBidder) {
            Preconditions.checkNotNull(advancedBidder);

            this.advancedBidders.add(advancedBidder);
            return this;
        }

        /**
         * Adds a collection of advanced bidder classes to be initialized.
         *
         * @param advancedBidders Collection of advanced bidder classes. Cannot be null.
         * @return The builder.
         */
        public Builder withAdvancedBidders(
                @NonNull final Collection<Class<? extends MoPubAdvancedBidder>> advancedBidders) {
            Preconditions.NoThrow.checkNotNull(advancedBidders);

            MoPubCollections.addAllNonNull(this.advancedBidders, advancedBidders);
            return this;
        }

        /**
         * Adds mediation settings for rewarded video custom events.
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
         * Adds a list of rewarded video custom events to initialize.
         *
         * @param networksToInit List of full class names as Strings to initialize for rewarded video.
         * @return The builder.
         */
        public Builder withNetworksToInit(@Nullable final List<String> networksToInit) {
            if (networksToInit == null) {
                return this;
            }

            this.networksToInit = new ArrayList<>();
            MoPubCollections.addAllNonNull(this.networksToInit, networksToInit);
            return this;
        }

        public SdkConfiguration build() {
            return new SdkConfiguration(adUnitId, advancedBidders, mediationSettings,
                    networksToInit);
        }
    }
}
