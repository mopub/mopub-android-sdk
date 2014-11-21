package com.mopub.common;


import com.mopub.common.Preconditions.NoThrow;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class)
public class PreconditionsTest {

    @Before
    public void setUp() {
        NoThrow.setStrictMode(false);
    }
    
    @Test
    public void checkArgument_success_shouldNotThrow() {
        Preconditions.checkArgument(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkArgument_failure_shouldThrowIllegalArgumentException() {
        Preconditions.checkArgument(false);
    }

    @Test
    public void checkArgument_failure_withMessage_shouldThrowIllegalArgumentException() {
        try {
            Preconditions.checkArgument(false, "message");
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void checkArgument_failure_withNullMessage_shouldThrowIllegalArgumentException() {
        try {
            Preconditions.checkArgument(false, null);
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void checkArgument_failure_withComplexMessage_shouldThrowIllegalArgumentException() {
        try {
            Preconditions.checkArgument(false, "I %s fail", "should");
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void checkState_success_shouldNotThrow() {
        Preconditions.checkArgument(true);
    }

    @Test(expected = IllegalStateException.class)
    public void checkState_failure_shouldThrowIllegalStateException() {
        Preconditions.checkState(false);
    }

    @Test
    public void checkState_failure_withMessage_shouldThrowIllegalStateException() {
        try {
            Preconditions.checkState(false, "message");
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void checkState_failure_withNullMessage_shouldThrowIllegalStateException() {
        try {
            Preconditions.checkState(false, null);
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void checkState_failure_withComplexMessage_shouldThrowIllegalStateException() {
        try {
            Preconditions.checkState(false, "I %s fail", "should");
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void checkNotNull_success_shouldNotThrow() {
        Preconditions.checkNotNull(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void checkNotNull_failure_shouldThrowNullPointerException() {
        Preconditions.checkNotNull(null);
    }

    @Test
    public void checkNotNull_failure_withMessage_shouldThrowNullPointerException() {
        try {
            Preconditions.checkNotNull(null, "message");
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void checkNotNull_failure_withNullMessage_shouldThrowNullPointerException() {
        try {
            Preconditions.checkNotNull(null, null);
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void checkNotNull_failure_withComplexMessage_shouldThrowNullPointerException() {
        try {
            Preconditions.checkNotNull(null, "I %s fail", "should");
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void checkUiThread_success_shouldNotThrow() {
        Preconditions.checkUiThread();
    }

    @Test
    public void noThrow_checkArgument_success_shouldReturnTrue() {
        assertThat(NoThrow.checkArgument(true)).isTrue();
    }

    @Test
    public void noThrow_checkArgument_failure_shouldReturnFalse() {
        assertThat(NoThrow.checkArgument(false)).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void noThrow_strictMode_checkArgument_failure_shouldThrowIllegalArgumentException() {
        NoThrow.setStrictMode(true);
        NoThrow.checkArgument(false);
    }

    @Test
    public void noThrow_strictMode_checkArgument_failure_withMessage_shouldThrowIllegalArgumentException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkArgument(false, "message");
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void noThrow_strictMode_checkArgument_failure_withNullMessage_shouldThrowIllegalArgumentException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkArgument(false, null);
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void noThrow_strictMode_checkArgument_failure_withComplexMessage_shouldThrowIllegalArgumentException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkArgument(false, "I %s fail", "should");
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void noThrow_checkState_success_shouldReturnTrue() {
        assertThat(NoThrow.checkArgument(true)).isTrue();
    }

    @Test
    public void noThrow_checkState_failure_shouldReturnFalse() {
        assertThat(NoThrow.checkState(false)).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void noThrow_strictMode_checkState_failure_shouldThrowIllegalStateException() {
        NoThrow.setStrictMode(true);
        NoThrow.checkState(false);
    }

    @Test
    public void noThrow_strictMode_checkState_failure_withMessage_shouldThrowIllegalStateException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkState(false, "message");
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void noThrow_strictMode_checkState_failure_withNullMessage_shouldThrowIllegalStateException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkState(false, null);
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void noThrow_strictMode_checkState_failure_withComplexMessage_shouldThrowIllegalStateException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkState(false, "I %s fail", "should");
            fail("no exception thrown");
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void noThrow_checkNotNull_success_shouldReturnTrue() {
        assertThat(NoThrow.checkNotNull(new Object())).isTrue();
    }

    @Test
    public void noThrow_checkNotNull_failure_shouldReturnFalse() {
        assertThat(NoThrow.checkNotNull(null)).isFalse();
    }

    @Test(expected = NullPointerException.class)
    public void noThrow_strictMode_checkNotNull_failure_shouldThrowNullPointerException() {
        NoThrow.setStrictMode(true);
        NoThrow.checkNotNull(null);
    }

    @Test
    public void noThrow_strictMode_checkNotNull_failure_withMessage_shouldThrowNullPointerException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkNotNull(null, "message");
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("message");
        }
    }

    @Test
    public void noThrow_strictMode_checkNotNull_failure_withNullMessage_shouldThrowNullPointerException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkNotNull(null, null);
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("null");
        }
    }

    @Test
    public void noThrow_strictMode_checkNotNull_failure_withComplexMessage_shouldThrowNullPointerException() {
        NoThrow.setStrictMode(true);
        try {
            NoThrow.checkNotNull(null, "I %s fail", "should");
            fail("no exception thrown");
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage()).isEqualTo("I should fail");
        }
    }

    @Test
    public void noThrow_checkUiThread_success_shouldReturnTrue() {
        assertThat(NoThrow.checkUiThread()).isTrue();
    }

    @Test
    public void checkArgument_failure_withInvalidMessage_shouldThrowIllegalArgumentException() {
        try {
            Preconditions.checkArgument(false, "messages: ", "message1", "message2");
            fail("no exception thrown");
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).isEqualTo("messages: ");
        }
    }
}
