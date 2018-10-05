// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.toolbox.HttpHeaderParser;

/**
 * Volley request class helper to support ad requests specifics
 */
public class MultiAdRequest extends MoPubRequest<MultiAdResponse> {

    @NonNull
    public final Listener mListener;
    @NonNull
    final AdFormat mAdFormat;
    @Nullable
    final String mAdUnitId;
    @NonNull
    private final Context mContext;

    private int hashCode = 0;

    public interface Listener extends Response.ErrorListener {
        void onSuccessResponse(MultiAdResponse response);
    }

    MultiAdRequest(@NonNull final String url,
                   @NonNull final AdFormat adFormat,
                   @Nullable final String adUnitId,
                   @NonNull final Context context,
                   @NonNull final Listener listener) {
        super(context, clearUrlIfSdkNotInitialized(url), listener);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(context);
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
        if (personalInfoManager != null) {
            personalInfoManager.requestSync(false);
        }
    }

    /**
     * For 5.2.0 and onwards, disable load when the sdk is not initialized.
     *
     * @param url The original url
     * @return The original url if the sdk is initialized. Otherwise, returns an empty url.
     */
    @NonNull
    private static String clearUrlIfSdkNotInitialized(@NonNull final String url) {
        if (MoPub.getPersonalInformationManager() == null || !MoPub.isSdkInitialized()) {
            MoPubLog.e("Make sure to call MoPub#initializeSdk before loading an ad.");
            return "";
        }
        return url;
    }

    /**
     * Callback from Volley to parse network response
     * @param networkResponse data to be parsed
     * @return valid response or null in case of error
     */
    @Nullable
    @Override
    protected Response<MultiAdResponse> parseNetworkResponse(NetworkResponse networkResponse) {
        MultiAdResponse multiAdResponse;
        try {
            multiAdResponse = new MultiAdResponse(mContext, networkResponse, mAdFormat, mAdUnitId);
        } catch (Exception ex) {
            if (ex instanceof MoPubNetworkError) {
                return Response.error((MoPubNetworkError) ex);
            }
            // Volley network error
            return Response.error(new MoPubNetworkError(ex, MoPubNetworkError.Reason.UNSPECIFIED));
        }

        return Response.success(multiAdResponse, HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    /**
     * Callback from Volley to deliver successful result to listener
     * @param multiAdResponse valid object {@link MultiAdResponse}
     */
    @Override
    protected void deliverResponse(MultiAdResponse multiAdResponse) {
        if (!isCanceled()) {
            mListener.onSuccessResponse(multiAdResponse);
        }
    }

    @Override
    public void cancel() {
        super.cancel();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof MultiAdRequest)) {
            return false;
        }

        MultiAdRequest other = (MultiAdRequest) obj;
        int res = 0;
        if (mAdUnitId != null) {
            res = other.mAdUnitId == null ? 1 : mAdUnitId.compareTo(other.mAdUnitId);
        } else if (other.mAdUnitId != null) {
            res = -1;
        }

        return res == 0
                && mAdFormat == other.mAdFormat
                && getUrl().compareTo(other.getUrl()) == 0;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int result = mAdUnitId == null ? 29 : mAdUnitId.hashCode();
            result = 31 * result + mAdFormat.hashCode();
            result = 31 * result + getOriginalUrl().hashCode();
            hashCode = result;
        }
        return hashCode;
    }
}
