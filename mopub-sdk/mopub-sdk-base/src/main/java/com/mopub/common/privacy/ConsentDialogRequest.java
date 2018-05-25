package com.mopub.common.privacy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

class ConsentDialogRequest extends MoPubRequest<ConsentDialogResponse> {
    private static final String HTML_KEY = "dialog_html";

    public interface Listener extends Response.ErrorListener {
        void onSuccess(ConsentDialogResponse response);
    }

    @Nullable
    private Listener mListener;

    ConsentDialogRequest(@NonNull Context context, @NonNull String url, @Nullable Listener listener) {
        super(context, url, listener);

        mListener = listener;

        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    @Override
    protected Response<ConsentDialogResponse> parseNetworkResponse(final NetworkResponse networkResponse) {
        final String responseBody = parseStringBody(networkResponse);

        ConsentDialogResponse response;
        try {
            final JSONObject jsonBody = new JSONObject(responseBody);
            String html = jsonBody.getString(HTML_KEY);
            if (TextUtils.isEmpty(html)) {
                throw new JSONException("Empty HTML body");
            }
            response = new ConsentDialogResponse(html);
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError(
                            "Unable to parse consent dialog request network response.",
                            MoPubNetworkError.Reason.BAD_BODY,
                            null
                    )
            );
        }

        return Response.success(response, HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    @Override
    protected void deliverResponse(ConsentDialogResponse consentDialogResponse) {
        if (mListener != null) {
            mListener.onSuccess(consentDialogResponse);
        }
    }
}
