package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Used to manage the mapping between MoPub ad unit ids and third-party ad network ids for rewarded videos.
 */
class RewardedVideoData {
    @NonNull
    private final Map<String, CustomEventRewardedVideo> mAdUnitToCustomEventMap;
    @NonNull
    private final Map<String, MoPubReward> mAdUnitToRewardMap;
    @NonNull
    private final Map<Class<? extends CustomEventRewardedVideo>, MoPubReward> mCustomEventToRewardMap;
    @NonNull
    private final Map<TwoPartKey, Set<String>> mCustomEventToMoPubIdMap;
    @NonNull
    private final Set<CustomEventRewardedVideo.CustomEventRewardedVideoListener> mAdNetworkListeners;
    @Nullable
    private String mCurrentAdUnitId;

    RewardedVideoData() {
        mAdUnitToCustomEventMap = new TreeMap<String, CustomEventRewardedVideo>();
        mAdUnitToRewardMap = new TreeMap<String, MoPubReward>();
        mCustomEventToRewardMap = new HashMap<Class<? extends CustomEventRewardedVideo>, MoPubReward>();
        mCustomEventToMoPubIdMap = new HashMap<TwoPartKey, Set<String>>();
        mAdNetworkListeners = new HashSet<CustomEventRewardedVideo.CustomEventRewardedVideoListener>();
    }

    @Nullable
    CustomEventRewardedVideo getCustomEvent(@NonNull String moPubId) {
        return mAdUnitToCustomEventMap.get(moPubId);
    }

    @Nullable
    MoPubReward getMoPubReward(@Nullable String moPubId) {
        return mAdUnitToRewardMap.get(moPubId);
    }

    @Nullable
    MoPubReward getLastShownMoPubReward(@NonNull Class<? extends CustomEventRewardedVideo> customEventClass) {
        return mCustomEventToRewardMap.get(customEventClass);
    }

    @NonNull
    Set<String> getMoPubIdsForAdNetwork(
            @NonNull Class<? extends CustomEventRewardedVideo> customEventClass,
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
            @NonNull CustomEventRewardedVideo customEvent,
            @Nullable CustomEventRewardedVideo.CustomEventRewardedVideoListener listener,
            @NonNull String adNetworkId) {
        mAdUnitToCustomEventMap.put(moPubId, customEvent);
        mAdNetworkListeners.add(listener);
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
            return;
        }

        if (intCurrencyAmount < 0) {
            return;
        }

        mAdUnitToRewardMap.put(moPubId, MoPubReward.success(currencyName, intCurrencyAmount));
    }

    /**
     * This method should be called right before the rewarded video is shown in order to store the
     * reward associated with the custom event class. If called earlier in the rewarded lifecycle,
     * it's possible that this mapping will be overridden by another reward value before the video
     * is shown.
     *
     * @param customEventClass the rewarded video custom event class
     * @param moPubReward the reward from teh MoPub ad server returned in HTTP headers
     */
    void updateCustomEventLastShownRewardMapping(
            @NonNull final Class<? extends CustomEventRewardedVideo> customEventClass,
            @Nullable final MoPubReward moPubReward) {
        Preconditions.checkNotNull(customEventClass);
        mCustomEventToRewardMap.put(customEventClass, moPubReward);
    }

    void associateCustomEventWithMoPubId(
            @NonNull Class<? extends CustomEventRewardedVideo> customEventClass,
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

    void setCurrentAdUnitId(@Nullable final String currentAdUnitId) {
        mCurrentAdUnitId = currentAdUnitId;
    }

    @Nullable
    String getCurrentAdUnitId() {
        return mCurrentAdUnitId;
    }

    private static class TwoPartKey extends Pair<Class<? extends CustomEventRewardedVideo>, String> {
        @NonNull
        final Class<? extends CustomEventRewardedVideo> customEventClass;
        @NonNull
        final String adNetworkId;

        public TwoPartKey(
                @NonNull final Class<? extends CustomEventRewardedVideo> customEventClass,
                @NonNull final String adNetworkId) {
            super(customEventClass, adNetworkId);

            this.customEventClass = customEventClass;
            this.adNetworkId = adNetworkId;
        }
    }
}
