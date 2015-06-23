package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.mobileads.BaseVideoPlayerActivity.VIDEO_URL;
import static com.mopub.mobileads.BaseVideoPlayerActivity.startMraid;
import static com.mopub.mobileads.BaseVideoPlayerActivity.startVast;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@RunWith(SdkTestRunner.class)
public class BaseVideoPlayerActivityTest {
    private static final String MRAID_VIDEO_URL = "http://mraidVideo";

    private long testBroadcastIdentifier;
    private VastVideoConfiguration vastVideoConfiguration;

    @Before
    public void setup() throws Exception {
        vastVideoConfiguration = mock(VastVideoConfiguration.class, withSettings().serializable());
        testBroadcastIdentifier = 1234;
    }

    @Test
    public void startMraid_shouldStartMraidVideoPlayerActivity() throws Exception {
        startMraid(Robolectric.buildActivity(Activity.class).create().get(), MRAID_VIDEO_URL);
        assertMraidVideoPlayerActivityStarted(MraidVideoPlayerActivity.class, MRAID_VIDEO_URL);
    }

    @Test
    public void startVast_shouldStartMraidVideoPlayerActivity() throws Exception {
        startVast(Robolectric.buildActivity(Activity.class).create().get(), vastVideoConfiguration, testBroadcastIdentifier);
        assertVastVideoPlayerActivityStarted(MraidVideoPlayerActivity.class, vastVideoConfiguration, testBroadcastIdentifier);
    }

    static void assertVastVideoPlayerActivityStarted(final Class clazz,
            final VastVideoConfiguration vastVideoConfiguration,
            final long broadcastIdentifier) {
        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertIntentAndBroadcastIdentifierAreCorrect(intent, clazz, broadcastIdentifier);

        final VastVideoConfiguration expectedVastVideoConfiguration =
                (VastVideoConfiguration) intent.getSerializableExtra(VastVideoViewController.VAST_VIDEO_CONFIGURATION);
        assertThat(expectedVastVideoConfiguration).isEqualsToByComparingFields(vastVideoConfiguration);
    }

    public static void assertMraidVideoPlayerActivityStarted(final Class clazz, final String url) {
        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
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
