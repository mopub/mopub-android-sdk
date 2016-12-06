package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

/**
 * An interface for creating ad views and rendering them using ad data.
 *
 * Normally you will use the subclass {@link MoPubStaticNativeAdRenderer} with {@link
 * com.mopub.nativeads.ViewBinder} to customize your ad view with your own layout. However, if you
 * wish to programmatically create or manage your ad view, you can implement {@code MoPubAdRenderer}
 * directly.
 *
 * @param <T> The ad payload type.
 */
public interface MoPubAdRenderer<T extends BaseNativeAd> {
    /**
     * Creates a new view to be used as an ad.
     * <p/>
     * This method is called when you call {@link com.mopub.nativeads.MoPubStreamAdPlacer#getAdView}
     * and the convertView is null. You must return a valid view.
     *
     * @param context The context. Useful for creating a view. This is recommended to be an
     *                Activity. If you have custom themes defined in your Activity, not passing
     *                in that Activity will result in the default Application theme being used
     *                when creating the ad view.
     * @param parent  The parent that the view will eventually be attached to. You might use the
     *                parent to determine layout parameters, but should return the view without
     *                attaching it to the parent.
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

    /**
     * Determines if this renderer supports the type of native ad passed in.
     *
     * @param nativeAd The native ad to render.
     * @return True if the renderer can render the native ad and false if it cannot.
     */
    boolean supports(@NonNull BaseNativeAd nativeAd);
}
