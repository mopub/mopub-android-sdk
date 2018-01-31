package com.mopub.nativeads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.ads.MediaView;
import com.mopub.common.Preconditions;

import java.util.WeakHashMap;

import static android.view.View.VISIBLE;

/**
 * Include this class if you want to use Facebook native video ads. This renderer handles Facebook
 * static and video native ads. This will automatically replace the main image view with the
 * Facebook MediaView that handles showing the main asset.
 * Certified with Facebook Audience Network 4.26.1
 */
public class FacebookAdRenderer implements MoPubAdRenderer<FacebookNative.FacebookVideoEnabledNativeAd> {
    private final ViewBinder mViewBinder;

    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    final WeakHashMap<View, FacebookNativeViewHolder> mViewHolderMap;

    /**
     * Constructs a native ad renderer with a view binder.
     *
     * @param viewBinder The view binder to use when inflating and rendering an ad.
     */
    public FacebookAdRenderer(final ViewBinder viewBinder) {
        mViewBinder = viewBinder;
        mViewHolderMap = new WeakHashMap<View, FacebookNativeViewHolder>();
    }

    @Override
    public View createAdView(final Context context, final ViewGroup parent) {
        final View adView = LayoutInflater
                .from(context)
                .inflate(mViewBinder.layoutId, parent, false);
        final View mainImageView = adView.findViewById(mViewBinder.mainImageId);
        if (mainImageView == null) {
            return adView;
        }

        final ViewGroup.LayoutParams mainImageViewLayoutParams = mainImageView.getLayoutParams();
        final MediaView.LayoutParams mediaViewLayoutParams = new MediaView.LayoutParams(
                mainImageViewLayoutParams.width, mainImageViewLayoutParams.height);

        if (mainImageViewLayoutParams instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams marginParams =
                    (ViewGroup.MarginLayoutParams) mainImageViewLayoutParams;
            mediaViewLayoutParams.setMargins(marginParams.leftMargin,
                    marginParams.topMargin,
                    marginParams.rightMargin,
                    marginParams.bottomMargin);
        }

        if (mainImageViewLayoutParams instanceof RelativeLayout.LayoutParams) {
            final RelativeLayout.LayoutParams mainImageViewRelativeLayoutParams =
                    (RelativeLayout.LayoutParams) mainImageViewLayoutParams;
            final int[] rules = mainImageViewRelativeLayoutParams.getRules();
            for (int i = 0; i < rules.length; i++) {
                mediaViewLayoutParams.addRule(i, rules[i]);
            }
            mainImageView.setVisibility(View.INVISIBLE);
        } else {
            mainImageView.setVisibility(View.GONE);
        }

        final MediaView mediaView = new MediaView(context);
        ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();
        int mainImageIndex = mainImageParent.indexOfChild(mainImageView);
        mainImageParent.addView(mediaView, mainImageIndex + 1, mediaViewLayoutParams);
        return adView;
    }

    @Override
    public void renderAdView(final View view,
            final FacebookNative.FacebookVideoEnabledNativeAd facebookVideoEnabledNativeAd) {
        FacebookNativeViewHolder facebookNativeViewHolder = mViewHolderMap.get(view);
        if (facebookNativeViewHolder == null) {
            facebookNativeViewHolder = FacebookNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, facebookNativeViewHolder);
        }

