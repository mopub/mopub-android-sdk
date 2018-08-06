package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.SimpleAdapter;

import com.mopub.common.Preconditions;

import java.util.Map;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.KiipNativeRewardView;
import me.kiip.sdk.Modal;
import me.kiip.sdk.Poptart;

public class KiipMrecBanner extends CustomEventBanner{
    private static final String TAG = KiipMrecBanner.class.getSimpleName();
    private CustomEventBannerListener mCustomEventBannerListener;
    private KiipNativeRewardView mRewardView;

    private KiipNativeRewardView.OnShowListener onShowListener = new KiipNativeRewardView.OnShowListener() {
        @Override
        public void onShow(Kiip kiip) {
            Log.d(TAG, "KiipNativeRewardView onShow");
            mCustomEventBannerListener.onBannerExpanded();
        }
    };


    @Override
    protected void loadBanner(final Context context, CustomEventBannerListener customEventBannerListener, Map<String, Object> localExtras, Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventBannerListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (!(context instanceof Activity)) {
            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mRewardView = new KiipNativeRewardView(context);
        mRewardView.setOnShowListener(onShowListener);

        mCustomEventBannerListener = customEventBannerListener;

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
                        Log.d(TAG, "Kiip poptart failed to load.");
                        mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
                    }

                    @Override
                    public void onFinished(Kiip kiip, Poptart poptart) {
                        if (poptart != null) {
                            Log.d(TAG, "Kiip poptart loaded successfully.");
                            mRewardView.showReward(Kiip.getInstance(), poptart);
                            mCustomEventBannerListener.onBannerLoaded(mRewardView);

                        } else {
                            Log.d(TAG, "Kiip poptart loaded empty.");
                            mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
                        }
                    }
                });
            }

        }
    }

    @Override
    protected void onInvalidate() {

    }

}
