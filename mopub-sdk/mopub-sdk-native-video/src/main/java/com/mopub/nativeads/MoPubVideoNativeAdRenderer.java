package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.WeakHashMap;

import static android.view.View.VISIBLE;

public class MoPubVideoNativeAdRenderer implements MoPubAdRenderer<VideoNativeAd> {
    @NonNull private final MediaViewBinder mMediaViewBinder;

    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    @VisibleForTesting
    @NonNull final WeakHashMap<View, MediaViewHolder> mMediaViewHolderMap;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param mediaViewBinder The view binder to use when inflating and rendering an ad.
     */
    public MoPubVideoNativeAdRenderer(@NonNull final MediaViewBinder mediaViewBinder) {
        mMediaViewBinder = mediaViewBinder;
        mMediaViewHolderMap = new WeakHashMap<View, MediaViewHolder>();
    }

    @Override
    @NonNull
    public View createAdView(@NonNull final Context context, @Nullable final ViewGroup parent) {
        return LayoutInflater
                .from(context)
                .inflate(mMediaViewBinder.layoutId, parent, false);
    }

    @Override
    public void renderAdView(@NonNull final View view,
            @NonNull final VideoNativeAd videoNativeAd) {
        MediaViewHolder mediaViewHolder = mMediaViewHolderMap.get(view);
        if (mediaViewHolder == null) {
            mediaViewHolder = MediaViewHolder.fromViewBinder(view, mMediaViewBinder);
            mMediaViewHolderMap.put(view, mediaViewHolder);
        }

        update(mediaViewHolder, videoNativeAd);
        NativeRendererHelper.updateExtras(mediaViewHolder.mainView, mMediaViewBinder.extras, videoNativeAd.getExtras());
        setViewVisibility(mediaViewHolder, VISIBLE);

        MediaLayout mediaLayout = (MediaLayout) view.findViewById(mMediaViewBinder.mediaLayoutId);
        videoNativeAd.render(mediaLayout);
    }

    @Override
    public boolean supports(@NonNull final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof VideoNativeAd;
    }

    private void update(@NonNull final MediaViewHolder mediaViewHolder,
            @NonNull final VideoNativeAd videoNativeAd) {
        NativeRendererHelper.addTextView(mediaViewHolder.titleView,
                videoNativeAd.getTitle());
        NativeRendererHelper.addTextView(mediaViewHolder.textView, videoNativeAd.getText());
        NativeRendererHelper.addCtaButton(mediaViewHolder.callToActionView,
                mediaViewHolder.mainView, videoNativeAd.getCallToAction()
        );
        if (mediaViewHolder.mediaLayout != null) {
            NativeImageHelper.loadImageView(videoNativeAd.getMainImageUrl(),
                    mediaViewHolder.mediaLayout.getMainImageView());
        }
        NativeImageHelper.loadImageView(videoNativeAd.getIconImageUrl(),
                mediaViewHolder.iconImageView);
        NativeRendererHelper.addPrivacyInformationIcon(
                mediaViewHolder.privacyInformationIconImageView,
                videoNativeAd.getPrivacyInformationIconImageUrl(),
                videoNativeAd.getPrivacyInformationIconClickThroughUrl());
    }

    private void setViewVisibility(@NonNull final MediaViewHolder mediaViewHolder,
            final int visibility) {
        if (mediaViewHolder.mainView != null) {
            mediaViewHolder.mainView.setVisibility(visibility);
        }
    }
}

