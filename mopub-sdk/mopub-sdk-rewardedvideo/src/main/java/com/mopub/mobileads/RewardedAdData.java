package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

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

/**
 * Used to manage the mapping between MoPub ad unit ids and third-party ad network ids for rewarded ads.
 */
class RewardedAdData {
    @NonNull
    private final Map<String, CustomEventRewardedAd> mAdUnitToCustomEventMap;
    @NonNull
    private final Map<String, MoPubReward> mAdUnitToRewardMap;
    @NonNull
    private final Map<String, Set<MoPubReward>> mAdUnitToAvailableRewardsMap;
    @NonNull
    private final Map<String, String> mAdUnitToServerCompletionUrlMap;
    @NonNull
    private final Map<String, String> mAdUnitToCustomDataMap;
    @NonNull
    private final Map<Class<? extends CustomEventRewardedAd>, MoPubReward> mCustomEventToRewardMap;
    @NonNull
    private final Map<TwoPartKey, Set<String>> mCustomEventToMoPubIdMap;
    @Nullable
    private String mCurrentlyShowingAdUnitId;
    @Nullable
    private String mCustomerId;


    RewardedAdData() {
        mAdUnitToCustomEventMap = new TreeMap<String, CustomEventRewardedAd>();
        mAdUnitToRewardMap = new TreeMap<String, MoPubReward>();
        mAdUnitToAvailableRewardsMap = new TreeMap<String, Set<MoPubReward>>();
        mAdUnitToServerCompletionUrlMap = new TreeMap<String, String>();
        mAdUnitToCustomDataMap = new TreeMap<String, String>();
        mCustomEventToRewardMap = new HashMap<Class<? extends CustomEventRewardedAd>, MoPubReward>();
        mCustomEventToMoPubIdMap = new HashMap<TwoPartKey, Set<String>>();
    }

    @Nullable
    CustomEventRewardedAd getCustomEvent(@Nullable String moPubId) {
        return mAdUnitToCustomEventMap.get(moPubId);
    }

    @Nullable
    MoPubReward getMoPubReward(@Nullable String moPubId) {
        return mAdUnitToRewardMap.get(moPubId);
    }

    @Nullable
    String getCustomData(@Nullable String moPubId) {
        return mAdUnitToCustomDataMap.get(moPubId);
    }

    void addAvailableReward(
            @NonNull String moPubId,
            @Nullable String currencyName,
            @Nullable String currencyAmount) {
        Preconditions.checkNotNull(moPubId);
        if (currencyName == null || currencyAmount == null) {
            MoPubLog.e(String.format(Locale.US, "Currency name and amount cannot be null: " +
                    "name = %s, amount = %s", currencyName, currencyAmount));
            return;
        }

        int intCurrencyAmount;
        try {
            intCurrencyAmount = Integer.parseInt(currencyAmount);
        } catch(NumberFormatException e) {
            MoPubLog.e(String.format(Locale.US, "Currency amount must be an integer: %s",
                    currencyAmount));
            return;
        }

        if (intCurrencyAmount < 0) {
            MoPubLog.e(String.format(Locale.US, "Currency amount cannot be negative: %s",
                    currencyAmount));
            return;
        }

        if (mAdUnitToAvailableRewardsMap.containsKey(moPubId)) {
            mAdUnitToAvailableRewardsMap.get(moPubId)
                    .add(MoPubReward.success(currencyName, intCurrencyAmount));
        } else {
            HashSet<MoPubReward> availableRewards = new HashSet<>();
            availableRewards.add(MoPubReward.success(currencyName, intCurrencyAmount));
            mAdUnitToAvailableRewardsMap.put(moPubId, availableRewards);
        }
    }

    @NonNull
    Set<MoPubReward> getAvailableRewards(@NonNull String moPubId) {
        Preconditions.checkNotNull(moPubId);
        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(moPubId);
        return (availableRewards == null) ? Collections.<MoPubReward>emptySet() : availableRewards;
    }

