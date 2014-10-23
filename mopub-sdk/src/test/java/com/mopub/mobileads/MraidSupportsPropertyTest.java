package com.mopub.mobileads;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class MraidSupportsPropertyTest {

    @Test
    public void MraidSupportsProperty_toJsonPair_should_returnTheSupportsJsonObject() throws Exception {
        MraidSupportsProperty property =
                new MraidSupportsProperty()
                        .withSms(true)
                        .withTel(true)
                        .withCalendar(false)
                        .withStorePicture(false)
                        .withInlineVideo(false);
        assertThat(property.toJsonPair()).isEqualTo("supports: {" +
                "sms: true, " +
                "tel: true, " +
                "calendar: false, " +
                "storePicture: false, " +
                "inlineVideo: false}");
    }

    @Test
    public void MraidSupportsProperty_toJSONPair_should_returnAllDefaultPairValuesAsFalse() throws Exception {
        MraidSupportsProperty property =
                new MraidSupportsProperty().withCalendar(true).withInlineVideo(true);
        assertThat(property.toJsonPair()).isEqualTo("supports: {" +
                "sms: false, " +
                "tel: false, " +
                "calendar: true, " +
                "storePicture: false, " +
                "inlineVideo: true}");
    }
}
