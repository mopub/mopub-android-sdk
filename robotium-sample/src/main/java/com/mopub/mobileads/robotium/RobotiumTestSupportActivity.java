package com.mopub.mobileads.robotium;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;

import static com.mopub.mobileads.MoPubInterstitial.InterstitialAdListener;

public class RobotiumTestSupportActivity extends Activity {

    private MoPubView moPubView;
    private String bannerAdUnitId;
    private EditText bannerEditText;
    private InterstitialAdListener interstitialListener;
    private EditText interstitialEditText;
    private MoPubInterstitial moPubInterstitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        moPubView = (MoPubView) findViewById(R.id.mopubview);
        bannerEditText = (EditText) findViewById(R.id.banner_adunit_id_field);
        interstitialEditText = (EditText) findViewById(R.id.interstitial_adunit_id_field);

        // Banners
        Button loadBannerButton = (Button) findViewById(R.id.banner_load_button);
        loadBannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bannerAdUnitId = bannerEditText.getText().toString();
                moPubView.setAdUnitId(bannerAdUnitId);
                moPubView.loadAd();
            }
        });

        // Interstitials
        Button loadInterstitialButton = (Button) findViewById(R.id.interstitial_load_button);
        loadInterstitialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String interstitialAdUnitId = interstitialEditText.getText().toString();
                moPubInterstitial = new MoPubInterstitial(RobotiumTestSupportActivity.this, interstitialAdUnitId);
                moPubInterstitial.setInterstitialAdListener(interstitialListener);
                moPubInterstitial.load();
            }
        });

        Button showInterstitialButton = (Button) findViewById(R.id.interstitial_show_button);
        showInterstitialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (moPubInterstitial != null && moPubInterstitial.isReady()) {
                    moPubInterstitial.show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (moPubView != null) moPubView.destroy();
        if (moPubInterstitial != null) moPubInterstitial.destroy();
        super.onDestroy();
    }

    public void setInterstitialListener(InterstitialAdListener interstitialListener) {
        this.interstitialListener = interstitialListener;
    }
}
