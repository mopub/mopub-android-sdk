package com.mopub.network;

import android.app.Activity;
import android.os.Handler;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.Cache;
import com.mopub.volley.Network;
import com.mopub.volley.Request;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.toolbox.BasicNetwork;
import com.mopub.volley.toolbox.DiskBasedCache;
import com.mopub.volley.toolbox.HttpStack;
import com.mopub.volley.toolbox.HurlStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubRequestQueueTest {

    private MoPubRequestQueue subject;
    private MoPubRequestQueue.DelayedRequestHelper delayedRequestHelper;
    @Mock private ScribeRequest mockScribeRequest;
    @Mock private MoPubRequestQueue.DelayedRequestHelper mockDelayedRequestHelper;
    @Mock private Handler mockHandler;

    @Before
    public void setUp() {
        // NOTE: It's possible to start a new test with a queue set from a previous test suite
        // Make sure we get a fresh one
        Networking.setRequestQueueForTesting(null);

        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        // Construct our dependencies & build the object
        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activity);
        final HurlStack.UrlRewriter urlRewriter = new PlayServicesUrlRewriter(clientMetadata.getDeviceId(), activity);
        // No Custom SSL Factory

        HttpStack httpStack = new RequestQueueHttpStack("test_user_agent", urlRewriter);

        Network network = new BasicNetwork(httpStack);
        File volleyCacheDir = new File(activity.getCacheDir().getPath() + File.separator
                + Networking.CACHE_DIRECTORY_NAME);
        Cache cache = new DiskBasedCache(volleyCacheDir, (int) DeviceUtils.diskCacheSizeBytes(volleyCacheDir, Constants.TEN_MB));
        subject = new MoPubRequestQueue(cache, network);
        subject.start();

        delayedRequestHelper = subject.new DelayedRequestHelper(mockScribeRequest, 100, mockHandler);
    }

    @After
    public void tearDown() {
        // NOTE: Make sure that we clear the queue after the last test in the test suite runs so
        // that the next test suite starts fresh
        Networking.setRequestQueueForTesting(null);
    }

    @Test
    public void addDelayedRequest_shouldStartDelayedRequestHelper_shouldPutRequestInMap() throws Exception {
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);

        verify(mockDelayedRequestHelper).start();
        assertThat(subject.getDelayedRequests().get(mockScribeRequest)).isEqualTo(mockDelayedRequestHelper);
        assertThat(subject.getDelayedRequests().entrySet()).hasSize(1);
    }

    @Test
    public void addDelayedRequest_shouldCancelPreexistingRequest() throws Exception {
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);

        verify(mockDelayedRequestHelper, never()).cancel();
        verify(mockScribeRequest, never()).cancel();

        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);

        verify(mockDelayedRequestHelper).cancel();
        verify(mockScribeRequest).cancel();
    }

    @Test
    public void addDelayedRequest_withUniqueRequest_shouldNotCancelOtherRequests() throws Exception {
        ScribeRequest mockScribeRequest2 = mock(ScribeRequest.class);
        MoPubRequestQueue.DelayedRequestHelper mockDelayedRequestHelper2 = mock(MoPubRequestQueue.DelayedRequestHelper.class);
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);
        subject.addDelayedRequest(mockScribeRequest2, mockDelayedRequestHelper2);

        verify(mockDelayedRequestHelper, never()).cancel();
        verify(mockScribeRequest, never()).cancel();

        verify(mockDelayedRequestHelper2).start();
        assertThat(subject.getDelayedRequests().get(mockScribeRequest)).isEqualTo(mockDelayedRequestHelper);
        assertThat(subject.getDelayedRequests().get(mockScribeRequest2)).isEqualTo(mockDelayedRequestHelper2);
        assertThat(subject.getDelayedRequests().entrySet()).hasSize(2);
    }

    @Test
    public void cancelAll_shouldCancelAllRequestsInTheDelayedRequestMapThatPassTheFilter() throws Exception {
        ScribeRequest mockScribeRequest2 = mock(ScribeRequest.class);
        MoPubRequestQueue.DelayedRequestHelper mockDelayedRequestHelper2 = mock(MoPubRequestQueue.DelayedRequestHelper.class);
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);
        subject.addDelayedRequest(mockScribeRequest2, mockDelayedRequestHelper2);


        subject.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request == mockScribeRequest;
            }
        });

        verify(mockDelayedRequestHelper).cancel();
        verify(mockScribeRequest).cancel();

        verify(mockDelayedRequestHelper2, never()).cancel();
        verify(mockScribeRequest2, never()).cancel();

        assertThat(subject.getDelayedRequests().entrySet()).hasSize(1);
        assertThat(subject.getDelayedRequests().get(mockScribeRequest2)).isEqualTo(mockDelayedRequestHelper2);
    }

    @Test
    public void cancelAll_shouldCancelAllRequestsWithMatchingObjectTag() throws Exception {
        ScribeRequest mockScribeRequest2 = mock(ScribeRequest.class);
        MoPubRequestQueue.DelayedRequestHelper mockDelayedRequestHelper2 = mock(MoPubRequestQueue.DelayedRequestHelper.class);
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);
        subject.addDelayedRequest(mockScribeRequest2, mockDelayedRequestHelper2);

        when(mockScribeRequest.getTag()).thenReturn(1);
        when(mockScribeRequest2.getTag()).thenReturn(2);

        subject.cancelAll(1);

        verify(mockDelayedRequestHelper).cancel();
        verify(mockScribeRequest).cancel();

        verify(mockDelayedRequestHelper2, never()).cancel();
        verify(mockScribeRequest2, never()).cancel();

        assertThat(subject.getDelayedRequests().entrySet()).hasSize(1);
        assertThat(subject.getDelayedRequests().get(mockScribeRequest2)).isEqualTo(mockDelayedRequestHelper2);
    }

    @Test
    public void cancel_shouldCancelMatchingRequest() throws Exception {
        ScribeRequest mockScribeRequest2 = mock(ScribeRequest.class);
        MoPubRequestQueue.DelayedRequestHelper mockDelayedRequestHelper2 = mock(MoPubRequestQueue.DelayedRequestHelper.class);
        subject.addDelayedRequest(mockScribeRequest, mockDelayedRequestHelper);
        subject.addDelayedRequest(mockScribeRequest2, mockDelayedRequestHelper2);

        subject.cancel(mockScribeRequest);

        verify(mockDelayedRequestHelper).cancel();
        verify(mockScribeRequest).cancel();

        verify(mockDelayedRequestHelper2, never()).cancel();
        verify(mockScribeRequest2, never()).cancel();

        assertThat(subject.getDelayedRequests().entrySet()).hasSize(1);
        assertThat(subject.getDelayedRequests().get(mockScribeRequest2)).isEqualTo(mockDelayedRequestHelper2);
    }
    
    @Test
    public void DelayedRequestHelper_start_shouldPostDelayedRunnable() throws Exception {
        delayedRequestHelper.start();
        verify(mockHandler).postDelayed(delayedRequestHelper.mDelayedRunnable, 100);
    }

    @Test
    public void DelayedRequestHelper_cancel_shouldCancelDelayedRunnable() throws Exception {
        delayedRequestHelper.cancel();
        verify(mockHandler).removeCallbacks(delayedRequestHelper.mDelayedRunnable);
    }

    @Test
    public void DelayedRequestHelper_Runnable_run_shouldRemoveRequestFromDelayedRequestsMap_shouldAddRequestToQueue() throws Exception {
        subject.addDelayedRequest(mockScribeRequest, 100);
        assertThat(subject.getDelayedRequests().entrySet().size()).isEqualTo(1);
        MoPubRequestQueue.DelayedRequestHelper delayedRequestHelper = subject.getDelayedRequests().get(mockScribeRequest);

        delayedRequestHelper.mDelayedRunnable.run();

        assertThat(subject.getDelayedRequests().entrySet()).isEmpty();
        verify(mockScribeRequest).setRequestQueue(subject);
    }
}
