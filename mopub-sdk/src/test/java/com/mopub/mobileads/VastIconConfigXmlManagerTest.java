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
public class VastIconConfigXmlManagerTest {

    private VastIconXmlManager subject;
    private Node iconNode;

    @Before
    public void setup() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                    "<StaticResource creativeType=\"ImAge/JpEg\">" +
                        "<![CDATA[imageJpeg]]>" +
                    "</StaticResource>" +
                    "<IconClicks>" +
                        "<IconClickThrough>" +
                            "<![CDATA[clickThroughUri]]>" +
                        "</IconClickThrough>" +
                        "<IconClickTracking>" +
                            "<![CDATA[clickTrackingUri1]]>" +
                        "</IconClickTracking>" +
                        "<IconClickTracking>" +
                            "<![CDATA[clickTrackingUri2]]>" +
                        "</IconClickTracking>" +
                    "</IconClicks>" +
                    "<IconViewTracking>" +
                        "<![CDATA[viewTrackingUri1]]>" +
                    "</IconViewTracking>" +
                    "<IconViewTracking>" +
                        "<![CDATA[viewTrackingUri2]]>" +
                    "</IconViewTracking>" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
    }

    @Test
    public void getWidth_shouldReturnWidth() throws Exception {
        assertThat(subject.getWidth()).isEqualTo(123);
    }

    @Test
    public void getWidth_withNoWidth_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getWidth()).isNull();
    }

    @Test
    public void getHeight_shouldReturnHeight() throws Exception {
        assertThat(subject.getHeight()).isEqualTo(456);
    }

    @Test
    public void getHeight_withNoHeight_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getHeight()).isNull();
    }

    @Test
    public void getOffsetMS_shouldReturnOffset() throws Exception {
        assertThat(subject.getOffsetMS()).isEqualTo(3723000);
    }

    @Test
    public void getOffsetMS_withNoOffset_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" " +
                "duration=\"01:02:03.456\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getOffsetMS()).isNull();
    }

    @Test
    public void getOffsetMS_withMalformedOffset_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"malformed\" " +
                "duration=\"01:02:03.456\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getOffsetMS()).isNull();
    }

    @Test
    public void getDurationMS_shouldReturnDuration() throws Exception {
        assertThat(subject.getDurationMS()).isEqualTo(3723456);
    }

    @Test
    public void getDurationMS_withNoDuration_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" >" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getDurationMS()).isNull();
    }

    @Test
    public void getDurationMS_withMalformedDuration_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"malformed\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getDurationMS()).isNull();
    }

    @Test
    public void getResourceXmlManager_shouldReturnVastResourceXmlManager() throws Exception {
        VastResourceXmlManager resourceXmlManager = subject.getResourceXmlManager();
        assertThat(resourceXmlManager.getStaticResource()).isEqualTo("imageJpeg");
        assertThat(resourceXmlManager.getStaticResourceType()).isEqualTo("image/jpeg");
    }

    @Test
    public void getClickTrackingUris_shouldReturnClickTrackingUris() throws Exception {
        assertThat(VastUtils.vastTrackersToStrings(subject.getClickTrackingUris()))
                .containsOnly("clickTrackingUri1", "clickTrackingUri2");
    }

    @Test
    public void getClickTrackingUris_withNoClickTrackingUris_shouldReturnEmptyList() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                    "<IconClicks>" +
                        "<IconClickThrough>" +
                            "<![CDATA[clickThroughUri]]>" +
                        "</IconClickThrough>" +
                    "</IconClicks>" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getClickTrackingUris()).isEmpty();
    }

    @Test
    public void getClickThroughUri_shouldReturnClickThroughUri() throws Exception {
        assertThat(subject.getClickThroughUri()).isEqualTo("clickThroughUri");
    }

    @Test
    public void getClickThroughUri_withNoClickThroughUri_shouldReturnNull() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                "<IconClicks>" +
                    "<IconClickTracking>" +
                        "<![CDATA[clickTrackingUri1]]>" +
                    "</IconClickTracking>" +
                    "<IconClickTracking>" +
                        "<![CDATA[clickTrackingUri2]]>" +
                    "</IconClickTracking>" +
                "</IconClicks>" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getClickThroughUri()).isNull();
    }

    @Test
    public void getViewTrackingUris_shouldReturnViewTrackingUris() throws Exception {
        assertThat(VastUtils.vastTrackersToStrings(subject.getViewTrackingUris()))
                .containsOnly("viewTrackingUri1", "viewTrackingUri2");
    }

    @Test
    public void getViewTrackingUris_withNoViewTrackingUris_shouldReturnEmptyList() throws Exception {
        String iconXml = "<Icon program=\"program\" width=\"123\" height=\"456\" xPosition=\"789\" " +
                "yPosition=\"101\" apiFramework=\"apiFramework\" offset=\"01:02:03\" " +
                "duration=\"01:02:03.456\">" +
                "</Icon>";

        iconNode = createNode(iconXml);
        subject = new VastIconXmlManager(iconNode);
        assertThat(subject.getViewTrackingUris()).isEmpty();
    }
}
