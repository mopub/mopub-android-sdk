package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.mobileads.BaseVideoPlayerActivity.VIDEO_URL;
import static com.mopub.mobileads.BaseVideoPlayerActivity.startMraid;
import static com.mopub.mobileads.BaseVideoPlayerActivity.startVast;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BaseVideoPlayerActivityTest {
    private static final String MRAID_VIDEO_URL = "https://mraidVideo";

    private long testBroadcastIdentifier;
    private VastVideoConfig mVastVideoConfig;

    @Before
    public void setup() throws Exception {
        mVastVideoConfig = mock(VastVideoConfig.class, withSettings().serializable());
        testBroadcastIdentifier = 1234;
    }

    @Test
    public void startMraid_shouldStartMraidVideoPlayerActivity() throws Exception {
        startMraid(Robolectric.buildActivity(Activity.class).create().get(), MRAID_VIDEO_URL);
        assertMraidVideoPlayerActivityStarted(MraidVideoPlayerActivity.class, MRAID_VIDEO_URL);
    }

    @Test
    public void startVast_shouldStartMraidVideoPlayerActivity() throws Exception {
        startVast(Robolectric.buildActivity(Activity.class).create().get(), mVastVideoConfig,
                testBroadcastIdentifier);
        assertVastVideoPlayerActivityStarted(MraidVideoPlayerActivity.class, mVastVideoConfig,
                testBroadcastIdentifier);
    }

    @Test
    public void onDestroy_shouldReleaseAudioFocus() throws Exception {
        BaseVideoPlayerActivity subject = spy(
                Robolectric.buildActivity(BaseVideoPlayerActivity.class).create().get());
        AudioManager mockAudioManager = mock(AudioManager.class);
        when(subject.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager);

        subject.onDestroy();

        verify(mockAudioManager).abandonAudioFocus(null);
        verifyNoMoreInteractions(mockAudioManager);
    }

    static void assertVastVideoPlayerActivityStarted(final Class clazz,
            final VastVideoConfig vastVideoConfig,
            final long broadcastIdentifier) {
        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertIntentAndBroadcastIdentifierAreCorrect(intent, clazz, broadcastIdentifier);

        final VastVideoConfig expectedVastVideoConfig =
                (VastVideoConfig) intent.getSerializableExtra(VastVideoViewController.VAST_VIDEO_CONFIG);
        assertThat(expectedVastVideoConfig).isEqualsToByComparingFields(vastVideoConfig);
    }

    public static void assertMraidVideoPlayerActivityStarted(final Class clazz, final String url) {
        final Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertIntentAndBroadcastIdentifierAreCorrect(intent, clazz, null);

        assertThat(intent.getStringExtra(VIDEO_URL)).isEqualTo(url);
    }

    static void assertIntentAndBroadcastIdentifierAreCorrect(final Intent intent,
            final Class clazz,
            final Long expectedBroadcastId) {
        assertThat(intent.getComponent().getClassName()).isEqualTo(clazz.getCanonicalName());
        assertThat(Utils.bitMaskContainsFlag(intent.getFlags(), Intent.FLAG_ACTIVITY_NEW_TASK)).isTrue();

        if (expectedBroadcastId != null) {
            final long actualBroadcastId = (Long) intent.getSerializableExtra(BROADCAST_IDENTIFIER_KEY);
            assertThat(actualBroadcastId).isEqualTo(expectedBroadcastId);
        }
    }
}
