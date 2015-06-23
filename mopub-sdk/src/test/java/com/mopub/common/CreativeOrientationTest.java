package com.mopub.common;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class CreativeOrientationTest {

    @Test
    public void fromHeader_nullParam_shouldBeUndefined() {
        assertThat(CreativeOrientation.fromHeader(null)).isEqualTo(CreativeOrientation.UNDEFINED);
    }

    @Test
    public void fromHeader_emptyParam_shouldBeUndefined() {
        assertThat(CreativeOrientation.fromHeader("")).isEqualTo(CreativeOrientation.UNDEFINED);
    }

    @Test
    public void fromHeader_withGarbage_shouldBeUndefined() {
        assertThat(CreativeOrientation.fromHeader("p0rtr41t")).isEqualTo(CreativeOrientation.UNDEFINED);
    }

    @Test
    public void fromHeader_lParam_shouldBeLandscape() {
        assertThat(CreativeOrientation.fromHeader("l")).isEqualTo(CreativeOrientation.LANDSCAPE);
    }

    @Test
    public void fromHeader_uppercaseL_shouldBeLandscape() {
        assertThat(CreativeOrientation.fromHeader("L")).isEqualTo(CreativeOrientation.LANDSCAPE);
    }

    @Test
    public void fromHeader_pParam_shouldBePortrait() {
        assertThat(CreativeOrientation.fromHeader("p")).isEqualTo(CreativeOrientation.PORTRAIT);
    }

    @Test
    public void fromHeader_uppercaseP_shouldBePortrait() {
        assertThat(CreativeOrientation.fromHeader("P")).isEqualTo(CreativeOrientation.PORTRAIT);
    }
}
