package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.DeviceUtils.ForceOrientation;
import com.mopub.mobileads.test.support.VastUtils;

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
            "                    <Creatives>" +
            "                        <Creative sequence=\"1\" AdID=\"\">" +
            "                            <Linear skipoffset=\"25%\">" +
            "                                <Duration>00:00:58</Duration>" +
            "                                <TrackingEvents>" +
            "                                    <Tracking event=\"start\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"progress\" offset=\"13%\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"progress\" offset=\"01:01:10.300\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            // Invalid tracking due to ambiguous offset.
            "                                    <Tracking event=\"progress\" offset=\"01:01\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            // Invalid tracking due to a too-high percentage offset.
            "                                    <Tracking event=\"progress\" offset=\"113%\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            // Invalid tracking due to a negative percentage offset.
            "                                    <Tracking event=\"progress\" offset=\"-113%\">" +
            "                                        <![CDATA[" +
            "                                        http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            // Invalid tracking due to a non-number offset
            "                                    <Tracking event=\"progress\" offset=\"ten seconds\">" +
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
            "                                    <Tracking event=\"close\">" +
            "                                        <![CDATA[" +
            "                                        http://www.mopub.com/close?q=ignatius" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"close\">" +
            "                                        <![CDATA[" +
            "                                        http://www.mopub.com/close?q=j3" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"skip\">" +
            "                                        <![CDATA[" +
            "                                        http://www.mopub.com/skip?q=ignatius" +
            "                                        ]]>" +
            "                                    </Tracking>" +
            "                                    <Tracking event=\"skip\">" +
            "                                        <![CDATA[" +
            "                                        http://www.mopub.com/skip?q=j3" +
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
            "                               <Icons>" +
            "                                   <Icon program=\"program\" width=\"123\" height=\"234\" xPosition=\"789\" " +
            "                                   yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
            "                                   duration=\"01:02:03.456\">" +
            "                                       <StaticResource creativeType=\"ImAge/JpEg\">" +
            "                                           <![CDATA[imageJpeg]]>" +
            "                                       </StaticResource>" +
            "                                       <IconClicks>" +
            "                                           <IconClickThrough>" +
            "                                               <![CDATA[clickThroughUri]]>" +
            "                                           </IconClickThrough>" +
            "                                           <IconClickTracking>" +
            "                                               <![CDATA[clickTrackingUri1]]>" +
            "                                           </IconClickTracking>" +
            "                                           <IconClickTracking>" +
            "                                               <![CDATA[clickTrackingUri2]]>" +
            "                                           </IconClickTracking>" +
            "                                       </IconClicks>" +
            "                                       <IconViewTracking>" +
            "                                           <![CDATA[viewTrackingUri1]]>" +
            "                                       </IconViewTracking>" +
            "                                       <IconViewTracking>" +
            "                                           <![CDATA[viewTrackingUri2]]>" +
            "                                       </IconViewTracking>" +
            "                                    </Icon>" +
            "                                </Icons>" +
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
            "                                   <CompanionClickTracking><![CDATA[http://companionClickTracking1]]></CompanionClickTracking>" +
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
            "                                   <CompanionClickTracking><![CDATA[http://companionClickTracking2]]></CompanionClickTracking>" +
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
            "                                   <CompanionClickTracking><![CDATA[http://companionClickTracking3]]></CompanionClickTracking>" +
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
            "                                   <CompanionClickTracking><![CDATA[http://companionClickTracking4]]></CompanionClickTracking>" +
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
            "                                   <BADCompanionClickTracking><![CDATA[http://companionClickTracking5]]></BADCompanionClickTracking>" +
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
            "                        <Extension type=\"MoPub\">" +
            "                           <MoPubCtaText>custom CTA text</MoPubCtaText>" +
            "                           <MoPubSkipText>skip</MoPubSkipText>" +
            "                           <MoPubCloseIcon>http://ton.twitter.com/exchange-media/images/v4/star_icon_3x.png</MoPubCloseIcon>" +
            "                           <MoPubForceOrientation>device</MoPubForceOrientation>" +
            "                        </Extension>" +
            "                    </Extensions>" +
            "                </InLine>" +
            "                <Wrapper>" +
            "                   <AdSystem>Acudeo Compatible</AdSystem>" +
            "                   <VASTAdTagURI>http://0.dsp.dev1.mopub.com/xml</VASTAdTagURI>" +
            "                   <Impression>http://myTrackingURL/wrapper/impression</Impression>" +
            "                   <Creatives>" +
            "                   </Creatives>" +
            "                </Wrapper>" +
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

    /**
     * UNIT TESTS
     */

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
    public void parseVastXml_withMalformedXml_shouldNotCauseProblems() {
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

        assertThat(badManager.getMoPubImpressionTrackers().size()).isEqualTo(0);
    }

    @Test
    public void parseVastXml_withMalformedNodes_shouldNotCauseProblems() {
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

        assertThat(badManager.getAdXmlManagers()).isEmpty();
    }

    @Test
    public void getAdXmlManagers_shouldReturnAllAdXmlManagers() throws Exception {
        String vastXml = "<VAST xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:noNamespaceSchemaLocation=\"vast.xsd\">" +
                "    <Ad id=\"12345678\">" +
                "        <InLine></InLine>" +
                "    </Ad>" +
                "    <Ad id=\"87654321\">" +
                "        <Wrapper></Wrapper>" +
                "    </Ad>" +
                "</VAST>";

        VastXmlManager subject = new VastXmlManager();
        subject.parseVastXml(vastXml);
        List<VastAdXmlManager> vastAdXmlManagers = subject.getAdXmlManagers();

        assertThat(vastAdXmlManagers.size()).isEqualTo(2);
        assertThat(vastAdXmlManagers.get(0).getInLineXmlManager()).isNotNull();
        assertThat(vastAdXmlManagers.get(0).getWrapperXmlManager()).isNull();
        assertThat(vastAdXmlManagers.get(1).getInLineXmlManager()).isNull();
        assertThat(vastAdXmlManagers.get(1).getWrapperXmlManager()).isNotNull();
    }

    @Test
    public void getMoPubImpressionTrackers_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getMoPubImpressionTrackers()))
                .containsOnly("http://www.mopub.com/imp1", "http://www.mopub.com/imp2");
    }

    @Test
    public void getCustomCtaText_shouldReturnTheCorrectValue() {
        String customCtaText = mXmlManager.getCustomCtaText();

        assertThat(customCtaText).isEqualTo("custom CTA text");
    }

    @Test
    public void getCustomSkipText_shouldReturnTheCorrectValue() {
        String customSkipText = mXmlManager.getCustomSkipText();

        assertThat(customSkipText).isEqualTo("skip");
    }

    @Test
    public void getCustomCloseIconUrl_shouldReturnTheCorrectValue() {
        String customCloseIconUrl = mXmlManager.getCustomCloseIconUrl();

        assertThat(customCloseIconUrl).isEqualTo("http://ton.twitter" +
                ".com/exchange-media/images/v4/star_icon_3x.png");
    }

    @Test
    public void getCustomForceOrientation_shouldReturnTheCorrectValue() {
        ForceOrientation customForceOrientation = mXmlManager.getCustomForceOrientation();

        assertThat(customForceOrientation).isEqualTo(ForceOrientation.DEVICE_ORIENTATION);
    }

    /**
     * INTEGRATION TESTS
     */

    @Test
    public void getVastAdTagURI_withWrapperXmlManager_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getAdXmlManagers().get(0).getWrapperXmlManager().getVastAdTagURI();

        assertThat(url).isEqualTo("http://0.dsp.dev1.mopub.com/xml");
    }

    @Test
    public void getImpressionTrackers_withInLineXmlManager_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getImpressionTrackers()))
                .containsOnly("http://ad.doubleclick.net/imp;v7;x;223626102;0-0;0;47414672;0/0;" +
                                "30477563/30495440/1;;~aopt=0/0/ff/0;~cs=j%3fhttp://s0.2mdn" +
                                ".net/dot.gif",
                        "http://ad.doubleclick.net/ad/N270.Process_Other/B3473145;sz=1x1;ord=2922389?");
    }

    @Test
    public void getCompanionAdXmlManagers_withInLineXmlManager_shouldReturnListOfPopulatedCompanionAdXmlManagers() throws Exception {
        List<VastCompanionAdXmlManager> imageCompanionAdXmlManagers = mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getCompanionAdXmlManagers();
        assertThat(imageCompanionAdXmlManagers.size()).isEqualTo(7);

        assertThat(imageCompanionAdXmlManagers.get(0).getWidth()).isEqualTo(728);
        assertThat(imageCompanionAdXmlManagers.get(0).getHeight()).isEqualTo(90);
        assertThat(imageCompanionAdXmlManagers.get(0).getResourceXmlManager().getStaticResourceType())
                .isEqualTo("image/jpeg");
        assertThat(imageCompanionAdXmlManagers.get(0).getResourceXmlManager().getStaticResource())
                .isEqualTo("http://demo.tremormedia.com/proddev/vast/728x90_banner1.jpg");
        assertThat(imageCompanionAdXmlManagers.get(0).getClickThroughUrl()).isEqualTo(
                "http://www.tremormedia.com");
        assertThat(imageCompanionAdXmlManagers.get(0).getClickTrackers()).isEmpty();

        assertThat(imageCompanionAdXmlManagers.get(1).getWidth()).isEqualTo(300);
        assertThat(imageCompanionAdXmlManagers.get(1).getHeight()).isEqualTo(250);
        assertThat(imageCompanionAdXmlManagers.get(1).getResourceXmlManager().getStaticResourceType())
                .isEqualTo("image/png");
        assertThat(imageCompanionAdXmlManagers.get(1).getResourceXmlManager().getStaticResource())
                .isEqualTo("http://demo.tremormedia.com/proddev/vast/Blistex1.png");
        assertThat(imageCompanionAdXmlManagers.get(1).getClickThroughUrl()).isEqualTo(
                "http://www.tremormedia.com");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(1)
                .getClickTrackers())).containsOnly("http://companionClickTracking1");

        assertThat(imageCompanionAdXmlManagers.get(2).getWidth()).isEqualTo(456);
        assertThat(imageCompanionAdXmlManagers.get(2).getHeight()).isEqualTo(789);
        assertThat(imageCompanionAdXmlManagers.get(2).getResourceXmlManager().getStaticResourceType())
                .isEqualTo("image/bmp");
        assertThat(imageCompanionAdXmlManagers.get(2).getResourceXmlManager().getStaticResource())
                .isEqualTo("http://cdn.liverail.com/adasset/229/7969/300x250.bmp");
        assertThat(imageCompanionAdXmlManagers.get(2).getClickThroughUrl())
                .isEqualTo("http://clickThroughUrl1.com/");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(2)
                .getClickTrackers())).containsOnly("http://companionClickTracking2");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(2)
                .getCompanionCreativeViewTrackers())).containsOnly("http://trackingUrl1.com/");

        assertThat(imageCompanionAdXmlManagers.get(3).getWidth()).isEqualTo(1011);
        assertThat(imageCompanionAdXmlManagers.get(3).getHeight()).isEqualTo(789);
        assertThat(imageCompanionAdXmlManagers.get(3).getResourceXmlManager().getStaticResourceType())
                .isEqualTo("image/gif");
        assertThat(imageCompanionAdXmlManagers.get(3).getResourceXmlManager().getStaticResource())
                .isEqualTo("http://cdn.liverail.com/adasset/229/7969/300x250.gif");
        assertThat(imageCompanionAdXmlManagers.get(3).getClickThroughUrl()).isEqualTo(
                "http://clickThroughUrl2.com/");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(3)
                .getClickTrackers())).containsOnly("http://companionClickTracking3");
        assertThat(imageCompanionAdXmlManagers.get(3).getCompanionCreativeViewTrackers()).isEmpty();

        assertThat(imageCompanionAdXmlManagers.get(4).getWidth()).isEqualTo(300);
        assertThat(imageCompanionAdXmlManagers.get(4).getHeight()).isEqualTo(60);
        assertThat(imageCompanionAdXmlManagers.get(4).getResourceXmlManager().getStaticResourceType())
                .isEqualTo("application/x-shockwave-flash");
        assertThat(imageCompanionAdXmlManagers.get(4).getResourceXmlManager().getStaticResource())
                .isEqualTo(
                        "http://cdn.liverail.com/adasset4/1331/229/7969/5122396e510b80db6b5ef4013ddabe90.swf");
        assertThat(imageCompanionAdXmlManagers.get(4).getClickThroughUrl()).isEqualTo(
                "http://clickThroughUrl3.com/");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(4)
                .getClickTrackers())).containsOnly("http://companionClickTracking4");
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(4)
                .getCompanionCreativeViewTrackers())).containsOnly("http://trackingUrl2.com/");

        assertThat(imageCompanionAdXmlManagers.get(5).getWidth()).isEqualTo(299);
        assertThat(imageCompanionAdXmlManagers.get(5).getHeight()).isEqualTo(249);
        assertThat(imageCompanionAdXmlManagers.get(5).getResourceXmlManager().getStaticResourceType()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getResourceXmlManager().getStaticResource()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getClickThroughUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(5).getClickTrackers()).isEmpty();
        assertThat(VastUtils.vastTrackersToStrings(imageCompanionAdXmlManagers.get(5)
                .getCompanionCreativeViewTrackers()))
                .containsOnly("http://myTrackingURL/firstCompanionCreativeView",
                        "http://myTrackingURL/secondCompanionCreativeView");

        assertThat(imageCompanionAdXmlManagers.get(6).getWidth()).isEqualTo(9000);
        assertThat(imageCompanionAdXmlManagers.get(6).getHeight()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getResourceXmlManager().getStaticResourceType()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getResourceXmlManager().getStaticResource()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getClickThroughUrl()).isNull();
        assertThat(imageCompanionAdXmlManagers.get(6).getClickTrackers()).isEmpty();
        assertThat(imageCompanionAdXmlManagers.get(6).getCompanionCreativeViewTrackers()).isEmpty();
    }

    @Test
    public void getAbsoluteProgressTrackers_withLinearXmlManager_shouldReturnCorrectValues() {
        List<VastAbsoluteProgressTracker> trackers = mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getLinearXmlManagers().get(0).getAbsoluteProgressTrackers();

        assertThat(trackers.size()).isEqualTo(2);

        VastAbsoluteProgressTracker tracker0 = trackers.get(0);
        assertThat(tracker0.getTrackingMilliseconds()).isEqualTo(2000);
        assertThat(tracker0.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;");

        VastAbsoluteProgressTracker tracker1 = trackers.get(1);
        assertThat(tracker1.getTrackingMilliseconds()).isEqualTo(3670300);
        assertThat(tracker1.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;" +
                "src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;" +
                "rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;");
    }

    @Test
    public void getFractionalTrackers_withLinearXmlManager_shouldReturnCorrectValues() {
        List<VastFractionalProgressTracker> trackers = mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getLinearXmlManagers().get(0).getFractionalProgressTrackers();

        assertThat(trackers.size()).isEqualTo(7);

        VastFractionalProgressTracker tracker0 = trackers.get(0);
        assertThat(tracker0.trackingFraction()).isEqualTo(0.13f);
        assertThat(tracker0.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=11;ecn1=1;etm1=0;");

        VastFractionalProgressTracker tracker1 = trackers.get(1);
        assertThat(tracker1.trackingFraction()).isEqualTo(0.25f);
        assertThat(tracker1.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=26;ecn1=1;etm1=0;");

        VastFractionalProgressTracker tracker2 = trackers.get(2);
        assertThat(tracker2.trackingFraction()).isEqualTo(0.25f);
        assertThat(tracker2.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.2;sz=1x1;ord=2922389?");

        VastFractionalProgressTracker tracker3 = trackers.get(3);
        assertThat(tracker3.trackingFraction()).isEqualTo(0.5f);
        assertThat(tracker3.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=18;ecn1=1;etm1=0;");

        VastFractionalProgressTracker tracker4 = trackers.get(4);
        assertThat(tracker4.trackingFraction()).isEqualTo(0.5f);
        assertThat(tracker4.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.3;sz=1x1;ord=2922389?");

        VastFractionalProgressTracker tracker5 = trackers.get(5);
        assertThat(tracker5.trackingFraction()).isEqualTo(0.75f);
        assertThat(tracker5.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;timestamp=2922389;eid1=27;ecn1=1;etm1=0;");

        VastFractionalProgressTracker tracker6 = trackers.get(6);
        assertThat(tracker6.trackingFraction()).isEqualTo(0.75f);
        assertThat(tracker6.getTrackingUrl()).isEqualTo("http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.4;sz=1x1;ord=2922389?");
    }

    @Test
    public void getVideoCompleteTrackers_withLinearXmlManager_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getAdXmlManagers().get(0).getInLineXmlManager().getLinearXmlManagers().get(0).getVideoCompleteTrackers()))
                .containsOnly("http://ad.doubleclick.net/activity;src=2215309;met=1;v=1;" +
                                "pid=47414672;aid=223626102;ko=0;cid=30477563;rid=30495440;rv=1;" +
                                "timestamp=2922389;eid1=13;ecn1=1;etm1=0;",
                        "http://ad.doubleclick.net/ad/N270.Process_Other/B3473145.5;sz=1x1;" +
                                "ord=2922389?");
    }

    @Test
    public void getVideoCloseTrackers_withLinearXmlManager_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getLinearXmlManagers().get(0).getVideoCloseTrackers()))
                .containsOnly("http://www.mopub.com/close?q=ignatius",
                        "http://www.mopub.com/close?q=j3");
    }

    @Test
    public void getVideoSkipTrackers_withLinearXmlManager_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getLinearXmlManagers().get(0).getVideoSkipTrackers()))
                .containsOnly("http://www.mopub.com/skip?q=ignatius",
                        "http://www.mopub.com/skip?q=j3");
    }

    @Test
    public void getClickThroughUrl_withLinearXmlManager_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getAdXmlManagers().get(0)
                .getInLineXmlManager().getLinearXmlManagers().get(0).getClickThroughUrl();

        assertThat(url).isEqualTo("http://www.google.com/support/richmedia");
    }

    @Test
    public void getClickTrackers_withLinearXmlManager_shouldReturnTheCorrectValue() {
        assertThat(VastUtils.vastTrackersToStrings(mXmlManager.getAdXmlManagers().get(0).getInLineXmlManager().getLinearXmlManagers().get(0).getClickTrackers()))
                .containsOnly("http://ad.doubleclick.net/click%3Bh%3Dv8/3e1b/3/0/%2a/z%3B223626102%3B0-0%3B0%3B47414672%3B255-0/0%3B30477563/30495440/1%3B%3B%7Eaopt%3D0/0/ff/0%3B%7Esscs%3D%3fhttp://s0.2mdn.net/dot.gif",
                        "http://ad.doubleclick.net/clk;212442087;33815766;i?http://www.google.com/support/richmedia");
    }

    @Test
    public void getSkipOffset_withLinearXmlManager_shouldReturnTheCorrectValue() {
        String skipOffset = mXmlManager.getAdXmlManagers().get(0).getInLineXmlManager()
                .getLinearXmlManagers().get(0).getSkipOffset();

        assertThat(skipOffset).isEqualTo("25%");
    }

    @Test
    public void getMediaFileUrl_withMediaXmlManager_shouldReturnTheCorrectValue() {
        String url = mXmlManager.getAdXmlManagers().get(0).getInLineXmlManager()
                .getLinearXmlManagers().get(0).getMediaXmlManagers().get(0).getMediaUrl();

        assertThat(url).isEqualTo("http://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4");
    }
}
