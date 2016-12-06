package com.mopub.common.event;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.network.ScribeBackoffPolicy;
import com.mopub.network.ScribeRequest;
import com.mopub.network.ScribeRequestManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * The ScribeEventRecorder manages events being sent to the Scribe service. It is responsible for
 * sampling, batching and kicking off network requests. It is also responsible for ensuring batched
 * events don't grow unbounded.
 */
public class ScribeEventRecorder implements EventRecorder {
    private static final String SCRIBE_URL = "https://analytics.mopub.com/i/jot/exchange_client_event";
    private static final int QUEUE_MAX_SIZE = 500;

    /**
     * As of SDK 3.6.0, events are roughly 1200 bytes in size. 1200 * 100 = 0.114441 MiB
     * This threshhold must always be < 1 MiB
     */
    private static final int EVENT_COUNT_SEND_THRESHHOLD = 100;

    /**
     * 2 minute polling time to check for send up events
     */
    private static final int POLLING_PERIOD_MS = 2 * 60 * 1000;

    @NonNull private final EventSampler mEventSampler;
    @NonNull private final Queue<BaseEvent> mEventQueue;
    @NonNull private final EventSerializer mEventSerializer;
    @NonNull private final ScribeRequestManager mScribeRequestManager;

    @NonNull private final Handler mPollHandler;
    @NonNull private final PollingRunnable mPollingRunnable;

    ScribeEventRecorder(@NonNull Looper looper) {
        this(new EventSampler(),
                new LinkedList<BaseEvent>(),
                new EventSerializer(),
                new ScribeRequestManager(looper),
                new Handler(looper));
    }

    @VisibleForTesting
    ScribeEventRecorder(@NonNull EventSampler eventSampler,
            @NonNull Queue<BaseEvent> eventQueue,
            @NonNull EventSerializer eventSerializer,
            @NonNull ScribeRequestManager scribeRequestManager,
            @NonNull Handler handler) {
        mEventSampler = eventSampler;
        mEventQueue = eventQueue;
        mEventSerializer = eventSerializer;
        mScribeRequestManager = scribeRequestManager;
        mPollHandler = handler;
        mPollingRunnable = new PollingRunnable();
    }

    @Override
    public void record(@NonNull BaseEvent baseEvent) {
        if (!mEventSampler.sample(baseEvent)) {
            return;
        }

        if (mEventQueue.size() >= QUEUE_MAX_SIZE) {
            MoPubLog.d("EventQueue is at max capacity. " +
                    "Event \"" + baseEvent.getName() + "\" is being dropped.");
            return;
        }

        mEventQueue.add(baseEvent);
        if (mEventQueue.size() >= EVENT_COUNT_SEND_THRESHHOLD) {
            sendEvents();
        }

        scheduleNextPoll();
    }

    @VisibleForTesting
    void sendEvents() {
        if (mScribeRequestManager.isAtCapacity()) {
            return;
        }

        final List<BaseEvent> events = dequeueEvents();
        if (events.isEmpty()) {
            return;
        }

        mScribeRequestManager.makeRequest(
                new ScribeRequest.ScribeRequestFactory() {
                    @Override
                    public ScribeRequest createRequest(ScribeRequest.Listener listener) {
                        return new ScribeRequest(SCRIBE_URL, events, mEventSerializer, listener);
                    }
                },
                new ScribeBackoffPolicy()
        );
    }

    @VisibleForTesting
    @NonNull
    List<BaseEvent> dequeueEvents() {
        ArrayList<BaseEvent> baseEvents = new ArrayList<BaseEvent>();

        // Note: Some queues do not have constant time O(1) performance for its #size()
        // method, so we're peeking and polling instead
        while (mEventQueue.peek() != null && baseEvents.size() < EVENT_COUNT_SEND_THRESHHOLD) {
            baseEvents.add(mEventQueue.poll());
        }
        return baseEvents;
    }

    @VisibleForTesting
    void scheduleNextPoll() {
        // Only schedule if there are no messages already scheduled.
        // The user defined message code, the 'what' param in Handler#hasMessages, defaults to
        // 0 for posting a delayed runnable
        if (mPollHandler.hasMessages(0) || mEventQueue.isEmpty()) {
            return;
        }

        mPollHandler.postDelayed(mPollingRunnable, POLLING_PERIOD_MS);
    }

    class PollingRunnable implements Runnable {
        @Override
        public void run() {
            sendEvents();
            scheduleNextPoll();
        }
    }
}
