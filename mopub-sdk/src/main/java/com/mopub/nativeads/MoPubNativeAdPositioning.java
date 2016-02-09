package com.mopub.nativeads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides factory methods for setting up native ad positions.
 *
 * This class supports 2 types of positioning to use when placing ads into your stream:
 * <ol>
 *     <li><b>Server positioning</b>. The SDK will connect to the MoPub server to determine the
 *     optimal positions for ads. You can also configure these positions yourself using the
 *     MoPub publisher UI. We recommend using server positioning, and it is the default if you
 *     do not specify positioning when loading ads.</li>
 *     <li><b>Client positioning.</b> Requires you to hard-code positions into your app. You
 *     can specify fixed positions for ads, as well as a repeating interval.
 *     </li>
 * </ol>
 */
public final class MoPubNativeAdPositioning {
    /**
     * Allows the SDK to connect to the MoPub server in order to determine ad
     * positions.
     */
    public static class MoPubServerPositioning {
        // No-op. This is used by the ad placer as an indicator to use server positioning.
    }

    /**
     * Allows hard-coding ad positions into your app.
     */
    public static class MoPubClientPositioning {
        /**
         * Constant indicating that ad positions should not repeat.
         */
        public static final int NO_REPEAT = Integer.MAX_VALUE;

        @NonNull private final ArrayList<Integer> mFixedPositions = new ArrayList<Integer>();
        private int mRepeatInterval = NO_REPEAT;

        public MoPubClientPositioning() {
        }

        /**
         * Specifies a fixed ad position.
         *
         * @param position The ad position.
         * @return This object for easy use in chained setters.
         */
        @NonNull
        public MoPubClientPositioning addFixedPosition(final int position) {
            if (!Preconditions.NoThrow.checkArgument(position >= 0)) {
                return this;
            }

            // Add in sorted order if this does not exist.
            int index = Collections.binarySearch(mFixedPositions, position);
            if (index < 0) {
                mFixedPositions.add(~index, position);
            }
            return this;
        }

        /**
         * Returns an ordered array of fixed ad positions.
         *
         * @return Fixed ad positions.
         */
        @NonNull
        List<Integer> getFixedPositions() {
            return mFixedPositions;
        }

        /**
         * Enables showing ads ad at a repeated interval.
         *
         * @param interval The frequency at which to show ads. Must be an integer greater than 1 or
         * the constant NO_REPEAT.
         * @return This object for easy use in chained setters.
         */
        @NonNull
        public MoPubClientPositioning enableRepeatingPositions(final int interval) {
            if (!Preconditions.NoThrow.checkArgument(
                    interval > 1, "Repeating interval must be greater than 1")) {
                mRepeatInterval = NO_REPEAT;
                return this;
            }
            mRepeatInterval = interval;
            return this;
        }

        /**
         * Returns the repeating ad interval.
         *
         * Repeating ads start after the last fixed position. Returns {@link #NO_REPEAT} if there is
         * no repeating interval.
         *
         * @return The repeating ad interval.
         */
        int getRepeatingInterval() {
            return mRepeatInterval;
        }
    }

    @NonNull
    static MoPubClientPositioning clone(@NonNull MoPubClientPositioning positioning) {
        Preconditions.checkNotNull(positioning);

        MoPubClientPositioning clone = new MoPubClientPositioning();
        clone.mFixedPositions.addAll(positioning.mFixedPositions);
        clone.mRepeatInterval = positioning.mRepeatInterval;
        return clone;
    }

    /**
     * Creates and returns a {@link MoPubClientPositioning} object.
     * @return A new positioning object.
     */
    @NonNull
    public static MoPubClientPositioning clientPositioning() {
        return new MoPubClientPositioning();
    }

    /**
     * Creates and returns a {@link MoPubServerPositioning} object.
     * @return A new positioning object.
     */
    @NonNull
    public static MoPubServerPositioning serverPositioning() {
        return new MoPubServerPositioning();
    }
}
