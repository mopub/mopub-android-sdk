package com.mopub.common.privacy;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.mopub.common.GpsHelper;
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
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(GpsHelper.class)
public class MoPubIdentifierTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private MoPubIdentifier.AdvertisingIdChangeListener idChangeListener;

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
    public void constructor_nonFirstStart_shouldNotStartRefreshThread_shouldReadSharedPref() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, true);

        subject = new MoPubIdentifier(context);

        AdvertisingId idData = subject.getAdvertisingInfo();
        assertThat(idData.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(idData.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(idData.isRotationRequired()).isEqualTo(savedId.isRotationRequired());
        assertThat(idData.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_noAmazonNoGoogle_expiredId_shouldCallListener() throws Exception {
        AdvertisingId savedId = writeExpiredAdvertisingInfoToSharedPreferences(context, false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);
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
    public void refreshAdvertisingInfoBackgroundThread_expiredId_listenerNotSet_shouldNotCrash() throws Exception {
        AdvertisingId savedId = writeExpiredAdvertisingInfoToSharedPreferences(context, true);

        subject = new MoPubIdentifier(context);
        subject.setIdChangeListener(null);
        subject.refreshAdvertisingInfoBackgroundThread();
        verify(idChangeListener, never()).onIdChanged(any(AdvertisingId.class), any(AdvertisingId.class));

        AdvertisingId newId = subject.getAdvertisingInfo();

        assertThat(newId.mMopubId).isNotEqualTo(savedId.mMopubId);
        assertThat(newId.mAdvertisingId).isNotEqualTo(savedId.mAdvertisingId);
        assertThat(newId.isDoNotTrack()).isFalse();
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_noAmazonNoGoogle_nonExpiredId_shouldCallListenerOnlyOnce() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);

        subject = new MoPubIdentifier(context, idChangeListener);
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
    public void refreshAdvertisingInfoBackgroundThread_amazonNoGoogle_shoulUseAmazonId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);
        setupAmazonAdvertisingInfo(false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);
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
    public void refreshAdvertisingInfoBackgroundThread_googleNoAmazon_doNotTrackFalse_shoulUseGoogleId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);
        setupGooglePlayService(context, false);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);
        verify(idChangeListener).onIdChanged(oldIdClientCaptor.capture(), newIdClientCaptor.capture());

        AdvertisingId oldId = oldIdClientCaptor.getValue();
        AdvertisingId newId = newIdClientCaptor.getValue();

        assertThat(oldId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(oldId.mAdvertisingId).isEqualTo(savedId.mAdvertisingId);
        assertThat(oldId.isDoNotTrack()).isEqualTo(savedId.isDoNotTrack());
        assertThat(oldId.getIdWithPrefix(true)).isEqualTo(savedId.getIdWithPrefix(true));

        assertThat(newId.isDoNotTrack()).isFalse();
        assertThat(newId.mAdvertisingId).isEqualTo(GOOGLE_AD_ID);
        assertThat(newId.mMopubId).isEqualTo(savedId.mMopubId);
        assertThat(newId.getIdWithPrefix(true)).isEqualTo("ifa:" + GOOGLE_AD_ID);
    }

    @Test
    public void refreshAdvertisingInfoBackgroundThread_googleNoAmazon_doNotTrackTrue_shoulUseGoogleId() throws Exception {
        AdvertisingId savedId = writeAdvertisingInfoToSharedPreferences(context, false);
        setupGooglePlayService(context, true);

        ArgumentCaptor<AdvertisingId> oldIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);
        ArgumentCaptor<AdvertisingId> newIdClientCaptor = ArgumentCaptor.forClass(AdvertisingId.class);

        subject = new MoPubIdentifier(context, idChangeListener);
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
    public void sharedPreferences_WriteAndRead_ShouldMatch() throws Exception {
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
        AdvertisingId adConfig2 = (AdvertisingId)  new Reflection.MethodBuilder(null, "readIdFromStorage")
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
        final long time = Calendar.getInstance().getTimeInMillis()-AdvertisingId.ROTATION_TIME_MS;
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
