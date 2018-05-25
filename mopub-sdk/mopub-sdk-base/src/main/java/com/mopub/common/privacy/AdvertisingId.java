package com.mopub.common.privacy;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mopub.common.Preconditions;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class AdvertisingId implements Serializable {
    static final long ROTATION_TIME_MS = 24 * 60 * 60 * 1000;
    private static final String PREFIX_IFA = "ifa:";
    private static final String PREFIX_MOPUB = "mopub:";

    /**
     * time when mopub generated ID was rotated last time
     */
    @NonNull
    final Calendar mLastRotation;

    /**
     * Advertising ID from device, may not always be available.
     * Empty string if ifa is not available.
     */
    @NonNull
    final String mAdvertisingId;

    /**
     * virtual device ID, rotated every 24 hours
     */
    @NonNull
    final String mMopubId;

    /**
     * limit ad tracking device setting
     */
    final boolean mDoNotTrack;

    AdvertisingId(@NonNull String ifaId,
                  @NonNull String mopubId,
                  boolean limitAdTrackingEnabled,
                  long rotationTime) {
        Preconditions.NoThrow.checkNotNull(ifaId);
        Preconditions.NoThrow.checkNotNull(ifaId);

        mAdvertisingId = ifaId;
        mMopubId = mopubId;
        mDoNotTrack = limitAdTrackingEnabled;
        mLastRotation = Calendar.getInstance();
        mLastRotation.setTimeInMillis(rotationTime);
    }

    /**
     * @param consent - true means user is OK to track his data for Ad purposes
     * @return read advertising ID or UUID
     */
    public String getIdentifier(boolean consent) {
        return mDoNotTrack || !consent ? mMopubId : mAdvertisingId;
    }

    /**
     * @param consent - true means user is OK to track his data for Ad purposes
     * @return one of two: "mopub:mMopubId" or "ifa:mAdvertisingId"
     */
    @NonNull
    public String getIdWithPrefix(boolean consent) {
        if (mDoNotTrack || !consent || mAdvertisingId.isEmpty()) {
            return PREFIX_MOPUB + mMopubId;
        }
        return PREFIX_IFA + mAdvertisingId;
    }

    /**
     * Gets the ifa with the ifa prefix.
     *
     * @return The ifa, if it exists. Empty string if it doesn't.
     */
    @NonNull
    String getIfaWithPrefix() {
        if (TextUtils.isEmpty(mAdvertisingId)) {
            return "";
        }
        return PREFIX_IFA + mAdvertisingId;
    }

    /**
     * @return device Do Not Track settings
     */
    public boolean isDoNotTrack() {
        return mDoNotTrack;
    }

    @NonNull
    static AdvertisingId generateExpiredAdvertisingId() {
        Calendar time = Calendar.getInstance();
        String mopubId = generateIdString();
        return new AdvertisingId("", mopubId, false, time.getTimeInMillis() - ROTATION_TIME_MS - 1);
    }

    @NonNull
    static AdvertisingId generateFreshAdvertisingId() {
        Calendar time = Calendar.getInstance();
        String mopubId = generateIdString();
        return new AdvertisingId("", mopubId, false, time.getTimeInMillis());
    }

    @NonNull
    static String generateIdString() {
        return UUID.randomUUID().toString();
    }

    boolean isRotationRequired() {
        Calendar now = Calendar.getInstance();
        return now.getTimeInMillis() - mLastRotation.getTimeInMillis() >= ROTATION_TIME_MS;
    }

    @Override
    public String toString() {
        return "AdvertisingId{" +
                "mLastRotation=" + mLastRotation +
                ", mAdvertisingId='" + mAdvertisingId + '\'' +
                ", mMopubId='" + mMopubId + '\'' +
                ", mDoNotTrack=" + mDoNotTrack +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdvertisingId)) return false;

        AdvertisingId that = (AdvertisingId) o;

        if (mDoNotTrack != that.mDoNotTrack) return false;
        if (!mAdvertisingId.equals(that.mAdvertisingId)) return false;
        return mMopubId.equals(that.mMopubId);
    }

    @Override
    public int hashCode() {
        int result = mAdvertisingId.hashCode();
        result = 31 * result + mMopubId.hashCode();
        result = 31 * result + (mDoNotTrack ? 1 : 0);
        return result;
    }
}
