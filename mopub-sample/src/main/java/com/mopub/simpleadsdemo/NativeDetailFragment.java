package com.mopub.simpleadsdemo;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.nativeads.AdapterHelper;
import com.mopub.nativeads.GooglePlayServicesAdRenderer;
import com.mopub.nativeads.MediaViewBinder;
import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubNativeAdLoadedListener;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.MoPubStreamAdPlacer;
import com.mopub.nativeads.MoPubVideoNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;

import static com.mopub.nativeads.RequestParameters.NativeAdAsset;

public class NativeDetailFragment extends Fragment {
    private MoPubSampleAdUnit mAdConfiguration;
    private MoPubNative moPubNative;

    private AdapterHelper adapterHelper;
    private RequestParameters mRequestParameters;
    private MoPubNative.MoPubNativeNetworkListener moPubNativeNetworkListener;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mAdConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.native_detail_fragment, container, false);
        final RelativeLayout nativeAdView = (RelativeLayout) view.findViewById(R.id.native_ad_view);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If your app already has location access, include it here.
                final Location location = null;
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
                        NativeAdAsset.STAR_RATING
                );

                mRequestParameters = new RequestParameters.Builder()
                        .location(location)
                        .keywords(keywords)
                        .userDataKeywords(userDataKeywords)
                        .desiredAssets(desiredAssets)
                        .build();

                moPubNative.makeRequest(mRequestParameters);

            }
        });

        final String adUnitId = mAdConfiguration.getAdUnitId();
        views.mDescriptionView.setText(mAdConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));

        moPubNative = new MoPubNative(getActivity(), mAdConfiguration.getAdUnitId(), new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(final NativeAd nativeAd) {
                Log.d("MoPub", "Native ad has loaded.");
                // Set the native event listeners (onImpression, and onClick).
                nativeAd.setMoPubNativeEventListener(new NativeAd.MoPubNativeEventListener() {
                    @Override
                    public void onImpression(View view) {
                        Log.d("MoPub", "Native ad recorded an impression.");
                    }

                    @Override
                    public void onClick(View view) {
                        Log.d("MoPub", "Native ad recorded a click.");
                    }
                });

                View adView = adapterHelper.getAdView(null, null, nativeAd, new ViewBinder.Builder(0).build());
                nativeAdView.addView(adView);
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                Log.d("MoPub", "Native ad failed to load with error: " + errorCode.toString());
            }
        });

        ViewBinder staticViewBinder = new ViewBinder.Builder(R.layout.native_ad_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mainImageId(R.id.native_main_image)
                .iconImageId(R.id.native_icon_image)
                .callToActionId(R.id.native_cta)
                .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                .build();

        MediaViewBinder videoViewBinder = new MediaViewBinder.Builder(R.layout.video_ad_list_item)
                .titleId(R.id.native_title)
                .textId(R.id.native_text)
                .mediaLayoutId(R.id.native_media_layout)
                .iconImageId(R.id.native_icon_image)
                .callToActionId(R.id.native_cta)
                .privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
                .build();

        // Set up a renderer for a static native ad.
        final MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer = new MoPubStaticNativeAdRenderer(staticViewBinder);

        // Set up a renderer for a video native ad.
        final MoPubVideoNativeAdRenderer moPubVideoNativeAdRenderer = new MoPubVideoNativeAdRenderer(videoViewBinder);

        final GooglePlayServicesAdRenderer googlePlayServicesAdRenderer = new GooglePlayServicesAdRenderer(staticViewBinder);

//        final FacebookAdRenderer facebookAdRenderer = new FacebookAdRenderer(staticViewBinder);

        moPubNative.registerAdRenderer(moPubStaticNativeAdRenderer);
        moPubNative.registerAdRenderer(moPubVideoNativeAdRenderer);
        moPubNative.registerAdRenderer(googlePlayServicesAdRenderer);
//        moPubNative.registerAdRenderer(facebookAdRenderer);

        adapterHelper = new AdapterHelper(getActivity(), 0, 3); // When standalone, any range will be fine.

        return view;
    }

    @Override
    public void onDestroyView() {
        // You must call this or the ad adapter may cause a memory leak.
        moPubNative.destroy();
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        // MoPub recommends reloading ads when the user returns to a view.
        super.onResume();
    }

}
