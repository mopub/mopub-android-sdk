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
import com.mopub.common.FullAdType;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.event.BaseEvent;
import com.mopub.common.event.Event;
import com.mopub.common.event.EventDetails;
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

import static com.mopub.common.ExternalViewabilitySessionManager.ViewabilityVendor;
import static com.mopub.network.HeaderUtils.extractBooleanHeader;
import static com.mopub.network.HeaderUtils.extractHeader;
import static com.mopub.network.HeaderUtils.extractIntegerHeader;
import static com.mopub.network.HeaderUtils.extractPercentHeaderString;

public class AdRequest extends Request<AdResponse> {

    @NonNull private final AdRequest.Listener mListener;
    @NonNull private final AdFormat mAdFormat;
    @Nullable private final String mAdUnitId;
    @NonNull private final Context mContext;

    public interface Listener extends Response.ErrorListener {
        void onSuccess(AdResponse response);
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
        final Integer refreshTimeSeconds = extractIntegerHeader(headers,
                ResponseHeader.REFRESH_TIME);
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

        String dspCreativeId = extractHeader(headers, ResponseHeader.DSP_CREATIVE_ID);
        builder.setDspCreativeId(dspCreativeId);

        String networkType = extractHeader(headers, ResponseHeader.NETWORK_TYPE);
        builder.setNetworkType(networkType);

        String redirectUrl = extractHeader(headers, ResponseHeader.REDIRECT_URL);
        builder.setRedirectUrl(redirectUrl);

        // X-Clickthrough is parsed into the AdResponse as the click tracker
        // Used by AdViewController, Rewarded Video, Native Adapter, MoPubNative
        String clickTrackingUrl = extractHeader(headers, ResponseHeader.CLICK_TRACKING_URL);
        builder.setClickTrackingUrl(clickTrackingUrl);

        builder.setImpressionTrackingUrl(extractHeader(headers, ResponseHeader.IMPRESSION_URL));

        String failUrl = extractHeader(headers, ResponseHeader.FAIL_URL);
        builder.setFailoverUrl(failUrl);

        String requestId = getRequestId(failUrl);
        builder.setRequestId(requestId);

        boolean isScrollable = extractBooleanHeader(headers, ResponseHeader.SCROLLABLE, false);
        builder.setScrollable(isScrollable);

        Integer width = extractIntegerHeader(headers, ResponseHeader.WIDTH);
        Integer height = extractIntegerHeader(headers, ResponseHeader.HEIGHT);
        builder.setDimensions(width, height);

        Integer adTimeoutDelaySeconds = extractIntegerHeader(headers, ResponseHeader.AD_TIMEOUT);
        builder.setAdTimeoutDelayMilliseconds(
                adTimeoutDelaySeconds == null
                        ? null
                        : adTimeoutDelaySeconds * 1000);

        // Response Body encoding / decoding
        String responseBody = parseStringBody(networkResponse);
        builder.setResponseBody(responseBody);
        if (AdType.STATIC_NATIVE.equals(adTypeString) || AdType.VIDEO_NATIVE.equals(adTypeString)) {
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

        // Default browser agent from X-Browser-Agent header
        BrowserAgent browserAgent = BrowserAgent.fromHeader(
                extractIntegerHeader(headers, ResponseHeader.BROWSER_AGENT));
        MoPub.setBrowserAgentFromAdServer(browserAgent);
        builder.setBrowserAgent(browserAgent);

        // Process server extras if they are present:
        String customEventData = extractHeader(headers, ResponseHeader.CUSTOM_EVENT_DATA);

        // Some server-supported custom events (like Millennial banners) use a different header field
        if (TextUtils.isEmpty(customEventData)) {
            customEventData = extractHeader(headers, ResponseHeader.NATIVE_PARAMS);
        }

        final Map<String, String> serverExtras;
        try {
            serverExtras = Json.jsonStringToMap(customEventData);
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError("Failed to decode server extras for custom event data.",
                            e, MoPubNetworkError.Reason.BAD_HEADER_DATA));
        }

