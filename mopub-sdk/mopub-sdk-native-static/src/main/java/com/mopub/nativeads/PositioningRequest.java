package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestUtils;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.HttpHeaderParser;
import com.mopub.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Map;

import static com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

public class PositioningRequest extends JsonRequest<MoPubClientPositioning> {
    private static final String FIXED_KEY = "fixed";
    private static final String SECTION_KEY = "section";
    private static final String POSITION_KEY = "position";
    private static final String REPEATING_KEY = "repeating";
    private static final String INTERVAL_KEY = "interval";

    // Max value to avoid bad integer math calculations. This is 2 ^ 16.
    private static final int MAX_VALUE = 1 << 16;

    @NonNull private final String mOriginalUrl;
    @NonNull private final Context mContext;

    public PositioningRequest(@NonNull final Context context,
            final String url,
            final Response.Listener<MoPubClientPositioning> listener,
            final Response.ErrorListener errorListener) {
        super(MoPubRequestUtils.chooseMethod(url),
                MoPubRequestUtils.truncateQueryParamsIfPost(url),
                null,
                listener,
                errorListener);

        mOriginalUrl = url;
        mContext = context.getApplicationContext();
    }

    // This is done just for unit testing visibility.
    @Override
    protected void deliverResponse(final MoPubClientPositioning response) {
        super.deliverResponse(response);
    }

    @Override
    protected Response<MoPubClientPositioning> parseNetworkResponse(final NetworkResponse response) {
        if (response.statusCode != HttpURLConnection.HTTP_OK) {
            return Response.error(new VolleyError(response));
        }

        if (response.data.length == 0) {
            return Response.error(new VolleyError("Empty positioning response", new JSONException("Empty response")));
        }

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));

            return Response.success(parseJson(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new VolleyError("Couldn't parse JSON from Charset", e));
        } catch (JSONException e) {
            return Response.error(new VolleyError("JSON Parsing Error", e));
        } catch (MoPubNetworkError e) {
            return Response.error(e);
        }
    }

    @NonNull
    @VisibleForTesting
    MoPubClientPositioning parseJson(@NonNull String jsonString) throws  JSONException, MoPubNetworkError {
        JSONObject jsonObject = new JSONObject(jsonString);

        // If the server returns an error explicitly, throw an exception with the message.
        String error = jsonObject.optString("error", null);
        if (error != null) {
            if (error.equalsIgnoreCase("WARMING_UP")) {
                throw new MoPubNetworkError(MoPubNetworkError.Reason.WARMING_UP);
            }
            throw new JSONException(error);
        }

        // Parse fixed and repeating rules.
        JSONArray fixed = jsonObject.optJSONArray(FIXED_KEY);
        JSONObject repeating = jsonObject.optJSONObject(REPEATING_KEY);
        if (fixed == null && repeating == null) {
            throw new JSONException("Must contain fixed or repeating positions");
        }

        MoPubClientPositioning positioning = new MoPubClientPositioning();
        if (fixed != null) {
            parseFixedJson(fixed, positioning);
        }
        if (repeating != null) {
            parseRepeatingJson(repeating, positioning);
        }
        return positioning;
    }

    private void parseFixedJson(@NonNull final JSONArray fixed,
            @NonNull final MoPubClientPositioning positioning) throws JSONException {
        for (int i = 0; i < fixed.length(); ++i) {
            JSONObject positionObject = fixed.getJSONObject(i);
            int section = positionObject.optInt(SECTION_KEY, 0);
            if (section < 0) {
                throw new JSONException("Invalid section " + section + " in JSON response");
            }
            if (section > 0) {
                // Ignore sections > 0.
                continue;
            }
            int position = positionObject.getInt(POSITION_KEY);
            if (position < 0 || position > MAX_VALUE) {
                throw new JSONException("Invalid position " + position + " in JSON response");
            }
            positioning.addFixedPosition(position);
        }
    }

    private void parseRepeatingJson(@NonNull final JSONObject repeatingObject,
            @NonNull final MoPubClientPositioning positioning) throws JSONException {
        int interval = repeatingObject.getInt(INTERVAL_KEY);
        if (interval < 2 || interval > MAX_VALUE) {
            throw new JSONException("Invalid interval " + interval + " in JSON response");
        }
        positioning.enableRepeatingPositions(interval);
    }

    @Override
    protected Map<String, String> getParams() {
        if (!MoPubRequestUtils.isMoPubRequest(getUrl())) {
            return null;
        }

        return MoPubRequestUtils.convertQueryToMap(mContext, mOriginalUrl);
    }

    @Override
    public byte[] getBody() {
        final String body = MoPubRequestUtils.generateBodyFromParams(getParams(), getUrl());
        if (body == null) {
            return null;
        }
        return body.getBytes();
    }
}
