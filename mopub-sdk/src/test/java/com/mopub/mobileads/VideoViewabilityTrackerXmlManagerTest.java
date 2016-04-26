package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VideoViewabilityTrackerXmlManagerTest {
    private VideoViewabilityTrackerXmlManager subject;

    @Test
    public void getViewablePlaytimeMS_shouldParseHourFormat() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"01:01:01.001\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isEqualTo(3661001);
    }

    @Test
    public void getViewablePlaytimeMS_shouldParseSecondsFormat() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"01.001\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isEqualTo(1001);
    }

    @Test
    public void getViewablePlaytimeMS_shouldParseIntegerFormat() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"2\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isEqualTo(2000);
    }

    @Test
    public void getViewablePlaytimeMS_withoutViewablePlaytimeMS_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isNull();
    }

    @Test
    public void getViewablePlaytimeMS_withNegativeInteger_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"-1\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isNull();
    }

    @Test
    public void getViewablePlaytimeMS_withInvalidHourFormat_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"a01:01:01.001\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isNull();
    }

    @Test
    public void getViewablePlaytimeMS_withInvalidSecondsFormat_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"a01.001\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isNull();
    }

    @Test
    public void getViewablePlaytimeMS_withInvalidViewablePlaytimeMS_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1234!@#$%^*(asdf\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getViewablePlaytimeMS()).isNull();
    }

    @Test
    public void getPercentViewable_shouldParseWithPercentSign() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"25%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isEqualTo(25);
    }

    @Test
    public void getPercentViewable_shouldParseWithoutPercentSign() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"25\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isEqualTo(25);
    }

    @Test
    public void getPercentViewable_shouldTruncateFloats() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"25.9\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isEqualTo(25);
    }

    @Test
    public void getPercentViewable_withoutPercentViewable_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isNull();
    }

    @Test
    public void getPercentViewable_withNegativeInteger_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"-25\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isNull();
    }

    @Test
    public void getPercentViewable_withIntegerGreaterThan100_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"101\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isNull();
    }

    @Test
    public void getPercentViewable_withInvalidPercentViewable_shouldReturnNull() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"1\"" +
                "                             percentViewable=\"1234!@#$%^*(asdf\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getPercentViewable()).isNull();
    }

    @Test
    public void getVideoViewabilityTrackerUrl_shouldReturnVideoViewabilityTrackerUrl() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"2\"" +
                "                             percentViewable=\"50%\">" +
                "                         <![CDATA[https://ad.server.com/impression/dot.gif]]>" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getVideoViewabilityTrackerUrl())
                .isEqualTo("https://ad.server.com/impression/dot.gif");
    }

    @Test
    public void getVideoViewabilityTrackerUrl_withoutTrackerUrl_shouldReturnEmptyString() throws Exception {
        String videoViewabilityXml = "<MoPubViewabilityTracker" +
                "                             viewablePlaytime=\"2\"" +
                "                             percentViewable=\"50%\">" +
                "                     </MoPubViewabilityTracker>";

        subject = new VideoViewabilityTrackerXmlManager(createNode(videoViewabilityXml));

        assertThat(subject.getVideoViewabilityTrackerUrl()).isEmpty();
    }

}