        if (redirectUrl != null) {
            serverExtras.put(DataKeys.REDIRECT_URL_KEY, redirectUrl);
        }
        if (clickTrackingUrl != null) {
            // X-Clickthrough parsed into serverExtras
            // Used by Banner, Interstitial
            serverExtras.put(DataKeys.CLICKTHROUGH_URL_KEY, clickTrackingUrl);
        }
        if (eventDataIsInResponseBody(adTypeString, fullAdTypeString)) {
            // Some MoPub-specific custom events get their serverExtras from the response itself:
            serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, responseBody);
            serverExtras.put(DataKeys.SCROLLABLE_KEY, Boolean.toString(isScrollable));
            serverExtras.put(DataKeys.CREATIVE_ORIENTATION_KEY, extractHeader(headers, ResponseHeader.ORIENTATION));
        }
        if (AdType.STATIC_NATIVE.equals(adTypeString) || AdType.VIDEO_NATIVE.equals(adTypeString)) {
            final String impressionMinVisiblePercent = extractPercentHeaderString(headers,
                    ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT);
            final String impressionVisibleMS = extractHeader(headers,
                    ResponseHeader.IMPRESSION_VISIBLE_MS);
            final String impressionMinVisiblePx = extractHeader(headers,
                    ResponseHeader.IMPRESSION_MIN_VISIBLE_PX);
            if (!TextUtils.isEmpty(impressionMinVisiblePercent)) {
                serverExtras.put(DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT,
                        impressionMinVisiblePercent);
            }
            if (!TextUtils.isEmpty(impressionVisibleMS)) {
                serverExtras.put(DataKeys.IMPRESSION_VISIBLE_MS, impressionVisibleMS);
            }
            if (!TextUtils.isEmpty(impressionMinVisiblePx)) {
                serverExtras.put(DataKeys.IMPRESSION_MIN_VISIBLE_PX, impressionMinVisiblePx);
            }
        }
        if (AdType.VIDEO_NATIVE.equals(adTypeString)) {
            serverExtras.put(DataKeys.PLAY_VISIBLE_PERCENT,
                    extractPercentHeaderString(headers, ResponseHeader.PLAY_VISIBLE_PERCENT));
            serverExtras.put(DataKeys.PAUSE_VISIBLE_PERCENT,
                    extractPercentHeaderString(headers, ResponseHeader.PAUSE_VISIBLE_PERCENT));
            serverExtras.put(DataKeys.MAX_BUFFER_MS, extractHeader(headers,
                    ResponseHeader.MAX_BUFFER_MS));

            builder.setEventDetails(new EventDetails.Builder()
                            .adUnitId(mAdUnitId)
                            .adType(adTypeString)
                            .adNetworkType(networkType)
                            .adWidthPx(width)
                            .adHeightPx(height)
                            .dspCreativeId(dspCreativeId)
                            .geoLatitude(location == null ? null : location.getLatitude())
                            .geoLongitude(location == null ? null : location.getLongitude())
                            .geoAccuracy(location == null ? null : location.getAccuracy())
                            .performanceDurationMs(networkResponse.networkTimeMs)
                            .requestId(requestId)
                            .requestStatusCode(networkResponse.statusCode)
                            .requestUri(getUrl())
                            .build()
            );
        }

        // Extract internal video trackers, if available
        final String videoTrackers = extractHeader(headers, ResponseHeader.VIDEO_TRACKERS);
        if (videoTrackers != null) {
            serverExtras.put(DataKeys.VIDEO_TRACKERS_KEY, videoTrackers);
        }
        if (AdType.REWARDED_VIDEO.equals(adTypeString) ||
                (AdType.INTERSTITIAL.equals(adTypeString) &&
                        FullAdType.VAST.equals(fullAdTypeString))) {
            serverExtras.put(DataKeys.EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY,
                    extractHeader(headers, ResponseHeader.VIDEO_VIEWABILITY_TRACKERS));
        }

        // Banner imp tracking
        if (AdFormat.BANNER.equals(mAdFormat)) {
            serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS,
                    extractHeader(headers, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_MS));
            serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS,
                    extractHeader(headers, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_DIPS));
        }

        // Disable viewability vendors, if any
        final String disabledViewabilityVendors = extractHeader(headers,
                ResponseHeader.DISABLE_VIEWABILITY);
        if (!TextUtils.isEmpty(disabledViewabilityVendors)) {
            final ViewabilityVendor disabledVendors =
                    ViewabilityVendor.fromKey(disabledViewabilityVendors);
            if (disabledVendors != null) {
                disabledVendors.disable();
            }
        }

        builder.setServerExtras(serverExtras);

        if (AdType.REWARDED_VIDEO.equals(adTypeString) || AdType.CUSTOM.equals(adTypeString) ||
                AdType.REWARDED_PLAYABLE.equals(adTypeString)) {
            final String rewardedVideoCurrencyName = extractHeader(headers,
                    ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME);
            final String rewardedVideoCurrencyAmount = extractHeader(headers,
                    ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT);
            final String rewardedCurrencies = extractHeader(headers,
                    ResponseHeader.REWARDED_CURRENCIES);
            final String rewardedVideoCompletionUrl = extractHeader(headers,
                    ResponseHeader.REWARDED_VIDEO_COMPLETION_URL);
            final Integer rewardedDuration = extractIntegerHeader(headers,
                    ResponseHeader.REWARDED_DURATION);
            final boolean shouldRewardOnClick = extractBooleanHeader(headers,
                    ResponseHeader.SHOULD_REWARD_ON_CLICK, false);
            builder.setRewardedVideoCurrencyName(rewardedVideoCurrencyName);
            builder.setRewardedVideoCurrencyAmount(rewardedVideoCurrencyAmount);
            builder.setRewardedCurrencies(rewardedCurrencies);
            builder.setRewardedVideoCompletionUrl(rewardedVideoCompletionUrl);
            builder.setRewardedDuration(rewardedDuration);
            builder.setShouldRewardOnClick(shouldRewardOnClick);
        }

        AdResponse adResponse = builder.build();
        logScribeEvent(adResponse, networkResponse, location);

        return Response.success(builder.build(),  // Cast needed for Response generic.
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    private boolean eventDataIsInResponseBody(@Nullable String adType,
            @Nullable String fullAdType) {
        return AdType.MRAID.equals(adType) || AdType.HTML.equals(adType) ||
                (AdType.INTERSTITIAL.equals(adType) && FullAdType.VAST.equals(fullAdType)) ||
                (AdType.REWARDED_VIDEO.equals(adType) && FullAdType.VAST.equals(fullAdType)) ||
                AdType.REWARDED_PLAYABLE.equals(adType);
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
                        .withDspCreativeId(adResponse.getDspCreativeId())
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
