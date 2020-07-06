// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

import androidx.annotation.Nullable;

import com.mopub.common.CloseableLayout;
import com.mopub.common.CloseableLayout.OnCloseListener;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;

abstract class BaseInterstitialActivity extends Activity {
    @Nullable protected AdData mAdData;
    @Nullable private CloseableLayout mCloseableLayout;
    private long mBroadcastIdentifier;

    public abstract View getAdView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent == null) {
            MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, "Null intent on Activity. Unable to show ad.");
            finish();
            return;
        }
        mAdData = getAdDataFromIntent(intent);
        if (mAdData == null) {
            MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, "Null ad data on Activity. Unable to show ad.");
            finish();
            return;
        }
        mBroadcastIdentifier = mAdData.getBroadcastIdentifier();

        View adView = getAdView();

        mCloseableLayout = new CloseableLayout(this);
        final int blackColor = getResources().getColor(android.R.color.black);
        mCloseableLayout.setBackgroundColor(blackColor);
        mCloseableLayout.setOnCloseListener(new OnCloseListener() {
            @Override
            public void onClose() {
                finish();
            }
        });
        mCloseableLayout.addView(adView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setContentView(mCloseableLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Utils.hideNavigationBar(this);
    }

    @Override
    protected void onDestroy() {
        if (mCloseableLayout != null) {
            mCloseableLayout.removeAllViews();
        }
        super.onDestroy();
    }

    @Nullable
    protected CloseableLayout getCloseableLayout() {
        return mCloseableLayout;
    }

    long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    protected void showInterstitialCloseButton() {
        if (mCloseableLayout != null) {
            mCloseableLayout.setCloseVisible(true);
        }
    }

    protected void hideInterstitialCloseButton() {
        if (mCloseableLayout != null) {
            mCloseableLayout.setCloseVisible(false);
        }
    }

    @Nullable
    protected static AdData getAdDataFromIntent(Intent intent) {
        try {
            return (AdData) intent.getParcelableExtra(DataKeys.AD_DATA_KEY);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
