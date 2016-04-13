package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Node;

import java.util.List;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastBaseInLineWrapperXmlManagerTest {
    private VastBaseInLineWrapperXmlManager subject;

    @Test
    public void getImpressionTrackers_shouldReturnImpressionTrackers() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/close]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(VastUtils.vastTrackersToStrings(subject.getImpressionTrackers()))
                .containsOnly("https://impression/m/inlineOne", "https://impression/m/inlineTwo");
    }

    @Test
    public void getImpressionTrackers_withNoImpressionTrackers_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getImpressionTrackers()).isEmpty();
    }

    @Test
    public void getLinearXmlManagers_shouldReturnLinearXmlManagers() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/closeOne]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "                       <!-- There should only be a single Linear, CompanionAds, or NonLinearAds element per Creative -->" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/ignored]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "                 </Creative>" +
                "                 <Creative>" +
                "                       <NonLinearAds>" +
                "                       </NonLinearAds>" +
                "                 </Creative>" +
                "                 <Creative>" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/closeTwo]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        List<VastLinearXmlManager> linearXmlManagers = subject.getLinearXmlManagers();
        assertThat(linearXmlManagers).hasSize(2);
        assertThat(VastUtils.vastTrackersToStrings(linearXmlManagers.get(0).getVideoCloseTrackers()))
                .containsOnly("https://tracking/m/closeOne");
        assertThat(VastUtils.vastTrackersToStrings(linearXmlManagers.get(1).getVideoCloseTrackers()))
                .containsOnly("https://tracking/m/closeTwo");
    }

    @Test
    public void getLinearXmlManagers_withNoLinearNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                       <NonLinearAds>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/close]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </NonLinearAds>" +
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getLinearXmlManagers_withNoCreativeNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "          <Creatives>" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/closeTwo]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getLinearXmlManagers_withNoCreativesNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "                 <Creative>" +
                "                       <Linear>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/closeTwo]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </Linear>" +
                "                 </Creative>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getCompanionAdXmlManagers_shouldReturnCompanionAdXmlManagers() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[https://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                       <NonLinearAds>" +
                "                           <TrackingEvents>" +
                "                               <Tracking event=\"close\">" +
                "                                   <![CDATA[https://tracking/m/closeOne]]>" +
                "                               </Tracking>" +
                "                           </TrackingEvents>" +
                "                       </NonLinearAds>" +
                "                 </Creative>" +
                "                 <Creative>" +
                "                     <CompanionAds>" +
                "                         <Companion>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeTwo]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                             <CompanionClickTracking>" +
                "                                 <![CDATA[https://clicktrackingOne]]>" +
                "                             </CompanionClickTracking>" +
                "                         </Companion>"+
                "                         <Companion>" +
                "                             <CompanionClickTracking>" +
                "                                 <![CDATA[https://clicktrackingTwo]]>" +
                "                             </CompanionClickTracking>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeThree]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                         </Companion>"+
                "                     </CompanionAds>" +
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getCompanionAdXmlManagers()).hasSize(2);
        assertThat(VastUtils.vastTrackersToStrings(subject.getCompanionAdXmlManagers().get(0).getClickTrackers()))
                .containsOnly("https://clicktrackingOne");
        assertThat(VastUtils.vastTrackersToStrings(subject.getCompanionAdXmlManagers().get(1).getClickTrackers()))
                .containsOnly("https://clicktrackingTwo");
    }

    @Test
    public void getCompanionAdXmlManagers_withNoCompanionNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                     <CompanionAds>" +
                "                         <NotACompanion>" +
                "                         </NotACompanion>"+
                "                     </CompanionAds>" +
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getCompanionAdXmlManagers_withNoCompanionAdsNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Creatives>" +
                "                 <Creative>" +
                "                         <Companion>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeThree]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                         </Companion>"+
                "                 </Creative>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getCompanionAdXmlManagers_withNoCreativeNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "          <Creatives>" +
                "                     <CompanionAds>" +
                "                         <Companion>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeThree]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                         </Companion>"+
                "                     </CompanionAds>" +
                "          </Creatives>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getCompanionAdXmlManagers_withNoCreativesNodes_shouldReturnEmptyList() throws Exception {
        String inLineXml = "<InLine>" +
                "                 <Creative>" +
                "                     <CompanionAds>" +
                "                         <Companion>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeThree]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                         </Companion>"+
                "                     </CompanionAds>" +
                "                 </Creative>" +
                "</InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getLinearXmlManagers()).isEmpty();
    }

    @Test
    public void getVastExtensionsXmlManager_shouldReturnExtensions() throws Exception {
        String inLineXml = "<InLine>" +
                "               <Extensions>" +
                "                   <Extension>Extension 1</Extension>" +
                "                   <Extension>Extension 2</Extension>" +
                "               </Extensions>" +
                "           </InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getVastExtensionParentXmlManager()).isNotNull();
        assertThat(subject.getVastExtensionParentXmlManager().getVastExtensionXmlManagers())
                .hasSize(2);
    }

    @Test
    public void getVastExtensionParentXmlManager_withMultipleParentExtensions_shouldReturnFirstParentExtensionOnly() throws Exception {
        String inLineXml = "<InLine>" +
                "               <Extensions>" +
                "                   <Extension>Extension 1</Extension>" +
                "               </Extensions>" +
                "               <Extensions>" +
                "                   <Extension>Extension 2</Extension>" +
                "                   <Extension>Extension 3</Extension>" +
                "               </Extensions>" +
                "           </InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getVastExtensionParentXmlManager()).isNotNull();
        assertThat(subject.getVastExtensionParentXmlManager().getVastExtensionXmlManagers())
                .hasSize(1);
    }

    @Test
    public void getVastExtensionParentXmlManager_withoutExtensions_shouldReturnNull() throws Exception {
        String inLineXml = "<InLine>" +
                "                 <Creative>" +
                "                     <CompanionAds>" +
                "                         <Companion>" +
                "                             <TrackingEvents>" +
                "                                 <Tracking event=\"creativeView\">" +
                "                                     <![CDATA[https://tracking/m/closeThree]]>" +
                "                                 </Tracking>" +
                "                             </TrackingEvents>" +
                "                         </Companion>"+
                "                     </CompanionAds>" +
                "                 </Creative>" +
                "           </InLine>";

        Node inLineNode = createNode(inLineXml);
        subject = new VastInLineXmlManager(inLineNode);

        assertThat(subject.getVastExtensionParentXmlManager()).isNull();
    }
}
