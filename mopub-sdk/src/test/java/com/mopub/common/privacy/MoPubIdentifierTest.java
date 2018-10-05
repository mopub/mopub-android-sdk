// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.mopub.common.GpsHelper;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.util.concurrent.RoboExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Calendar;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(GpsHelper.class)
public class MoPubIdentifierTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private MoPubIdentifier.AdvertisingIdChangeListener idChangeListener;
    private SdkInitializationListener initializationListener;

    private Context context;
    private MoPubIdentifier subject;

    private static final String GOOGLE_AD_ID = "google_ad_id";
    private static final String AMAZON_AD_ID = "amazon_ad_id";
    private static final String TEST_IFA_ID = "test_ifa_id";
    public static final String TEST_MOPUB_ID = "test_mopub_id";

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        context = activity.getApplicationContext();
        idChangeListener = mock(MoPubIdentifier.AdvertisingIdChangeListener.class);
        initializationListener = mock(SdkInitializationListener.class);
        AsyncTasks.setExecutor(new RoboExecutorService());
    }

    @After
    public void tearDown() {
        // delete changes made by setupAmazonAdvertisingInfo
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putString(resolver, "limit_ad_tracking", null);
        Settings.Secure.putString(resolver, "advertising_id", null);
        // clear shared preferences
        MoPubIdentifier.clearStorage(context);
    }

    @Test
    public void constructor_withNotExpiredOldId_withNoAmazon_withNoGoogle_shouldReadSharedPref() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, true);

        subject = new MoPubIdentifier(context);

        AdvertisingId idData = subject.getAdvertisingInfo();
        assertThat(idData.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(idData.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(idData.isRotationRequired()).isEqualTo(savedId.isRotationRequired());
        assertThat(idData.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
    }

    @Test
    public void constructor_withExpiredOldId_withNoAmazon_withNoGoogle_shouldCallOnIdChanged() throws Exception {
        AdvertisingId savedId = writeExpiredAdvertisingInfoToSharedPreferences(context, false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());

        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();

        assertThat(oldId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(oldId.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(oldId.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());

        assertThat(newId.isDoNotTrack()).isFalse();
        assertThat(newId.mAdvertisingId).isEmpty();
        assertThat(newId.getIdWithPrefix(false)).contains("mopub:");
    }

    @Test
    public void constructor_withNotExpiredOldid_withNoAmazon_withNoGoogle_shouldCallOnIdChanngedOnlyOnce() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);

        subject = new MoPubIdentifier(context, idChangeListener);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(any(AdvertisingId.class), any(AdvertisingId.class));

        AdvertisingId idData = subject.getAdvertisingInfo();
        assertThat(idData.getIdWithPrefix(true)).contains("ifa:");
        assertThat(idData.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(idData.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(idData.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());

        reset(idChangeListener);
        subject.refreshAdvertisingInfoBackgroundThread();
        verify(idChangeListener, never()).onIdChanged(any(AdvertisingId.class), any(AdvertisingId.class));
    }

    @Test
    public void constructor_withExpiredId_withNoListenerSet_shouldNotCrash_shouldRotateMopubId() throws Exception {
        AdvertisingId savedId = writeExpiredAdvertisingInfoToSharedPreferences(context, true);

        subject = new MoPubIdentifier(context);
        ShadowLooper.runUiThreadTasks();
        subject.setIdChangeListener(null);
        AdvertisingId newId = subject.getAdvertisingInfo();

        assertThat(newId.mMopubId).isNotEqualTo(savedId.mMopubId);
        assertThat(newId.mAdvertisingId).isNotEqualTo(savedId.mAdvertisingId);
        assertThat(newId.isDoNotTrack()).isFalse();
        assertThat(newId.isRotationRequired()).isFalse();
    }

    @Test
    public void constructor_withGoogle_withNoAmazon_withDoNotTrackTrue_shoulUseGoogleId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);
        setupGooglePlayService(context, true);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());

        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();

        assertThat(oldId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(oldId.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(oldId.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
        assertThat(oldId.getIdWithPrefix(true)).isEqualTo(savedId.getIdWithPrefix(true));

        assertThat(newId.isDoNotTrack()).isTrue();
        assertThat(newId.mAdvertisingId).isEqualTo(GOOGLE_AD_ID);
        assertThat(newId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(newId.getIdWithPrefix(true)).isEqualTo("mopub:" + savedId.mMopubId);
    }

    @Test
    public void constructor_withAmazon_withNoGoogle_shoulUseAmazonId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);
        setupAmazonAdvertisingInfo(false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());

        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();

        assertThat(oldId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(oldId.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(oldId.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
        assertThat(oldId.getIdWithPrefix(true)).isEqualTo(savedId.getIdWithPrefix(true));

        assertThat(newId.isDoNotTrack()).isFalse();
        assertThat(newId.mAdvertisingId).isEqualTo(AMAZON_AD_ID);
        assertThat(newId.mMopubId).isEqualTo(savedId.mMopubId);
    }

    @Test
    public void constructor_withGoogle_withNoAmazon_withDoNotTrackFalse_shoulUseGoogleId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, true);
        setupGooglePlayService(context, false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());

        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();

        // verify that oldId is from SharedPreferences
        assertThat(oldId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(oldId.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(oldId.isDoNotTrack()).isTrue();
        assertThat(oldId.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
        assertThat(oldId.getIdWithPrefix(true)).isEqualTo(savedId.getIdWithPrefix(true));
        // verify that newId is from Google Play Services
        assertThat(newId.isDoNotTrack()).isFalse();
        assertThat(newId.mAdvertisingId).isEqualTo(GOOGLE_AD_ID);
        assertThat(newId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(newId.getIdWithPrefix(true)).isEqualTo("ifa:" + GOOGLE_AD_ID);
    }

    @Test
    public void sharedPreferences_WriteAndReadAdvertisingId_shouldMatch() throws Exception {
        final long time = Calendar.getInstance().getTimeInMillis();
        AdvertisingId adConfig = new AdvertisingId(TEST_IFA_ID,
                TEST_MOPUB_ID,
                true,
                time);

        // save to shared preferences
        new Reflection.MethodBuilder(null, "writeIdToStorage")
                .setAccessible()
                .setStatic(MoPubIdentifier.class)
                .addParam(Context.class, context)
                .addParam(AdvertisingId.class, adConfig)
                .execute();

        // read from shared preferences
        AdvertisingId adConfig2 = (AdvertisingId) new Reflection.MethodBuilder(null, "readIdFromStorage")
                .setAccessible()
                .setStatic(MoPubIdentifier.class)
                .addParam(Context.class, context)
                .execute();

        assert null != adConfig2;
        assertThat(adConfig2.mAdvertisingId).isEqualTo(TEST_IFA_ID);
        assertThat(adConfig2.mMopubId).isEqualTo(TEST_MOPUB_ID);
        assertThat(adConfig2.mDoNotTrack).isTrue();
        assertThat(adConfig2.mLastRotation.getTimeInMillis()).isEqualTo(time);
    }

    @Test
    public void isPlayServiceAvailable_whenGoogleAvailable_shouldCallGpsHelper_shouldReturnTrue() {
        subject = new MoPubIdentifier(context, idChangeListener);
        assertThat(subject.isPlayServicesAvailable()).isFalse();

        setupGooglePlayService(context, false);

        assertThat(subject.isPlayServicesAvailable()).isTrue();
        verifyStatic();
        GpsHelper.isPlayServicesAvailable(any(Context.class));
    }

    @Test
    public void setAdvertisingInfo_whenCalledTwice_shouldCallInitializationListenerOnce_validateSavedAdvertisingIds() throws Exception {
        final AdvertisingId adId1 = new AdvertisingId("ifa1", "mopub1", false, Calendar.getInstance().getTimeInMillis());
        final AdvertisingId adId2 = new AdvertisingId("ifa2", "mopub2", false, Calendar.getInstance().getTimeInMillis());

        writeAdvertisingInfoToSharedPreferences(context, false);
        subject = new MoPubIdentifier(context);
        subject.setIdChangeListener(idChangeListener);
        subject.setInitializationListener(initializationListener);

        subject.setAdvertisingInfo(adId1);

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(any(AdvertisingId.class), any(AdvertisingId.class));
        verify(initializationListener).onInitializationFinished();
        AdvertisingId storedId = MoPubIdentifier.readIdFromStorage(context);
        assertThat(adId1.equals(storedId)).isTrue();

        reset(initializationListener);
        reset(idChangeListener);

        // call setAdvertisingInfo second time
        subject.setAdvertisingInfo(adId2);

        verify(idChangeListener).onIdChanged(adId1, adId2);
        verify(initializationListener, never()).onInitializationFinished();
        assertThat(adId2.equals(MoPubIdentifier.readIdFromStorage(context))).isTrue();
    }

    @Test
    public void rotateMopubId_withExpiredOldId_shouldRotateMoPubId() {
        subject = new MoPubIdentifier(context);
        AdvertisingId originalId = AdvertisingId.generateExpiredAdvertisingId();
        subject.setAdvertisingInfo(originalId);
        subject.setIdChangeListener(idChangeListener);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject.rotateMopubId();

        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(newId.isRotationRequired()).isFalse();
        assertThat(originalId.equals(oldId)).isTrue();
    }

    @Test
    public void rotateMopubId_withNotExpiredOldId_shouldNotRotateMoPubId() {
        subject = new MoPubIdentifier(context);
        AdvertisingId oldId = AdvertisingId.generateFreshAdvertisingId();
        subject.setAdvertisingInfo(oldId);
        subject.setIdChangeListener(idChangeListener);

        subject.rotateMopubId();

        verify(idChangeListener, never()).onIdChanged(any(AdvertisingId.class), any(AdvertisingId.class));
        AdvertisingId newId = subject.getAdvertisingInfo();
        assertThat(newId.isRotationRequired()).isFalse();
        assertThat(oldId.equals(newId)).isTrue();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_withExpiredId_withGoogle_withNoAmazon_shouldRotateMoPubId() {
        AdvertisingId expiredId = AdvertisingId.generateExpiredAdvertisingId();
        subject = new MoPubIdentifier(context);
        setupGooglePlayService(context, true);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        subject.setAdvertisingInfo(expiredId);
        subject.setIdChangeListener(idChangeListener);

        subject.refreshAdvertisingInfoBackgroundThread();

        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(oldId.equals(expiredId)).isTrue();
        assertThat(oldId.mMopubId.equals(newId.mMopubId)).isFalse(); // rotation
        assertThat(newId.mAdvertisingId.equals(GOOGLE_AD_ID)).isTrue();
        assertThat(newId.mDoNotTrack).isTrue();
        assertThat(newId.isRotationRequired()).isFalse();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_withExpiredId_withNoGoogle_withNoAmazon_shouldRotateMoPubId() {
        AdvertisingId originalId = AdvertisingId.generateExpiredAdvertisingId();
        subject = new MoPubIdentifier(context);
        subject.setAdvertisingInfo(originalId);
        subject.setIdChangeListener(idChangeListener);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject.refreshAdvertisingInfoBackgroundThread();

        ShadowLooper.runUiThreadTasks();
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(newId.isRotationRequired()).isFalse();
        assertThat(originalId.equals(oldId)).isTrue();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_withNotExpiredId_withGoogle_withNoAmazon_shouldNotRotateMoPubId() {
        AdvertisingId freshId = AdvertisingId.generateFreshAdvertisingId();
        subject = new MoPubIdentifier(context);
        setupGooglePlayService(context, true);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        subject.setAdvertisingInfo(freshId);
        subject.setIdChangeListener(idChangeListener);

        subject.refreshAdvertisingInfoBackgroundThread();

        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(oldId.equals(freshId)).isTrue();
        assertThat(oldId.mMopubId.equals(newId.mMopubId)).isTrue(); // no rotation
        assertThat(newId.mAdvertisingId.equals(GOOGLE_AD_ID)).isTrue();
        assertThat(newId.mDoNotTrack).isTrue();
        assertThat(newId.isRotationRequired()).isFalse();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_withExpiredId_withAmazon_withNoGoogle_shouldRotateMoPubId() {
        AdvertisingId expiredId = AdvertisingId.generateExpiredAdvertisingId();
        subject = new MoPubIdentifier(context);
        setupAmazonAdvertisingInfo(true);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        subject.setAdvertisingInfo(expiredId);
        subject.setIdChangeListener(idChangeListener);

        subject.refreshAdvertisingInfoBackgroundThread();

        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(oldId.equals(expiredId)).isTrue();
        assertThat(oldId.mMopubId.equals(newId.mMopubId)).isFalse(); // rotation
        assertThat(newId.mAdvertisingId.equals(AMAZON_AD_ID)).isTrue();
        assertThat(newId.mDoNotTrack).isTrue();
        assertThat(newId.isRotationRequired()).isFalse();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_withNotExpiredId_withAmazon_withNoGoogle_shouldNotRotateMoPubId() {
        AdvertisingId freshId = AdvertisingId.generateFreshAdvertisingId();
        subject = new MoPubIdentifier(context);
        setupAmazonAdvertisingInfo(true);
        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        subject.setAdvertisingInfo(freshId);
        subject.setIdChangeListener(idChangeListener);

        subject.refreshAdvertisingInfoBackgroundThread();

        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());
        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();
        assertThat(oldId.equals(freshId)).isTrue();
        assertThat(oldId.mMopubId.equals(newId.mMopubId)).isTrue(); // no rotation
        assertThat(newId.mAdvertisingId.equals(AMAZON_AD_ID)).isTrue();
        assertThat(newId.mDoNotTrack).isTrue();
        assertThat(newId.isRotationRequired()).isFalse();
    }

    // Unit tests utility functions
    public static void setupGooglePlayService(Context context, boolean limitAdTracking) {
        PowerMockito.mockStatic(GpsHelper.class);
        PowerMockito.when(GpsHelper.isPlayServicesAvailable(context)).thenReturn(true);
        PowerMockito.when(GpsHelper.isLimitAdTrackingEnabled(context)).thenReturn(limitAdTracking);
        PowerMockito.when(GpsHelper.fetchAdvertisingInfoSync(context)).thenReturn(new GpsHelper.AdvertisingInfo(GOOGLE_AD_ID, limitAdTracking));
    }

    public static void setupAmazonAdvertisingInfo(boolean limitAdTracking) {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putInt(resolver, "limit_ad_tracking", limitAdTracking ? 1 : 0);
        Settings.Secure.putString(resolver, "advertising_id", AMAZON_AD_ID);
    }

    // might be useful in other unit tests
    public static void clearPreferences(@NonNull final Context context) {
        try {
            // clear shared preferences between tests
            new Reflection.MethodBuilder(null, "clearStorage")
                    .setAccessible()
                    .setStatic(MoPubIdentifier.class)
                    .addParam(Context.class, context)
                    .execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AdvertisingId writeAdvertisingInfoToSharedPreferences(Context context, boolean doNotTrack) throws Exception {
        final long time = Calendar.getInstance().getTimeInMillis();
        return writeAdvertisingInfoToSharedPreferences(context, doNotTrack, time);
    }

    private static AdvertisingId writeExpiredAdvertisingInfoToSharedPreferences(Context context, boolean doNotTrack) throws Exception {
        final long time = Calendar.getInstance().getTimeInMillis() - AdvertisingId.ROTATION_TIME_MS;
        return writeAdvertisingInfoToSharedPreferences(context, doNotTrack, time);
    }

    private static AdvertisingId writeAdvertisingInfoToSharedPreferences(Context context, boolean doNotTrack, long time) throws Exception {
        AdvertisingId adConfig = new AdvertisingId(TEST_IFA_ID,
                TEST_MOPUB_ID,
                doNotTrack,
                time);

        // save to shared preferences
        new Reflection.MethodBuilder(null, "writeIdToStorage")
                .setAccessible()
                .setStatic(MoPubIdentifier.class)
                .addParam(Context.class, context)
                .addParam(AdvertisingId.class, adConfig)
                .execute();
        return adConfig;
    }
}
