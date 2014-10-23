package com.mopub.mobileads;

import android.app.Activity;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.util.Dips;
import com.mopub.mobileads.util.Interstitials;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.mopub.common.util.Drawables.INTERSTITIAL_CLOSE_BUTTON_NORMAL;
import static com.mopub.common.util.Drawables.INTERSTITIAL_CLOSE_BUTTON_PRESSED;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;

abstract class BaseInterstitialActivity extends Activity {
    private OnClickListener mCloseOnClickListener;

    enum JavaScriptWebViewCallbacks {
        WEB_VIEW_DID_APPEAR("javascript:webviewDidAppear();"),
        WEB_VIEW_DID_CLOSE("javascript:webviewDidClose();");

        private String mUrl;
        private JavaScriptWebViewCallbacks(String url) {
            mUrl = url;
        }

        protected String getUrl() {
            return mUrl;
        }
    }

    private static final float CLOSE_BUTTON_SIZE_DP = 50f;
    private static final float CLOSE_BUTTON_PADDING = 8f;

    private ImageView mCloseButton;
    private RelativeLayout mLayout;
    private int mButtonSize;
    private int mButtonPadding;
    private long mBroadcastIdentifier;

    public abstract View getAdView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mButtonSize = Dips.asIntPixels(CLOSE_BUTTON_SIZE_DP, this);
        mButtonPadding = Dips.asIntPixels(CLOSE_BUTTON_PADDING, this);
        mCloseOnClickListener = new OnClickListener() {
            @Override
            public void onClick(final View view) {
                finish();
            }
        };

        // The contentView needs to be a RelativeLayout so the close button can be properly aligned
        mLayout = new RelativeLayout(this);
        final RelativeLayout.LayoutParams adViewLayout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        mLayout.addView(getAdView(), adViewLayout);
        setContentView(mLayout);

        final AdConfiguration adConfiguration = getAdConfiguration();
        if (adConfiguration != null) {
            mBroadcastIdentifier = adConfiguration.getBroadcastIdentifier();
        }

        createInterstitialCloseButton();
    }

    @Override
    protected void onDestroy() {
        mLayout.removeAllViews();
        super.onDestroy();
    }

    long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    protected void showInterstitialCloseButton() {
        mCloseButton.setVisibility(VISIBLE);
    }

    protected void hideInterstitialCloseButton() {
        mCloseButton.setVisibility(INVISIBLE);
    }

    protected AdConfiguration getAdConfiguration() {
        AdConfiguration adConfiguration;
        try {
            adConfiguration = (AdConfiguration) getIntent().getSerializableExtra(AD_CONFIGURATION_KEY);
        } catch (ClassCastException e) {
            adConfiguration = null;
        }
        return adConfiguration;
    }

    void addCloseEventRegion() {
        final int buttonSizePixels = Dips.dipsToIntPixels(CLOSE_BUTTON_SIZE_DP, this);
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(buttonSizePixels, buttonSizePixels);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        Interstitials.addCloseEventRegion(mLayout, layoutParams, mCloseOnClickListener);
    }

    private void createInterstitialCloseButton() {
        mCloseButton = new ImageButton(this);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {-android.R.attr.state_pressed}, INTERSTITIAL_CLOSE_BUTTON_NORMAL.decodeImage(this));
        states.addState(new int[] {android.R.attr.state_pressed}, INTERSTITIAL_CLOSE_BUTTON_PRESSED.decodeImage(this));
        mCloseButton.setImageDrawable(states);
        mCloseButton.setBackgroundDrawable(null);
        mCloseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        RelativeLayout.LayoutParams buttonLayout = new RelativeLayout.LayoutParams(mButtonSize, mButtonSize);
        buttonLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonLayout.setMargins(mButtonPadding, 0, mButtonPadding, 0);
        mLayout.addView(mCloseButton, buttonLayout);
    }
}
