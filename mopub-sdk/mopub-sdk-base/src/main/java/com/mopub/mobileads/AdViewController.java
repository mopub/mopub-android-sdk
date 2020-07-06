// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.Utils;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.SingleImpression;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;
import static com.mopub.mobileads.AdData.DEFAULT_FULLSCREEN_TIMEOUT_DELAY;
import static com.mopub.mobileads.AdData.DEFAULT_INLINE_TIMEOUT_DELAY;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.INTERNAL_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class AdViewController implements AdLifecycleListener.LoadListener, AdLifecycleListener.InteractionListener {
    static final int DEFAULT_REFRESH_TIME_MILLISECONDS = 60000;  // 1 minute
    private static final int MAX_REFRESH_TIME_MILLISECONDS = 600000; // 10 minutes
    private static final double BACKOFF_FACTOR = 1.5;
    private static final FrameLayout.LayoutParams WRAP_AND_CENTER_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
    private final static WeakHashMap<View, Boolean> sViewShouldHonorServerDimensions = new WeakHashMap<>();

    private static final String INLINE_AD_ADAPTER =
            "com.mopub.mobileads.InlineAdAdapter";

    private static final String FULLSCREEN_AD_ADAPTER =
            "com.mopub.mobileads.FullscreenAdAdapter";

    private final long mBroadcastIdentifier;

    @Nullable
    private Context mContext;
    @Nullable
    private MoPubAd mMoPubAd;
    @Nullable
    private WebViewAdUrlGenerator mUrlGenerator;

    @Nullable
    private Request mActiveRequest;
    @Nullable
    AdLoader mAdLoader;
    @NonNull
    private final AdLoader.Listener mAdListener;
    @Nullable
    private AdResponse mAdResponse;
    @Nullable
    private String mBaseAdClassName;
    private final Runnable mRefreshRunnable;

    private boolean mIsDestroyed;
    private Handler mHandler;
    private boolean mHasOverlay;

    // This is the power of the exponential term in the exponential backoff calculation.
    @VisibleForTesting
    int mBackoffPower = 1;

    private Map<String, Object> mLocalExtras = new HashMap<>();

    /**
     * This is the current auto refresh status. If this is true, then ads will attempt to refresh.
     * If mRefreshTimeMillis is null or not greater than 0, the auto refresh runnable will not
     * be called.
     */
    private boolean mCurrentAutoRefreshStatus = true;

    /**
     * This is the publisher-specified auto refresh flag. AdViewController will only attempt to
     * refresh ads when this is true. Setting this to false will block refreshing.
     */
    private boolean mShouldAllowAutoRefresh = true;

    private String mKeywords;
    private String mUserDataKeywords;
    private Point mRequestedAdSize;
    private WindowInsets mWindowInsets;
    private boolean mIsTesting;
    private boolean mAdWasLoaded;
    private AdAdapter mAdAdapter;
    @Nullable
    private String mAdUnitId;
    @Nullable
    private Integer mRefreshTimeMillis;
    @NonNull
    private String mLastTrackedRequestId;

    public static void setShouldHonorServerDimensions(View view) {
        sViewShouldHonorServerDimensions.put(view, true);
    }

    private static boolean getShouldHonorServerDimensions(View view) {
        return sViewShouldHonorServerDimensions.get(view) != null;
    }

    public AdViewController(@NonNull Context context, @NonNull MoPubAd moPubAd) {
        mContext = context;
        mMoPubAd = moPubAd;

        // Timeout value of less than 0 means use the ad format's default timeout
        mBroadcastIdentifier = Utils.generateUniqueId();

        mUrlGenerator = new WebViewAdUrlGenerator(mContext.getApplicationContext());

        mAdListener = new AdLoader.Listener() {
            @Override
            public void onSuccess(final AdResponse response) {
                onAdLoadSuccess(response);
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                onAdLoadError(volleyError);
            }
        };

        mRefreshRunnable = new Runnable() {
            public void run() {
                final MoPubAd moPubAd = getMoPubAd();
                if (moPubAd != null) {
                    setRequestedAdSize(moPubAd.resolveAdSize());
                }
                internalLoadAd();
            }
        };
        mRefreshTimeMillis = DEFAULT_REFRESH_TIME_MILLISECONDS;
        mHandler = new Handler();
        mLastTrackedRequestId = "";
    }

    @VisibleForTesting
    void onAdLoadSuccess(@NonNull final AdResponse adResponse) {
        mBackoffPower = 1;
        mAdResponse = adResponse;
        mBaseAdClassName = adResponse.getBaseAdClassName();
        // Do other ad loading setup. See AdFetcher & AdLoadTask.
        mRefreshTimeMillis = mAdResponse.getRefreshTimeMillis();
        mActiveRequest = null;

        loadBaseAd();

        scheduleRefreshTimerIfEnabled();
    }

    @VisibleForTesting
    void onAdLoadError(final VolleyError error) {
        if (error instanceof MoPubNetworkError) {
            // If provided, the MoPubNetworkError's refresh time takes precedence over the
            // previously set refresh time.
            // The only types of NetworkErrors that can possibly modify
            // an ad's refresh time are CLEAR requests. For CLEAR requests that (erroneously) omit a
            // refresh time header and for all other non-CLEAR types of NetworkErrors, we simply
            // maintain the previous refresh time value.
            final MoPubNetworkError moPubNetworkError = (MoPubNetworkError) error;
            if (moPubNetworkError.getRefreshTimeMillis() != null) {
                mRefreshTimeMillis = moPubNetworkError.getRefreshTimeMillis();
            }
        }

        final MoPubErrorCode errorCode = getErrorCodeFromVolleyError(error, mContext);
        if (errorCode == MoPubErrorCode.SERVER_ERROR) {
            mBackoffPower++;
        }

        adDidFail(errorCode);
    }

    @VisibleForTesting
    @NonNull
    static MoPubErrorCode getErrorCodeFromVolleyError(@NonNull final VolleyError error,
                                                      @Nullable final Context context) {
        final NetworkResponse networkResponse = error.networkResponse;

        // For MoPubNetworkErrors, networkResponse is null.
        if (error instanceof MoPubNetworkError) {
            switch (((MoPubNetworkError) error).getReason()) {
                case WARMING_UP:
                    return MoPubErrorCode.WARMUP;
                case NO_FILL:
                    return MoPubErrorCode.NO_FILL;
                default:
                    return UNSPECIFIED;
            }
        }

        if (networkResponse == null) {
            if (!DeviceUtils.isNetworkAvailable(context)) {
                return MoPubErrorCode.NO_CONNECTION;
            }
            return UNSPECIFIED;
        }

        if (error.networkResponse.statusCode >= 400) {
            return MoPubErrorCode.SERVER_ERROR;
        }

        return UNSPECIFIED;
    }

    @Nullable
    public MoPubAd getMoPubAd() {
        return mMoPubAd;
    }

    @Nullable
    public AdAdapter getAdAdapter() {
        return mAdAdapter;
    }

    @Nullable
    public Context getContext() {
        return mContext;
    }

    public void loadAd() {
        mBackoffPower = 1;
        internalLoadAd();
    }

    private void internalLoadAd() {
        mAdWasLoaded = true;
        if (TextUtils.isEmpty(mAdUnitId)) {
            MoPubLog.log(CUSTOM, "Can't load an ad in this ad view because the ad unit ID is not set. " +
                    "Did you forget to call setAdUnitId()?");
            adDidFail(MoPubErrorCode.MISSING_AD_UNIT_ID);
            return;
        }

        if (!isNetworkAvailable()) {
            MoPubLog.log(CUSTOM, "Can't load an ad because there is no network connectivity.");
            adDidFail(MoPubErrorCode.NO_CONNECTION);
            return;
        }

        String adUrl = generateAdUrl();
        loadNonJavascript(adUrl, null);
    }

    void loadNonJavascript(@Nullable final String url, @Nullable final MoPubError moPubError) {
        if (url == null) {
            adDidFail(MoPubErrorCode.NO_FILL);
            return;
        }

        if (!url.startsWith("javascript:")) {
            MoPubLog.log(CUSTOM, "Loading url: " + url);
        }

        if (mActiveRequest != null) {
            if (!TextUtils.isEmpty(mAdUnitId)) {  // This shouldn't be able to happen?
                MoPubLog.log(CUSTOM, "Already loading an ad for " + mAdUnitId + ", wait to finish.");
            }
            return;
        }

        fetchAd(url, moPubError);
    }

    @Deprecated
    public void reload() {
        loadAd();
    }

    /**
     * Returns true if continuing to load the failover url, false if the ad actually did not fill.
     */
    boolean loadFailUrl(final MoPubErrorCode errorCode) {
        if (errorCode == null) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Load failed.",
                    UNSPECIFIED.getIntCode(),
                    UNSPECIFIED);
        } else {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Load failed.",
                    errorCode,
                    errorCode.getIntCode());
        }

        if (mAdLoader != null && mAdLoader.hasMoreAds()) {
            loadNonJavascript("", errorCode);
            return true;
        } else {
            // No other URLs to try, so signal a failure.
            adDidFail(MoPubErrorCode.NO_FILL);
            return false;
        }
    }

    void setNotLoading() {
        if (mActiveRequest != null) {
            if (!mActiveRequest.isCanceled()) {
                mActiveRequest.cancel();
            }
            mActiveRequest = null;
        }
        mAdLoader = null;
    }


    public String getKeywords() {
        return mKeywords;
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public String getUserDataKeywords() {
        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }
        return mUserDataKeywords;
    }

    public void setUserDataKeywords(String userDataKeywords) {
        if (!MoPub.canCollectPersonalInformation()) {
            mUserDataKeywords = null;
            return;
        }
        mUserDataKeywords = userDataKeywords;
    }

    public Location getLocation() {
        return LocationService.getLastKnownLocation(mContext);
    }

    public void setLocation(Location location) {
    }

    void setRequestedAdSize(final Point requestedAdSize) {
        mRequestedAdSize = requestedAdSize;
    }

    public void setWindowInsets(final WindowInsets windowInsets) {
        mWindowInsets = windowInsets;
    }

    public String getAdUnitId() {
        return mAdUnitId;
    }

    @Nullable
    public String getBaseAdClassName() {
        return mBaseAdClassName;
    }

    public void setAdUnitId(@NonNull String adUnitId) {
        mAdUnitId = adUnitId;
    }

    public long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    public int getAdWidth() {
        if (mAdResponse != null && mAdResponse.getWidth() != null) {
            return mAdResponse.getWidth();
        }

        return 0;
    }

    public int getAdHeight() {
        if (mAdResponse != null && mAdResponse.getHeight() != null) {
            return mAdResponse.getHeight();
        }

        return 0;
    }

    /**
     * This has been renamed to {@link #getCurrentAutoRefreshStatus()}.
     */
    @Deprecated
    public boolean getAutorefreshEnabled() {
        return getCurrentAutoRefreshStatus();
    }

    public boolean getCurrentAutoRefreshStatus() {
        return mCurrentAutoRefreshStatus;
    }

    void pauseRefresh() {
        setAutoRefreshStatus(false);
    }

    void resumeRefresh() {
        if (mShouldAllowAutoRefresh && !mHasOverlay) {
            setAutoRefreshStatus(true);
        }
    }

    void setShouldAllowAutoRefresh(final boolean shouldAllowAutoRefresh) {
        mShouldAllowAutoRefresh = shouldAllowAutoRefresh;
        setAutoRefreshStatus(shouldAllowAutoRefresh);
    }

    private void setAutoRefreshStatus(final boolean newAutoRefreshStatus) {
        final boolean autoRefreshStatusChanged = mAdWasLoaded &&
                (mCurrentAutoRefreshStatus != newAutoRefreshStatus);
        if (autoRefreshStatusChanged) {
            final String enabledString = (newAutoRefreshStatus) ? "enabled" : "disabled";
            MoPubLog.log(CUSTOM, "Refresh " + enabledString + " for ad unit (" + mAdUnitId + ").");
        }

        mCurrentAutoRefreshStatus = newAutoRefreshStatus;
        if (mAdWasLoaded && mCurrentAutoRefreshStatus) {
            scheduleRefreshTimerIfEnabled();
        } else if (!mCurrentAutoRefreshStatus) {
            cancelRefreshTimer();
        }
    }

    void engageOverlay() {
        mHasOverlay = true;
        pauseRefresh();
    }

    void dismissOverlay() {
        mHasOverlay = false;
        resumeRefresh();
    }

    public String getDspCreativeId() {
        if (mAdUnitId != null && mAdResponse != null) {
            return mAdResponse.getDspCreativeId();
        }
        return "";
    }

    public boolean getAllowCustomClose() {
        if (mAdResponse == null) {
            return false;
        }
        return mAdResponse.allowCustomClose();
    }

    @Nullable
    public String getFullAdType() {
        if (mAdResponse == null) {
            return null;
        }
        return mAdResponse.getFullAdType();
    }

    public boolean getTesting() {
        return mIsTesting;
    }

    public void setTesting(boolean enabled) {
        mIsTesting = enabled;
    }

    boolean isDestroyed() {
        return mIsDestroyed;
    }

    /*
     * Clean up the internal state of the AdViewController.
     */
    void cleanup() {
        if (mIsDestroyed) {
            return;
        }

        setNotLoading();

        setAutoRefreshStatus(false);
        cancelRefreshTimer();

        // WebView subclasses are not garbage-collected in a timely fashion on Froyo and below,
        // thanks to some persistent references in WebViewCore. We manually release some resources
        // to compensate for this "leak".
        invalidateAdapter();
        mMoPubAd = null;
        mContext = null;
        mUrlGenerator = null;
        mLastTrackedRequestId = "";

        // Flag as destroyed. LoadUrlTask checks this before proceeding in its onPostExecute().
        mIsDestroyed = true;
    }

    @NonNull
    Integer getAdTimeoutDelay(final AdFormat adFormat) {
        int defaultValue;
        if (adFormat == AdFormat.BANNER) {
            defaultValue = DEFAULT_INLINE_TIMEOUT_DELAY;
        } else {
            defaultValue = DEFAULT_FULLSCREEN_TIMEOUT_DELAY;
        }

        if (mAdResponse == null) {
            return defaultValue;
        }
        return mAdResponse.getAdTimeoutMillis(defaultValue);
    }

    void registerClick() {
        if (mAdResponse != null) {
            // Click tracker fired from Banners and Interstitials
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getClickTrackingUrl(),
                    mContext);
        }
    }

    void fetchAd(@NonNull String url, @Nullable final MoPubError moPubError) {
        MoPubAd moPubAd = getMoPubAd();
        if (moPubAd == null || mContext == null) {
            MoPubLog.log(CUSTOM, "Can't load an ad in this ad view because it was destroyed.");
            setNotLoading();
            return;
        }

        synchronized (this) {
            if (mAdLoader == null || !mAdLoader.hasMoreAds()) {
                mAdLoader = new AdLoader(url, moPubAd.getAdFormat(), mAdUnitId, mContext, mAdListener);
            }
        }
        mActiveRequest = mAdLoader.loadNextAd(moPubError);
    }

    void forceRefresh() {
        invalidateAdapter();
        setNotLoading();
        loadAd();
    }

    protected void invalidateAdapter() {
        final AdAdapter adAdapter = getAdAdapter();
        if (adAdapter != null) {
            adAdapter.invalidate();
            mAdAdapter = null;
        }
    }

    @Nullable
    String generateAdUrl() {
        if (mUrlGenerator == null) {
            return null;
        }

        final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();

        mUrlGenerator
                .withAdUnitId(mAdUnitId)
                .withKeywords(mKeywords)
                .withUserDataKeywords(canCollectPersonalInformation ? mUserDataKeywords : null)
                .withRequestedAdSize(mRequestedAdSize)
                .withWindowInsets(mWindowInsets);

        return mUrlGenerator.generateUrlString(Constants.HOST);
    }

    void adDidFail(MoPubErrorCode errorCode) {
        MoPubLog.log(CUSTOM, "Ad failed to load.");
        setNotLoading();

        MoPubAd moPubAd = getMoPubAd();
        if (moPubAd == null) {
            return;
        }

        if (!TextUtils.isEmpty(mAdUnitId)) {
            scheduleRefreshTimerIfEnabled();
        }

        moPubAd.onAdLoadFailed(errorCode);
    }

    void scheduleRefreshTimerIfEnabled() {
        cancelRefreshTimer();
        if (mCurrentAutoRefreshStatus && mRefreshTimeMillis != null && mRefreshTimeMillis > 0) {

            mHandler.postDelayed(mRefreshRunnable,
                    Math.min(MAX_REFRESH_TIME_MILLISECONDS,
                            mRefreshTimeMillis * (long) Math.pow(BACKOFF_FACTOR, mBackoffPower)));
        }
    }

    void setLocalExtras(Map<String, Object> localExtras) {
        mLocalExtras = (localExtras != null)
                ? new TreeMap<>(localExtras)
                : new TreeMap<String, Object>();
    }

    /**
     * Returns a copied map of localExtras
     */
    Map<String, Object> getLocalExtras() {
        return (mLocalExtras != null)
                ? new TreeMap<>(mLocalExtras)
                : new TreeMap<String, Object>();
    }

    private void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    @SuppressLint("MissingPermission")
    private boolean isNetworkAvailable() {
        if (mContext == null) {
            return false;
        }
        // If we don't have network state access, just assume the network is up.
        if (!DeviceUtils.isPermissionGranted(mContext, ACCESS_NETWORK_STATE)) {
            return true;
        }

        // Otherwise, perform the connectivity check.
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (cm != null) {
            networkInfo = cm.getActiveNetworkInfo();
        }
        return networkInfo != null && networkInfo.isConnected();
    }

    void setAdContentView(final View view) {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd instanceof MoPubView) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ((MoPubView) moPubAd).removeAllViews();
                    ((MoPubView) moPubAd).addView(view, getAdLayoutParams(view));
                }
            });
        }
    }

    private FrameLayout.LayoutParams getAdLayoutParams(View view) {
        Integer width = null;
        Integer height = null;
        if (mAdResponse != null) {
            width = mAdResponse.getWidth();
            height = mAdResponse.getHeight();
        }

        if (width != null && height != null && getShouldHonorServerDimensions(view) && width > 0 && height > 0) {
            int scaledWidth = Dips.asIntPixels(width, mContext);
            int scaledHeight = Dips.asIntPixels(height, mContext);

            return new FrameLayout.LayoutParams(scaledWidth, scaledHeight, Gravity.CENTER);
        } else {
            return WRAP_AND_CENTER_LAYOUT_PARAMS;
        }
    }

    protected void loadBaseAd() {
        final String baseAdClassName = mAdResponse.getBaseAdClassName();
        final Map<String, String> serverExtras = mAdResponse.getServerExtras();
        final String adType = mAdResponse.getAdType();
        final String fullAdType = mAdResponse.getFullAdType();
        final String impressionMinVisibleDipsString = mAdResponse.getImpressionMinVisibleDips();
        final String impressionMinVisibleMsString = mAdResponse.getImpressionMinVisibleMs();
        final boolean allowCustomClose = mAdResponse.allowCustomClose();

        Preconditions.checkNotNull(serverExtras);

        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd == null) {
            MoPubLog.log(CUSTOM, "Can't load an ad in this ad view because it was destroyed.");
            loadFailUrl(INTERNAL_ERROR);
            return;
        }

        if (TextUtils.isEmpty(baseAdClassName)) {
            MoPubLog.log(CUSTOM, "Couldn't invoke base ad because the server did not specify one.");
            loadFailUrl(ADAPTER_NOT_FOUND);
            return;
        }

        invalidateAdapter();

        MoPubLog.log(CUSTOM, "Loading ad adapter.");

        final Map<String, String> extras = new TreeMap<>(serverExtras);
        for (final String key : mLocalExtras.keySet()) {
            final Object value = mLocalExtras.get(key);
            if (value != null && !extras.containsKey(key)) {
                extras.put(key, value.toString());
            }
        }

        String adapterClassName;
        if (moPubAd.getAdFormat() == AdFormat.BANNER) {
            adapterClassName = INLINE_AD_ADAPTER;
        } else {
            adapterClassName = FULLSCREEN_AD_ADAPTER;
        }

        final String adPayload = serverExtras.remove(DataKeys.HTML_RESPONSE_BODY_KEY);

        final AdData adData = new AdData.Builder()
                .extras(extras)
                .broadcastIdentifier(getBroadcastIdentifier())
                .timeoutDelayMillis(getAdTimeoutDelay(moPubAd.getAdFormat()))
                .impressionMinVisibleDips(impressionMinVisibleDipsString)
                .impressionMinVisibleMs(impressionMinVisibleMsString)
                .dspCreativeId(getDspCreativeId())
                .adPayload(adPayload != null ? adPayload : "")
                .adWidth(getAdWidth())
                .adHeight(getAdHeight())
                .adType(adType)
                .fullAdType(fullAdType)
                .allowCustomClose(allowCustomClose)
                .build();

        if (Reflection.classFound(adapterClassName)) {
            try {
                Class<? extends AdAdapter> adAdapterClass = Class.forName(adapterClassName)
                        .asSubclass(AdAdapter.class);
                Constructor<?> adAdapterConstructor = adAdapterClass.getDeclaredConstructor(
                        new Class[]{
                                Context.class,
                                String.class,
                                AdData.class
                        }
                );
                adAdapterConstructor.setAccessible(true);
                mAdAdapter = (AdAdapter) adAdapterConstructor.newInstance(
                        mContext,
                        baseAdClassName,
                        adData
                );
                mAdAdapter.load(this);
            } catch (Exception e) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Error loading ad adapter", e);
                loadFailUrl(ADAPTER_NOT_FOUND);
            }
        } else {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Could not load adapter",
                    ADAPTER_NOT_FOUND,
                    ADAPTER_NOT_FOUND.getIntCode());
            loadFailUrl(ADAPTER_NOT_FOUND);
        }
    }

    void show() {
        final AdAdapter adAdapter = getAdAdapter();
        if (adAdapter != null) {
            adAdapter.setInteractionListener(this);
            adAdapter.show(getMoPubAd());
        }
    }

    @Deprecated
    @VisibleForTesting
    Integer getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }

    @Deprecated
    @VisibleForTesting
    void setRefreshTimeMillis(@Nullable final Integer refreshTimeMillis) {
        mRefreshTimeMillis = refreshTimeMillis;
    }

    @Override
    public void onAdLoaded() {
        scheduleRefreshTimerIfEnabled();

        if (mAdLoader != null) {
            mAdLoader.creativeDownloadSuccess();
            mAdLoader = null;
        } else {
            MoPubLog.log(CUSTOM, "mAdLoader is not supposed to be null");
        }

        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdLoaded();
        }
    }

    @Override
    public void onAdLoadFailed(@NotNull MoPubErrorCode errorCode) {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdLoadFailed(errorCode);
        }
    }

    @Override
    public void onAdFailed(@NotNull MoPubErrorCode errorCode) {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdLoadFailed(errorCode);
        }
    }

    @Override
    public void onAdShown() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdShown();
        }
    }

    @Override
    public void onAdClicked() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdClicked();
        }
    }

    @Override
    public void onAdImpression() {
        if (mAdResponse != null) {
            final String requestId = mAdResponse.getRequestId();
            // If we have already tracked these impressions, don't do it again
            if (mLastTrackedRequestId.equals(requestId)) {
                MoPubLog.log(CUSTOM, "Ignoring duplicate impression.");
                return;
            }

            if (requestId != null) {
                mLastTrackedRequestId = requestId;
            }
            TrackingRequest.makeTrackingHttpRequest(mAdResponse.getImpressionTrackingUrls(), mContext);

            new SingleImpression(mAdResponse.getAdUnitId(), mAdResponse.getImpressionData()).sendImpression();
        }
    }

    @Override
    public void onAdDismissed() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdDismissed();
        }
    }

    @Override
    public void onAdComplete(@Nullable final MoPubReward moPubReward) {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdComplete(moPubReward);
        }
    }

    @Override
    public void onAdResumeAutoRefresh() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdResumeAutoRefresh();
        }
    }

    @Override
    public void onAdPauseAutoRefresh() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdPauseAutoRefresh();
        }
    }

    @Override
    public void onAdExpanded() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdExpanded();
        }
    }

    @Override
    public void onAdCollapsed() {
        final MoPubAd moPubAd = getMoPubAd();
        if (moPubAd != null) {
            moPubAd.onAdCollapsed();
        }
    }

    @VisibleForTesting
    public void setMoPubAd(@Nullable final MoPubAd moPubAd) {
        mMoPubAd = moPubAd;
    }

    @VisibleForTesting
    public void setAdResponse(@Nullable final AdResponse adResponse) {
        mAdResponse = adResponse;
    }
}
