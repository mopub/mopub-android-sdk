package com.mopub.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.DataKeys;
import com.mopub.common.FullAdType;
import com.mopub.common.MoPub;
import com.mopub.common.MoPub.BrowserAgent;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;
import com.mopub.common.util.ResponseHeader;
import com.mopub.mobileads.AdTypeTranslator;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.ExternalViewabilitySessionManager.ViewabilityVendor;
import static com.mopub.network.HeaderUtils.extractBooleanHeader;
import static com.mopub.network.HeaderUtils.extractHeader;
import static com.mopub.network.HeaderUtils.extractIntegerHeader;
import static com.mopub.network.HeaderUtils.extractPercentHeaderString;

public class AdRequest extends MoPubRequest<AdResponse> {

    @VisibleForTesting
    static final String AD_RESPONSES_KEY = "ad-responses";
    private static final String ADM_KEY = "adm";
    private static final String BODY_KEY = "body";
    private static final String HEADERS_KEY = "headers";

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
        super(context, url, listener);
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

        final PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();
        if (personalInfoManager == null) {
            MoPubLog.e("Make sure you initialize the SDK before loading an ad. For now, the SDK " +
                    "will be automatically initialized on your behalf. Starting from release " +
                    "5.2.0, initialization will be a strict requirement, and ad requests " +
                    "made with an uninitialized SDK will begin to fail.");
            MoPub.initializeSdk(context,
                    new SdkConfiguration.Builder(adUnitId == null ? "" : adUnitId).build(), null);
        } else {
            personalInfoManager.requestSync(false);
        }
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

        // convert all keys to lowercase
        final Map<String, String> headers = new HashMap<>();
        for(final String key : networkResponse.headers.keySet() ){
            headers.put(key.toLowerCase(), networkResponse.headers.get(key));
        }

        final JSONObject jsonHeaders;
        final JSONObject currentAdResponse;

        if (extractBooleanHeader(headers, ResponseHeader.WARMUP, false)) {
            return Response.error(new MoPubNetworkError("Ad Unit is warming up.",
                    MoPubNetworkError.Reason.WARMING_UP));
        }

        final AdResponse.Builder builder = new AdResponse.Builder();
        builder.setAdUnitId(mAdUnitId);

        // Response Body encoding / decoding
        final String responseBody = parseStringBody(networkResponse);
        builder.setResponseBody(responseBody);

        if (AdType.MULTI.equalsIgnoreCase(extractHeader(headers, ResponseHeader.AD_RESPONSE_TYPE))) {
            try {
                final JSONObject rootBody = new JSONObject(responseBody);
                final JSONArray adResponsesJson = rootBody.getJSONArray(AD_RESPONSES_KEY);
                // Currently, there is only one ad response. Client-side waterfall will be
                // able to handle multiple ad responses.
                currentAdResponse = adResponsesJson.getJSONObject(0);
                jsonHeaders = currentAdResponse.getJSONObject(HEADERS_KEY);
            } catch (JSONException e) {
                return Response.error(
                        new MoPubNetworkError("Failed to decode header JSON",
                                e, MoPubNetworkError.Reason.BAD_HEADER_DATA));
            }
        } else {
            jsonHeaders = new JSONObject(headers);
            currentAdResponse = null;
        }

        String adTypeString = extractHeader(jsonHeaders, ResponseHeader.AD_TYPE);
        String fullAdTypeString = extractHeader(jsonHeaders, ResponseHeader.FULL_AD_TYPE);
        builder.setAdType(adTypeString);
        builder.setFullAdType(fullAdTypeString);

        // In the case of a CLEAR response, the REFRESH_TIME header must still be respected. Ensure
        // that it is parsed and passed along to the MoPubNetworkError.
        final Integer refreshTimeSeconds = extractIntegerHeader(jsonHeaders,
                ResponseHeader.REFRESH_TIME);
        final Integer refreshTimeMilliseconds = refreshTimeSeconds == null
                ? null
                : refreshTimeSeconds * 1000;
        builder.setRefreshTimeMilliseconds(refreshTimeMilliseconds);

        if (AdType.CLEAR.equals(adTypeString)) {
            final AdResponse adResponse = builder.build();
            return Response.error(
                    new MoPubNetworkError(
                            "No ads found for ad unit.",
                            MoPubNetworkError.Reason.NO_FILL,
                            refreshTimeMilliseconds
                    )
            );
        }

