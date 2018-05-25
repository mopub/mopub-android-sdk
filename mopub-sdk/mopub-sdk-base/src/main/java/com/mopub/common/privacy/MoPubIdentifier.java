package com.mopub.common.privacy;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.GpsHelper;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Calendar;

public class MoPubIdentifier {

    public interface AdvertisingIdChangeListener {
        void onIdChanged(@NonNull final AdvertisingId oldId, @NonNull final AdvertisingId newId);
    }

    private static final String PREF_AD_INFO_GROUP = "com.mopub.settings.identifier";
    private static final String PREF_IFA_IDENTIFIER = "privacy.identifier.ifa";
    private static final String PREF_MOPUB_IDENTIFIER = "privacy.identifier.mopub";
    private static final String PREF_IDENTIFIER_TIME = "privacy.identifier.time";
    private static final String PREF_LIMIT_AD_TRACKING = "privacy.limit.ad.tracking";
    private static final int MISSING_VALUE = -1;

    @NonNull
    private AdvertisingId mAdInfo;
    @NonNull
    private final Context mAppContext;

    @Nullable
    private AdvertisingIdChangeListener mIdChangeListener;

    private boolean mRefreshingAdvertisingInfo;

    private boolean initialized;
    @Nullable
    private SdkInitializationListener mInitializationListener;

    public MoPubIdentifier(@NonNull final Context appContext) {
        this(appContext, null);
    }

    @VisibleForTesting
    MoPubIdentifier(@NonNull final Context appContext,
            @Nullable final AdvertisingIdChangeListener idChangeListener) {
        Preconditions.NoThrow.checkNotNull(appContext);

        mAppContext = appContext;
        mIdChangeListener = idChangeListener;
        mAdInfo = readIdFromStorage(mAppContext);
        if (mAdInfo == null) {
            mAdInfo = AdvertisingId.generateExpiredAdvertisingId();
        }
        refreshAdvertisingInfo();
    }

    /**
     * @return the most recent advertising ID and Do Not Track settings. This method  internally
     * initiates AdvertisingId refresh. The value is returned instantly on UI thread,
     * but may take some time to communicate with Google Play Services API when called
     * from background thread.
     */
    @NonNull
    public AdvertisingId getAdvertisingInfo() {
        final AdvertisingId adInfo = mAdInfo;
        refreshAdvertisingInfo();
        return adInfo;
    }

    private void refreshAdvertisingInfo() {
        if (mRefreshingAdvertisingInfo) {
            return;
        }
        mRefreshingAdvertisingInfo = true;
        new RefreshAdvertisingInfoAsyncTask().execute();
    }

    void refreshAdvertisingInfoBackgroundThread() {
        long time = Calendar.getInstance().getTimeInMillis();

        // try google
        if (isPlayServicesAvailable()) {
            GpsHelper.AdvertisingInfo info = GpsHelper.fetchAdvertisingInfoSync(mAppContext);
            if (info != null) {
                final AdvertisingId oldId = mAdInfo;
                if (info.limitAdTracking && oldId.isRotationRequired()) {
                    setAdvertisingInfo(info.advertisingId, AdvertisingId.generateIdString(), info.limitAdTracking, time);
                } else {
                    setAdvertisingInfo(info.advertisingId, oldId.mMopubId, info.limitAdTracking, oldId.mLastRotation.getTimeInMillis());
                }
                return;
            }
        }

        // try amazon
        final AdvertisingId info = getAmazonAdvertisingInfo(mAppContext);
        if (info != null) {
            final AdvertisingId oldId = mAdInfo;
            if (info.isDoNotTrack() && oldId.isRotationRequired()) {
                setAdvertisingInfo(info.mAdvertisingId, AdvertisingId.generateIdString(), info.mDoNotTrack, time);
            } else {
                setAdvertisingInfo(info.mAdvertisingId, oldId.mMopubId, info.mDoNotTrack, oldId.mLastRotation.getTimeInMillis());
            }
            return;
        }

        // MoPub
        rotateMopubId();
    }

    @Nullable
    private static synchronized AdvertisingId readIdFromStorage(@NonNull final Context appContext) {
        Preconditions.checkNotNull(appContext);

        Calendar now = Calendar.getInstance();
        try {
            final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(appContext, PREF_AD_INFO_GROUP);
            final String ifa_id = preferences.getString(PREF_IFA_IDENTIFIER, "");
            final String mopub_id = preferences.getString(PREF_MOPUB_IDENTIFIER, "");
            final long time = preferences.getLong(PREF_IDENTIFIER_TIME, now.getTimeInMillis());
            final boolean limitTracking = preferences.getBoolean(PREF_LIMIT_AD_TRACKING, false);
            if (!TextUtils.isEmpty(ifa_id) && !TextUtils.isEmpty(mopub_id)) {
                return new AdvertisingId(ifa_id, mopub_id, limitTracking, time);
            }
        } catch (ClassCastException ex) {
            MoPubLog.e("Cannot read identifier from shared preferences");
        }
        return null;
    }

