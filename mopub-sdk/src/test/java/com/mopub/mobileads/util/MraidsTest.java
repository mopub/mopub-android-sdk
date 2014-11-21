package com.mopub.mobileads.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;

import com.mopub.common.test.support.SdkTestRunner;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.common.util.VersionCode.HONEYCOMB_MR2;
import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
public class MraidsTest {
    Context context;

    @Before
    public void setup() {
        context = new Activity();
    }

    @Test
    public void isTelAvailable_whenCanAcceptIntent_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData("tel", null, null, "android.intent.action.DIAL");

        assertThat(Mraids.isTelAvailable(context)).isTrue();
    }

    @Test
    public void isTelAvailable_whenCanNotAcceptIntent_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.DIAL");

        assertThat(Mraids.isTelAvailable(context)).isFalse();
    }

    @Test
    public void isSmsAvailable_whenCanAcceptIntent_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData("sms", null, null, "android.intent.action.VIEW");

        assertThat(Mraids.isSmsAvailable(context)).isTrue();
    }

    @Test
    public void isSmsAvailable_whenCanNotAcceptIntent_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.VIEW");

        assertThat(Mraids.isSmsAvailable(context)).isFalse();
    }

    @Test
    public void isStorePictureAvailable_whenPermissionDeclaredAndMediaMounted_shouldReturnTrue() throws Exception {
        Robolectric.getShadowApplication().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

        assertThat(Mraids.isStorePictureSupported(context)).isTrue();
    }

    @Test
    public void isStorePictureAvailable_whenPermissionDenied_shouldReturnFalse() throws Exception {
        Robolectric.getShadowApplication().denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

        assertThat(Mraids.isStorePictureSupported(context)).isFalse();
    }

    @Test
    public void isStorePictureAvailable_whenMediaUnmounted_shouldReturnFalse() throws Exception {
        Robolectric.getShadowApplication().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED);

        assertThat(Mraids.isStorePictureSupported(context)).isFalse();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void isCalendarAvailable_atLeastIcs_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData(null, null, Mraids.ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");
        assertThat(Mraids.isCalendarAvailable(context)).isTrue();
    }

    @Config(reportSdk = VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void isCalendarAvailable_beforeIcs_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData(null, null, Mraids.ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");
        assertThat(Mraids.isCalendarAvailable(context)).isFalse();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void isCalendarAvailable_atLeastIcs_butCanNotAcceptIntent_shouldReturnFalse() throws
            Exception {
        context = createMockContextWithSpecificIntentData(null, null, "vnd.android.cursor.item/NOPE", "android.intent.action.INSERT");
        assertThat(Mraids.isCalendarAvailable(context)).isFalse();
    }

    @Test
    public void isInlineVideoAvailable_whenCanAcceptMraidVideoPlayerActivityIntent_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData(null, "com.mopub.mobileads.MraidVideoPlayerActivity", null, null);

        assertThat(Mraids.isInlineVideoAvailable(context)).isTrue();
    }

    @Test
    public void isInlineVideoAvailable_whenCanNotAcceptMraidVideoPlayerActivityIntent_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData(null, "com.mopub.mobileads.DO_NOT_ACCEPT", null, null);

        assertThat(Mraids.isInlineVideoAvailable(context)).isFalse();
    }

    public static Context createMockContextWithSpecificIntentData(final String scheme, final String componentName, final String type, final String action) {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        resolveInfos.add(new ResolveInfo());

        stub(context.getPackageManager()).toReturn(packageManager);

        BaseMatcher intentWithSpecificData = new BaseMatcher() {
            // check that the specific intent has the special data, i.e. "tel:", or a component name, or string type, based on a particular data

            @Override
            public boolean matches(Object item) {
                if (item != null && item instanceof Intent ){
                    boolean result = action != null || type != null || componentName != null || scheme != null;
                    if (action != null) {
                        if (((Intent) item).getAction() != null) {
                            result = result && action.equals(((Intent) item).getAction());
                        }
                    }

                    if (type != null) {
                        if (((Intent) item).getType() != null) {
                            result = result && type.equals(((Intent) item).getType());
                        }
                    }

                    if (componentName != null) {
                        if (((Intent) item).getComponent() != null) {
                            result = result && componentName.equals(((Intent) item).getComponent().getClassName());
                        }
                    }

                    if (scheme != null) {
                        if (((Intent) item).getData() != null) {
                            result = result && scheme.equals(((Intent) item).getData().getScheme());
                        }
                    }
                    return result;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {

            }
        };

        // It is okay to query with specific intent or nothing, because by default, none of the query would normally any resolveInfo anyways
        stub(packageManager.queryIntentActivities((Intent) argThat(intentWithSpecificData), eq(0))).toReturn(resolveInfos);
        return context;
    }
}
