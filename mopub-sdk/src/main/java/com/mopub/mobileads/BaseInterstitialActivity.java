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
    protected AdReport mAdReport;

    enum JavaScriptWebViewCallbacks {
        // The ad server appends these functions to the MRAID javascript to help with third party
        // impression tracking.
        WEB_VIEW_DID_APPEAR("webviewDidAppear();"),
        WEB_VIEW_DID_CLOSE("webviewDidClose();");

        private String mJavascript;
        private JavaScriptWebViewCallbacks(String javascript) {
            mJavascript = javascript;
        }

        protected String getJavascript() {
            return mJavascript;
        }

        protected String getUrl() {
            return "javascript:" + mJavascript;
        }
    }

    private CloseableLayout mCloseableLayout;
    private Long mBroadcastIdentifier;

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
        mCloseableLayout.removeAllViews();
        super.onDestroy();
    }

    Long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    protected void showInterstitialCloseButton() {
        mCloseableLayout.setCloseVisible(true);
    }

    protected void hideInterstitialCloseButton() {
        mCloseableLayout.setCloseVisible(false);
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
