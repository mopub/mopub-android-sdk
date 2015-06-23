package com.mopub.common.event;

import android.support.annotation.NonNull;

/**
 * Immutable data class with client event data.
 */
public class Event extends BaseEvent {
    private Event(@NonNull Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseEvent.Builder {
        public Builder(@NonNull Name name, @NonNull Category category, double samplingRate) {
            super(ScribeCategory.EXCHANGE_CLIENT_EVENT, name, category, samplingRate);
        }

        @NonNull
        @Override
        public Event build() {
            return new Event(this);
        }
    }
}
