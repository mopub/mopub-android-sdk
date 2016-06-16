package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedVideoCompletionRequestHandlerTest {
    @Mock
    private MoPubRequestQueue mockRequestQueue;
    private Context context;
    private String url;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        url = "testUrl";
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void makeRewardedVideoCompletionRequest_shouldAddMacros_shouldMakeVideoCompletionRequest() throws Exception {
        RewardedVideoCompletionRequestHandler.makeRewardedVideoCompletionRequest(context, url,
                "customer id");

        verify(mockRequestQueue).add(argThat(isUrl(
                "testUrl&customer_id=customer%20id&nv=" +
                        Uri.encode(MoPub.SDK_VERSION) + "&v=" +
                        MoPubRewardedVideoManager.API_VERSION)));
    }

    @Test
    public void getTimeout_shouldReturnCorrectTimeoutBasedOnRetry() {
        final int maxTimeout = RewardedVideoCompletionRequestHandler.RETRY_TIMES[RewardedVideoCompletionRequestHandler.RETRY_TIMES.length - 1];

        assertThat(RewardedVideoCompletionRequestHandler.getTimeout(-1)).isEqualTo(maxTimeout);

        assertThat(RewardedVideoCompletionRequestHandler.getTimeout(0)).isEqualTo(
                RewardedVideoCompletionRequestHandler.RETRY_TIMES[0]);

        assertThat(RewardedVideoCompletionRequestHandler.getTimeout(1)).isEqualTo(
                RewardedVideoCompletionRequestHandler.RETRY_TIMES[1]);

        assertThat(RewardedVideoCompletionRequestHandler.getTimeout(1234567)).isEqualTo(
                maxTimeout);
    }

    @Test
    public void retryTimes_shouldAllBeGreaterThanRequestTimeoutDelay() {
        for (int retryTime : RewardedVideoCompletionRequestHandler.RETRY_TIMES) {
            assertThat(
                    retryTime - RewardedVideoCompletionRequestHandler.REQUEST_TIMEOUT_DELAY)
                    .isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    public void onErrorResponse_shouldSetShouldStopToTrueWhenResponseNot500To599() {
        RewardedVideoCompletionRequestHandler subject =
                new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");

        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject.onErrorResponse(new VolleyError(new NetworkResponse(500, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onErrorResponse(new VolleyError(new NetworkResponse(501, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onErrorResponse(new VolleyError(new NetworkResponse(599, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onErrorResponse(new VolleyError(new NetworkResponse(200, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onErrorResponse(new VolleyError(new NetworkResponse(499, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onErrorResponse(new VolleyError(new NetworkResponse(600, null, null, true)));
        assertThat(subject.getShouldStop()).isEqualTo(true);
    }

    @Test
    public void onResponse_shouldSetShouldStopToTrueWhenResponseNot500To599() {
        RewardedVideoCompletionRequestHandler subject =
                new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");

        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject.onResponse(500);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onResponse(501);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onResponse(599);
        assertThat(subject.getShouldStop()).isEqualTo(false);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onResponse(200);
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onResponse(499);
        assertThat(subject.getShouldStop()).isEqualTo(true);

        subject = new RewardedVideoCompletionRequestHandler(context, "url", "customer_id");
        subject.onResponse(600);
        assertThat(subject.getShouldStop()).isEqualTo(true);
    }

    @Test
    public void makeRewardedVideoCompletionRequest_shouldRetry() {
        Handler mockHandler = mock(Handler.class);
        RewardedVideoCompletionRequestHandler subject =
                new RewardedVideoCompletionRequestHandler(context, "url", "customer_id",
                        mockHandler);

        subject.makeRewardedVideoCompletionRequest();

        assertThat(subject.getRetryCount()).isEqualTo(1);
        verify(mockHandler).postDelayed(any(Runnable.class),
                eq((long) RewardedVideoCompletionRequestHandler.RETRY_TIMES[0]));
    }

    @Test
    public void makeRewardedVideoCompletionRequest_shouldNotRetryIfShouldStopIsSetToTrue() {
        Handler mockHandler = mock(Handler.class);
        RewardedVideoCompletionRequestHandler subject =
                new RewardedVideoCompletionRequestHandler(context, "url", "customer_id",
                        mockHandler);
        // This should set shouldStop to true.
        subject.onResponse(200);

        subject.makeRewardedVideoCompletionRequest();

        assertThat(subject.getShouldStop()).isTrue();
        verifyZeroInteractions(mockHandler);
    }

    @Test
    public void makeRewardedVideoCompletionRequest_shouldNotRetryIfMaxRetriesReached() {
        Handler mockHandler = mock(Handler.class);
        RewardedVideoCompletionRequestHandler subject =
                new RewardedVideoCompletionRequestHandler(context, "url", "customer_id",
                        mockHandler);
        subject.setRetryCount(RewardedVideoCompletionRequestHandler.MAX_RETRIES);

        subject.makeRewardedVideoCompletionRequest();

        verifyZeroInteractions(mockHandler);
    }
}