        update(facebookNativeViewHolder, facebookVideoEnabledNativeAd);
        NativeRendererHelper.updateExtras(facebookNativeViewHolder.getMainView(),
                mViewBinder.extras,
                facebookVideoEnabledNativeAd.getExtras());
        setViewVisibility(facebookNativeViewHolder, VISIBLE);
    }

    @Override
    public boolean supports(final BaseNativeAd nativeAd) {
        Preconditions.checkNotNull(nativeAd);
        return nativeAd instanceof FacebookNative.FacebookVideoEnabledNativeAd;
    }

    private void update(final FacebookNativeViewHolder facebookNativeViewHolder,
            final FacebookNative.FacebookVideoEnabledNativeAd nativeAd) {
        final ImageView mainImageView = facebookNativeViewHolder.getMainImageView();
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTitleView(),
                nativeAd.getTitle());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getTextView(), nativeAd.getText());
        NativeRendererHelper.addTextView(facebookNativeViewHolder.getCallToActionView(),
                nativeAd.getCallToAction());
        NativeImageHelper.loadImageView(nativeAd.getMainImageUrl(), mainImageView);
        NativeImageHelper.loadImageView(nativeAd.getIconImageUrl(),
                facebookNativeViewHolder.getIconImageView());
        NativeRendererHelper.addPrivacyInformationIcon(
                facebookNativeViewHolder.getPrivacyInformationIconImageView(),
                nativeAd.getPrivacyInformationIconImageUrl(),
                nativeAd.getPrivacyInformationIconClickThroughUrl());
        final MediaView mediaView = facebookNativeViewHolder.getMediaView();
        if (mediaView != null && mainImageView != null) {
            nativeAd.updateMediaView(mediaView);
            mediaView.setVisibility(View.VISIBLE);
            if (facebookNativeViewHolder.isMainImageViewInRelativeView()) {
                mainImageView.setVisibility(View.INVISIBLE);
            } else {
                mainImageView.setVisibility(View.GONE);
            }

        }
    }

    private static void setViewVisibility(final FacebookNativeViewHolder facebookNativeViewHolder,
            final int visibility) {
        if (facebookNativeViewHolder.getMainView() != null) {
            facebookNativeViewHolder.getMainView().setVisibility(visibility);
        }
    }

    static class FacebookNativeViewHolder {
        private final StaticNativeViewHolder mStaticNativeViewHolder;
        private final MediaView mMediaView;
        private final boolean isMainImageViewInRelativeView;

        // Use fromViewBinder instead of a constructor
        private FacebookNativeViewHolder(final StaticNativeViewHolder staticNativeViewHolder,
                final MediaView mediaView, final boolean mainImageViewInRelativeView) {
            mStaticNativeViewHolder = staticNativeViewHolder;
            mMediaView = mediaView;
            isMainImageViewInRelativeView = mainImageViewInRelativeView;
        }

        static FacebookNativeViewHolder fromViewBinder(final View view,
                final ViewBinder viewBinder) {
            StaticNativeViewHolder staticNativeViewHolder = StaticNativeViewHolder.fromViewBinder(view, viewBinder);
            final View mainImageView = staticNativeViewHolder.mainImageView;
            boolean mainImageViewInRelativeView = false;
            MediaView mediaView = null;
            if (mainImageView != null) {
                final ViewGroup mainImageParent = (ViewGroup) mainImageView.getParent();
                if (mainImageParent instanceof RelativeLayout) {
                    mainImageViewInRelativeView = true;
                }
                final int mainImageIndex = mainImageParent.indexOfChild(mainImageView);
                final View viewAfterImageView = mainImageParent.getChildAt(mainImageIndex + 1);
                if (viewAfterImageView instanceof MediaView) {
                    mediaView = (MediaView) viewAfterImageView;
                }
            }
            return new FacebookNativeViewHolder(staticNativeViewHolder, mediaView, mainImageViewInRelativeView);
        }

        public View getMainView() {
            return mStaticNativeViewHolder.mainView;
        }

        public TextView getTitleView() {
            return mStaticNativeViewHolder.titleView;
        }

        public TextView getTextView() {
            return mStaticNativeViewHolder.textView;
        }

        public TextView getCallToActionView() {
            return mStaticNativeViewHolder.callToActionView;
        }

        public ImageView getMainImageView() {
            return mStaticNativeViewHolder.mainImageView;
        }

        public ImageView getIconImageView() {
            return mStaticNativeViewHolder.iconImageView;
        }

        public ImageView getPrivacyInformationIconImageView() {
            return mStaticNativeViewHolder.privacyInformationIconImageView;
        }

        public MediaView getMediaView() {
            return mMediaView;
        }

        public boolean isMainImageViewInRelativeView() {
            return isMainImageViewInRelativeView;
        }
    }
}
