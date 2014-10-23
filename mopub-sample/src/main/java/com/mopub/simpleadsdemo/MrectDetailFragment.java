package com.mopub.simpleadsdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import static com.mopub.mobileads.MoPubView.BannerAdListener;
import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

public class MrectDetailFragment extends Fragment implements BannerAdListener {
    private MoPubView mMoPubView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final MoPubSampleAdUnit adConfiguration =
                MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.mrect_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        hideSoftKeyboard(views.mKeywordsField);
        mMoPubView = (MoPubView) view.findViewById(R.id.mrect_mopubview);
        final String adUnitId = adConfiguration.getAdUnitId();
        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String keywords = views.mKeywordsField.getText().toString();
                loadMoPubView(adUnitId, keywords);
            }
        });
        mMoPubView.setBannerAdListener(this);
        loadMoPubView(adUnitId, null);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMoPubView != null) {
            mMoPubView.destroy();
            mMoPubView = null;
        }
    }

    private void loadMoPubView(final String adUnitId, final String keywords) {
        mMoPubView.setAdUnitId(adUnitId);
        mMoPubView.setKeywords(keywords);
        mMoPubView.loadAd();
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(MoPubView banner) {
        logToast(getActivity(), "Mrect loaded.");
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        logToast(getActivity(), "Mrect failed to load: " + errorMessage);
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        logToast(getActivity(), "Mrect clicked.");
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        logToast(getActivity(), "Mrect expanded.");
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        logToast(getActivity(), "Mrect collapsed.");
    }
}
