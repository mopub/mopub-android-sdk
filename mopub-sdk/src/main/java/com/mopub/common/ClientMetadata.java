package com.mopub.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Singleton that caches Client objects so they will be available to background threads.
 */
public class ClientMetadata {
    // Network type constant defined after API 9:
    private static final int TYPE_ETHERNET = 9;

    private static final String DEVICE_ORIENTATION_PORTRAIT = "p";
    private static final String DEVICE_ORIENTATION_LANDSCAPE = "l";
    private static final String DEVICE_ORIENTATION_SQUARE = "s";
    private static final String DEVICE_ORIENTATION_UNKNOWN = "u";
    private static final String IFA_PREFIX = "ifa:";
    private static final String SHA_PREFIX = "sha:";
    private static final int UNKNOWN_NETWORK = -1;

    private static volatile ClientMetadata sInstance;

    private String mNetworkOperator;
    private String mIsoCountryCode;
    private String mNetworkOperatorName;
    private String mUdid;

    /**
     * Returns the singleton ClientMetadata object, using the context to obtain data if necessary.
     */
    public static ClientMetadata getInstance(Context context) {
        // Use a local variable so we can reduce accesses of the volatile field.
        ClientMetadata result = sInstance;
        if (result == null) {
            synchronized (ClientMetadata.class) {
                result = sInstance;
                if (result == null) {
                    result = new ClientMetadata(context);
                    sInstance = result;
                }
            }
        }
        return result;
    }

    /**
     * Can be used by background threads and other objects without a context to attempt to get
     * ClientMetadata. If the object has never been referenced from a thread with a context,
     * this will return null.
     */
    public static ClientMetadata getInstance() {
        ClientMetadata result = sInstance;
        if (result == null) {
            // If it's being initialized in another thread, wait for the lock.
            synchronized (ClientMetadata.class) {
                result = sInstance;
            }
        }

        return result;
    }

    public static enum MoPubNetworkType {
        UNKNOWN(0),
        ETHERNET(1),
        WIFI(2),
        MOBILE(3);

        private final int mId;

        private MoPubNetworkType(int id) {
            mId = id;
        }

        @Override
        public String toString() {
            return Integer.toString(mId);
        }

        private static MoPubNetworkType fromAndroidNetworkType(int type) {
            switch(type) {
                case TYPE_ETHERNET:
                    return ETHERNET;
                case ConnectivityManager.TYPE_WIFI:
                    return WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                case ConnectivityManager.TYPE_MOBILE_HIPRI:
                case ConnectivityManager.TYPE_MOBILE_MMS:
                case ConnectivityManager.TYPE_MOBILE_SUPL:
                    return MOBILE;
                default:
                    return UNKNOWN;
            }
        }
    }

    // Cached client metadata used for generating URLs and events.
    private final String mDeviceManufacturer;
    private final String mDeviceModel;
    private final String mDeviceProduct;
    private final String mSdkVersion;
    private final String mAppVersion;
    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;

    private ClientMetadata(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mDeviceManufacturer = Build.MANUFACTURER;
        mDeviceModel = Build.MODEL;
        mDeviceProduct = Build.PRODUCT;
        mSdkVersion = MoPub.SDK_VERSION;

        // Cache context items that don't change:
        mAppVersion = getAppVersionFromContext(mContext);

        final TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        mNetworkOperator = telephonyManager.getNetworkOperator();
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA &&
                telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            mNetworkOperator = telephonyManager.getSimOperator();
        }

        mIsoCountryCode = telephonyManager.getNetworkCountryIso();
        try {
            // Some Lenovo devices require READ_PHONE_STATE here.
            mNetworkOperatorName = telephonyManager.getNetworkOperatorName();
        } catch (SecurityException e) {
            mNetworkOperatorName = null;
        }

        mUdid = getUdidFromContext(mContext);
    }

    private static String getAppVersionFromContext(Context context) {
        try {
            final String packageName = context.getPackageName();
            final PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (Exception exception) {
            MoPubLog.d("Failed to retrieve PackageInfo#versionName.");
            return null;
        }
    }

    private static String getUdidFromContext(Context context) {
        // try to use the android id from Google Play Services if available
        // if not fall back on the device id
        final String androidId = GpsHelper.getAdvertisingId(context);

        if (androidId != null) {
            return IFA_PREFIX + androidId;
        } else {
            String deviceId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            deviceId = (deviceId == null) ? "" : Utils.sha1(deviceId);
            return SHA_PREFIX + deviceId;
        }
    }

    /**
     * @return the display orientation. Useful when generating ad requests.
     */
    public String getOrientationString() {
        final int orientationInt = mContext.getResources().getConfiguration().orientation;
        String orientation = DEVICE_ORIENTATION_UNKNOWN;
        if (orientationInt == Configuration.ORIENTATION_PORTRAIT) {
            orientation = DEVICE_ORIENTATION_PORTRAIT;
        } else if (orientationInt == Configuration.ORIENTATION_LANDSCAPE) {
            orientation = DEVICE_ORIENTATION_LANDSCAPE;
        } else if (orientationInt == Configuration.ORIENTATION_SQUARE) {
            orientation = DEVICE_ORIENTATION_SQUARE;
        }
        return orientation;
    }


    public MoPubNetworkType getActiveNetworkType() {
        int networkType = UNKNOWN_NETWORK;
        if (mContext.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE) == PERMISSION_GRANTED) {
            NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            networkType = activeNetworkInfo != null
                    ? activeNetworkInfo.getType() : UNKNOWN_NETWORK;
        }
        return MoPubNetworkType.fromAndroidNetworkType(networkType);
    }


    /**
     * Get the logical density of the display as in {@link android.util.DisplayMetrics#density}
     */
    public float getDensity() {
        return mContext.getResources().getDisplayMetrics().density;
    }

    /**
     * @return whether doNotTrack is enabled in the advertising settings.
     */
    public boolean getDoNoTrack() {
        return GpsHelper.isLimitAdTrackingEnabled(mContext);
    }

    /**
     * @return the network operator.
     */
    public String getNetworkOperator() {
        return mNetworkOperator;
    }

    /**
     * @return the country code of the device.
     */
    public String getIsoCountryCode() {
        return mIsoCountryCode;
    }

    /**
     * @return the network operator name.
     */
    public String getNetworkOperatorName() {
        return mNetworkOperatorName;
    }

    /**
     * @return the Google Play advertising ID or the device ID if Play Services are not available.
     */
    public String getUdid() {
        return mUdid;
    }

    /**
     * @return the device manufacturer.
     */
    public String getDeviceManufacturer() {
        return mDeviceManufacturer;
    }

    /**
     * @return the device model identifier.
     */
    public String getDeviceModel() {
        return mDeviceModel;
    }

    /**
     * @return the device product identifier.
     */
    public String getDeviceProduct() {
        return mDeviceProduct;
    }

    /**
     * @return the MoPub SDK Version.
     */
    public String getSdkVersion() {
        return mSdkVersion;
    }

    /**
     * @return the version of the application the SDK is included in.
     */
    public String getAppVersion() {
        return mAppVersion;
    }

    @VisibleForTesting
    public static synchronized void clearForTesting() {
        sInstance = null;
    }
}
