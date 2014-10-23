package com.mopub.nativeads;

import android.app.Activity;
import android.os.Build.VERSION_CODES;

import com.mopub.common.DownloadResponse;
import com.mopub.common.DownloadTask;
import com.mopub.common.DownloadTask.DownloadTaskListener;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.PositioningSource.PositioningListener;
import com.mopub.nativeads.ServerPositioningSource.DownloadTaskProvider;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;

import static junit.framework.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ServerPositioningSourceTest {
    @Mock DownloadTaskProvider mockDownloadTaskProvider;
    @Mock DownloadTaskListener mockDownloadTaskListener;
    @Mock DownloadTask mockDownloadTask;
    @Mock PositioningListener mockPositioningListener;
    @Captor ArgumentCaptor<DownloadTaskListener> taskListenerCaptor;
    @Mock DownloadResponse mockValidResponse;
    @Mock DownloadResponse mockNotFoundResponse;
    @Mock DownloadResponse mockInvalidJsonResponse;
    @Mock DownloadResponse mockWarmingUpJsonResponse;
    @Captor ArgumentCaptor<MoPubClientPositioning> positioningCaptor;

    ServerPositioningSource subject;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new ServerPositioningSource(activity, mockDownloadTaskProvider);

        when(mockDownloadTaskProvider.get(any(DownloadTaskListener.class)))
                .thenReturn(mockDownloadTask);

        when(mockValidResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockValidResponse.getByteArray()).thenReturn("{fixed: []}".getBytes());

        when(mockInvalidJsonResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockInvalidJsonResponse.getByteArray()).thenReturn("blah blah".getBytes());

        when(mockWarmingUpJsonResponse.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(mockWarmingUpJsonResponse.getByteArray()).thenReturn(
                "{\"error\":\"WARMING_UP\"}".getBytes());

        when(mockNotFoundResponse.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void loadPositions_atLeastIcs_shouldExecuteDownloadTask() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockDownloadTask).executeOnExecutor(any(Executor.class), any(HttpGet.class));
    }

    @Config(reportSdk = VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void loadPositions_beforeIcs_shouldExecuteDownloadTask() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockDownloadTask).execute(any(HttpGet.class));
    }

    @Test
    public void loadPositionsTwice_shouldCancelPreviousDownloadTask_shouldNotCallListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockDownloadTask).cancel(true);

        // Cancelling completes the download tasks with a null response.
        taskListenerCaptor.getValue().onComplete("some_url", null);
        verify(mockPositioningListener, never()).onFailed();
        verify(mockPositioningListener, never()).onLoad(any(MoPubClientPositioning.class));
    }

    @Test
    public void loadPositionsTwice_withPendingRetry_shouldNotCancelPreviousDownloadTask() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockValidResponse);

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        verify(mockDownloadTask, never()).cancel(anyBoolean());
    }

    @Test
    public void loadPositions_thenComplete_withValidResponse_shouldCallOnLoadListener() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockValidResponse);

        verify(mockPositioningListener).onLoad(positioningCaptor.capture());
        MoPubClientPositioning positioning = positioningCaptor.getValue();
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubClientPositioning.NO_REPEAT);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void loadPositions_thenComplete_withNotFoundResponse_shouldRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);
        
        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockNotFoundResponse);

        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockDownloadTask, times(2))
                .executeOnExecutor(any(Executor.class), any(HttpGet.class));
    }

    @Test
    public void loadPositions_thenComplete_withWarmingUpResponse_shouldRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockWarmingUpJsonResponse);

        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockDownloadTask, times(2))
                .executeOnExecutor(any(Executor.class), any(HttpGet.class));
    }

    @Test
    public void loadPositions_thenComplete_withInvalidJsonResponse_shouldRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockInvalidJsonResponse);

        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockDownloadTask, times(2))
                .executeOnExecutor(any(Executor.class), any(HttpGet.class));
    }

    @Test
    public void loadPositions_withPendingRetry_shouldNotRetry() {
        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockInvalidJsonResponse);

        subject.loadPositions("test_ad_unit", mockPositioningListener);
        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockDownloadTask, times(2))
                .executeOnExecutor(any(Executor.class), any(HttpGet.class));
    }

    @Test
    public void loadPositions_thenFailAfterMaxRetryTime_shouldCallFailureHandler() {
        ServerPositioningSource.MAXIMUM_RETRY_TIME_MILLISECONDS = 999;

        subject.loadPositions("test_ad_unit", mockPositioningListener);

        verify(mockDownloadTaskProvider).get(taskListenerCaptor.capture());
        taskListenerCaptor.getValue().onComplete("some_url", mockInvalidJsonResponse);

        Robolectric.getUiThreadScheduler().advanceToLastPostedRunnable();
        verify(mockPositioningListener).onFailed();
    }

    @Test
    public void parseJsonResponse_noFixedPositions_shouldReturnEmptyPositioning()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{fixed: []}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJsonResponse_oneFixedPosition_shouldReturnValidPositioning()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{fixed: [{position: 2}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(2);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJsonResponse_twoFixedPositions_shouldReturnValidPositioning()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{fixed: [{position: 1}, {position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsExactly(1, 8);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJsonResponse_twoFixedPositions_shouldIgnoreNonZeroSection()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{fixed: [{section: 0, position: 5}, {section: 1, position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(5);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJsonResponse_invalidFixedPosition_shouldThrowException() {
        // Must have either fixed or repeating positions.
        checkException(null, "Empty response");
        checkException("", "Empty response");
        checkException("{}", "Must contain fixed or repeating positions");
        checkException("{\"error\":\"WARMING_UP\"}", "WARMING_UP");

        // Position is required.
        checkException("{fixed: [{}]}", "JSONObject[\"position\"] not found.");
        checkException("{fixed: [{section: 0}]}", "JSONObject[\"position\"] not found.");

        // Section is optional, but if it exists must be > 0
        checkException("{fixed: [{section: -1, position: 8}]}", "Invalid section -1 in JSON response");

        // Positions must be between [0 and 2 ^ 16).
        checkException("{fixed: [{position: -1}]}", "Invalid position -1 in JSON response");
        checkException("{fixed: [{position: 1}, {position: -8}]}",
                "Invalid position -8 in JSON response");
        checkException("{fixed: [{position: 1}, {position: 66000}]}",
                "Invalid position 66000 in JSON response");
    }

    @Test
    public void parseJsonResponse_repeatingInterval_shouldReturnValidPositioning()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    @Test
    public void parseJsonResponse_invalidRepeating_shouldThrowException() {
        checkException("{repeating: }", "Missing value at character 12");
        checkException("{repeating: {}}", "JSONObject[\"interval\"] not found.");

        // Intervals must be between [2 and 2 ^ 16).
        checkException("{repeating: {interval: -1}}", "Invalid interval -1 in JSON response");
        checkException("{repeating: {interval: 0}}", "Invalid interval 0 in JSON response");
        checkException("{repeating: {interval: 1}}", "Invalid interval 1 in JSON response");
        checkException("{repeating: {interval: 66000}}",
                "Invalid interval 66000 in JSON response");
    }

    @Test
    public void parseJsonResponse_fixedAndRepeating_shouldReturnValidPositioning()
            throws JSONException {
        MoPubClientPositioning positioning = subject.parseJsonResponse(
                "{fixed: [{position: 0}, {position: 1}], repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).containsExactly(0, 1);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    private void checkException(String json, String expectedMessage) {
        try {
            subject.parseJsonResponse(json);
        } catch (JSONException e) {
            assertThat(e.getMessage()).isEqualTo(expectedMessage);
            return;
        }
        fail("Should have received an exception");
    }
}
