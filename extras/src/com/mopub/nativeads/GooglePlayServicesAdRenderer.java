package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.formats.AdChoicesView;
import com.google.android.gms.ads.formats.NativeAdView;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.GooglePlayServicesNative.GooglePlayServicesNativeAd;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The {@link GooglePlayServicesAdRenderer} class is used to render
 * GooglePlayServicesStaticNativeAds.
 *
 * Compatible with version 11.4.0 of the Google Play Services SDK.
 */
public class GooglePlayServicesAdRenderer implements MoPubAdRenderer<GooglePlayServicesNativeAd> {

    /**
     * Key to set and get star rating text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_STAR_RATING = "key_star_rating";

    /**
     * Key to set and get advertiser text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_ADVERTISER = "key_advertiser";

    /**
     * Key to set and get store text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_STORE = "key_store";

    /**
     * Key to set and get price text view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_PRICE = "key_price";

    /**
     * Key to set and get the AdChoices icon view as an extra in the view binder.
     */
    public static final String VIEW_BINDER_KEY_AD_CHOICES_ICON_CONTAINER = "ad_choices_container";

    /**
     * ID for the frame layout that wraps the Google ad view.
     */
    @IdRes
    private static final int ID_WRAPPING_FRAME = 1001;

    /**
     * ID for the Google native ad view.
     */
    @IdRes
    private static final int ID_GOOGLE_NATIVE_VIEW = 1002;

    /**
     * A view binder containing the layout resource and views to be rendered by the renderer.
     */
    private final ViewBinder mViewBinder;

    /**
     * A weak hash map used to keep track of view holder so that the views can be properly recycled.
     */
    private final WeakHashMap<View, GoogleStaticNativeViewHolder> mViewHolderMap;

    public GooglePlayServicesAdRenderer(ViewBinder viewBinder) {
        this.mViewBinder = viewBinder;
        this.mViewHolderMap = new WeakHashMap<>();
    }

    @NonNull
    @Override
    public View createAdView(@NonNull Context context, @Nullable ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(mViewBinder.layoutId, parent, false);
        // Create a frame layout and add the inflated view as a child. This will allow us to add
        // the Google native ad view into the view hierarchy at render time.
        FrameLayout wrappingView = new FrameLayout(context);
        wrappingView.setId(ID_WRAPPING_FRAME);
        wrappingView.addView(view);
        Log.i(GooglePlayServicesNative.TAG, "Ad view created.");
        return wrappingView;
    }

    @Override
    public void renderAdView(@NonNull View view,
            @NonNull GooglePlayServicesNativeAd nativeAd) {
        GoogleStaticNativeViewHolder viewHolder = mViewHolderMap.get(view);
        if (viewHolder == null) {
            viewHolder = GoogleStaticNativeViewHolder.fromViewBinder(view, mViewBinder);
            mViewHolderMap.put(view, viewHolder);
        }

        removeGoogleNativeAdView(view, nativeAd.shouldSwapMargins());

        NativeAdView nativeAdView = null;
        if (nativeAd.isNativeAppInstallAd()) {
            nativeAdView = new NativeAppInstallAdView(view.getContext());
            updateAppInstallAdView(nativeAd, viewHolder, (NativeAppInstallAdView) nativeAdView);
        } else if (nativeAd.isNativeContentAd()) {
            nativeAdView = new NativeContentAdView(view.getContext());
            updateContentAdView(nativeAd, viewHolder, (NativeContentAdView) nativeAdView);
        }

        if (nativeAdView != null) {
            insertGoogleNativeAdView(nativeAdView, view, nativeAd.shouldSwapMargins());
        } else {
            Log.w(GooglePlayServicesNative.TAG,
                    "Couldn't add Google native ad view. NativeAdView is null.");
        }
    }

