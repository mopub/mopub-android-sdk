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

import java.util.WeakHashMap;

/**
 * @deprecated As of release 2.4, use {@link MoPubStaticNativeAdRenderer} instead
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

    /**
     * Used to keep track of the last {@link NativeAd} a view was associated with in order to clean
     * up its state before associating with a new {@link NativeAd}
     */
    static private final WeakHashMap<View, NativeAd> sNativeAdMap =
            new WeakHashMap<View, NativeAd>();

    @Deprecated
    @NonNull
    static View getAdView(@Nullable View convertView,
            @Nullable final ViewGroup parent,
            @NonNull final Activity activity,
            @Nullable final NativeAd nativeAd,
            @Nullable final ViewBinder viewBinder) {

        Preconditions.NoThrow.checkNotNull(viewBinder, "ViewBinder is null.");

        if (convertView != null) {
            clearNativeAd(activity, convertView);
        }

        if (nativeAd == null || nativeAd.isDestroyed() || viewBinder == null) {
            MoPubLog.d("NativeAd or viewBinder null or invalid. Returning empty view");
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.EMPTY.equals(convertView.getTag())) {
                convertView = new View(activity);
                convertView.setTag(ViewType.EMPTY);
                convertView.setVisibility(View.GONE);
            }
        } else {
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.AD.equals(convertView.getTag())) {
                convertView = nativeAd.createAdView(activity, parent);
                convertView.setTag(ViewType.AD);
            }
            prepareNativeAd(activity, convertView, nativeAd);
            nativeAd.renderAdView(convertView);
        }

        return convertView;
    }

    private static void clearNativeAd(@NonNull final Context context,
            @NonNull final View view) {
        final NativeAd nativeAd = sNativeAdMap.get(view);
        if (nativeAd != null) {
            nativeAd.clear(view);
        }
    }

    private static void prepareNativeAd(@NonNull final Context context,
            @NonNull final View view,
            @NonNull final NativeAd nativeAd) {
        sNativeAdMap.put(view, nativeAd);
        nativeAd.prepare(view);
    }
}
