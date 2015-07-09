package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

/**
 * An interface for creating ad views and rendering them using ad data.
 *
 * Normally you will use the subclass {@link com.mopub.nativeads.MoPubNativeAdRenderer} with {@link
 * com.mopub.nativeads.ViewBinder} to customize your ad view with your own layout. However, if you
 * wish to programmatically create or manage your ad view, you can implement {@code MoPubAdRenderer}
 * directly.
 *
 * @param <T> The ad payload type.
 */
public interface MoPubAdRenderer<T> {
    /**
     * Creates a new view to be used as an ad.
     *
     * This method is called when you call {@link com.mopub.nativeads.MoPubStreamAdPlacer#getAdView}
     * and the convertView is null. You must return a valid view.
     *
     * @param context The context. Useful for creating a view.
     * @param parent The parent that the view will eventually be attached to. You might use the
     * parent to determine layout parameters, but should return the view without attaching it to the
     * parent.
     * @return A new ad view.
     */
    @NonNull
    View createAdView(@NonNull Context context, @Nullable ViewGroup parent);

    /**
     * Renders a view created by {@link #createAdView} by filling it with ad data.
     *
     * @param view The ad {@link View}
     * @param ad The ad data that should be bound to the view.
     */
    void renderAdView(@NonNull View view, @NonNull T ad);
}
