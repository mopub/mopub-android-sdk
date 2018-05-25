package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.io.Serializable;

/**
 * State encapsulation for VAST tracking URLs that may or may not only be called once. For example,
 * progress trackers are only called once, but error trackers are repeatable.
 */
public class VastTracker implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull private final MessageType mMessageType;
    @NonNull private final String mContent;
    private boolean mCalled;
    private boolean mIsRepeatable;

    enum MessageType { TRACKING_URL, QUARTILE_EVENT }

    public VastTracker(@NonNull final MessageType messageType, @NonNull final String content) {
        Preconditions.checkNotNull(messageType);
        Preconditions.checkNotNull(content);

        mMessageType = messageType;
        mContent = content;
    }

    // Legacy implementation implied URL tracking
    public VastTracker(@NonNull final String trackingUrl) {
        this(MessageType.TRACKING_URL, trackingUrl);
    }

    public VastTracker(@NonNull String trackingUrl, boolean isRepeatable) {
        this(trackingUrl);
        mIsRepeatable = isRepeatable;
    }

    @NonNull
    public MessageType getMessageType() {
        return mMessageType;
    }

    @NonNull
    public String getContent() {
        return mContent;
    }

    public void setTracked() {
        mCalled = true;
    }

    public boolean isTracked() {
        return mCalled;
    }

    public boolean isRepeatable() {
        return mIsRepeatable;
    }
}
