package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

/**
 * This is the base class for implementations of all native ad formats. When implementing a new
 * native ad format, be sure to also implement and register an appropriate {@link MoPubAdRenderer}
 * that supports the format.
 */
public abstract class BaseNativeAd {

    public interface NativeEventListener {
        void onAdImpressed();
        void onAdClicked();
    }

    @NonNull final private Set<String> mImpressionTrackers;
    @NonNull final private Set<String> mClickTrackers;
    @Nullable private NativeEventListener mNativeEventListener;

    protected BaseNativeAd() {
        mImpressionTrackers = new HashSet<String>();
        mClickTrackers = new HashSet<String>();
    }

    // Lifecycle Handlers
    /**
     * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
     * to prepare state for recording an impression or click before a view is rendered to screen.
     *
     * This method is optional.
     */
    public abstract void prepare(@NonNull final View view);

    /**
     * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
     * to reset or clear state of the native ad after it goes off screen and before it is rendered
     * again.
     *
     * This method is optional.
     */
    public abstract void clear(@NonNull final View view);

    /**
     * Your {@link BaseNativeAd} subclass should implement this method if the network requires the developer
     * to destroy or cleanup their native ad when they are permanently finished with it.
     *
     * This method is optional.
     */
    public abstract void destroy();

    public void setNativeEventListener(
            @Nullable final NativeEventListener nativeEventListener) {
        mNativeEventListener = nativeEventListener;
    }

    // Event Notifiers
    /**
     * Notifies the SDK that the ad has been shown. This will cause the SDK to record an impression
     * for the ad. This method must be called when the native ad is impressed in order for the
     * MoPub impression trackers to fire correctly.
     */
    protected final void notifyAdImpressed() {
        if (mNativeEventListener != null) {
            mNativeEventListener.onAdImpressed();
        }
    }

    /**
     * Notifies the SDK that the user has clicked the ad. This will cause the SDK to record an
     * click for the ad. This method must be called when the native ad is clicked in order for the
     * MoPub click trackers to fire correctly.
     */
    protected final void notifyAdClicked() {
        if (mNativeEventListener != null) {
            mNativeEventListener.onAdClicked();
        }
    }

    final protected void addImpressionTrackers(final Object impressionTrackers) throws ClassCastException {
        if (!(impressionTrackers instanceof JSONArray)) {
            throw new ClassCastException("Expected impression trackers of type JSONArray.");
        }

        final JSONArray trackers = (JSONArray) impressionTrackers;
        for (int i = 0; i < trackers.length(); i++) {
            try {
                addImpressionTracker(trackers.getString(i));
            } catch (JSONException e) {
                // This will only occur if we access a non-existent index in JSONArray.
                MoPubLog.d("Unable to parse impression trackers.");
            }
        }
    }

    final protected void addClickTrackers(final Object clickTrackers) throws ClassCastException {
        if (!(clickTrackers instanceof JSONArray)) {
            throw new ClassCastException("Expected click trackers of type JSONArray.");
        }

        final JSONArray trackers = (JSONArray) clickTrackers;
        for (int i = 0; i < trackers.length(); i++) {
            try {
                addClickTracker(trackers.getString(i));
            } catch (JSONException e) {
                // This will only occur if we access a non-existent index in JSONArray.
                MoPubLog.d("Unable to parse click trackers.");
            }
        }
    }

    final public void addImpressionTracker(@NonNull final String url) {
        if (!Preconditions.NoThrow.checkNotNull(url, "impressionTracker url is not allowed to be null")) {
            return;
        }
        mImpressionTrackers.add(url);
    }

    final public void addClickTracker(@NonNull final String url) {
        if (!Preconditions.NoThrow.checkNotNull(url, "clickTracker url is not allowed to be null")) {
            return;
        }
        mClickTrackers.add(url);
    }

    /**
     * Returns a Set<String> of all impression trackers associated with this native ad. Note that
     * network requests will automatically be made to each of these impression trackers when the
     * native ad is display on screen. See {@link StaticNativeAd#getImpressionMinPercentageViewed}
     * and {@link StaticNativeAd#getImpressionMinTimeViewed()} for relevant
     * impression-tracking parameters.
     */
    @NonNull
    Set<String> getImpressionTrackers() {
        return new HashSet<String>(mImpressionTrackers);
    }

    /**
     * Returns a Set<String> of all click trackers associated with this native ad. Note that
     * network requests will automatically be made to each of these click trackers when the
     * native ad is clicked.
     */
    @NonNull
    Set<String> getClickTrackers() {
        return new HashSet<String>(mClickTrackers);
    }
}
