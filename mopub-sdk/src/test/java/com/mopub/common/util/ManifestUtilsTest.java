// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubFullscreenActivity;
import com.mopub.mobileads.MoPubFullscreenActivity;
import com.mopub.mobileads.MraidVideoPlayerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests largely do nothing now since manifest merging takes care of activity declarations.
 * However, it's still possible for publishers to bypass or override manifest merging, so we should
 * still check.
 */
@RunWith(RobolectricTestRunner.class)
public class ManifestUtilsTest {
    private Context context;
    private List<Class<? extends Activity>> requiredWebViewSdkActivities;
    private List<Class<? extends Activity>> requiredNativeSdkActivities;

    @Before
    public void setUp() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());

        requiredWebViewSdkActivities = ManifestUtils.getRequiredWebViewSdkActivities();
        requiredNativeSdkActivities = ManifestUtils.getRequiredNativeSdkActivities();

        setDebugMode(false);
        ShadowLog.clear();

        MoPubLog.setLogLevel(MoPubLog.LogLevel.DEBUG);
    }

    @After
    public void tearDown() throws Exception {
        setDebugMode(false);
        // This may have been set to a mock during testing. Reset this class back to normal
        ManifestUtils.setFlagCheckUtil(new ManifestUtils.FlagCheckUtil());

        addInterstitialModule();
    }

    @Test
    public void checkWebViewSdkActivitiesDeclared_shouldNotIncludeActivityDeclarations() throws Exception {
        ShadowLog.setupLogging();

        ManifestUtils.checkWebViewActivitiesDeclared(context);

        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser",
                "com.mopub.common.privacy.ConsentDialogActivity"
        );
    }

    @Test
    public void checkNativeSdkActivitiesDeclared_shouldNotIncludeActivityDeclarations() throws Exception {
        ShadowLog.setupLogging();

        ManifestUtils.checkNativeActivitiesDeclared(context);

        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser",
                "com.mopub.common.privacy.ConsentDialogActivity"
        );
    }

    @Test
    public void checkSdkActivitiesDeclared_shouldNotIncludeActivityDeclarations() throws Exception {
        ShadowLog.setupLogging();

        ManifestUtils.checkGdprActivitiesDeclared(context);

        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.RewardedMraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser",
                "com.mopub.common.privacy.ConsentDialogActivity"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withAllActivitiesDeclared_shouldNotShowLogOrToast() throws Exception {
        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());
        addActivityToShadowPackageManager(context, MraidVideoPlayerActivity.class.getName());
        addActivityToShadowPackageManager(context, MoPubBrowser.class.getName());

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @Test
    public void displayWarningForMissingActivities_withoutInterstitialModule_withoutInterstitialActivitiesDeclared_shouldNotShowLogOrToast() throws Exception {
        removeInterstitialModule();
        addActivityToShadowPackageManager(context, MraidVideoPlayerActivity.class.getName());
        addActivityToShadowPackageManager(context, MoPubBrowser.class.getName());

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @Test
     public void displayWarningForMissingActivities_withOneMissingActivity_shouldNotLogMessage() throws Exception {
        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());
        addActivityToShadowPackageManager(context, MraidVideoPlayerActivity.class.getName());
        // Here, we leave out MoPubBrowser on purpose

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.RewardedMraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser",
                "com.mopub.common.privacy.ConsentDialogActivity"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withAllMissingActivities_shouldNotLogMessage() throws Exception {
        setDebugMode(true);
        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.RewardedMraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser",
                "com.mopub.common.privacy.ConsentDialogActivity"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withMissingActivities_withDebugTrue_shouldNotShowToast() throws Exception {
        setDebugMode(true);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
    }

    @Test
    public void displayWarningForMissingActivities_withMissingActivities_withDebugFalse_shouldNotShowToast() throws Exception {
        setDebugMode(false);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void displayWarningForMisconfiguredActivities_withAllActivitiesConfigured_shouldNotLogOrShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(true);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());
        addActivityToShadowPackageManager(context, MraidVideoPlayerActivity.class.getName());
        addActivityToShadowPackageManager(context, MoPubBrowser.class.getName());

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withOneMisconfiguredActivity_shouldLogOnlyThatOne() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);

        // Misconfigure the first activity; only return false if the activity is MoPubActivity
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                return MoPubFullscreenActivity.class != args[0];
            }
        }).when(mockActivitiyConfigCheck).hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN));

        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(true);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());
        addActivityToShadowPackageManager(context, MraidVideoPlayerActivity.class.getName());
        addActivityToShadowPackageManager(context, MoPubBrowser.class.getName());

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertLogIncludes("com.mopub.mobileads.MoPubFullscreenActivity");
        assertLogIncludes("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include keyboardHidden.");
        assertLogDoesntInclude(
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.RewardedMraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser"
        );
        assertLogDoesntInclude("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include orientation.");
        assertLogDoesntInclude("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include screenSize.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void displayWarningForMisconfiguredActivities_withOneMisconfiguredActivity_withMissingAllConfigChangesValues_shouldLogAllConfigChangesValues() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);

        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertLogIncludes("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include keyboardHidden.");
        assertLogIncludes("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include orientation.");
        assertLogIncludes("The android:configChanges param for activity " + MoPubFullscreenActivity.class.getName() + " must include screenSize.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void displayWarningForMisconfiguredActivities_withMisconfiguredActivities_withDebugTrue_shouldShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());

        setDebugMode(true);

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNotNull();
        final String toastText = ShadowToast.getTextOfLatestToast();
        assertThat(toastText).isEqualTo("ERROR: YOUR MOPUB INTEGRATION IS INCOMPLETE.\nCheck logcat and update your AndroidManifest.xml with the correct activities and configuration.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void displayWarningForMisconfiguredActivities_withMisconfiguredActivities_withDebugFalse_shouldNotShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        addActivityToShadowPackageManager(context, MoPubFullscreenActivity.class.getName());

        setDebugMode(false);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
    }

    @Test
    public void isDebuggable_whenApplicationIsDebuggable_shouldReturnTrue() throws Exception {
        setDebugMode(true);

        assertThat(ManifestUtils.isDebuggable(context)).isTrue();
    }

    @Test
    public void isDebuggable_whenApplicationIsNotDebuggable_shouldReturnFalse() throws Exception {
        setDebugMode(false);

        assertThat(ManifestUtils.isDebuggable(context)).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getRequiredWebViewSdkActivities_shouldIncludeRequiredActivities() throws Exception {
        assertThat(requiredWebViewSdkActivities).containsOnly(
                MoPubFullscreenActivity.class,
                MoPubFullscreenActivity.class,
                MoPubBrowser.class
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getRequiredWebViewSdkActivities_withoutInterstitialModule_shouldNotHaveAllRequiredActivities() throws Exception {
        removeInterstitialModule();
        assertThat(requiredWebViewSdkActivities).containsOnly(
                MoPubBrowser.class
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getRequiredNativeSdkActivities_shouldIncludeRequiredActivities() throws Exception {
        assertThat(requiredNativeSdkActivities).containsOnly(
                MoPubBrowser.class
        );
    }

    private void addInterstitialModule() {
        Class moPubActivityClass = com.mopub.mobileads.MoPubFullscreenActivity.class;
        if (!ManifestUtils.getRequiredWebViewSdkActivities().contains(moPubActivityClass)) {
            ManifestUtils.getRequiredWebViewSdkActivities().add(moPubActivityClass);
        }
        Class fullscreenActivityClass = com.mopub.mobileads.MoPubFullscreenActivity.class;
        if (!ManifestUtils.getRequiredWebViewSdkActivities().contains(fullscreenActivityClass)) {
            ManifestUtils.getRequiredWebViewSdkActivities().add(fullscreenActivityClass);
        }
    }

    private void removeInterstitialModule() {
        Class moPubActivityClass = com.mopub.mobileads.MoPubFullscreenActivity.class;
        Class fullscreenActivityClass = com.mopub.mobileads.MoPubFullscreenActivity.class;
        ManifestUtils.getRequiredWebViewSdkActivities().remove(moPubActivityClass);
        ManifestUtils.getRequiredWebViewSdkActivities().remove(fullscreenActivityClass);
    }

    private void setDebugMode(boolean enabled) {
        final ApplicationInfo applicationInfo = context.getApplicationInfo();

        if (enabled) {
            applicationInfo.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        } else {
            applicationInfo.flags &= ~ApplicationInfo.FLAG_DEBUGGABLE;
        }

        when(context.getApplicationInfo()).thenReturn(applicationInfo);
    }

    private void assertLogIncludes(final String... messages) {
        final String logText = ShadowLog.getLogs().get(0).msg;
        for (final String message : messages) {
            assertThat(logText).containsOnlyOnce(message);
        }
    }

    private void assertLogDoesntInclude(final String... messages) {
        List<ShadowLog.LogItem> logs = ShadowLog.getLogs();
        if (logs.isEmpty()) {
            return;
        }
        final String logText = logs.get(0).msg;
        for (final String message : messages) {
            assertThat(logText).doesNotContain(message);
        }
    }

    private static void addActivityToShadowPackageManager(final Context context, final String activityName) {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.targetActivity = activityName;
        activityInfo.packageName = "com.mopub.mobileads.test";
        activityInfo.name = activityName;
        shadowOf(context.getPackageManager()).addOrUpdateActivity(activityInfo);
    }
}