    private static synchronized void writeIdToStorage(@NonNull final Context context, @NonNull final AdvertisingId info) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(info);

        final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(context, PREF_AD_INFO_GROUP);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_LIMIT_AD_TRACKING, info.mDoNotTrack);
        editor.putString(PREF_IFA_IDENTIFIER, info.mAdvertisingId);
        editor.putString(PREF_MOPUB_IDENTIFIER, info.mMopubId);
        editor.putLong(PREF_IDENTIFIER_TIME, info.mLastRotation.getTimeInMillis());
        editor.apply();
    }

    @VisibleForTesting
    static synchronized void clearStorage(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final SharedPreferences preferences = SharedPreferencesHelper.getSharedPreferences(context, PREF_AD_INFO_GROUP);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_LIMIT_AD_TRACKING);
        editor.remove(PREF_IFA_IDENTIFIER);
        editor.remove(PREF_MOPUB_IDENTIFIER);
        editor.remove(PREF_IDENTIFIER_TIME);
        editor.apply();
    }

    private void rotateMopubId() {
        if (!mAdInfo.isRotationRequired()) {
            setAdvertisingInfo(mAdInfo);
            return;
        }

        setAdvertisingInfo(AdvertisingId.generateFreshAdvertisingId());
    }

    private void setAdvertisingInfo(@NonNull String advertisingId, @NonNull String mopubId, boolean limitAdTracking, long rotationTime) {
        Preconditions.NoThrow.checkNotNull(advertisingId);
        Preconditions.NoThrow.checkNotNull(mopubId);

        setAdvertisingInfo(new AdvertisingId(advertisingId, mopubId, limitAdTracking, rotationTime));
    }

    private void setAdvertisingInfo(@NonNull final AdvertisingId newId) {
        AdvertisingId oldId = mAdInfo;
        mAdInfo = newId;
        writeIdToStorage(mAppContext, mAdInfo);

        if (!mAdInfo.equals(oldId) || !initialized) {
            notifyIdChangeListener(oldId, mAdInfo);
        }

        if (!initialized) {
            reportInitializationComplete();
        }
    }

    /**
     * @param idChangeListener - will be called every time the OS Advertising ID or
     *                         flag do-not-track changes its value. Pass null to stop listening.
     */
    public void setIdChangeListener(@Nullable final AdvertisingIdChangeListener idChangeListener) {
        mIdChangeListener = idChangeListener;
    }

    void setInitializationListener(@Nullable final SdkInitializationListener initializationListener) {
        mInitializationListener = initializationListener;
        if (initialized) {
            reportInitializationComplete();
        }
    }

    private void reportInitializationComplete() {
        if (mInitializationListener != null) {
            mInitializationListener.onInitializationFinished();
            mInitializationListener = null;
        }
        initialized = true;
    }

    private void notifyIdChangeListener(@NonNull final AdvertisingId oldId, @NonNull final AdvertisingId newId) {
        Preconditions.checkNotNull(newId);

        if (mIdChangeListener != null) {
            mIdChangeListener.onIdChanged(oldId, newId);
        }
    }

    private boolean isPlayServicesAvailable() {
        return GpsHelper.isPlayServicesAvailable(mAppContext);
    }

    // For Amazon tablets running Fire OS 5.1+ and TV devices running Fire OS 5.2.1.1+, the
    // advertising info is available as System Settings.
    // See https://developer.amazon.com/public/solutions/devices/fire-tv/docs/fire-tv-advertising-id
    @Nullable
    private AdvertisingId getAmazonAdvertisingInfo(@NonNull final Context context) {
        Preconditions.NoThrow.checkNotNull(context);

        ContentResolver resolver = context.getContentResolver();
        int limitAdTracking = Settings.Secure.getInt(resolver, "limit_ad_tracking", MISSING_VALUE);
        String advertisingId = Settings.Secure.getString(resolver, "advertising_id");

        if (limitAdTracking != MISSING_VALUE && !TextUtils.isEmpty(advertisingId)) {
            boolean doNotTrack = limitAdTracking != 0;
            final AdvertisingId oldId = mAdInfo;
            // merge Amazon and MoPub data in one object
            return new AdvertisingId(advertisingId, oldId.mMopubId, doNotTrack, oldId.mLastRotation.getTimeInMillis());
        }
        return null;
    }

    private class RefreshAdvertisingInfoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(final Void... voids) {
            refreshAdvertisingInfoBackgroundThread();
            mRefreshingAdvertisingInfo = false;
            return null;
        }
    }
}
