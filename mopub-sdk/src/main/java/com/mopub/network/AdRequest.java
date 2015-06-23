package com.mopub.network;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.event.BaseEvent;
import com.mopub.common.event.Event;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.AdTypeTranslator;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.mopub.network.HeaderUtils.extractBooleanHeader;
import static com.mopub.network.HeaderUtils.extractHeader;
import static com.mopub.network.HeaderUtils.extractIntegerHeader;

public class AdRequest extends Request<AdResponse> {

    @NonNull private final AdRequest.Listener mListener;
    @NonNull private final AdFormat mAdFormat;
    @Nullable private final String mAdUnitId;
    @NonNull private final Context mContext;

    public interface Listener extends Response.ErrorListener {
        public void onSuccess(AdResponse response);
    }

    public AdRequest(@NonNull final String url,
            @NonNull final AdFormat adFormat,
            @Nullable final String adUnitId,
            @NonNull Context context,
            @NonNull final Listener listener) {
        super(Method.GET, url, listener);
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(listener);
        mAdUnitId = adUnitId;
        mListener = listener;
        mAdFormat = adFormat;
        mContext = context.getApplicationContext();
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    @NonNull
    public Listener getListener() {
        return mListener;
    }

    @Override
    public Map<String, String> getHeaders() {
        TreeMap<String, String> headers = new TreeMap<String, String>();

        // Use default locale first for language code
        String languageCode = Locale.getDefault().getLanguage();

        // If user's preferred locale is different from default locale, override language code
        Locale userLocale = mContext.getResources().getConfiguration().locale;
        if (userLocale != null) {
            if (! userLocale.getLanguage().trim().isEmpty()) {
                languageCode = userLocale.getLanguage().trim();
            }
        }

        // Do not add header if language is empty
        if (! languageCode.isEmpty()) {
            headers.put(ResponseHeader.ACCEPT_LANGUAGE.getKey(), languageCode);
        }

        return headers;
    }

    @Override
    protected Response<AdResponse> parseNetworkResponse(final NetworkResponse networkResponse) {
        // NOTE: We never get status codes outside of {[200, 299], 304}. Those errors are sent to the
        // error listener.

        Map<String, String> headers = networkResponse.headers;
        if (extractBooleanHeader(headers, ResponseHeader.WARMUP, false)) {
            return Response.error(new MoPubNetworkError("Ad Unit is warming up.", MoPubNetworkError.Reason.WARMING_UP));
        }


        Location location = LocationService.getLastKnownLocation(mContext,
                MoPub.getLocationPrecision(),
                MoPub.getLocationAwareness());

        AdResponse.Builder builder = new AdResponse.Builder();
        builder.setAdUnitId(mAdUnitId);

        String adTypeString = extractHeader(headers, ResponseHeader.AD_TYPE);
        String fullAdTypeString = extractHeader(headers, ResponseHeader.FULL_AD_TYPE);
        builder.setAdType(adTypeString);
        builder.setFullAdType(fullAdTypeString);

        // In the case of a CLEAR response, the REFRESH_TIME header must still be respected. Ensure
        // that it is parsed and passed along to the MoPubNetworkError.
        final Integer refreshTimeSeconds = extractIntegerHeader(headers, ResponseHeader.REFRESH_TIME);
        final Integer refreshTimeMilliseconds = refreshTimeSeconds == null
                ? null
                : refreshTimeSeconds * 1000;
        builder.setRefreshTimeMilliseconds(refreshTimeMilliseconds);

        if (AdType.CLEAR.equals(adTypeString)) {
            final AdResponse adResponse = builder.build();
            logScribeEvent(adResponse, networkResponse, location);
            return Response.error(
                    new MoPubNetworkError(
                            "No ads found for ad unit.",
                            MoPubNetworkError.Reason.NO_FILL,
                            refreshTimeMilliseconds
                    )
            );
        }

        builder.setNetworkType(extractHeader(headers, ResponseHeader.NETWORK_TYPE));

        String redirectUrl = extractHeader(headers, ResponseHeader.REDIRECT_URL);
        builder.setRedirectUrl(redirectUrl);

        String clickTrackingUrl = extractHeader(headers, ResponseHeader.CLICK_TRACKING_URL);
        builder.setClickTrackingUrl(clickTrackingUrl);

        builder.setImpressionTrackingUrl(extractHeader(headers, ResponseHeader.IMPRESSION_URL));

        String failUrl = extractHeader(headers, ResponseHeader.FAIL_URL);
        builder.setFailoverUrl(failUrl);

        String requestId = getRequestId(failUrl);
        builder.setRequestId(requestId);

        boolean isScrollable = extractBooleanHeader(headers, ResponseHeader.SCROLLABLE, false);
        builder.setScrollable(isScrollable);

        builder.setDimensions(extractIntegerHeader(headers, ResponseHeader.WIDTH),
                extractIntegerHeader(headers, ResponseHeader.HEIGHT));

        Integer adTimeoutDelaySeconds = extractIntegerHeader(headers, ResponseHeader.AD_TIMEOUT);
        builder.setAdTimeoutDelayMilliseconds(
                adTimeoutDelaySeconds == null
                        ? null
                        : adTimeoutDelaySeconds * 1000);

        // Response Body encoding / decoding
        String responseBody = parseStringBody(networkResponse);
        builder.setResponseBody(responseBody);
        if (AdType.NATIVE.equals(adTypeString)) {
            try {
                builder.setJsonBody(new JSONObject(responseBody));
            } catch (JSONException e) {
                return Response.error(
                        new MoPubNetworkError("Failed to decode body JSON for native ad format",
                                e, MoPubNetworkError.Reason.BAD_BODY));
            }
        }

        // Derive custom event fields
        String customEventClassName = AdTypeTranslator.getCustomEventName(mAdFormat, adTypeString,
                fullAdTypeString, headers);
        builder.setCustomEventClassName(customEventClassName);

        // Process server extras if they are present:
        String customEventData = extractHeader(headers, ResponseHeader.CUSTOM_EVENT_DATA);

        // Some server-supported custom events (like Millennial banners) use a different header field
        if (TextUtils.isEmpty(customEventData)) {
            customEventData = extractHeader(headers, ResponseHeader.NATIVE_PARAMS);
        }
        try {
            builder.setServerExtras(Json.jsonStringToMap(customEventData));
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError("Failed to decode server extras for custom event data.",
                            e, MoPubNetworkError.Reason.BAD_HEADER_DATA));
        }

        // Some MoPub-specific custom events get their serverExtras from the response itself:
        if (eventDataIsInResponseBody(adTypeString, fullAdTypeString)) {
            Map<String, String> eventDataMap = new TreeMap<String, String>();
            eventDataMap.put(DataKeys.HTML_RESPONSE_BODY_KEY, responseBody);
            eventDataMap.put(DataKeys.SCROLLABLE_KEY, Boolean.toString(isScrollable));
            eventDataMap.put(DataKeys.CREATIVE_ORIENTATION_KEY, extractHeader(headers, ResponseHeader.ORIENTATION));
            if (redirectUrl != null) {
                eventDataMap.put(DataKeys.REDIRECT_URL_KEY, redirectUrl);
            }
            if (clickTrackingUrl != null) {
                eventDataMap.put(DataKeys.CLICKTHROUGH_URL_KEY, clickTrackingUrl);
            }
            builder.setServerExtras(eventDataMap);
        }

        AdResponse adResponse = builder.build();
        logScribeEvent(adResponse, networkResponse, location);

        return Response.success(builder.build(),  // Cast needed for Response generic.
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    private boolean eventDataIsInResponseBody(@Nullable String adType,
            @Nullable String fullAdType) {
        return "mraid".equals(adType) || "html".equals(adType) ||
                ("interstitial".equals(adType) && "vast".equals(fullAdType));
    }

    // Based on Volley's StringResponse class.
    protected String parseStringBody(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return parsed;
    }

    @Override
    protected void deliverResponse(final AdResponse adResponse) {
        mListener.onSuccess(adResponse);
    }

    @Nullable
    @VisibleForTesting
    String getRequestId(@Nullable String failUrl) {
        if (failUrl == null) {
            return null;
        }

        String requestId = null;
        Uri uri = Uri.parse(failUrl);
        try {
            requestId = uri.getQueryParameter("request_id");
        } catch (UnsupportedOperationException e) {
            MoPubLog.d("Unable to obtain request id from fail url.");
        }

        return requestId;
    }

    @VisibleForTesting
    void logScribeEvent(@NonNull AdResponse adResponse, @NonNull NetworkResponse networkResponse,
            @Nullable Location location) {
        Preconditions.checkNotNull(adResponse);
        Preconditions.checkNotNull(networkResponse);

        MoPubEvents.log(
                new Event.Builder(BaseEvent.Name.AD_REQUEST, BaseEvent.Category.REQUESTS,
                        BaseEvent.SamplingRate.AD_REQUEST.getSamplingRate())
                        .withAdUnitId(mAdUnitId)
                        .withAdCreativeId(adResponse.getDspCreativeId())
                        .withAdType(adResponse.getAdType())
                        .withAdNetworkType(adResponse.getNetworkType())
                        .withAdWidthPx(adResponse.getWidth() != null
                                ? adResponse.getWidth().doubleValue()
                                : null)
                        .withAdHeightPx(adResponse.getHeight() != null
                                ? adResponse.getHeight().doubleValue()
                                : null)
                        .withGeoLat(location != null ? location.getLatitude() : null)
                        .withGeoLon(location != null ? location.getLongitude() : null)
                        .withGeoAccuracy(location != null ? (double) location.getAccuracy() : null)
                        .withPerformanceDurationMs((double) networkResponse.networkTimeMs)
                        .withRequestId(adResponse.getRequestId())
                        .withRequestStatusCode(networkResponse.statusCode)
                        .withRequestUri(getUrl())
                        .build()
        );
    }
}
