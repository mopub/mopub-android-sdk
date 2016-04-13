package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Node;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastCompanionAdConfigXmlManagerTest {

    private VastCompanionAdXmlManager subject;
    private Node companionNode;

    @Before
    public void setup() throws Exception {
        String companionXml = "<Companion id=\"valid\" height=\"250\" width=\"300\">" +
                "    <StaticResource creativeType=\"image/png\">https://pngimage</StaticResource>" +
                "    <TrackingEvents>" +
                "        <Tracking event=\"creativeView\">https://tracking/creativeView1</Tracking>" +
                "        <Tracking event=\"creativeView\">https://tracking/creativeView2</Tracking>" +
                "        <Tracking event=\"creativeView\">https://tracking/creativeView3</Tracking>" +
                "    </TrackingEvents>" +
                "    <CompanionClickThrough>https://clickthrough</CompanionClickThrough>" +
                "    <CompanionClickThrough>https://second_clickthrough</CompanionClickThrough>" +
                "    <CompanionClickTracking>" +
                "        <![CDATA[https://clicktrackingOne]]>" +
                "    </CompanionClickTracking>" +
                "    <CompanionClickTracking>" +
                "        <![CDATA[https://clicktrackingTwo]]>" +
                "    </CompanionClickTracking>" +
                "    <RandomUnusedTag>This_is_unused</RandomUnusedTag>" +
                "</Companion>";

        companionNode = createNode(companionXml);
        subject = new VastCompanionAdXmlManager(companionNode);
    }

    @Test
    public void getWidth_shouldReturnWidthAttributes() {
        assertThat(subject.getWidth()).isEqualTo(300);
    }

    @Test
    public void getWidth_withNoWidthAttribute_shouldReturnNull() throws Exception {
        String companionXml = "<Companion id=\"valid\" height=\"250\">" +
                "</Companion>";

        companionNode = createNode(companionXml);
        subject = new VastCompanionAdXmlManager(companionNode);

        assertThat(subject.getWidth()).isNull();
    }

    @Test
    public void getHeight_shouldReturnHeightAttributes() {
        assertThat(subject.getHeight()).isEqualTo(250);
    }

    @Test
    public void getHeight_withNoHeightAttribute_shouldReturnNull() throws Exception {
        String companionXml = "<Companion id=\"valid\" width=\"300\">" +
                "</Companion>";

        companionNode = createNode(companionXml);
        subject = new VastCompanionAdXmlManager(companionNode);

        assertThat(subject.getHeight()).isNull();
    }

    @Test
    public void getResourceXmlManager_shouldReturnVastResourceXmlManager() throws Exception {
        VastResourceXmlManager resourceXmlManager = subject.getResourceXmlManager();
        assertThat(resourceXmlManager.getStaticResource()).isEqualTo("https://pngimage");
        assertThat(resourceXmlManager.getStaticResourceType()).isEqualTo("image/png");
    }

    @Test
    public void getClickThroughUrl_shouldReturnFirstStringUrl() {
        assertThat(subject.getClickThroughUrl()).isEqualTo("https://clickthrough");
    }

    @Test
    public void getClickTrackers_shouldReturnAllUrls() {
        assertThat(VastUtils.vastTrackersToStrings(subject.getClickTrackers()))
                .containsOnly("https://clicktrackingOne",
                        "https://clicktrackingTwo");
    }
}
