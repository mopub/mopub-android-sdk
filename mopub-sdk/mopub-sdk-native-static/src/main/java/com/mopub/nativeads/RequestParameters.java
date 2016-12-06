package com.mopub.nativeads;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.EnumSet;

public class RequestParameters {

    public enum NativeAdAsset {
        TITLE("title"),
        TEXT("text"),
        ICON_IMAGE("iconimage"),
        MAIN_IMAGE("mainimage"),
        CALL_TO_ACTION_TEXT("ctatext"),
        STAR_RATING("starrating");

        private final String mAssetName;

        private NativeAdAsset(@NonNull String assetName) {
            mAssetName = assetName;
        }

        @NonNull
        @Override
        public String toString() {
            return mAssetName;
        }
    }

    @Nullable private final String mKeywords;
    @Nullable private final Location mLocation;
    @Nullable private final EnumSet<NativeAdAsset> mDesiredAssets;

    public final static class Builder {
        private String keywords;
        private Location location;
        private EnumSet<NativeAdAsset> desiredAssets;

        @NonNull
        public final Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        @NonNull
        public final Builder location(Location location) {
            this.location = location;
            return this;
        }

        // Specify set of assets used by this ad request. If not set, this defaults to all assets
        @NonNull
        public final Builder desiredAssets(final EnumSet<NativeAdAsset> desiredAssets) {
            this.desiredAssets = EnumSet.copyOf(desiredAssets);
            return this;
        }

        @NonNull
        public final RequestParameters build() {
            return new RequestParameters(this);
        }
    }

    private RequestParameters(@NonNull Builder builder) {
        mKeywords = builder.keywords;
        mLocation = builder.location;
        mDesiredAssets = builder.desiredAssets;
    }

    @Nullable
    public final String getKeywords() {
        return mKeywords;
    }

    @Nullable
    public final Location getLocation() {
        return mLocation;
    }

    public final String getDesiredAssets() {
        String result = "";

        if (mDesiredAssets != null) {
            result = TextUtils.join(",", mDesiredAssets.toArray());
        }
        return result;
    }
}
