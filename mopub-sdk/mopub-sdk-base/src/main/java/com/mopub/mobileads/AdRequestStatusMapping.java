package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.Map;
import java.util.TreeMap;

public class AdRequestStatusMapping {
    @NonNull
    private final Map<String, AdRequestStatus> mAdUnitToAdRequestStatus;

    public AdRequestStatusMapping() {
        mAdUnitToAdRequestStatus = new TreeMap<String, AdRequestStatus>();
    }

    void markFail(@NonNull final String adUnitId) {
        mAdUnitToAdRequestStatus.remove(adUnitId);
    }

    void markLoading(@NonNull final String adUnitId) {
        mAdUnitToAdRequestStatus.put(adUnitId, new AdRequestStatus(LoadingStatus.LOADING));
    }

    void markLoaded(
            @NonNull final String adUnitId,
            @Nullable final String failUrlString,
            @Nullable final String impressionTrackerUrlString,
            @Nullable final String clickTrackerUrlString) {
        mAdUnitToAdRequestStatus.put(adUnitId, new AdRequestStatus(
                LoadingStatus.LOADED,
                failUrlString,
                impressionTrackerUrlString,
                clickTrackerUrlString));
    }

    void markPlayed(@NonNull final String adUnitId) {
        // If possible, attempt to keep the URL fields in AdRequestStatus
        if (mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            final AdRequestStatus adRequestStatus = mAdUnitToAdRequestStatus.get(adUnitId);
            adRequestStatus.setStatus(LoadingStatus.PLAYED);
        } else {
            mAdUnitToAdRequestStatus.put(adUnitId, new AdRequestStatus(LoadingStatus.PLAYED));
        }
    }

    boolean canPlay(@NonNull final String adUnitId) {
        final AdRequestStatus adRequestStatus = mAdUnitToAdRequestStatus.get(adUnitId);
        return adRequestStatus != null
            && LoadingStatus.LOADED.equals(adRequestStatus.getStatus());
    }

    boolean isLoading(@NonNull final String adUnitId) {
        if (!mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            return false;
        }

        return mAdUnitToAdRequestStatus.get(adUnitId).getStatus() == LoadingStatus.LOADING;
    }

    @Nullable String getFailoverUrl(@NonNull final String adUnitId) {
        if (!mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            return null;
        }

        return mAdUnitToAdRequestStatus.get(adUnitId).getFailurl();
    }

    @Nullable String getImpressionTrackerUrlString(@NonNull final String adUnitId) {
        if (!mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            return null;
        }

        return mAdUnitToAdRequestStatus.get(adUnitId).getImpressionUrl();
    }

    @Nullable String getClickTrackerUrlString(@NonNull final String adUnitId) {
        if (!mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            return null;
        }

        return mAdUnitToAdRequestStatus.get(adUnitId).getClickUrl();
    }

    void clearImpressionUrl(@NonNull final String adUnitId) {
        if (mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            mAdUnitToAdRequestStatus.get(adUnitId).setImpressionUrl(null);
        }
    }

    void clearClickUrl(@NonNull final String adUnitId) {
        if (mAdUnitToAdRequestStatus.containsKey(adUnitId)) {
            mAdUnitToAdRequestStatus.get(adUnitId).setClickUrl(null);
        }
    }

    @Deprecated
    @VisibleForTesting
    void clearMapping() {
        mAdUnitToAdRequestStatus.clear();
    }

    private static enum LoadingStatus { LOADING, LOADED, PLAYED }
    private static class AdRequestStatus {
        @NonNull
        private LoadingStatus mLoadingStatus;
        @Nullable
        private String mFailUrl;
        @Nullable
        private String mImpressionUrl;
        @Nullable
        private String mClickUrl;

        public AdRequestStatus(@NonNull final LoadingStatus loadingStatus) {
            this(loadingStatus, null, null, null);
        }

        public AdRequestStatus(
                @NonNull final LoadingStatus loadingStatus,
                @Nullable final String failUrl,
                @Nullable final String impressionUrl,
                @Nullable final String clickUrl) {
            Preconditions.checkNotNull(loadingStatus);

            mLoadingStatus = loadingStatus;
            mFailUrl = failUrl;
            mImpressionUrl = impressionUrl;
            mClickUrl = clickUrl;
        }

        @NonNull
        private LoadingStatus getStatus() {
            return mLoadingStatus;
        }

        private void setStatus(@NonNull final LoadingStatus loadingStatus) {
            mLoadingStatus = loadingStatus;
        }

        @Nullable
        private String getFailurl() {
            return mFailUrl;
        }

        @Nullable
        private String getImpressionUrl() {
            return mImpressionUrl;
        }

        private void setImpressionUrl(@Nullable final String impressionUrl) {
            mImpressionUrl = impressionUrl;
        }

        @Nullable
        private String getClickUrl() {
            return mClickUrl;
        }

        private void setClickUrl(@Nullable final String clickUrl) {
            mClickUrl = clickUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (this == o) {
                return true;
            }

            if (!(o instanceof AdRequestStatus)) {
                return false;
            }

            final AdRequestStatus that = (AdRequestStatus) o;

            return this.mLoadingStatus.equals(that.mLoadingStatus) &&
                    TextUtils.equals(this.mFailUrl, that.mFailUrl) &&
                    TextUtils.equals(this.mImpressionUrl, that.mImpressionUrl) &&
                    TextUtils.equals(this.mClickUrl, that.mClickUrl);
        }

        @Override
        public int hashCode() {
            int result = 29;
            result = 31 * result + mLoadingStatus.ordinal();
            result = 31 * result + (mFailUrl != null
                    ? mFailUrl.hashCode()
                    : 0);
            result = 31 * result + (mImpressionUrl != null
                    ? mImpressionUrl.hashCode()
                    : 0);
            result = 31 * result + (mClickUrl != null
                    ? mClickUrl.hashCode()
                    : 0);
            return result;
        }
    }
}
