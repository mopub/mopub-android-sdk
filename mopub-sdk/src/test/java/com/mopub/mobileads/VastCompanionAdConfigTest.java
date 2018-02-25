package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastCompanionAdConfigTest {

    private static final String RESOLVED_CLICKTHROUGH_URL = "https://www.mopub.com/";
    private static final String CLICKTHROUGH_URL = "deeplink+://navigate?" +
            "&primaryUrl=bogus%3A%2F%2Furl" +
            "&fallbackUrl=" + Uri.encode(RESOLVED_CLICKTHROUGH_URL);

    private VastCompanionAdConfig subject;
    private Context context;
    @Mock private MoPubRequestQueue mockRequestQueue;

    @Before
    public void setup() {
        subject = new VastCompanionAdConfig(123, 456,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 123, 456),
                CLICKTHROUGH_URL,
                VastUtils.stringsToVastTrackers("clickTrackerOne", "clickTrackerTwo"),
                VastUtils.stringsToVastTrackers("viewTrackerOne", "viewTrackerTwo")
        );
        context = Robolectric.buildActivity(Activity.class).create().get();
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void constructor_shouldSetParamsCorrectly() throws Exception {
        assertThat(subject.getWidth()).isEqualTo(123);
        assertThat(subject.getHeight()).isEqualTo(456);
        assertThat(subject.getVastResource().getResource()).isEqualTo("resource");
        assertThat(subject.getVastResource().getType()).isEqualTo(VastResource.Type.STATIC_RESOURCE);
        assertThat(subject.getVastResource().getCreativeType())
                .isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(subject.getClickThroughUrl()).isEqualTo(CLICKTHROUGH_URL);
        assertThat(VastUtils.vastTrackersToStrings(subject.getClickTrackers()))
                .containsOnly("clickTrackerOne", "clickTrackerTwo");
        assertThat(VastUtils.vastTrackersToStrings(subject.getCreativeViewTrackers()))
                .containsOnly("viewTrackerOne", "viewTrackerTwo");
    }

    @Test
    public void handleImpression_shouldTrackImpression() throws Exception {
        subject.handleImpression(context, 123);

        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerOne")));
        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerTwo")));
    }

    @Test
    public void handleClick_shouldNotTrackClick() throws Exception {
        subject.handleClick(context, 1, null, "dsp_creative_id");

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void handleClick_shouldOpenMoPubBrowser() throws Exception {
        subject.handleClick(context, 1, null, "dsp_creative_id");

        Robolectric.flushBackgroundThreadScheduler();
        Intent startedActivity = shadowOf((Activity) context).getNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY))
                .isEqualTo(RESOLVED_CLICKTHROUGH_URL);
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DSP_CREATIVE_ID))
                .isEqualTo("dsp_creative_id");
        assertThat(startedActivity.getData()).isNull();
    }
}
