package com.mopub.nativeads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.WeakHashMap;

import static android.view.View.GONE;
import static com.mopub.nativeads.MoPubNative.MoPubNativeListener;

/**
 * @deprecated As of release 2.4, use {@link com.mopub.nativeads.MoPubNativeAdRenderer} instead
 */
@Deprecated
class NativeAdViewHelper {
    private NativeAdViewHelper() {}

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
    static View getAdView(View convertView,
                          final ViewGroup parent,
                          final Context context,
                          final NativeResponse nativeResponse,
                          final ViewBinder viewBinder,
                          final MoPubNativeListener moPubNativeListener) {

        if (viewBinder == null) {
            MoPubLog.d("ViewBinder is null, returning empty view.");
            return new View(context);
        }

        final MoPubNativeAdRenderer moPubNativeAdRenderer = new MoPubNativeAdRenderer(viewBinder);
        if (convertView == null) {
            convertView = moPubNativeAdRenderer.createAdView(context, parent);
        }

        clearNativeResponse(context, convertView);

        if (nativeResponse == null) {
            // If we don't have content for the view, then hide the view for now
            MoPubLog.d("NativeResponse is null, returning hidden view.");
            convertView.setVisibility(GONE);
        } else if (nativeResponse.isDestroyed()) {
            MoPubLog.d("NativeResponse is destroyed, returning hidden view.");
            convertView.setVisibility(GONE);
        } else {
            prepareNativeResponse(context, convertView, nativeResponse);
            moPubNativeAdRenderer.renderAdView(convertView, nativeResponse);
        }

        return convertView;
    }

    private static void clearNativeResponse(final Context context, final View view) {
        getImpressionTracker(context).removeView(view);
        final NativeResponse nativeResponse = sNativeResponseMap.get(view);
        if (nativeResponse != null) {
            nativeResponse.clear(view);
        }
    }

    private static void prepareNativeResponse(final Context context,
            final View view,
            final NativeResponse nativeResponse) {
        sNativeResponseMap.put(view, nativeResponse);
        if (!nativeResponse.isOverridingImpressionTracker()) {
            getImpressionTracker(context).addView(view, nativeResponse);
        }
        nativeResponse.prepare(view);
    }

    private static ImpressionTracker getImpressionTracker(final Context context) {
        ImpressionTracker impressionTracker = sImpressionTrackerMap.get(context);
        if (impressionTracker == null) {
            impressionTracker = new ImpressionTracker(context);
            sImpressionTrackerMap.put(context, impressionTracker);
        }
        return impressionTracker;
    }
}
