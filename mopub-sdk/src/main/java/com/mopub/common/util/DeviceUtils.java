package com.mopub.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StatFs;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.mopub.common.CreativeOrientation;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.common.util.VersionCode.HONEYCOMB;
import static com.mopub.common.util.VersionCode.currentApiLevel;
import static java.util.Collections.list;

public class DeviceUtils {
    private static final int MAX_MEMORY_CACHE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final int MIN_DISK_CACHE_SIZE = 30 * 1024 * 1024; // 30 MB
    private static final int MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100 MB

    private DeviceUtils() {}

    public static enum ForceOrientation {
        FORCE_PORTRAIT("portrait"),
        FORCE_LANDSCAPE("landscape"),
        DEVICE_ORIENTATION("device"),
        UNDEFINED("");

        @NonNull private final String mKey;

        private ForceOrientation(@NonNull final String key) {
            mKey = key;
        }

        @NonNull
        public static ForceOrientation getForceOrientation(@Nullable String key) {
            for (final ForceOrientation orientation : ForceOrientation.values()) {
                if (orientation.mKey.equalsIgnoreCase(key)) {
                    return orientation;
                }
            }

            return UNDEFINED;
        }
    }

    public static enum IP {
        IPv4,
        IPv6;

        private boolean matches(final String address) {
            switch (this) {
                case IPv4:
                    return InetAddressUtils.isIPv4Address(address);
                case IPv6:
                    return InetAddressUtils.isIPv6Address(address);
                default:
                    return false;
            }
        }

        private String toString(final String address) {
            switch (this) {
                case IPv4:
                    return address;
                case IPv6:
                    return address.split("%")[0];
                default:
                    return null;
            }
        }
    }

    public static String getIpAddress(IP ip) throws SocketException {
        for (final NetworkInterface networkInterface : list(NetworkInterface.getNetworkInterfaces())) {
            for (final InetAddress address : list(networkInterface.getInetAddresses())) {
                if (!address.isLoopbackAddress()) {
                    String hostAddress = address.getHostAddress().toUpperCase(Locale.US);
                    if (ip.matches(hostAddress)) {
                        return ip.toString(hostAddress);
                    }
                }
            }
        }

        return null;
    }

    public static String getHashedUdid(final Context context) {
        if (context == null) {
            return null;
        }

        String udid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return Utils.sha1(udid);
    }

    public static boolean isNetworkAvailable(@Nullable final Context context) {
        if (context == null) {
            return false;
        }

        final int internetPermission = context.checkCallingOrSelfPermission(INTERNET);
        if (internetPermission == PackageManager.PERMISSION_DENIED) {
            return false;
        }

        /**
         * This is only checking if we have permission to access the network state
         * It's possible to not have permission to check network state but still be able
         * to access the network itself.
         */
        final int networkAccessPermission = context.checkCallingOrSelfPermission(ACCESS_NETWORK_STATE);
        if (networkAccessPermission == PackageManager.PERMISSION_DENIED) {
            return true;
        }

        // Otherwise, perform the connectivity check.
        try {
            final ConnectivityManager connnectionManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connnectionManager.getActiveNetworkInfo();
            return networkInfo.isConnected();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static int memoryCacheSizeBytes(final Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        long memoryClass = activityManager.getMemoryClass();

        if (currentApiLevel().isAtLeast(HONEYCOMB)) {
            try {
                final int flagLargeHeap = ApplicationInfo.class.getDeclaredField("FLAG_LARGE_HEAP").getInt(null);
                if (Utils.bitMaskContainsFlag(context.getApplicationInfo().flags, flagLargeHeap)) {
                    memoryClass = (Integer) new MethodBuilder(activityManager, "getLargeMemoryClass").execute();
                }
            } catch (Exception e) {
                MoPubLog.d("Unable to reflectively determine large heap size on Honeycomb and above.");
            }
        }

        long result = Math.min(MAX_MEMORY_CACHE_SIZE, memoryClass / 8 * 1024 * 1024);
        return (int) result;
    }

    public static long diskCacheSizeBytes(File dir, long minSize) {
        long size = minSize;
        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long availableBytes = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            size = availableBytes / 50;
        } catch (IllegalArgumentException e) {
            MoPubLog.d("Unable to calculate 2% of available disk space, defaulting to minimum");
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

    public static long diskCacheSizeBytes(File dir) {
        return diskCacheSizeBytes(dir, MIN_DISK_CACHE_SIZE);
    }

    public static int getScreenOrientation(@NonNull final Activity activity) {
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final int deviceOrientation = activity.getResources().getConfiguration().orientation;

        return getScreenOrientationFromRotationAndOrientation(rotation, deviceOrientation);
    }

    static int getScreenOrientationFromRotationAndOrientation(int rotation, int orientation) {
        if (Configuration.ORIENTATION_PORTRAIT == orientation) {
            switch (rotation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_180:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        } else if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
            switch (rotation) {
                case Surface.ROTATION_180:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
        } else {
            MoPubLog.d("Unknown screen orientation. Defaulting to portrait.");
            return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
    }

    /**
     * Lock this activity in the requested orientation, rotating the display if necessary.
     *
     * @param creativeOrientation the orientation of the screen needed by the ad creative.
     */
    public static void lockOrientation(@NonNull Activity activity, @NonNull CreativeOrientation creativeOrientation) {
        if (!Preconditions.NoThrow.checkNotNull(creativeOrientation) || !Preconditions.NoThrow.checkNotNull(activity)) {
            return;
        }

        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int currentRotation = display.getRotation();
        final int deviceOrientation = activity.getResources().getConfiguration().orientation;

        final int currentOrientation = getScreenOrientationFromRotationAndOrientation(currentRotation, deviceOrientation);
        int requestedOrientation;

        // Choose a requested orientation that will result in the smallest change from the existing orientation.
        if (CreativeOrientation.PORTRAIT == creativeOrientation) {
            if (ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == currentOrientation) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
        } else if (CreativeOrientation.LANDSCAPE == creativeOrientation) {
            if (ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE == currentOrientation) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
        } else {
            // Don't lock screen orientation if the creative doesn't care.
            return;
        }

        activity.setRequestedOrientation(requestedOrientation);
    }

    /**
     * This tries to get the physical number of pixels on the device. This attempts to include
     * the pixels in the notification bar and soft buttons.
     *
     * @param context Needs a context (application is fine) to determine width/height.
     * @return Width and height of the device
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Point getDeviceDimensions(@NonNull final Context context) {
        Integer bestWidthPixels = null;
        Integer bestHeightPixels = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            final WindowManager windowManager = (WindowManager) context.getSystemService(
                    Context.WINDOW_SERVICE);
            final Display display = windowManager.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                final Point screenSize = new Point();
                display.getRealSize(screenSize);
                bestWidthPixels = screenSize.x;
                bestHeightPixels = screenSize.y;
            } else {
                try {
                    bestWidthPixels = (Integer) new MethodBuilder(display,
                            "getRawWidth").execute();
                    bestHeightPixels = (Integer) new MethodBuilder(display,
                            "getRawHeight").execute();
                } catch (Exception e) {
                    // Best effort. If this fails, just get the height and width normally,
                    // which may not capture the pixels used in the notification bar.
                    MoPubLog.v("Display#getRawWidth/Height failed.", e);
                }
            }
        }

        if (bestWidthPixels == null || bestHeightPixels == null) {
            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            bestWidthPixels = displayMetrics.widthPixels;
            bestHeightPixels = displayMetrics.heightPixels;
        }

        return new Point(bestWidthPixels, bestHeightPixels);
    }
}
