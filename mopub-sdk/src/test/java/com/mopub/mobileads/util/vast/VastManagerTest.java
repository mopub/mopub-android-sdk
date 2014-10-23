package com.mopub.mobileads.util.vast;

import android.app.Activity;
import android.content.Context;
import android.view.Display;
import android.view.WindowManager;

import com.mopub.common.CacheService;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import static com.mopub.mobileads.util.vast.VastManager.VastManagerListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class VastManagerTest {
    static final String TEST_VAST_XML_STRING = "<VAST version='2.0'><Ad id='62833'><Wrapper><AdSystem>Tapad</AdSystem><VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml</VASTAdTagURI><Impression>http://myTrackingURL/wrapper/impression1</Impression><Impression>http://myTrackingURL/wrapper/impression2</Impression><Creatives><Creative AdID='62833'><Linear><TrackingEvents><Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking><Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking><Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking><Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking><Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking><Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking><Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking><Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking><Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking><Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking><Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking></TrackingEvents><VideoClicks><ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking></VideoClicks><MediaFiles><MediaFile delivery='progressive' bitrate='416' width='300' height='250' type='video/mp4'><![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video1.mp4]]></MediaFile></MediaFiles></Linear></Creative><Creative AdID=\"601364-Companion\"> <CompanionAds><Companion width=\"9000\"></Companion> </CompanionAds></Creative></Creatives></Wrapper></Ad></VAST><MP_TRACKING_URLS><MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL><MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL></MP_TRACKING_URLS>";
    static final String TEST_NESTED_VAST_XML_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VAST version='2.0'><Ad id='57722'><InLine><AdSystem version='1.0'>Tapad</AdSystem><AdTitle><![CDATA[PKW6T_LIV_DSN_Audience_TAPAD_3rd Party Audience Targeting_Action Movi]]></AdTitle><Description/><Impression><![CDATA[http://rtb-test.dev.tapad.com:8080/creative/imp.png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp&ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1]]></Impression><Creatives><Creative sequence='1' id='57722'><Linear><Duration>00:00:15</Duration><VideoClicks><ClickThrough><![CDATA[http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com]]></ClickThrough></VideoClicks><MediaFiles><MediaFile delivery='progressive' bitrate='416' width='800' height='480' type='video/mp4'><![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video.mp4]]></MediaFile></MediaFiles></Linear></Creative><Creative AdID=\"601364-Companion\"><CompanionAds><Companion id=\"valid\" height=\"250\" width=\"300\"><StaticResource creativeType=\"image/jpeg\">http://demo.tremormedia.com/proddev/vast/Blistex1.jpg</StaticResource><TrackingEvents><Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking><Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking></TrackingEvents><CompanionClickThrough>http://www.tremormedia.com</CompanionClickThrough></Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";
    static final String TEST_VAST_BAD_NEST_URL_XML_STRING = "<VAST version='2.0'><Ad id='62833'><Wrapper><AdSystem>Tapad</AdSystem><VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml\"$|||</VASTAdTagURI><Impression>http://myTrackingURL/wrapper/impression1</Impression><Impression>http://myTrackingURL/wrapper/impression2</Impression><Creatives><Creative AdID='62833'><Linear><TrackingEvents><Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking><Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking><Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking><Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking><Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking><Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking><Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking><Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking><Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking><Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking><Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking></TrackingEvents><VideoClicks><ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking></VideoClicks></Linear></Creative></Creatives></Wrapper></Ad></VAST><MP_TRACKING_URLS><MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL><MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL></MP_TRACKING_URLS>";

    private VastManager subject;
    private FakeHttpLayer mFakeHttpLayer;
    private VastManagerListener vastManagerListener;
    private Activity context;
    private VastVideoConfiguration vastVideoConfiguration;
    private Semaphore semaphore;

    @Before
    public void setup() {
        context = new Activity();
        CacheService.initializeDiskCache(context);
        subject = new VastManager(context);
        mFakeHttpLayer = Robolectric.getFakeHttpLayer();

        semaphore = new Semaphore(0);
        vastManagerListener = mock(VastManagerListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                VastManagerTest.this.vastVideoConfiguration = (VastVideoConfiguration) args[0];
                semaphore.release();
                return null;
            }
        }).when(vastManagerListener).onVastVideoConfigurationPrepared(any(VastVideoConfiguration.class));
    }

    @After
    public void tearDown() {
        Robolectric.getFakeHttpLayer().clearPendingHttpResponses();
        CacheService.clearAndNullCaches();
    }

    private void prepareVastVideoConfiguration() {
        subject.prepareVastVideoConfiguration(TEST_VAST_XML_STRING, vastManagerListener);

        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
    }

    @Test
    public void prepareVastVideoConfiguration_shouldNotifyTheListenerAndContainTheCorrectVastValues() throws Exception {
        // Vast redirect responses
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);
        // Video download response
        mFakeHttpLayer.addPendingHttpResponse(200, "video_data");

        prepareVastVideoConfiguration();
        semaphore.acquire();
        verify(vastManagerListener).onVastVideoConfigurationPrepared(any(VastVideoConfiguration.class));

        assertThat(vastVideoConfiguration.getNetworkMediaFileUrl()).isEqualTo("https://s3.amazonaws.com/mopub-vast/tapad-video.mp4");

        final String expectedFilePathDiskCache = CacheService.getFilePathDiskCache(vastVideoConfiguration.getNetworkMediaFileUrl());
        assertThat(vastVideoConfiguration.getDiskMediaFileUrl()).isEqualTo(expectedFilePathDiskCache);

        assertThat(vastVideoConfiguration.getClickThroughUrl()).isEqualTo("http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com");
        assertThat(vastVideoConfiguration.getImpressionTrackers().size()).isEqualTo(5);
        assertThat(vastVideoConfiguration.getStartTrackers().size()).isEqualTo(1);
        assertThat(vastVideoConfiguration.getFirstQuartileTrackers().size()).isEqualTo(1);
        assertThat(vastVideoConfiguration.getMidpointTrackers().size()).isEqualTo(1);
        assertThat(vastVideoConfiguration.getThirdQuartileTrackers().size()).isEqualTo(1);
        assertThat(vastVideoConfiguration.getCompleteTrackers().size()).isEqualTo(1);
        assertThat(vastVideoConfiguration.getClickTrackers().size()).isEqualTo(1);

        final VastCompanionAd vastCompanionAd = vastVideoConfiguration.getVastCompanionAd();
        assertThat(vastCompanionAd.getWidth()).isEqualTo(300);
        assertThat(vastCompanionAd.getHeight()).isEqualTo(250);
        assertThat(vastCompanionAd.getImageUrl()).isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.jpg");
        assertThat(vastCompanionAd.getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
        assertThat(vastCompanionAd.getClickTrackers())
                .containsOnly("http://myTrackingURL/firstCompanionCreativeView", "http://myTrackingURL/secondCompanionCreativeView");
    }

    @Test
    public void prepareVastVideoConfiguration_shouldHandleMultipleRedirects() throws Exception {
        // Vast redirect responses
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_VAST_XML_STRING);
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_VAST_XML_STRING);
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);
        // Video download response
        mFakeHttpLayer.addPendingHttpResponse(200, "video_data");

        prepareVastVideoConfiguration();
        semaphore.acquire();
        verify(vastManagerListener).onVastVideoConfigurationPrepared(any(VastVideoConfiguration.class));

        // at this point it should have 3 sets of data from TEST_VAST_XML_STRING and one set from TEST_NESTED_VAST_XML_STRING
        assertThat(vastVideoConfiguration.getNetworkMediaFileUrl()).isEqualTo("https://s3.amazonaws.com/mopub-vast/tapad-video.mp4");
        final String expectedFilePathDiskCache = CacheService.getFilePathDiskCache(vastVideoConfiguration.getNetworkMediaFileUrl());
        assertThat(vastVideoConfiguration.getDiskMediaFileUrl()).isEqualTo(expectedFilePathDiskCache);

        assertThat(vastVideoConfiguration.getClickThroughUrl()).isEqualTo("http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com");
        assertThat(vastVideoConfiguration.getImpressionTrackers().size()).isEqualTo(13);
        assertThat(vastVideoConfiguration.getStartTrackers().size()).isEqualTo(3);
        assertThat(vastVideoConfiguration.getFirstQuartileTrackers().size()).isEqualTo(3);
        assertThat(vastVideoConfiguration.getMidpointTrackers().size()).isEqualTo(3);
        assertThat(vastVideoConfiguration.getThirdQuartileTrackers().size()).isEqualTo(3);
        assertThat(vastVideoConfiguration.getCompleteTrackers().size()).isEqualTo(3);
        assertThat(vastVideoConfiguration.getClickTrackers().size()).isEqualTo(3);

        final VastCompanionAd vastCompanionAd = vastVideoConfiguration.getVastCompanionAd();
        assertThat(vastCompanionAd.getWidth()).isEqualTo(300);
        assertThat(vastCompanionAd.getHeight()).isEqualTo(250);
        assertThat(vastCompanionAd.getImageUrl()).isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.jpg");
        assertThat(vastCompanionAd.getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
        assertThat(vastCompanionAd.getClickTrackers())
                .containsOnly("http://myTrackingURL/firstCompanionCreativeView", "http://myTrackingURL/secondCompanionCreativeView");
    }


    @Test
    public void prepareVastVideoConfiguration_shouldReturnCorrectVastValuesWhenAVastRedirectFails() throws Exception {
        // Vast redirect response
        mFakeHttpLayer.addPendingHttpResponse(404, "");
        // Video download response
        mFakeHttpLayer.addPendingHttpResponse(200, "video_data");

        prepareVastVideoConfiguration();
        semaphore.acquire();
        verify(vastManagerListener).onVastVideoConfigurationPrepared(any(VastVideoConfiguration.class));

        assertThat(vastVideoConfiguration.getNetworkMediaFileUrl()).isEqualTo("https://s3.amazonaws.com/mopub-vast/tapad-video1.mp4");

        final String expectedFilePathDiskCache = CacheService.getFilePathDiskCache(vastVideoConfiguration.getNetworkMediaFileUrl());
        assertThat(vastVideoConfiguration.getDiskMediaFileUrl()).isEqualTo(expectedFilePathDiskCache);

        assertThat(vastVideoConfiguration.getClickThroughUrl()).isEqualTo(null);
        assertThat(vastVideoConfiguration.getImpressionTrackers().size()).isEqualTo(4);
        assertThat(vastVideoConfiguration.getFirstQuartileTrackers().size()).isEqualTo(1);
    }

    @Test
    public void prepareVastVideoConfiguration_withNoMediaUrlInXml_shouldReturnNull() throws Exception {
        subject.prepareVastVideoConfiguration(TEST_VAST_BAD_NEST_URL_XML_STRING, vastManagerListener);

        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
        semaphore.acquire();

        verify(vastManagerListener).onVastVideoConfigurationPrepared(null);
        assertThat(vastVideoConfiguration).isEqualTo(null);
    }

    @Test
    public void prepareVastVideoConfiguration_withNullXml_shouldReturnNull() throws Exception {
        subject.prepareVastVideoConfiguration(null, vastManagerListener);

        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
        semaphore.acquire();

        verify(vastManagerListener).onVastVideoConfigurationPrepared(null);
        assertThat(vastVideoConfiguration).isEqualTo(null);
    }

    @Test
    public void prepareVastVideoConfiguration_withEmptyXml_shouldReturnNull() throws Exception {
        subject.prepareVastVideoConfiguration("", vastManagerListener);

        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
        semaphore.acquire();

        verify(vastManagerListener).onVastVideoConfigurationPrepared(null);
        assertThat(vastVideoConfiguration).isEqualTo(null);
    }

    @Test
    public void prepareVastVideoConfiguration_withVideoInDiskCache_shouldNotDownloadVideo() throws Exception {
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);

        CacheService.putToDiskCache("https://s3.amazonaws.com/mopub-vast/tapad-video.mp4", "video_data".getBytes());

        prepareVastVideoConfiguration();
        semaphore.acquire();

        assertThat(mFakeHttpLayer.getSentHttpRequestInfos().size()).isEqualTo(1);
        verify(vastManagerListener).onVastVideoConfigurationPrepared(any(VastVideoConfiguration.class));
        assertThat(vastVideoConfiguration.getDiskMediaFileUrl())
                .isEqualTo(CacheService.getFilePathDiskCache("https://s3.amazonaws.com/mopub-vast/tapad-video.mp4"));
    }

    @Test
    public void prepareVastVideoConfiguration_withUninitializedDiskCache_shouldReturnNull() throws Exception {
        CacheService.clearAndNullCaches();
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);

        prepareVastVideoConfiguration();
        semaphore.acquire();

        verify(vastManagerListener).onVastVideoConfigurationPrepared(null);
        assertThat(vastVideoConfiguration).isEqualTo(null);
    }

    @Test
    public void cancel_shouldCancelBackgroundProcessingAndNotNotifyListenerWithNull() throws Exception {
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);

        Robolectric.getBackgroundScheduler().pause();

        subject.prepareVastVideoConfiguration(TEST_VAST_XML_STRING, vastManagerListener);

        subject.cancel();

        Robolectric.runBackgroundTasks();
        Robolectric.runUiThreadTasks();
        semaphore.acquire();

        verify(vastManagerListener).onVastVideoConfigurationPrepared(null);
        assertThat(vastVideoConfiguration).isEqualTo(null);
    }

    @Test
    public void getBestMediaFileUrl_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isEqualTo("video_url");
    }

    @Test
    public void getBestMediaFileUrl_withNullMediaType_shouldReturnNull() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, null, "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withIncompatibleMediaType_shouldReturnNull() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/rubbish", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withNullMediaUrl_shouldReturnNull() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/mp4", null);

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withNullDimension_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(null, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isEqualTo("video_url");
    }

    @Test
    public void getBestMediaFileUrl_withZeroDimension_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(0, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isEqualTo("video_url");
    }

    @Test
    public void getBestMediaFileUrl_withNegativeDimension_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.MediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(-1, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isEqualTo("video_url");
    }

    @Test
    public void getBestMediaFileUrl_withSameAspectRatios_shouldReturnUrlWithAreaCloserToScreenArea1() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastXmlManager.MediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(2400, 1440, "video/mp4", "video_url1");
        // Double screen size
        final VastXmlManager.MediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(1600, 960, "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url2");
    }

    @Test
    public void getBestMediaFileUrl_withSameAspectRatios_shouldReturnUrlWithAreaCloserToScreenArea2() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastXmlManager.MediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(2400, 1440, "video/mp4", "video_url1");
        // Half screen size
        final VastXmlManager.MediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(400, 240, "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url2");
    }

    @Test
    public void getBestMediaFileUrl_withSameArea_shouldReturnUrlWithAspectRatioCloserToScreenAspectRatio() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Landscape
        final VastXmlManager.MediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(400, 240, "video/mp4", "video_url1");
        // Portrait
        final VastXmlManager.MediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(240, 400, "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url1");
    }

    @Test
    public void getBestMediaFileUrl_withInvalidMediaTypeAndNullDimension_shouldReturnUrlWithNullDimension() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Invalid media type
        final VastXmlManager.MediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(800, 480, "video/invalid", "video_url1");
        // Null dimension
        final VastXmlManager.MediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(null, null, "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url2");
    }

    @Test
    public void getBestMediaFileUrl_withInvalidMediaTypeAndNullMediaType_shouldReturnNull() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        final VastXmlManager.MediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(800, 480, "video/invalid", "video_url1");
        final VastXmlManager.MediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(800, 480, null, "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestCompanionAd_shouldReturnCompanionAd() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(300, 250, "image/jpeg", "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertCompanionAdsAreEqual(companionXmlManager, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withNullMediaType_shouldReturnNull() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(300, 250, null, "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withIncompatibleMediaType_shouldReturnNull() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(300, 250, "image/rubbish", "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withNullImageUrl_shouldReturnNull() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(300, 250, "image/png", null);

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withNullDimension_shouldReturnCompanionAd() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(null, 250, "image/png", "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertCompanionAdsAreEqual(companionXmlManager, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withZeroDimension_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(0, 250, "image/png", "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertCompanionAdsAreEqual(companionXmlManager, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withNegativeDimension_shouldReturnMediaFileUrl() throws Exception {
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(-300, 250, "image/png", "image_url");

        final VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager));
        assertCompanionAdsAreEqual(companionXmlManager, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withSameAspectRatios_shouldReturnCompanionAdWithAreaCloserToScreenArea1() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager1 = initializeCompanionXmlManagerMock(2400, 1440, "image/png", "image_url1");
        // Double screen size
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager2 = initializeCompanionXmlManagerMock(1600, 960, "image/bmp", "image_url2");

        VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager1, companionXmlManager2));
        assertCompanionAdsAreEqual(companionXmlManager2, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withSameAspectRatios_shouldReturnCompanionAdWithAreaCloserToScreenArea2() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager1 = initializeCompanionXmlManagerMock(2400, 1440, "image/png", "image_url1");
        // Half screen size
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager2 = initializeCompanionXmlManagerMock(400, 240, "image/bmp", "image_url2");

        VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager1, companionXmlManager2));
        assertCompanionAdsAreEqual(companionXmlManager2, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withSameArea_shouldReturnCompanionAdWithAspectRatioCloserToScreenAspectRatio() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Landscape
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager1 = initializeCompanionXmlManagerMock(400, 240, "image/png", "image_url1");
        // Portrait
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager2 = initializeCompanionXmlManagerMock(240, 400, "image/bmp", "image_url2");

        VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager1, companionXmlManager2));
        assertCompanionAdsAreEqual(companionXmlManager1, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withInvalidMediaTypeAndNullDimension_shouldReturnCompanionAdWithNullDimension() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager1 = initializeCompanionXmlManagerMock(800, 480, "image/invalid", "image_url1");
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager2 = initializeCompanionXmlManagerMock(null, null, "image/bmp", "image_url2");

        VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager1, companionXmlManager2));
        assertCompanionAdsAreEqual(companionXmlManager2, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAdithInvalidMediaTypeAndNullMediaType_shouldReturnNull() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager1 = initializeCompanionXmlManagerMock(800, 480, "image/invalid", "image_url1");
        final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager2 = initializeCompanionXmlManagerMock(800, 480, null, "image_url2");

        VastCompanionAd bestCompanionAd = subject.getBestCompanionAd(Arrays.asList(companionXmlManager1, companionXmlManager2));
        assertThat(bestCompanionAd).isEqualTo(null);
    }

    private VastXmlManager.MediaXmlManager initializeMediaXmlManagerMock(
            final Integer width,
            final Integer height,
            final String type,
            final String mediaUrl) {
        VastXmlManager.MediaXmlManager mediaXmlManager = mock(VastXmlManager.MediaXmlManager.class);
        when(mediaXmlManager.getWidth()).thenReturn(width);
        when(mediaXmlManager.getHeight()).thenReturn(height);
        when(mediaXmlManager.getType()).thenReturn(type);
        when(mediaXmlManager.getMediaUrl()).thenReturn(mediaUrl);
        return mediaXmlManager;
    }

    private VastXmlManager.ImageCompanionAdXmlManager initializeCompanionXmlManagerMock(
            final Integer width,
            final Integer height,
            final String type,
            final String imageUrl) {
        VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = mock(VastXmlManager.ImageCompanionAdXmlManager.class);
        when(companionXmlManager.getWidth()).thenReturn(width);
        when(companionXmlManager.getHeight()).thenReturn(height);
        when(companionXmlManager.getType()).thenReturn(type);
        when(companionXmlManager.getImageUrl()).thenReturn(imageUrl);
        return companionXmlManager;
    }

    private void assertCompanionAdsAreEqual(
            final VastXmlManager.ImageCompanionAdXmlManager imageCompanionAdXmlManager,
            final VastCompanionAd vastCompanionAd) {
        final VastCompanionAd vastCompanionAd1 = new VastCompanionAd(
                imageCompanionAdXmlManager.getWidth(),
                imageCompanionAdXmlManager.getHeight(),
                imageCompanionAdXmlManager.getImageUrl(),
                imageCompanionAdXmlManager.getClickThroughUrl(),
                new ArrayList<String>(imageCompanionAdXmlManager.getClickTrackers())
        );
        assertCompanionAdsAreEqual(vastCompanionAd, vastCompanionAd1);
    }

    private void assertCompanionAdsAreEqual(
            final VastCompanionAd vastCompanionAd1,
            final VastCompanionAd vastCompanionAd2) {
        assertThat(vastCompanionAd1.getWidth()).isEqualTo(vastCompanionAd2.getWidth());
        assertThat(vastCompanionAd1.getHeight()).isEqualTo(vastCompanionAd2.getHeight());
        assertThat(vastCompanionAd1.getImageUrl()).isEqualTo(vastCompanionAd2.getImageUrl());
        assertThat(vastCompanionAd1.getClickThroughUrl()).isEqualTo(vastCompanionAd2.getClickThroughUrl());
        assertThat(vastCompanionAd1.getClickTrackers()).isEqualTo(vastCompanionAd2.getClickTrackers());
    }
}
