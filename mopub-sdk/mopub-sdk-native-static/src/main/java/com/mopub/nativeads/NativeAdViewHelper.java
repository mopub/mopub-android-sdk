// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import com.mopub.mobileads.native_static.R;

/**
 * Use {@link MoPubStaticNativeAdRenderer} instead
 */
class NativeAdViewHelper {
    private NativeAdViewHelper() {
    }

    @VisibleForTesting
    enum ViewType {
        EMPTY,
        AD
    }

    @NonNull
    static View getAdView(@Nullable View convertView,
            @Nullable final ViewGroup parent,
            @NonNull final Context context,
            @Nullable final NativeAd nativeAd) {

        if (convertView != null) {
            clearNativeAd(convertView);
        }

        if (nativeAd == null || nativeAd.isDestroyed()) {
            MoPubLog.d("NativeAd null or invalid. Returning empty view");
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.EMPTY.equals(convertView.getTag())) {
                convertView = new View(context);
                convertView.setTag(ViewType.EMPTY);
                convertView.setVisibility(View.GONE);
            }
        } else {
            // Only create a view if one hasn't been created already
            if (convertView == null || !ViewType.AD.equals(convertView.getTag())) {
                convertView = nativeAd.createAdView(context, parent);
                convertView.setTag(ViewType.AD);
            }
            prepareNativeAd(convertView, nativeAd);
            nativeAd.renderAdView(convertView);
        }

        return convertView;
    }

    private static void clearNativeAd(@NonNull final View view) {
        final NativeAd nativeAd = (NativeAd) view.getTag(R.id.mopub_tag_NativeAdViewHelper_NativeAd);
        if (nativeAd != null) {
            view.setTag(R.id.mopub_tag_NativeAdViewHelper_NativeAd, null);
            nativeAd.clear(view);
        }
    }

    private static void prepareNativeAd(@NonNull final View view,
            @NonNull final NativeAd nativeAd) {
        view.setTag(R.id.mopub_tag_NativeAdViewHelper_NativeAd, nativeAd);
        nativeAd.prepare(view);
    }
}
