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
public class VastMediaXmlManagerTest {

    private VastMediaXmlManager subject;
    private Node mediaNode;

    @Before
    public void setup() throws Exception {
        String mediaXml = "<MediaFile id=\"1\" delivery=\"progressive\" type=\"video/quicktime\" " +
                "bitrate=\"457\"" +
                "               width=\"300\" height=\"225\">" +
                "        <![CDATA[" +
                "        https://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4" +
                "        ]]>" +
                "</MediaFile>";

        mediaNode = createNode(mediaXml);
        subject = new VastMediaXmlManager(mediaNode);
    }

    @Test
    public void getWidth_shouldReturnWidthAttribute() {
        assertThat(subject.getWidth()).isEqualTo(300);
    }

    @Test
    public void getWidth_withNoWidthAttribute_shouldReturnNull() throws Exception {
        String mediaXml = "<MediaFile id=\"1\" delivery=\"progressive\" type=\"video/quicktime\" " +
                "bitrate=\"457\" height=\"225\">" +
                "        <![CDATA[" +
                "        https://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4" +
                "        ]]>" +
                "</MediaFile>";

        mediaNode = createNode(mediaXml);
        subject = new VastMediaXmlManager(mediaNode);

        assertThat(subject.getWidth()).isNull();
    }

    @Test
    public void getHeight_shouldReturnHeightAttribute() {
        assertThat(subject.getHeight()).isEqualTo(225);
    }

    @Test
    public void getHeight_withNoHeightAttribute_shouldReturnNull() throws Exception {
        String mediaXml = "<MediaFile id=\"1\" delivery=\"progressive\" type=\"video/quicktime\" " +
                "bitrate=\"457\" width=\"300\">" +
                "        <![CDATA[" +
                "        https://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4" +
                "        ]]>" +
                "</MediaFile>";

        mediaNode = createNode(mediaXml);
        subject = new VastMediaXmlManager(mediaNode);

        assertThat(subject.getHeight()).isNull();
    }

    @Test
    public void getType_shouldReturnMediaFileType() {
        assertThat(subject.getType()).isEqualTo("video/quicktime");
    }

    @Test
    public void getType_withNoTypeAttribute_shouldReturnNull() throws Exception {
        String mediaXml = "<MediaFile id=\"1\" delivery=\"progressive\" " +
                "bitrate=\"457\" width=\"300\" height=\"225\">" +
                "        <![CDATA[" +
                "        https://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4" +
                "        ]]>" +
                "</MediaFile>";

        mediaNode = createNode(mediaXml);
        subject = new VastMediaXmlManager(mediaNode);

        assertThat(subject.getType()).isNull();
    }

    @Test
    public void getMediaUrl_shouldReturnMediaFileUrl() {
        assertThat(subject.getMediaUrl()).isEqualTo("https://s3.amazonaws.com/uploads.hipchat" +
                ".com/10627/429509/t8hqeqf98nvtir7/big_buck_bunny.mp4");
    }

    @Test
    public void getMediaUrl_withNoMediaUrl_shouldReturnNull() throws Exception {
        String mediaXml = "<MediaFile id=\"1\" delivery=\"progressive\" type=\"video/quicktime\" " +
                "bitrate=\"457\" width=\"300\" height=\"225\">" +
                "</MediaFile>";

        mediaNode = createNode(mediaXml);
        subject = new VastMediaXmlManager(mediaNode);

        assertThat(subject.getMediaUrl()).isNull();
    }
}
