package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

/**
 * This class is used to update a {@link VastVideoViewController}'s countdown display according to rules
 * contained in the {@link VastVideoViewController}
 */
public class VastVideoViewCountdownRunnable extends RepeatingHandlerRunnable {

    @NonNull private final VastVideoViewController mVideoViewController;

    public VastVideoViewCountdownRunnable(@NonNull VastVideoViewController videoViewController,
            @NonNull Handler handler) {
        super(handler);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(videoViewController);


        mVideoViewController = videoViewController;
    }

    @Override
    public void doWork() {
        mVideoViewController.updateCountdown();

        if (mVideoViewController.shouldBeInteractable()) {
            mVideoViewController.makeVideoInteractable();
        }
    }
}
