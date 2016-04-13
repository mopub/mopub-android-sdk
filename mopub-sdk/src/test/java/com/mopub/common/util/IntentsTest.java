package com.mopub.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import com.mopub.common.MoPubBrowser;
import com.mopub.exceptions.IntentNotResolvableException;
import com.mopub.exceptions.UrlParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(RobolectricTestRunner.class)
public class IntentsTest {
    private Activity activityContext;
    private Context applicationContext;

    @Before
    public void setUp() {
        activityContext = Robolectric.buildActivity(Activity.class).create().get();
        applicationContext = activityContext.getApplicationContext();
    }

    @Test
    public void startActivity_withActivityContext_shouldStartActivityWithNoNewFlags() throws IntentNotResolvableException {
        Intents.startActivity(activityContext, new Intent());

        final Intent intent = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isFalse();
    }

    @Test
    public void getStartActivityIntent_withActivityContext_shouldReturnIntentWithoutNewTaskFlag() throws Exception {
        Context context = new Activity();

        final Intent intent = Intents.getStartActivityIntent(context, MoPubBrowser.class, null);

        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubBrowser.class.getName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isFalse();
        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void getStartActivityIntent_withApplicationContext_shouldReturnIntentWithNewTaskFlag() throws Exception {
        Context context = new Activity().getApplicationContext();

        final Intent intent = Intents.getStartActivityIntent(context, MoPubBrowser.class, null);

        assertThat(intent.getComponent().getClassName()).isEqualTo(MoPubBrowser.class.getName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), FLAG_ACTIVITY_NEW_TASK)).isTrue();
        assertThat(intent.getExtras()).isNull();
    }

    @Test
    public void getStartActivityIntent_withBundle_shouldReturnIntentWithExtras() throws Exception {
        Context context = new Activity();
        Bundle bundle = new Bundle();
        bundle.putString("arbitrary key", "even more arbitrary value");

        final Intent intent = Intents.getStartActivityIntent(context, MoPubBrowser.class, bundle);

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

        assertThat(Intents.deviceCanHandleIntent(context, specificIntent)).isTrue();
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

        assertThat(Intents.deviceCanHandleIntent(context, otherIntent)).isFalse();
    }

    @Test
    public void intentForNativeBrowserScheme_shouldProperlyHandleEncodedUrls() throws UrlParseException {
        Intent intent;

        intent = Intents.intentForNativeBrowserScheme(Uri.parse("mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.example.com"));
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getDataString()).isEqualTo("https://www.example.com");

        intent = Intents.intentForNativeBrowserScheme(Uri.parse("mopubnativebrowser://navigate?url=https://www.example.com/?query=1&two=2"));
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getDataString()).isEqualTo("https://www.example.com/?query=1");

        intent = Intents.intentForNativeBrowserScheme(Uri.parse("mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.example.com%2F%3Fquery%3D1%26two%3D2"));
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getDataString()).isEqualTo("https://www.example.com/?query=1&two=2");
    }

    @Test(expected = UrlParseException.class)
    public void intentForNativeBrowserScheme_whenNotMoPubNativeBrowser_shouldThrowException() throws UrlParseException {
        Intents.intentForNativeBrowserScheme(Uri.parse("mailto://navigate?url=https://www.example.com"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForNativeBrowserScheme_whenNotNavigate_shouldThrowException() throws UrlParseException {
        Intents.intentForNativeBrowserScheme(Uri.parse("mopubnativebrowser://getout?url=https://www.example.com"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForNativeBrowserScheme_whenUrlParameterMissing_shouldThrowException() throws UrlParseException {
        Intents.intentForNativeBrowserScheme(Uri.parse("mopubnativebrowser://navigate"));
    }

    @Test
    public void intentForShareTweetScheme_whenValidUri_shouldReturnShareTweetIntent() throws UrlParseException {
        Intent intent;
        final String shareMessage = "Check out @SpaceX's Tweet: https://twitter.com/SpaceX/status/596026229536460802";

        intent = Intents.intentForShareTweet(Uri.parse("mopubshare://tweet?screen_name=SpaceX&tweet_id=596026229536460802"));
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_SEND);
        assertThat(intent.getType()).isEqualTo("text/plain");
        assertThat(intent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo(shareMessage);
        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo(shareMessage);
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenWrongScheme_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mailto://tweet?screen_name=SpaceX&tweet_id=596026229536460802"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenWrongHost_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mopubshare://twat?screen_name=SpaceX&tweet_id=596026229536460802"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenScreenNameParameterMissing_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mopubshare://tweet?foo=SpaceX&tweet_id=596026229536460802"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenScreenNameParameterIsEmpty_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mopubshare://tweet?screen_name=&tweet_id=596026229536460802"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenTweetIdParameterMissing_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mopubshare://tweet?screen_name=SpaceX&bar=596026229536460802"));
    }

    @Test(expected = UrlParseException.class)
    public void intentForShareTweetScheme_whenTweetIdParameterIsEmpty_shouldThrowException() throws UrlParseException {
        Intents.intentForShareTweet(Uri.parse("mopubshare://tweet?screen_name=SpaceX&tweet_id="));
    }

    @Test
    public void launchIntentForUserClick_shouldStartActivity() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get()
                .getApplicationContext();
        Intent intent = mock(Intent.class);

        Intents.launchIntentForUserClick(context, intent, null);
        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();

        assertThat(startedActivity).isNotNull();
    }
}
