// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

import com.mopub.common.IntentActions;

import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public class EventForwardingBroadcastReceiver extends BaseBroadcastReceiver {
    private final AdLifecycleListener.InteractionListener mInteractionListener;

    private static IntentFilter sIntentFilter;

    public EventForwardingBroadcastReceiver(AdLifecycleListener.InteractionListener interactionListener, final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mInteractionListener = interactionListener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(IntentActions.ACTION_FULLSCREEN_FAIL);
            sIntentFilter.addAction(IntentActions.ACTION_FULLSCREEN_SHOW);
            sIntentFilter.addAction(IntentActions.ACTION_FULLSCREEN_DISMISS);
            sIntentFilter.addAction(IntentActions.ACTION_FULLSCREEN_CLICK);
            sIntentFilter.addAction(IntentActions.ACTION_REWARDED_AD_COMPLETE);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mInteractionListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (IntentActions.ACTION_FULLSCREEN_FAIL.equals(action)) {
            mInteractionListener.onAdFailed(NETWORK_INVALID_STATE);
        } else if (IntentActions.ACTION_FULLSCREEN_SHOW.equals(action)) {
            mInteractionListener.onAdShown();
            mInteractionListener.onAdImpression();
        } else if (IntentActions.ACTION_FULLSCREEN_DISMISS.equals(action)) {
            mInteractionListener.onAdDismissed();
            unregister(this);
        } else if (IntentActions.ACTION_FULLSCREEN_CLICK.equals(action)) {
            mInteractionListener.onAdClicked();
        } else if (IntentActions.ACTION_REWARDED_AD_COMPLETE.equals(action)) {
            mInteractionListener.onAdComplete(null);
        }
    }
}
