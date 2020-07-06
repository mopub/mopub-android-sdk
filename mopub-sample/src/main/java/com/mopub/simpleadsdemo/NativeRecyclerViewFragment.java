// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mopub.nativeads.FacebookAdRenderer;
import com.mopub.nativeads.GooglePlayServicesAdRenderer;
import com.mopub.nativeads.MediaViewBinder;
import com.mopub.nativeads.MintegralAdRenderer;
import com.mopub.nativeads.MoPubNativeAdPositioning;
import com.mopub.nativeads.MoPubRecyclerAdapter;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.MoPubVideoNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.VerizonNativeAdRenderer;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;
import java.util.Locale;

public class NativeRecyclerViewFragment extends Fragment {
    private MoPubRecyclerAdapter mRecyclerAdapter;
    private MoPubSampleAdUnit mAdConfiguration;
    private RequestParameters mRequestParameters;
    private enum LayoutType { LINEAR, GRID }
    private LayoutType mLayoutType;
    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.recycler_view_fragment, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.native_recycler_view);
        final DetailFragmentViewHolder viewHolder = DetailFragmentViewHolder.fromView(view);
        final Button switchButton = (Button) view.findViewById(R.id.switch_button);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                toggleRecyclerLayout();
            }
        });

        viewHolder.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                updateRequestParameters(viewHolder);

                if (mRecyclerAdapter != null) {
                    mRecyclerAdapter.refreshAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
                }
            }
        });
        final String adUnitId = mAdConfiguration.getAdUnitId();
        viewHolder.mDescriptionView.setText(mAdConfiguration.getDescription());
        viewHolder.mAdUnitIdView.setText(adUnitId);
        viewHolder.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        viewHolder.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        updateRequestParameters(viewHolder);

        final RecyclerView.Adapter originalAdapter = new DemoRecyclerAdapter();

        mRecyclerAdapter = new MoPubRecyclerAdapter(getActivity(), originalAdapter,
                new MoPubNativeAdPositioning.MoPubServerPositioning());

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
        mRecyclerAdapter.registerAdRenderer(mintegralAdRenderer);
        mRecyclerAdapter.registerAdRenderer(verizonNativeAdRenderer);
        mRecyclerAdapter.registerAdRenderer(googlePlayServicesAdRenderer);
        mRecyclerAdapter.registerAdRenderer(facebookAdRenderer);
        mRecyclerAdapter.registerAdRenderer(moPubStaticNativeAdRenderer);
        mRecyclerAdapter.registerAdRenderer(moPubVideoNativeAdRenderer);

        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mLayoutType = LayoutType.LINEAR;
        mRecyclerAdapter.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
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

    void toggleRecyclerLayout() {
        if (mLayoutType == LayoutType.LINEAR) {
            mLayoutType = LayoutType.GRID;
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        } else {
            mLayoutType = LayoutType.LINEAR;
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
    }

    @Override
    public void onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        mRecyclerAdapter.destroy();
        super.onDestroyView();
    }

    private static class DemoRecyclerAdapter extends RecyclerView.Adapter<DemoViewHolder> {
        private static final int ITEM_COUNT = 150;
        @Override
        public DemoViewHolder onCreateViewHolder(final ViewGroup parent,
                final int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new DemoViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final DemoViewHolder holder, final int position) {
            holder.textView.setText(String.format(Locale.US, "Content Item #%d", position));
        }

        @Override
        public long getItemId(final int position) {
            return (long) position;
        }

        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }
    }

    /**
     * A view holder for R.layout.simple_list_item_1
     */
    private static class DemoViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public DemoViewHolder(final View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
