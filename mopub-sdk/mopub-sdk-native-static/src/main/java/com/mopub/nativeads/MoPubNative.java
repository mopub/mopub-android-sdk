package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.ManifestUtils;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.GpsHelper.fetchAdvertisingInfoAsync;
import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.INVALID_REQUEST_URL;
import static com.mopub.nativeads.NativeErrorCode.INVALID_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.NATIVE_RENDERER_CONFIGURATION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.UNSPECIFIED;

public class MoPubNative {

    public interface MoPubNativeNetworkListener {
        void onNativeLoad(final NativeAd nativeAd);
        void onNativeFail(final NativeErrorCode errorCode);
    }

    static final MoPubNativeNetworkListener EMPTY_NETWORK_LISTENER =
            new MoPubNativeNetworkListener() {
        @Override
        public void onNativeLoad(@NonNull final NativeAd nativeAd) {
            // If this listener is invoked, it means that MoPubNative instance has been destroyed
            // so destroy any leftover incoming NativeAds
            nativeAd.destroy();
        }
        @Override
        public void onNativeFail(final NativeErrorCode errorCode) {
        }
    };

    // Highly recommended to be an Activity since 3rd party networks need it
    @NonNull private final WeakReference<Context> mContext;
    @NonNull private final String mAdUnitId;
    @NonNull private MoPubNativeNetworkListener mMoPubNativeNetworkListener;

    // For small sets TreeMap, takes up less memory than HashMap
    @NonNull private Map<String, Object> mLocalExtras = new TreeMap<String, Object>();
    @NonNull private final AdRequest.Listener mVolleyListener;
    @Nullable private AdRequest mNativeRequest;
    @NonNull AdRendererRegistry mAdRendererRegistry;

    public MoPubNative(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull final MoPubNativeNetworkListener moPubNativeNetworkListener) {
        this(context, adUnitId, new AdRendererRegistry(), moPubNativeNetworkListener);
    }

    @VisibleForTesting
    public MoPubNative(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull AdRendererRegistry adRendererRegistry,
            @NonNull final MoPubNativeNetworkListener moPubNativeNetworkListener) {
        Preconditions.checkNotNull(context, "context may not be null.");
        Preconditions.checkNotNull(adUnitId, "AdUnitId may not be null.");
        Preconditions.checkNotNull(adRendererRegistry, "AdRendererRegistry may not be null.");
        Preconditions.checkNotNull(moPubNativeNetworkListener, "MoPubNativeNetworkListener may not be null.");

        ManifestUtils.checkNativeActivitiesDeclared(context);

        mContext = new WeakReference<Context>(context);
        mAdUnitId = adUnitId;
        mMoPubNativeNetworkListener = moPubNativeNetworkListener;
        mAdRendererRegistry = adRendererRegistry;
        mVolleyListener = new AdRequest.Listener() {
            @Override
            public void onSuccess(@NonNull final AdResponse response) {
                onAdLoad(response);
            }

            @Override
            public void onErrorResponse(@NonNull final VolleyError volleyError) {
                onAdError(volleyError);
            }
        };

        // warm up cache for google play services info
        fetchAdvertisingInfoAsync(context, null);
    }

    /**
     * Registers an ad renderer for rendering a specific native ad format.
     * Note that if multiple ad renderers support a specific native ad format, the first
     * one registered will be used.
     */
    public void registerAdRenderer(MoPubAdRenderer moPubAdRenderer) {
        mAdRendererRegistry.registerAdRenderer(moPubAdRenderer);
    }

    public void destroy() {
        mContext.clear();
        if (mNativeRequest != null) {
            mNativeRequest.cancel();
            mNativeRequest = null;
        }
        mMoPubNativeNetworkListener = EMPTY_NETWORK_LISTENER;
    }

    public void setLocalExtras(@Nullable final Map<String, Object> localExtras) {
        if (localExtras == null) {
            mLocalExtras = new TreeMap<String, Object>();
        } else {
            mLocalExtras = new TreeMap<String, Object>(localExtras);
        }
    }

