package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.ShadowMoPubHttpUrlConnection;
import com.mopub.mobileads.test.support.VastUtils;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.mobileads.VastXmlManagerAggregator.VastXmlManagerAggregatorListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Config(shadows = {ShadowMoPubHttpUrlConnection.class})
@RunWith(SdkTestRunner.class)
public class VastXmlManagerAggregatorTest {
    static final String TEST_VAST_XML_STRING = "<VAST version='2.0'>" +
            "    <Ad id='empty'>" +
            "        <InLine>" +
            "            <Impression><![CDATA[http:emptyimpression]]></Impression>" +
            "            <Creatives>" +
            "                <Creative>" +
            "                    <Linear>" +
            "                        <MediaFiles>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "            </Creatives>" +
            "            <Error><![CDATA[http://neverCallThisError]]></Error>" +
            "        </InLine>" +
            "    </Ad>" +
            "    <Ad id='62833' sequence='1'>" +
            "        <Wrapper>" +
            "            <AdSystem>Tapad</AdSystem>" +
            "            <VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml</VASTAdTagURI>" +
            "            <Impression>http://myTrackingURL/wrapper/impression1</Impression>" +
            "            <Impression>http://myTrackingURL/wrapper/impression2</Impression>" +
            "            <Creatives>" +
            "                <Creative AdID='62833'>" +
            "                    <Linear>" +
            "                        <TrackingEvents>" +
            "                            <Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking>" +
            "                            <Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking>" +
            "                            <Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking>" +
            "                            <Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking>" +
            "                            <Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking>" +
            "                            <Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking>" +
            "                            <Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking>" +
            "                            <Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking>" +
            "                            <Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking>" +
            "                            <Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking>" +
            "                            <Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking>" +
            "                        </TrackingEvents>" +
            "                        <VideoClicks>" +
            "                            <ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking>" +
            "                        </VideoClicks>" +
            "                        <MediaFiles>" +
            "                            <MediaFile delivery='progressive' bitrate='416' width='300' height='250' type='video/mp4'>" +
            "                                <![CDATA[http://videosInWrappersShouldNeverBePlayed]]>" +
            "                            </MediaFile>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "                <Creative AdID=\"601364-Companion\">" +
            "                    <CompanionAds>" +
            "                        <Companion id=\"wrappercompanion\" height=\"250\" width=\"456\">" +
            "                            <StaticResource creativeType=\"image/jpeg\">" +
            "                                http://wrapperCompanionAdStaticResource" +
            "                            </StaticResource>" +
            "                            <TrackingEvents>" +
            "                                <Tracking event=\"creativeView\">http://firstWrapperCompanionCreativeView</Tracking>" +
            "                                <Tracking event=\"creativeView\">http://secondWrapperCompanionCreativeView</Tracking>" +
            "                            </TrackingEvents>" +
            "                            <CompanionClickThrough>http://wrapperCompanionClickThrough</CompanionClickThrough>" +
            "                            <CompanionClickTracking><![CDATA[http://wrapperCompanionClickTracking]]></CompanionClickTracking>" +
            "                        </Companion> " +
            "                        <Companion id=\"noresource\" height=\"250\" width=\"456\">" +
            "                            <TrackingEvents>" +
            "                                <Tracking event=\"creativeView\">http://firstNoResourceWrapperCompanionCreativeView</Tracking>" +
            "                                <Tracking event=\"creativeView\">http://secondNoResourceWrapperCompanionCreativeView</Tracking>" +
            "                            </TrackingEvents>" +
            "                            <CompanionClickThrough>http://noResourceWrapperCompanionClickThrough</CompanionClickThrough>" +
            "                            <CompanionClickTracking><![CDATA[http://noResourceWrapperCompanionClickTracking1]]></CompanionClickTracking>" +
            "                        </Companion> " +
            "                    </CompanionAds>" +
            "                </Creative>" +
            "            </Creatives>" +
            "            <Extensions>" +
            "                <Extension type=\"MoPub\">" +
            "                    <MoPubViewabilityTracker" +
            "                            viewablePlaytime=\"2.5\"" +
            "                            percentViewable=\"50%\">" +
            "                        <![CDATA[http://ad.server.com/impression/dot.gif]]>" +
            "                    </MoPubViewabilityTracker>" +
            "                </Extension>" +
            "            </Extensions>" +
            "            <Error><![CDATA[http://wrapperErrorOne?errorcode=[ERRORCODE]]]></Error>" +
            "            <Error><![CDATA[http://wrapperErrorTwo?errorcode=[ERRORCODE]]]></Error>" +
            "        </Wrapper>" +
            "    </Ad>" +
            "</VAST>" +
            "<MP_TRACKING_URLS>" +
            "    <MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL>" +
            "    <MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL>" +
            "</MP_TRACKING_URLS>";

    static final String TEST_NESTED_VAST_XML_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<VAST version='2.0'>" +
            "    <Ad id='57722'>" +
            "        <InLine>" +
            "            <AdSystem version='1.0'>Tapad</AdSystem>" +
            "            <AdTitle><![CDATA[PKW6T_LIV_DSN_Audience_TAPAD_3rd Party Audience Targeting_Action Movi]]></AdTitle>" +
            "            <Description/>" +
            "            <Impression><![CDATA[http://rtb-test.dev.tapad.com:8080/creative/imp.png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp&ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1]]></Impression>" +
            "            <Creatives>" +
            "                <Creative sequence='1' id='57722'>" +
            "                    <Linear>" +
            "                       <Icons>" +
            "                           <Icon program=\"program\" width=\"123\" height=\"234\" xPosition=\"789\" " +
            "                           yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
            "                           duration=\"01:02:03.456\">" +
            "                               <StaticResource creativeType=\"ImAge/JpEg\">" +
            "                                   <![CDATA[imageJpeg]]>" +
            "                               </StaticResource>" +
            "                               <IconClicks>" +
            "                                   <IconClickThrough>" +
            "                                       <![CDATA[clickThroughUri]]>" +
            "                                   </IconClickThrough>" +
            "                                   <IconClickTracking>" +
            "                                       <![CDATA[clickTrackingUri1]]>" +
            "                                   </IconClickTracking>" +
            "                                   <IconClickTracking>" +
            "                                       <![CDATA[clickTrackingUri2]]>" +
            "                                   </IconClickTracking>" +
            "                               </IconClicks>" +
            "                               <IconViewTracking>" +
            "                                   <![CDATA[viewTrackingUri1]]>" +
            "                               </IconViewTracking>" +
            "                               <IconViewTracking>" +
            "                                   <![CDATA[viewTrackingUri2]]>" +
            "                               </IconViewTracking>" +
            "                            </Icon>" +
            "                        </Icons>" +
            "                        <Duration>00:00:15</Duration>" +
            "                        <VideoClicks>" +
            "                            <ClickThrough><![CDATA[http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com]]></ClickThrough>" +
            "                        </VideoClicks>" +
            "                        <MediaFiles>" +
            "                            <MediaFile delivery='progressive' bitrate='416' width='800' height='480' type='video/mp4'>" +
            "                                <![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video.mp4]]>" +
            "                            </MediaFile>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "                <Creative AdID=\"601364-Companion\">" +
            "                    <CompanionAds>" +
            "                        <Companion id=\"valid\" height=\"250\" width=\"300\">" +
            "                            <StaticResource creativeType=\"image/jpeg\">" +
            "                                http://demo.tremormedia.com/proddev/vast/Blistex1.jpg" +
            "                            </StaticResource>" +
            "                            <TrackingEvents>" +
            "                                <Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking>" +
            "                                <Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking>" +
            "                            </TrackingEvents>" +
            "                            <CompanionClickThrough>http://www.tremormedia.com</CompanionClickThrough>" +
            "                            <CompanionClickTracking><![CDATA[http://companionClickTracking1]]></CompanionClickTracking>" +
            "                            <CompanionClickTracking><![CDATA[http://companionClickTracking2]]></CompanionClickTracking>" +
            "                        </Companion>" +
            "                    </CompanionAds>" +
            "                </Creative>" +
            "            </Creatives>" +
            "            <Error><![CDATA[http://nestedInLineErrorOne]]></Error>" +
            "            <Error><![CDATA[http://nestedInLineErrorTwo]]></Error>" +
            "        </InLine>" +
            "    </Ad>" +
            "</VAST>";