        String dspCreativeId = extractHeader(jsonHeaders, ResponseHeader.DSP_CREATIVE_ID);
        builder.setDspCreativeId(dspCreativeId);

        String networkType = extractHeader(jsonHeaders, ResponseHeader.NETWORK_TYPE);
        builder.setNetworkType(networkType);

        String redirectUrl = extractHeader(jsonHeaders, ResponseHeader.REDIRECT_URL);
        builder.setRedirectUrl(redirectUrl);

        // X-Clickthrough is parsed into the AdResponse as the click tracker
        // Used by AdViewController, Rewarded Video, Native Adapter, MoPubNative
        String clickTrackingUrl = extractHeader(jsonHeaders, ResponseHeader.CLICK_TRACKING_URL);
        builder.setClickTrackingUrl(clickTrackingUrl);

        builder.setImpressionTrackingUrl(extractHeader(jsonHeaders, ResponseHeader.IMPRESSION_URL));

        String failUrl = extractHeader(jsonHeaders, ResponseHeader.FAIL_URL);
        builder.setFailoverUrl(failUrl);

        String requestId = getRequestId(failUrl);
        builder.setRequestId(requestId);

        boolean isScrollable = extractBooleanHeader(jsonHeaders, ResponseHeader.SCROLLABLE, false);
        builder.setScrollable(isScrollable);

        Integer width = extractIntegerHeader(jsonHeaders, ResponseHeader.WIDTH);
        Integer height = extractIntegerHeader(jsonHeaders, ResponseHeader.HEIGHT);
        builder.setDimensions(width, height);

