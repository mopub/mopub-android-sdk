package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mopub.common.Preconditions;

import java.util.Map;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.Modal;
import me.kiip.sdk.Poptart;

/**
 * A custom event for showing Kiip interstitial ads.
 */
public class KiipInterstitial extends CustomEventInterstitial {

    private CustomEventInterstitialListener mInterstitialListener;
    private Context mAppContext;
    private Poptart mPoptart;

    private Modal.OnShowListener onPoptartShow = new Modal.OnShowListener() {
        @Override
        public void onShow(Modal modal) {
            Log.d("MoPub", "Kiip Modal onShow");
            mInterstitialListener.onInterstitialShown();
        }
    };


    private Modal.OnDismissListener onPoptartDismiss = new Modal.OnDismissListener() {
        @Override
        public void onDismiss(Modal modal) {
            Log.d("MoPub", "Kiip Modal onDismiss");
            mInterstitialListener.onInterstitialDismissed();
        }
    };

    @Override
    protected void loadInterstitial(@NonNull Context context,
                                    @NonNull CustomEventInterstitialListener interstitialListener,
                                    @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) {

        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(interstitialListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (!(context instanceof Activity)) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mInterstitialListener = interstitialListener;
        mAppContext = context;

        if (Kiip.getInstance() == null) {
            if (serverExtras.containsKey("appKey") && serverExtras.containsKey("appSecret")) {
                Kiip.init((Application) context.getApplicationContext(),
                        serverExtras.get("appKey"),
                        serverExtras.get("appSecret"));
            }
        }

        if (Kiip.getInstance() != null) {
            if (serverExtras.containsKey("testMode")) {
                Kiip.getInstance().setTestMode(Boolean.valueOf(serverExtras.get("testMode")));
            }
            if (serverExtras.containsKey("email")) {
                Kiip.getInstance().setEmail(serverExtras.get("email"));
            }
            if (serverExtras.containsKey("gender")) {
                Kiip.getInstance().setEmail(serverExtras.get("gender"));
            }
            if (serverExtras.containsKey("birthday")) {
                Kiip.getInstance().setEmail(serverExtras.get("birthday"));
            }
            if (serverExtras.containsKey("userId")) {
                Kiip.getInstance().setUserId(serverExtras.get("userId"));
            }
            if (serverExtras.containsKey("ageGroup")) {
                Kiip.getInstance().setAgeGroup(Kiip.AgeGroup.values()[Integer.valueOf(serverExtras.get("ageGroup"))]);
            }
            if (serverExtras.containsKey("momentId")) {
                Kiip.getInstance().saveMoment(serverExtras.get("momentId"), new Kiip.Callback() {
                    @Override
                    public void onFailed(Kiip kiip, Exception e) {
                        Log.d("MoPub", "Kiip poptart failed to load.");
                        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                    }

                    @Override
                    public void onFinished(Kiip kiip, Poptart poptart) {
                        if (poptart != null) {
                            Log.d("MoPub", "Kiip poptart loaded successfully.");
                            mPoptart = poptart;
                            final Modal modal = mPoptart.getModal();
                            if (modal != null) {
                                modal.setOnShowListener(onPoptartShow);
                                modal.setOnDismissListener(onPoptartDismiss);
                            }
                            mInterstitialListener.onInterstitialLoaded();
                        } else {
                            Log.d("MoPub", "Kiip poptart loaded empty.");
                            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NO_FILL);
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing Kiip poptart");
        if (mPoptart != null && !mPoptart.isShowing()) {
            mPoptart.show(mAppContext);
            mPoptart = null;
        }
    }

    @Override
    protected void onInvalidate() {
        if (Kiip.getInstance() != null) {
            mPoptart = null;
        }
    }
}
