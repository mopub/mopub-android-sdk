package com.mopub.mobileads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.factories.AdFetcherFactory;
import com.mopub.mobileads.factories.HttpClientFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.*;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static com.mopub.common.GpsHelper.GpsHelperListener;
import static com.mopub.common.GpsHelper.asyncFetchAdvertisingInfo;
import static com.mopub.common.GpsHelper.asyncFetchAdvertisingInfoIfNotCached;
import static com.mopub.common.LocationService.getLastKnownLocation;

public class AdViewController {
    static final int MINIMUM_REFRESH_TIME_MILLISECONDS = 10000;
    static final int DEFAULT_REFRESH_TIME_MILLISECONDS = 60000;
    private static final FrameLayout.LayoutParams WRAP_AND_CENTER_LAYOUT_PARAMS =
            new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
    private static WeakHashMap<View,Boolean> sViewShouldHonorServerDimensions = new WeakHashMap<View, Boolean>();;

    private final Context mContext;
    private GpsHelperListener mGpsHelperListener;
    private MoPubView mMoPubView;
    private final WebViewAdUrlGenerator mUrlGenerator;
    private AdFetcher mAdFetcher;
    private AdConfiguration mAdConfiguration;
    private final Runnable mRefreshRunnable;

    private boolean mIsDestroyed;
    private Handler mHandler;
    private boolean mIsLoading;
    private String mUrl;

    private Map<String, Object> mLocalExtras = new HashMap<String, Object>();
    private boolean mAutoRefreshEnabled = true;
    private boolean mPreviousAutoRefreshSetting = true;
    private String mKeywords;
    private Location mLocation;
    private boolean mIsTesting;
    private boolean mAdWasLoaded;

    protected static void setShouldHonorServerDimensions(View view) {
        sViewShouldHonorServerDimensions.put(view, true);
    }

    private static boolean getShouldHonorServerDimensions(View view) {
        return sViewShouldHonorServerDimensions.get(view) != null;
    }

    public AdViewController(Context context, MoPubView view) {
        mContext = context;
        mMoPubView = view;

        mUrlGenerator = new WebViewAdUrlGenerator(context);
        mAdConfiguration = new AdConfiguration(mContext);

        mAdFetcher = AdFetcherFactory.create(this, mAdConfiguration.getUserAgent());

        mGpsHelperListener = new AdViewControllerGpsHelperListener();

        asyncFetchAdvertisingInfo(mContext);

        mRefreshRunnable = new Runnable() {
            public void run() {
                loadAd();
            }
        };

        mHandler = new Handler();
    }

    public MoPubView getMoPubView() {
        return mMoPubView;
    }

    public void loadAd() {
        mAdWasLoaded = true;
        if (mAdConfiguration.getAdUnitId() == null) {
            MoPubLog.d("Can't load an ad in this ad view because the ad unit ID is null. " +
                    "Did you forget to call setAdUnitId()?");
            return;
        }

        if (!isNetworkAvailable()) {
            MoPubLog.d("Can't load an ad because there is no network connectivity.");
            scheduleRefreshTimerIfEnabled();
            return;
        }

        if (mLocation == null) {
            mLocation = getLastKnownLocation(mContext,
                    MoPub.getLocationPrecision(),
                    MoPub.getLocationAwareness());
        }

        // If we have access to Google Play Services (GPS) but the advertising info
        // is not cached then guarantee we get it before building the ad request url
        // in the callback, this is a requirement from Google
        asyncFetchAdvertisingInfoIfNotCached(mContext, mGpsHelperListener);
    }

    void loadNonJavascript(String url) {
        if (url == null) return;

        MoPubLog.d("Loading url: " + url);
        if (mIsLoading) {
            if (mAdConfiguration.getAdUnitId() != null) {
                MoPubLog.i("Already loading an ad for " + mAdConfiguration.getAdUnitId() + ", wait to finish.");
            }
            return;
        }

        mUrl = url;
        mAdConfiguration.setFailUrl(null);
        mIsLoading = true;

        fetchAd(mUrl);
    }

    public void reload() {
        MoPubLog.d("Reload ad: " + mUrl);
        loadNonJavascript(mUrl);
    }

    void loadFailUrl(MoPubErrorCode errorCode) {
        mIsLoading = false;

        Log.v("MoPub", "MoPubErrorCode: " + (errorCode == null ? "" : errorCode.toString()));

        if (mAdConfiguration.getFailUrl() != null) {
            MoPubLog.d("Loading failover url: " + mAdConfiguration.getFailUrl());
            loadNonJavascript(mAdConfiguration.getFailUrl());
        } else {
            // No other URLs to try, so signal a failure.
            adDidFail(MoPubErrorCode.NO_FILL);
        }
    }

