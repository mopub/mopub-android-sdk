package com.mopub.common.event;

import com.mopub.common.ClientMetadata;

/**
 * Immutable data class with client event data.
 */
public class Event extends BaseEvent {
    Event(final Type eventType, final String requestUrl, final ClientMetadata metadata) {
        super(eventType, requestUrl, metadata);
    }
}
