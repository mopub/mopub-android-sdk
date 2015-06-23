package com.mopub.simpleadsdemo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;

import java.util.Locale;
import java.util.Set;

import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

public class RewardedVideoDetailFragment extends Fragment implements MoPubRewardedVideoListener {

    private Button mShowButton;
    private static boolean rewardedVideoInitialized;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final MoPubSampleAdUnit adConfiguration =
                MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.interstitial_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        hideSoftKeyboard(views.mKeywordsField);

        if (!rewardedVideoInitialized) {
            MoPub.initializeRewardedVideo(getActivity());
            rewardedVideoInitialized = true;
        }
        MoPub.setRewardedVideoListener(this);

        final String adUnitId = adConfiguration.getAdUnitId();
        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoPub.loadRewardedVideo(adUnitId);
                mShowButton.setEnabled(false);
            }
        });
        mShowButton = (Button) view.findViewById(R.id.interstitial_show_button);
        mShowButton.setEnabled(false);
        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MoPub.showRewardedVideo(adUnitId);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // MoPubRewardedVideoListener implementation
    @Override
    public void onRewardedVideoLoadSuccess(@NonNull final String adUnitId) {
        mShowButton.setEnabled(true);
        logToast(getActivity(), "Rewarded video loaded.");
    }

    @Override
    public void onRewardedVideoLoadFailure(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        mShowButton.setEnabled(false);
        logToast(getActivity(), String.format(Locale.US, "Rewarded video failed to load: %s", errorCode.toString()));
    }

    @Override
    public void onRewardedVideoStarted(@NonNull final String adUnitId) {
        logToast(getActivity(), "Rewarded video started.");
        mShowButton.setEnabled(false);
    }

    @Override
    public void onRewardedVideoPlaybackError(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        logToast(getActivity(), String.format(Locale.US, "Rewarded video playback error: %s", errorCode.toString()));
        mShowButton.setEnabled(false);
    }

    @Override
    public void onRewardedVideoClosed(@NonNull final String adUnitId) {
        logToast(getActivity(), "Rewarded video closed.");
        mShowButton.setEnabled(false);
    }

    @Override
    public void onRewardedVideoCompleted(@NonNull final Set<String> adUnitIds, @NonNull final MoPubReward reward) {
        logToast(getActivity(),
                String.format(Locale.US,
                        "Rewarded video completed with reward  \"%d %s\"",
                        reward.getAmount(),
                        reward.getLabel()));
    }
}
