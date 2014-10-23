package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import com.mopub.common.factories.MethodBuilderFactory;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.util.concurrent.Semaphore;

import static com.mopub.common.util.Reflection.MethodBuilder;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class GpsHelperTest {
    private MethodBuilder methodBuilder;
    private Activity context;
    private TestAdInfo adInfo;
    private Semaphore semaphore;
    private GpsHelper.GpsHelperListener semaphoreGpsHelperListener;
    private Looper gpsHelperListenerCallbackLooper;

    // This class emulates the AdInfo class returned from the Google Play Services
    // AdvertisingIdClient.getAdvertisingIdInfo method; need to implement getters for reflection calls
    public static final class TestAdInfo {
        public static final String ADVERTISING_ID = "38400000-8cf0-11bd-b23e-10b96e40000d";
        public static final boolean LIMIT_AD_TRACKING_ENABLED = true;

        public String getId() {
            return ADVERTISING_ID;
        }

        public boolean isLimitAdTrackingEnabled() {
            return LIMIT_AD_TRACKING_ENABLED;
        }
    }

    @Before
    public void setup() {
    	context = new Activity();
        adInfo = new TestAdInfo();

        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);

        semaphore = new Semaphore(0);
        semaphoreGpsHelperListener = new GpsHelper.GpsHelperListener() {
            @Override
            public void onFetchAdInfoCompleted() {
                gpsHelperListenerCallbackLooper = Looper.myLooper();
                semaphore.release();
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void isGpsAvailable_whenGooglePlayServicesIsLinked_shouldReturnTrue() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);
        assertThat(GpsHelper.isGpsAvailable(context)).isTrue();
    }

    @Test
    public void isGpsAvailable_whenGooglePlayServicesReturnsNonSuccessCode_shouldReturnFalse() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE + 1);
        assertThat(GpsHelper.isGpsAvailable(context)).isFalse();
    }

    @Test
    public void isGpsAvailable_whenGooglePlayServicesReturnsNull_shouldReturnFalse() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(null);
        assertThat(GpsHelper.isGpsAvailable(context)).isFalse();
    }

    @Test
    public void isGpsAvailable_whenGooglePlayServicesIsNotLinked_shouldReturnFalse() throws Exception {
        assertThat(GpsHelper.isGpsAvailable(context)).isFalse();
    }

    @Test
    public void asyncFetchAdvertisingInfo_whenGooglePlayServicesIsLinked_shouldInvokeCallbackOnMainLooper() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(
                adInfo,
                adInfo.ADVERTISING_ID,
                adInfo.LIMIT_AD_TRACKING_ENABLED
        );

        GpsHelper.asyncFetchAdvertisingInfo(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        assertThat(gpsHelperListenerCallbackLooper).isEqualTo(Looper.getMainLooper());
    }

    @Test
    public void asyncFetchAdvertisingInfo_whenGooglePlayServicesIsLinked_shouldPopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(
                adInfo,
                adInfo.ADVERTISING_ID,
                adInfo.LIMIT_AD_TRACKING_ENABLED
        );

        GpsHelper.asyncFetchAdvertisingInfo(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verifySharedPreferences(context, adInfo);
    }

    @Test
    public void asyncFetchAdvertisingInfo_whenReflectedMethodCallThrows_shouldNotPopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenThrow(new Exception());

        GpsHelper.asyncFetchAdvertisingInfo(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verifyCleanSharedPreferences(context);
    }

    @Test
    public void asyncFetchAdvertisingInfo_whenReflectedMethodCallReturnsNull_shouldNotPopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(null);

        GpsHelper.asyncFetchAdvertisingInfo(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verifyCleanSharedPreferences(context);
    }

    @Test
    public void asyncFetchAdvertisingInfoIfNotCached_whenGooglePlayServicesIsLinkedAndSharedPreferencesIsClean_shouldPopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE,
                adInfo,
                adInfo.ADVERTISING_ID,
                adInfo.LIMIT_AD_TRACKING_ENABLED
        );

        GpsHelper.asyncFetchAdvertisingInfoIfNotCached(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verifySharedPreferences(context, adInfo);
    }

    @Test
    public void asyncFetchAdvertisingInfoIfNotCached_whenGooglePlayServicesLinkedAndSharedPreferencesIsPopulated_shouldNotRePopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        populateAndVerifySharedPreferences(context, adInfo);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE
        );

        GpsHelper.asyncFetchAdvertisingInfoIfNotCached(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verify(methodBuilder).execute();
    }

    @Test
    public void asyncFetchAdvertisingInfoIfNotCached_whenGooglePlayServicesIsNotLinked_shouldNotPopulateSharedPreferences() throws Exception {
        verifyCleanSharedPreferences(context);
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE + 1
        );

        GpsHelper.asyncFetchAdvertisingInfoIfNotCached(context, semaphoreGpsHelperListener);
        safeAcquireSemaphore();
        verifyCleanSharedPreferences(context);
    }

    @Test
    public void isSharedPreferencesPopulated_whenContainsAdvertisingIdKeyAndIsLimitAdTrackingEnabledKey_shouldReturnTrue() throws Exception {
        verifyCleanSharedPreferences(context);
        populateAndVerifySharedPreferences(context, adInfo);
        assertThat(GpsHelper.isSharedPreferencesPopluated(context)).isTrue();
    }

    @Test
    public void isSharedPreferencesPopulated_whenDoesntContainBothKeys_shouldReturnFalse() throws Exception {
        verifyCleanSharedPreferences(context);
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putString(GpsHelper.ADVERTISING_ID_KEY, adInfo.ADVERTISING_ID)
                .commit();
        assertThat(GpsHelper.isSharedPreferencesPopluated(context)).isFalse();

        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().commit();
        verifyCleanSharedPreferences(context);
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putBoolean(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY, adInfo.LIMIT_AD_TRACKING_ENABLED)
                .commit();
        assertThat(GpsHelper.isSharedPreferencesPopluated(context)).isFalse();
    }

    @Test
    public void isSharedPreferencesPopulated_whenClean_shouldReturnFalse() throws Exception {
        verifyCleanSharedPreferences(context);
        assertThat(GpsHelper.isSharedPreferencesPopluated(context)).isFalse();
    }

    @Test
    public void updateSharedPreferences_whenPassingInValidAdInfoObject_shouldUpdateSharedPreferences() throws Exception {
        // Use the real MethodBuilderFactory for this test, not the mock one
        // Most mocks are set by default in SdkTestRunner setup
        MethodBuilderFactory.setInstance(new MethodBuilderFactory());
        verifyCleanSharedPreferences(context);
        GpsHelper.updateSharedPreferences(context, adInfo);
        verifySharedPreferences(context, adInfo);
    }

    @Test
    public void reflectedGetIsLimitAdTrackingEnabled_whenIsLimitAdTrackingEnabledIsSet_shouldReturnIsLimitAdTrackingEnabled() throws Exception {
        MethodBuilderFactory.setInstance(new MethodBuilderFactory());
        assertThat(GpsHelper.reflectedIsLimitAdTrackingEnabled(adInfo, false)).isEqualTo(adInfo.LIMIT_AD_TRACKING_ENABLED);
    }

    @Test
    public void reflectedGetIsLimitAdTrackingEnabled_whenReflectedMethodCallThrows_shouldReturnDefaultValue() throws Exception {
        when(methodBuilder.execute()).thenThrow(new Exception());
        assertThat(GpsHelper.reflectedIsLimitAdTrackingEnabled(new Object(), false)).isFalse();
        verify(methodBuilder).execute();
        assertThat(GpsHelper.reflectedIsLimitAdTrackingEnabled(new Object(), true)).isTrue();
        verify(methodBuilder, times(2)).execute();
    }

    @Test
    public void reflectedGetIsLimitAdTrackingEnabled_whenReflectedMethodCallReturnsNull_shouldReturnDefaultValue() throws Exception {
        when(methodBuilder.execute()).thenReturn(null);
        assertThat(GpsHelper.reflectedIsLimitAdTrackingEnabled(new Object(), false)).isFalse();
        verify(methodBuilder).execute();
        assertThat(GpsHelper.reflectedIsLimitAdTrackingEnabled(new Object(), true)).isTrue();
        verify(methodBuilder, times(2)).execute();
    }

    @Test
    public void reflectedGetAdvertisingId_whenAdvertisingIdIsSet_shouldReturnAdvertisingId() throws Exception {
        MethodBuilderFactory.setInstance(new MethodBuilderFactory());
        assertThat(GpsHelper.reflectedGetAdvertisingId(adInfo, null)).isEqualTo(adInfo.ADVERTISING_ID);
    }

    @Test
    public void reflectedGetAdvertisingId_whenReflectedMethodCallThrows_shouldReturnDefaultValue() throws Exception {
        when(methodBuilder.execute()).thenThrow(new Exception());
        assertThat(GpsHelper.reflectedGetAdvertisingId(new Object(), null)).isNull();
        verify(methodBuilder).execute();
        String defaultAdId = "TEST_DEFAULT";
        assertThat(GpsHelper.reflectedGetAdvertisingId(new Object(), defaultAdId)).isEqualTo(defaultAdId);
        verify(methodBuilder, times(2)).execute();
    }

    @Test
    public void getAdvertisingId_whenGooglePlayServicesIsLinkedAndAdvertisingIdIsCached_shouldReturnAdvertisingId() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putString(GpsHelper.ADVERTISING_ID_KEY, adInfo.ADVERTISING_ID)
                .commit();
        assertThat(GpsHelper.getAdvertisingId(context)).isEqualTo(adInfo.ADVERTISING_ID);
    }

    @Test
    public void getAdvertisingId_whenGooglePlayServicesIsLinkedAndAdInfoIsNotCached_shouldReturnNull() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);
        assertThat(GpsHelper.getAdvertisingId(context)).isNull();
    }

    @Test
    public void getAdvertisingId_whenGooglePlayServicesIsNotLinked_shouldReturnNull() throws Exception {
        assertThat(GpsHelper.getAdvertisingId(context)).isNull();
    }

    @Test
    public void isLimitAdTrackingEnabled_whenGooglePlayServicesIsLinkedAndLimitAdTrackingIsCached_shouldReturnLimitAdTracking() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putBoolean(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY, adInfo.LIMIT_AD_TRACKING_ENABLED)
                .commit();
        assertThat(GpsHelper.isLimitAdTrackingEnabled(context)).isEqualTo(adInfo.LIMIT_AD_TRACKING_ENABLED);
    }

    @Test
    public void isLimitAdTrackingEnabled_whenGooglePlayServicesIsLinkedAndAdInfoIsNotCached_shouldReturnFalse() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);
        assertThat(GpsHelper.isLimitAdTrackingEnabled(context)).isFalse();
    }

    @Test
    public void isLimitAdTrackingEnabled_whenGooglePlayServicesIsNotLinked_shouldReturnFalse() throws Exception {
        assertThat(GpsHelper.isLimitAdTrackingEnabled(context)).isFalse();
    }

    static public void populateAndVerifySharedPreferences(Context context, TestAdInfo adInfo) {
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putString(GpsHelper.ADVERTISING_ID_KEY, adInfo.ADVERTISING_ID)
                .putBoolean(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY, adInfo.LIMIT_AD_TRACKING_ENABLED)
                .commit();
        verifySharedPreferences(context, adInfo);
    }

    private void safeAcquireSemaphore() throws Exception {
        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
        semaphore.acquire();
    }

    static public void verifySharedPreferences(Context context, TestAdInfo adInfo) {
        SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(context);
        assertThat(sharedPreferences.contains(GpsHelper.ADVERTISING_ID_KEY)).isTrue();
        assertThat(sharedPreferences.getString(GpsHelper.ADVERTISING_ID_KEY, null)).isEqualTo(adInfo.ADVERTISING_ID);
        assertThat(sharedPreferences.contains(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY)).isTrue();
        assertThat(sharedPreferences.getBoolean(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY, false)).isEqualTo(adInfo.LIMIT_AD_TRACKING_ENABLED);
    }

    static public void verifyCleanSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(context);
        assertThat(sharedPreferences.contains(GpsHelper.ADVERTISING_ID_KEY)).isFalse();
        assertThat(sharedPreferences.contains(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY)).isFalse();
    }
}

