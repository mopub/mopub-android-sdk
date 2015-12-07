package com.mopub.network;

import android.os.Looper;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RequestManagerTest {

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
    }

    @Test
    public void makeRequest_shouldAddRequestToQueue() throws Exception {
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);

        verify(mockRequestQueue).add(mockScribeRequest);
    }


    @Test
    public void makeRequest_shouldCancelTheCurrentRequest() throws Exception {
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        Request<?> request = subject.getCurrentRequest();

        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);

        verify(mockRequestQueue).cancel(request);
    }

    @Test
    public void cancelRequest_shouldCancelRequestInQueue_shouldClearRequest() throws Exception {
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        subject.cancelRequest();

        verify(mockRequestQueue).cancel(mockScribeRequest);
        assertThat(subject.getCurrentRequest()).isNull();
    }

    @Test
    public void cancelRequest_withNullRequestQueue_shouldOnlyClearCurrentRequest() throws Exception {
        Networking.setRequestQueueForTesting(null);

        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        subject.cancelRequest();

        verify(mockRequestQueue, never()).cancel(mockScribeRequest);
        assertThat(subject.getCurrentRequest()).isNull();
    }

    @Test
    public void cancelRequest_withNullCurrentRequest_shouldOnlyClearCurrentRequest() throws Exception {
        subject.cancelRequest();

        verify(mockRequestQueue, never()).cancel(mockScribeRequest);
        assertThat(subject.getCurrentRequest()).isNull();
    }

    @Test
    public void makeRequestInternal_shouldAddNewRequestToQueue() throws Exception {
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        reset(mockRequestQueue);

        ScribeRequest previousRequest = (ScribeRequest) subject.getCurrentRequest();
        ScribeRequest nextRequest = mock(ScribeRequest.class);
        when(mockScribeRequestFactory.createRequest(subject)).thenReturn(nextRequest);

        subject.makeRequestInternal();

        verify(mockRequestQueue).add(nextRequest);
        verify(mockRequestQueue, never()).addDelayedRequest(any(Request.class), anyInt());
        assertThat(previousRequest).isNotEqualTo(nextRequest);
    }

    @Test
    public void makeRequestInternal_withRetryCountGreaterThan0_shouldAddNewDelayedRequestToQueue() throws Exception {
        when(mockScribeBackoffPolicy.getRetryCount()).thenReturn(1);
        when(mockScribeBackoffPolicy.getBackoffMs()).thenReturn(100);
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        reset(mockRequestQueue);

        ScribeRequest previousRequest = (ScribeRequest) subject.getCurrentRequest();
        ScribeRequest nextRequest = mock(ScribeRequest.class);
        when(mockScribeRequestFactory.createRequest(subject)).thenReturn(nextRequest);

        subject.makeRequestInternal();

        verify(mockRequestQueue).addDelayedRequest(nextRequest, 100);
        verify(mockRequestQueue, never()).add(any(Request.class));
        assertThat(previousRequest).isNotEqualTo(nextRequest);
    }

    @Test
    public void makeRequestInternal_withNullRequestQueue_shouldClearCurrentRequest_shouldNotAddRequestToQueue() throws Exception {
        Networking.setRequestQueueForTesting(null);

        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);

        verify(mockRequestQueue, never()).add(any(Request.class));
        verify(mockRequestQueue, never()).addDelayedRequest(any(Request.class), anyInt());
        assertThat(subject.getCurrentRequest()).isNull();
    }

    @Test
    public void clearRequest_shouldSetCurrentRequestToNull() throws Exception {
        subject.makeRequest(mockScribeRequestFactory, mockScribeBackoffPolicy);
        assertThat(subject.getCurrentRequest()).isNotNull();
        subject.clearRequest();
        assertThat(subject.getCurrentRequest()).isNull();
    }
}

