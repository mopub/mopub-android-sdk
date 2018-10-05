// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import com.mopub.mobileads.native_static.R;

import static android.view.View.VISIBLE;

/**
 * An implementation of {@link com.mopub.nativeads.MoPubAdRenderer} for rendering native ads.
 */
public class MoPubStaticNativeAdRenderer implements MoPubAdRenderer<StaticNativeAd> {
    @NonNull private final ViewBinder mViewBinder;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param viewBinder The view binder to use when inflating and rendering an ad.
     */
    public MoPubStaticNativeAdRenderer(@NonNull final ViewBinder viewBinder) {
        mViewBinder = viewBinder;
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
            @NonNull final StaticNativeAd staticNativeAd) {
        StaticNativeViewHolder staticNativeViewHolder = (StaticNativeViewHolder)
            view.getTag(R.id.mopub_tag_MoPubStaticNativeAdRenderer_StaticNativeViewHolder);
        if (staticNativeViewHolder == null) {
            staticNativeViewHolder = StaticNativeViewHolder.fromViewBinder(view, mViewBinder);
            view.setTag(R.id.mopub_tag_MoPubStaticNativeAdRenderer_StaticNativeViewHolder, staticNativeViewHolder);
        }

        update(staticNativeViewHolder, staticNativeAd);
        NativeRendererHelper.updateExtras(staticNativeViewHolder.mainView,
                mViewBinder.extras,
                staticNativeAd.getExtras());
        setViewVisibility(staticNativeViewHolder, VISIBLE);
    }

    @Override
    public boolean supports(@NonNull final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof StaticNativeAd;
    }

    private void update(@NonNull final StaticNativeViewHolder staticNativeViewHolder,
            @NonNull final StaticNativeAd staticNativeAd) {
        NativeRendererHelper.addTextView(staticNativeViewHolder.titleView,
                staticNativeAd.getTitle());
        NativeRendererHelper.addTextView(staticNativeViewHolder.textView, staticNativeAd.getText());
        NativeRendererHelper.addTextView(staticNativeViewHolder.callToActionView,
                staticNativeAd.getCallToAction());
        NativeImageHelper.loadImageView(staticNativeAd.getMainImageUrl(),
                staticNativeViewHolder.mainImageView);
        NativeImageHelper.loadImageView(staticNativeAd.getIconImageUrl(),
                staticNativeViewHolder.iconImageView);
        NativeRendererHelper.addPrivacyInformationIcon(
                staticNativeViewHolder.privacyInformationIconImageView,
                staticNativeAd.getPrivacyInformationIconImageUrl(),
                staticNativeAd.getPrivacyInformationIconClickThroughUrl());
    }

    private void setViewVisibility(@NonNull final StaticNativeViewHolder staticNativeViewHolder,
            final int visibility) {
        if (staticNativeViewHolder.mainView != null) {
            staticNativeViewHolder.mainView.setVisibility(visibility);
        }
    }
}
