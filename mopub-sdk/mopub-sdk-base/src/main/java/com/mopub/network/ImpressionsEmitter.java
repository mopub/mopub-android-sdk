// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;

/**
 * Single source of impression level revenue data. Implement interface {@link ImpressionListener}
 * and subscribe to events from ImpressionEmitter to receive detailed impression data.
 * This class is not linked to the activity lifecycle. It is recommended to subscribe to events
 * when application starts even before the first activity created.
 */
@SuppressWarnings("WeakerAccess")
public class ImpressionsEmitter {
    private static final HashSet<ImpressionListener> mListeners = new HashSet<>();

    /**
     * Call this method to start listening for impression level revenue data events.
     *
     * @param listener - {@link ImpressionListener} interface implementation
     */
    public static void addListener(@NonNull final ImpressionListener listener) {
        Preconditions.checkNotNull(listener);

        synchronized (ImpressionsEmitter.class) {
            mListeners.add(listener);
        }
    }

    /**
     * Call this method to unsubscribe from impression level revenue data events.
     *
     * @param listener - previously submitted to addListener() {@link ImpressionListener}
     */
    public static void removeListener(@NonNull final ImpressionListener listener) {
        Preconditions.checkNotNull(listener);

        synchronized (ImpressionsEmitter.class) {
            mListeners.remove(listener);
        }
    }

    /**
     * MoPub SDK internal method. Should not be used by publishers.
     */
    static void send(@NonNull final String adUnitId, @Nullable final ImpressionData impressionData) {
        Preconditions.checkNotNull(adUnitId);

        Set<ImpressionListener> listenerSet = cloneListeners();
        for (ImpressionListener listener : listenerSet) {
            listener.onImpression(adUnitId, impressionData);
        }
    }

    private static Set<ImpressionListener> cloneListeners() {
        synchronized (ImpressionsEmitter.class) {
            return new HashSet<>(mListeners);
        }
    }

    @VisibleForTesting
    static void clear() {
        synchronized (ImpressionsEmitter.class) {
            mListeners.clear();
        }
    }
}
