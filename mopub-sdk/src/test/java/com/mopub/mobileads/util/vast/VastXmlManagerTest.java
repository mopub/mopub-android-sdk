package com.mopub.mobileads.util.vast;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastXmlManagerTest {
    private static final String XML_HEADER_TAG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String TEST_VAST_XML_STRING = "<VAST xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:noNamespaceSchemaLocation=\"vast.xsd\">" +
            "            <Ad id=\"223626102\">" +
            "                <InLine>" +
            "                    <AdSystem version=\"2.0\">DART_DFA</AdSystem>" +
            "                    <AdTitle>In-Stream Video</AdTitle>" +
            "                    <Description>A test creative with a description.</Description>" +
            "                    <Survey/>" +
            "                    <Impression id=\"DART\">" +
            "                        <![CDATA[" +
            "                        http://ad.doubleclick.net/imp;v7;x;223626102;0-0;0;47414672;0/0;30477563/30495440/1;;~aopt=0/0/ff/0;~cs=j%3fhttp://s0.2mdn.net/dot.gif" +
            "                        ]]>" +
            "                    </Impression>" +
            "                    <Impression id=\"ThirdParty\">" +
            "                        <![CDATA[" +
            "                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145;sz=1x1;ord=2922389?" +
            "                        ]]>" +
            "                    </Impression>" +
            "                    <VASTAdTagURI><![CDATA[ http://0.dsp.dev1.mopub.com/xml ]]></VASTAdTagURI>" +
            "                    <Creatives>" +
            "                        <Creative sequence=\"1\" AdID=\"\">" +
            "                            <Linear>" +
            "                                <Duration>00:00:58</Duration>" +
            "                                <TrackingEvents>" +
            "                                    <Tracking event=\"start\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"midpoint\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=18;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"midpoint\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.3;sz=1x1;ord=2922389?" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"firstQuartile\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=26;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"firstQuartile\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.2;sz=1x1;ord=2922389?" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"thirdQuartile\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=27;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"thirdQuartile\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.4;sz=1x1;ord=2922389?" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"complete\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=13;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"complete\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.5;sz=1x1;ord=2922389?" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"mute\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=16;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"pause\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=15;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"fullscreen\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=19;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"fullscreen\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.6;sz=1x1;ord=2922389?" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                </TrackingEvents>" +
            "                                <AdParameters/>" +
            "                                <VideoClicks>" +
            "                                    <ClickThrough>" +
            "                                        <![CDATA[ http://www.google.com/support/richmedia ]]>" +
            "                                    </ClickThrough>" +
            "                                    <ClickTracking id=\"DART\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/click%3Bh%3Dv8/3e1b/3/0/%2a/z%3B223626102%3B0-0%3B0%3B47414672%3B255-0/0%3B30477563/30495440/1%3B%3B%7Eaopt%3D0/0/ff/0%3B%7Esscs%3D%3fhttp://s0.2mdn.net/dot.gif" +
            "                                        ]]>" +
            "                                    </ClickTracking>" +
            "                                    <ClickTracking id=\"ThirdParty\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/clk;212442087;33815766;i?http://www.google.com/support/richmedia" +
            "                                        ]]>" +
            "                                    </ClickTracking>" +
            "                                </VideoClicks>" +
            "                                <MediaFiles>" +
            "                                    <MediaFile id=\"1\" delivery=\"progressive\" type=\"video/quicktime\" bitrate=\"457\"" +
            "                                               width=\"300\" height=\"225\">" +
            "                                        <![CDATA[" +
            "                                        http://s3.amazonaws.com/uploads.hipchat.com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4" +
            "                                        ]]>" +
            "                                    </MediaFile>" +
            "                                </MediaFiles>" +
            "                            </Linear>" +
            "                        </Creative>" +
            "                        <Creative AdID=\"601364-Companion\">" +
            "                            <CompanionAds>" +
            "                               <Companion height=\"90\" width=\"728\">" +
            "                                   <StaticResource creativeType=\"image/jpeg\">http://demo.tremormedia.com/proddev/vast/728x90_banner1.jpg</StaticResource>" +
            "                                   <CompanionClickThrough>http://www.tremormedia.com</CompanionClickThrough>" +
            "                                   <BADTrackingEvents>" +
            "                                       <Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking>" +
            "                                       <Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking>" +
            "                                   </BADTrackingEvents>" +
            "                               </Companion>" +
            "                               <Companion id=\"valid\" height=\"250\" width=\"300\">" +
            "                                   <StaticResource creativeType=\"image/png\">http://demo.tremormedia.com/proddev/vast/Blistex1.png</StaticResource>" +
            "                                   <TrackingEvents>" +
            "                                       <Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking>" +
            "                                       <Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking>" +
            "                                   </TrackingEvents>" +
            "                                   <CompanionClickThrough>http://www.tremormedia.com</CompanionClickThrough>" +
            "                               </Companion>" +
            "                               <Companion height=\"789\" width=\"456\">" +
            "                                   <StaticResource creativeType=\"image/bmp\">" +
            "                                       <![CDATA[" +
            "                                           http://cdn.liverail.com/adasset/229/7969/300x250.bmp" +
            "                                       ]]>" +
            "                                   </StaticResource>" +
            "                                   <TrackingEvents>" +
            "                                       <Tracking event=\"creativeView\">" +
            "                                           <![CDATA[" +
            "                                               http://trackingUrl1.com/" +
            "                                           ]]>" +
            "                                       </Tracking>" +
            "                                   </TrackingEvents>" +
            "                                   <CompanionClickThrough>" +
            "                                       <![CDATA[" +
            "                                           http://clickThroughUrl1.com/" +
            "                                       ]]>" +
            "                                   </CompanionClickThrough>" +
            "                               </Companion>" +
            "                               <Companion height=\"789\" width=\"1011\">" +
            "                                   <StaticResource creativeType=\"image/gif\">" +
            "                                       <![CDATA[" +
            "                                           http://cdn.liverail.com/adasset/229/7969/300x250.gif" +
            "                                       ]]>" +
            "                                   </StaticResource>" +
            "                                   <CompanionClickThrough>" +
            "                                       <![CDATA[" +
            "                                           http://clickThroughUrl2.com/" +
            "                                       ]]>" +
            "                                   </CompanionClickThrough>" +
            "                               </Companion>" +
            "                               <Companion width=\"300\" height=\"60\">" +
            "                                   <StaticResource creativeType=\"application/x-shockwave-flash\">" +
            "                                       <![CDATA[" +
            "                                           http://cdn.liverail.com/adasset4/1331/229/7969/5122396e510b80db6b5ef4013ddabe90.swf" +
            "                                       ]]>" +
            "                                   </StaticResource>" +
            "                                   <TrackingEvents>" +
            "                                       <Tracking event=\"creativeView\">" +
            "                                           <![CDATA[" +
            "                                               http://trackingUrl2.com/" +
            "                                           ]]>" +
            "                                       </Tracking>" +
            "                                   </TrackingEvents>" +
            "                                   <CompanionClickThrough>" +
            "                                       <![CDATA[" +
            "                                           http://clickThroughUrl3.com/" +
            "                                       ]]>" +
            "                                   </CompanionClickThrough>" +
            "                               </Companion>" +
            "                               <Companion id=\"valid\" height=\"249\" width=\"299\">" +
            "                                   <BADStaticResource creativeType=\"image/jpeg\">http://demo.tremormedia.com/proddev/vast/Blistex1.jpg</BADStaticResource>" +
            "                                   <TrackingEvents>" +
            "                                           <Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking>" +
            "                                           <Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking>" +
            "                                           <BADTracking event=\"creativeView\">http://myTrackingURL/thirdCompanionCreativeView</BADTracking>" +
            "                                           <Tracking BADevent=\"creativeView\">http://myTrackingURL/fourthCompanionCreativeView</Tracking>" +
            "                                           <Tracking event=\"BADcreativeView\">http://myTrackingURL/fifthCompanionCreativeView</Tracking>" +
            "                                   </TrackingEvents>" +
            "                                   <BADCompanionClickThrough>http://www.tremormedia.com</BADCompanionClickThrough>" +
            "                               </Companion>" +
            "                               <Companion width=\"9000\">" +
            "                                   <TrackingEvents>" +
            "                                       <ThisWillNotBeFound>" +
            "                                           <Tracking event=\"creativeView\">http://myTrackingURL/firstCompanionCreativeView</Tracking>" +
            "                                           <Tracking event=\"creativeView\">http://myTrackingURL/secondCompanionCreativeView</Tracking>" +
            "                                       </ThisWillNotBeFound>" +
            "                                   </TrackingEvents>" +
            "                               </Companion>" +
            "                               <BADCompanion>" +
            "                               </BADCompanion>" +
            "                            </CompanionAds>" +
            "                        </Creative>" +
            "                    </Creatives>" +
            "                    <Extensions>" +
            "                        <Extension type=\"DART\">" +
            "                            <AdServingData>" +
            "                                <DeliveryData>" +
            "                                    <GeoData>" +
            "                                        <![CDATA[" +
            "                                        ct=US&st=CA&ac=415&zp=94103&bw=4&dma=197&city=13358" +
            "                                        ]]>" +
            "                                    </GeoData>" +
            "                                </DeliveryData>" +
            "                            </AdServingData>" +
            "                        </Extension>" +
            "                    </Extensions>" +
            "                </InLine>" +
            "            </Ad>" +
            "        </VAST>" +
            "<MP_TRACKING_URLS>" +
            "   <MP_TRACKING_URL>http://www.mopub.com/imp1</MP_TRACKING_URL>" +
            "   <MP_TRACKING_URL>http://www.mopub.com/imp2</MP_TRACKING_URL>" +
            "</MP_TRACKING_URLS>";

    private VastXmlManager mXmlManager;
    private boolean mExceptionRaised;

    @Before
    public void setup() {
        mXmlManager = new VastXmlManager();
        mExceptionRaised = false;

        try {
            mXmlManager.parseVastXml(TEST_VAST_XML_STRING);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            mExceptionRaised = true;
        } catch (IOException e) {
            e.printStackTrace();
            mExceptionRaised = true;
        } catch (SAXException e) {
            e.printStackTrace();
            mExceptionRaised = true;
        }
    }

    @Test
    public void parseVastXml_shouldNotRaiseAnExceptionProcessingValidXml() {
        assertThat(mExceptionRaised).isEqualTo(false);
    }

    @Test
    public void parseVastXml_shouldNotRaiseAnExceptionProcessingXmlWithXmlHeaderTag() throws ParserConfigurationException, IOException, SAXException {
        String xmlString = XML_HEADER_TAG + TEST_VAST_XML_STRING;

        mXmlManager = new VastXmlManager();
        mXmlManager.parseVastXml(xmlString);
    }

    @Test
    public void getImpressionTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getImpressionTrackers();

        assertThat(trackers.size()).isEqualTo(4);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);
        String tracker3 = trackers.get(2);
        String tracker4 = trackers.get(3);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/imp;v7;x;223626102;0-0;0;47414672;0/0;30477563/30495440/1;;~aopt=0/0/ff/0;~cs=j%3fhttp://s0.2mdn.net/dot.gif");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145;sz=1x1;ord=2922389?");
        assertThat(tracker3).isEqualTo("http://www.mopub.com/imp1");
        assertThat(tracker4).isEqualTo("http://www.mopub.com/imp2");
    }

    @Test
    public void getVideoStartTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getVideoStartTrackers();

        assertThat(trackers.size()).isEqualTo(1);

        String tracker1 = trackers.get(0);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;");
    }

    @Test
    public void getVideoFirstQuartileTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getVideoFirstQuartileTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=26;ecn1=1;etm1=0;");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.2;sz=1x1;ord=2922389?");
    }

    @Test
    public void getVideoMidpointTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getVideoMidpointTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=18;ecn1=1;etm1=0;");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.3;sz=1x1;ord=2922389?");
    }

    @Test
    public void getVideoThirdQuartileTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getVideoThirdQuartileTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=27;ecn1=1;etm1=0;");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.4;sz=1x1;ord=2922389?");
    }

    @Test
    public void getVideoCompleteTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getVideoCompleteTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=13;ecn1=1;etm1=0;");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.5;sz=1x1;ord=2922389?");
    }

    @Test
    public void getClickThroughUrl_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getClickThroughUrl();

        assertThat(url).isEqualTo("http://www.google.com/support/richmedia");
    }

    @Test
    public void getClickTrackers_shouldReturnTheCorrectValue() {
        List<String> trackers = mXmlManager.getClickTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        String tracker1 = trackers.get(0);
        String tracker2 = trackers.get(1);

        assertThat(tracker1).isEqualTo("http://ad.doubleclick.net/click%3Bh%3Dv8/3e1b/3/0/%2a/z%3B223626102%3B0-0%3B0%3B47414672%3B255-0/0%3B30477563/30495440/1%3B%3B%7Eaopt%3D0/0/ff/0%3B%7Esscs%3D%3fhttp://s0.2mdn.net/dot.gif");
        assertThat(tracker2).isEqualTo("http://ad.doubleclick.net/clk;212442087;33815766;i?http://www.google.com/support/richmedia");
    }

    @Test
    public void getCompanionAdXmlManagers_shouldReturnListOfPopulatedCompanionAdXmlManagers() throws Exception {
        List<VastXmlManager.ImageCompanionAdXmlManager> imageCompanionAdXmlManagers = mXmlManager.getCompanionAdXmlManagers();
        assertThat(imageCompanionAdXmlManagers.size()).isEqualTo(7);

        assertThat(imageCompanionAdXmlManagers.get(0).getWidth()).isEqualTo(728);
        assertThat(imageCompanionAdXmlManagers.get(0).getHeight()).isEqualTo(90);
        assertThat(imageCompanionAdXmlManagers.get(0).getType()).isEqualTo("image/jpeg");
        assertThat(imageCompanionAdXmlManagers.get(0).getImageUrl()).isEqualTo("http://demo.tremormedia.com/proddev/vast/728x90_banner1.jpg");
        assertThat(imageCompanionAdXmlManagers.get(0).getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
        assertThat(imageCompanionAdXmlManagers.get(0).getClickTrackers()).isEmpty();

        assertThat(imageCompanionAdXmlManagers.get(1).getWidth()).isEqualTo(300);
        assertThat(imageCompanionAdXmlManagers.get(1).getHeight()).isEqualTo(250);
        assertThat(imageCompanionAdXmlManagers.get(1).getType()).isEqualTo("image/png");
        assertThat(imageCompanionAdXmlManagers.get(1).getImageUrl()).isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.png");
        assertThat(imageCompanionAdXmlManagers.get(1).getClickThroughUrl()).isEqualTo("http://www.tremormedia.com");
        assertThat(imageCompanionAdXmlManagers.get(1).getClickTrackers())
                .containsOnly("http://myTrackingURL/firstCompanionCreativeView", "http://myTrackingURL/secondCompanionCreativeView");

        assertThat(imageCompanionAdXmlManagers.get(2).getWidth()).isEqualTo(456);
        assertThat(imageCompanionAdXmlManagers.get(2).getHeight()).isEqualTo(789);
        assertThat(imageCompanionAdXmlManagers.get(2).getType()).isEqualTo("image/bmp");
        assertThat(imageCompanionAdXmlManagers.get(2).getImageUrl()).isEqualTo("http://cdn.liverail.com/adasset/229/7969/300x250.bmp");
        assertThat(imageCompanionAdXmlManagers.get(2).getClickThroughUrl()).isEqualTo("http://clickThroughUrl1.com/");
        assertThat(imageCompanionAdXmlManagers.get(2).getClickTrackers()).containsOnly("http://trackingUrl1.com/");

        assertThat(imageCompanionAdXmlManagers.get(3).getWidth()).isEqualTo(1011);
        assertThat(imageCompanionAdXmlManagers.get(3).getHeight()).isEqualTo(789);
        assertThat(imageCompanionAdXmlManagers.get(3).getType()).isEqualTo("image/gif");
        assertThat(imageCompanionAdXmlManagers.get(3).getImageUrl()).isEqualTo("http://cdn.liverail.com/adasset/229/7969/300x250.gif");
        assertThat(imageCompanionAdXmlManagers.get(3).getClickThroughUrl()).isEqualTo("http://clickThroughUrl2.com/");
        assertThat(imageCompanionAdXmlManagers.get(3).getClickTrackers()).isEmpty();

        assertThat(imageCompanionAdXmlManagers.get(4).getWidth()).isEqualTo(300);
        assertThat(imageCompanionAdXmlManagers.get(4).getHeight()).isEqualTo(60);
        assertThat(imageCompanionAdXmlManagers.get(4).getType()).isEqualTo("application/x-shockwave-flash");
        assertThat(imageCompanionAdXmlManagers.get(4).getImageUrl()).isEqualTo("http://cdn.liverail.com/adasset4/1331/229/7969/5122396e510b80db6b5ef4013ddabe90.swf");
        assertThat(imageCompanionAdXmlManagers.get(4).getClickThroughUrl()).isEqualTo("http://clickThroughUrl3.com/");
        assertThat(imageCompanionAdXmlManagers.get(4).getClickTrackers()).containsOnly("http://trackingUrl2.com/");

        assertThat(imageCompanionAdXmlManagers.get(5).getWidth()).isEqualTo(299);
        assertThat(imageCompanionAdXmlManagers.get(5).getHeight()).isEqualTo(249);
        assertThat(imageCompanionAdXmlManagers.get(5).getType()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getImageUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getClickThroughUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getClickTrackers())
                .containsOnly("http://myTrackingURL/firstCompanionCreativeView", "http://myTrackingURL/secondCompanionCreativeView");

        assertThat(imageCompanionAdXmlManagers.get(6).getWidth()).isEqualTo(9000);
        assertThat(imageCompanionAdXmlManagers.get(6).getHeight()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getType()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getImageUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getClickThroughUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getClickTrackers()).isEmpty();
    }

    @Test
    public void getMediaFileUrl_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getMediaFileUrl();

        assertThat(url).isEqualTo("http://s3.amazonaws.com/uploads.hipchat.com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4");
    }

    @Test
    public void getVastAdTagURI_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getVastAdTagURI();

        assertThat(url).isEqualTo("http://0.dsp.dev1.mopub.com/xml");
    }

    @Test
    public void parsingMalformedXml_shouldNotCauseProblems() {
        String badXml = "<im>going<<<to||***crash></,>CDATA[]YOUR_FACE";

        VastXmlManager badManager = new VastXmlManager();

        try {
            badManager.parseVastXml(badXml);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        assertThat(badManager.getImpressionTrackers().size()).isEqualTo(0);
    }

    @Test
    public void parsingVastWithMalformedNodes_shouldNotCauseProblems() {
        String badXml = "<VAST><Impression id=\"DART\"></Impression><Tracking event=\"start\"><![CDATA[ good ]]><ExtraNode><![CDATA[ bad ]]></ExtraNode></Tracking></VAST>";

        VastXmlManager badManager = new VastXmlManager();

        try {
            badManager.parseVastXml(badXml);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        assertThat(badManager.getMediaFileUrl()).isEqualTo(null);
        assertThat(badManager.getVideoMidpointTrackers().size()).isEqualTo(0);
        assertThat(badManager.getImpressionTrackers().size()).isEqualTo(0);

        List<String> startTrackers = badManager.getVideoStartTrackers();
        assertThat(startTrackers.size()).isEqualTo(1);
        assertThat(startTrackers.get(0)).isEqualTo("good");
    }
}
