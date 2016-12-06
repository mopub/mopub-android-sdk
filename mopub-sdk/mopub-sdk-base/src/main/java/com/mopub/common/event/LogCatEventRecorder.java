package com.mopub.common.event;

import com.mopub.common.logging.MoPubLog;

class LogCatEventRecorder implements EventRecorder {
    @Override
    public void record(final BaseEvent baseEvent) {
        MoPubLog.d(baseEvent.toString());
    }
}

