package com.mopub.common.privacy;

import android.app.Activity;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.MoPubNetworkError;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.RetryPolicy;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.nio.charset.Charset;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ConsentDialogRequestTest {
    private static final String URL = "https://"+ Constants.HOST+"/m/gdpr_consent_dialog?adunit_id=testAdUnitId&nv=5.0.0&language=en";
    private static final String HTML = "html-body-text";
    private static final String BODY = "{ dialog_html : '" + HTML + "' }";

    @Mock
    private ConsentDialogRequest.Listener listener;

    private Activity activity;
    private ConsentDialogRequest subject;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new ConsentDialogRequest(activity, URL, listener);
    }

    @Test
    public void constructor_shouldSetParametersCorrectly() {
        RetryPolicy retryPolicy = subject.getRetryPolicy();

        assertThat(subject.getUrl()).isEqualTo(URL.substring(0, URL.indexOf('?')));
        assertThat(retryPolicy).isNotNull();
        assertThat(retryPolicy.getCurrentTimeout()).isEqualTo(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS);
        assertThat(subject.shouldCache()).isFalse();
    }

    @Test
    public void parseNetworkResponse_validBody_shouldSucceed() {
        NetworkResponse testResponse = new NetworkResponse(BODY.getBytes(Charset.defaultCharset()));
        final Response<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.result).isNotNull();
        assertThat(response.result.getHtml()).isEqualTo(HTML);
    }

    @Test
    public void parseNetworkResponse_emptyBody_shouldReturnErrorBadBody() {
        NetworkResponse testResponse = new NetworkResponse("".getBytes(Charset.defaultCharset()));
        final Response<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.BAD_BODY);
    }

    @Test
    public void parseNetworkResponse_bodyBrokenJson_shouldReturnErrorBadBody() {
        NetworkResponse testResponse = new NetworkResponse("{ html - 'body' }".getBytes(Charset.defaultCharset()));
        final Response<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.BAD_BODY);
    }

    @Test
    public void parseNetworkResponse_jsonNoHtmlTag_shouldReturnErrorBadBody() {
        NetworkResponse testResponse = new NetworkResponse("{ k: 1 }".getBytes(Charset.defaultCharset()));
        final Response<ConsentDialogResponse> response = subject.parseNetworkResponse(testResponse);

        assertThat(response.error).isNotNull();
        assertThat(response.error).isInstanceOf(MoPubNetworkError.class);
        assertThat(((MoPubNetworkError) response.error).getReason()).isEqualTo(MoPubNetworkError.Reason.BAD_BODY);
    }

    @Test
    public void deliverResponse_validListener_callsListener() {
        ConsentDialogResponse response = new ConsentDialogResponse("html-text");
        subject.deliverResponse(response);

        verify(listener).onSuccess(response);
    }

    @Test
    public void deliverResponse_nullListener_doesntCrash() {
        subject = new ConsentDialogRequest(activity, URL, null);
        ConsentDialogResponse response = new ConsentDialogResponse("html-text");
        subject.deliverResponse(response);

        verify(listener, never()).onSuccess(any(ConsentDialogResponse.class));
        verify(listener, never()).onErrorResponse(any(VolleyError.class));
    }
}
