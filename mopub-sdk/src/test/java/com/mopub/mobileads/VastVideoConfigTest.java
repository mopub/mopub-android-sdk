package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

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
}
