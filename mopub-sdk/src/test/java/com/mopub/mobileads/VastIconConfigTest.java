package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

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
public class VastIconConfigTest {

    private VastIconConfig subject;
    private Context context;
    @Mock private MoPubRequestQueue mockRequestQueue;
    private String dspCreativeId;

    @Before
    public void setup() {
        subject = new VastIconConfig(123, 456, 789, 101,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 123, 456),
                VastUtils.stringsToVastTrackers("clickTrackerOne", "clickTrackerTwo"),
                "https://www.mopub.com/",
                VastUtils.stringsToVastTrackers("viewTrackerOne", "viewTrackerTwo")
        );
        context = Robolectric.buildActivity(Activity.class).create().get();
        dspCreativeId = "dspCreativeId";
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void constructor_shouldSetParamsCorrectly() throws Exception {
        assertThat(subject.getWidth()).isEqualTo(123);
        assertThat(subject.getHeight()).isEqualTo(456);
        assertThat(subject.getOffsetMS()).isEqualTo(789);
        assertThat(subject.getDurationMS()).isEqualTo(101);
        assertThat(subject.getVastResource().getResource()).isEqualTo("resource");
        assertThat(subject.getVastResource().getType()).isEqualTo(VastResource.Type.STATIC_RESOURCE);
        assertThat(subject.getVastResource().getCreativeType())
                .isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(VastUtils.vastTrackersToStrings(subject.getClickTrackingUris()))
                .containsOnly("clickTrackerOne", "clickTrackerTwo");
        assertThat(subject.getClickThroughUri()).isEqualTo("https://www.mopub.com/");
        assertThat(VastUtils.vastTrackersToStrings(subject.getViewTrackingUris()))
                .containsOnly("viewTrackerOne", "viewTrackerTwo");
    }

    @Test
    public void constructor_withNullOffset_shouldSetOffsetTo0() throws Exception {
        subject = new VastIconConfig(123, 456, null, 101,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 123, 456),
                VastUtils.stringsToVastTrackers("clickTrackerOne", "clickTrackerTwo"),
                "clickThroughUri",
                VastUtils.stringsToVastTrackers("viewTrackerOne", "viewTrackerTwo")
        );

        assertThat(subject.getOffsetMS()).isEqualTo(0);
    }

    @Test
    public void handleImpression_shouldTrackImpression() throws Exception {
        subject.handleImpression(context, 123, "uri");

        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerOne")));
        verify(mockRequestQueue).add(argThat(isUrl("viewTrackerTwo")));
    }

    @Test
    public void handleClick_shouldNotTrackClick() throws Exception {
        subject.handleClick(context, null, dspCreativeId);

        verifyNoMoreInteractions(mockRequestQueue);
    }


    @Test
    public void handleClick_shouldOpenMoPubBrowser() throws Exception {
        subject.handleClick(context, null, dspCreativeId);

        Robolectric.flushBackgroundThreadScheduler();
        Intent startedActivity = shadowOf((Activity) context).getNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY))
                .isEqualTo("https://www.mopub.com/");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DSP_CREATIVE_ID))
                .isEqualTo("dspCreativeId");
        assertThat(startedActivity.getData()).isNull();
    }
}
