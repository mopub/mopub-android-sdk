// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;

import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;
import static com.mopub.simpleadsdemo.Utils.hideSoftKeyboard;
import static com.mopub.simpleadsdemo.Utils.logToast;

public class InterstitialDetailFragment extends Fragment implements InterstitialAdListener {
    private MoPubInterstitial mMoPubInterstitial;
    private Button mShowButton;
    @Nullable private CallbacksAdapter mCallbacksAdapter;

    private enum InterstitialCallbacks {
        LOADED("onAdLoaded"),
        FAILED("onAdFailed"),
        SHOWN("onAdShown"),
        CLICKED("onAdClicked"),
        DISMISSED("onAdDismissed");

        InterstitialCallbacks(@NonNull final String name) {
            this.name = name;
        }

        @NonNull
        private final String name;

        @Override
        @NonNull
        public String toString() {
            return name;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final MoPubSampleAdUnit adConfiguration = MoPubSampleAdUnit.fromBundle(getArguments());
        final View view = inflater.inflate(R.layout.interstitial_detail_fragment, container, false);
        final DetailFragmentViewHolder views = DetailFragmentViewHolder.fromView(view);
        views.mKeywordsField.setText(getArguments().getString(MoPubListFragment.KEYWORDS_KEY, ""));
        views.mUserDataKeywordsField.setText(getArguments().getString(MoPubListFragment.USER_DATA_KEYWORDS_KEY, ""));
        hideSoftKeyboard(views.mUserDataKeywordsField);

        final String adUnitId = adConfiguration.getAdUnitId();
        views.mDescriptionView.setText(adConfiguration.getDescription());
        views.mAdUnitIdView.setText(adUnitId);
        views.mLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mShowButton.setEnabled(false);
                if (mMoPubInterstitial == null) {
                    mMoPubInterstitial = new MoPubInterstitial(getActivity(), adUnitId);
                    mMoPubInterstitial.setInterstitialAdListener(InterstitialDetailFragment.this);
                }
                final String keywords = views.mKeywordsField.getText().toString();
                final String userDatakeywords = views.mUserDataKeywordsField.getText().toString();
                mMoPubInterstitial.setKeywords(keywords);
                mMoPubInterstitial.setUserDataKeywords(userDatakeywords);
                if (mCallbacksAdapter != null) {
                    mCallbacksAdapter.generateCallbackList(InterstitialCallbacks.class);
                }
                mMoPubInterstitial.load();
            }
        });
        mShowButton = views.mShowButton;
        mShowButton.setEnabled(false);
        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMoPubInterstitial.show();
            }
        });

        final RecyclerView callbacksView = view.findViewById(R.id.callbacks_recycler_view);
        final Context context = getContext();
        if (callbacksView != null && context != null) {
            callbacksView.setLayoutManager(new LinearLayoutManager(context));
            mCallbacksAdapter = new CallbacksAdapter(context);
            mCallbacksAdapter.generateCallbackList(InterstitialCallbacks.class);
            callbacksView.setAdapter(mCallbacksAdapter);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mMoPubInterstitial != null) {
            mMoPubInterstitial.destroy();
            mMoPubInterstitial = null;
        }
    }

    // InterstitialAdListener implementation
    @Override
    public void onInterstitialLoaded(MoPubInterstitial interstitial) {
        mShowButton.setEnabled(true);
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), "Interstitial loaded.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(InterstitialCallbacks.LOADED.toString());
    }

    @Override
    public void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode) {
        mShowButton.setEnabled(false);
        final String errorMessage = (errorCode != null) ? errorCode.toString() : "";
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), "Interstitial failed to load: " + errorMessage);
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(InterstitialCallbacks.FAILED.toString(), errorMessage);
    }

    @Override
    public void onInterstitialShown(MoPubInterstitial interstitial) {
        mShowButton.setEnabled(false);
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), "Interstitial shown.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(InterstitialCallbacks.SHOWN.toString());
    }

    @Override
    public void onInterstitialClicked(MoPubInterstitial interstitial) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), "Interstitial clicked.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(InterstitialCallbacks.CLICKED.toString());

    }

    @Override
    public void onInterstitialDismissed(MoPubInterstitial interstitial) {
        if (mCallbacksAdapter == null) {
            logToast(getActivity(), "Interstitial dismissed.");
            return;
        }
        mCallbacksAdapter.notifyCallbackCalled(InterstitialCallbacks.DISMISSED.toString());
    }
}