    void setFailUrl(String failUrl) {
        mAdConfiguration.setFailUrl(failUrl);
    }

    void setNotLoading() {
        this.mIsLoading = false;
    }

    public String getKeywords() {
        return mKeywords;
    }

    public void setKeywords(String keywords) {
        mKeywords = keywords;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public String getAdUnitId() {
        return mAdConfiguration.getAdUnitId();
    }

    public void setAdUnitId(String adUnitId) {
        mAdConfiguration.setAdUnitId(adUnitId);
    }

    public void setTimeout(int milliseconds) {
        if (mAdFetcher != null) {
            mAdFetcher.setTimeout(milliseconds);
        }
    }

    public int getAdWidth() {
        return mAdConfiguration.getWidth();
    }

    public int getAdHeight() {
        return mAdConfiguration.getHeight();
    }

    public String getClickthroughUrl() {
        return mAdConfiguration.getClickthroughUrl();
    }

    public String getRedirectUrl() {
        return mAdConfiguration.getRedirectUrl();
    }

    public String getResponseString() {
        return mAdConfiguration.getResponseString();
    }

    public boolean getAutorefreshEnabled() {
        return mAutoRefreshEnabled;
    }

    void pauseRefresh() {
        mPreviousAutoRefreshSetting = mAutoRefreshEnabled;
        setAutorefreshEnabled(false);
    }

    void unpauseRefresh() {
        setAutorefreshEnabled(mPreviousAutoRefreshSetting);
    }

    void forceSetAutorefreshEnabled(boolean enabled) {
        mPreviousAutoRefreshSetting = enabled;
        setAutorefreshEnabled(enabled);
    }

    private void setAutorefreshEnabled(boolean enabled) {
        final boolean autorefreshChanged = mAdWasLoaded && (mAutoRefreshEnabled != enabled);
        if (autorefreshChanged) {
            final String enabledString = (enabled) ? "enabled" : "disabled";
            final String adUnitId = (mAdConfiguration != null) ? mAdConfiguration.getAdUnitId() : null;

            MoPubLog.d("Refresh " + enabledString + " for ad unit (" + adUnitId + ").");
        }

        mAutoRefreshEnabled = enabled;
        if (mAdWasLoaded && mAutoRefreshEnabled) {
            scheduleRefreshTimerIfEnabled();
        } else if (!mAutoRefreshEnabled) {
            cancelRefreshTimer();
        }
    }

    public boolean getTesting() {
        return mIsTesting;
    }

    public void setTesting(boolean enabled) {
        mIsTesting = enabled;
    }

    AdConfiguration getAdConfiguration() {
        return mAdConfiguration;
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

        setAutorefreshEnabled(false);
        cancelRefreshTimer();

        // WebView subclasses are not garbage-collected in a timely fashion on Froyo and below,
        // thanks to some persistent references in WebViewCore. We manually release some resources
        // to compensate for this "leak".

        mAdFetcher.cleanup();
        mAdFetcher = null;

        mAdConfiguration.cleanup();

        mMoPubView = null;

        // Flag as destroyed. LoadUrlTask checks this before proceeding in its onPostExecute().
        mIsDestroyed = true;
    }

    void configureUsingHttpResponse(final HttpResponse response) {
        mAdConfiguration.addHttpResponse(response);
    }

    Integer getAdTimeoutDelay() {
        return mAdConfiguration.getAdTimeoutDelay();
    }

    int getRefreshTimeMilliseconds() {
        return mAdConfiguration.getRefreshTimeMilliseconds();
    }

    @Deprecated
    void setRefreshTimeMilliseconds(int refreshTimeMilliseconds) {
        mAdConfiguration.setRefreshTimeMilliseconds(refreshTimeMilliseconds);
    }

    void trackImpression() {
        new Thread(new Runnable() {
            public void run () {
                if (mAdConfiguration.getImpressionUrl() == null) return;

                DefaultHttpClient httpClient = HttpClientFactory.create();
                try {
                    HttpGet httpget = new HttpGet(mAdConfiguration.getImpressionUrl());
                    httpget.addHeader("User-Agent", mAdConfiguration.getUserAgent());
                    httpClient.execute(httpget);
                } catch (Exception e) {
                    MoPubLog.d("Impression tracking failed : " + mAdConfiguration.getImpressionUrl(), e);
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }).start();
    }

    void registerClick() {
        new Thread(new Runnable() {
            public void run () {
                if (mAdConfiguration.getClickthroughUrl() == null) return;

                DefaultHttpClient httpClient = HttpClientFactory.create();
                try {
                    MoPubLog.d("Tracking click for: " + mAdConfiguration.getClickthroughUrl());
                    HttpGet httpget = new HttpGet(mAdConfiguration.getClickthroughUrl());
                    httpget.addHeader("User-Agent", mAdConfiguration.getUserAgent());
                    httpClient.execute(httpget);
                } catch (Exception e) {
                    MoPubLog.d("Click tracking failed: " + mAdConfiguration.getClickthroughUrl(), e);
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }
            }
        }).start();
    }

    void fetchAd(String mUrl) {
        if (mAdFetcher != null) {
            mAdFetcher.fetchAdForUrl(mUrl);
        }
    }

    void forceRefresh() {
        setNotLoading();
        loadAd();
    }

    String generateAdUrl() {
        return mUrlGenerator
                .withAdUnitId(mAdConfiguration.getAdUnitId())
                .withKeywords(mKeywords)
                .withLocation(mLocation)
                .generateUrlString(getServerHostname());
    }

    void adDidFail(MoPubErrorCode errorCode) {
        MoPubLog.i("Ad failed to load.");
        setNotLoading();
        scheduleRefreshTimerIfEnabled();
        getMoPubView().adFailed(errorCode);
    }

    void scheduleRefreshTimerIfEnabled() {
        cancelRefreshTimer();
        if (mAutoRefreshEnabled && mAdConfiguration.getRefreshTimeMilliseconds() > 0) {
            mHandler.postDelayed(mRefreshRunnable, mAdConfiguration.getRefreshTimeMilliseconds());
        }

    }

    void setLocalExtras(Map<String, Object> localExtras) {
        mLocalExtras = (localExtras != null)
                ? new HashMap<String,Object>(localExtras)
                : new HashMap<String,Object>();
    }

    Map<String, Object> getLocalExtras() {
        return (mLocalExtras != null)
                ? new HashMap<String,Object>(mLocalExtras)
                : new HashMap<String,Object>();
    }

    private void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    private String getServerHostname() {
        return mIsTesting ? MoPubView.HOST_FOR_TESTING : MoPubView.HOST;
    }

    private boolean isNetworkAvailable() {
        // If we don't have network state access, just assume the network is up.
        int result = mContext.checkCallingPermission(ACCESS_NETWORK_STATE);
        if (result == PackageManager.PERMISSION_DENIED) return true;

        // Otherwise, perform the connectivity check.
        ConnectivityManager cm
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    void setAdContentView(final View view) {
        // XXX: This method is called from the WebViewClient's callbacks, which has caused an error on a small portion of devices
        // We suspect that the code below may somehow be running on the wrong UI Thread in the rare case.
        // see: http://stackoverflow.com/questions/10426120/android-got-calledfromwrongthreadexception-in-onpostexecute-how-could-it-be
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MoPubView moPubView = getMoPubView();
                if (moPubView == null) {
                    return;
                }
                moPubView.removeAllViews();
                moPubView.addView(view, getAdLayoutParams(view));
            }
        });
    }

    private FrameLayout.LayoutParams getAdLayoutParams(View view) {
        int width = mAdConfiguration.getWidth();
        int height = mAdConfiguration.getHeight();

        if (getShouldHonorServerDimensions(view) && width > 0 && height > 0) {
            int scaledWidth = Dips.asIntPixels(width, mContext);
            int scaledHeight = Dips.asIntPixels(height, mContext);

            return new FrameLayout.LayoutParams(scaledWidth, scaledHeight, Gravity.CENTER);
        } else {
            return WRAP_AND_CENTER_LAYOUT_PARAMS;
        }
    }

    class AdViewControllerGpsHelperListener implements GpsHelperListener {
        @Override
        public void onFetchAdInfoCompleted() {
            String adUrl = generateAdUrl();
            loadNonJavascript(adUrl);
        }
    }

    @Deprecated
    void setGpsHelperListener(GpsHelperListener gpsHelperListener) {
        mGpsHelperListener = gpsHelperListener;
    }

    @Deprecated
    public void customEventDidLoadAd() {
        setNotLoading();
        trackImpression();
        scheduleRefreshTimerIfEnabled();
    }

    @Deprecated
    public void customEventDidFailToLoadAd() {
        loadFailUrl(MoPubErrorCode.UNSPECIFIED);
    }

    @Deprecated
    public void customEventActionWillBegin() {
        registerClick();
    }

    @Deprecated
    public void setClickthroughUrl(String clickthroughUrl) {
        mAdConfiguration.setClickthroughUrl(clickthroughUrl);
    }

    /**
     * @deprecated As of release 2.4
     */
    @Deprecated
    public boolean isFacebookSupported() {
        return false;
    }

    /**
     * @deprecated As of release 2.4
     */
    @Deprecated
    public void setFacebookSupported(boolean enabled) {}
}
