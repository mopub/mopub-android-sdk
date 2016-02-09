package com.mopub.network;

import android.app.Activity;

import com.mopub.common.event.BaseEvent;
import com.mopub.common.event.EventSerializer;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ScribeRequestTest {

    private ScribeRequest subject;
    @Mock private List<BaseEvent> mockEvents;
    @Mock private EventSerializer mockEventSerializer;
    @Mock private ScribeRequest.Listener mockListener;

    @Before
    public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        Networking.getRequestQueue(activity);

        subject = new ScribeRequest("url", mockEvents, mockEventSerializer, mockListener);
    }

    @Test
    public void constructor_shouldSetShouldCacheToFalse() throws Exception {
        assertThat(subject.shouldCache()).isFalse();
    }

    @Test
    public void constructor_shouldSetRetryPolicyToDefaultRetryPolicy() throws Exception {
        assertThat(subject.getRetryPolicy()).isExactlyInstanceOf(DefaultRetryPolicy.class);
    }

    @Test
    public void getParams_returnJsonSerializedEventsInMap() throws Exception {
        JSONArray mockJsonArray = mock(JSONArray.class);
        when(mockJsonArray.toString()).thenReturn("jsonArrayToString");
        when(mockEventSerializer.serializeAsJson(mockEvents)).thenReturn(mockJsonArray);

        Map<String, String> params = subject.getParams();

        verify(mockEventSerializer).serializeAsJson(mockEvents);
        assertThat(params.keySet().size()).isEqualTo(1);
        assertThat(params.get("log")).isEqualTo("jsonArrayToString");
    }
    
    @Test
    public void parseNetworkResponse_shouldReturnSuccessResponse() throws Exception {
        NetworkResponse networkResponse = new NetworkResponse(200, "abc".getBytes(Charset.defaultCharset()), new HashMap<String, String>(), false);

        Response<Void> response = subject.parseNetworkResponse(networkResponse);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    public void deliverResponse_shouldNotifyListener() throws Exception {
        subject.deliverResponse(null);

        verify(mockListener).onResponse();
    }
}
