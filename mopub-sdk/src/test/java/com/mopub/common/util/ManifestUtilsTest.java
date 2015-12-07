package com.mopub.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.mopub.TestSdkHelper;
import com.mopub.common.MoPubBrowser;
import com.mopub.mobileads.MoPubActivity;
import com.mopub.mobileads.MraidActivity;
import com.mopub.mobileads.MraidVideoPlayerActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
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

@RunWith(RobolectricTestRunner.class)
public class ManifestUtilsTest {
    private Context context;
    private List<Class<? extends Activity>> requiredWebViewSdkActivities;
    private List<Class<? extends Activity>> requiredNativeSdkActivities;

    @Mock private ResolveInfo mockResolveInfo;

    @Before
    public void setUp() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());

        requiredWebViewSdkActivities = ManifestUtils.getRequiredWebViewSdkActivities();
        requiredNativeSdkActivities = ManifestUtils.getRequiredNativeSdkActivities();

        setDebugMode(false);
    }

    @After
    public void tearDown() throws Exception {
        setDebugMode(false);
        // This may have been set to a mock during testing. Reset this class back to normal
        ManifestUtils.setFlagCheckUtil(new ManifestUtils.FlagCheckUtil());
    }

    @Test
    public void checkWebViewSdkActivitiesDeclared_shouldIncludeFourActivityDeclarations() throws Exception {
        ShadowLog.setupLogging();

        ManifestUtils.checkWebViewActivitiesDeclared(context);

        assertLogIncludes(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser"
        );
    }

    @Test
    public void checkNativeSdkActivitiesDeclared_shouldIncludeOneActivityDeclaration() throws Exception {
        ShadowLog.setupLogging();

        ManifestUtils.checkNativeActivitiesDeclared(context);

        assertLogIncludes("com.mopub.common.MoPubBrowser");
        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withAllActivitiesDeclared_shouldNotShowLogOrToast() throws Exception {
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidVideoPlayerActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubBrowser.class), mockResolveInfo);

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @Test
     public void displayWarningForMissingActivities_withOneMissingActivity_shouldLogOnlyThatOne() throws Exception {
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidVideoPlayerActivity.class), mockResolveInfo);
        // Here, we leave out MoPubBrowser on purpose

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertLogIncludes("com.mopub.common.MoPubBrowser");
        assertLogDoesntInclude(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withAllMissingActivities_shouldLogMessage() throws Exception {
        setDebugMode(true);
        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        final List<ShadowLog.LogItem> logs = ShadowLog.getLogs();

        assertLogIncludes(
                "com.mopub.mobileads.MoPubActivity",
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser"
        );
    }

    @Test
    public void displayWarningForMissingActivities_withMissingActivities_withDebugTrue_shouldShowToast() throws Exception {
        setDebugMode(true);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNotNull();
        final String toastText = ShadowToast.getTextOfLatestToast();
        assertThat(toastText).isEqualTo("ERROR: YOUR MOPUB INTEGRATION IS INCOMPLETE.\nCheck logcat and update your AndroidManifest.xml with the correct activities and configuration.");
    }

    @Test
    public void displayWarningForMissingActivities_withMissingActivities_withDebugFalse_shouldNotShowToast() throws Exception {
        setDebugMode(false);

        ManifestUtils.displayWarningForMissingActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withAllActivitiesConfigured_shouldNotLogOrShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(true);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidVideoPlayerActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubBrowser.class), mockResolveInfo);

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
                return MoPubActivity.class != args[0];
            }
        }).when(mockActivitiyConfigCheck).hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN));

        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(true);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MraidVideoPlayerActivity.class), mockResolveInfo);
        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubBrowser.class), mockResolveInfo);

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertLogIncludes("com.mopub.mobileads.MoPubActivity");
        assertLogIncludes("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include keyboardHidden.");
        assertLogDoesntInclude(
                "com.mopub.mobileads.MraidActivity",
                "com.mopub.mobileads.MraidVideoPlayerActivity",
                "com.mopub.common.MoPubBrowser"
        );
        assertLogDoesntInclude("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include orientation.");
        assertLogDoesntInclude("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include screenSize.");
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withOneMisconfiguredActivity_withMissingAllConfigChangesValues_shouldLogAllConfigChangesValues() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);

        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);

        ShadowLog.setupLogging();

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertLogIncludes("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include keyboardHidden.");
        assertLogIncludes("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include orientation.");
        assertLogIncludes("The android:configChanges param for activity " + MoPubActivity.class.getName() + " must include screenSize.");
    }

    @SuppressWarnings("unchecked")

    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withMissingScreenSize_withApiLessThan13_shouldNotLogOrShowToast() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.HONEYCOMB);
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withMissingScreenSize_withTargetApiLessThan13_shouldNotLogOrShowToast() throws Exception {
        // Set target API to < 13
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        applicationInfo.targetSdkVersion = Build.VERSION_CODES.HONEYCOMB_MR1;
        when(context.getApplicationInfo()).thenReturn(applicationInfo);

        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(true);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);

        ShadowLog.setupLogging();
        setDebugMode(true);

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNull();
        assertThat(ShadowLog.getLogs()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withMisconfiguredActivities_withDebugTrue_shouldShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);

        setDebugMode(true);

        ManifestUtils.displayWarningForMisconfiguredActivities(context, requiredWebViewSdkActivities);

        assertThat(ShadowToast.getLatestToast()).isNotNull();
        final String toastText = ShadowToast.getTextOfLatestToast();
        assertThat(toastText).isEqualTo("ERROR: YOUR MOPUB INTEGRATION IS INCOMPLETE.\nCheck logcat and update your AndroidManifest.xml with the correct activities and configuration.");
    }

    @SuppressWarnings("unchecked")
    @TargetApi(13)
    @Test
    public void displayWarningForMisconfiguredActivities_withMisconfiguredActivities_withDebugFalse_shouldNotShowToast() throws Exception {
        ManifestUtils.FlagCheckUtil mockActivitiyConfigCheck = mock(ManifestUtils.FlagCheckUtil.class);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_KEYBOARD_HIDDEN))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_ORIENTATION))).thenReturn(false);
        when(mockActivitiyConfigCheck.hasFlag(any(Class.class), anyInt(), eq(ActivityInfo.CONFIG_SCREEN_SIZE))).thenReturn(false);
        ManifestUtils.setFlagCheckUtil(mockActivitiyConfigCheck);

        RuntimeEnvironment.getRobolectricPackageManager().addResolveInfoForIntent(new Intent(context, MoPubActivity.class), mockResolveInfo);

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
                MoPubActivity.class,
                MraidActivity.class,
                MraidVideoPlayerActivity.class,
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
        final String logText = ShadowLog.getLogs().get(0).msg;
        for (final String message : messages) {
            assertThat(logText).doesNotContain(message);
        }
    }
}
