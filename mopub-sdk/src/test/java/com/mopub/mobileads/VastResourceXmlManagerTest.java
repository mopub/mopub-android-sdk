package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Node;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastResourceXmlManagerTest {

    private VastResourceXmlManager subject;
    private Node resourceNode;

    @Before
    public void setup() throws Exception {
        String resourceXml =
                "<root>" +
                    "<StaticResource creativeType=\"ImAge/JpEg\">" +
                        "<![CDATA[StaticResource]]>" +
                    "</StaticResource>" +
                    "<IFrameResource>" +
                        "<![CDATA[IFrameResource]]>" +
                    "</IFrameResource>" +
                    "<HTMLResource>" +
                        "<![CDATA[HTMLResource]]>" +
                    "</HTMLResource>" +
                "</root>";

        resourceNode = createNode(resourceXml);
        subject = new VastResourceXmlManager(resourceNode);
    }

    @Test
    public void getStaticResource_shouldReturnStaticResource() throws Exception {
        assertThat(subject.getStaticResource()).isEqualTo("StaticResource");
    }

    @Test
    public void getStaticResource_withNoStaticResource_shouldReturnNull() throws Exception {
        String iconXml = "<root>" +
                    "<IFrameResource>" +
                        "<![CDATA[IFrameResource]]>" +
                    "</IFrameResource>" +
                    "<HTMLResource>" +
                        "<![CDATA[HTMLResource]]>" +
                    "</HTMLResource>" +
                "</root>";

        resourceNode = createNode(iconXml);
        subject = new VastResourceXmlManager(resourceNode);
        assertThat(subject.getStaticResource()).isNull();
    }

    @Test
    public void getStaticResourceType_shouldReturnLowerCaseStaticResourceType() throws Exception {
        assertThat(subject.getStaticResourceType()).isEqualTo("image/jpeg");
    }

    @Test
    public void getStaticResourceType_withNoStaticCreativeType_shouldReturnNull() throws Exception {
        String resourceXml = "<root>" +
                    "<StaticResource>" +
                        "<![CDATA[StaticResource]]>" +
                    "</StaticResource>" +
                    "<IFrameResource>" +
                        "<![CDATA[IFrameResource]]>" +
                    "</IFrameResource>" +
                    "<HTMLResource>" +
                        "<![CDATA[HTMLResource]]>" +
                    "</HTMLResource>" +
                "</root>";

        resourceNode = createNode(resourceXml);
        subject = new VastResourceXmlManager(resourceNode);
        assertThat(subject.getStaticResourceType()).isNull();
    }

    @Test
    public void getIFrameResource_shouldReturnIFrameResource() throws Exception {
        assertThat(subject.getIFrameResource()).isEqualTo("IFrameResource");
    }

    @Test
    public void getIFrameResource_withNoIFrameResouce_shouldReturnNull() throws Exception {
        String resourceXml = "<root>" +
                    "<StaticResource creativeType=\"ImAge/JpEg\">" +
                        "<![CDATA[StaticResource]]>" +
                    "</StaticResource>" +
                    "<HTMLResource>" +
                        "<![CDATA[HTMLResource]]>" +
                    "</HTMLResource>" +
                "</root>";

        resourceNode = createNode(resourceXml);
        subject = new VastResourceXmlManager(resourceNode);
        assertThat(subject.getIFrameResource()).isNull();
    }

    @Test
    public void getHTMLResource_shouldReturnHTMLResource() throws Exception {
        assertThat(subject.getHTMLResource()).isEqualTo("HTMLResource");
    }

    @Test
    public void getHTMLResource_withNoHTMLResource_shouldReturnNull() throws Exception {
        String resourceXml = "<root>" +
                    "<StaticResource creativeType=\"ImAge/JpEg\">" +
                        "<![CDATA[StaticResource]]>" +
                    "</StaticResource>" +
                    "<IFrameResource>" +
                        "<![CDATA[IFrameResource]]>" +
                    "</IFrameResource>" +
                "</root>";

        resourceNode = createNode(resourceXml);
        subject = new VastResourceXmlManager(resourceNode);
        assertThat(subject.getHTMLResource()).isNull();
    }

}
