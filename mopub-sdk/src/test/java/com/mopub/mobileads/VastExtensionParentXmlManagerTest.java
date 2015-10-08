package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.VastUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import java.util.List;

import static com.mopub.mobileads.test.support.VastUtils.createNode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastExtensionParentXmlManagerTest {
    private VastExtensionParentXmlManager subject;

    @Test
    public void getVastExtensionXmlManagers_shouldReturnExtensionManagers() throws Exception {
        String parentExtensionXml = "<Extensions>" +
                "                       <Extension>Extension 1</Extension>" +
                "                       <Extension>Extension 2</Extension>" +
                "                    </Extensions>";

        subject = new VastExtensionParentXmlManager(createNode(parentExtensionXml));

        assertThat(subject.getVastExtensionXmlManagers()).isNotNull();
        assertThat(subject.getVastExtensionXmlManagers()).hasSize(2);
    }

    @Test
    public void getVastExtensionXmlManagers_withoutExtensions_shouldReturnEmptyList() throws Exception {
        String parentExtensionXml = "<Extensions></Extensions>";

        subject = new VastExtensionParentXmlManager(createNode(parentExtensionXml));

        assertThat(subject.getVastExtensionXmlManagers()).isNotNull();
        assertThat(subject.getVastExtensionXmlManagers()).isEmpty();
    }

}
