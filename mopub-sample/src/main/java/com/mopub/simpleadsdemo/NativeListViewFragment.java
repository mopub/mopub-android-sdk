package com.mopub.simpleadsdemo;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mopub.nativeads.MoPubAdAdapter;
import com.mopub.nativeads.MoPubNativeAdPositioning;
import com.mopub.nativeads.MoPubNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;

import static com.mopub.nativeads.RequestParameters.NativeAdAsset;

public class NativeListViewFragment extends Fragment {
    private MoPubAdAdapter mAdAdapter;
    private MoPubSampleAdUnit mAdConfiguration;
    private RequestParameters mRequestParameters;

    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.native_list_view_fragment, container, false);
        final ListView listView = (ListView) view.findViewById(R.id.native_list_view);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If your app already has location access, include it here.
                final Location location = null;
                final String keywords = views.mKeywordsField.getText().toString();

                // Setting desired assets on your request helps native ad networks and bidders
                // provide higher-quality ads.
                final EnumSet<NativeAdAsset> desiredAssets = EnumSet.of(
                        NativeAdAsset.TITLE,
                        NativeAdAsset.TEXT,
                        NativeAdAsset.ICON_IMAGE,
                        NativeAdAsset.MAIN_IMAGE,
                        NativeAdAsset.CALL_TO_ACTION_TEXT);

                mRequestParameters = new RequestParameters.Builder()
                        .location(location)
                        .keywords(keywords)
                        .desiredAssets(desiredAssets)
                        .build();

                mAdAdapter.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
            }
        });
        final String adUnitId = mAdConfiguration.getAdUnitId();
        views.mDescriptionView.setText(mAdConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1);
        for (int i = 0; i < 100; ++i) {
            adapter.add("Item " + i);
        }

        // Create an ad adapter with ads in positions 0, 4, and every 10 places thereafter.
        // This adapter will be used in place of the original adapter for the ListView.
        mAdAdapter = new MoPubAdAdapter(getActivity(), adapter, MoPubNativeAdPositioning.clientPositioning()
                .addFixedPosition(0)
                .addFixedPosition(4)
                .enableRepeatingPositions(10));

        // Set up an renderer that knows how to put ad data in an ad view.
        final MoPubNativeAdRenderer adRenderer = new MoPubNativeAdRenderer(
                new ViewBinder.Builder(R.layout.native_ad_list_item)
                        .titleId(R.id.native_title)
                        .textId(R.id.native_text)
                        .mainImageId(R.id.native_main_image)
                        .iconImageId(R.id.native_icon_image)
                        .callToActionId(R.id.native_cta)
                        .build());

        // Register the renderer with the MoPubAdAdapter and then set the adapter on the ListView.
        mAdAdapter.registerAdRenderer(adRenderer);
        listView.setAdapter(mAdAdapter);
        return view;
    }

    @Override
    public void onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        mAdAdapter.destroy();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        // MoPub recommends loading knew ads when the user returns to your activity.
        mAdAdapter.loadAds(mAdConfiguration.getAdUnitId(), mRequestParameters);
        super.onResume();
    }
}
