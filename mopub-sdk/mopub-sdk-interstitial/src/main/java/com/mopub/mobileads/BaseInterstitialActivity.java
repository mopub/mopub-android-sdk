package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;

import com.mopub.common.AdReport;
import com.mopub.common.CloseableLayout;
import com.mopub.common.CloseableLayout.OnCloseListener;
import com.mopub.common.DataKeys;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;

abstract class BaseInterstitialActivity extends Activity {
    @Nullable protected AdReport mAdReport;
    @Nullable private CloseableLayout mCloseableLayout;
    @Nullable private Long mBroadcastIdentifier;

    public abstract View getAdView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mBroadcastIdentifier = getBroadcastIdentifierFromIntent(intent);
        mAdReport = getAdReportFromIntent(intent);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View adView = getAdView();

        mCloseableLayout = new CloseableLayout(this);
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

    @Nullable
    Long getBroadcastIdentifier() {
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

    protected static Long getBroadcastIdentifierFromIntent(Intent intent) {
        if (intent.hasExtra(BROADCAST_IDENTIFIER_KEY)) {
            return intent.getLongExtra(BROADCAST_IDENTIFIER_KEY, -1L);
        }
        return null;
    }

    @Nullable
    protected static AdReport getAdReportFromIntent(Intent intent) {
        try {
            return (AdReport) intent.getSerializableExtra(DataKeys.AD_REPORT_KEY);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
