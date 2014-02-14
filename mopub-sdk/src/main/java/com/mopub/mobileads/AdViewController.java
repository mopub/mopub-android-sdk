/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import com.mopub.mobileads.MoPubView.LocationAwareness;
import com.mopub.mobileads.factories.AdFetcherFactory;
import com.mopub.mobileads.factories.HttpClientFactory;
import com.mopub.mobileads.util.Dips;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.math.BigDecimal;
import java.util.*;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static com.mopub.mobileads.MoPubView.DEFAULT_LOCATION_PRECISION;

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
    private MoPubView mMoPubView;
    private final AdUrlGenerator mUrlGenerator;
    private AdFetcher mAdFetcher;
    private AdConfiguration mAdConfiguration;
    private final Runnable mRefreshRunnable;

    private boolean mIsDestroyed;
    private Handler mHandler;
    private boolean mIsLoading;
    private String mUrl;

    private Map<String, Object> mLocalExtras = new HashMap<String, Object>();
    private boolean mAutoRefreshEnabled = true;
    private String mKeywords;
    private Location mLocation;
    private LocationAwareness mLocationAwareness = LocationAwareness.LOCATION_AWARENESS_NORMAL;
    private int mLocationPrecision = DEFAULT_LOCATION_PRECISION;
    private boolean mIsFacebookSupported = true;
    private boolean mIsTesting;

    protected static void setShouldHonorServerDimensions(View view) {
        sViewShouldHonorServerDimensions.put(view, true);
    }

    private static boolean getShouldHonorServerDimensions(View view) {
        return sViewShouldHonorServerDimensions.get(view) != null;
    }

    public AdViewController(Context context, MoPubView view) {
        mContext = context;
        mMoPubView = view;

        mUrlGenerator = new AdUrlGenerator(context);
        mAdConfiguration = new AdConfiguration(mContext);

        mAdFetcher = AdFetcherFactory.create(this, mAdConfiguration.getUserAgent());

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
        if (mAdConfiguration.getAdUnitId() == null) {
            Log.d("MoPub", "Can't load an ad in this ad view because the ad unit ID is null. " +
                    "Did you forget to call setAdUnitId()?");
            return;
        }

        if (!isNetworkAvailable()) {
            Log.d("MoPub", "Can't load an ad because there is no network connectivity.");
            scheduleRefreshTimerIfEnabled();
            return;
        }

        if (mLocation == null) {
            mLocation = getLastKnownLocation();
        }

        String adUrl = generateAdUrl();
        loadNonJavascript(adUrl);
    }

    void loadNonJavascript(String url) {
        if (url == null) return;

        Log.d("MoPub", "Loading url: " + url);
        if (mIsLoading) {
            if (mAdConfiguration.getAdUnitId() != null) {
                Log.i("MoPub", "Already loading an ad for " + mAdConfiguration.getAdUnitId() + ", wait to finish.");
            }
            return;
        }

        mUrl = url;
        mAdConfiguration.setFailUrl(null);
        mIsLoading = true;

        fetchAd(mUrl);
    }

    public void reload() {
        Log.d("MoPub", "Reload ad: " + mUrl);
        loadNonJavascript(mUrl);
    }

    void loadFailUrl(MoPubErrorCode errorCode) {
        mIsLoading = false;

        Log.v("MoPub", "MoPubErrorCode: " + (errorCode == null ? "" : errorCode.toString()));

        if (mAdConfiguration.getFailUrl() != null) {
            Log.d("MoPub", "Loading failover url: " + mAdConfiguration.getFailUrl());
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

    public boolean isFacebookSupported() {
        return mIsFacebookSupported;
    }

    public void setFacebookSupported(boolean enabled) {
        mIsFacebookSupported = enabled;
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

    @Deprecated
    public void setClickthroughUrl(String clickthroughUrl) {
        mAdConfiguration.setClickthroughUrl(clickthroughUrl);
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

    public void setAutorefreshEnabled(boolean enabled) {
        mAutoRefreshEnabled = enabled;

        if (mAdConfiguration.getAdUnitId() != null) {
            Log.d("MoPub", "Automatic refresh for " + mAdConfiguration + " set to: " + enabled + ".");

        }

        if (mAutoRefreshEnabled) {
            scheduleRefreshTimerIfEnabled();
        } else {
            cancelRefreshTimer();
        }
    }

    public boolean getTesting() {
        return mIsTesting;
    }

    public void setTesting(boolean enabled) {
        mIsTesting = enabled;
    }

    int getLocationPrecision() {
        return mLocationPrecision;
    }

    void setLocationPrecision(int precision) {
        mLocationPrecision = Math.max(0, precision);
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
                    Log.d("MoPub", "Impression tracking failed : " + mAdConfiguration.getImpressionUrl(), e);
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
                    Log.d("MoPub", "Tracking click for: " + mAdConfiguration.getClickthroughUrl());
                    HttpGet httpget = new HttpGet(mAdConfiguration.getClickthroughUrl());
                    httpget.addHeader("User-Agent", mAdConfiguration.getUserAgent());
                    httpClient.execute(httpget);
                } catch (Exception e) {
                    Log.d("MoPub", "Click tracking failed: " + mAdConfiguration.getClickthroughUrl(), e);
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
                .withFacebookSupported(mIsFacebookSupported)
                .withLocation(mLocation)
                .generateUrlString(getServerHostname());
    }

    void adDidFail(MoPubErrorCode errorCode) {
        Log.i("MoPub", "Ad failed to load.");
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

    /*
     * Returns the last known location of the device using its GPS and network location providers.
     * May be null if:
     * - Location permissions are not requested in the Android manifest file
     * - The location providers don't exist
     * - Location awareness is disabled in the parent MoPubView
     */
    private Location getLastKnownLocation() {
        Location result;

        if (mLocationAwareness == LocationAwareness.LOCATION_AWARENESS_DISABLED) {
            return null;
        }

        LocationManager lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Location gpsLocation = null;
        try {
            gpsLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            Log.d("MoPub", "Failed to retrieve GPS location: access appears to be disabled.");
        } catch (IllegalArgumentException e) {
            Log.d("MoPub", "Failed to retrieve GPS location: device has no GPS provider.");
        }

        Location networkLocation = null;
        try {
            networkLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            Log.d("MoPub", "Failed to retrieve network location: access appears to be disabled.");
        } catch (IllegalArgumentException e) {
            Log.d("MoPub", "Failed to retrieve network location: device has no network provider.");
        }

        if (gpsLocation == null && networkLocation == null) {
            return null;
        }
        else if (gpsLocation != null && networkLocation != null) {
            if (gpsLocation.getTime() > networkLocation.getTime()) result = gpsLocation;
            else result = networkLocation;
        }
        else if (gpsLocation != null) result = gpsLocation;
        else result = networkLocation;

        // Truncate latitude/longitude to the number of digits specified by locationPrecision.
        if (mLocationAwareness == LocationAwareness.LOCATION_AWARENESS_TRUNCATED) {
            double lat = result.getLatitude();
            double truncatedLat = BigDecimal.valueOf(lat)
                    .setScale(mLocationPrecision, BigDecimal.ROUND_HALF_DOWN)
                    .doubleValue();
            result.setLatitude(truncatedLat);

            double lon = result.getLongitude();
            double truncatedLon = BigDecimal.valueOf(lon)
                    .setScale(mLocationPrecision, BigDecimal.ROUND_HALF_DOWN)
                    .doubleValue();
            result.setLongitude(truncatedLon);
        }

        return result;
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
}
