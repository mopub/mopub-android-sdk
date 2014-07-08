package com.mopub.nativeads;

import android.content.Context;
import android.view.View;

import com.mopub.common.DownloadResponse;
import com.mopub.common.DownloadTask;
import com.mopub.common.GpsHelper;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.ManifestUtils;
import com.mopub.common.util.MoPubLog;
import com.mopub.common.util.ResponseHeader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.GpsHelper.GpsHelperListener;
import static com.mopub.common.GpsHelper.asyncFetchAdvertisingInfo;
import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.MoPubNative.MoPubNativeListener.EMPTY_MOPUB_NATIVE_LISTENER;
import static com.mopub.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.INVALID_REQUEST_URL;
import static com.mopub.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.UNEXPECTED_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.UNSPECIFIED;

public final class MoPubNative {

    public interface MoPubNativeListener {
        public void onNativeLoad(final NativeResponse nativeResponse);
        public void onNativeFail(final NativeErrorCode errorCode);
        public void onNativeImpression(final View view);
        public void onNativeClick(final View view);

        public static final MoPubNativeListener EMPTY_MOPUB_NATIVE_LISTENER = new MoPubNativeListener() {
            @Override public void onNativeLoad(final NativeResponse nativeResponse) {}
            @Override public void onNativeFail(final NativeErrorCode errorCode) {}
            @Override public void onNativeImpression(final View view) {}
            @Override public void onNativeClick(final View view) {}
        };
    }

    // must be an activity context since 3rd party networks need it
    private final WeakReference<Context> mContext;
    private final String mAdUnitId;
    private MoPubNativeListener mMoPubNativeListener;
    private Map<String, Object> mLocalExtras;

    public MoPubNative(final Context context, final String adUnitId, final MoPubNativeListener moPubNativeListener) {
        ImpressionTrackingManager.start();

        if (context == null) {
            throw new IllegalArgumentException("Context may not be null.");
        } else if (adUnitId == null) {
            throw new IllegalArgumentException("AdUnitId may not be null.");
        } else if (moPubNativeListener == null) {
            throw new IllegalArgumentException("MoPubNativeListener may not be null.");
        }

        ManifestUtils.checkNativeActivitiesDeclared(context);

        mContext = new WeakReference<Context>(context);
        mAdUnitId = adUnitId;
        mMoPubNativeListener = moPubNativeListener;
        
        // warm up cache for google play services info
        asyncFetchAdvertisingInfo(context);
    }

    public void destroy() {
        mContext.clear();
        mMoPubNativeListener = EMPTY_MOPUB_NATIVE_LISTENER;
    }

    public void setLocalExtras(final Map<String, Object> localExtras) {
        mLocalExtras = new HashMap<String, Object>(localExtras);
    }

    public void makeRequest() {
        final RequestParameters requestParameters = null;
        makeRequest(requestParameters);
    }

    public void makeRequest(final RequestParameters requestParameters) {
        makeRequest(new NativeGpsHelperListener(requestParameters));
    }

    void makeRequest(final NativeGpsHelperListener nativeGpsHelperListener) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (!DeviceUtils.isNetworkAvailable(context)) {
            mMoPubNativeListener.onNativeFail(CONNECTION_ERROR);
            return;
        }

        // If we have access to Google Play Services (GPS) but the advertising info
        // is not cached then guarantee we get it before building the ad request url
        // in the callback, this is a requirement from Google
        GpsHelper.asyncFetchAdvertisingInfoIfNotCached(
                context,
                nativeGpsHelperListener
        );
    }

    void loadNativeAd(final RequestParameters requestParameters) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        final String endpointUrl = new NativeUrlGenerator(context)
                .withAdUnitId(mAdUnitId)
                .withRequest(requestParameters)
                .generateUrlString(Constants.NATIVE_HOST);

        if (endpointUrl != null) {
            MoPubLog.d("Loading ad from: " + endpointUrl);
        }

        requestNativeAd(endpointUrl);
    }

    private void requestNativeAd(final String endpointUrl) {
        final HttpGet httpGet;
        try {
            httpGet = new HttpGet(endpointUrl);
        } catch (IllegalArgumentException e) {
            mMoPubNativeListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        } catch (NullPointerException e){
            mMoPubNativeListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        }

        downloadJson(httpGet);
    }

    private void downloadJson(final HttpUriRequest httpUriRequest) {
        final DownloadTask jsonDownloadTask = new DownloadTask(new DownloadTask.DownloadTaskListener() {
            @Override
            public void onComplete(final String url, final DownloadResponse downloadResponse) {
                if (downloadResponse == null) {
                    mMoPubNativeListener.onNativeFail(UNSPECIFIED);
                } else if (downloadResponse.getStatusCode() >= 500 &&
                        downloadResponse.getStatusCode() < 600) {
                    mMoPubNativeListener.onNativeFail(SERVER_ERROR_RESPONSE_CODE);
                } else if (downloadResponse.getStatusCode() != HttpStatus.SC_OK) {
                    mMoPubNativeListener.onNativeFail(UNEXPECTED_RESPONSE_CODE);
                } else if (downloadResponse.getContentLength() == 0) {
                    mMoPubNativeListener.onNativeFail(EMPTY_AD_RESPONSE);
                } else {
                    final CustomEventNativeListener customEventNativeListener = new CustomEventNativeListener() {
                        @Override
                        public void onNativeAdLoaded(final NativeAdInterface nativeAd) {
                            final Context context = getContextOrDestroy();
                            if (context == null) {
                                return;
                            }
                            mMoPubNativeListener.onNativeLoad(new NativeResponse(context, downloadResponse, nativeAd, mMoPubNativeListener));
                        }

                        @Override
                        public void onNativeAdFailed(final NativeErrorCode errorCode) {
                            requestNativeAd(downloadResponse.getFirstHeader(ResponseHeader.FAIL_URL));
                        }
                    };

                    final Context context = getContextOrDestroy();
                    if (context == null) {
                        return;
                    }
                    CustomEventNativeAdapter.loadNativeAd(
                            context,
                            mLocalExtras,
                            downloadResponse,
                            customEventNativeListener
                    );
                }
            }
        });

        try {
            AsyncTasks.safeExecuteOnExecutor(jsonDownloadTask, httpUriRequest);
        } catch (Exception e) {
            MoPubLog.d("Failed to download json", e);

            mMoPubNativeListener.onNativeFail(UNSPECIFIED);
        }

    }

    Context getContextOrDestroy() {
        final Context context = mContext.get();
        if (context == null) {
            destroy();
            MoPubLog.d("Weak reference to Activity Context in MoPubNative became null. This instance" +
                    " of MoPubNative is destroyed and No more requests will be processed.");
        }
        return context;
    }

    // Do not store this class as a member of MoPubNative; will result in circular reference
    class NativeGpsHelperListener implements GpsHelperListener {
        private RequestParameters mRequestParameters;
        NativeGpsHelperListener(RequestParameters requestParameters) {
            mRequestParameters = requestParameters;
        }
        @Override
        public void onFetchAdInfoCompleted() {
            loadNativeAd(mRequestParameters);
        }
    }

    @Deprecated
    MoPubNativeListener getMoPubNativeListener() {
        return mMoPubNativeListener;
    }
}