    /**
     * This method will add the given Google native ad view into the view hierarchy of the given
     * MoPub native ad view.
     *
     * @param googleNativeAdView Google's native ad view to be added as a parent to the MoPub's
     *                           view.
     * @param moPubNativeAdView  MoPub's native ad view created by this renderer.
     * @param swapMargins        {@code true} if the margins need to be swapped, {@code false}
     *                           otherwise.
     */
    private static void insertGoogleNativeAdView(NativeAdView googleNativeAdView,
            View moPubNativeAdView,
            boolean swapMargins) {
        if (moPubNativeAdView instanceof FrameLayout
                && moPubNativeAdView.getId() == ID_WRAPPING_FRAME) {
            googleNativeAdView.setId(ID_GOOGLE_NATIVE_VIEW);
            FrameLayout outerFrame = (FrameLayout) moPubNativeAdView;
            View actualView = outerFrame.getChildAt(0);

            if (swapMargins) {
                // Google native ad view renders the AdChoices icon in one of the four corners of
                // its view. If a margin is specified on the actual ad view, the AdChoices view
                // might be rendered outside the actual ad view. Moving the margins from the
                // actual ad view to Google native ad view will make sure that the AdChoices icon
                // is being rendered within the bounds of the actual ad view.
                FrameLayout.LayoutParams googleNativeAdViewParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                FrameLayout.LayoutParams actualViewParams =
                        (FrameLayout.LayoutParams) actualView.getLayoutParams();
                googleNativeAdViewParams.setMargins(actualViewParams.leftMargin,
                        actualViewParams.topMargin,
                        actualViewParams.rightMargin,
                        actualViewParams.bottomMargin);
                googleNativeAdView.setLayoutParams(googleNativeAdViewParams);
                actualViewParams.setMargins(0, 0, 0, 0);
            } else {
                googleNativeAdView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            outerFrame.removeView(actualView);
            googleNativeAdView.addView(actualView);
            outerFrame.addView(googleNativeAdView);
        } else {
            Log.w(GooglePlayServicesNative.TAG,
                    "Couldn't add Google native ad view. Wrapping view not found.");
        }
    }

    /**
     * This method will remove the Google native ad view from the view hierarchy if one is present.
     *
     * @param view        the view from which to remove the Google native ad view.
     * @param swapMargins {@code true} if the margins need to be swapped before removing the
     *                    Google native ad view, {@code false} otherwise.
     */
    protected static void removeGoogleNativeAdView(@NonNull View view, boolean swapMargins) {
        if (view instanceof FrameLayout && view.getId() == ID_WRAPPING_FRAME) {
            View adView = view.findViewById(ID_GOOGLE_NATIVE_VIEW);
            if (adView != null) {
                ViewGroup outerView = (ViewGroup) view;
                int index = outerView.indexOfChild(adView);
                outerView.removeView(adView);
                View actualNativeView = ((ViewGroup) adView).getChildAt(0);
                if (actualNativeView != null) {
                    if (swapMargins) {
                        FrameLayout.LayoutParams actualViewParams =
                                (FrameLayout.LayoutParams) actualNativeView.getLayoutParams();
                        FrameLayout.LayoutParams googleNativeViewParams =
                                (FrameLayout.LayoutParams) adView.getLayoutParams();
                        actualViewParams.setMargins(
                                googleNativeViewParams.leftMargin,
                                googleNativeViewParams.topMargin,
                                googleNativeViewParams.rightMargin,
                                googleNativeViewParams.bottomMargin);
                    }
                    ((ViewGroup) adView).removeView(actualNativeView);
                    outerView.addView(actualNativeView, index);
                }

                if (adView instanceof NativeAdView) {
                    ((NativeAdView) adView).destroy();
                }
            }
        }
    }

    /**
     * This method will render the given native ad view using the native ad and set the views to
     * Google's native content ad view.
     *
     * @param staticNativeAd         a static native ad object containing the required assets to
     *                               set to the native ad view.
     * @param staticNativeViewHolder a static native view holder object containing the mapped
     *                               views from the view binder.
     * @param contentAdView          the Google native content ad view in the view hierarchy.
     */
    private void updateContentAdView(GooglePlayServicesNativeAd staticNativeAd,
            GoogleStaticNativeViewHolder staticNativeViewHolder,
            NativeContentAdView contentAdView) {
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTitleView, staticNativeAd.getTitle());
        contentAdView.setHeadlineView(staticNativeViewHolder.mTitleView);
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTextView, staticNativeAd.getText());
        contentAdView.setBodyView(staticNativeViewHolder.mTextView);
        NativeRendererHelper.addTextView(staticNativeViewHolder.mCallToActionView,
                staticNativeAd.getCallToAction());
        contentAdView.setCallToActionView(staticNativeViewHolder.mCallToActionView);
        NativeImageHelper.loadImageView(staticNativeAd.getMainImageUrl(),
                staticNativeViewHolder.mMainImageView);
        contentAdView.setImageView(staticNativeViewHolder.mMainImageView);
        NativeImageHelper.loadImageView(staticNativeAd.getIconImageUrl(),
                staticNativeViewHolder.mIconImageView);
        contentAdView.setLogoView(staticNativeViewHolder.mIconImageView);
        if (staticNativeAd.getAdvertiser() != null) {
            NativeRendererHelper.addTextView(
                    staticNativeViewHolder.mAdvertiserTextView, staticNativeAd.getAdvertiser());
            contentAdView.setAdvertiserView(staticNativeViewHolder.mAdvertiserTextView);
        }

