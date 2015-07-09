package com.mopub.nativeads;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.mopub.common.Preconditions;
import com.mopub.common.Preconditions.NoThrow;
import com.mopub.common.VisibleForTesting;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubServerPositioning;

import java.util.List;
import java.util.WeakHashMap;

import static android.widget.AdapterView.OnItemClickListener;
import static android.widget.AdapterView.OnItemLongClickListener;
import static android.widget.AdapterView.OnItemSelectedListener;
import static com.mopub.nativeads.VisibilityTracker.VisibilityTrackerListener;

/**
 * {@code MoPubAdAdapter} facilitates placing ads into an Android {@link android.widget.ListView} or
 * other widgets that use a {@link android.widget.ListAdapter}.
 *
 * For your content items, this class will call your original adapter with the original position of
 * content before ads were loaded.
 *
 * This adapter uses a {@link com.mopub.nativeads.MoPubStreamAdPlacer} object internally. If you
 * wish to avoid wrapping your original adapter, you can use {@code MoPubStreamAdPlacer} directly.
 */
public class MoPubAdAdapter extends BaseAdapter {
    @NonNull private final WeakHashMap<View, Integer> mViewPositionMap;
    @NonNull private final Adapter mOriginalAdapter;
    @NonNull private final MoPubStreamAdPlacer mStreamAdPlacer;
    @NonNull private final VisibilityTracker mVisibilityTracker;

    @Nullable private MoPubNativeAdLoadedListener mAdLoadedListener;

    /**
     * Creates a new MoPubAdAdapter object.
     *
     * By default, the adapter will contact the server to determine ad positions. If you
     * wish to hard-code positions in your app, see {@link MoPubAdAdapter(Context,
     * MoPubClientPositioning)}.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     */
    public MoPubAdAdapter(@NonNull final Context context, @NonNull final Adapter originalAdapter) {
        this(context, originalAdapter, MoPubNativeAdPositioning.serverPositioning());
    }

    /**
     * Creates a new MoPubAdAdapter object, using server positioning.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     * @param adPositioning A positioning object for specifying where ads will be placed in your
     * stream. See {@link MoPubNativeAdPositioning#serverPositioning()}.
     */
    public MoPubAdAdapter(@NonNull final Context context,
            @NonNull final Adapter originalAdapter,
            @NonNull final MoPubServerPositioning adPositioning) {
        this(new MoPubStreamAdPlacer(context, adPositioning), originalAdapter,
                new VisibilityTracker(context));
    }

    /**
     * Creates a new MoPubAdAdapter object, using client positioning.
     *
     * @param context The activity context.
     * @param originalAdapter Your original adapter.
     * @param adPositioning A positioning object for specifying where ads will be placed in your
     * stream. See {@link MoPubNativeAdPositioning#clientPositioning()}.
     */
    public MoPubAdAdapter(@NonNull final Context context,
            @NonNull final Adapter originalAdapter,
            @NonNull final MoPubClientPositioning adPositioning) {
        this(new MoPubStreamAdPlacer(context, adPositioning), originalAdapter,
                new VisibilityTracker(context));
    }

