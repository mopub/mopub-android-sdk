// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * Used to manage the mapping between MoPub ad unit ids and third-party ad network ids for rewarded ads.
 */
class RewardedAdData {
    @NonNull
    private final Map<String, AdAdapter> mAdUnitToAdAdapterMap;
    @NonNull
    private final Map<String, MoPubReward> mAdUnitToRewardMap;
    @NonNull
    private final Map<String, Set<MoPubReward>> mAdUnitToAvailableRewardsMap;
    @NonNull
    private final Map<String, String> mAdUnitToServerCompletionUrlMap;
    @NonNull
    private final Map<String, String> mAdUnitToCustomDataMap;
    @NonNull
    private final Map<AdAdapter, MoPubReward> mAdAdapterToRewardMap;
    @NonNull
    private final Map<AdAdapter, Set<String>> mAdAdapterToAdUnitIdMap;
    @Nullable
    private String mCurrentlyShowingAdUnitId;
    @Nullable
    private String mCustomerId;


    RewardedAdData() {
        mAdUnitToAdAdapterMap = new TreeMap<String, AdAdapter>();
        mAdUnitToRewardMap = new TreeMap<String, MoPubReward>();
        mAdUnitToAvailableRewardsMap = new TreeMap<String, Set<MoPubReward>>();
        mAdUnitToServerCompletionUrlMap = new TreeMap<String, String>();
        mAdUnitToCustomDataMap = new TreeMap<String, String>();
        mAdAdapterToRewardMap = new HashMap<AdAdapter, MoPubReward>();
        mAdAdapterToAdUnitIdMap = new HashMap<AdAdapter, Set<String>>();
    }

    @Nullable
    AdAdapter getAdAdapter(@Nullable String adUnitId) {
        return mAdUnitToAdAdapterMap.get(adUnitId);
    }

    @Nullable
    MoPubReward getMoPubReward(@Nullable String adUnitId) {
        return mAdUnitToRewardMap.get(adUnitId);
    }

    @Nullable
    String getCustomData(@Nullable String adUnitId) {
        return mAdUnitToCustomDataMap.get(adUnitId);
    }