    static final String TEST_NESTED_NO_COMPANION_VAST_XML_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<VAST version='2.0'>" +
            "    <Ad id='57722'>" +
            "        <InLine>" +
            "            <AdSystem version='1.0'>Tapad</AdSystem>" +
            "            <AdTitle><![CDATA[PKW6T_LIV_DSN_Audience_TAPAD_3rd Party Audience Targeting_Action Movi]]></AdTitle>" +
            "            <Description/>" +
            "            <Impression><![CDATA[http://rtb-test.dev.tapad.com:8080/creative/imp.png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp&ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1]]></Impression>" +
            "            <Creatives>" +
            "                <Creative sequence='1' id='57722'>" +
            "                    <Linear>" +
            "                        <Duration>00:00:15</Duration>" +
            "                        <VideoClicks>" +
            "                            <ClickThrough><![CDATA[http://rtb-test.dev.tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com]]></ClickThrough>" +
            "                        </VideoClicks>" +
            "                        <MediaFiles>" +
            "                            <MediaFile delivery='progressive' bitrate='416' width='800' height='480' type='video/mp4'>" +
            "                                <![CDATA[https://s3.amazonaws.com/mopub-vast/tapad-video.mp4]]>" +
            "                            </MediaFile>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "            </Creatives>" +
            "            <Extensions>" +
            "                <Extension type=\"MoPub\">" +
            "                    <MoPubViewabilityTracker" +
            "                            viewablePlaytime=\"3.5\"" +
            "                            percentViewable=\"70%\">" +
            "                        <![CDATA[http://ad.server.com/impression/dot.png]]>" +
            "                    </MoPubViewabilityTracker>" +
            "                </Extension>" +
            "            </Extensions>" +
            "        </InLine>" +
            "    </Ad>" +
            "</VAST>";

    static final String TEST_VAST_BAD_NEST_URL_XML_STRING = "<VAST version='2.0'><Ad id='62833'><Wrapper><AdSystem>Tapad</AdSystem><VASTAdTagURI>http://dsp.x-team.staging.mopub.com/xml\"$|||</VASTAdTagURI><Impression>http://myTrackingURL/wrapper/impression1</Impression><Impression>http://myTrackingURL/wrapper/impression2</Impression><Creatives><Creative AdID='62833'><Linear><TrackingEvents><Tracking event='creativeView'>http://myTrackingURL/wrapper/creativeView</Tracking><Tracking event='start'>http://myTrackingURL/wrapper/start</Tracking><Tracking event='midpoint'>http://myTrackingURL/wrapper/midpoint</Tracking><Tracking event='firstQuartile'>http://myTrackingURL/wrapper/firstQuartile</Tracking><Tracking event='thirdQuartile'>http://myTrackingURL/wrapper/thirdQuartile</Tracking><Tracking event='complete'>http://myTrackingURL/wrapper/complete</Tracking><Tracking event='mute'>http://myTrackingURL/wrapper/mute</Tracking><Tracking event='unmute'>http://myTrackingURL/wrapper/unmute</Tracking><Tracking event='pause'>http://myTrackingURL/wrapper/pause</Tracking><Tracking event='resume'>http://myTrackingURL/wrapper/resume</Tracking><Tracking event='fullscreen'>http://myTrackingURL/wrapper/fullscreen</Tracking></TrackingEvents><VideoClicks><ClickTracking>http://myTrackingURL/wrapper/click</ClickTracking></VideoClicks></Linear></Creative></Creatives><Error>![CDATA[http://badNestedError]]</Error]</Wrapper></Ad></VAST><MP_TRACKING_URLS><MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL><MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL></MP_TRACKING_URLS>";

    static final String TEST_JUST_ERROR_XML_STRING = "<VAST version='3.0'>" +
            "<Error><![CDATA[http://justErrorTracking?errorcode=[ERRORCODE]]]></Error>" +
            "</VAST>";

    static final String TEST_INVALID_VAST_XML_STRING = "<VAST version='fail'>" +
            "This is not vast." +
            "</VAST>";

    static final String TEST_INVALID_XML_STRING = "this is not xml at all<<<";

    static final String TEST_VAST_WITH_NEGATIVE_SEQUENCE_NUMBER_XML_STRING = "<VAST version='3.0'>" +
            "    <Ad id='sequenceTooHigh' sequence='42'>" +
            "        <InLine>" +
            "            <Impression><![CDATA[http:sequenceTooHighImp]]></Impression>" +
            "            <Creatives>" +
            "                <Creative>" +
            "                    <Linear>" +
            "                        <MediaFiles>" +
            "                            <MediaFile delivery='progressive' bitrate='416' width='300' height='250' type='video/mp4'>" +
            "                                <![CDATA[http://sequenceTooHighVideo]]>" +
            "                            </MediaFile>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "            </Creatives>" +
            "        </InLine>" +
            "    </Ad>" +
            "    <Ad id='negativeSequence' sequence='-2'>" +
            "        <InLine>" +
            "            <Impression><![CDATA[http://negativeSequence]]></Impression>" +
            "            <Creatives>" +
            "                <Creative>" +
            "                    <Linear>" +
            "                        <MediaFiles>" +
            "                            <MediaFile delivery='progressive' bitrate='416' width='300' height='250' type='video/mp4'>" +
            "                                <![CDATA[http://negativeSequence]]>" +
            "                            </MediaFile>" +
            "                        </MediaFiles>" +
            "                    </Linear>" +
            "                </Creative>" +
            "            </Creatives>" +
            "        </InLine>" +
            "    </Ad>" +
            "</VAST>";