    @VisibleForTesting
    MoPubAdAdapter(@NonNull final MoPubStreamAdPlacer streamAdPlacer,
            @NonNull final Adapter originalAdapter,
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
        mOriginalAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                mStreamAdPlacer.setItemCount(mOriginalAdapter.getCount());
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
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

        mStreamAdPlacer.setItemCount(mOriginalAdapter.getCount());
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
     * Registers a {@link MoPubNativeAdRenderer} to use when displaying ads in your stream.
     *
     * This renderer will automatically create and render your view when you call {@link #getView}.
     * If you register a second renderer, it will replace the first, although this behavior is
     * subject to change in a future SDK version.
     *
     * @param adRenderer The ad renderer.
     *
     * @deprecated in version 3.9.0, use {@link #registerViewBinder(ViewBinder)} instead.
     */
    @Deprecated
    public final void registerAdRenderer(@NonNull final MoPubAdRenderer adRenderer) {
        if (!Preconditions.NoThrow.checkNotNull(
                adRenderer, "Tried to set a null ad renderer on the placer.")) {
            return;
        }
        mStreamAdPlacer.registerAdRenderer(adRenderer);
    }

    /**
     * Registers a {@link ViewBinder} to use when displaying ads in your stream.
     *
     * This binder will be used automatically to create and render your view when you call
     * {@link #getView}. If you register a second {@link ViewBinder}, it will replace the first,
     * although this behavior is subject to change in a future SDK version.
     *
     * @param viewBinder The view binder
     */
    public final void registerViewBinder(@NonNull final ViewBinder viewBinder) {
        if (!Preconditions.NoThrow.checkNotNull(
                viewBinder, "Tried to set a null view binder on the placer.")) {
            return;
        }
        mStreamAdPlacer.registerAdRenderer(new MoPubNativeAdRenderer(viewBinder));
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
     * We recommend using {@link #loadAds(String, RequestParameters)} instead of this method, in
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
     * When loading ads, {@link MoPubNativeAdLoadedListener#onAdLoaded(int)} will be called for each
     * ad that is added to the stream.
     *
     * To refresh ads in your stream, call {@link #refreshAds(ListView, String)}. When new ads load,
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
     * listen for ads to load using {@link MoPubNativeAdLoadedListener#onAdLoaded(int)}.
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
     * refresh ads, call {@link #refreshAds(ListView, String, RequestParameters)} instead of this
     * method.
     *
     * When ads are cleared, {@link MoPubNativeAdLoadedListener#onAdRemoved} will be called for each
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

    @Override
    public boolean areAllItemsEnabled() {
        return mOriginalAdapter instanceof ListAdapter
                && ((ListAdapter) mOriginalAdapter).areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(final int position) {
        return isAd(position) || (mOriginalAdapter instanceof ListAdapter
                && ((ListAdapter) mOriginalAdapter).isEnabled(mStreamAdPlacer.getOriginalPosition(
                position)));
    }

    /**
     * Returns the number of items in your stream, including ads.
     *
     * @return The count of items.
     * @inheritDoc
     */
    @Override
    public int getCount() {
        return mStreamAdPlacer.getAdjustedCount(mOriginalAdapter.getCount());
    }

    /**
     * For ad items, returns an ad data object. For non ad items, calls you original adapter using
     * the original item position.
     *
     * @inheritDoc
     */
    @Nullable
    @Override
    public Object getItem(final int position) {
        final Object ad = mStreamAdPlacer.getAdData(position);
        if (ad != null) {
            return ad;
        }
        return mOriginalAdapter.getItem(mStreamAdPlacer.getOriginalPosition(position));
    }

    /**
     * For ad items, returns an ID representing the ad. For non ad items, calls your original
     * adapter using the original item position.
     *
     * For ads, this ID will be a negative integer. If you feel that this ID might collide with your
     * original adapter's IDs, you should return {@code false} from {@code #hasStableIds()}.
     *
     * @inheritDoc
     */
    @Override
    public long getItemId(final int position) {
        final Object adData = mStreamAdPlacer.getAdData(position);
        if (adData != null) {
            return -System.identityHashCode(adData);
        }
        return mOriginalAdapter.getItemId(mStreamAdPlacer.getOriginalPosition(position));
    }

    /**
     * Returns the value returned by {@code hasStableIds()} on your original adapter.
     *
     * @inheritDoc
     */
    @Override
    public boolean hasStableIds() {
        return mOriginalAdapter.hasStableIds();
    }

    /**
     * For ad items, returns an ad View for the underlying position. For non-ad items, calls your
     * original adapter using the original ad position.
     *
     * @inheritDoc
     */
    @Nullable
    @Override
    public View getView(final int position, final View view, final ViewGroup viewGroup) {
        final View resultView;
        final View adView = mStreamAdPlacer.getAdView(position, view, viewGroup);
        if (adView != null) {
            resultView = adView;
        } else {
            resultView = mOriginalAdapter.getView(
                    mStreamAdPlacer.getOriginalPosition(position), view, viewGroup);
        }
        mViewPositionMap.put(resultView, position);
        mVisibilityTracker.addView(resultView, 0);

        return resultView;
    }

    /**
     * For ad items, returns a number greater than or equal to the view type count for your
     * underlying adapter. For non-ad items, calls your original adapter using the original ad
     * position.
     *
     * @inheritDoc
     */
    @Override
    public int getItemViewType(final int position) {
        final int viewType = mStreamAdPlacer.getAdViewType(position);
        if (viewType != MoPubStreamAdPlacer.CONTENT_VIEW_TYPE) {
            return viewType + mOriginalAdapter.getViewTypeCount() - 1;
        }
        return mOriginalAdapter.getItemViewType(mStreamAdPlacer.getOriginalPosition(position));
    }

    /**
     * Returns the view type count of your original adapter, plus the the number of possible view
     * types for ads. The number of possible ad view types is currently 1, but this is subject to
     * change in future SDK versions.
     *
     * @inheritDoc
     */
    @Override
    public int getViewTypeCount() {
        return mOriginalAdapter.getViewTypeCount() + mStreamAdPlacer.getAdViewTypeCount();
    }

    /**
     * Returns whether the adapter is empty, calling through to your original adapter.
     *
     * @inheritDoc
     */
    @Override
    public boolean isEmpty() {
        return mOriginalAdapter.isEmpty() && mStreamAdPlacer.getAdjustedCount(0) == 0;
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
     * @see {@link MoPubStreamAdPlacer#getOriginalPosition(int)}
     * @param position The adjusted position.
     * @return The original position before placing ads.
     */
    public int getOriginalPosition(final int position) {
        return mStreamAdPlacer.getOriginalPosition(position);
    }

    /**
     * Returns the position of an item considering ads in the stream.
     *
     * @see {@link MoPubStreamAdPlacer#getAdjustedPosition(int)}
     * @param originalPosition The original position.
     * @return The position adjusted by placing ads.
     */
    public int getAdjustedPosition(final int originalPosition) {
        return mStreamAdPlacer.getAdjustedPosition(originalPosition);
    }

    /**
     * Inserts a content row at the given position, adjusting ad positions accordingly.
     *
     * Use this method if you are inserting an item into your stream and want to increment ad
     * positions based on that new item.
     *
     * If you do not want to increment your ad positions when inserting items, you can simply call
     * notifyDataSetChanged on the adapter and let it reload items normally. This is typically the
     * case when inserting items at the end of your stream.
     *
     * @see {@link MoPubStreamAdPlacer#insertItem(int)}
     * @param originalPosition The original content position at which to add an item. If you have an
     * adjusted position, you will need to call {@link #getOriginalPosition} to get this value.
     */
    public void insertItem(final int originalPosition) {
        mStreamAdPlacer.insertItem(originalPosition);
    }

    /**
     * Removes the content row at the given position, adjusting ad positions accordingly.
     *
     * Use this method if you are removing an item from your stream and want to decrement ad
     * positions based on that removed item.
     *
     * If you do not want to decrement your ad positions when inserting items, you can simply call
     * notifyDataSet changed on the adapter and let it reload items normally. This is typically the
     * case when removing items from the end of your stream.
     *
     * @see {@link MoPubStreamAdPlacer#removeItem(int)}
     * @param originalPosition The original content position at which to add an item. If you have an
     * adjusted position, you will need to call {@link #getOriginalPosition} to get this value.
     */
    public void removeItem(final int originalPosition) {
        mStreamAdPlacer.removeItem(originalPosition);
    }

    /**
     * Sets an on click listener for the given ListView, automatically adjusting the listener
     * callback positions based on ads in the adapter.
     *
     * This listener will not be called when ads are clicked.
     *
     * @param listView The ListView for this adapter.
     * @param listener An on click listener.
     */
    public void setOnClickListener(@NonNull final ListView listView,
            @Nullable final OnItemClickListener listener) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter.setOnClickListener with a" +
                " null ListView")) {
            return;
        }
        if (listener == null) {
            listView.setOnItemClickListener(null);
            return;
        }

        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, final View view,
                    final int position, final long id) {
                if (!mStreamAdPlacer.isAd(position)) {
                    listener.onItemClick(
                            adapterView, view, mStreamAdPlacer.getOriginalPosition(position), id);
                }
            }
        });
    }

    /**
     * Sets an on long click listener for the given ListView, automatically adjusting the listener
     * callback positions based on ads in the adapter.
     *
     * This listener will not be called when ads are long clicked.
     *
     * @param listView The ListView for this adapter.
     * @param listener An an long click listener.
     */
    public void setOnItemLongClickListener(@NonNull final ListView listView,
            @Nullable final OnItemLongClickListener listener) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter." +
                "setOnItemLongClickListener with a null ListView")) {
            return;
        }
        if (listener == null) {
            listView.setOnItemLongClickListener(null);
            return;
        }

        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView,
                    final View view, final int position, final long id) {
                return isAd(position) || listener.onItemLongClick(
                        adapterView, view, mStreamAdPlacer.getOriginalPosition(position), id);
            }
        });
    }

    /**
     * Sets an on item selected listener for the given ListView, automatically adjusting the
     * listener callback positions based on ads in the adapter.
     *
     * @param listView The ListView for this adapter.
     * @param listener An an item selected listener.
     */
    public void setOnItemSelectedListener(@NonNull final ListView listView,
            @Nullable final OnItemSelectedListener listener) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter.setOnItemSelectedListener" +
                " with a null ListView")) {
            return;
        }
        if (listener == null) {
            listView.setOnItemSelectedListener(null);
            return;
        }

        listView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> adapterView,
                    final View view, final int position, final long id) {
                if (!isAd(position)) {
                    listener.onItemSelected(adapterView, view,
                            mStreamAdPlacer.getOriginalPosition(position), id);
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> adapterView) {
                listener.onNothingSelected(adapterView);
            }
        });
    }

    /**
     * Sets the currently selected item in the ListView, automatically adjusting the position based
     * on ads in the adapter.
     *
     * @param listView The ListView for this adapter.
     * @param originalPosition The original content position before loading ads.
     */
    public void setSelection(@NonNull final ListView listView, final int originalPosition) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter.setSelection with a null " +
                "ListView")) {
            return;
        }

        listView.setSelection(mStreamAdPlacer.getAdjustedPosition(originalPosition));
    }

    /**
     * Scrolls an item in the ListView, automatically adjusting the position based on ads in the
     * adapter.
     *
     * @param listView The ListView for this adapter.
     * @param originalPosition The original content position before loading ads.
     */
    public void smoothScrollToPosition(@NonNull final ListView listView,
            final int originalPosition) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter.smoothScrollToPosition " +
                "with a null ListView")) {
            return;
        }

        listView.smoothScrollToPosition(mStreamAdPlacer.getAdjustedPosition(originalPosition));
    }

    /**
     * Refreshes ads in the given ListView while preserving the scroll position.
     *
     * Call this instead of {@link #loadAds(String)} in order to preserve the scroll position in
     * your list.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     */
    public void refreshAds(@NonNull final ListView listView, @NonNull String adUnitId) {
        refreshAds(listView, adUnitId, null);
    }

    /**
     * Refreshes ads in the given ListView while preserving the scroll position.
     *
     * Call this instead of {@link #loadAds(String, RequestParameters)} in order to preserve the
     * scroll position in your list.
     *
     * @param adUnitId The ad unit ID to use when loading ads.
     * @param requestParameters Targeting information to pass to the ad server.
     */
    public void refreshAds(@NonNull final ListView listView,
            @NonNull String adUnitId, @Nullable RequestParameters requestParameters) {
        if (!NoThrow.checkNotNull(listView, "You called MoPubAdAdapter.refreshAds with a null " +
                "ListView")) {
            return;
        }

        // Get scroll offset of the first view, if it exists.
        View firstView = listView.getChildAt(0);
        int offsetY = (firstView == null) ? 0 : firstView.getTop();

        // Find the range of positions where we should not clear ads.
        int firstPosition = listView.getFirstVisiblePosition();
        int startRange = Math.max(firstPosition - 1, 0);
        while (mStreamAdPlacer.isAd(startRange) && startRange > 0) {
            startRange--;
        }
        int lastPosition = listView.getLastVisiblePosition();
        while (mStreamAdPlacer.isAd(lastPosition) && lastPosition < getCount() - 1) {
            lastPosition++;
        }
        int originalStartRange = mStreamAdPlacer.getOriginalPosition(startRange);
        int originalEndRange = mStreamAdPlacer.getOriginalCount(lastPosition + 1);

        // Remove ads before and after the range.
        int originalCount = mStreamAdPlacer.getOriginalCount(getCount());
        mStreamAdPlacer.removeAdsInRange(originalEndRange, originalCount);
        int numAdsRemoved = mStreamAdPlacer.removeAdsInRange(0, originalStartRange);

        // Reset the scroll position, and reload ads.
        if (numAdsRemoved > 0) {
            listView.setSelectionFromTop(firstPosition - numAdsRemoved, offsetY);
        }
        loadAds(adUnitId, requestParameters);
    }
}