        Integer adTimeoutDelaySeconds = extractIntegerHeader(jsonHeaders, ResponseHeader.AD_TIMEOUT);
        builder.setAdTimeoutDelayMilliseconds(
                adTimeoutDelaySeconds == null
                        ? null
                        : adTimeoutDelaySeconds * 1000);

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
                fullAdTypeString, jsonHeaders);
        builder.setCustomEventClassName(customEventClassName);

        // Default browser agent from X-Browser-Agent header
        BrowserAgent browserAgent = BrowserAgent.fromHeader(
                extractIntegerHeader(jsonHeaders, ResponseHeader.BROWSER_AGENT));
        MoPub.setBrowserAgentFromAdServer(browserAgent);
        builder.setBrowserAgent(browserAgent);

        // Process server extras if they are present:
        String customEventData = extractHeader(jsonHeaders, ResponseHeader.CUSTOM_EVENT_DATA);

        // Some server-supported custom events (like Millennial banners) use a different header field
        if (TextUtils.isEmpty(customEventData)) {
            customEventData = extractHeader(jsonHeaders, ResponseHeader.NATIVE_PARAMS);
        }

        final Map<String, String> serverExtras;
        try {
            serverExtras = Json.jsonStringToMap(customEventData);
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError("Failed to decode server extras for custom event data.",
                            e, MoPubNetworkError.Reason.BAD_HEADER_DATA));
        }

        try {
            if (currentAdResponse != null) {
                serverExtras.put(DataKeys.ADM_KEY, currentAdResponse.getString(ADM_KEY));
            }
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError("Failed to parse ADM for advanced bidding",
                            e, MoPubNetworkError.Reason.BAD_BODY));
        }

        if (!TextUtils.isEmpty(redirectUrl)) {
            serverExtras.put(DataKeys.REDIRECT_URL_KEY, redirectUrl);
        }
        if (!TextUtils.isEmpty(clickTrackingUrl)) {
            // X-Clickthrough parsed into serverExtras
            // Used by Banner, Interstitial
            serverExtras.put(DataKeys.CLICKTHROUGH_URL_KEY, clickTrackingUrl);
        }
        if (eventDataIsInResponseBody(adTypeString, fullAdTypeString)) {
            // Some MoPub-specific custom events get their serverExtras from the response itself:
            serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, responseBody);
            serverExtras.put(DataKeys.SCROLLABLE_KEY, Boolean.toString(isScrollable));
            serverExtras.put(DataKeys.CREATIVE_ORIENTATION_KEY, extractHeader(jsonHeaders, ResponseHeader.ORIENTATION));
        }
        if (AdType.STATIC_NATIVE.equals(adTypeString) || AdType.VIDEO_NATIVE.equals(adTypeString)) {
            final String impressionMinVisiblePercent = extractPercentHeaderString(jsonHeaders,
                    ResponseHeader.IMPRESSION_MIN_VISIBLE_PERCENT);
            final String impressionVisibleMS = extractHeader(jsonHeaders,
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
                    extractPercentHeaderString(jsonHeaders, ResponseHeader.PLAY_VISIBLE_PERCENT));
            serverExtras.put(DataKeys.PAUSE_VISIBLE_PERCENT,
                    extractPercentHeaderString(jsonHeaders, ResponseHeader.PAUSE_VISIBLE_PERCENT));
            serverExtras.put(DataKeys.MAX_BUFFER_MS, extractHeader(jsonHeaders,
                    ResponseHeader.MAX_BUFFER_MS));
        }

        // Extract internal video trackers, if available
        final String videoTrackers = extractHeader(jsonHeaders, ResponseHeader.VIDEO_TRACKERS);
        if (!TextUtils.isEmpty(videoTrackers)) {
            serverExtras.put(DataKeys.VIDEO_TRACKERS_KEY, videoTrackers);
        }
        if (AdType.REWARDED_VIDEO.equals(adTypeString) ||
                (AdType.INTERSTITIAL.equals(adTypeString) &&
                        FullAdType.VAST.equals(fullAdTypeString))) {
            serverExtras.put(DataKeys.EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY,
                    extractHeader(jsonHeaders, ResponseHeader.VIDEO_VIEWABILITY_TRACKERS));
        }

        // Banner imp tracking
        if (AdFormat.BANNER.equals(mAdFormat)) {
            serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS,
                    extractHeader(headers, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_MS));
            serverExtras.put(DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS,
                    extractHeader(headers, ResponseHeader.BANNER_IMPRESSION_MIN_VISIBLE_DIPS));
        }

        // Disable viewability vendors, if any
        final String disabledViewabilityVendors = extractHeader(jsonHeaders,
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
            final String rewardedVideoCurrencyName = extractHeader(jsonHeaders,
                    ResponseHeader.REWARDED_VIDEO_CURRENCY_NAME);
            final String rewardedVideoCurrencyAmount = extractHeader(jsonHeaders,
                    ResponseHeader.REWARDED_VIDEO_CURRENCY_AMOUNT);
            final String rewardedCurrencies = extractHeader(jsonHeaders,
                    ResponseHeader.REWARDED_CURRENCIES);
            final String rewardedVideoCompletionUrl = extractHeader(jsonHeaders,
                    ResponseHeader.REWARDED_VIDEO_COMPLETION_URL);
            final Integer rewardedDuration = extractIntegerHeader(jsonHeaders,
                    ResponseHeader.REWARDED_DURATION);
            final boolean shouldRewardOnClick = extractBooleanHeader(jsonHeaders,
                    ResponseHeader.SHOULD_REWARD_ON_CLICK, false);
            builder.setRewardedVideoCurrencyName(rewardedVideoCurrencyName);
            builder.setRewardedVideoCurrencyAmount(rewardedVideoCurrencyAmount);
            builder.setRewardedCurrencies(rewardedCurrencies);
            builder.setRewardedVideoCompletionUrl(rewardedVideoCompletionUrl);
            builder.setRewardedDuration(rewardedDuration);
            builder.setShouldRewardOnClick(shouldRewardOnClick);
        }

        final boolean invalidateConsent = extractBooleanHeader(jsonHeaders, ResponseHeader.INVALIDATE_CONSENT, false);
        final boolean forceExplicitNo = extractBooleanHeader(jsonHeaders, ResponseHeader.FORCE_EXPLICIT_NO, false);
        final boolean reacquireConsent = extractBooleanHeader(jsonHeaders, ResponseHeader.REACQUIRE_CONSENT, false);
        String consentChangeReason = extractHeader(jsonHeaders, ResponseHeader.CONSENT_CHANGE_REASON);

        if (MoPub.getPersonalInformationManager() != null) {
            if (forceExplicitNo) {
                MoPub.getPersonalInformationManager().forceExplicitNo(consentChangeReason);
            } else if (invalidateConsent) {
                MoPub.getPersonalInformationManager().invalidateConsent(consentChangeReason);
            } else if (reacquireConsent) {
                MoPub.getPersonalInformationManager().reacquireConsent(consentChangeReason);
            }
        }

        AdResponse adResponse = builder.build();

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
}
