package com.mopub.nativeads;

import android.app.Activity;
import android.os.Build.VERSION_CODES;

import com.mopub.common.DownloadResponse;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.PositioningSource.PositioningListener;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ServerPositioningSourceTest {
    @Mock PositioningListener mockPositioningListener;
    @Captor ArgumentCaptor<PositioningRequest> positionRequestCaptor;
    @Mock DownloadResponse mockValidResponse;
    @Mock DownloadResponse mockNotFoundResponse;
    @Mock DownloadResponse mockInvalidJsonResponse;
    @Mock DownloadResponse mockWarmingUpJsonResponse;

    @Mock
    MoPubRequestQueue mockRequestQueue;

    @Captor ArgumentCaptor<MoPubClientPositioning> positioningCaptor;

    ServerPositioningSource subject;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new ServerPositioningSource(activity);

        Networking.setRequestQueueForTesting(mockRequestQueue);

        when(mockValidResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockValidResponse.getByteArray()).thenReturn("{fixed: []}".getBytes());

        when(mockInvalidJsonResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockInvalidJsonResponse.getByteArray()).thenReturn("blah blah".getBytes());

        when(mockWarmingUpJsonResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockWarmingUpJsonResponse.getByteArray()).thenReturn(
                "{\"error\":\"WARMING_UP\"}".getBytes());

        when(mockNotFoundResponse.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void loadPositions_shouldAddToRequestQueue() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(any(Request.class));
    }

    @Test
    public void loadPositionsTwice_shouldCancelPreviousRequest_shouldNotCallListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue, times(2)).add(any(Request.class));

        verify(mockPositioningListener, never()).onFailed();
        verify(mockPositioningListener, never()).onLoad(any(MoPubClientPositioning.class));
    }

    @Test
    public void loadPositionsTwice_afterSuccess_shouldNotCancelPreviousRequest() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockRequestQueue).add(any(Request.class));
    }

    @Test
    public void loadPositions_thenComplete_withValidResponse_shouldCallOnLoadListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());

        final PositioningRequest value = positionRequestCaptor.getValue();
        final MoPubClientPositioning response = new MoPubClientPositioning().enableRepeatingPositions(3);
        value.deliverResponse(response);

        verify(mockPositioningListener).onLoad(eq(response));
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void loadPositions_thenComplete_withErrorResponse_shouldRetry() throws Exception {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);
        // We get VolleyErrors for invalid JSON, 404s, 5xx, and {"error": "WARMING_UP"}
        positionRequestCaptor.getValue().deliverError(new VolleyError("Some test error"));

        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockRequestQueue).add(any(Request.class));
    }


    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void loadPositions_withPendingRetry_shouldNotRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        reset(mockRequestQueue);
        positionRequestCaptor.getValue().deliverError(new VolleyError("testError"));

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        // If a retry happened, we'd have two here.
        verify(mockRequestQueue).add(any(Request.class));
    }

    @Test
    public void loadPositions_thenFailAfterMaxRetryTime_shouldCallFailureHandler() {
        ServerPositioningSource.MAXIMUM_RETRY_TIME_MILLISECONDS = 999;

        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        positionRequestCaptor.getValue().deliverError(new VolleyError("test error"));
        verify(mockPositioningListener).onFailed();
    }

    @Test
    public void loadPositions_withNoConnection_shouldLogMoPubErrorCodeNoConnection_shouldCallFailureHandler() {
        LogManager.getLogManager().getLogger("com.mopub").setLevel(Level.ALL);

        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockRequestQueue).add(positionRequestCaptor.capture());
        positionRequestCaptor.getValue().deliverError(new NoConnectionError());
        verify(mockPositioningListener).onFailed();

        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 2);
        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim()).isEqualTo(MoPubErrorCode.NO_CONNECTION.toString());
    }
}
