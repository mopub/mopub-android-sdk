package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.MraidVideoViewController;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidVideoPlayerActivityTest {
    private static final String VAST = "vast";
    private static final String MRAID = "mraid";

    private MraidVideoPlayerActivity subject;
    private long testBroadcastIdentifier;
    private Intent intent;
    private Context context;
    private BaseVideoViewController baseVideoViewController;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        intent = new Intent(context, MraidVideoPlayerActivity.class);

        testBroadcastIdentifier = 1001;
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, testBroadcastIdentifier);
        baseVideoViewController = mock(BaseVideoViewController.class);
    }

    @Test
    public void onCreate_withVastExtraKey_shouldUseVastVideoViewController() throws Exception {
        initializeSubjectForVast();

        assertThat(subject.getBaseVideoViewController()).isInstanceOf(VastVideoViewController.class);
    }

    @Test
    public void onCreate_withMraidExtraKey_shouldUseMraidVideoViewController() throws Exception {
        initializeSubjectForMraid();

        assertThat(subject.getBaseVideoViewController()).isInstanceOf(MraidVideoViewController.class);
    }

    @Ignore("pending: this is currently impossible to write")
    @Test
    public void onCreate_shouldForwardOnCreateToViewController() throws Exception {
        initializeSubjectWithMockViewController();

    }

    @Test
    public void onPause_shouldForwardOnPauseToViewController() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onPause();

        verify(baseVideoViewController).onPause();
    }

    @Test
    public void onResume_shouldForwardOnResumeToViewController() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onResume();

        verify(baseVideoViewController).onResume();
    }

    @Test
    public void onDestroy_shouldForwardOnDestroyToViewController() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onDestroy();

        verify(baseVideoViewController).onDestroy();
    }

    @Test
    public void onActivityResult_shouldForwardOnActivityResultToViewController() throws Exception {
        initializeSubjectWithMockViewController();

        int expectedRequestCode = -100;
        int expectedResultCode = 200;
        Intent expectedData = new Intent("arbitrary_data");
        subject.onActivityResult(expectedRequestCode, expectedResultCode, expectedData);

        verify(baseVideoViewController).onActivityResult(
                eq(expectedRequestCode),
                eq(expectedResultCode),
                eq(expectedData)
        );
    }

    @Test
    public void onSetContentView_shouldActuallySetContentView() throws Exception {
        initializeSubjectWithMockViewController();
        final View expectedView = new ImageView(context);

        subject.onSetContentView(expectedView);

        assertThat(Shadows.shadowOf(subject).getContentView()).isEqualTo(expectedView);
    }

    @Test
    public void onSetRequestedOrientation_shouldActuallySetRequestedOrientation() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onSetRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        assertThat(subject.getRequestedOrientation()).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Test
    public void onFinish_shouldActuallyCallFinish() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onFinish();

        assertThat(subject.isFinishing());
    }

    @Test
    public void onStartActivityForResult_shouldStartAnActivityWithRelevantRequestCodeAndExtras() throws Exception {
        initializeSubjectWithMockViewController();

        final Bundle expectedExtras = new Bundle();
        expectedExtras.putString("hello", "goodbye");

        subject.onStartActivityForResult(MoPubBrowser.class, 100, expectedExtras);

        final ShadowActivity.IntentForResult intentForResult = Shadows.shadowOf(subject).getNextStartedActivityForResult();

        assertThat(intentForResult.intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(intentForResult.intent.getExtras()).isEqualTo(expectedExtras);
        assertThat(intentForResult.requestCode).isEqualTo(100);
    }

    @Test
    public void onStartActivityForResult_withNullClass_shouldNotStartAnActivity() throws Exception {
        initializeSubjectWithMockViewController();

        subject.onStartActivityForResult(null, 100, new Bundle());

        final ShadowActivity.IntentForResult intentForResult = Shadows.shadowOf(subject).getNextStartedActivityForResult();
        assertThat(intentForResult).isNull();
    }

    private void initializeSubjectForMraid() {
        intent.putExtra(BaseVideoPlayerActivity.VIDEO_CLASS_EXTRAS_KEY, "mraid");

        subject = Robolectric.buildActivity(MraidVideoPlayerActivity.class)
                .withIntent(intent)
                .create()
                .get();
    }

    private void initializeSubjectForVast() {
        intent.putExtra(BaseVideoPlayerActivity.VIDEO_CLASS_EXTRAS_KEY, "vast");
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("video_path");
        intent.putExtra(VastVideoViewController.VAST_VIDEO_CONFIG, vastVideoConfig);

        subject = Robolectric.buildActivity(MraidVideoPlayerActivity.class)
                .withIntent(intent)
                .create()
                .get();
    }

    private void initializeSubjectWithMockViewController() {
        initializeSubjectForMraid();

        subject.setBaseVideoViewController(baseVideoViewController);
    }
}
