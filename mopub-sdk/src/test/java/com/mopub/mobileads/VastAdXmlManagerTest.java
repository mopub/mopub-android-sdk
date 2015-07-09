package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastAdXmlManagerTest {

    private VastAdXmlManager subject;

    @Test
    public void getInLineXmlManager_shouldReturnInLineXmlManager() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inline]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapper]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        VastInLineXmlManager vastInLineXmlManager = subject.getInLineXmlManager();
        assertThat(VastUtils.vastTrackersToStrings(vastInLineXmlManager.getImpressionTrackers()))
                .containsOnly("http://impression/m/inline");
    }

    @Test
    public void getInLineXmlManager_withMultipleInLineNodes_shouldReturnFirstInLineXmlManager() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inlineTwo]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapper]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        VastInLineXmlManager vastInLineXmlManager = subject.getInLineXmlManager();
        assertThat(VastUtils.vastTrackersToStrings(vastInLineXmlManager.getImpressionTrackers()))
                .containsOnly("http://impression/m/inlineOne");
    }

    @Test
    public void getInLineXmlManager_withNoInLineNodes_shouldReturnNull() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapper]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        assertThat(subject.getInLineXmlManager()).isNull();
    }

    @Test
    public void getWrapperXmlManager_shouldReturnWrapperXmlManager() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inline]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapper]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        VastWrapperXmlManager vastWrapperXmlManager = subject.getWrapperXmlManager();
        assertThat(VastUtils.vastTrackersToStrings(vastWrapperXmlManager.getImpressionTrackers()))
                .containsOnly("http://impression/m/wrapper");
    }

    @Test
    public void getWrapperXmlManager_withMultipleWrapperNodes_shouldReturnFirstWrapperXmlManager() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inlineOne]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapperOne]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "    <Wrapper>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/wrapperTwo]]>" +
                "          </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        VastWrapperXmlManager vastWrapperXmlManager = subject.getWrapperXmlManager();
        assertThat(VastUtils.vastTrackersToStrings(vastWrapperXmlManager.getImpressionTrackers()))
                .containsOnly("http://impression/m/wrapperOne");
    }

    @Test
    public void getWrapperXmlManager_withNoWrapperNodes_shouldReturnNull() throws Exception {
        String adXml = "<Ad id=\"223626102\">" +
                "    <InLine>" +
                "          <Impression id=\"DART\">" +
                "                 <![CDATA[http://impression/m/inline]]>" +
                "          </Impression>" +
                "    </InLine>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        assertThat(subject.getWrapperXmlManager()).isNull();
    }

    @Test
    public void getSequence_shouldReturnSequence() throws Exception {
        String adXml = "<Ad id=\"223626102\" sequence=\"42\">" +
                "    <InLine>" +
                "        <Impression id=\"DART\">" +
                "            <![CDATA[http://impression/m/inline]]>" +
                "        </Impression>" +
                "    </InLine>" +
                "    <Wrapper>" +
                "        <Impression id=\"DART\">" +
                "            <![CDATA[http://impression/m/wrapper]]>" +
                "        </Impression>" +
                "    </Wrapper>" +
                "</Ad>";

        Node adNode = createNode(adXml);
        subject = new VastAdXmlManager(adNode);

        assertThat(subject.getSequence()).isEqualTo("42");
    }
}
