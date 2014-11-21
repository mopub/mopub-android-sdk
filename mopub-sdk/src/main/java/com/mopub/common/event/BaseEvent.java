package com.mopub.common.event;

import com.mopub.common.ClientMetadata;

public abstract class BaseEvent {
    public enum Type {
        NETWORK_REQUEST("request"),
        DATA_ERROR("invalid_data");

        public final String mName;
        Type(String name) {
            mName = name;
        }
    }

    private final ClientMetadata mMetadata;
    private final String mEventName;
    private final String mRequestUrl;
    private final long mEventTimeUtcMillis;

    BaseEvent(Type eventType, String requestUrl, ClientMetadata metadata) {
        mEventTimeUtcMillis = System.currentTimeMillis();
        mEventName = eventType.mName;
        mRequestUrl = requestUrl;
        mMetadata = metadata;
    }

    public long getEventTimeUtcMillis() {
        return mEventTimeUtcMillis;
    }

    public String getRequestUrl() {
        return mRequestUrl;
    }

    public String getEventName() {
        return mEventName;
    }

    public ClientMetadata getMetadata() {
        return mMetadata;
    }
}