    public void makeRequest() {
        makeRequest((RequestParameters)null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters) {
        makeRequest(requestParameters, null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters,
            @Nullable Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (!DeviceUtils.isNetworkAvailable(context)) {
            mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            return;
        }

        loadNativeAd(requestParameters, sequenceNumber);
    }

    private void loadNativeAd(
            @Nullable final RequestParameters requestParameters,
            @Nullable final Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        final NativeUrlGenerator generator = new NativeUrlGenerator(context)
                .withAdUnitId(mAdUnitId)
                .withRequest(requestParameters);

        if (sequenceNumber != null) {
            generator.withSequenceNumber(sequenceNumber);
        }

        final String endpointUrl = generator.generateUrlString(Constants.HOST);

        if (endpointUrl != null) {
            MoPubLog.d("Loading ad from: " + endpointUrl);
        }

        requestNativeAd(endpointUrl);
    }

    void requestNativeAd(@Nullable final String endpointUrl) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (endpointUrl == null) {
            mMoPubNativeNetworkListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        }

        mNativeRequest = new AdRequest(endpointUrl, AdFormat.NATIVE, mAdUnitId, context, mVolleyListener);
        RequestQueue requestQueue = Networking.getRequestQueue(context);
        requestQueue.add(mNativeRequest);
    }

    private void onAdLoad(@NonNull final AdResponse response) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }
        final CustomEventNativeListener customEventNativeListener =
                new CustomEventNativeListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull final BaseNativeAd nativeAd) {
                        final Context context = getContextOrDestroy();
                        if (context == null) {
                            return;
                        }

                        MoPubAdRenderer renderer = mAdRendererRegistry.getRendererForAd(nativeAd);
                        if (renderer == null) {
                            onNativeAdFailed(NATIVE_RENDERER_CONFIGURATION_ERROR);
                            return;
                        }

                        mMoPubNativeNetworkListener.onNativeLoad(new NativeAd(context,
                                        response.getImpressionTrackingUrl(),
                                        response.getClickTrackingUrl(),
                                        mAdUnitId,
                                        nativeAd,
                                        renderer)
                        );
                    }

                    @Override
                    public void onNativeAdFailed(final NativeErrorCode errorCode) {
                        MoPubLog.v(String.format("Native Ad failed to load with error: %s.", errorCode));
                        requestNativeAd(response.getFailoverUrl());
                    }
                };

        CustomEventNativeAdapter.loadNativeAd(
                context,
                mLocalExtras,
                response,
                customEventNativeListener
        );
    }

    @VisibleForTesting
    void onAdError(@NonNull final VolleyError volleyError) {
        MoPubLog.d("Native ad request failed.", volleyError);
        if (volleyError instanceof MoPubNetworkError) {
            MoPubNetworkError error = (MoPubNetworkError) volleyError;
            switch (error.getReason()) {
                case BAD_BODY:
                    mMoPubNativeNetworkListener.onNativeFail(INVALID_RESPONSE);
                    return;
                case BAD_HEADER_DATA:
                    mMoPubNativeNetworkListener.onNativeFail(INVALID_RESPONSE);
                    return;
                case WARMING_UP:
                    // Used for the sample app to signal a toast.
                    // This is not customer-facing except in the sample app.
                    MoPubLog.c(MoPubErrorCode.WARMUP.toString());
                    mMoPubNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
                    return;
                case NO_FILL:
                    mMoPubNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
                    return;
                case UNSPECIFIED:
                default:
                    mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
                    return;
            }
        } else {
            // Process our other status code errors.
            NetworkResponse response = volleyError.networkResponse;
            if (response != null && response.statusCode >= 500 && response.statusCode < 600) {
                mMoPubNativeNetworkListener.onNativeFail(SERVER_ERROR_RESPONSE_CODE);
            } else if (response == null && !DeviceUtils.isNetworkAvailable(mContext.get())) {
                MoPubLog.c(String.valueOf(MoPubErrorCode.NO_CONNECTION.toString()));
                mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            } else {
                mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
            }
        }
    }

    @VisibleForTesting
    @Nullable
    Context getContextOrDestroy() {
        final Context context = mContext.get();
        if (context == null) {
            destroy();
            MoPubLog.d("Weak reference to Context in MoPubNative became null. This instance" +
                    " of MoPubNative is destroyed and No more requests will be processed.");
        }
        return context;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    MoPubNativeNetworkListener getMoPubNativeNetworkListener() {
        return mMoPubNativeNetworkListener;
    }
}
