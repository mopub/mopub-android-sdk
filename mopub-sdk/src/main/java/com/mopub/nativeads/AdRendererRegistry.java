package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;

import java.util.ArrayList;

/**
 * A data structure providing methods to store and retrieve native ad renderers.
 */
public class AdRendererRegistry {

    @NonNull private final ArrayList<MoPubAdRenderer> mMoPubAdRenderers;

    public AdRendererRegistry() {
        mMoPubAdRenderers = new ArrayList<MoPubAdRenderer>();
    }

    /**
     * Registers an ad renderer for rendering a specific native ad format.
     * Note that if multiple ad renderers support a specific native ad format, the first
     * one registered will be used.
     */
    public void registerAdRenderer(@NonNull final MoPubAdRenderer moPubAdRenderer) {
        mMoPubAdRenderers.add(moPubAdRenderer);
    }

    public int getAdRendererCount() {
        return mMoPubAdRenderers.size();
    }

    @NonNull
    public Iterable<MoPubAdRenderer> getRendererIterable() {
        return mMoPubAdRenderers;
    }

    /**
     * Returns the view type of the first registered ad renderer that supports rendering the
     * {@link NativeAd} passed in. View types reserved for native ads are greater than or equal
     * to 1, hence we add 1 when returning the view type.
     *
     * @param nativeAd The {@link NativeAd} to render.
     * @return The integer representing the view type of the first renderer registered that
     *         supports rendering the {@link NativeAd}.
     */
    public int getViewTypeForAd(@NonNull final NativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        for (int i = 0; i < mMoPubAdRenderers.size(); ++i) {
            if (nativeAd.getMoPubAdRenderer() == mMoPubAdRenderers.get(i)) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Returns the first registered ad renderer that supports rendering the native ad passed in.
     *
     * @param nativeAd The native ad to render.
     * @return The renderer that supports rendering the native ad.
     */
    @Nullable
    public MoPubAdRenderer getRendererForAd(@NonNull final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        for (MoPubAdRenderer moPubAdRenderer : mMoPubAdRenderers) {
            if (moPubAdRenderer.supports(nativeAd)) {
                return moPubAdRenderer;
            }
        }
        return null;
    }

    /**
     * Returns the renderer corresponding to view type passed in. View types reserved for native
     * ads are greater than or equal to 1, hence we subtract 1 when matching the renderer.
     *
     * @param viewType The integer representing the view type of renderer.
     * @return The renderer mapped to the view type.
     */
    @Nullable
    public MoPubAdRenderer getRendererForViewType(final int viewType) {
        try {
            return mMoPubAdRenderers.get(viewType - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