        // Add the AdChoices icon to the container if one is provided by the publisher.
        if (staticNativeViewHolder.mAdChoicesIconContainer != null) {
            AdChoicesView adChoicesView = new AdChoicesView(contentAdView.getContext());
            staticNativeViewHolder.mAdChoicesIconContainer.removeAllViews();
            staticNativeViewHolder.mAdChoicesIconContainer.addView(adChoicesView);
            contentAdView.setAdChoicesView(adChoicesView);
        }

        // Set the privacy information icon to null as the Google Mobile Ads SDK automatically
        // renders the AdChoices icon.
        NativeRendererHelper.addPrivacyInformationIcon(
                staticNativeViewHolder.mPrivacyInformationIconImageView, null, null);

        contentAdView.setNativeAd(staticNativeAd.getContentAd());
    }

    /**
     * This method will render the given native ad view using the native ad and set the views to
     * Google's native app install ad view.
     *
     * @param staticNativeAd         a static native ad object containing the required assets to set
     *                               to the native ad view.
     * @param staticNativeViewHolder a static native view holder object containing the mapped
     *                               views from the view binder.
     * @param appInstallAdView       the Google native app install ad view in the view hierarchy.
     */
    private void updateAppInstallAdView(GooglePlayServicesNativeAd staticNativeAd,
            GoogleStaticNativeViewHolder staticNativeViewHolder,
            NativeAppInstallAdView appInstallAdView) {

        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTitleView, staticNativeAd.getTitle());
        appInstallAdView.setHeadlineView(staticNativeViewHolder.mTitleView);
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mTextView, staticNativeAd.getText());
        appInstallAdView.setBodyView(staticNativeViewHolder.mTextView);
        NativeRendererHelper.addTextView(
                staticNativeViewHolder.mCallToActionView, staticNativeAd.getCallToAction());
        appInstallAdView.setCallToActionView(staticNativeViewHolder.mCallToActionView);
        NativeImageHelper.loadImageView(
                staticNativeAd.getMainImageUrl(), staticNativeViewHolder.mMainImageView);
        appInstallAdView.setImageView(staticNativeViewHolder.mMainImageView);
        NativeImageHelper.loadImageView(
                staticNativeAd.getIconImageUrl(), staticNativeViewHolder.mIconImageView);
        appInstallAdView.setIconView(staticNativeViewHolder.mIconImageView);
        if (staticNativeAd.getStarRating() != null) {
            NativeRendererHelper.addTextView(staticNativeViewHolder.mStarRatingTextView,
                    String.format(
                            Locale.getDefault(), "%.1f/5 Stars", staticNativeAd.getStarRating()));
            appInstallAdView.setStarRatingView(staticNativeViewHolder.mStarRatingTextView);
        }
        if (staticNativeAd.getPrice() != null) {
            NativeRendererHelper.addTextView(
                    staticNativeViewHolder.mPriceTextView, staticNativeAd.getPrice());
            appInstallAdView.setPriceView(staticNativeViewHolder.mPriceTextView);
        }
        if (staticNativeAd.getStore() != null) {
            NativeRendererHelper.addTextView(
                    staticNativeViewHolder.mStoreTextView, staticNativeAd.getStore());
            appInstallAdView.setStoreView(staticNativeViewHolder.mStoreTextView);
        }

        // Set the privacy information icon to null as the Google Mobile Ads SDK automatically
        // renders the AdChoices icon.
        NativeRendererHelper.addPrivacyInformationIcon(
                staticNativeViewHolder.mPrivacyInformationIconImageView, null, null);

        // Add the AdChoices icon to the container if one is provided by the publisher.
        if (staticNativeViewHolder.mAdChoicesIconContainer != null) {
            AdChoicesView adChoicesView = new AdChoicesView(appInstallAdView.getContext());
            staticNativeViewHolder.mAdChoicesIconContainer.removeAllViews();
            staticNativeViewHolder.mAdChoicesIconContainer.addView(adChoicesView);
            appInstallAdView.setAdChoicesView(adChoicesView);
        }

        appInstallAdView.setNativeAd(staticNativeAd.getAppInstallAd());
    }

    @Override
    public boolean supports(@NonNull BaseNativeAd nativeAd) {
        return nativeAd instanceof GooglePlayServicesNativeAd;
    }

    private static class GoogleStaticNativeViewHolder {
        @Nullable
        View mMainView;
        @Nullable
        TextView mTitleView;
        @Nullable
        TextView mTextView;
        @Nullable
        TextView mCallToActionView;
        @Nullable
        ImageView mMainImageView;
        @Nullable
        ImageView mIconImageView;
        @Nullable
        ImageView mPrivacyInformationIconImageView;
        @Nullable
        TextView mStarRatingTextView;
        @Nullable
        TextView mAdvertiserTextView;
        @Nullable
        TextView mStoreTextView;
        @Nullable
        TextView mPriceTextView;
        @Nullable
        FrameLayout mAdChoicesIconContainer;

        private static final GoogleStaticNativeViewHolder EMPTY_VIEW_HOLDER =
                new GoogleStaticNativeViewHolder();

        @NonNull
        public static GoogleStaticNativeViewHolder fromViewBinder(@NonNull View view,
                @NonNull ViewBinder viewBinder) {
            final GoogleStaticNativeViewHolder viewHolder = new GoogleStaticNativeViewHolder();
            viewHolder.mMainView = view;
            try {
                viewHolder.mTitleView = (TextView) view.findViewById(viewBinder.titleId);
                viewHolder.mTextView = (TextView) view.findViewById(viewBinder.textId);
                viewHolder.mCallToActionView =
                        (TextView) view.findViewById(viewBinder.callToActionId);
                viewHolder.mMainImageView =
                        (ImageView) view.findViewById(viewBinder.mainImageId);
                viewHolder.mIconImageView =
                        (ImageView) view.findViewById(viewBinder.iconImageId);
                viewHolder.mPrivacyInformationIconImageView =
                        (ImageView) view.findViewById(viewBinder.privacyInformationIconImageId);
                Map<String, Integer> extraViews = viewBinder.extras;
                Integer starRatingTextViewId = extraViews.get(VIEW_BINDER_KEY_STAR_RATING);
                if (starRatingTextViewId != null) {
                    viewHolder.mStarRatingTextView =
                            (TextView) view.findViewById(starRatingTextViewId);
                }
                Integer advertiserTextViewId = extraViews.get(VIEW_BINDER_KEY_ADVERTISER);
                if (advertiserTextViewId != null) {
                    viewHolder.mAdvertiserTextView =
                            (TextView) view.findViewById(advertiserTextViewId);
                }
                Integer storeTextViewId = extraViews.get(VIEW_BINDER_KEY_STORE);
                if (storeTextViewId != null) {
                    viewHolder.mStoreTextView = (TextView) view.findViewById(storeTextViewId);
                }
                Integer priceTextViewId = extraViews.get(VIEW_BINDER_KEY_PRICE);
                if (priceTextViewId != null) {
                    viewHolder.mPriceTextView = (TextView) view.findViewById(priceTextViewId);
                }
                Integer adChoicesIconViewId =
                        extraViews.get(VIEW_BINDER_KEY_AD_CHOICES_ICON_CONTAINER);
                if (adChoicesIconViewId != null) {
                    viewHolder.mAdChoicesIconContainer =
                            (FrameLayout) view.findViewById(adChoicesIconViewId);
                }
                return viewHolder;
            } catch (ClassCastException exception) {
                MoPubLog.w("Could not cast from id in ViewBinder to expected View type", exception);
                return EMPTY_VIEW_HOLDER;
            }
        }
    }
}