    void addAvailableReward(
            @NonNull String adUnitId,
            @Nullable String currencyName,
            @Nullable String currencyAmount) {
        Preconditions.checkNotNull(adUnitId);
        if (currencyName == null || currencyAmount == null) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Currency name and amount cannot be null: " +
                    "name = %s, amount = %s", currencyName, currencyAmount));
            return;
        }

        int intCurrencyAmount;
        try {
            intCurrencyAmount = Integer.parseInt(currencyAmount);
        } catch(NumberFormatException e) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Currency amount must be an integer: %s",
                    currencyAmount));
            return;
        }

        if (intCurrencyAmount < 0) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Currency amount cannot be negative: %s",
                    currencyAmount));
            return;
        }

        if (mAdUnitToAvailableRewardsMap.containsKey(adUnitId)) {
            mAdUnitToAvailableRewardsMap.get(adUnitId)
                    .add(MoPubReward.success(currencyName, intCurrencyAmount));
        } else {
            HashSet<MoPubReward> availableRewards = new HashSet<>();
            availableRewards.add(MoPubReward.success(currencyName, intCurrencyAmount));
            mAdUnitToAvailableRewardsMap.put(adUnitId, availableRewards);
        }
    }

    @NonNull
    Set<MoPubReward> getAvailableRewards(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);
        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(adUnitId);
        return (availableRewards == null) ? Collections.<MoPubReward>emptySet() : availableRewards;
    }

    void selectReward(@NonNull String adUnitId, @NonNull MoPubReward selectedReward) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(selectedReward);

        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(adUnitId);
        if (availableRewards == null || availableRewards.isEmpty()) {
            MoPubLog.log(CUSTOM, String.format(
                    Locale.US, "AdUnit %s does not have any rewards.", adUnitId));
            return;
        }

        if (!availableRewards.contains(selectedReward)) {
            MoPubLog.log(CUSTOM, String.format(
                    Locale.US, "Selected reward is invalid for AdUnit %s.", adUnitId));
            return;
        }

        updateAdUnitRewardMapping(adUnitId, selectedReward.getLabel(),
                Integer.toString(selectedReward.getAmount()));
    }

    void resetAvailableRewards(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);
        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(adUnitId);
        if (availableRewards != null && !availableRewards.isEmpty()) {
            availableRewards.clear();
        }
    }

    void resetSelectedReward(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        // Clear any reward previously selected for this AdUnit
        updateAdUnitRewardMapping(adUnitId, null, null);
    }

    @Nullable
    String getServerCompletionUrl(@Nullable final String adUnitId) {
        if (TextUtils.isEmpty(adUnitId)) {
            return null;
        }
        return mAdUnitToServerCompletionUrlMap.get(adUnitId);
    }

    @Nullable
    MoPubReward getLastShownMoPubReward(@NonNull AdAdapter adAdapter) {
        return mAdAdapterToRewardMap.get(adAdapter);
    }

    @NonNull
    Set<String> getAdUnitIdsForAdAdapter(@NonNull AdAdapter adAdapter) {
        Preconditions.checkNotNull(adAdapter);

        final Set<String> allIds = new HashSet<String>();
        for (final Map.Entry<AdAdapter, Set<String>> entry : mAdAdapterToAdUnitIdMap.entrySet()) {
            final AdAdapter currentAdAdapter = entry.getKey();
            if (adAdapter == currentAdAdapter) {
                allIds.addAll(entry.getValue());
            }
        }
        return allIds;
    }

    void updateAdUnitAdAdapterMapping(
            @NonNull String adUnitId,
            @NonNull AdAdapter adAdapter) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(adAdapter);

        mAdUnitToAdAdapterMap.put(adUnitId, adAdapter);
        associateAdAdapterWithAdUnitId(adAdapter, adUnitId);
    }

    void updateAdUnitRewardMapping(
            @NonNull String adUnitId,
            @Nullable String currencyName,
            @Nullable String currencyAmount) {
        Preconditions.checkNotNull(adUnitId);
        if (currencyName == null || currencyAmount == null) {
            // If we get here it means that the reward was not set on the frontend ad unit
            mAdUnitToRewardMap.remove(adUnitId);
            return;
        }

        int intCurrencyAmount;
        try {
            intCurrencyAmount = Integer.parseInt(currencyAmount);
        } catch(NumberFormatException e) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Currency amount must be an integer: %s",
                    currencyAmount));
            return;
        }

        if (intCurrencyAmount < 0) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Currency amount cannot be negative: %s",
                    currencyAmount));
            return;
        }

        mAdUnitToRewardMap.put(adUnitId, MoPubReward.success(currencyName, intCurrencyAmount));
    }

    void updateAdUnitToServerCompletionUrlMapping(@NonNull final String adUnitId,
            @Nullable final String serverCompletionUrl) {
        Preconditions.checkNotNull(adUnitId);
        mAdUnitToServerCompletionUrlMap.put(adUnitId, serverCompletionUrl);
    }

    /**
     * This method should be called right before the rewarded ad is shown in order to store the
     * reward associated with the base ad class. If called earlier in the rewarded lifecycle,
     * it's possible that this mapping will be overridden by another reward value before the ad
     * is shown.
     *
     * @param adAdapter TODO
     * @param moPubReward the reward from the MoPub ad server returned in HTTP headers
     */
    void updateLastShownRewardMapping(
            @NonNull final AdAdapter adAdapter,
            @Nullable final MoPubReward moPubReward) {
        Preconditions.checkNotNull(adAdapter);
        mAdAdapterToRewardMap.put(adAdapter, moPubReward);
    }

    void associateAdAdapterWithAdUnitId(
            @NonNull AdAdapter adAdapter,
            @NonNull String adUnitId) {
        // Remove previous mapping for this adUnitId
        final Iterator<Map.Entry<AdAdapter, Set<String>>> entryIterator =
                mAdAdapterToAdUnitIdMap.entrySet().iterator();
        while (entryIterator.hasNext()) {
            final Map.Entry<AdAdapter, Set<String>> entry = entryIterator.next();

            if (!entry.getKey().equals(adAdapter)) {
                if (entry.getValue().contains(adUnitId)) {
                    entry.getValue().remove(adUnitId);
                    // Ensure that entries containing empty Sets are completely removed from the Map
                    if (entry.getValue().isEmpty()) {
                        entryIterator.remove();
                    }

                    // adUnitIds can exist at most once in the Map values, so break upon finding a match
                    break;
                }
            }
        }

        // Add a new mapping if necessary.
        Set<String> adUnitIds = mAdAdapterToAdUnitIdMap.get(adAdapter);
        if (adUnitIds == null) {
            adUnitIds = new HashSet<String>();
            mAdAdapterToAdUnitIdMap.put(adAdapter, adUnitIds);
        }
        adUnitIds.add(adUnitId);
    }

    void setCurrentlyShowingAdUnitId(@Nullable final String currentAdUnitId) {
        mCurrentlyShowingAdUnitId = currentAdUnitId;
    }

    void updateAdUnitToCustomDataMapping(@NonNull final String adUnitId,
            @Nullable String customData) {
        Preconditions.NoThrow.checkNotNull(adUnitId);

        mAdUnitToCustomDataMap.put(adUnitId, customData);
    }

    @Nullable
    String getCurrentlyShowingAdUnitId() {
        return mCurrentlyShowingAdUnitId;
    }

    void setCustomerId(@Nullable final String customerId) {
        mCustomerId = customerId;
    }

    @Nullable
    String getCustomerId() {
        return mCustomerId;
    }

    @VisibleForTesting
    @Deprecated
    void clear() {
        mAdUnitToAdAdapterMap.clear();
        mAdUnitToRewardMap.clear();
        mAdUnitToAvailableRewardsMap.clear();
        mAdUnitToServerCompletionUrlMap.clear();
        mAdUnitToCustomDataMap.clear();
        mAdAdapterToRewardMap.clear();
        mAdAdapterToAdUnitIdMap.clear();
        mCurrentlyShowingAdUnitId = null;
        mCustomerId = null;
    }

    @VisibleForTesting
    @Deprecated
    /**
     * This method is purely used as a helper method in unit tests. Note that calling
     * {@link MoPubReward#success(String, int)} creates a new instance, even with the same reward
     * label and amount as an existing reward. Therefore, existence of a reward cannot be asserted
     * simply by comparing objects in the unit tests.
     */
    boolean existsInAvailableRewards(@NonNull String adUnitId, @NonNull String currencyName,
            int currencyAmount) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(currencyName);

        for (MoPubReward reward : getAvailableRewards(adUnitId)) {
            if (reward.getLabel().equals(currencyName) && reward.getAmount() == currencyAmount) {
                return true;
            }
        }

        return false;
    }

    private static class TwoPartKey extends Pair<AdAdapter, String> {
        @NonNull
        final AdAdapter adAdapter;
        @NonNull
        final String adNetworkId;

        public TwoPartKey(
                @NonNull final AdAdapter adAdapter,
                @NonNull final String adNetworkId) {
            super(adAdapter, adNetworkId);

            this.adAdapter = adAdapter;
            this.adNetworkId = adNetworkId;
        }
    }
}
