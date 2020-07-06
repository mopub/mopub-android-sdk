// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mopub.nativeads.AdapterHelper;
import com.mopub.nativeads.FacebookAdRenderer;
import com.mopub.nativeads.GooglePlayServicesAdRenderer;
import com.mopub.nativeads.MediaViewBinder;
import com.mopub.nativeads.MintegralAdRenderer;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.MoPubVideoNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.VerizonNativeAdRenderer;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;

import static com.mopub.simpleadsdemo.Utils.logToast;

public class NativeManualFragment extends Fragment {
    private MoPubSampleAdUnit mAdConfiguration;
    private MoPubNative mMoPubNative;
    private LinearLayout mAdContainer;
    @Nullable private RequestParameters mRequestParameters;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final View view = inflater.inflate(R.layout.native_manual_fragment, container, false);
        final DetailFragmentViewHolder viewHolder = DetailFragmentViewHolder.fromView(view);

        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        mAdContainer = view.findViewById(R.id.parent_view);

        viewHolder.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                updateRequestParameters(viewHolder);

                if (mAdContainer != null) {
                    mAdContainer.removeAllViews();
                }

                if (mMoPubNative != null) {
                    mMoPubNative.makeRequest(mRequestParameters);
                } else {
                    logToast(getActivity(), getName() + " failed to load. MoPubNative instance is null.");
                }
            }
        });

        String adUnitId = null;

        if (mAdConfiguration != null) {
            adUnitId = mAdConfiguration.getAdUnitId();
        }

        viewHolder.mDescriptionView.setText(mAdConfiguration.getDescription());
        viewHolder.mAdUnitIdView.setText(adUnitId);
        viewHolder.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        viewHolder.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        updateRequestParameters(viewHolder);

        mMoPubNative = new MoPubNative(getContext(), adUnitId, new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                NativeAd.MoPubNativeEventListener moPubNativeEventListener = new NativeAd.MoPubNativeEventListener() {
                    @Override
                    public void onImpression(View view) {
                        // The ad has registered an impression. You may call any app logic that
                        // depends on having the ad view shown.
                        logToast(getActivity(), getName() + " impressed.");
                    }

                    @Override
                    public void onClick(View view) {
                        logToast(getActivity(), getName() + " clicked.");
                    }
                };


                // In a manual integration, any interval that is at least 2 is acceptable
                final AdapterHelper adapterHelper = new AdapterHelper(getContext(), 0, 2);
                final View adView;

                adView = adapterHelper.getAdView(null, null, nativeAd, new ViewBinder.Builder(0).build());
                nativeAd.setMoPubNativeEventListener(moPubNativeEventListener);

                if (mAdContainer != null) {
                    mAdContainer.addView(adView);
                } else {
                    logToast(getActivity(), getName() + " failed to show. Ad container is null.");
                }
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                logToast(getActivity(), getName() + " failed to load: " + errorCode.toString());
            }
        });

        MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer = new MoPubStaticNativeAdRenderer(
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
        MoPubVideoNativeAdRenderer moPubVideoNativeAdRenderer = new MoPubVideoNativeAdRenderer(
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

        // The first renderer that can handle a particular native ad gets used.
        // We are prioritizing network renderers.

        mMoPubNative.registerAdRenderer(mintegralAdRenderer);
        mMoPubNative.registerAdRenderer(facebookAdRenderer);
        mMoPubNative.registerAdRenderer(googlePlayServicesAdRenderer);
        mMoPubNative.registerAdRenderer(verizonNativeAdRenderer);
        mMoPubNative.registerAdRenderer(moPubStaticNativeAdRenderer);
        mMoPubNative.registerAdRenderer(moPubVideoNativeAdRenderer);

        mMoPubNative.makeRequest(mRequestParameters);

        return view;
    }

    private void updateRequestParameters(@NonNull final DetailFragmentViewHolder views) {
        final String keywords = views.mKeywordsField.getText().toString();
        final String userDataKeywords = views.mUserDataKeywordsField.getText().toString();

        // Setting desired assets on your request helps native ad networks and bidders
        // provide higher-quality ads.
        final EnumSet<RequestParameters.NativeAdAsset> desiredAssets = EnumSet.of(
                RequestParameters.NativeAdAsset.TITLE,
                RequestParameters.NativeAdAsset.TEXT,
                RequestParameters.NativeAdAsset.ICON_IMAGE,
                RequestParameters.NativeAdAsset.MAIN_IMAGE,
                RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT,
                RequestParameters.NativeAdAsset.SPONSORED);

        mRequestParameters = new RequestParameters.Builder()
                .keywords(keywords)
                .userDataKeywords(userDataKeywords)
                .desiredAssets(desiredAssets)
                .build();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMoPubNative != null) {
            mMoPubNative.destroy();
            mMoPubNative = null;
        }

        if (mAdContainer != null) {
            mAdContainer.removeAllViews();
            mAdContainer = null;
        }
    }

    private String getName() {
        if (mAdConfiguration == null || TextUtils.isEmpty(mAdConfiguration.getHeaderName())) {
            return MoPubSampleAdUnit.AdType.MANUAL_NATIVE.getName();
        }

        return mAdConfiguration.getHeaderName();
    }
}