    void selectReward(@NonNull String moPubId, @NonNull MoPubReward selectedReward) {
        Preconditions.checkNotNull(moPubId);
        Preconditions.checkNotNull(selectedReward);

        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(moPubId);
        if (availableRewards == null || availableRewards.isEmpty()) {
            MoPubLog.e(String.format(
                    Locale.US, "AdUnit %s does not have any rewards.", moPubId));
            return;
        }

        if (!availableRewards.contains(selectedReward)) {
            MoPubLog.e(String.format(
                    Locale.US, "Selected reward is invalid for AdUnit %s.", moPubId));
            return;
        }

        updateAdUnitRewardMapping(moPubId, selectedReward.getLabel(),
                Integer.toString(selectedReward.getAmount()));
    }

    void resetAvailableRewards(@NonNull String moPubId) {
        Preconditions.checkNotNull(moPubId);
        Set<MoPubReward> availableRewards = mAdUnitToAvailableRewardsMap.get(moPubId);
        if (availableRewards != null && !availableRewards.isEmpty()) {
            availableRewards.clear();
        }
    }

    void resetSelectedReward(@NonNull String moPubId) {
        Preconditions.checkNotNull(moPubId);

        // Clear any reward previously selected for this AdUnit
        updateAdUnitRewardMapping(moPubId, null, null);
    }

    @Nullable
    String getServerCompletionUrl(@Nullable final String moPubId) {
        if (TextUtils.isEmpty(moPubId)) {
            return null;
        }
        return mAdUnitToServerCompletionUrlMap.get(moPubId);
    }

    @Nullable
    MoPubReward getLastShownMoPubReward(@NonNull Class<? extends CustomEventRewardedAd> customEventClass) {
        return mCustomEventToRewardMap.get(customEventClass);
    }

    @NonNull
    Set<String> getMoPubIdsForAdNetwork(
            @NonNull Class<? extends CustomEventRewardedAd> customEventClass,
            @Nullable String adNetworkId) {
        if (adNetworkId == null) {
            final Set<String> allIds = new HashSet<String>();
            for (final Map.Entry<TwoPartKey, Set<String>> entry : mCustomEventToMoPubIdMap.entrySet()) {
                final Class<?> clazz = entry.getKey().customEventClass;
                if (customEventClass == clazz) {
                    allIds.addAll(entry.getValue());
                }
            }
            return allIds;
        } else {
            final TwoPartKey key = new TwoPartKey(customEventClass, adNetworkId);
            return mCustomEventToMoPubIdMap.containsKey(key)
                    ? mCustomEventToMoPubIdMap.get(key)
                    : Collections.<String>emptySet();
        }
    }

    void updateAdUnitCustomEventMapping(
            @NonNull String moPubId,
            @NonNull CustomEventRewardedAd customEvent,
            @NonNull String adNetworkId) {
        mAdUnitToCustomEventMap.put(moPubId, customEvent);
        associateCustomEventWithMoPubId(customEvent.getClass(), adNetworkId, moPubId);
    }

    void updateAdUnitRewardMapping(
            @NonNull String moPubId,
            @Nullable String currencyName,
            @Nullable String currencyAmount) {
        Preconditions.checkNotNull(moPubId);
        if (currencyName == null || currencyAmount == null) {
            // If we get here it means that the reward was not set on the frontend ad unit
            mAdUnitToRewardMap.remove(moPubId);
            return;
        }

        int intCurrencyAmount;
        try {
            intCurrencyAmount = Integer.parseInt(currencyAmount);
        } catch(NumberFormatException e) {
            MoPubLog.e(String.format(Locale.US, "Currency amount must be an integer: %s",
                    currencyAmount));
            return;
        }

        if (intCurrencyAmount < 0) {
            MoPubLog.e(String.format(Locale.US, "Currency amount cannot be negative: %s",
                    currencyAmount));
            return;
        }

        mAdUnitToRewardMap.put(moPubId, MoPubReward.success(currencyName, intCurrencyAmount));
    }

