package com.mopub.common.event;

import android.os.HandlerThread;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;

/**
 * Public interface used to record client events.
 */
public class MoPubEvents {

    private static volatile EventDispatcher sEventDispatcher;

    /**
     * Log a BaseEvent. MoPub uses logged events to analyze and improve performance.
     * This method should not be called by app developers.
     */
    public static void log(BaseEvent baseEvent) {
        MoPubEvents.getDispatcher().dispatch(baseEvent);
    }

    @VisibleForTesting
    public static void setEventDispatcher(EventDispatcher dispatcher) {
        sEventDispatcher = dispatcher;
    }

    /**
     * Returns a singleton event dispatcher constructed with a single background thread meant to be
     * used for all event logging operations. Operations that end up on the main thread, such as
     * the result of a network request, should post to this background thread when interacting
     * with shared resources in order to avoid concurrency issues.
     *
     * This design is meant to emulate an {@code IntentService} which we can't use due to
     * the requirement of the publisher having to update their manifest file.
     */
    @VisibleForTesting
    static EventDispatcher getDispatcher() {
        EventDispatcher result = sEventDispatcher;
        if (result == null) {
            synchronized (MoPubEvents.class) {
                result = sEventDispatcher;
                if (result == null) {
                    ArrayList<EventRecorder> recorders = new ArrayList<EventRecorder>();
                    HandlerThread handlerThread = new HandlerThread("mopub_event_logging");
                    handlerThread.start();
                    recorders.add(new ScribeEventRecorder(handlerThread.getLooper()));
                    result = sEventDispatcher = new EventDispatcher(recorders, handlerThread.getLooper());
                }
            }
        }
        return result;
    }
}
