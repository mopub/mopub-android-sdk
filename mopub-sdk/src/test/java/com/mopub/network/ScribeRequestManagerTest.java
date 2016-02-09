package com.mopub.network;

import android.os.Looper;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ScribeRequestManagerTest {

    private ScribeRequestManager subject;
    @Mock private ScribeRequest.ScribeRequestFactory mockScribeRequestFactory;
    @Mock private ScribeBackoffPolicy mockScribeBackoffPolicy;
    @Mock private ScribeRequest mockScribeRequest;
    @Mock private MoPubRequestQueue mockRequestQueue;

    @Before
    public void setUp() {
        Networking.setRequestQueueForTesting(mockRequestQueue);

        subject = new ScribeRequestManager(Looper.getMainLooper());
        when(mockScribeRequestFactory.createRequest(subject)).thenReturn(mockScribeRequest);
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
    }

    @Test
    public void createRequest_shouldCreateNewScribeRequest() throws Exception {
        Request request = subject.createRequest();
        assertThat(request).isEqualTo(mockScribeRequest);
    }

    @Test
    public void onResponse_shouldClearRequest() throws Exception {
        subject.onResponse();
        assertThat(subject.getCurrentRequest()).isNull();
    }

    @Test
    public void onErrorResponse_withBackoffApplied_shouldCallBackoff_shouldMakeDelayedRequest() throws Exception {
        when(mockScribeBackoffPolicy.getRetryCount()).thenReturn(1);
        when(mockScribeBackoffPolicy.getBackoffMs()).thenReturn(100);

        VolleyError volleyError = new NoConnectionError();
        subject.onErrorResponse(volleyError);

        verify(mockScribeBackoffPolicy).backoff(volleyError);
        verify(mockRequestQueue).addDelayedRequest(mockScribeRequest, 100);
    }

    @Test
    public void onErrorResponse_withBackoffNotApplied_shouldClearRequest() throws Exception {
        reset(mockRequestQueue);

        VolleyError volleyError = new NoConnectionError();
        doThrow(new VolleyError()).when(mockScribeBackoffPolicy).backoff(volleyError);

        subject.onErrorResponse(volleyError);

        verify(mockScribeBackoffPolicy).backoff(volleyError);
        verify(mockRequestQueue, never()).add(mockScribeRequest);
        assertThat(subject.getCurrentRequest()).isNull();
    }
}