    private Activity context;
    private Semaphore semaphore;
    private VastXmlManagerAggregatorListener vastXmlManagerAggregatorListener;
    private VastXmlManagerAggregator subject;
    private VastVideoConfig mVastVideoConfig;

    @Mock
    MoPubRequestQueue mockRequestQueue;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();

        Networking.setRequestQueueForTesting(mockRequestQueue);

        semaphore = new Semaphore(0);
        vastXmlManagerAggregatorListener = mock(VastXmlManagerAggregatorListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                VastXmlManagerAggregatorTest.this.mVastVideoConfig = (VastVideoConfig) args[0];
                semaphore.release();
                return null;
            }
        }).when(vastXmlManagerAggregatorListener).onAggregationComplete(any(VastVideoConfig.class));

        // Always assume landscape (where width > height) since videos will always be played in this orientation
        int screenWidth = 800;
        int screenHeight = 480;
        double screenAspectRatio = (double) screenWidth / screenHeight;
        int screenArea = screenWidth * screenHeight;
        subject = new VastXmlManagerAggregator(vastXmlManagerAggregatorListener, screenAspectRatio,
                screenArea, context);
    }

    // NOTE most of the functionality of this class is tested through VastManagerTest
    // through integration tests

    @Test
    public void doInBackground_shouldNotFollowRedirectsOnceTheLimitHasBeenReached() throws Exception {
        for (int i = 0; i < VastXmlManagerAggregator.MAX_TIMES_TO_FOLLOW_VAST_REDIRECT; i++) {
            ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_VAST_XML_STRING);
        }
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_NESTED_VAST_XML_STRING);

        subject.execute(TEST_VAST_XML_STRING);
        semaphore.acquire();

        assertThat(mVastVideoConfig).isNull();
    }

    @Test
    public void doInBackground_shouldFollowMaxRedirectsMinusOne() throws Exception {
        for (int i = 0; i < VastXmlManagerAggregator.MAX_TIMES_TO_FOLLOW_VAST_REDIRECT - 1; i++) {
            ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_VAST_XML_STRING);
        }
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_NESTED_VAST_XML_STRING);

        subject.execute(TEST_VAST_XML_STRING);
        semaphore.acquire();

        assertThat(mVastVideoConfig.getNetworkMediaFileUrl()).isEqualTo("https://s3" +
                ".amazonaws.com/mopub-vast/tapad-video.mp4");
        assertThat(mVastVideoConfig.getClickThroughUrl()).isEqualTo("http://rtb-test.dev" +
                ".tapad.com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMj" +
                "AwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTN" +
                "BMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxh" +
                "JTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLa" +
                "XQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMT" +
                "E2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzM" +
                "wMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlk" +
                "PUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3" +
                "D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad." +
                "com");
        assertThat(mVastVideoConfig.getImpressionTrackers().size()).isEqualTo(4 *
                VastXmlManagerAggregator.MAX_TIMES_TO_FOLLOW_VAST_REDIRECT + 1);
        assertThat(mVastVideoConfig.getFractionalTrackers().size()).isEqualTo(3 *
                VastXmlManagerAggregator.MAX_TIMES_TO_FOLLOW_VAST_REDIRECT);
    }

    @Test
    public void getBestMediaFileUrl_shouldReturnMediaFileUrl() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isEqualTo("video_url");
    }

    @Test
    public void getBestMediaFileUrl_withNullMediaType_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, null, "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withIncompatibleMediaType_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/rubbish", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withNullMediaUrl_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(300, 250, "video/mp4", null);

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withNullDimension_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(null, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withZeroDimension_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(0, 250,
                "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withNegativeDimension_shouldReturnNull() throws Exception {
        final VastMediaXmlManager mediaXmlManager = initializeMediaXmlManagerMock(-1, 250, "video/mp4", "video_url");

        final String bestMediaFileUrl = subject.getBestMediaFileUrl(Arrays.asList(mediaXmlManager));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withSameAspectRatios_shouldReturnUrlWithAreaCloserToScreenArea1() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastMediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(2400, 1440, "video/mp4", "video_url1");
        // Double screen size
        final VastMediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(1600, 960, "video/mp4", "video_url2");

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
        final VastMediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(2400, 1440, "video/mp4", "video_url1");
        // Half screen size
        final VastMediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(400, 240,
                "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(
                Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url2");
    }

    @Test
    public void getBestMediaFileUrl_withSameArea_shouldReturnUrlWithAspectRatioCloserToScreenAspectRatio() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Landscape
        final VastMediaXmlManager mediaXmlManager1 =
                initializeMediaXmlManagerMock(400, 240, "video/mp4", "video_url1");
        // Portrait
        final VastMediaXmlManager mediaXmlManager2 =
                initializeMediaXmlManagerMock(240, 400, "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(
                Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isEqualTo("video_url1");
    }

    @Test
    public void getBestMediaFileUrl_withInvalidMediaTypeAndNullDimension_shouldReturnNull() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Invalid media type
        final VastMediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(800, 480, "video/invalid", "video_url1");
        // Null dimension
        final VastMediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(null, null,
                "video/mp4", "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(
                Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestMediaFileUrl_withInvalidMediaTypeAndNullMediaType_shouldReturnNull() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        final VastMediaXmlManager mediaXmlManager1 = initializeMediaXmlManagerMock(800, 480, "video/invalid", "video_url1");
        final VastMediaXmlManager mediaXmlManager2 = initializeMediaXmlManagerMock(800,
                480, null, "video_url2");

        String bestMediaFileUrl = subject.getBestMediaFileUrl(
                Arrays.asList(mediaXmlManager1, mediaXmlManager2));
        assertThat(bestMediaFileUrl).isNull();
    }

    @Test
    public void getBestCompanionAd_shouldReturnCompanionAd() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(
                300, 250, "image_url", "image/jpeg", null, null);

        final VastCompanionAdConfig bestCompanionAd =
                subject.getBestCompanionAd(Arrays.asList(companionXmlManager),
                        VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertCompanionAdsAreEqual(companionXmlManager, bestCompanionAd);
    }

    @Test
    public void getBestCompanionAd_withInvalidVastResource_shouldReturnNull() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager = initializeCompanionXmlManagerMock(
                300, 250, "image_url", "image/INVALID", null, null);

        final VastCompanionAdConfig bestCompanionAd =
                subject.getBestCompanionAd(Arrays.asList(companionXmlManager),
                        VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withNullDimension_shouldReturnNull() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager =
                initializeCompanionXmlManagerMock(null, 250, "image_url", "image/png", null, null);

        final VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withWidthTooSmall_shouldReturnNull() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager =
                initializeCompanionXmlManagerMock(299, 250, "image_url", "image/png", null, null);

        final VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withHeightTooSmall_shouldReturnNull() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager =
                initializeCompanionXmlManagerMock(300, 249, "image_url", "image/png", null, null);

        final VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd).isNull();
    }

    @Test
    public void getBestCompanionAd_withSameAspectRatios_shouldReturnCompanionAdWithAreaCloserToScreenArea1() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(2400, 1440, "image_url1", "image/png", null, null);
        // Double screen size
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(1600, 960, "image_url2", "image/bmp", null, null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager1, companionXmlManager2),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("image_url2");
    }

    @Test
    public void getBestCompanionAd_withSameAspectRatios_shouldReturnCompanionAdWithAreaCloserToScreenArea2() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Triple screen size
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(2400, 1440, "image_url1", "image/png", null, null);
        // Half screen size
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(400, 250, "image_url2", "image/bmp", null, null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager1, companionXmlManager2),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("image_url2");
    }

    @Test
    public void getBestCompanionAd_withSameArea_shouldReturnLandscapeCompanionAdWithAspectRatioCloserToScreenAspectRatio() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Landscape
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(400, 250, "image_url1", "image/png", null, null);
        // Portrait
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(250, 400, "image_url2", "image/bmp", null, null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager1, companionXmlManager2),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("image_url1");
    }

    @Test
    public void getBestCompanionAd_withSameArea_shouldReturnPortraitCompanionAdWithAspectRatioCloserToScreenAspectRatio() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Landscape
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(400, 300, "image_url1", "image/png", null, null);
        // Portrait
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(300, 400, "image_url2", "image/bmp", null, null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager1, companionXmlManager2),
                VastXmlManagerAggregator.CompanionOrientation.PORTRAIT);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("image_url2");
    }

    @Test
    public void getBestCompanionAd_withAllThreeResourceTypes_shouldReturnStaticResourceType() throws Exception {
        // Static Resource
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(400, 250, "StaticResource", "image/png", null,
                        null);
        // HTML Resource
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(250, 400, null, null, null, "HTMLResource");
        // IFrame Resource
        final VastCompanionAdXmlManager companionXmlManager3 =
                initializeCompanionXmlManagerMock(250, 400, null, null, "IFrameResource", null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager3, companionXmlManager2, companionXmlManager1),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("StaticResource");
    }

    @Test
    public void getBestCompanionAd_withHTMLAndStaticResourceTypes_shouldReturnStaticResourceType() throws Exception {
        // Static Resource
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(400, 250, "StaticResource", "image/png", null, null);
        // HTML Resource
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(250, 400, null, null, null, "HTMLResource");

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager2, companionXmlManager1),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("StaticResource");
    }

    @Test
    public void getBestCompanionAd_withInvalidStaticResource_withValidHtmlResource_shouldReturnHtmlResource() throws Exception {
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(400, 250, "StaticResource", "INVALID",
                        "IFrameResource", null);
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(300, 400, null, null, null, "HTMLResource");

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager2, companionXmlManager1),
                VastXmlManagerAggregator.CompanionOrientation.LANDSCAPE);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("HTMLResource");
    }

    @Test
    public void getBestCompanionAd_withCompanionAdTooSmall_shouldReturnCompanionAdWithAtLeastMinimumSize() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // 305 x 305 is both fewer pixels (screen area) and a worse aspect ratio. It still should be
        // chosen because 240 is not wide enough to be considered for a companion ad
        final VastCompanionAdXmlManager companionXmlManager1 =
                initializeCompanionXmlManagerMock(305, 305, "image_url1", "image/png", null, null);
        final VastCompanionAdXmlManager companionXmlManager2 =
                initializeCompanionXmlManagerMock(240, 400, "image_url2", "image/bmp", null, null);

        VastCompanionAdConfig bestCompanionAd = subject.getBestCompanionAd(
                Arrays.asList(companionXmlManager1, companionXmlManager2),
                VastXmlManagerAggregator.CompanionOrientation.PORTRAIT);
        assertThat(bestCompanionAd.getVastResource().getResource()).isEqualTo("image_url1");
    }

    @Test
    public void getScaledDimensions_withWidthLargerThanScreen_shouldScaleWidthAndHeight() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Width and height are evaluated in landscape
        Point point = subject.getScaledDimensions(1600, 400);

        assertThat(point.x).isEqualTo(800 - VastVideoViewController.WEBVIEW_PADDING);
        assertThat(point.y).isEqualTo(200 - VastVideoViewController.WEBVIEW_PADDING);
    }

    @Test
    public void getScaledDimensions_withHeightLargerThanScreen_shouldScaleWidthAndHeight() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Width and height are evaluated in landscape
        Point point = subject.getScaledDimensions(400, 960);

        assertThat(point.x).isEqualTo(200 - VastVideoViewController.WEBVIEW_PADDING);
        assertThat(point.y).isEqualTo(480 - VastVideoViewController.WEBVIEW_PADDING);
    }

    @Test
    public void getScaledDimensions_withWidthAndHeightSmallerThanScreen_shouldNotScaleWidthAndHeight() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Width and height are evaluated in landscape
        Point point = subject.getScaledDimensions(800, 480);

        assertThat(point.x).isEqualTo(800);
        assertThat(point.y).isEqualTo(480);
    }

    @Test
    public void getScaledDimensions_withWidthAdjustedToLessThanZero_shouldNotScaleWidthAndHeight() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Width and height are evaluated in landscape
        Point point = subject.getScaledDimensions(1600, 2);

        assertThat(point.x).isEqualTo(1600);
        assertThat(point.y).isEqualTo(2);
    }

    @Test
    public void getScaledDimensions_withHeightAdjustedToLessThanZero_shouldNotScaleWidthAndHeight() throws Exception {
        // Default screen width is 480, height is 800
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        assertThat(display.getWidth()).isEqualTo(480);
        assertThat(display.getHeight()).isEqualTo(800);

        // Width and height are evaluated in landscape
        Point point = subject.getScaledDimensions(2, 960);

        assertThat(point.x).isEqualTo(2);
        assertThat(point.y).isEqualTo(960);
    }

    @Test
    public void getBestIcon_shouldReturnBestIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, 50, 123, 456, "staticResource1", "image/jpeg", null, null,
                        VastUtils.stringsToVastTrackers("clickTrackingUri1", "clickTrackingUri2"),
                        "clickThroughUri",
                        VastUtils.stringsToVastTrackers("viewTrackingUri1", "viewTrackingUri2"));
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getWidth()).isEqualTo(40);
        assertThat(bestIcon.getHeight()).isEqualTo(50);
        assertThat(bestIcon.getOffsetMS()).isEqualTo(123);
        assertThat(bestIcon.getDurationMS()).isEqualTo(456);
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource1");
        assertThat(bestIcon.getVastResource().getType()).isEqualTo(VastResource.Type
                .STATIC_RESOURCE);
        assertThat(bestIcon.getVastResource().getCreativeType())
                .isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(VastUtils.vastTrackersToStrings(bestIcon.getClickTrackingUris()))
                .containsOnly("clickTrackingUri1", "clickTrackingUri2");
        assertThat(bestIcon.getClickThroughUri()).isEqualTo("clickThroughUri");
        assertThat(VastUtils.vastTrackersToStrings(bestIcon.getViewTrackingUris()))
                .containsOnly("viewTrackingUri1", "viewTrackingUri2");
    }

    @Test
    public void getBestIcon_withMissingWidth_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(null, 50, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null,
                        new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withNegativeWidth_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(-1, 50, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withWidthGreaterThan300dp_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(301, 50, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withMissingHeight_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, null, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withNegativeHeight_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, -1, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withHeightGreaterThan300dp_shouldNotSelectThatIcon() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, 301, null, null, "staticResource1", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, "staticResource2", "image/png",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager1, iconXmlManager2));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("staticResource2");
    }

    @Test
    public void getBestIcon_withAllThreeResourceTypes_shouldReturnStaticResourceType() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, 40, null, null, "StaticResource", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, null, null, "IFrameResource",
                        null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager3 =
                initializeIconXmlManagerMock(40, 40, null, null, null, null, null, "HTMLResource",
                        new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager3, iconXmlManager2,
                iconXmlManager1));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("StaticResource");
    }

    @Test
    public void getBestIcon_withHTMLAndStaticResourceTypes_shouldReturnStaticResourceType() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, 40, null, null, "StaticResource", "image/jpeg",
                        null, null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, null, null, null, "HTMLResource",
                        new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager2, iconXmlManager1));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("StaticResource");
    }

    @Test
    public void getBestIcon_withInvalidStaticResource_withValidHtmlResource_shouldReturnHtmlResource() throws Exception {
        final VastIconXmlManager iconXmlManager1 =
                initializeIconXmlManagerMock(40, 40, null, null, "StaticResource", "INVALID",
                        "IFrameResource", null, new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());
        final VastIconXmlManager iconXmlManager2 =
                initializeIconXmlManagerMock(40, 40, null, null, null, null, null, "HTMLResource",
                        new ArrayList<VastTracker>(), null, new ArrayList<VastTracker>());

        VastIconConfig bestIcon = subject.getBestIcon(Arrays.asList(iconXmlManager2, iconXmlManager1));
        assertThat(bestIcon.getVastResource().getResource()).isEqualTo("HTMLResource");
    }

    @Test
    public void evaluateVastXmlManager_withStandardInline_shouldReturnValidVastVideoConfiguration() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_NESTED_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getImpressionTrackers()))
                .containsOnly("http://rtb-test.dev.tapad.com:8080/creative/imp" +
                        ".png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp&ta_pinfo" +
                        "=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1");
        assertThat(vastVideoConfig.getFractionalTrackers()).isEmpty();
        assertThat(vastVideoConfig.getAbsoluteTrackers()).isEmpty();
        assertThat(vastVideoConfig.getPauseTrackers()).isEmpty();
        assertThat(vastVideoConfig.getResumeTrackers()).isEmpty();
        assertThat(vastVideoConfig.getCompleteTrackers()).isEmpty();
        assertThat(vastVideoConfig.getCloseTrackers()).isEmpty();
        assertThat(vastVideoConfig.getSkipTrackers()).isEmpty();
        assertThat(vastVideoConfig.getClickTrackers()).isEmpty();
        assertThat(vastVideoConfig.getClickThroughUrl()).isEqualTo(
                "http://rtb-test.dev.tapad" +
                        ".com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com");
        assertThat(vastVideoConfig.getNetworkMediaFileUrl()).isEqualTo(
                "https://s3.amazonaws.com/mopub-vast/tapad-video.mp4");
        assertThat(vastVideoConfig.getSkipOffsetString()).isNull();
        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getErrorTrackers()))
                .containsOnly("http://nestedInLineErrorOne", "http://nestedInLineErrorTwo");

        VastCompanionAdConfig[] companionAds = new VastCompanionAdConfig[2];
        companionAds[0] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE);
        companionAds[1] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT);
        for (VastCompanionAdConfig companionAd : companionAds) {
            assertThat(companionAd.getWidth()).isEqualTo(300);
            assertThat(companionAd.getHeight()).isEqualTo(250);
            assertThat(companionAd.getVastResource().getResource())
                    .isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.jpg");
            assertThat(companionAd.getVastResource().getType())
                    .isEqualTo(VastResource.Type.STATIC_RESOURCE);
            assertThat(companionAd.getVastResource().getCreativeType())
                    .isEqualTo(VastResource.CreativeType.IMAGE);
            assertThat(companionAd.getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getClickTrackers()))
                    .containsOnly("http://companionClickTracking1",
                            "http://companionClickTracking2");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getCreativeViewTrackers()))
                    .containsExactly("http://myTrackingURL/firstCompanionCreativeView",
                            "http://myTrackingURL/secondCompanionCreativeView");
        }

        VastIconConfig vastIconConfig = vastVideoConfig.getVastIconConfig();
        assertThat(vastIconConfig.getWidth()).isEqualTo(123);
        assertThat(vastIconConfig.getHeight()).isEqualTo(234);
        assertThat(vastIconConfig.getDurationMS()).isEqualTo(3723456);
        assertThat(vastIconConfig.getOffsetMS()).isEqualTo(3723000);
        assertThat(vastIconConfig.getVastResource().getResource()).isEqualTo("imageJpeg");
        assertThat(vastIconConfig.getVastResource().getType()).isEqualTo(VastResource.Type.STATIC_RESOURCE);
        assertThat(vastIconConfig.getVastResource().getCreativeType()).isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(VastUtils.vastTrackersToStrings(vastIconConfig.getClickTrackingUris()))
                .containsOnly("clickTrackingUri1", "clickTrackingUri2");
        assertThat(vastIconConfig.getClickThroughUri()).isEqualTo("clickThroughUri");
        assertThat(VastUtils.vastTrackersToStrings(vastIconConfig.getViewTrackingUris()))
                .containsOnly("viewTrackingUri1", "viewTrackingUri2");
    }

    @Test
    public void evaluateVastXmlManager_withAWrapperToAnInline_shouldReturnValidVastVideoConfiguration() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_NESTED_VAST_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getImpressionTrackers()))
                .containsOnly(
                        "http://rtb-test.dev.tapad.com:8080/creative/imp" +
                                ".png?ts=1374099035457&svid=1&creative_id=30731&ctx_type=InApp" +
                                "&ta_pinfo" +
                                "=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&liverail_cp=1",
                        "http://myTrackingURL/wrapper/impression1",
                        "http://myTrackingURL/wrapper/impression2",
                        "http://www.mopub.com/imp1",
                        "http://www.mopub.com/imp2");

        assertThat(vastVideoConfig.getFractionalTrackers()).hasSize(3);
        assertThat(
                vastVideoConfig.getFractionalTrackers().get(0)).isEqualsToByComparingFields(
                new VastFractionalProgressTracker("http://myTrackingURL/wrapper/firstQuartile",
                        0.25f));
        assertThat(
                vastVideoConfig.getFractionalTrackers().get(1)).isEqualsToByComparingFields(
                new VastFractionalProgressTracker("http://myTrackingURL/wrapper/midpoint",
                        0.5f));
        assertThat(
                vastVideoConfig.getFractionalTrackers().get(2)).isEqualsToByComparingFields(
                new VastFractionalProgressTracker("http://myTrackingURL/wrapper/thirdQuartile",
                        0.75f));

        assertThat(vastVideoConfig.getAbsoluteTrackers().size()).isEqualTo(2);
        assertThat(vastVideoConfig.getAbsoluteTrackers().get(0)).isEqualsToByComparingFields(
                new VastAbsoluteProgressTracker("http://myTrackingURL/wrapper/creativeView", 0));
        assertThat(vastVideoConfig.getAbsoluteTrackers().get(1)).isEqualsToByComparingFields(
                new VastAbsoluteProgressTracker("http://myTrackingURL/wrapper/start", 2000));

        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getPauseTrackers()))
                .containsOnly("http://myTrackingURL/wrapper/pause");
        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getResumeTrackers()))
                .containsOnly("http://myTrackingURL/wrapper/resume");
        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getCompleteTrackers()))
                .containsOnly("http://myTrackingURL/wrapper/complete");
        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getErrorTrackers()))
                .containsExactly(
                        "http://wrapperErrorOne?errorcode=[ERRORCODE]",
                        "http://wrapperErrorTwo?errorcode=[ERRORCODE]",
                        "http://nestedInLineErrorOne",
                        "http://nestedInLineErrorTwo");

        assertThat(vastVideoConfig.getCloseTrackers()).isEmpty();
        assertThat(vastVideoConfig.getSkipTrackers()).isEmpty();

        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getClickTrackers()))
                .containsOnly("http://myTrackingURL/wrapper/click");

        assertThat(vastVideoConfig.getClickThroughUrl()).isEqualTo(
                "http://rtb-test.dev.tapad" +
                        ".com:8080/click?ta_pinfo=JnRhX2JpZD1iNDczNTQwMS1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmaXA9OTguMTE2LjEyLjk0JnNzcD1MSVZFUkFJTCZ0YV9iaWRkZXJfaWQ9NTEzJTNBMzA1NSZjdHg9MTMzMSZ0YV9jYW1wYWlnbl9pZD01MTMmZGM9MTAwMjAwMzAyOSZ1YT1Nb3ppbGxhJTJGNS4wKyUyOE1hY2ludG9zaCUzQitJbnRlbCtNYWMrT1MrWCsxMF84XzMlMjkrQXBwbGVXZWJLaXQlMkY1MzcuMzYrJTI4S0hUTUwlMkMrbGlrZStHZWNrbyUyOStDaHJvbWUlMkYyNy4wLjE0NTMuMTE2K1NhZmFyaSUyRjUzNy4zNiZjcHQ9VkFTVCZkaWQ9ZDgyNWZjZDZlNzM0YTQ3ZTE0NWM4ZTkyNzMwMjYwNDY3YjY1NjllMSZpZD1iNDczNTQwMC1lZjJkLTExZTItYTNkNS0yMjAwMGE4YzEwOWQmcGlkPUNPTVBVVEVSJnN2aWQ9MSZicD0zNS4wMCZjdHhfdHlwZT1BJnRpZD0zMDU1JmNyaWQ9MzA3MzE%3D&crid=30731&ta_action_id=click&ts=1374099035458&redirect=http%3A%2F%2Ftapad.com");
        assertThat(vastVideoConfig.getNetworkMediaFileUrl()).isEqualTo(
                "https://s3.amazonaws.com/mopub-vast/tapad-video.mp4");
        assertThat(vastVideoConfig.getSkipOffsetString()).isNull();

        VastCompanionAdConfig[] companionAds = new VastCompanionAdConfig[2];
        companionAds[0] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE);
        companionAds[1] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT);
        for (VastCompanionAdConfig companionAd : companionAds) {
            assertThat(companionAd.getWidth()).isEqualTo(300);
            assertThat(companionAd.getHeight()).isEqualTo(250);
            assertThat(companionAd.getVastResource().getResource())
                    .isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.jpg");
            assertThat(companionAd.getVastResource().getType())
                    .isEqualTo(VastResource.Type.STATIC_RESOURCE);
            assertThat(companionAd.getVastResource().getCreativeType())
                    .isEqualTo(VastResource.CreativeType.IMAGE);
            assertThat(companionAd.getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getClickTrackers()))
                    .containsOnly("http://companionClickTracking1",
                            "http://companionClickTracking2",
                            "http://noResourceWrapperCompanionClickTracking1");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getCreativeViewTrackers()))
                    .containsExactly("http://myTrackingURL/firstCompanionCreativeView",
                            "http://myTrackingURL/secondCompanionCreativeView",
                            "http://firstNoResourceWrapperCompanionCreativeView",
                            "http://secondNoResourceWrapperCompanionCreativeView");
        }

        VastIconConfig vastIconConfig = vastVideoConfig.getVastIconConfig();
        assertThat(vastIconConfig.getWidth()).isEqualTo(123);
        assertThat(vastIconConfig.getHeight()).isEqualTo(234);
        assertThat(vastIconConfig.getDurationMS()).isEqualTo(3723456);
        assertThat(vastIconConfig.getOffsetMS()).isEqualTo(3723000);
        assertThat(vastIconConfig.getVastResource().getResource()).isEqualTo("imageJpeg");
        assertThat(vastIconConfig.getVastResource().getType()).isEqualTo(VastResource.Type.STATIC_RESOURCE);
        assertThat(vastIconConfig.getVastResource().getCreativeType()).isEqualTo(VastResource.CreativeType.IMAGE);
        assertThat(VastUtils.vastTrackersToStrings(vastIconConfig.getClickTrackingUris()))
                .containsOnly("clickTrackingUri1", "clickTrackingUri2");
        assertThat(vastIconConfig.getClickThroughUri()).isEqualTo("clickThroughUri");
        assertThat(VastUtils.vastTrackersToStrings(vastIconConfig.getViewTrackingUris()))
                .containsOnly("viewTrackingUri1", "viewTrackingUri2");
    }

    @Test
    public void evaluateVastXmlManager_withInvalidXml_shouldReturnNullVastVideoConfiguration() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_BAD_NEST_URL_XML_STRING,
                new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
    }

    @Test
    public void evaluateVastXmlManager_withRedirectHavingNoCompanionAd_shouldReturnVastVideoConfigurationWithCompanionAdOfWrapper() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200,
                TEST_NESTED_NO_COMPANION_VAST_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        VastCompanionAdConfig[] companionAds = new VastCompanionAdConfig[2];
        companionAds[0] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_LANDSCAPE);
        companionAds[1] = vastVideoConfig.getVastCompanionAd(
                Configuration.ORIENTATION_PORTRAIT);
        for (VastCompanionAdConfig companionAd : companionAds) {
            assertThat(companionAd.getWidth()).isEqualTo(456);
            assertThat(companionAd.getHeight()).isEqualTo(250);
            assertThat(companionAd.getVastResource().getResource()).isEqualTo("http" +
                    "://wrapperCompanionAdStaticResource");
            assertThat(companionAd.getClickThroughUrl()).isEqualTo(
                    "http://wrapperCompanionClickThrough");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getClickTrackers()))
                    .containsOnly("http://wrapperCompanionClickTracking");
            assertThat(VastUtils.vastTrackersToStrings(companionAd.getCreativeViewTrackers()))
                    .containsExactly("http://firstWrapperCompanionCreativeView",
                            "http://secondWrapperCompanionCreativeView");
        }
    }

    @Test
    public void evaluateVastXmlManager_withSequenceNumbers_shouldReturnVastVideoConfigurationWithNegativeSequenceNumber() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_WITH_NEGATIVE_SEQUENCE_NUMBER_XML_STRING,
                new ArrayList<VastTracker>());

        assertThat(vastVideoConfig.getNetworkMediaFileUrl()).isEqualTo(
                "http://negativeSequence");
        assertThat(VastUtils.vastTrackersToStrings(vastVideoConfig.getImpressionTrackers()))
                .containsOnly("http://negativeSequence");
    }

    @Test
    public void evaluateVastXmlManager_withVideoViewabilityTrackerInLine_shouldReturnVastVideoConfigurationWithVideoViewabilityTracker() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_NESTED_NO_COMPANION_VAST_XML_STRING, new ArrayList<VastTracker>());

        VideoViewabilityTracker tracker = vastVideoConfig.getVideoViewabilityTracker();
        assertThat(tracker.getPercentViewable()).isEqualTo(70);
        assertThat(tracker.getViewablePlaytimeMS()).isEqualTo(3500);
        assertThat(tracker.getTrackingUrl()).isEqualTo("http://ad.server.com/impression/dot.png");
    }

    @Test
    public void evaluateVastXmlManager_withVideoViewabilityTrackerInWrapper_shouldReturnVastVideoConfigurationWithVideoViewabilityTracker() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_NESTED_VAST_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(TEST_VAST_XML_STRING,
                new ArrayList<VastTracker>());

        VideoViewabilityTracker tracker = vastVideoConfig.getVideoViewabilityTracker();
        assertThat(tracker.getPercentViewable()).isEqualTo(50);
        assertThat(tracker.getViewablePlaytimeMS()).isEqualTo(2500);
        assertThat(tracker.getTrackingUrl()).isEqualTo("http://ad.server.com/impression/dot.gif");
    }

    @Test
    public void evaluateVastXmlManager_withVideoViewabilityTrackerBothInWrapperAndInLine_shouldReturnVastVideoConfigurationWithVideoViewabilityTrackerFromInLine() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200,
                TEST_NESTED_NO_COMPANION_VAST_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(TEST_VAST_XML_STRING,
                new ArrayList<VastTracker>());

        VideoViewabilityTracker tracker = vastVideoConfig.getVideoViewabilityTracker();
        assertThat(tracker.getPercentViewable()).isEqualTo(70);
        assertThat(tracker.getViewablePlaytimeMS()).isEqualTo(3500);
        assertThat(tracker.getTrackingUrl()).isEqualTo("http://ad.server.com/impression/dot.png");
    }

    @Test
    public void isValidSequenceNumber_withNull_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber(null)).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withNegativeInteger_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("-123")).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withHighPositiveInteger_shouldReturnFalse() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("123456")).isFalse();
    }

    @Test
    public void isValidSequenceNumber_withDecimal_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("123.456")).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withInvalidInteger_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("this should fail!")).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withZero_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("0")).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withOne_shouldReturnTrue() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("1")).isTrue();
    }

    @Test
    public void isValidSequenceNumber_withTwo_shouldReturnFalse() {
        assertThat(VastXmlManagerAggregator.isValidSequenceNumber("2")).isFalse();
    }

    @Test
    public void evaluateVastXmlManager_withJustError_shouldReturnNullVastVideoConfiguration_shouldFireErrorTracker() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_JUST_ERROR_XML_STRING,
                new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
        verify(mockRequestQueue).add(argThat(isUrl("http://justErrorTracking?errorcode=900")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void evaluateVastXmlManager_withWrapperToJustError_shouldReturnNullVastVideoConfiguration_shouldFireErrorTrackers() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_JUST_ERROR_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
        verify(mockRequestQueue).add(argThat(isUrl("http://justErrorTracking?errorcode=303")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void evaluateVastXmlManager_withWrapperToVastXmlError_shouldReturnNullVastVideoConfiguration_shouldFireErrorTracker() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_INVALID_VAST_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void evaluateVastXmlManager_withWrapperToInvalidXml_shouldReturnNullVastVideoConfiguration_shouldFireErrorTracker() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, TEST_INVALID_XML_STRING);
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
        verify(mockRequestQueue).add(argThat(isUrl("http://wrapperErrorOne?errorcode=100")));
        verify(mockRequestQueue).add(argThat(isUrl("http://wrapperErrorTwo?errorcode=100")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void evaluateVastXmlManager_withWrapperToNoHttpResponse_shouldReturnNullVastVideoConfiguration_shouldFireErrorTracker() {
        VastVideoConfig vastVideoConfig = subject.evaluateVastXmlManager(
                TEST_VAST_XML_STRING, new ArrayList<VastTracker>());

        assertThat(vastVideoConfig).isNull();
        verify(mockRequestQueue).add(argThat(isUrl("http://wrapperErrorOne?errorcode=301")));
        verify(mockRequestQueue).add(argThat(isUrl("http://wrapperErrorTwo?errorcode=301")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    private VastMediaXmlManager initializeMediaXmlManagerMock(
            final Integer width,
            final Integer height,
            final String type,
            final String mediaUrl) {
        VastMediaXmlManager mediaXmlManager = mock(VastMediaXmlManager.class);
        when(mediaXmlManager.getWidth()).thenReturn(width);
        when(mediaXmlManager.getHeight()).thenReturn(height);
        when(mediaXmlManager.getType()).thenReturn(type);
        when(mediaXmlManager.getMediaUrl()).thenReturn(mediaUrl);
        return mediaXmlManager;
    }

    private VastCompanionAdXmlManager initializeCompanionXmlManagerMock(
            final Integer width,
            final Integer height,
            final String staticResource,
            final String staticResourceType,
            final String iFrameResource,
            final String htmlResource) {
        VastCompanionAdXmlManager companionXmlManager = mock(VastCompanionAdXmlManager.class);
        when(companionXmlManager.getWidth()).thenReturn(width);
        when(companionXmlManager.getHeight()).thenReturn(height);

        VastResourceXmlManager mockResourceXmlManager = initializeVastResourceXmlManagerMock(
                staticResource,
                staticResourceType,
                iFrameResource,
                htmlResource
        );
        when(companionXmlManager.getResourceXmlManager()).thenReturn(mockResourceXmlManager);

        return companionXmlManager;
    }

    private void assertCompanionAdsAreEqual(
            final VastCompanionAdXmlManager companionAdXmlManager,
            final VastCompanionAdConfig companionAd) {
        final VastCompanionAdConfig companionAd1 = new VastCompanionAdConfig(
                companionAdXmlManager.getWidth(),
                companionAdXmlManager.getHeight(),
                VastResource.fromVastResourceXmlManager(
                        companionAdXmlManager.getResourceXmlManager(),
                        companionAdXmlManager.getWidth(),
                        companionAdXmlManager.getHeight()),
                companionAdXmlManager.getClickThroughUrl(),
                companionAdXmlManager.getClickTrackers(),
                companionAdXmlManager.getCompanionCreativeViewTrackers()
        );
        assertCompanionAdsAreEqual(companionAd, companionAd1);
    }

    private void assertCompanionAdsAreEqual(
            final VastCompanionAdConfig vastCompanionAdConfig1,
            final VastCompanionAdConfig vastCompanionAdConfig2) {
        assertThat(vastCompanionAdConfig1.getWidth()).isEqualTo(vastCompanionAdConfig2.getWidth());
        assertThat(vastCompanionAdConfig1.getHeight()).isEqualTo(vastCompanionAdConfig2.getHeight());
        assertThat(vastCompanionAdConfig1.getVastResource().getResource())
                .isEqualTo(vastCompanionAdConfig2.getVastResource().getResource());
        assertThat(vastCompanionAdConfig1.getVastResource().getType())
                .isEqualTo(vastCompanionAdConfig2.getVastResource().getType());
        assertThat(vastCompanionAdConfig1.getVastResource().getCreativeType())
                .isEqualTo(vastCompanionAdConfig2.getVastResource().getCreativeType());
        assertThat(vastCompanionAdConfig1.getClickThroughUrl()).isEqualTo(vastCompanionAdConfig2.getClickThroughUrl());
        assertThat(vastCompanionAdConfig1.getClickTrackers()).isEqualTo(vastCompanionAdConfig2.getClickTrackers());
        assertThat(vastCompanionAdConfig1.getCreativeViewTrackers()).isEqualTo(
                vastCompanionAdConfig2.getCreativeViewTrackers());
    }

    private VastIconXmlManager initializeIconXmlManagerMock(
            final Integer width,
            final Integer height,
            final Integer offsetMS,
            final Integer durationMS,
            final String staticResource,
            final String staticResourceType,
            final String iFrameResource,
            final String htmlResource,
            final List<VastTracker> clickTrackingUris,
            final String clickThroughUri,
            final List<VastTracker> viewTrackingUris) {
        VastIconXmlManager iconXmlManager = mock(VastIconXmlManager.class);
        when(iconXmlManager.getWidth()).thenReturn(width);
        when(iconXmlManager.getHeight()).thenReturn(height);
        when(iconXmlManager.getOffsetMS()).thenReturn(offsetMS);
        when(iconXmlManager.getDurationMS()).thenReturn(durationMS);

        VastResourceXmlManager mockResourceXmlManager = initializeVastResourceXmlManagerMock(
                staticResource,
                staticResourceType,
                iFrameResource,
                htmlResource
        );
        when(iconXmlManager.getResourceXmlManager()).thenReturn(mockResourceXmlManager);

        when(iconXmlManager.getClickTrackingUris()).thenReturn(clickTrackingUris);
        when(iconXmlManager.getClickThroughUri()).thenReturn(clickThroughUri);
        when(iconXmlManager.getViewTrackingUris()).thenReturn(viewTrackingUris);
        return iconXmlManager;
    }

    static VastResourceXmlManager initializeVastResourceXmlManagerMock(
            final String staticResource,
            final String staticResourceType,
            final String iFrameResource,
            final String htmlResource) {
        VastResourceXmlManager mockResourceXmlManager = mock(VastResourceXmlManager.class);
        when(mockResourceXmlManager.getStaticResource()).thenReturn(staticResource);
        when(mockResourceXmlManager.getStaticResourceType()).thenReturn(staticResourceType);
        when(mockResourceXmlManager.getIFrameResource()).thenReturn(iFrameResource);
        when(mockResourceXmlManager.getHTMLResource()).thenReturn(htmlResource);
        return mockResourceXmlManager;
    }

}
