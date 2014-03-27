/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import com.mopub.mobileads.util.Dips;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.resource.Drawables.INTERSTITIAL_CLOSE_BUTTON_NORMAL;
import static com.mopub.mobileads.resource.Drawables.INTERSTITIAL_CLOSE_BUTTON_PRESSED;

abstract class BaseInterstitialActivity extends Activity {
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

    private static final float CLOSE_BUTTON_SIZE = 50f;
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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mButtonSize = Dips.asIntPixels(CLOSE_BUTTON_SIZE, this);
        mButtonPadding = Dips.asIntPixels(CLOSE_BUTTON_PADDING, this);

        mLayout = new RelativeLayout(this);
        final RelativeLayout.LayoutParams adViewLayout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        adViewLayout.addRule(RelativeLayout.CENTER_IN_PARENT);
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
