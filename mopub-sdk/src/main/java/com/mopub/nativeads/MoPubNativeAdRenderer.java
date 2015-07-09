package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;

import java.util.WeakHashMap;

import static android.view.View.VISIBLE;

/**
 * An implementation of {@link com.mopub.nativeads.MoPubAdRenderer} for rendering native ads.
 */
public class MoPubNativeAdRenderer implements MoPubAdRenderer<NativeResponse> {
    @NonNull private final ViewBinder mViewBinder;

    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    @VisibleForTesting @NonNull final WeakHashMap<View, NativeViewHolder> mViewHolderMap;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param viewBinder The view binder to use when inflating and rendering an ad.
     */
    public MoPubNativeAdRenderer(@NonNull final ViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<View, NativeViewHolder>();
    }

    @Override
    @NonNull
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {
        return LayoutInflater
                .from(context)
                .inflate(mViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull final View view,
            @NonNull final NativeResponse nativeResponse) {
        NativeViewHolder nativeViewHolder = mViewHolderMap.get(view);
        if (nativeViewHolder == null) {
            nativeViewHolder = NativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, nativeViewHolder);
        }

        nativeViewHolder.update(nativeResponse);
        nativeViewHolder.updateExtras(nativeResponse, mViewBinder);
        nativeViewHolder.setViewVisibility(VISIBLE);
    }
}
