package com.mopub.common;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;

import java.math.BigDecimal;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class LocationService {
    public enum LocationAwareness {
        NORMAL, TRUNCATED, DISABLED;

        // These deprecated methods are only used to support the deprecated methods
        // MoPubView#setLocationAwareness, MoPubInterstitial#setLocationAwareness
        // and should not be used elsewhere. Unless interacting with those methods, use
        // the type MoPub.LocationAwareness

        @Deprecated
        public MoPub.LocationAwareness getNewLocationAwareness() {
            if (this == TRUNCATED) {
                return MoPub.LocationAwareness.TRUNCATED;
            } else if (this == DISABLED) {
                return MoPub.LocationAwareness.DISABLED;
            } else {
                return MoPub.LocationAwareness.NORMAL;
            }
        }

        @Deprecated
        public static LocationAwareness
                fromMoPubLocationAwareness(MoPub.LocationAwareness awareness) {
            if (awareness == MoPub.LocationAwareness.DISABLED) {
                return DISABLED;
            } else if (awareness == MoPub.LocationAwareness.TRUNCATED) {
                return TRUNCATED;
            } else {
                return NORMAL;
            }
        }
    }

    private static volatile LocationService sInstance;
    @VisibleForTesting @Nullable Location mLastKnownLocation;
    @VisibleForTesting long mLocationLastUpdatedMillis;

    private LocationService() {
    }

    @VisibleForTesting
    @NonNull
    static LocationService getInstance() {
        LocationService locationService = sInstance;
        if (locationService == null) {
            synchronized (LocationService.class) {
                locationService = sInstance;
                if (locationService == null) {
                    locationService = new LocationService();
                    sInstance = locationService;
                }
            }
        }
        return locationService;
    }

    public enum ValidLocationProvider {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER);

        @NonNull final String name;

        ValidLocationProvider(@NonNull final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        private boolean hasRequiredPermissions(@NonNull final Context context) {
            switch (this) {
                case NETWORK:
                    return DeviceUtils.isPermissionGranted(context, ACCESS_FINE_LOCATION)
                            || DeviceUtils.isPermissionGranted(context, ACCESS_COARSE_LOCATION);
                case GPS:
                    return DeviceUtils.isPermissionGranted(context, ACCESS_FINE_LOCATION);
                default:
                    return false;
            }
        }
    }

    /**
     * Returns the last known location of the device using its GPS and network location providers.
     * This only checks Android location providers as often as
     * {@link MoPub#getMinimumLocationRefreshTimeMillis()} says to, in milliseconds.
     * <p>
     * May be {@code null} if:
     * <ul>
     * <li> Location permissions are not requested in the Android manifest file
     * <li> The location providers don't exist
     * <li> Location awareness is disabled in the parent MoPubView
     * </ul>
     */
    @Nullable
    public static Location getLastKnownLocation(@NonNull final Context context,
            final int locationPrecision,
            final @NonNull MoPub.LocationAwareness locationAwareness) {

        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(locationAwareness);

        if (locationAwareness == MoPub.LocationAwareness.DISABLED) {
            return null;
        }

        final LocationService locationService = getInstance();

        if (isLocationFreshEnough()) {
            return locationService.mLastKnownLocation;
        }

        final Location gpsLocation = getLocationFromProvider(context, ValidLocationProvider.GPS);
        final Location networkLocation = getLocationFromProvider(context, ValidLocationProvider.NETWORK);
        final Location result = getMostRecentValidLocation(gpsLocation, networkLocation);

        // Truncate latitude/longitude to the number of digits specified by locationPrecision.
        if (locationAwareness == MoPub.LocationAwareness.TRUNCATED) {
            truncateLocationLatLon(result, locationPrecision);
        }

        locationService.mLastKnownLocation = result;
        locationService.mLocationLastUpdatedMillis = SystemClock.elapsedRealtime();
        return result;
    }

    @VisibleForTesting
    @Nullable
    static Location getLocationFromProvider(@NonNull final Context context,
            @NonNull final ValidLocationProvider provider) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(provider);

        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }

        if (!provider.hasRequiredPermissions(context)) {
            return null;
        }

        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            // noinspection ResourceType
            return locationManager.getLastKnownLocation(provider.toString());
        } catch (SecurityException e) {
            MoPubLog.d("Failed to retrieve location from " +
                    provider.toString() + " provider: access appears to be disabled.");
        } catch (IllegalArgumentException e) {
            MoPubLog.d("Failed to retrieve location: device has no " +
                    provider.toString() + " location provider.");
        } catch (NullPointerException e) { // This happens on 4.2.2 on a few Android TV devices
            MoPubLog.d("Failed to retrieve location: device has no " +
                    provider.toString() + " location provider.");
        }

        return null;
    }

    @VisibleForTesting
    @Nullable
    static Location getMostRecentValidLocation(@Nullable final Location a, @Nullable final Location b) {
        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        // At this point, locations A and B are non-null, so return the more recent one
        return (a.getTime() > b.getTime()) ? a : b;
    }

    @VisibleForTesting
    static void truncateLocationLatLon(@Nullable final Location location,
            final int precision) {
        if (location == null || precision < 0) {
            return;
        }

        double lat = location.getLatitude();
        double truncatedLat = BigDecimal.valueOf(lat)
                .setScale(precision, BigDecimal.ROUND_HALF_DOWN)
                .doubleValue();
        location.setLatitude(truncatedLat);

        double lon = location.getLongitude();
        double truncatedLon = BigDecimal.valueOf(lon)
                .setScale(precision, BigDecimal.ROUND_HALF_DOWN)
                .doubleValue();
        location.setLongitude(truncatedLon);
    }

    private static boolean isLocationFreshEnough() {
        final LocationService locationService = LocationService.getInstance();
        if (locationService.mLastKnownLocation == null) {
            return false;
        }
        return SystemClock.elapsedRealtime() - locationService.mLocationLastUpdatedMillis <=
                MoPub.getMinimumLocationRefreshTimeMillis();
    }

    @Deprecated
    @VisibleForTesting
    public static void clearLastKnownLocation() {
        getInstance().mLastKnownLocation = null;
    }
}
