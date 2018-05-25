package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;
import com.mopub.nativeads.MoPubCustomEventNative.MoPubStaticNativeAd;
import com.mopub.network.AdResponse;
import com.mopub.network.TrackingRequest;

import java.util.HashSet;
import java.util.Set;

import static com.mopub.nativeads.BaseNativeAd.NativeEventListener;

/**
 * This class represents a native ad instance returned from the MoPub Ad Server, MoPub Exchange, or
 * a mediated native ad network. This class can be used to create and render a {@link View} that
 * displays a native ad, tracking impressions and clicks for it.
 *
 * Using {@link MoPubStreamAdPlacer}, you can automatically have {@link NativeAd}s rendered into
 * {@link View}s and inserted into your app's content stream without manipulating this class
 * directly.
 *
 * In general you should get instances of {@link NativeAd} from {@link MoPubNative} instances in its
 * {@link MoPubNative.MoPubNativeNetworkListener#onAdLoad(AdResponse)} callback
 * and should not construct them directly.
 *
 * When you have a {@link NativeAd} instance and wish to show a view you should:
 *
 * 1. Call {@link #createAdView(Context, ViewGroup)} to inflate a {@link View} that can show this ad.
 * 2. Just before the ad is shown to the user, call {@link #prepare(View)}.
 * 3. Call {@link #renderAdView(View)} with a compatible {@link View} to render the ad data into the view.
 * 4. When the ad view is no longer shown to the user, call {@link #clear(View)}. You can later
 *    call {@link #prepare(View)} again if the ad will be shown to users.
 * 5. When the ad will never be shown again, call {@link #destroy()}.
 */
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

    /**
     * Returns the {@link BaseNativeAd} object for this {@link NativeAd}. This object is created by
     * a {@link CustomEventNative} subclass after a successful ad request and is specific to the ad
     * source. If it comes from the MoPub Exchange or is a directly-served ad it will be of the type
     * {@link MoPubStaticNativeAd} or MoPubVideoNativeAd. If it is returned by a mediated ad
     * network it may have another type.
     */
    @NonNull
    public BaseNativeAd getBaseNativeAd() {
        return mBaseNativeAd;
    }

    /**
     * Renders the ad view. It is highly recommended that this context is an Activity to preserve
     * the current activity's theme.
     *
     * @param context The context, preferably the Activity.
     * @param parent  An optional parent.
     * @return The rendered ad view.
     */
    @NonNull
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {
        return mMoPubAdRenderer.createAdView(context, parent);
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

    /**
     * Prepares the {@link NativeAd} to be seen on screen. You should call this method after calling
     * {@link #renderAdView(View)} with the same {@link View} and before the ad is shown on-screen.
     * This method is commonly used to initialize impression tracking and other state associated
     * with the {@link View}.
     */
    public void prepare(@NonNull final View view) {
        if (mIsDestroyed) {
            return;
        }

        mBaseNativeAd.prepare(view);
    }

    /**
     * Clears {@link NativeAd} state associated with this {@link View}. Call this when the {@link NativeAd} is no
     * longer seen by a user. If you would like to render a different {@link NativeAd} into the same View,
     * you must call this method first.
     */
    public void clear(@NonNull final View view) {
        if (mIsDestroyed) {
            return;
        }

        mBaseNativeAd.clear(view);
    }

    /**
     * Cleans up all {@link NativeAd} state. Call this method when the {@link NativeAd} will never be shown to a
     * user again.
     */
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
