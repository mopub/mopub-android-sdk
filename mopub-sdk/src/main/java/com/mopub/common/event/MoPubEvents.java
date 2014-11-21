package com.mopub.common.event;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.mopub.common.ClientMetadata;
import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;

/**
 * Records both untimed and timed events. This class maintains a queue of events to be recorded and
 * launches a background thread to handler their recording.
 */
public class MoPubEvents {

    private static volatile EventDispatcher sEventDispatcher;

    private static EventDispatcher getDispatcher() {
        EventDispatcher result = sEventDispatcher;
        if (result == null) {
            synchronized (MoPubEvents.class) {
                result = sEventDispatcher;
                if (result == null) {
                    ArrayList<EventRecorder> recorders = new ArrayList<EventRecorder>();
                    recorders.add(new NoopEventRecorder());
                    HandlerThread handlerThread = new HandlerThread("mopub_event_queue");
                    result = sEventDispatcher = new EventDispatcher(recorders, handlerThread);
                }
            }
        }
        return result;
    }

    @VisibleForTesting
    public static void setEventDispatcher(EventDispatcher dispatcher) {
        sEventDispatcher = dispatcher;
    }

    /**
     * Log a ClientEvent. MoPub uses logged events to analyze and improve performance.
     * This method should not be called by app developers.
     */
    public static void event(Event.Type eventType, String requestUrl) {
        final EventDispatcher dispatcher = MoPubEvents.getDispatcher();
        final Event event = new Event(eventType, requestUrl, ClientMetadata.getInstance());
        dispatcher.sendEventToHandlerThread(event);
    }


    /**
     * Create and start a TimedEvent. A TimedEvent isn't recored until
     * {@link com.mopub.common.event.TimedEvent#stop(int)} is called.
     */
    public static TimedEvent timedEvent(Event.Type eventType, String requestUrl) {
        final EventDispatcher events = getDispatcher();
        final TimedEvent event =
                new TimedEvent(eventType, requestUrl, ClientMetadata.getInstance(), events);
        return event;
    }

    private static class NoopEventRecorder implements EventRecorder {

        @Override
        public void recordEvent(final Event event) {

        }

        @Override
        public void recordTimedEvent(final TimedEvent event) {

        }
    }

    @VisibleForTesting
    public static class EventDispatcher implements TimedEvent.Listener {
        private final Iterable<EventRecorder> mEventRecorders;
        private final HandlerThread mHandlerThread;
        private final Handler mMessageHandler;

        @VisibleForTesting Handler.Callback mHandlerCallback;

        @VisibleForTesting
        EventDispatcher(Iterable<EventRecorder> recorders, HandlerThread handlerThread) {
            mEventRecorders = recorders;
            mHandlerCallback = new Handler.Callback() {
                @Override
                public boolean handleMessage(final Message msg) {
                    if (msg.obj instanceof TimedEvent) {
                        final TimedEvent event = (TimedEvent) msg.obj;
                        for (final EventRecorder recorder : mEventRecorders) {
                            recorder.recordTimedEvent(event);
                        }

                    } else if (msg.obj instanceof Event) {
                        final Event event = (Event) msg.obj;
                        for (final EventRecorder recorder : mEventRecorders) {
                            recorder.recordEvent(event);
                        }
                    }
                    return true; // Even if it's not an event, swallow the message.
                }
            };
            mHandlerThread = handlerThread;
            mHandlerThread.start();
            mMessageHandler = new Handler(mHandlerThread.getLooper(), mHandlerCallback);
        }

        private void sendEventToHandlerThread(BaseEvent event) {
            final Message message = Message.obtain(mMessageHandler, 0, event);
            message.sendToTarget();
        }

        @Override
        public void onStopped(final TimedEvent event) {
            this.sendEventToHandlerThread(event);
        }

        @Override
        public void onCancelled(final TimedEvent event) {
            // Nothing to do for now.
        }
    }
}
