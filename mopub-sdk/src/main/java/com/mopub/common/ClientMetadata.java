package com.mopub.common;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;

import java.util.Locale;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.content.pm.PackageManager.NameNotFoundException;
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

    private String mNetworkOperatorForUrl;
    private final String mNetworkOperator;
    private String mSimOperator;
    private final String mIsoCountryCode;
    private final String mSimIsoCountryCode;
    private String mNetworkOperatorName;
    private String mSimOperatorName;
    private String mUdid;
    private boolean mDoNotTrack = false;
    private boolean mAdvertisingInfoSet = false;

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

        public int getId() {
            return mId;
        }
    }

    private static volatile ClientMetadata sInstance;

    // Cached client metadata used for generating URLs and events.
    private final String mDeviceManufacturer;
    private final String mDeviceModel;
    private final String mDeviceProduct;
    private final String mDeviceOsVersion;
    private final String mSdkVersion;
    private final String mAppVersion;
    private final String mAppPackageName;
    private String mAppName;
    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;

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

    // NEVER CALL THIS AS A USER. Get it from the Singletons class.
    public ClientMetadata(Context context) {
        mContext = context.getApplicationContext();
        mConnectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mDeviceManufacturer = Build.MANUFACTURER;
        mDeviceModel = Build.MODEL;
        mDeviceProduct = Build.PRODUCT;
        mDeviceOsVersion = Build.VERSION.RELEASE;

        mSdkVersion = MoPub.SDK_VERSION;

        // Cache context items that don't change:
        mAppVersion = getAppVersionFromContext(mContext);
        PackageManager packageManager = mContext.getPackageManager();
        ApplicationInfo applicationInfo = null;
        mAppPackageName = context.getPackageName();
        try {
            applicationInfo = packageManager.getApplicationInfo(mAppPackageName, 0);
        } catch (final NameNotFoundException e) {
            // swallow
        }
        if (applicationInfo != null) {
            mAppName = (String) packageManager.getApplicationLabel(applicationInfo);
        }

        final TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mNetworkOperatorForUrl = telephonyManager.getNetworkOperator();
        mNetworkOperator = telephonyManager.getNetworkOperator();
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA &&
                telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            mNetworkOperatorForUrl = telephonyManager.getSimOperator();
            mSimOperator = telephonyManager.getSimOperator();
        }

        mIsoCountryCode = telephonyManager.getNetworkCountryIso();
        mSimIsoCountryCode = telephonyManager.getSimCountryIso();
        try {
            // Some Lenovo devices require READ_PHONE_STATE here.
            mNetworkOperatorName = telephonyManager.getNetworkOperatorName();
            if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                mSimOperatorName = telephonyManager.getSimOperatorName();
            }
        } catch (SecurityException e) {
            mNetworkOperatorName = null;
            mSimOperatorName = null;
        }

        // Get the device ID. This will be replaced later when the Play Services callbacks complete.
        mUdid = getDeviceIdFromContext(mContext);
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

    private static String getDeviceIdFromContext(Context context) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        deviceId = (deviceId == null) ? "" : Utils.sha1(deviceId);
        return SHA_PREFIX + deviceId;
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
     * @return the network operator for URL generators.
     */
    public String getNetworkOperatorForUrl() {
        return mNetworkOperatorForUrl;
    }

    /**
     * @return the network operator.
     */
    public String getNetworkOperator() {
        return mNetworkOperator;
    }

    public Locale getDeviceLocale() {
        return mContext.getResources().getConfiguration().locale;
    }

    /**
     * @return the sim operator.
     */
    public String getSimOperator() {
        return mSimOperator;
    }

    /**
     * @return the country code of the device.
     */
    public String getIsoCountryCode() {
        return mIsoCountryCode;
    }

    /**
     * @return the sim provider's country code.
     */
    public String getSimIsoCountryCode() {
        return mSimIsoCountryCode;
    }

    /**
     * @return the network operator name.
     */
    public String getNetworkOperatorName() {
        return mNetworkOperatorName;
    }

    /**
     * @return the sim operator name.
     */
    public String getSimOperatorName() {
        return mSimOperatorName;
    }

    /**
     * @return the stored device ID.
     */
    public synchronized String getDeviceId() {
        return mUdid;
    }

    /**
     * @return the user's do not track preference. Should be set whenever a getAdInfo() call is
     *         completed.
     */
    public synchronized boolean isDoNotTrackSet() {
        return mDoNotTrack;
    }

    public synchronized void setAdvertisingInfo(String advertisingId, boolean doNotTrack) {
        mUdid = IFA_PREFIX + advertisingId;
        mDoNotTrack = doNotTrack;
        mAdvertisingInfoSet = true;
    }

    public synchronized boolean isAdvertisingInfoSet() {
        return mAdvertisingInfoSet;
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
     * @return the device os version.
     */
    public String getDeviceOsVersion() {
        return mDeviceOsVersion;
    }

    /**
     * @return the device screen width in dips according to current orientation.
     */
    public int getDeviceScreenWidthDip() {
        return Dips.screenWidthAsIntDips(mContext);
    }

    /**
     * @return the device screen height in dips according to current orientation.
     */
    public int getDeviceScreenHeightDip() {
        return Dips.screenHeightAsIntDips(mContext);
    }

    /**
     * This tries to get the physical number of pixels on the device. This attempts to include
     * the pixels in the notification bar and soft buttons. This method only works after
     * mContext is initialized.
     *
     * @return Width and height of the device. This is 0 by 0 if there is no context.
     */
    public Point getDeviceDimensions() {
        if (Preconditions.NoThrow.checkNotNull(mContext)) {
            return DeviceUtils.getDeviceDimensions(mContext);
        }
        return new Point(0, 0);
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

    /**
     * @return the package of the application the SDK is included in.
     */
    public String getAppPackageName() {
        return mAppPackageName;
    }

    /**
     * @return the name of the application the SDK is included in.
     */
    public String getAppName() {
        return mAppName;
    }

    @Deprecated
    @VisibleForTesting
    public static void setInstance(ClientMetadata clientMetadata) {
        synchronized (ClientMetadata.class) {
            sInstance = clientMetadata;
        }
    }

    @VisibleForTesting
    public static void clearForTesting() {
        sInstance = null;
    }
}
