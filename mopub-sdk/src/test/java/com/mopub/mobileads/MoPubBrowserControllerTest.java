package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubBrowserControllerTest {
    private MoPubBrowserController subject;
    private MraidView mraidView;
    private Context context;
    private MraidView.MraidListener mraidListener;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        mraidView = new MraidView(context, null);
        mraidListener = mock(MraidView.MraidListener.class);
        mraidView.setMraidListener(mraidListener);

        subject = new MoPubBrowserController(mraidView);
    }

    @Test
    public void open_withApplicationUrl_shouldStartNewIntent() throws Exception {
        String applicationUrl = "amzn://blah";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(applicationUrl)), new ResolveInfo());

        subject.open(applicationUrl);

        Intent startedIntent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedIntent.getComponent()).isNull();
    }

    @Test
    public void open_withHttpApplicationUrl_shouldStartMoPubBrowser() throws Exception {
        String applicationUrl = "http://blah";

        subject.open(applicationUrl);

        Intent startedIntent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void open_withApplicationUrlThatCantBeHandled_shouldDefaultToMoPubBrowser() throws Exception {
        String applicationUrl = "canthandleme://blah";

        subject.open(applicationUrl);

        Intent startedIntent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedIntent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(applicationUrl);
    }

    @Test
    public void open_withHttpApplicationUrl_shouldCallMraidListenerOnOpenCallback() throws Exception {
        String applicationUrl = "http://blah";
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(applicationUrl)), new ResolveInfo());

        subject.open(applicationUrl);

        verify(mraidListener).onOpen(eq(mraidView));
    }

    @Test
    public void open_withApplicationUrl_shouldCallMraidListenerOnOpenCallback() throws Exception {
        String applicationUrl = "app://blah";

        subject.open(applicationUrl);

        verify(mraidListener).onOpen(eq(mraidView));
    }
}
