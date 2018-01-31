package com.mopub.simpleadsdemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import static com.mopub.mobileads.MoPubView.BannerAdListener;
import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

/**
 * A base class for creating banner style ads with various height and width dimensions.
 * <p>
 * A subclass simply needs to specify the height and width of the ad in pixels, and this class will
 * inflate a layout containing a programmatically rescaled {@link MoPubView} that will be used to
 * display the ad.
 */
public abstract class AbstractBannerDetailFragment extends Fragment implements BannerAdListener {
    private MoPubView mMoPubView;
    private MoPubSampleAdUnit mMoPubSampleAdUnit;

    public abstract int getWidth();

    public abstract int getHeight();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.banner_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);

        mMoPubSampleAdUnit = MoPubSampleAdUnit.fromBundle(getArguments());
        mMoPubView = (MoPubView) view.findViewById(R.id.banner_mopubview);
        LinearLayout.LayoutParams layoutParams =
                (LinearLayout.LayoutParams) mMoPubView.getLayoutParams();
        layoutParams.width = getWidth();
        layoutParams.height = getHeight();
        mMoPubView.setLayoutParams(layoutParams);

        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        hideSoftKeyboard(views.mKeywordsField);

        final String adUnitId = mMoPubSampleAdUnit.getAdUnitId();
        views.mDescriptionView.setText(mMoPubSampleAdUnit.getDescription());
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

    private String getName() {
        if (mMoPubSampleAdUnit == null) {
            return MoPubSampleAdUnit.AdType.BANNER.getName();
        }
        return mMoPubSampleAdUnit.getHeaderName();
    }

    // BannerAdListener
    @Override
    public void onBannerLoaded(MoPubView banner) {
        logToast(getActivity(), getName() + " loaded.");
    }

    @Override
    public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        logToast(getActivity(), getName() + " failed to load: " + errorMessage);
    }

    @Override
    public void onBannerClicked(MoPubView banner) {
        logToast(getActivity(), getName() + " clicked.");
    }

    @Override
    public void onBannerExpanded(MoPubView banner) {
        logToast(getActivity(), getName() + " expanded.");
    }

    @Override
    public void onBannerCollapsed(MoPubView banner) {
        logToast(getActivity(), getName() + " collapsed.");
    }
}
