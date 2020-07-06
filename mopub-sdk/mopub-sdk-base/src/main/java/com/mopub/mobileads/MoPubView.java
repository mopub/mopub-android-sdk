// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ManifestUtils;
import com.mopub.common.util.Visibility;
import com.mopub.mobileads.base.R;
import com.mopub.mobileads.factories.AdViewControllerFactory;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;
import static java.lang.Math.ceil;

public class MoPubView extends FrameLayout implements MoPubAd {

    public interface BannerAdListener {
        public void onBannerLoaded(@NonNull MoPubView banner);
        public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode);
        public void onBannerClicked(MoPubView banner);
        public void onBannerExpanded(MoPubView banner);
        public void onBannerCollapsed(MoPubView banner);
    }

    /**
     * MoPubAdSizeInt
     *
     * Integer values that represent the possible predefined ad heights in dp.
     */
    interface MoPubAdSizeInt {
        int MATCH_VIEW_INT = -1;
        int HEIGHT_50_INT = 50;
        int HEIGHT_90_INT = 90;
        int HEIGHT_250_INT = 250;
        int HEIGHT_280_INT = 280;
    }

    /**
     * MoPubAdSize
     *
     * These predefined constants are used to specify the desired height for an ad.
     */
    public enum MoPubAdSize implements MoPubAdSizeInt {

        MATCH_VIEW(MATCH_VIEW_INT),
        HEIGHT_50(HEIGHT_50_INT),
        HEIGHT_90(HEIGHT_90_INT),
        HEIGHT_250(HEIGHT_250_INT),
        HEIGHT_280(HEIGHT_280_INT);

        final private int mSizeInt;

        MoPubAdSize(final int sizeInt) {
            this.mSizeInt = sizeInt;
        }

        /**
         * This valueOf overload is used to get the associated the MoPubAdSize enum from an int (likely
         * from XML layout).
         *
         * @param adSizeInt The int value for which the MoPubAdSize is needed.
         * @return The MoPubAdSize associated with the level. Will return CUSTOM by default.
         */
        @NonNull
        public static MoPubAdSize valueOf(final int adSizeInt) {
            switch (adSizeInt) {
                case HEIGHT_50_INT:
                    return HEIGHT_50;
                case HEIGHT_90_INT:
                    return HEIGHT_90;
                case HEIGHT_250_INT:
                    return HEIGHT_250;
                case HEIGHT_280_INT:
                    return HEIGHT_280;
                case MATCH_VIEW_INT:
                default:
                    return MATCH_VIEW;
            }
        }

        public int toInt() {
            return mSizeInt;
        }
    }

    @Nullable
    protected AdViewController mAdViewController;

    private Context mContext;
    private int mScreenVisibility;
    private BroadcastReceiver mScreenStateReceiver;
    private MoPubView.MoPubAdSize mMoPubAdSize;
    private BannerAdListener mBannerAdListener;

    public MoPubView(Context context) {
        this(context, null);
    }

    public MoPubView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mMoPubAdSize = getMoPubAdSizeFromAttributeSet(context, attrs,
                MoPubAdSize.MATCH_VIEW);

        ManifestUtils.checkWebViewActivitiesDeclared(context);

        mContext = context;
        mScreenVisibility = getVisibility();

        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        setAdViewController(AdViewControllerFactory.create(context, this));
        registerScreenStateBroadcastReceiver();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setWindowInsets(getRootWindowInsets());
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setWindowInsets(insets);
        }
        return super.onApplyWindowInsets(insets);
    }

    private void registerScreenStateBroadcastReceiver() {
        mScreenStateReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                if (!Visibility.isScreenVisible(mScreenVisibility) || intent == null) {
                    return;
                }

                final String action = intent.getAction();

                if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    setAdVisibility(View.VISIBLE);
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    setAdVisibility(View.GONE);
                }
            }
        };

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenStateReceiver, filter);
    }

    private void unregisterScreenStateBroadcastReceiver() {
        try {
            mContext.unregisterReceiver(mScreenStateReceiver);
        } catch (Exception IllegalArgumentException) {
            MoPubLog.log(CUSTOM, "Failed to unregister screen state broadcast receiver (never registered).");
        }
    }

    public void loadAd(final MoPubAdSize moPubAdSize) {
        setAdSize(moPubAdSize);
        loadAd();
    }

    /*
     * Tears down the ad view: no ads will be shown once this method executes. The parent
     * Activity's onDestroy implementation must include a call to this method.
     */
    public void destroy() {
        MoPubLog.log(CUSTOM, "Destroy() called");
        unregisterScreenStateBroadcastReceiver();
        removeAllViews();

        if (mAdViewController != null) {
            mAdViewController.cleanup();
            mAdViewController = null;
        }
    }

    @Override
    protected void onWindowVisibilityChanged(final int visibility) {
        // Ignore transitions between View.GONE and View.INVISIBLE
        if (Visibility.hasScreenVisibilityChanged(mScreenVisibility, visibility)) {
            mScreenVisibility = visibility;
            setAdVisibility(mScreenVisibility);
        }
    }

    private void setAdVisibility(final int visibility) {
        if (mAdViewController == null) {
            return;
        }

        if (Visibility.isScreenVisible(visibility)) {
            mAdViewController.resumeRefresh();
        } else {
            mAdViewController.pauseRefresh();
        }
    }


    private MoPubAdSize getMoPubAdSizeFromAttributeSet(
            final Context context,
            final AttributeSet attrs,
            MoPubAdSize defaultMoPubAdSize) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MoPubView,
                0, 0);

        MoPubAdSize returnValue = defaultMoPubAdSize;

        try {
            final int moPubAdSizeInt = a.getInteger(R.styleable.MoPubView_moPubAdSize,
                    defaultMoPubAdSize.toInt());
            returnValue = MoPubAdSize.valueOf(moPubAdSizeInt);
        } catch(Resources.NotFoundException rnfe) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Encountered a problem while setting the MoPubAdSize",
                    rnfe);
        } finally {
            a.recycle();
        }

        return returnValue;
    }

    @Override
    @NonNull
    public Point resolveAdSize() {
        final Point resolvedAdSize = new Point(getWidth(), getHeight());
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();

        // If WRAP_CONTENT or MATCH_PARENT
        if (getParent() != null && layoutParams != null && layoutParams.width < 0) {
            resolvedAdSize.x = ((View) getParent()).getWidth();
        }

        // MoPubAdSize only applies to height
        if (mMoPubAdSize != MoPubAdSize.MATCH_VIEW) {
            final float density = mContext.getResources().getDisplayMetrics().density;
            resolvedAdSize.y = (int) ( ceil(mMoPubAdSize.toInt() * density) );
        } else if (getParent() != null && layoutParams != null && layoutParams.height < 0) {
            resolvedAdSize.y = ((View) getParent()).getHeight();
        }

        return resolvedAdSize;
    }

    @Override
    @NonNull
    public AdFormat getAdFormat() {
        return AdFormat.BANNER;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @deprecated As of 5.12.0. The location is set automatically based on GPS or Network provider value
     * @param location is ignored
     */
    @Deprecated
    public void setLocation(Location location) {
    }

    public Activity getActivity() {
        return (Activity) mContext;
    }

    public void setBannerAdListener(BannerAdListener listener) {
        mBannerAdListener = listener;
    }

    public BannerAdListener getBannerAdListener() {
        return mBannerAdListener;
    }

    public void setAutorefreshEnabled(boolean enabled) {
        if (mAdViewController != null) {
            mAdViewController.setShouldAllowAutoRefresh(enabled);
        }
    }

    public boolean getAutorefreshEnabled() {
        if (mAdViewController != null) return mAdViewController.getCurrentAutoRefreshStatus();
        else {
            MoPubLog.log(CUSTOM, "Can't get autorefresh status for destroyed MoPubView. " +
                    "Returning false.");
            return false;
        }
    }

    public void setTesting(boolean testing) {
        if (mAdViewController != null) mAdViewController.setTesting(testing);
    }

    public boolean getTesting() {
        if (mAdViewController != null) return mAdViewController.getTesting();
        else {
            MoPubLog.log(CUSTOM, "Can't get testing status for destroyed MoPubView. " +
                    "Returning false.");
            return false;
        }
    }

    public void forceRefresh() {
        if (mAdViewController != null) {
            mAdViewController.forceRefresh();
        }
    }

    public void setAdSize(final MoPubAdSize moPubAdSize) {
        mMoPubAdSize = moPubAdSize;
    }

    public MoPubAdSize getAdSize() {
        return mMoPubAdSize;
    }

    void setWindowInsets(@NonNull final WindowInsets windowInsets) {
        if (mAdViewController != null) {
            mAdViewController.setWindowInsets(windowInsets);
        }
    }

    @Override
    public AdViewController getAdViewController() {
        return mAdViewController;
    }

    @Override
    public void setAdViewController(@Nullable AdViewController adViewController) {
        mAdViewController = adViewController;
    }

    /**
     * @deprecated As of release 4.4.0
     */
    @Deprecated
    public void setTimeout(int milliseconds) {
    }

    @Deprecated
    public String getResponseString() {
        return null;
    }

    @Deprecated
    public String getClickTrackingUrl() {
        return null;
    }

    @Override
    public void onAdLoaded() {
        if (mAdViewController != null) {
            mAdViewController.show(); // inline ads immediately show themselves
        }

        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerLoaded(MoPubView.this);
        }
    }

    @Override
    public void onAdLoadFailed(@NonNull MoPubErrorCode errorCode) {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerFailed(MoPubView.this, errorCode);
        }
    }

    @Override
    public void onAdFailed(@NonNull MoPubErrorCode errorCode) {
        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerFailed(MoPubView.this, errorCode);
        }
    }

    @Override
    public void onAdExpanded() {
        if (mAdViewController != null) {
            mAdViewController.engageOverlay();
        }

        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerExpanded(MoPubView.this);
        }
    }

    @Override
    public void onAdCollapsed() {
        if (mAdViewController != null) {
            mAdViewController.dismissOverlay();
        }

        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerCollapsed(MoPubView.this);
        }
    }

    @Override
    public void onAdClicked() {
        if (mAdViewController != null) {
            mAdViewController.registerClick();
        }

        if (mBannerAdListener != null) {
            mBannerAdListener.onBannerClicked(MoPubView.this);
        }
    }

    @Override
    public void onAdPauseAutoRefresh() {
        if (mAdViewController != null) {
            mAdViewController.engageOverlay();
        }
    }

    @Override
    public void onAdResumeAutoRefresh() {
        if (mAdViewController != null) {
            mAdViewController.dismissOverlay();
        }
    }

    @Override
    public void onAdShown() { /* no-op for inline */ }

    @Override
    public void onAdImpression() { /* no-op for inline */ }

    @Override
    public void onAdDismissed() { /* no-op for inline */ }

    @Override
    public void onAdComplete(@Nullable final MoPubReward moPubReward) { /* UNUSED */ }
}
