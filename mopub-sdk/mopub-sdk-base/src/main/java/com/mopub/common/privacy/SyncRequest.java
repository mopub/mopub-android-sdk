package com.mopub.common.privacy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequest;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncRequest extends MoPubRequest<SyncResponse> {

    public interface Listener extends Response.ErrorListener {
        void onSuccess(SyncResponse response);
    }

    @Nullable private Listener mListener;

    public SyncRequest(@NonNull final Context context,
            @NonNull final String url,
            @Nullable final Listener listener) {
        super(context, url, listener);

        mListener = listener;

        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        setRetryPolicy(retryPolicy);
        setShouldCache(false);
    }

    @Override
    protected Response<SyncResponse> parseNetworkResponse(final NetworkResponse networkResponse) {
        final SyncResponse.Builder builder = new SyncResponse.Builder();
        final String responseBody = parseStringBody(networkResponse);

        try {
            final JSONObject jsonBody = new JSONObject(responseBody);
            builder.setIsGdprRegion(jsonBody.getString(PrivacyKey.IS_GDPR_REGION.getKey()))
                    .setForceExplicitNo(jsonBody.optString(PrivacyKey.FORCE_EXPLICIT_NO.getKey()))
                    .setInvalidateConsent(
                            jsonBody.optString(PrivacyKey.INVALIDATE_CONSENT.getKey()))
                    .setReacquireConsent(jsonBody.optString(PrivacyKey.REACQUIRE_CONSENT.getKey()))
                    .setIsWhitelisted(jsonBody.getString(PrivacyKey.IS_WHITELISTED.getKey()))
                    .setCurrentVendorListVersion(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_VERSION.getKey()))
                    .setCurrentVendorListLink(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_LINK.getKey()))
                    .setCurrentPrivacyPolicyLink(
                            jsonBody.getString(PrivacyKey.CURRENT_PRIVACY_POLICY_LINK.getKey()))
                    .setCurrentPrivacyPolicyVersion(
                            jsonBody.getString(PrivacyKey.CURRENT_PRIVACY_POLICY_VERSION.getKey()))
                    .setCurrentVendorListIabFormat(
                            jsonBody.optString(PrivacyKey.CURRENT_VENDOR_LIST_IAB_FORMAT.getKey()))
                    .setCurrentVendorListIabHash(
                            jsonBody.getString(PrivacyKey.CURRENT_VENDOR_LIST_IAB_HASH.getKey()))
                    .setCallAgainAfterSecs(
                            jsonBody.optString(PrivacyKey.CALL_AGAIN_AFTER_SECS.getKey()))
                    .setExtras(jsonBody.optString(PrivacyKey.EXTRAS.getKey()))
                    .setConsentChangeReason(
                            jsonBody.optString(PrivacyKey.CONSENT_CHANGE_REASON.getKey()));
        } catch (JSONException e) {
            return Response.error(
                    new MoPubNetworkError(
                            "Unable to parse sync request network response.",
                            MoPubNetworkError.Reason.BAD_BODY,
                            null
                    )
            );
        }

        return Response.success(builder.build(),
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    @Override
    protected void deliverResponse(final SyncResponse syncResponse) {
        if (mListener != null) {
            mListener.onSuccess(syncResponse);
        }
    }
}
