package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.w3c.dom.Node;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastWrapperXmlManagerTest {

    private VastWrapperXmlManager subject;

    @Test
    public void getVastAdTagURI_shouldReturnStringURI() throws Exception {
        String wrapperXml = "<Wrapper>" +
                "    <Impression id=\"DART\">" +
                "        <![CDATA[https://impression/m/wrapperOne]]>" +
                "    </Impression>" +
                "    <VASTAdTagURI>https://redirecturl/xml</VASTAdTagURI>" +
                "</Wrapper>";

        Node wrapperNode = createNode(wrapperXml);
        subject = new VastWrapperXmlManager(wrapperNode);

        assertThat(subject.getVastAdTagURI()).isEqualTo("https://redirecturl/xml");
    }

    @Test
    public void getVastAdTagURI_withNoVastAdTagURI_shouldReturnNull() throws Exception {
        String wrapperXml = "<Wrapper>" +
                "    <Impression id=\"DART\">" +
                "        <![CDATA[https://impression/m/wrapperOne]]>" +
                "    </Impression>" +
                "</Wrapper>";

        Node wrapperNode = createNode(wrapperXml);
        subject = new VastWrapperXmlManager(wrapperNode);

        assertThat(subject.getVastAdTagURI()).isNull();
    }
}
