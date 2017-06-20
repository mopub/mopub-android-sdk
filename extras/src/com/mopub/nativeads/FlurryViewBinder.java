package com.mopub.nativeads;

import android.support.annotation.NonNull;

/**
 * Please reference the Supported Mediation Partner page at http://bit.ly/2mqsuFH for the
 * latest version and ad format certifications.
 */
public class FlurryViewBinder {
    ViewBinder staticViewBinder;
    int videoViewId;

    private FlurryViewBinder(@NonNull Builder builder) {
        this.staticViewBinder = builder.staticViewBinder;
        this.videoViewId = builder.videoViewId;
    }

    public final static class Builder {
        ViewBinder staticViewBinder;
        int videoViewId;

        public Builder(final ViewBinder staticViewBinder) {
            this.staticViewBinder = staticViewBinder;
        }

        @NonNull
        public final Builder videoViewId(final int videoViewId) {
            this.videoViewId = videoViewId;
            return this;
        }

        @NonNull
        public final FlurryViewBinder build() {
            return new FlurryViewBinder(this);
        }
    }
}
