package com.mopub.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import com.mopub.common.MoPubBrowser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(RobolectricTestRunner.class)
public class IntentUtilsTest {
    @Test
    public void getStartActivityIntent_withActivityContext_shouldReturnIntentWithoutNewTaskFlag() throws Exception {
        Context context = new Activity();

        final Intent intent = IntentUtils.getStartActivityIntent(context, MoPubBrowser.class, null);

        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubBrowser.class.getName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isFalse();
        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void getStartActivityIntent_withApplicationContext_shouldReturnIntentWithNewTaskFlag() throws Exception {
        Context context = new Activity().getApplicationContext();

        final Intent intent = IntentUtils.getStartActivityIntent(context, MoPubBrowser.class, null);

        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubBrowser.class.getName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isTrue();
        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void getStartActivityIntent_withBundle_shouldReturnIntentWithExtras() throws Exception {
        Context context = new Activity();
        Bundle bundle = new Bundle();
        bundle.putString("arbitrary key", "even more arbitrary value");

        final Intent intent = IntentUtils.getStartActivityIntent(context, MoPubBrowser.class, bundle);

        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubBrowser.class.getName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isFalse();
        assertThat(intent.getExtras()).isEqualTo(bundle);
    }

    @Test
    public void deviceCanHandleIntent_whenActivityCanResolveIntent_shouldReturnTrue() throws Exception {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        resolveInfos.add(new ResolveInfo());

        stub(context.getPackageManager()).toReturn(packageManager);
        Intent specificIntent = new Intent();
        specificIntent.setData(Uri.parse("specificIntent:"));

        stub(packageManager.queryIntentActivities(eq(specificIntent), eq(0))).toReturn(resolveInfos);

        assertThat(IntentUtils.deviceCanHandleIntent(context, specificIntent)).isTrue();
    }

    @Test
    public void deviceCanHandleIntent_whenActivityCanNotResolveIntent_shouldReturnFalse() throws Exception {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        resolveInfos.add(new ResolveInfo());

        stub(context.getPackageManager()).toReturn(packageManager);
        Intent specificIntent = new Intent();
        specificIntent.setData(Uri.parse("specificIntent:"));

        Intent otherIntent = new Intent();
        otherIntent.setData(Uri.parse("other:"));
        stub(packageManager.queryIntentActivities(eq(specificIntent), eq(0))).toReturn(resolveInfos);

        assertThat(IntentUtils.deviceCanHandleIntent(context, otherIntent)).isFalse();
    }

    @Test
    public void generateUniqueId_withMultipleInvocations_shouldReturnUniqueValues() throws Exception {
        final int expectedIdCount = 100;

        Set<Long> ids = new HashSet<Long>(expectedIdCount);
        for (int i = 0; i < expectedIdCount; i++) {
            final long id = Utils.generateUniqueId();
            ids.add(id);
        }

        assertThat(ids).hasSize(expectedIdCount);
    }
}
