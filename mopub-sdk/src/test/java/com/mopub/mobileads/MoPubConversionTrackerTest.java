package com.mopub.mobileads;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubConversionTrackerTest {
    private MoPubConversionTracker subject;
    private Activity context;
    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Captor
    ArgumentCaptor<TrackingRequest> requestCaptor;

    private String expectedUdid;
    private boolean dnt = false;
    private static final String TEST_UDID = "20b013c721c";

    @Before
    public void setUp() throws Exception {
        subject = new MoPubConversionTracker();
        context = Robolectric.buildActivity(Activity.class).create().get();
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void reportAppOpen_Twice_shouldCallOnlyOnce() {
        subject.reportAppOpen(context);
        verify(mockRequestQueue).add(requestCaptor.capture());

        reset(mockRequestQueue);
        requestCaptor.getValue().deliverResponse(null);

        subject.reportAppOpen(context);
        verify(mockRequestQueue, never()).add(any(TrackingRequest.class));
    }

    @Test
    public void reportAppOpen_fails_shouldCallAgain() {
        subject.reportAppOpen(context);
        verify(mockRequestQueue).add(requestCaptor.capture());

        reset(mockRequestQueue);
        requestCaptor.getValue().deliverError(new VolleyError());

        subject.reportAppOpen(context);
        verify(mockRequestQueue).add(any(TrackingRequest.class));
    }
}

