package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoConfigTest {

    @Mock MoPubRequestQueue mockRequestQueue;
    private Activity activity;
    private VastVideoConfig subject;

    @Before
    public void setup() {
        activity = spy(Robolectric.buildActivity(Activity.class).create().get());
        Networking.setRequestQueueForTesting(mockRequestQueue);
        subject = new VastVideoConfig();
        subject.setNetworkMediaFileUrl("video_url");
    }

    @Test
    public void addFractionalTrackers_multipleTimes_shouldBeSorted() throws Exception {
        ArrayList<VastFractionalProgressTracker> testSet1 = new ArrayList<VastFractionalProgressTracker>();
        testSet1.add(new VastFractionalProgressTracker("test1a", 0.24f));
        testSet1.add(new VastFractionalProgressTracker("test1b", 0.5f));
        testSet1.add(new VastFractionalProgressTracker("test1c", 0.91f));

        ArrayList<VastFractionalProgressTracker> testSet2 = new ArrayList<VastFractionalProgressTracker>();
        testSet2.add(new VastFractionalProgressTracker("test2a", 0.14f));
        testSet2.add(new VastFractionalProgressTracker("test2b", 0.6f));
        testSet2.add(new VastFractionalProgressTracker("test2c", 0.71f));

        VastVideoConfig subject = new VastVideoConfig();

        subject.addFractionalTrackers(testSet1);
        subject.addFractionalTrackers(testSet2);

        assertThat(subject.getFractionalTrackers()).isSorted();
    }

    @Test
    public void addAbsoluteTrackers_multipleTimes_shouldBesSorted() throws Exception {
        ArrayList<VastAbsoluteProgressTracker> testSet1 = new ArrayList<VastAbsoluteProgressTracker>();
        testSet1.add(new VastAbsoluteProgressTracker("test1a", 1000));
        testSet1.add(new VastAbsoluteProgressTracker("test1b", 10000));
        testSet1.add(new VastAbsoluteProgressTracker("test1c", 50000));

        ArrayList<VastAbsoluteProgressTracker> testSet2 = new ArrayList<VastAbsoluteProgressTracker>();
        testSet2.add(new VastAbsoluteProgressTracker("test2a", 1100));
        testSet2.add(new VastAbsoluteProgressTracker("test2b", 9000));
        testSet2.add(new VastAbsoluteProgressTracker("test2c", 62000));

        VastVideoConfig subject = new VastVideoConfig();

        subject.addAbsoluteTrackers(testSet1);
        subject.addAbsoluteTrackers(testSet2);

        assertThat(subject.getAbsoluteTrackers()).isSorted();
    }

    @Test
    public void addVideoTrackers_withValidJSON_shouldHydrateUrls() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [" +
                        "\"http://mopub.com/%%VIDEO_EVENT%%/foo\"," +
                        "\"http://mopub.com/%%VIDEO_EVENT%%/bar\"" +
                    "]," +
                    "events: [ \"start\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> trackers = subject.getAbsoluteTrackers();
        assertThat(trackers.size()).isEqualTo(2);
        assertAbsoluteTracker(trackers.get(0), "http://mopub.com/start/foo", 0);
        assertAbsoluteTracker(trackers.get(1), "http://mopub.com/start/bar", 0);
    }

    @Test
    public void addVideoTrackers_withStartEvent_shouldAddAbsoluteTrackerWith0Ms() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"start\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> trackers = subject.getAbsoluteTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertAbsoluteTracker(trackers.get(0), "http://mopub.com/start/foo", 0);
    }

    @Test
    public void addVideoTrackers_withFirstQuartileEvent_shouldAddFractionalTrackerWithQuarterFraction() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"firstQuartile\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastFractionalProgressTracker> trackers = subject.getFractionalTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertFractionalTracker(trackers.get(0), "http://mopub.com/firstQuartile/foo", 0.25f);
    }

    @Test
    public void addVideoTrackers_withMidpointEvent_shouldAddFractionalTrackerWithHalfFraction() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"midpoint\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastFractionalProgressTracker> trackers = subject.getFractionalTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertFractionalTracker(trackers.get(0), "http://mopub.com/midpoint/foo", 0.5f);
    }

    @Test
    public void addVideoTrackers_withThirdQuartileEvent_shouldAddFractionalTrackerWithThirdFraction() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"thirdQuartile\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastFractionalProgressTracker> trackers = subject.getFractionalTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertFractionalTracker(trackers.get(0), "http://mopub.com/thirdQuartile/foo", 0.75f);
    }

    @Test
    public void addVideoTrackers_withCompleteEvent_shouldAddCompleteTracker() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"complete\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastTracker> trackers = subject.getCompleteTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertTracker(trackers.get(0), "http://mopub.com/complete/foo");
    }

    @Test
    public void addVideoTrackers_withCompanionAdViewEvent_shouldAddCreativeViewTracker() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"companionAdView\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();
        addCompanionAds(subject);

        subject.addVideoTrackers(videoTrackers);

        final List<VastTracker> landscapeTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE).getCreativeViewTrackers();
        final List<VastTracker> portraitTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT).getCreativeViewTrackers();
        assertThat(landscapeTrackers.size()).isEqualTo(2);
        assertThat(portraitTrackers.size()).isEqualTo(2);
        // First tracker is irrelevant and just necessary for test setup
        assertTracker(landscapeTrackers.get(1), "http://mopub.com/companionAdView/foo");
        assertTracker(portraitTrackers.get(1), "http://mopub.com/companionAdView/foo");
    }

    @Test
    public void addVideoTrackers_withCompanionClickEvent_shouldAddCreativeClickTracker() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                    "events: [ \"companionAdClick\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();
        addCompanionAds(subject);

        subject.addVideoTrackers(videoTrackers);

        final List<VastTracker> landscapeTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE).getClickTrackers();
        final List<VastTracker> portraitTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT).getClickTrackers();
        assertThat(landscapeTrackers.size()).isEqualTo(2);
        assertThat(portraitTrackers.size()).isEqualTo(2);
        // First tracker is irrelevant and just necessary for test setup
        assertTracker(landscapeTrackers.get(1), "http://mopub.com/companionAdClick/foo");
        assertTracker(portraitTrackers.get(1), "http://mopub.com/companionAdClick/foo");
    }

    @Test
    public void addVideoTrackers_withMultipleUrls_withMultipleEvents_shouldAddCorrespondingTrackers() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                    "urls: [" +
                        "\"http://mopub.com/%%VIDEO_EVENT%%/foo\"," +
                        "\"http://mopub.com/%%VIDEO_EVENT%%/bar\"" +
                    "]," +
                    "events: [" +
                        "\"start\"," +
                        "\"firstQuartile\"," +
                        "\"midpoint\"," +
                        "\"thirdQuartile\"," +
                        "\"complete\"," +
                        "\"companionAdView\"," +
                        "\"companionAdClick\"" +
                    "]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();
        addCompanionAds(subject);

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> startTrackers = subject.getAbsoluteTrackers();
        final List<VastFractionalProgressTracker> fractionalTrackers = // quartile trackers
                subject.getFractionalTrackers();
        final List<VastTracker> completeTrackers = subject.getCompleteTrackers();
        final List<VastTracker> landscapeCompanionViewTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE).getCreativeViewTrackers();
        final List<VastTracker> portraitCompanionViewTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT).getCreativeViewTrackers();
        final List<VastTracker> landscapeCompanionClickTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE).getClickTrackers();
        final List<VastTracker> portraitCompanionClickTrackers = subject.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT).getClickTrackers();
        assertThat(startTrackers.size()).isEqualTo(2);
        assertThat(fractionalTrackers.size()).isEqualTo(6);
        assertThat(completeTrackers.size()).isEqualTo(2);
        assertThat(landscapeCompanionViewTrackers.size()).isEqualTo(3);
        assertThat(portraitCompanionViewTrackers.size()).isEqualTo(3);
        assertThat(landscapeCompanionClickTrackers.size()).isEqualTo(3);
        assertThat(portraitCompanionClickTrackers.size()).isEqualTo(3);
        assertAbsoluteTracker(startTrackers.get(0), "http://mopub.com/start/foo", 0);
        assertAbsoluteTracker(startTrackers.get(1), "http://mopub.com/start/bar", 0);
        assertFractionalTracker(fractionalTrackers.get(0),
                "http://mopub.com/firstQuartile/foo", 0.25f);
        assertFractionalTracker(fractionalTrackers.get(1),
                "http://mopub.com/firstQuartile/bar", 0.25f);
        assertFractionalTracker(fractionalTrackers.get(2), "http://mopub.com/midpoint/foo", 0.5f);
        assertFractionalTracker(fractionalTrackers.get(3), "http://mopub.com/midpoint/bar", 0.5f);
        assertFractionalTracker(fractionalTrackers.get(4),
                "http://mopub.com/thirdQuartile/foo", 0.75f);
        assertFractionalTracker(fractionalTrackers.get(5),
                "http://mopub.com/thirdQuartile/bar", 0.75f);
        assertTracker(completeTrackers.get(0), "http://mopub.com/complete/foo");
        assertTracker(completeTrackers.get(1), "http://mopub.com/complete/bar");
        // First tracker is irrelevant and just necessary for test setup
        assertTracker(landscapeCompanionViewTrackers.get(1),
                "http://mopub.com/companionAdView/foo");
        assertTracker(landscapeCompanionViewTrackers.get(2),
                "http://mopub.com/companionAdView/bar");
        assertTracker(portraitCompanionViewTrackers.get(1),
                "http://mopub.com/companionAdView/foo");
        assertTracker(portraitCompanionViewTrackers.get(2),
                "http://mopub.com/companionAdView/bar");
        assertTracker(landscapeCompanionClickTrackers.get(1),
                "http://mopub.com/companionAdClick/foo");
        assertTracker(landscapeCompanionClickTrackers.get(2),
                "http://mopub.com/companionAdClick/bar");
        assertTracker(portraitCompanionClickTrackers.get(1),
                "http://mopub.com/companionAdClick/foo");
        assertTracker(portraitCompanionClickTrackers.get(2),
                "http://mopub.com/companionAdClick/bar");
    }

    @Test
    public void addVideoTrackers_withCompanionAdViewEvent_withoutCompanionAd_shouldDoNothing() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                "events: [ \"companionAdView\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();
        // Note companion ads were NOT added

        subject.addVideoTrackers(videoTrackers);

        // Trackers would be in companion ad, so just make sure they are still null
        assertThat(subject.getVastCompanionAd(Configuration.ORIENTATION_LANDSCAPE)).isNull();
        assertThat(subject.getVastCompanionAd(Configuration.ORIENTATION_PORTRAIT)).isNull();
    }

    @Test
    public void addVideoTrackers_withoutUrls_shouldDoNothing() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                "events: [ \"start\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> trackers = subject.getAbsoluteTrackers();
        assertThat(trackers).isEmpty();
    }

    @Test
    public void addVideoTrackers_withoutEvents_shouldDoNothing() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> trackers = subject.getAbsoluteTrackers();
        assertThat(trackers).isEmpty();
    }

    @Test
    public void addVideoTrackers_withInvalidEvent_shouldSkipInvalidEvent() throws Exception {
        final JSONObject videoTrackers = new JSONObject("{" +
                "urls: [ \"http://mopub.com/%%VIDEO_EVENT%%/foo\" ]," +
                "events: [ \"start\", \"INVALID\" ]" +
                "}");
        VastVideoConfig subject = new VastVideoConfig();

        subject.addVideoTrackers(videoTrackers);

        final List<VastAbsoluteProgressTracker> trackers = subject.getAbsoluteTrackers();
        assertThat(trackers.size()).isEqualTo(1);
        assertThat(trackers.get(0).getContent()).isEqualTo("http://mopub.com/start/foo");
    }

    @Test
    public void getUntriggeredTrackersBefore_withTriggeredTrackers_shouldNotReturnTriggered() throws Exception {
        VastVideoConfig subject = new VastVideoConfig();
        subject.setDiskMediaFileUrl("disk_video_path");
        subject.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                        new VastFractionalProgressTracker("second", 0.5f),
                        new VastFractionalProgressTracker("third", 0.75f)));
        subject.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("5secs", 5000),
                        new VastAbsoluteProgressTracker("10secs", 10000)));

        final List<VastTracker> untriggeredTrackers = subject.getUntriggeredTrackersBefore(11000,
                11000);
        assertThat(untriggeredTrackers).hasSize(5);
        untriggeredTrackers.get(0).setTracked();

        final List<VastTracker> secondTrackersList = subject.getUntriggeredTrackersBefore(11000,
                11000);
        assertThat(secondTrackersList).hasSize(4);
    }

    @Test
    public void getUntriggeredTrackersBefore_shouldReturnAllTrackersSorted() throws Exception {
        VastVideoConfig subject = new VastVideoConfig();
        subject.setDiskMediaFileUrl("disk_video_path");
        subject.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                        new VastFractionalProgressTracker("second", 0.5f),
                        new VastFractionalProgressTracker("third", 0.75f)));
        subject.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("1secs", 1000),
                        new VastAbsoluteProgressTracker("10secs", 10000)));

        final List<VastTracker> untriggeredTrackers = subject.getUntriggeredTrackersBefore(11000,
                11000);
        assertThat(untriggeredTrackers).hasSize(5);

        // Sorted absolute trackers, followed by sorted fractional trackers
        final VastTracker tracker0 = untriggeredTrackers.get(0);
        assertThat(tracker0).isExactlyInstanceOf(VastAbsoluteProgressTracker.class);
        assertThat(((VastAbsoluteProgressTracker) tracker0).getTrackingMilliseconds()).isEqualTo(
                1000);

        final VastTracker tracker1 = untriggeredTrackers.get(1);
        assertThat(tracker1).isExactlyInstanceOf(VastAbsoluteProgressTracker.class);
        assertThat(((VastAbsoluteProgressTracker) tracker1).getTrackingMilliseconds()).isEqualTo(
                10000);


        final VastTracker tracker2 = untriggeredTrackers.get(2);
        assertThat(tracker2).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker2).trackingFraction()).isEqualTo(0.25f);

        final VastTracker tracker3 = untriggeredTrackers.get(3);
        assertThat(tracker3).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker3).trackingFraction()).isEqualTo(0.5f);

        final VastTracker tracker4 = untriggeredTrackers.get(4);
        assertThat(tracker4).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker4).trackingFraction()).isEqualTo(0.75f);
    }

    @Test
    public void getUntriggeredTrackersBefore_withNegativeCurrentTime_shouldReturnNoTrackers() throws Exception {
        VastVideoConfig subject = new VastVideoConfig();
        subject.setDiskMediaFileUrl("disk_video_path");
        subject.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("zero", 0f),
                        new VastFractionalProgressTracker("half", 0.5f)));
        subject.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("5secs", 5000),
                        new VastAbsoluteProgressTracker("10secs", 10000)));

        final List<VastTracker> untriggeredTrackers = subject.getUntriggeredTrackersBefore(-2000,
                11000);
        assertThat(untriggeredTrackers).isEmpty();
    }

    @Test
    public void handleClickForResult_withNullClickThroughUrl_shouldNotOpenNewActivity() throws Exception {
        subject.handleClickForResult(activity, 1234, 1);

        Robolectric.getForegroundThreadScheduler().unPause();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void handleClickForResult_withMoPubNativeBrowserClickThroughUrl_shouldOpenExternalBrowser_shouldMakeTrackingHttpRequest() throws Exception {
        subject.setClickThroughUrl(
                "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.mopub.com%2F");
        subject.addClickTrackers(
                Arrays.asList(new VastTracker("https://trackerone+content=[CONTENTPLAYHEAD]"),
                        new VastTracker("https://trackertwo+error=[ERRORCODE]&asset=[ASSETURI]")));

        subject.handleClickForResult(activity, 2345, 1234);

        Robolectric.getForegroundThreadScheduler().unPause();
        Robolectric.getBackgroundThreadScheduler().advanceBy(0);
        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getDataString()).isEqualTo("https://www.mopub.com/");
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        verify(mockRequestQueue).add(argThat(isUrl("https://trackerone+content=00:00:02.345")));
        verify(mockRequestQueue).add(argThat(isUrl("https://trackertwo+error=&asset=video_url")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void handleClickWithoutResult_shouldOpenExternalBrowser_shouldMakeTrackingHttpRequest() throws Exception {
        subject.setClickThroughUrl(
                "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.mopub.com%2F");
        subject.addClickTrackers(
                Arrays.asList(new VastTracker("https://trackerone+content=[CONTENTPLAYHEAD]"),
                        new VastTracker("https://trackertwo+error=[ERRORCODE]&asset=[ASSETURI]")));

        subject.handleClickWithoutResult(activity.getApplicationContext(), 2345);

        Robolectric.getForegroundThreadScheduler().unPause();
        Robolectric.getBackgroundThreadScheduler().advanceBy(0);
        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getDataString()).isEqualTo("https://www.mopub.com/");
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        verify(mockRequestQueue).add(argThat(isUrl("https://trackerone+content=00:00:02.345")));
        verify(mockRequestQueue).add(argThat(isUrl("https://trackertwo+error=&asset=video_url")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void handleClickForResult_withMalformedMoPubNativeBrowserClickThroughUrl_shouldNotOpenANewActivity() throws Exception {
        // url2 is an invalid query parameter
        subject.setClickThroughUrl(
                "mopubnativebrowser://navigate?url2=https%3A%2F%2Fwww.mopub.com%2F");

        subject.handleClickForResult(activity, 3456, 1);

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void handleClickForResult_withAboutBlankClickThroughUrl_shouldFailSilently() throws Exception {
        subject.setClickThroughUrl("about:blank");

        subject.handleClickForResult(activity, 4567, 1);

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    private void assertAbsoluteTracker(final VastAbsoluteProgressTracker actualTracker,
            final String expectedUrl, final int expectedMs) {
        assertThat(actualTracker.getContent()).isEqualTo(expectedUrl);
        assertThat(actualTracker.getTrackingMilliseconds()).isEqualTo(expectedMs);
    }

    private void assertFractionalTracker(final VastFractionalProgressTracker actualTracker,
            final String expectedUrl, final float expectedFraction) {
        assertThat(actualTracker.getContent()).isEqualTo(expectedUrl);
        assertThat(actualTracker.trackingFraction()).isEqualTo(expectedFraction);
    }

    private void assertTracker(final VastTracker actualTracker, final String expectedUrl) {
        assertThat(actualTracker.getContent()).isEqualTo(expectedUrl);
    }

    private void addCompanionAds(VastVideoConfig subject) {
        VastCompanionAdConfig companionLandscape = new VastCompanionAdConfig(123, 456,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 123, 456),
                "http://mopub.com",
                VastUtils.stringsToVastTrackers("clickTracker"),
                VastUtils.stringsToVastTrackers("viewTracker"));
        VastCompanionAdConfig companionPortrait = new VastCompanionAdConfig(123, 456,
                new VastResource("resource", VastResource.Type.STATIC_RESOURCE, VastResource
                        .CreativeType.IMAGE, 123, 456),
                "http://mopub.com",
                VastUtils.stringsToVastTrackers("clickTracker"),
                VastUtils.stringsToVastTrackers("viewTracker"));
        subject.setVastCompanionAd(companionLandscape, companionPortrait);
    }
}
