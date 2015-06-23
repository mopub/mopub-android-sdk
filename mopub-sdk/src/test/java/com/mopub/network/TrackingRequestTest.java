package com.mopub.network;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class TrackingRequestTest {

    @Mock
    private MoPubRequestQueue mockRequestQueue;
    private Context context;
    private String url;


    @Before
    public void setup() {
        context = (Context) Robolectric.buildActivity(Activity.class).create().get();
        url = "testUrl";
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void makeTrackingHttpRequest_shouldMakeTrackingHttpRequestWithWebViewUserAgent() throws Exception {
        TrackingRequest.makeTrackingHttpRequest(url, context);

        verify(mockRequestQueue).add(any(TrackingRequest.class));
    }

    @Test
    public void makeTrackingHttpRequest_withNullUrl_shouldNotMakeTrackingHttpRequest() throws Exception {
        TrackingRequest.makeTrackingHttpRequest((String) null, context);

        verify(mockRequestQueue, never()).add(any(TrackingRequest.class));
    }

    @Test
    public void makeTrackingHttpRequest_withNullContext_shouldNotMakeTrackingHttpRequest() throws Exception {
        TrackingRequest.makeTrackingHttpRequest(url, null);

        verify(mockRequestQueue, never()).add(any(TrackingRequest.class));
    }
}
