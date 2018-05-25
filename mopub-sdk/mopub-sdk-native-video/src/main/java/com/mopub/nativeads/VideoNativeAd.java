package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.Preconditions;

import java.util.HashMap;
import java.util.Map;

public abstract class VideoNativeAd extends BaseNativeAd implements NativeVideoController.Listener {

    // Basic fields
    @Nullable private String mMainImageUrl;
    @Nullable private String mIconImageUrl;
    @Nullable private String mClickDestinationUrl;
    @Nullable private String mCallToAction;
    @Nullable private String mTitle;
    @Nullable private String mText;
    @Nullable private String mPrivacyInformationIconClickThroughUrl;
    @Nullable private String mPrivacyInformationIconImageUrl;
    @Nullable private String mVastVideo;

    // Extras
    @NonNull
    private final Map<String, Object> mExtras;

    public VideoNativeAd() {
        mExtras = new HashMap<String, Object>();
    }

    // Getters
    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getText() {
        return mText;
    }

    @Nullable
    public String getMainImageUrl() {
        return mMainImageUrl;
    }

    @Nullable
    public String getIconImageUrl() {
        return mIconImageUrl;
    }

    @Nullable
    public String getClickDestinationUrl() {
        return mClickDestinationUrl;
    }

    @Nullable
    public String getVastVideo() {
        return mVastVideo;
    }

    @Nullable
    public String getCallToAction() {
        return mCallToAction;
    }

    @Nullable
    public String getPrivacyInformationIconClickThroughUrl() {
        return mPrivacyInformationIconClickThroughUrl;
    }

    /**
     * Returns the Privacy Information image url.
     *
     * @return String representing the Privacy Information Icon image url, or {@code null} if not
     * set.
     */
    @Nullable
    public String getPrivacyInformationIconImageUrl() {
        return mPrivacyInformationIconImageUrl;
    }

    /**
     * Given a particular String key, return the associated Object value from the ad's extras
     * map. See {@link VideoNativeAd#getExtras()} for more information.
     */
    @Nullable
    final public Object getExtra(@NonNull final String key) {
        if (!Preconditions.NoThrow.checkNotNull(key, "getExtra key is not allowed to be null")) {
            return null;
        }
        return mExtras.get(key);
    }

    final public Map<String, Object> getExtras() {
        return mExtras;
    }

    // Setters
    public void setTitle(@Nullable String title) {
        mTitle = title;
    }

    public void setText(@Nullable String text) {
        mText = text;
    }

    public void setMainImageUrl(@Nullable String mainImageUrl) {
        mMainImageUrl = mainImageUrl;
    }

    public void setIconImageUrl(@Nullable String iconImageUrl) {
        mIconImageUrl = iconImageUrl;
    }

    public void setClickDestinationUrl(@Nullable String clickDestinationUrl) {
        mClickDestinationUrl = clickDestinationUrl;
    }

    public void setVastVideo(String vastVideo) {
        mVastVideo = vastVideo;
    }

    public void setCallToAction(@Nullable String callToAction) {
        mCallToAction = callToAction;
    }

    public void setPrivacyInformationIconClickThroughUrl(
            @Nullable String privacyInformationIconClickThroughUrl) {
        mPrivacyInformationIconClickThroughUrl = privacyInformationIconClickThroughUrl;
    }

    public void setPrivacyInformationIconImageUrl(
            @Nullable String privacyInformationIconImageUrl) {
        mPrivacyInformationIconImageUrl = privacyInformationIconImageUrl;
    }

    final public void addExtra(@NonNull final String key, @Nullable final Object value) {
        if (!Preconditions.NoThrow.checkNotNull(key, "addExtra key is not allowed to be null")) {
            return;
        }
        mExtras.put(key, value);
    }

    // Lifecycle Handlers
    @Override
    public void prepare(@NonNull final View view) { }

    @Override
    public void clear(@NonNull final View view) { }

    @Override
    public void destroy() { }

    // Render
    public void render(@NonNull MediaLayout mediaLayout) { }
}
