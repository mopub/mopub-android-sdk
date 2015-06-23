package com.mopub.network;

import android.support.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.event.BaseEvent;
import com.mopub.common.event.EventSerializer;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A POST request for logging custom events to the Scribe service.
 */
public class ScribeRequest extends Request<Void> {

    public interface Listener extends Response.ErrorListener {
        public void onResponse();
    }

    public interface ScribeRequestFactory extends RequestManager.RequestFactory {
        public ScribeRequest createRequest(ScribeRequest.Listener listener);
    }

    @NonNull private final List<BaseEvent> mEvents;
    @NonNull private final EventSerializer mEventSerializer;
    @NonNull private final ScribeRequest.Listener mListener;

    public ScribeRequest(@NonNull String url,
            @NonNull List<BaseEvent> events,
            @NonNull EventSerializer eventSerializer,
            @NonNull Listener listener) {
        super(Method.POST, url, listener);

        mEvents = events;
        mEventSerializer = eventSerializer;
        mListener = listener;

        setShouldCache(false);

        // This retry policy applies to socket timeouts only
        setRetryPolicy(new DefaultRetryPolicy());
    }

    /**
     * This is method runs on the background thread
     */
    @Override
    protected Map<String,String> getParams() {
        JSONArray jsonArray = mEventSerializer.serializeAsJson(mEvents);
        Map<String,String> params = new HashMap<String, String>();
        params.put("log", jsonArray.toString());
        return params;
    }

    @Override
    protected Response<Void> parseNetworkResponse(NetworkResponse networkResponse) {
        // NOTE: We never get status codes outside of {[200, 299], 304}. Those errors are sent to the
        // error listener.
        return Response.success(null, HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    @Override
    protected void deliverResponse(Void aVoid) {
        mListener.onResponse();
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    public List<BaseEvent> getEvents() {
        return mEvents;
    }
}

