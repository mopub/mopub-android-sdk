package com.mopub.mobileads.util.vast;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;

import java.util.List;
import java.util.concurrent.Semaphore;

import static com.mopub.mobileads.util.vast.VastXmlManagerAggregator.VastXmlManagerAggregatorListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class VastXmlManagerAggregatorTest {
    static final String TEST_VAST_XML_STRING = "<VAST version='2.0'><Ad id='62833'><Wrapper><AdSystem>Tapad</AdSystem><VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml</VASTAdTagURI><Impression>http://myTrackingURL/wrapper/impression1</Impression><Impression>http://myTrackingURL/wrapper/impression2</Impression><Creatives><Creative AdID='62833'><Linear><TrackingEvents><Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking><Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking><Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking><Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking><Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking><Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking><Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking><Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking><Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking><Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking><Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking></TrackingEvents><VideoClicks><ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking></VideoClicks><MediaFiles><MediaFile delivery='progressive' bitrate='416' width='300' height='250' type='video/mp4'><![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video1.mp4]]></MediaFile></MediaFiles></Linear></Creative><Creative AdID=\"601364-Companion\"> <CompanionAds><Companion width=\"9000\"></Companion> </CompanionAds></Creative></Creatives></Wrapper></Ad></VAST><MP_TRACKING_URLS><MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL><MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL></MP_TRACKING_URLS>";
    static final String TEST_NESTED_VAST_XML_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><VAST version='2.0'><Ad id='57722'><InLine><AdSystem version='1.0'>Tapad</AdSystem><AdTitle><![CDATA[PKW6T_LIV_DSN_Audience_TAPAD_3rd Party Audience Targeting_Action Movi]]></AdTitle><Description/><Impression><![CDATA[http://rtb-test.dev.tapad.com:8080/creative/imp.png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp&ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1]]></Impression><Creatives><Creative sequence='1' id='57722'><Linear><Duration>00:00:15</Duration><VideoClicks><ClickThrough><![CDATA[http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com]]></ClickThrough></VideoClicks><MediaFiles><MediaFile delivery='progressive' bitrate='416' width='800' height='480' type='video/mp4'><![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video.mp4]]></MediaFile></MediaFiles></Linear></Creative><Creative AdID=\"601364-Companion\"><CompanionAds><Companion id=\"valid\" height=\"250\" width=\"300\"><StaticResource creativeType=\"image/jpeg\">http://demo.tremormedia.com/proddev/vast/Blistex1.jpg</StaticResource><TrackingEvents><Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking><Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking></TrackingEvents><CompanionClickThrough>http://www.tremormedia.com</CompanionClickThrough></Companion></CompanionAds></Creative></Creatives></InLine></Ad></VAST>";
    static final String TEST_VAST_BAD_NEST_URL_XML_STRING = "<VAST version='2.0'><Ad id='62833'><Wrapper><AdSystem>Tapad</AdSystem><VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml\"$|||</VASTAdTagURI><Impression>http://myTrackingURL/wrapper/impression1</Impression><Impression>http://myTrackingURL/wrapper/impression2</Impression><Creatives><Creative AdID='62833'><Linear><TrackingEvents><Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking><Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking><Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking><Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking><Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking><Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking><Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking><Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking><Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking><Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking><Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking></TrackingEvents><VideoClicks><ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking></VideoClicks></Linear></Creative></Creatives></Wrapper></Ad></VAST><MP_TRACKING_URLS><MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL><MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL></MP_TRACKING_URLS>";

    private FakeHttpLayer mFakeHttpLayer;
    private Semaphore semaphore;
    private VastXmlManagerAggregatorListener vastXmlManagerAggregatorListener;
    private VastXmlManagerAggregator subject;
    private List<VastXmlManager> vastXmlManagers;

    @Before
    public void setup() {
        mFakeHttpLayer = Robolectric.getFakeHttpLayer();

        semaphore = new Semaphore(0);
        vastXmlManagerAggregatorListener = mock(VastXmlManagerAggregatorListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                VastXmlManagerAggregatorTest.this.vastXmlManagers = (List<VastXmlManager>) args[0];
                semaphore.release();
                return null;
            }
        }).when(vastXmlManagerAggregatorListener).onAggregationComplete(anyListOf(VastXmlManager.class));

        subject = new VastXmlManagerAggregator(vastXmlManagerAggregatorListener);
    }

    // NOTE most of the functionality of this class is tested through VastManagerTest
    // through integration tests

    @Test
    public void processVast_shouldNotFollowRedirectsOnceTheLimitHasBeenReached() throws Exception {
        mFakeHttpLayer.addPendingHttpResponse(200, TEST_NESTED_VAST_XML_STRING);

        subject.setTimesFollowedVastRedirect(VastXmlManagerAggregator.MAX_TIMES_TO_FOLLOW_VAST_REDIRECT);
        subject.execute(TEST_VAST_XML_STRING);
        semaphore.acquire();

        assertThat(vastXmlManagers.size()).isEqualTo(1);
        assertThat(vastXmlManagers.get(0).getMediaFileUrl()).isEqualTo("https://s3.amazonaws.com/mopub-vast/tapad-video1.mp4");
        assertThat(vastXmlManagers.get(0).getClickThroughUrl()).isEqualTo(null);
        assertThat(vastXmlManagers.get(0).getImpressionTrackers().size()).isEqualTo(4);
        assertThat(vastXmlManagers.get(0).getFractionalProgressTrackers().size()).isEqualTo(3);
    }

}