    void updateAdUnitToServerCompletionUrlMapping(@NonNull final String moPubId,
            @Nullable final String serverCompletionUrl) {
        Preconditions.checkNotNull(moPubId);
        mAdUnitToServerCompletionUrlMap.put(moPubId, serverCompletionUrl);
    }

    /**
     * This method should be called right before the rewarded ad is shown in order to store the
     * reward associated with the custom event class. If called earlier in the rewarded lifecycle,
     * it's possible that this mapping will be overridden by another reward value before the ad
     * is shown.
     *
     * @param customEventClass the rewarded ad custom event class
     * @param moPubReward the reward from the MoPub ad server returned in HTTP headers
     */
    void updateCustomEventLastShownRewardMapping(
            @NonNull final Class<? extends CustomEventRewardedAd> customEventClass,
            @Nullable final MoPubReward moPubReward) {
        Preconditions.checkNotNull(customEventClass);
        mCustomEventToRewardMap.put(customEventClass, moPubReward);
    }

    void associateCustomEventWithMoPubId(
            @NonNull Class<? extends CustomEventRewardedAd> customEventClass,
            @NonNull String adNetworkId,
            @NonNull String moPubId) {
        final TwoPartKey newCustomEventMapping = new TwoPartKey(customEventClass, adNetworkId);

        // Remove previous mapping for this moPubId
        final Iterator<Map.Entry<TwoPartKey, Set<String>>> entryIterator =
                mCustomEventToMoPubIdMap.entrySet().iterator();
        while (entryIterator.hasNext()) {
            final Map.Entry<TwoPartKey, Set<String>> entry = entryIterator.next();

            if (!entry.getKey().equals(newCustomEventMapping)) {
                if (entry.getValue().contains(moPubId)) {
                    entry.getValue().remove(moPubId);
                    // Ensure that entries containing empty Sets are completely removed from the Map
                    if (entry.getValue().isEmpty()) {
                        entryIterator.remove();
                    }

                    // moPubIds can exist at most once in the Map values, so break upon finding a match
                    break;
                }
            }
        }

        // Add a new mapping if necessary.
        Set<String> moPubIds = mCustomEventToMoPubIdMap.get(newCustomEventMapping);
        if (moPubIds == null) {
            moPubIds = new HashSet<String>();
            mCustomEventToMoPubIdMap.put(newCustomEventMapping, moPubIds);
        }
        moPubIds.add(moPubId);
    }

    void setCurrentlyShowingAdUnitId(@Nullable final String currentAdUnitId) {
        mCurrentlyShowingAdUnitId = currentAdUnitId;
    }

    void updateAdUnitToCustomDataMapping(@NonNull final String moPubId,
            @Nullable String customData) {
        Preconditions.NoThrow.checkNotNull(moPubId);

        mAdUnitToCustomDataMap.put(moPubId, customData);
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
        mAdUnitToCustomEventMap.clear();
        mAdUnitToRewardMap.clear();
        mAdUnitToAvailableRewardsMap.clear();
        mAdUnitToServerCompletionUrlMap.clear();
        mAdUnitToCustomDataMap.clear();
        mCustomEventToRewardMap.clear();
        mCustomEventToMoPubIdMap.clear();
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
    boolean existsInAvailableRewards(@NonNull String moPubId, @NonNull String currencyName,
            int currencyAmount) {
        Preconditions.checkNotNull(moPubId);
        Preconditions.checkNotNull(currencyName);

        for (MoPubReward reward : getAvailableRewards(moPubId)) {
            if (reward.getLabel().equals(currencyName) && reward.getAmount() == currencyAmount) {
                return true;
            }
        }

        return false;
    }

    private static class TwoPartKey extends Pair<Class<? extends CustomEventRewardedAd>, String> {
        @NonNull
        final Class<? extends CustomEventRewardedAd> customEventClass;
        @NonNull
        final String adNetworkId;

        public TwoPartKey(
                @NonNull final Class<? extends CustomEventRewardedAd> customEventClass,
                @NonNull final String adNetworkId) {
            super(customEventClass, adNetworkId);

            this.customEventClass = customEventClass;
            this.adNetworkId = adNetworkId;
        }
    }
}
