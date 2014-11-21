package com.mopub.common.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class StringsTest {
    @Test
    public void isEmpty_shouldReturnValidResponse() throws Exception {
        assertThat(Strings.isEmpty("")).isTrue();

        assertThat(Strings.isEmpty("test")).isFalse();

        assertThat(Strings.isEmpty(null)).isFalse();
    }
}
