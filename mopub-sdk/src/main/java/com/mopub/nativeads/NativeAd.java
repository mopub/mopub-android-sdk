package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;
import com.mopub.network.TrackingRequest;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.nativeads.BaseNativeAd.NativeEventListener;

public class NativeAd {

    /**
     * Listen for MoPub specific click and impression events
     */
    public interface MoPubNativeEventListener {
        void onImpression(final View view);
        void onClick(final View view);
    }

    @NonNull private final Context mContext;
    @NonNull private final BaseNativeAd mBaseNativeAd;
    @NonNull private final MoPubAdRenderer mMoPubAdRenderer;
    @NonNull private final Set<String> mImpressionTrackers;
    @NonNull private final Set<String> mClickTrackers;
    @NonNull private final String mAdUnitId;
    @Nullable private MoPubNativeEventListener mMoPubNativeEventListener;

    private boolean mRecordedImpression;
    private boolean mIsClicked;
    private boolean mIsDestroyed;

    public NativeAd(@NonNull final Context context,
            @NonNull final String moPubImpressionTrackerUrl,
            @NonNull final String moPubClickTrackerUrl,
            @NonNull final String adUnitId,
            @NonNull final BaseNativeAd baseNativeAd,
            @NonNull final MoPubAdRenderer moPubAdRenderer) {
        mContext = context.getApplicationContext();
        mAdUnitId = adUnitId;

        mImpressionTrackers = new HashSet<String>();
        mImpressionTrackers.add(moPubImpressionTrackerUrl);
        mImpressionTrackers.addAll(baseNativeAd.getImpressionTrackers());

        mClickTrackers = new HashSet<String>();
        mClickTrackers.add(moPubClickTrackerUrl);
        mClickTrackers.addAll(baseNativeAd.getClickTrackers());

        mBaseNativeAd = baseNativeAd;
        mBaseNativeAd.setNativeEventListener(new NativeEventListener() {
            @Override
            public void onAdImpressed() {
                recordImpression(null);
            }

            @Override
            public void onAdClicked() {
                handleClick(null);
            }
        });

        mMoPubAdRenderer = moPubAdRenderer;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("\n");
        stringBuilder.append("impressionTrackers").append(":").append(mImpressionTrackers).append("\n");
        stringBuilder.append("clickTrackers").append(":").append(mClickTrackers).append("\n");
        stringBuilder.append("recordedImpression").append(":").append(mRecordedImpression).append("\n");
        stringBuilder.append("isClicked").append(":").append(mIsClicked).append("\n");
        stringBuilder.append("isDestroyed").append(":").append(mIsDestroyed).append("\n");
        return stringBuilder.toString();
    }

    public void setMoPubNativeEventListener(@Nullable final MoPubNativeEventListener moPubNativeEventListener) {
        mMoPubNativeEventListener = moPubNativeEventListener;
    }

    @NonNull
    public String getAdUnitId() {
        return mAdUnitId;
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    @NonNull
    public View createAdView(ViewGroup parent) {
        return mMoPubAdRenderer.createAdView(mContext, parent);
    }

    public void renderAdView(View view) {
        //noinspection unchecked
        mMoPubAdRenderer.renderAdView(view, mBaseNativeAd);
    }

    @NonNull
    public MoPubAdRenderer getMoPubAdRenderer() {
        return mMoPubAdRenderer;
    }

    // Lifecycle Handlers
    public void prepare(@NonNull final View view) {
        if (mIsDestroyed) {
            return;
        }

        mBaseNativeAd.prepare(view);
    }

    public void clear(@NonNull final View view) {
        if (mIsDestroyed) {
            return;
        }

        mBaseNativeAd.clear(view);
    }

    public void destroy() {
        if (mIsDestroyed) {
            return;
        }

        mBaseNativeAd.destroy();
        mIsDestroyed = true;
    }

    // Event Handlers
    @VisibleForTesting
    void recordImpression(@Nullable final View view) {
        if (mRecordedImpression || mIsDestroyed) {
            return;
        }

        TrackingRequest.makeTrackingHttpRequest(mImpressionTrackers, mContext);
        if (mMoPubNativeEventListener != null) {
            mMoPubNativeEventListener.onImpression(view);
        }

        mRecordedImpression = true;
    }

    @VisibleForTesting
    void handleClick(@Nullable final View view) {
        if (mIsClicked || mIsDestroyed) {
            return;
        }

        TrackingRequest.makeTrackingHttpRequest(mClickTrackers, mContext);
        if (mMoPubNativeEventListener != null) {
            mMoPubNativeEventListener.onClick(view);
        }

        mIsClicked = true;
    }
}
