package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.mopub.common.IntentActions;

import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class EventForwardingBroadcastReceiver extends BaseBroadcastReceiver {
    private final CustomEventInterstitialListener mCustomEventInterstitialListener;


    private static IntentFilter sIntentFilter;


    public EventForwardingBroadcastReceiver(CustomEventInterstitialListener customEventInterstitialListener, final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mCustomEventInterstitialListener = customEventInterstitialListener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(IntentActions.ACTION_INTERSTITIAL_FAIL);
            sIntentFilter.addAction(IntentActions.ACTION_INTERSTITIAL_SHOW);
            sIntentFilter.addAction(IntentActions.ACTION_INTERSTITIAL_DISMISS);
            sIntentFilter.addAction(IntentActions.ACTION_INTERSTITIAL_CLICK);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mCustomEventInterstitialListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (IntentActions.ACTION_INTERSTITIAL_FAIL.equals(action)) {
            mCustomEventInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
        } else if (IntentActions.ACTION_INTERSTITIAL_SHOW.equals(action)) {
            mCustomEventInterstitialListener.onInterstitialShown();
        } else if (IntentActions.ACTION_INTERSTITIAL_DISMISS.equals(action)) {
            mCustomEventInterstitialListener.onInterstitialDismissed();
            unregister(this);
        } else if (IntentActions.ACTION_INTERSTITIAL_CLICK.equals(action)) {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }

    }
}
