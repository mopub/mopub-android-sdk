package com.mopub.network;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ScribeBackoffPolicyTest {

    private ScribeBackoffPolicy subject;

    @Before
    public void setUp() {
        subject = new ScribeBackoffPolicy();
    }

    @Test
    public void backoff_with503Error_shouldUpdateBackoffTime() throws Exception {
        NetworkResponse networkResponse = new NetworkResponse(503, null, null, false);
        VolleyError volleyError = new VolleyError(networkResponse);

        assertThat(subject.getBackoffMs()).isEqualTo(0);
        assertThat(subject.getRetryCount()).isEqualTo(0);

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(60000);
        assertThat(subject.getRetryCount()).isEqualTo(1);
    }

    @Test
    public void backoff_with504Error_shouldUpdateBackoffTime() throws Exception {
        NetworkResponse networkResponse = new NetworkResponse(504, null, null, false);
        VolleyError volleyError = new VolleyError(networkResponse);

        assertThat(subject.getBackoffMs()).isEqualTo(0);
        assertThat(subject.getRetryCount()).isEqualTo(0);

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(60000);
        assertThat(subject.getRetryCount()).isEqualTo(1);
    }

    @Test
    public void backoff_withNoConnectionError_shouldUpdateBackoffTime() throws Exception {
        VolleyError volleyError = new NoConnectionError();

        assertThat(subject.getBackoffMs()).isEqualTo(0);
        assertThat(subject.getRetryCount()).isEqualTo(0);

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(60000);
        assertThat(subject.getRetryCount()).isEqualTo(1);
    }

    @Test(expected = VolleyError.class)
    public void backoff_withOtherErrorType_shouldRethrowException() throws Exception {
        NetworkResponse networkResponse = new NetworkResponse(500, null, null, false);
        VolleyError volleyError = new VolleyError(networkResponse);

        subject.backoff(volleyError);
    }

    @Test
    public void backoff_shouldUpdateBackoffTime5TimesMax() throws Exception {
        VolleyError volleyError = new NoConnectionError();

        assertThat(subject.getBackoffMs()).isEqualTo(0);
        assertThat(subject.getRetryCount()).isEqualTo(0);
        assertThat(subject.hasAttemptRemaining()).isTrue();

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(60000);
        assertThat(subject.getRetryCount()).isEqualTo(1);
        assertThat(subject.hasAttemptRemaining()).isTrue();

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(120000);
        assertThat(subject.getRetryCount()).isEqualTo(2);
        assertThat(subject.hasAttemptRemaining()).isTrue();

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(240000);
        assertThat(subject.getRetryCount()).isEqualTo(3);
        assertThat(subject.hasAttemptRemaining()).isTrue();

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(480000);
        assertThat(subject.getRetryCount()).isEqualTo(4);
        assertThat(subject.hasAttemptRemaining()).isTrue();

        subject.backoff(volleyError);

        assertThat(subject.getBackoffMs()).isEqualTo(960000);
        assertThat(subject.getRetryCount()).isEqualTo(5);
        assertThat(subject.hasAttemptRemaining()).isFalse();
    }

    @Test(expected = NoConnectionError.class)
    public void backoff_withNoAttemptsRemaining_shouldRethrowVolleyException() throws Exception {
        VolleyError volleyError = new NoConnectionError();

        try {
            subject.backoff(volleyError);
            subject.backoff(volleyError);
            subject.backoff(volleyError);
            subject.backoff(volleyError);
            subject.backoff(volleyError);
        } catch (Exception e) {
            fail("Exception should not be thrown from above backoffs.");
        }

        subject.backoff(volleyError);
    }
}
