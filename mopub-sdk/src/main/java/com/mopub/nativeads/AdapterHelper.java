package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;

import static com.mopub.nativeads.MoPubNative.MoPubNativeListener;

/**
 * @deprecated As of release 2.4, use {@link com.mopub.nativeads.MoPubAdAdapter} or
 * {@link com.mopub.nativeads.MoPubStreamAdPlacer} instead
 */
@Deprecated
public final class AdapterHelper {
    @NonNull private final WeakReference<Activity> mActivity;
    @NonNull private final Context mApplicationContext;
    private final int mStart;
    private final int mInterval;

    @Deprecated
    public AdapterHelper(@NonNull final Context context, final int start, final int interval) {
        Preconditions.checkNotNull(context, "Context cannot be null.");
        Preconditions.checkArgument(context instanceof Activity, "Context must be an Activity.");
        Preconditions.checkArgument(start >= 0, "start position must be non-negative");
        Preconditions.checkArgument(interval >= 2, "interval must be at least 2");

        mActivity = new WeakReference<Activity>((Activity) context);
        mApplicationContext = context.getApplicationContext();
        mStart = start;
        mInterval = interval;
    }

    @Deprecated
    @NonNull
    public View getAdView(@Nullable final View convertView,
            @Nullable final ViewGroup parent,
            @Nullable final NativeResponse nativeResponse,
            @Nullable final ViewBinder viewBinder,
            @Nullable @SuppressWarnings("unused") final MoPubNativeListener moPubNativeListener) {
        final Activity activity = mActivity.get();
        if (activity == null) {
            MoPubLog.w("Weak reference to Activity Context in"
                    + " AdapterHelper became null. Returning empty view.");
            return new View(mApplicationContext);
        }

        return NativeAdViewHelper.getAdView(
                convertView,
                parent,
                activity,
                nativeResponse,
                viewBinder
        );
    }

    // Total number of content rows + ad rows
    @Deprecated
    public int shiftedCount(final int originalCount) {
        return originalCount + numberOfAdsThatCouldFitWithContent(originalCount);
    }

    // Shifted position of content in the backing list
    @Deprecated
    public int shiftedPosition(final int position) {
        return position - numberOfAdsSeenUpToPosition(position);
    }

    @Deprecated
    public boolean isAdPosition(final int position) {
        if (position < mStart) {
            return false;
        }

        return ((position - mStart) % mInterval == 0);
    }

    private int numberOfAdsSeenUpToPosition(final int position) {
        // This method takes a position from a list of content and ads mixed together
        // and calculates the number of ads seen up to that point

        if (position <= mStart) {
            return 0;
        }

        // Add 1 to the result since we start with an ad at start position and round down
        return (int) Math.floor((double) (position - mStart) / mInterval) + 1;
    }

    private int numberOfAdsThatCouldFitWithContent(final int contentRowCount) {
        // This method is passed the number of content rows from the backing list
        // and calculates how many ads could fit in with the content

        if (contentRowCount <= mStart) {
            return 0;
        }

        final int spacesBetweenAds = mInterval - 1;
        if ((contentRowCount - mStart) % spacesBetweenAds == 0) {
            // Don't add 1 to result since we never include an ad at the last position in the list
            return (contentRowCount - mStart) / spacesBetweenAds;
        } else {
            // Add 1 to the result since we start with an ad at start position and round down
            return (int) Math.floor((double) (contentRowCount - mStart) / spacesBetweenAds) + 1;
        }
    }

    // Testing
    @Deprecated
    @VisibleForTesting
    void clearActivityContext() {
        mActivity.clear();
    }
}
