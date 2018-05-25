package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class LocationServiceTest {
    private Activity activity;
    private Location networkLocation;
    private Location gpsLocation;
    private Location cachedLocation;
    private ShadowLocationManager shadowLocationManager;
    private PersonalInfoManager mockPersonalInfoManager;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        LocationService.clearLastKnownLocation();

        networkLocation = new Location("networkLocation");
        networkLocation.setLatitude(3.1415926535);
        networkLocation.setLongitude(-27.18281828459);
        networkLocation.setAccuracy(10);
        networkLocation.setTime(100);

        gpsLocation = new Location("gpsLocation");
        gpsLocation.setLatitude(-1.23456789);
        gpsLocation.setLongitude(98.7654321);
        gpsLocation.setAccuracy(1000);
        gpsLocation.setTime(200);

        cachedLocation = new Location("cachedLocation");
        cachedLocation.setLatitude(37.776822);
        cachedLocation.setLongitude(-122.416604);
        cachedLocation.setAccuracy(25);

        shadowLocationManager = shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER, networkLocation);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, gpsLocation);
        mockPersonalInfoManager = mock(PersonalInfoManager.class);
    }

    @After
    public void tearDown() {
        LocationService.clearLastKnownLocation();
    }

    @Test
    public void getLastKnownLocation_withFinePermission_shouldReturnMoreRecentLocation() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_FINE_LOCATION);

        final Location result =
                LocationService.getLastKnownLocation(activity, 10, MoPub.LocationAwareness.NORMAL);

        // gpsLocation has a more recent timestamp than networkLocation
        assertThat(result).isEqualTo(gpsLocation);
    }

    @Test
    public void getLastKnownLocation_withFinePermission_withLocationAwarenessTruncated_shouldTruncateLocationLatLon() throws  Exception{
        Shadows.shadowOf(activity).grantPermissions(ACCESS_FINE_LOCATION);

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        final Location result =
                LocationService.getLastKnownLocation(activity, 2, MoPub.LocationAwareness.TRUNCATED);

        // expected more recent gpsLocation, truncated
        assertThat(result.getLatitude()).isEqualTo(-1.23);
        assertThat(result.getLongitude()).isEqualTo(98.77);
        // accuracy should be unchanged
        assertThat(result.getAccuracy()).isEqualTo(1000);
    }

    @Test
    public void getLastKnownLocation_withOnlyCoarsePermission_shouldReturnNetworkLocation() throws Exception {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION);
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        final Location result =
                LocationService.getLastKnownLocation(activity, 10, MoPub.LocationAwareness.NORMAL);

        // only has coarse location access, expected networkLocation
        assertThat(result).isEqualTo(networkLocation);
    }

    @Test
    public void getLastKnownLocation_withOnlyCoarsePermission_withLocationAwarenessTruncated_shouldTruncateLocationLatLon() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION);

        final Location result =
                LocationService.getLastKnownLocation(activity, 2, MoPub.LocationAwareness.TRUNCATED);

        // expected networkLocation, truncated
        assertThat(result.getLatitude()).isEqualTo(3.14);
        assertThat(result.getLongitude()).isEqualTo(-27.18);
        // accuracy should be unchanged
        assertThat(result.getAccuracy()).isEqualTo(10);
    }

    @Test
    public void getLastKnownLocation_withNoLocationPermissions_shouldReturnNull() {
        final Location result =
                LocationService.getLastKnownLocation(activity, 10, MoPub.LocationAwareness.NORMAL);

        assertThat(result).isNull();
    }

    @Test
    public void getLastKnownLocation_withLocationAwarenessDisabled_shouldReturnNull() throws Exception {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
            .setStatic(MoPub.class)
            .setAccessible()
            .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
            .execute();

        final Location result =
                LocationService.getLastKnownLocation(activity, 10, MoPub.LocationAwareness.DISABLED);

        assertThat(result).isNull();
    }

    @Test
    public void getLastKnownLocation_withFreshPreviousKnownLocation_shouldReturnPreviousKnownLocation() {
        LocationService locationService = LocationService.getInstance();
        locationService.mLastKnownLocation = cachedLocation;
        // Setting the location updated time to be more recent than minimum location refresh time,
        // in milliseconds.
        locationService.mLocationLastUpdatedMillis = SystemClock.elapsedRealtime() -
                MoPub.getMinimumLocationRefreshTimeMillis() / 2;

        final Location result = LocationService.getLastKnownLocation(activity, 10,
                MoPub.LocationAwareness.NORMAL);

        assertThat(result).isEqualTo(cachedLocation);
    }

    @Test
    public void getLastKnownLocation_withStalePreviousKnownLocation_shouldReturnNull() {
        LocationService locationService = LocationService.getInstance();
        locationService.mLastKnownLocation = cachedLocation;
        // Setting the location updated time to be older than minimum location refresh time,
        // in milliseconds.
        locationService.mLocationLastUpdatedMillis = SystemClock.elapsedRealtime() -
                MoPub.getMinimumLocationRefreshTimeMillis() * 2;

        final Location result = LocationService.getLastKnownLocation(activity, 10,
                MoPub.LocationAwareness.NORMAL);

        assertThat(result).isNull();
    }

    @Test
    public void getLocationFromProvider_withNetworkProvider_withCoarsePermission_shouldReturnNetworkLocation() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION);

        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.NETWORK);

        assertThat(result).isEqualTo(networkLocation);
    }

    @Test
    public void getLocationFromProvider_withNetworkProvider_withFinePermission_shouldReturnNetworkLocation() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_FINE_LOCATION);

        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.NETWORK);

        assertThat(result).isEqualTo(networkLocation);
    }

    @Test
    public void getLocationFromProvider_withNetworkProvider_withNoPermissions_shouldReturnNull() {
        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.NETWORK);

        assertThat(result).isNull();
    }

    @Test
    public void getLocationFromProvider_withGpsProvider_withCoarsePermission_shouldReturnNull() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION);

        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.GPS);

        assertThat(result).isNull();
    }

    @Test
    public void getLocationFromProvider_withGpsProvider_withFinePermission_shouldReturnGpsLocation() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_FINE_LOCATION);

        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.GPS);

        assertThat(result).isEqualTo(gpsLocation);
    }

    @Test
    public void getLocationFromProvider_withGpsProvider_withNoPermissions_shouldReturnNull() {
        final Location result = LocationService.getLocationFromProvider(activity,
                LocationService.ValidLocationProvider.GPS);

        assertThat(result).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void getLocationFromProvider_withNullContext_shouldThrowNPE() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);

        LocationService.getLocationFromProvider(null, LocationService.ValidLocationProvider.GPS);
    }

    @Test(expected = NullPointerException.class)
    public void getLocationFromProvider_withNullProvider_shouldThrowNPE() {
        Shadows.shadowOf(activity).grantPermissions(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION);

        LocationService.getLocationFromProvider(activity, null);
    }

    @Test
    public void getMostRecentValidLocation_shouldReturnMoreRecentLocation() {
        assertThat(LocationService.getMostRecentValidLocation(networkLocation, gpsLocation))
                .isEqualTo(gpsLocation);
    }

    @Test
    public void getMostRecentValidLocation_withFirstLocationValid_withSecondLocationNull_shouldReturnFirstLocation() {
        assertThat(LocationService.getMostRecentValidLocation(networkLocation, null))
                .isEqualTo(networkLocation);
    }

    @Test
    public void getMostRecentValidLocation_withSecondLocationValid_withFirstLocationNull_shouldReturnSecondLocation() {
        assertThat(LocationService.getMostRecentValidLocation(null, gpsLocation))
                .isEqualTo(gpsLocation);
    }

    @Test
    public void getMostRecentValidLocation_withBothLocationsNull_shouldReturnNull() {
        assertThat(LocationService.getMostRecentValidLocation(null, null)).isNull();
    }

    @Test
    public void truncateLocationLatLon_shouldRoundLatitudeAndLongitude() {
        LocationService.truncateLocationLatLon(networkLocation, 4);

        assertThat(networkLocation.getLatitude()).isEqualTo(3.1416);
        assertThat(networkLocation.getLongitude()).isEqualTo(-27.1828);
        // accuracy should be unchanged
        assertThat(networkLocation.getAccuracy()).isEqualTo(10);
    }

    @Test
    public void truncateLocationLatLon_withZeroPrecision_shouldRemoveFractionalPortion() {
        LocationService.truncateLocationLatLon(networkLocation, 0);

        assertThat(networkLocation.getLatitude()).isEqualTo(3);
        assertThat(networkLocation.getLongitude()).isEqualTo(-27);
        assertThat(networkLocation.getAccuracy()).isEqualTo(10);
    }

    @Test
    public void truncateLocationLatLon_withPrecisionLongerThanProvidedDecimalPoints_shouldNotChangeLocation() {
        LocationService.truncateLocationLatLon(gpsLocation, 100);

        assertThat(gpsLocation.getLatitude()).isEqualTo(-1.23456789);
        assertThat(gpsLocation.getLongitude()).isEqualTo(98.7654321);
        assertThat(gpsLocation.getAccuracy()).isEqualTo(1000);
    }

    @Test
    public void truncateLocationLatLon_withNullLocation_shouldNotChangeLocation() {
        LocationService.truncateLocationLatLon(null, 1);

        assertThat(gpsLocation.getLatitude()).isEqualTo(-1.23456789);
        assertThat(gpsLocation.getLongitude()).isEqualTo(98.7654321);
        assertThat(gpsLocation.getAccuracy()).isEqualTo(1000);
    }

    @Test
    public void truncateLocationLatLon_withNegativePrecision_shouldNotChangeLocation() {
        LocationService.truncateLocationLatLon(gpsLocation, -1);

        assertThat(gpsLocation.getLatitude()).isEqualTo(-1.23456789);
        assertThat(gpsLocation.getLongitude()).isEqualTo(98.7654321);
        assertThat(gpsLocation.getAccuracy()).isEqualTo(1000);
    }
}
