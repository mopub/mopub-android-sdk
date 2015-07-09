package com.mopub.common.util;

import android.app.Activity;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class NumbersTest {
    @Test
    public void parseDouble_withNumberValue_shouldReturnDouble() throws Exception {
        int anInt = 2;
        assertThat(Numbers.parseDouble(anInt)).isEqualTo(anInt);

        double aDouble = 2.1;
        assertThat(Numbers.parseDouble(aDouble)).isEqualTo(aDouble);

        float aFloat = 2.2f;
        assertThat(Numbers.parseDouble(aFloat)).isEqualTo(aFloat);

        double nan = Double.NaN;
        assertThat(Numbers.parseDouble(nan)).isEqualTo(nan);
    }

    @Test
    public void parseDouble_withStringValue_shouldReturnDouble() throws Exception {
        assertThat(Numbers.parseDouble("0.01")).isEqualTo(0.01);
        assertThat(Numbers.parseDouble("-1015")).isEqualTo(-1015);
    }

    @Test
    public void parseDouble_withInvalidStringValue_shouldThrowClassCastException() throws Exception {
        try {
            Numbers.parseDouble("dog");
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // pass
        }

        try {
            Numbers.parseDouble("123a");
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // pass
        }
    }

    @Test
    public void parseDouble_withInvalidObjectType_shouldThrowClassCastException() throws Exception {
        try {
            Numbers.parseDouble(mock(Activity.class));
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // pass
        }
    }

    @Test
    public void convertMillisecondsToSecondsRoundedUp_shouldReturnLongMillisecondsAsRoundedUpLongSeconds() throws Exception {
        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(0)).isEqualTo(0);

        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(1)).isEqualTo(1);
        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(999)).isEqualTo(1);
        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(1000)).isEqualTo(1);

        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(1001)).isEqualTo(2);
        assertThat(Numbers.convertMillisecondsToSecondsRoundedUp(100000)).isEqualTo(100);
    }
}
