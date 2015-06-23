package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.WeakHashMap;

/**
 * @deprecated As of release 2.4, use {@link com.mopub.nativeads.MoPubNativeAdRenderer} instead
 */
@Deprecated
class NativeAdViewHelper {
    private NativeAdViewHelper() {
    }

    @VisibleForTesting
    enum ViewType {
        EMPTY,
        AD
    }

    // Because the impression tracker requires tracking drawing views,
    // each context requires a separate impression tracker. To avoid leaking, keep weak references.
    @VisibleForTesting
    static final WeakHashMap<Context, ImpressionTracker> sImpressionTrackerMap =
            new WeakHashMap<Context, ImpressionTracker>();

    // Used to keep track of the last NativeResponse a view was associated with in order to clean
    // up its state before associating with a new NativeResponse
    static private final WeakHashMap<View, NativeResponse> sNativeResponseMap =
            new WeakHashMap<View, NativeResponse>();

    @Deprecated
    @NonNull
    static View getAdView(@Nullable View convertView,
            @Nullable final ViewGroup parent,
            @NonNull final Context context,
            @Nullable final NativeResponse nativeResponse,
            @Nullable final ViewBinder viewBinder) {

        Preconditions.NoThrow.checkNotNull(viewBinder, "ViewBinder is null.");

        if (convertView != null) {
            clearNativeResponse(context, convertView);
        }

        if (nativeResponse == null || nativeResponse.isDestroyed() || viewBinder == null) {
            MoPubLog.d("nativeResponse or viewBinder null or invalid. Returning empty view");
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.EMPTY.equals(convertView.getTag())) {
                convertView = new View(context);
                convertView.setTag(ViewType.EMPTY);
                convertView.setVisibility(View.GONE);
            }
        } else {
            final MoPubNativeAdRenderer moPubNativeAdRenderer = new MoPubNativeAdRenderer(viewBinder);
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.AD.equals(convertView.getTag())) {
                convertView = moPubNativeAdRenderer.createAdView(context, parent);
                convertView.setTag(ViewType.AD);
            }
            prepareNativeResponse(context, convertView, nativeResponse);
            moPubNativeAdRenderer.renderAdView(convertView, nativeResponse);
        }

        return convertView;
    }

    private static void clearNativeResponse(@NonNull final Context context,
            @NonNull final View view) {
        getImpressionTracker(context).removeView(view);
        final NativeResponse nativeResponse = sNativeResponseMap.get(view);
        if (nativeResponse != null) {
            nativeResponse.clear(view);
        }
    }

    private static void prepareNativeResponse(@NonNull final Context context,
            @NonNull final View view,
            @NonNull final NativeResponse nativeResponse) {
        sNativeResponseMap.put(view, nativeResponse);
        if (!nativeResponse.isOverridingImpressionTracker()) {
            getImpressionTracker(context).addView(view, nativeResponse);
        }
        nativeResponse.prepare(view);
    }

    private static ImpressionTracker getImpressionTracker(@NonNull final Context context) {
        ImpressionTracker impressionTracker = sImpressionTrackerMap.get(context);
        if (impressionTracker == null) {
            impressionTracker = new ImpressionTracker(context);
            sImpressionTrackerMap.put(context, impressionTracker);
        }
        return impressionTracker;
    }
}
