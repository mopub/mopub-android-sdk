// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.mopub.nativeads.FacebookAdRenderer;
import com.mopub.nativeads.GooglePlayServicesAdRenderer;
import com.mopub.nativeads.MediaViewBinder;
import com.mopub.nativeads.MintegralAdRenderer;
import com.mopub.nativeads.MoPubNativeAdLoadedListener;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.MoPubStreamAdPlacer;
import com.mopub.nativeads.MoPubVideoNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.VerizonNativeAdRenderer;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;

import static com.mopub.nativeads.RequestParameters.NativeAdAsset;

public class NativeGalleryFragment extends Fragment implements MoPubNativeAdLoadedListener {
    private MoPubSampleAdUnit mAdConfiguration;
    private ViewPager mViewPager;
    private CustomPagerAdapter mPagerAdapter;
    private MoPubStreamAdPlacer mStreamAdPlacer;
    private RequestParameters mRequestParameters;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.native_gallery_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateRequestParameters(views);

                if (mStreamAdPlacer != null) {
                    mStreamAdPlacer.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
                }
            }
        });

        final String adUnitId = mAdConfiguration.getAdUnitId();
        views.mDescriptionView.setText(mAdConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        mViewPager = (ViewPager) view.findViewById(R.id.gallery_pager);
        updateRequestParameters(views);

        // Set up a renderer for a static native ad.
        final MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer = new MoPubStaticNativeAdRenderer(
                new ViewBinder.Builder(R.layout.native_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mainImageId(R.id.native_main_image)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .sponsoredTextId(R.id.native_sponsored_text_view)
                        .build()
        );

        // Set up a renderer for a video native ad.
        final MoPubVideoNativeAdRenderer moPubVideoNativeAdRenderer = new MoPubVideoNativeAdRenderer(
                new MediaViewBinder.Builder(R.layout.video_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaLayoutId(R.id.native_media_layout)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .sponsoredTextId(R.id.native_sponsored_text_view)
                        .build());

        // Set up a renderer for Facebook video ads.
        final FacebookAdRenderer facebookAdRenderer = new FacebookAdRenderer(
                new FacebookAdRenderer.FacebookViewBinder.Builder(R.layout.native_ad_fan_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaViewId(R.id.native_media_view)
                        .adIconViewId(R.id.native_icon)
                        .callToActionId(R.id.native_cta)
                        .adChoicesRelativeLayoutId(R.id.native_privacy_information_icon_layout)
                        .build());

        // Set up a renderer for AdMob ads.
        final GooglePlayServicesAdRenderer googlePlayServicesAdRenderer = new GooglePlayServicesAdRenderer(
                new MediaViewBinder.Builder(R.layout.video_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaLayoutId(R.id.native_media_layout)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .build());

        // Set up a renderer for Verizon ads.
        final VerizonNativeAdRenderer verizonNativeAdRenderer = new VerizonNativeAdRenderer(
                new ViewBinder.Builder(R.layout.native_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mainImageId(R.id.native_main_image)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                        .build());

        // Set up a renderer for Mintegral ads.
        final MintegralAdRenderer mintegralAdRenderer = new MintegralAdRenderer(
                new MintegralAdRenderer.MintegralViewBinder.Builder(R.layout.native_ad_mintegral_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mediaViewId(R.id.native_main_image)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .adChoicesId(R.id.native_privacy_information_icon_image)
                        .build());

        // This ad placer is used to automatically insert ads into the ViewPager.
        mStreamAdPlacer = new MoPubStreamAdPlacer(getActivity());

        // The first renderer that can handle a particular native ad gets used.
        // We are prioritizing network renderers.
        mStreamAdPlacer.registerAdRenderer(mintegralAdRenderer);
        mStreamAdPlacer.registerAdRenderer(verizonNativeAdRenderer);
        mStreamAdPlacer.registerAdRenderer(googlePlayServicesAdRenderer);
        mStreamAdPlacer.registerAdRenderer(facebookAdRenderer);
        mStreamAdPlacer.registerAdRenderer(moPubStaticNativeAdRenderer);
        mStreamAdPlacer.registerAdRenderer(moPubVideoNativeAdRenderer);
        mStreamAdPlacer.setAdLoadedListener(this);

        mPagerAdapter = new CustomPagerAdapter(getChildFragmentManager(), mStreamAdPlacer);
        mViewPager.setAdapter(mPagerAdapter);

        return view;
    }

    public MoPubStreamAdPlacer getAdPlacer() {
        return mStreamAdPlacer;
    }

    private void updateRequestParameters(@NonNull final DetailFragmentViewHolder views) {
        final String keywords = views.mKeywordsField.getText().toString();
        final String userDataKeywords = views.mUserDataKeywordsField.getText().toString();

        // Setting desired assets on your request helps native ad networks and bidders
        // provide higher-quality ads.
        final EnumSet<NativeAdAsset> desiredAssets = EnumSet.of(
                NativeAdAsset.TITLE,
                NativeAdAsset.TEXT,
                NativeAdAsset.ICON_IMAGE,
                NativeAdAsset.MAIN_IMAGE,
                NativeAdAsset.CALL_TO_ACTION_TEXT,
                NativeAdAsset.SPONSORED);

        mRequestParameters = new RequestParameters.Builder()
                .keywords(keywords)
                .userDataKeywords(userDataKeywords)
                .desiredAssets(desiredAssets)
                .build();
    }

    @Override
    public void onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        mStreamAdPlacer.destroy();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        // MoPub recommends reloading ads when the user returns to a view.
        mStreamAdPlacer.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
        super.onResume();
    }

    @Override
    public void onAdLoaded(final int position) {
        mViewPager.invalidate();
        mPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAdRemoved(final int position) {
        mViewPager.invalidate();
        mPagerAdapter.notifyDataSetChanged();
    }

    private static class CustomPagerAdapter extends FragmentStatePagerAdapter {
        private static final int ITEM_COUNT = 30;
        private MoPubStreamAdPlacer mStreamAdPlacer;

        public CustomPagerAdapter(final FragmentManager fragmentManager,
                MoPubStreamAdPlacer streamAdPlacer) {
            super(fragmentManager);
            this.mStreamAdPlacer = streamAdPlacer;
            streamAdPlacer.setItemCount(ITEM_COUNT);
        }

        @Override
        public int getItemPosition(final Object object) {
            // This forces all items to be recreated when invalidate() is called on the ViewPager.
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(final int i) {
            mStreamAdPlacer.placeAdsInRange(i - 5, i + 5);
            if (mStreamAdPlacer.isAd(i)) {
                return AdFragment.newInstance(i);
            }
            return ContentFragment.newInstance(mStreamAdPlacer.getOriginalPosition(i));
        }

        @Override
        public int getCount() {
            return mStreamAdPlacer.getAdjustedCount(ITEM_COUNT);
        }

        @Override
        public CharSequence getPageTitle(final int position) {
            if (mStreamAdPlacer.isAd(position)) {
                return "Advertisement";
            }
            return "Content Item " + mStreamAdPlacer.getOriginalPosition(position);
        }

    }

    public static class ContentFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static ContentFragment newInstance(int sectionNumber) {
            ContentFragment fragment = new ContentFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                final Bundle savedInstanceState) {
            // Inflate the view.
            View rootView = inflater.inflate(R.layout.native_gallery_content, container, false);
            int contentNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            TextView textView = (TextView) rootView.findViewById(R.id.native_gallery_content_text);
            textView.setText("Content Item " + contentNumber);
            return rootView;
        }
    }

    public static class AdFragment extends Fragment {
        private static final String ARG_AD_POSITION = "ad_position";
        private MoPubStreamAdPlacer mAdPlacer;

        public static AdFragment newInstance(int adPosition) {
            AdFragment fragment = new AdFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(ARG_AD_POSITION, adPosition);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public void onAttach(final Activity activity) {
            mAdPlacer = ((NativeGalleryFragment) getParentFragment()).getAdPlacer();
            super.onAttach(activity);
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                final Bundle savedInstanceState) {
            if (mAdPlacer != null) {
                int position = getArguments().getInt(ARG_AD_POSITION);
                mAdPlacer.placeAdsInRange(position - 5, position + 5);
                return mAdPlacer.getAdView(position, null, container);
            }

            return null;
        }
    }
}
