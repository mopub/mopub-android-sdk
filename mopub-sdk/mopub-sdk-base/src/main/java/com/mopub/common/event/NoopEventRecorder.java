package com.mopub.common.event;

class NoopEventRecorder implements EventRecorder {
    @Override
    public void record(final BaseEvent baseEvent) {
    }
}

