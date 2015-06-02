package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Preconditions.NoThrow;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubServerPositioning;

import java.util.List;
import java.util.WeakHashMap;
import static com.mopub.nativeads.VisibilityTracker.VisibilityTrackerListener;

/**
 * {@code MoPubAdRecycleAdapter} facilitates placing ads into an Android {@link android.support.v7.widget.RecyclerView}
 *
 * For your content items, this class will call your original adapter with the original position of
 * content before ads were loaded.
 *
 * This adapter uses a {@link MoPubStreamAdPlacer} object internally. If you
 * wish to avoid wrapping your original adapter, you can use {@code MoPubStreamAdPlacer} directly.
 *
 * Created by Shem Magnezi on 05/19/15.
 */
public class MoPubAdRecycleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @NonNull private WeakHashMap<View, Integer> mViewPositionMap;
    @NonNull private RecyclerView.Adapter<RecyclerView.ViewHolder> mOriginalAdapter;
    @NonNull private MoPubStreamAdPlacer mStreamAdPlacer;
    @NonNull private VisibilityTracker mVisibilityTracker;
    @Nullable private MoPubNativeAdLoadedListener mAdLoadedListener;

    /**
     * Creates a new MoPubAdRecycleAdapter object.
     *
     * By default, the adapter will contact the server to determine ad positions. If you
     * wish to hard-code positions in your app, see {@link com.mopub.nativeads.MoPubAdRecycleAdapter ( android.content.Context,
     * com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning)}.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     */
    public MoPubAdRecycleAdapter(@NonNull final Context context, @NonNull final RecyclerView.Adapter originalAdapter) {
        this(context, originalAdapter, MoPubNativeAdPositioning.serverPositioning());
    }

    /**
     * Creates a new MoPubAdRecycleAdapter object, using server positioning.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     * @param adPositioning A positioning object for specifying where ads will be placed in your
     * stream. See {@link com.mopub.nativeads.MoPubNativeAdPositioning#serverPositioning()}.
     */
    public MoPubAdRecycleAdapter(@NonNull final Context context,
                                 @NonNull final RecyclerView.Adapter originalAdapter,
                                 @NonNull final MoPubServerPositioning adPositioning) {
        this(new MoPubStreamAdPlacer(context, adPositioning), originalAdapter,
                new VisibilityTracker(context));
    }

    /**
     * Creates a new MoPubAdRecycleAdapter object, using client positioning.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     * @param adPositioning A positioning object for specifying where ads will be placed in your
     * stream. See {@link com.mopub.nativeads.MoPubNativeAdPositioning#clientPositioning()}.
     */
    public MoPubAdRecycleAdapter(@NonNull final Context context,
                                 @NonNull final RecyclerView.Adapter originalAdapter,
                                 @NonNull final MoPubClientPositioning adPositioning) {
        this(new MoPubStreamAdPlacer(context, adPositioning), originalAdapter,
                new VisibilityTracker(context));
    }

    @VisibleForTesting
    MoPubAdRecycleAdapter(@NonNull final MoPubStreamAdPlacer streamAdPlacer,
                          @NonNull final RecyclerView.Adapter originalAdapter,
                          @NonNull final VisibilityTracker visibilityTracker) {
        mOriginalAdapter = originalAdapter;
        mStreamAdPlacer = streamAdPlacer;
        mViewPositionMap = new WeakHashMap<View, Integer>();

        mVisibilityTracker = visibilityTracker;
        mVisibilityTracker.setVisibilityTrackerListener(new VisibilityTrackerListener() {
            @Override
            public void onVisibilityChanged(@NonNull final List<View> visibleViews,
                    final List<View> invisibleViews) {
                handleVisibilityChange(visibleViews);
            }
        });
        mOriginalAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
                notifyDataSetChanged();
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
                notifyDataSetChanged();
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
                notifyDataSetChanged();
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
                notifyDataSetChanged();
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
                notifyDataSetChanged();
            }
        });

        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
            }

            public void onItemRangeChanged(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
            }

            public void onItemRangeInserted(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
            }

            public void onItemRangeRemoved(int positionStart, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
            }

            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
            }
        });

        mStreamAdPlacer.setAdLoadedListener(new MoPubNativeAdLoadedListener() {
            @Override
            public void onAdLoaded(final int position) {
                handleAdLoaded(position);
            }

            @Override
            public void onAdRemoved(final int position) {
                handleAdRemoved(position);
            }

        });

        mStreamAdPlacer.setItemCount(mOriginalAdapter.getItemCount());
    }

    @VisibleForTesting
    void handleAdLoaded(final int position) {
        if (mAdLoadedListener != null) {
            mAdLoadedListener.onAdLoaded(position);
        }
        notifyDataSetChanged();
    }

    @VisibleForTesting
    void handleAdRemoved(final int position) {
        if (mAdLoadedListener != null) {
            mAdLoadedListener.onAdRemoved(position);
        }
        notifyDataSetChanged();
    }

    /**
     * Registers a {@link com.mopub.nativeads.MoPubNativeAdRenderer} to use when displaying ads in your stream.
     *
     * If you register a second renderer, it will replace the first, although this behavior is
     * subject to change in a future SDK version.
     *
     * @param adRenderer The ad renderer.
     */
    public final void registerAdRenderer(@NonNull final MoPubAdRenderer adRenderer) {
        if (!NoThrow.checkNotNull(
                adRenderer, "Tried to set a null ad renderer on the placer.")) {
            return;
        }
        mStreamAdPlacer.registerAdRenderer(adRenderer);
    }

    /**
     * Sets a listener that will be called after the SDK loads new ads from the server and places
     * them into your stream.
     *
     * The listener will be active between when you call {@link #loadAds} and when you call {@link
     * #destroy()}. You can also set the listener to {@code null} to remove the listener.
     *
     * Note that there is not a one to one correspondence between calls to {@link #loadAds} and this
     * listener. The SDK will call the listener every time an ad loads.
     *
     * @param listener The listener.
     */
    public final void setAdLoadedListener(@Nullable final MoPubNativeAdLoadedListener listener) {
        mAdLoadedListener = listener;
    }

    /**
     * Start loading ads from the MoPub server.
     *
     * We recommend using {@link #loadAds(String, com.mopub.nativeads.RequestParameters)} instead of this method, in
     * order to pass targeting information to the server.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     */
    public void loadAds(@NonNull final String adUnitId) {
        mStreamAdPlacer.loadAds(adUnitId);
    }

    /**
     * Start loading ads from the MoPub server, using the given request targeting information.
     *
     * When loading ads, {@link com.mopub.nativeads.MoPubNativeAdLoadedListener#onAdLoaded(int)} will be called for each
     * ad that is added to the stream.
     *
     * To refresh ads in your stream, call {@link #refreshAds(android.support.v7.widget.RecyclerView, String)}. When new ads load,
     * they will replace the current ads in your stream. If you are using {@code
     * MoPubNativeAdLoadedListener} you will see a call to {@code onAdRemoved} for each of the old
     * ads, followed by a calls to {@code onAdLoaded}.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     * @param requestParameters Targeting information to pass to the ad server.
     */
    public void loadAds(@NonNull final String adUnitId,
            @Nullable final RequestParameters requestParameters) {
        mStreamAdPlacer.loadAds(adUnitId, requestParameters);
    }

    /**
     * Whether the given position is an ad.
     *
     * This will return {@code true} only if there is an ad loaded for this position. You can also
     * listen for ads to load using {@link com.mopub.nativeads.MoPubNativeAdLoadedListener#onAdLoaded(int)}.
     *
     * @param position The position to check for an ad, expressed in terms of the position in the
     * stream including ads.
     * @return Whether there is an ad at the given position.
     */
    public boolean isAd(final int position) {
        return mStreamAdPlacer.isAd(position);
    }

    /**
     * Stops loading ads, immediately clearing any ads currently in the stream.
     *
     * This method also stops ads from loading as the user moves through the stream. If you want to
     * refresh ads, call {@link #refreshAds(android.support.v7.widget.RecyclerView, String, com.mopub.nativeads.RequestParameters)} instead of this
     * method.
     *
     * When ads are cleared, {@link com.mopub.nativeads.MoPubNativeAdLoadedListener#onAdRemoved} will be called for each
     * ad that is removed from the stream.
     */
    public void clearAds() {
        mStreamAdPlacer.clearAds();
    }

    /**
     * Destroys the ad adapter, preventing it from future use.
     *
     * You must call this method before the hosting activity for this class is destroyed in order to
     * avoid a memory leak. Typically you should destroy the adapter in the life-cycle method that
     * is counterpoint to the method you used to create the adapter. For example, if you created the
     * adapter in {@code Fragment#onCreateView} you should destroy it in {code
     * Fragment#onDestroyView}.
     */
    public void destroy() {
        mStreamAdPlacer.destroy();
        mVisibilityTracker.destroy();
    }

    private void handleVisibilityChange(@NonNull final List<View> visibleViews) {
        // Loop through all visible positions in order to build a max and min range, and then
        // place ads into that range.
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (final View view : visibleViews) {
            final Integer pos = mViewPositionMap.get(view);
            if (pos == null) {
                continue;
            }
            min = Math.min(pos, min);
            max = Math.max(pos, max);
        }
        mStreamAdPlacer.placeAdsInRange(min, max + 1);
    }

    /**
     * Returns the original position of an item considering ads in the stream.
     *
     * @see {@link com.mopub.nativeads.MoPubStreamAdPlacer#getOriginalPosition(int)}
     * @param position The adjusted position.
     * @return The original position before placing ads.
     */
    public int getOriginalPosition(final int position) {
        return mStreamAdPlacer.getOriginalPosition(position);
    }

    /**
     * Returns the position of an item considering ads in the stream.
     *
     * @see {@link com.mopub.nativeads.MoPubStreamAdPlacer#getAdjustedPosition(int)}
     * @param originalPosition The original position.
     * @return The position adjusted by placing ads.
     */
    public int getAdjustedPosition(final int originalPosition) {
        return mStreamAdPlacer.getAdjustedPosition(originalPosition);
    }

    /**
     * Refreshes ads in the given RecycleView while preserving the scroll position.
     *
     * Call this instead of {@link #loadAds(String)} in order to preserve the scroll position in
     * your list.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     */
    public void refreshAds(@NonNull final RecyclerView recycleView, @NonNull String adUnitId) {
        refreshAds(recycleView, adUnitId, null);
    }

    /**
     * Refreshes ads in the given RecycleView while preserving the scroll position.
     *
     * Call this instead of {@link #loadAds(String, com.mopub.nativeads.RequestParameters)} in order to preserve the
     * scroll position in your list.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     * @param requestParameters Targeting information to pass to the ad server.
     */
    public void refreshAds(@NonNull final RecyclerView recycleView,
            @NonNull String adUnitId, @Nullable RequestParameters requestParameters) {
        if (!NoThrow.checkNotNull(recycleView, "You called MoPubAdRecycleAdapter.refreshAds with a null " +
                "ListView")) {
            return;
        }

        // Get scroll offset of the first view, if it exists.
        View firstView = recycleView.getChildAt(0);
        int offsetY = (firstView == null) ? 0 : firstView.getTop();

        // Find the range of positions where we should not clear ads.
        RecyclerView.LayoutManager layoutManager = recycleView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager manager = (LinearLayoutManager) layoutManager;

            int firstPosition = manager.findFirstVisibleItemPosition();
            int startRange = Math.max(firstPosition - 1, 0);
            while (mStreamAdPlacer.isAd(startRange) && startRange > 0) {
                startRange--;
            }
            int lastPosition = manager.findLastVisibleItemPosition();
            while (mStreamAdPlacer.isAd(lastPosition) && lastPosition < getItemCount() - 1) {
                lastPosition++;
            }
            int originalStartRange = mStreamAdPlacer.getOriginalPosition(startRange);
            int originalEndRange = mStreamAdPlacer.getOriginalCount(lastPosition + 1);

            // Remove ads before and after the range.
            int originalCount = mStreamAdPlacer.getOriginalCount(getItemCount());
            mStreamAdPlacer.removeAdsInRange(originalEndRange, originalCount);
            int numAdsRemoved = mStreamAdPlacer.removeAdsInRange(0, originalStartRange);

            // Reset the scroll position, and reload ads.
            if (numAdsRemoved > 0) {
                manager.scrollToPositionWithOffset(firstPosition - numAdsRemoved, offsetY);
            }
        } else {
            MoPubLog.w("LayoutManager not support scrolling, position can't be saved");
        }
        loadAds(adUnitId, requestParameters);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Ad type
        if (viewType == 1) {
            View adView = mStreamAdPlacer.getAdView(parent);
            return new MoPubViewHolder(adView);
        } else {
            return mOriginalAdapter.onCreateViewHolder(parent, viewType - 2); //2 for the Ad type
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MoPubViewHolder) {
            mStreamAdPlacer.getAdView(position, holder.itemView, null);
        } else {
            mOriginalAdapter.onBindViewHolder(holder, mStreamAdPlacer.getOriginalPosition(position));
        }
        mViewPositionMap.put(holder.itemView, position);
        mVisibilityTracker.addView(holder.itemView, 0);

    }

    @Override
    public int getItemCount() {
        return mStreamAdPlacer.getAdjustedCount(mOriginalAdapter.getItemCount());
    }

    @Override
    public int getItemViewType(int position) {
        final int viewType = mStreamAdPlacer.getAdViewType(position);
        if (viewType != MoPubStreamAdPlacer.CONTENT_VIEW_TYPE) {
            return viewType;
        }
        return mOriginalAdapter.getItemViewType(mStreamAdPlacer.getOriginalPosition(position)) + 2; //2 for the Ad type
    }

    @Override
    public long getItemId(int position) {
        final Object adData = mStreamAdPlacer.getAdData(position);
        if (adData != null) {
            return ~System.identityHashCode(adData) + 1;
        }
        return mOriginalAdapter.getItemId(mStreamAdPlacer.getOriginalPosition(position));
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof MoPubViewHolder)) {
            mOriginalAdapter.onViewRecycled(holder);
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof MoPubViewHolder)) {
            mOriginalAdapter.onViewAttachedToWindow(holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof MoPubViewHolder)) {
            mOriginalAdapter.onViewDetachedFromWindow(holder);
        }
    }

    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mOriginalAdapter.onAttachedToRecyclerView(recyclerView);
    }

    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mOriginalAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    @NonNull
    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getOriginalAdapter() {
        return mOriginalAdapter;
    }

    public static class MoPubViewHolder extends RecyclerView.ViewHolder {
        public MoPubViewHolder(View view) {
            super(view);
        }
    }

}
