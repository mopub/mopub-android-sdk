package com.mopub.nativeads;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;

import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.MraidVideoPlayerActivity;
import com.mopub.mobileads.VastManager;
import com.mopub.mobileads.VastTracker;
import com.mopub.mobileads.VastVideoConfig;
import com.mopub.mobileads.VideoViewabilityTracker;
import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.nativeads.NativeVideoController.NativeVideoProgressRunnable;
import com.mopub.network.TrackingRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.DataKeys.IMPRESSION_MIN_VISIBLE_PERCENT;
import static com.mopub.common.DataKeys.IMPRESSION_MIN_VISIBLE_PX;
import static com.mopub.common.DataKeys.IMPRESSION_VISIBLE_MS;
import static com.mopub.common.DataKeys.JSON_BODY_KEY;
import static com.mopub.common.DataKeys.MAX_BUFFER_MS;
import static com.mopub.common.DataKeys.PAUSE_VISIBLE_PERCENT;
import static com.mopub.common.DataKeys.PLAY_VISIBLE_PERCENT;
import static com.mopub.common.DataKeys.VIDEO_TRACKERS_KEY;
import static com.mopub.nativeads.NativeImageHelper.preCacheImages;
import static com.mopub.nativeads.NativeVideoController.VisibilityTrackingEvent;

public class MoPubCustomEventVideoNative extends CustomEventNative {

    @Override
    protected void loadNativeAd(@NonNull final Context context,
            @NonNull final CustomEventNativeListener customEventNativeListener,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) {
        final Object json = localExtras.get(JSON_BODY_KEY);
        // null or non-JSONObjects should not be passed in localExtras as JSON_BODY_KEY
        if (!(json instanceof JSONObject)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
            return;
        }

        final VideoResponseHeaders videoResponseHeaders = new VideoResponseHeaders(serverExtras);
        if (!videoResponseHeaders.hasValidHeaders()) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
            return;
        }

        final Object clickTrackingUrlFromHeaderObject =
                localExtras.get(DataKeys.CLICK_TRACKING_URL_KEY);
        // Ensure click tracking url is a non-empty String
        if (!(clickTrackingUrlFromHeaderObject instanceof String) ||
                TextUtils.isEmpty((String) clickTrackingUrlFromHeaderObject)) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            return;
        }

        final String clickTrackingUrlFromHeader = (String) clickTrackingUrlFromHeaderObject;
        final MoPubVideoNativeAd videoNativeAd = new MoPubVideoNativeAd(context, (JSONObject) json,
                customEventNativeListener, videoResponseHeaders,
                clickTrackingUrlFromHeader);
        try {
            videoNativeAd.loadAd();
        } catch (IllegalArgumentException e) {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
        }
    }

    public static class MoPubVideoNativeAd extends VideoNativeAd
            implements VastManager.VastManagerListener, NativeVideoProgressRunnable.ProgressListener,
            AudioManager.OnAudioFocusChangeListener {

        enum Parameter {
            IMPRESSION_TRACKER("imptracker", true),
            CLICK_TRACKER("clktracker", true),
            TITLE("title", false),
            TEXT("text", false),
            IMAGE_URL("mainimage", false),
            ICON_URL("iconimage", false),
            CLICK_DESTINATION("clk", false),
            FALLBACK("fallback", false),
            CALL_TO_ACTION("ctatext", false),
            VAST_VIDEO("video", false);

            @NonNull final String mName;
            final boolean mRequired;

            Parameter(@NonNull final String name, final boolean required) {
                Preconditions.checkNotNull(name);
                mName = name;
                mRequired = required;
            }

            @Nullable
            static Parameter from(@NonNull final String name) {
                Preconditions.checkNotNull(name);
                for (final Parameter parameter : values()) {
                    if (parameter.mName.equals(name)) {
                        return parameter;
                    }
                }

                return null;
            }

            @NonNull
            @VisibleForTesting
            static final Set<String> requiredKeys = new HashSet<String>();
            static {
                for (final Parameter parameter : values()) {
                    if (parameter.mRequired) {
                        requiredKeys.add(parameter.mName);
                    }
                }
            }
        }

        public enum VideoState {
            CREATED, LOADING, BUFFERING, PAUSED, PLAYING, PLAYING_MUTED, ENDED, FAILED_LOAD
        }

        static final String PRIVACY_INFORMATION_CLICKTHROUGH_URL = "https://www.mopub.com/optout/";

        @NonNull private final Context mContext;
        @NonNull private final JSONObject mJsonObject;
        @NonNull private VideoState mVideoState;
        @NonNull private final VisibilityTracker mVideoVisibleTracking;
        @NonNull private final String mMoPubClickTrackingUrl;
        @NonNull private final CustomEventNativeListener mCustomEventNativeListener;
        @NonNull private final VideoResponseHeaders mVideoResponseHeaders;
        @NonNull private final NativeVideoControllerFactory mNativeVideoControllerFactory;
        @Nullable private NativeVideoController mNativeVideoController;

        // We need to hold a reference to the VastManager because internal VAST classes
        // hold only weak refs to this.
        @NonNull private final VastManager mVastManager;
        @Nullable VastVideoConfig mVastVideoConfig;
        @Nullable private MediaLayout mMediaLayout;
        @Nullable private View mRootView;

        private final long mId;
        private boolean mNeedsSeek;
        private boolean mNeedsPrepare;
        private boolean mPauseCanBeTracked = false;
        private boolean mResumeCanBeTracked = false;

        // These variables influence video state.
        private int mLatestVideoControllerState;
        private boolean mError;
        private boolean mLatestVisibility;
        private boolean mMuted;
        private boolean mEnded;

        public MoPubVideoNativeAd(
                @NonNull final Context context,
                @NonNull final JSONObject jsonObject,
                @NonNull final CustomEventNativeListener customEventNativeListener,
                @NonNull final VideoResponseHeaders videoResponseHeaders,
                @NonNull final String clickTrackingUrl) {
            this(context, jsonObject, customEventNativeListener, videoResponseHeaders,
                    new VisibilityTracker(context), new NativeVideoControllerFactory(),
                    clickTrackingUrl, VastManagerFactory.create(context.getApplicationContext(), false));
        }

        @VisibleForTesting
        MoPubVideoNativeAd(
                @NonNull final Context context,
                @NonNull final JSONObject jsonObject,
                @NonNull final CustomEventNativeListener customEventNativeListener,
                @NonNull final VideoResponseHeaders videoResponseHeaders,
                @NonNull final VisibilityTracker visibilityTracker,
                @NonNull final NativeVideoControllerFactory nativeVideoControllerFactory,
                @NonNull final String clickTrackingUrl,
                @NonNull final VastManager vastManager) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(jsonObject);
            Preconditions.checkNotNull(customEventNativeListener);
            Preconditions.checkNotNull(videoResponseHeaders);
            Preconditions.checkNotNull(visibilityTracker);
            Preconditions.checkNotNull(nativeVideoControllerFactory);
            Preconditions.checkNotNull(clickTrackingUrl);
            Preconditions.checkNotNull(vastManager);

            mContext = context.getApplicationContext();
            mJsonObject = jsonObject;
            mCustomEventNativeListener = customEventNativeListener;
            mVideoResponseHeaders = videoResponseHeaders;

            mNativeVideoControllerFactory = nativeVideoControllerFactory;
            mMoPubClickTrackingUrl = clickTrackingUrl;

            mId = Utils.generateUniqueId();
            mNeedsSeek = true;
            mVideoState = VideoState.CREATED;

            mNeedsPrepare = true;
            mLatestVideoControllerState = NativeVideoController.STATE_IDLE;
            mMuted = true;
            mVideoVisibleTracking = visibilityTracker;
            mVideoVisibleTracking.setVisibilityTrackerListener(new VisibilityTracker
                    .VisibilityTrackerListener() {
                @Override
                public void onVisibilityChanged(final List<View> visibleViews,
                        final List<View> invisibleViews) {
                    if (!visibleViews.isEmpty() && !mLatestVisibility) { // State transition
                        mLatestVisibility = true;
                        maybeChangeState();
                    } else if (!invisibleViews.isEmpty() && mLatestVisibility) { // state transition
                        mLatestVisibility = false;
                        maybeChangeState();
                    }
                }
            });
            mVastManager = vastManager;
        }

        void loadAd() throws IllegalArgumentException {
            if (!containsRequiredKeys(mJsonObject)) {
                throw new IllegalArgumentException("JSONObject did not contain required keys.");
            }

            final Iterator<String> keys = mJsonObject.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                final Parameter parameter = Parameter.from(key);

                if (parameter != null) {
                    try {
                        addInstanceVariable(parameter, mJsonObject.opt(key));
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("JSONObject key (" + key
                                + ") contained unexpected value.");
                    }
                } else {
                    addExtra(key, mJsonObject.opt(key));
                }
            }
            setPrivacyInformationIconClickThroughUrl(PRIVACY_INFORMATION_CLICKTHROUGH_URL);

            preCacheImages(mContext, getAllImageUrls(), new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    mVastManager.prepareVastVideoConfiguration(getVastVideo(),
                            MoPubVideoNativeAd.this,
                            null,
                            mContext);
                }

                @Override
                public void onImagesFailedToCache(final NativeErrorCode errorCode) {
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });
        }

        @Override
        public void onVastVideoConfigurationPrepared(@Nullable VastVideoConfig vastVideoConfig) {
            if (vastVideoConfig == null) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
                return;
            }

            final List<NativeVideoController.VisibilityTrackingEvent> visibilityTrackingEvents =
                    new ArrayList<VisibilityTrackingEvent>();

            // Custom visibility tracking event from http response headers
            final VisibilityTrackingEvent visibilityTrackingEvent = new VisibilityTrackingEvent();
            visibilityTrackingEvent.strategy = new HeaderVisibilityStrategy(this);
            visibilityTrackingEvent.minimumPercentageVisible =
                    mVideoResponseHeaders.getImpressionMinVisiblePercent();
            visibilityTrackingEvent.totalRequiredPlayTimeMs =
                    mVideoResponseHeaders.getImpressionVisibleMs();
            visibilityTrackingEvents.add(visibilityTrackingEvent);
            visibilityTrackingEvent.minimumVisiblePx =
                    mVideoResponseHeaders.getImpressionVisiblePx();

            // VAST impression trackers
            for (final VastTracker vastTracker : vastVideoConfig.getImpressionTrackers()) {
                final VisibilityTrackingEvent vastImpressionTrackingEvent =
                        new VisibilityTrackingEvent();
                vastImpressionTrackingEvent.strategy = new PayloadVisibilityStrategy(mContext,
                        vastTracker.getContent());
                vastImpressionTrackingEvent.minimumPercentageVisible =
                        mVideoResponseHeaders.getImpressionMinVisiblePercent();
                vastImpressionTrackingEvent.totalRequiredPlayTimeMs =
                        mVideoResponseHeaders.getImpressionVisibleMs();
                visibilityTrackingEvents.add(vastImpressionTrackingEvent);
                vastImpressionTrackingEvent.minimumVisiblePx =
                        mVideoResponseHeaders.getImpressionVisiblePx();
            }

            // Visibility tracking event from http response Vast payload
            mVastVideoConfig = vastVideoConfig;
            final VideoViewabilityTracker vastVideoViewabilityTracker =
                    mVastVideoConfig.getVideoViewabilityTracker();
            if (vastVideoViewabilityTracker != null) {
                final VisibilityTrackingEvent vastVisibilityTrackingEvent =
                        new VisibilityTrackingEvent();
                vastVisibilityTrackingEvent.strategy =
                        new PayloadVisibilityStrategy(mContext,
                                vastVideoViewabilityTracker.getContent());
                vastVisibilityTrackingEvent.minimumPercentageVisible =
                        vastVideoViewabilityTracker.getPercentViewable();
                vastVisibilityTrackingEvent.totalRequiredPlayTimeMs =
                        vastVideoViewabilityTracker.getViewablePlaytimeMS();
                visibilityTrackingEvents.add(vastVisibilityTrackingEvent);
            }

            Set<String> clickTrackers = new HashSet<String>();
            clickTrackers.add(mMoPubClickTrackingUrl);
            clickTrackers.addAll(getClickTrackers());

            final ArrayList<VastTracker> vastClickTrackers = new ArrayList<VastTracker>();
            for (String clickTrackingUrl : clickTrackers) {
                vastClickTrackers.add(new VastTracker(clickTrackingUrl, false));
            }
            mVastVideoConfig.addClickTrackers(vastClickTrackers);

            // Always use click destination URL from JSON "clk" value instead of from VAST document
            mVastVideoConfig.setClickThroughUrl(getClickDestinationUrl());

            mNativeVideoController = mNativeVideoControllerFactory.createForId(
                    mId, mContext, visibilityTrackingEvents, mVastVideoConfig);

            mCustomEventNativeListener.onNativeAdLoaded(this);

            // Internal Video Trackers
            final JSONObject videoTrackers = mVideoResponseHeaders.getVideoTrackers();
            if (videoTrackers != null) {
                mVastVideoConfig.addVideoTrackers(videoTrackers);
            }
        }

        private boolean containsRequiredKeys(@NonNull final JSONObject jsonObject) {
            Preconditions.checkNotNull(jsonObject);

            final Set<String> keys = new HashSet<String>();
            final Iterator<String> jsonKeys = jsonObject.keys();
            while (jsonKeys.hasNext()) {
                keys.add(jsonKeys.next());
            }

            return keys.containsAll(Parameter.requiredKeys);
        }

        private void addInstanceVariable(@NonNull final Parameter key,
                @Nullable final Object value) throws ClassCastException {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(value);

            try {
                switch (key) {
                    case IMPRESSION_TRACKER:
                        addImpressionTrackers(value);
                        break;
                    case TITLE:
                        setTitle((String) value);
                        break;
                    case TEXT:
                        setText((String) value);
                        break;
                    case IMAGE_URL:
                        setMainImageUrl((String) value);
                        break;
                    case ICON_URL:
                        setIconImageUrl((String) value);
                        break;
                    case CLICK_DESTINATION:
                        setClickDestinationUrl((String) value);
                        break;
                    case CLICK_TRACKER:
                        parseClickTrackers(value);
                        break;
                    case CALL_TO_ACTION:
                        setCallToAction((String) value);
                        break;
                    case VAST_VIDEO:
                        setVastVideo((String) value);
                        break;
                    default:
                        MoPubLog.d("Unable to add JSON key to internal mapping: " + key.mName);
                        break;
                }
            } catch (ClassCastException e) {
                if (!key.mRequired) {
                    MoPubLog.d("Ignoring class cast exception for optional key: " + key.mName);
                } else {
                    throw e;
                }
            }
        }

        private void parseClickTrackers(@NonNull final Object clickTrackers) {
            if (clickTrackers instanceof JSONArray) {
                addClickTrackers(clickTrackers);
            } else {
                addClickTracker((String) clickTrackers);
            }
        }

        @Override
        public void render(@NonNull MediaLayout mediaLayout) {
            Preconditions.checkNotNull(mediaLayout);

            mVideoVisibleTracking.addView(mRootView,
                    mediaLayout,
                    mVideoResponseHeaders.getPlayVisiblePercent(),
                    mVideoResponseHeaders.getPauseVisiblePercent(),
                    mVideoResponseHeaders.getImpressionVisiblePx());

            mMediaLayout = mediaLayout;
            mMediaLayout.initForVideo();

            mMediaLayout.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int
                        height) {

                    mNativeVideoController.setListener(MoPubVideoNativeAd.this);
                    mNativeVideoController.setOnAudioFocusChangeListener(MoPubVideoNativeAd.this);
                    mNativeVideoController.setProgressListener(MoPubVideoNativeAd.this);
                    mNativeVideoController.setTextureView(mMediaLayout.getTextureView());
                    mMediaLayout.resetProgress();

                    // If we're returning to an ended video, make a note of that so we don't flash
                    // a bunch of UI changes while we prepare the data.
                    final long duration = mNativeVideoController.getDuration();
                    final long currentPosition = mNativeVideoController.getCurrentPosition();
                    if (mLatestVideoControllerState == NativeVideoController.STATE_ENDED
                        || (duration > 0 && duration - currentPosition < NativeVideoController.RESUME_FINISHED_THRESHOLD)) {
                        mEnded = true;
                    }

                    if (mNeedsPrepare) {
                        mNeedsPrepare = false;
                        mNativeVideoController.prepare(MoPubVideoNativeAd.this);
                    }

                    mNeedsSeek = true;
                    maybeChangeState();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                        int height) { }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
                    mNeedsPrepare = true;
                    mNativeVideoController.release(MoPubVideoNativeAd.this);
                    applyState(VideoState.PAUSED);
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
            });

            mMediaLayout.setPlayButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mMediaLayout.resetProgress();
                    mNativeVideoController.seekTo(0);
                    mEnded = false;
                    mNeedsSeek = false;
                }
            });

            mMediaLayout.setMuteControlClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    mMuted = !mMuted;
                    maybeChangeState();
                }
            });

            mMediaLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    prepareToLeaveView();
                    mNativeVideoController.triggerImpressionTrackers();
                    MraidVideoPlayerActivity.startNativeVideo(mContext, mId, mVastVideoConfig);
                }
            });

            if (mNativeVideoController.getPlaybackState() == NativeVideoController.STATE_CLEARED) {
                mNativeVideoController.prepare(this);
            }

            applyState(VideoState.PAUSED);
        }

        // Lifecycle Handlers
        @Override
        public void prepare(@NonNull final View view) {
            Preconditions.checkNotNull(view);
            mRootView = view;
            mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prepareToLeaveView();
                    // No need to call notifyAdClicked since handleCtaClick does clickTracking
                    mNativeVideoController.triggerImpressionTrackers();
                    mNativeVideoController.handleCtaClick(mContext);
                }
            });
        }

        @Override
        public void clear(@NonNull final View view) {
            Preconditions.checkNotNull(view);
            mNativeVideoController.clear();
            cleanUpMediaLayout();
        }

        @Override
        public void destroy() {
            cleanUpMediaLayout();
            mNativeVideoController.setPlayWhenReady(false);
            mNativeVideoController.release(this);
            NativeVideoController.remove(mId);
            mVideoVisibleTracking.destroy();
        }

        @Override
        public void onStateChanged(final boolean playWhenReady, final int playbackState) {
            mLatestVideoControllerState = playbackState;
            maybeChangeState();
        }

        @Override
        public void onError(final Exception e) {
            MoPubLog.w("Error playing back video.", e);
            mError = true;
            maybeChangeState();
        }

        @Override
        public void updateProgress(final int progressTenthPercent) {
            mMediaLayout.updateProgress(progressTenthPercent);
        }

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Mute the video
                mMuted = true;
                maybeChangeState();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
                mNativeVideoController.setAudioVolume(0.3f);
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
                mNativeVideoController.setAudioVolume(1.0f);
                maybeChangeState();
            }
        }

        private void cleanUpMediaLayout() {
            // When clearing, we also clear medialayout references so if we're rendered again
            // with the same view, we reset the video state correctly.
            if (mMediaLayout != null) {
                mMediaLayout.setMode(MediaLayout.Mode.IMAGE);
                mMediaLayout.setSurfaceTextureListener(null);
                mMediaLayout.setPlayButtonClickListener(null);
                mMediaLayout.setMuteControlClickListener(null);
                mMediaLayout.setOnClickListener(null);
                mVideoVisibleTracking.removeView(mMediaLayout);
                mMediaLayout = null;
            }
        }

        private void prepareToLeaveView() {
            mNeedsSeek = true;
            mNeedsPrepare = true;

            // Clean up any references to this class when storing the NativeVideoController
            // in a static location and starting a new activity
            mNativeVideoController.setListener(null);
            mNativeVideoController.setOnAudioFocusChangeListener(null);
            mNativeVideoController.setProgressListener(null);
            mNativeVideoController.clear();

            applyState(VideoState.PAUSED, true);
        }

        private void maybeChangeState() {
            VideoState newState = mVideoState;

            if (mError) {
                newState = VideoState.FAILED_LOAD;
            } else if (mEnded) {
                newState = VideoState.ENDED;
            } else {
                if (mLatestVideoControllerState == NativeVideoController.STATE_IDLE) {
                    newState = VideoState.LOADING;
                } else if (mLatestVideoControllerState == NativeVideoController.STATE_BUFFERING) {
                    newState = VideoState.BUFFERING;
                } else if (mLatestVideoControllerState == NativeVideoController.STATE_ENDED) {
                    mEnded = true;
                    newState = VideoState.ENDED;
                } else if (mLatestVideoControllerState == NativeVideoController.STATE_READY) {
                    if (mLatestVisibility) {
                        newState = mMuted ? VideoState.PLAYING_MUTED : VideoState.PLAYING;
                    } else {
                        newState = VideoState.PAUSED;
                    }
                }
            }

            applyState(newState);
        }

        @VisibleForTesting
        void applyState(@NonNull final VideoState videoState) {
            applyState(videoState, false);
        }

        @VisibleForTesting
        void applyState(@NonNull final VideoState videoState, boolean transitionToFullScreen) {
            Preconditions.checkNotNull(videoState);

            // Ignore the state change if video player is not ready to take state changes.
            if (mVastVideoConfig == null || mNativeVideoController == null || mMediaLayout == null) {
                return;
            }

            // Check and set mVideoState so any changes we make to exo state don't
            // trigger a duplicate run of this.
            if (mVideoState == videoState) {
                return;
            }
            VideoState previousState = mVideoState;
            mVideoState = videoState;

            switch (videoState) {
                case FAILED_LOAD:
                    mVastVideoConfig.handleError(mContext, null, 0);
                    mNativeVideoController.setAppAudioEnabled(false);
                    mMediaLayout.setMode(MediaLayout.Mode.IMAGE);
                    break;
                case CREATED:
                case LOADING:
                    mNativeVideoController.setPlayWhenReady(true);
                    mMediaLayout.setMode(MediaLayout.Mode.LOADING);
                    break;
                case BUFFERING:
                    mNativeVideoController.setPlayWhenReady(true);
                    mMediaLayout.setMode(MediaLayout.Mode.BUFFERING);
                    break;
                case PAUSED:
                    if (transitionToFullScreen) {
                        // Prevents firing resume trackers when we return from full-screen.
                        mResumeCanBeTracked = false;
                    }

                    if (!transitionToFullScreen) {
                        mNativeVideoController.setAppAudioEnabled(false);
                        if (mPauseCanBeTracked) {
                            TrackingRequest.makeVastTrackingHttpRequest(
                                    mVastVideoConfig.getPauseTrackers(),
                                    null, // VastErrorCode
                                    (int) mNativeVideoController.getCurrentPosition(),
                                    null, // Asset URI
                                    mContext);
                            mPauseCanBeTracked = false;
                            mResumeCanBeTracked = true;
                        }
                    }
                    mNativeVideoController.setPlayWhenReady(false);
                    mMediaLayout.setMode(MediaLayout.Mode.PAUSED);
                    break;
                case PLAYING:
                    handleResumeTrackersAndSeek(previousState);

                    mNativeVideoController.setPlayWhenReady(true);
                    mNativeVideoController.setAudioEnabled(true);
                    mNativeVideoController.setAppAudioEnabled(true);
                    mMediaLayout.setMode(MediaLayout.Mode.PLAYING);
                    mMediaLayout.setMuteState(MediaLayout.MuteState.UNMUTED);
                    break;
                case PLAYING_MUTED:
                    handleResumeTrackersAndSeek(previousState);

                    mNativeVideoController.setPlayWhenReady(true);
                    mNativeVideoController.setAudioEnabled(false);
                    mNativeVideoController.setAppAudioEnabled(false);
                    mMediaLayout.setMode(MediaLayout.Mode.PLAYING);
                    mMediaLayout.setMuteState(MediaLayout.MuteState.MUTED);
                    break;
                case ENDED:
                    if (mNativeVideoController.hasFinalFrame()) {
                        mMediaLayout.setMainImageDrawable(mNativeVideoController.getFinalFrame());
                    }
                    mPauseCanBeTracked = false;
                    mResumeCanBeTracked = false;
                    mVastVideoConfig.handleComplete(mContext, 0);
                    mNativeVideoController.setAppAudioEnabled(false);
                    mMediaLayout.setMode(MediaLayout.Mode.FINISHED);
                    mMediaLayout.updateProgress(1000);
                    break;
            }
        }

        private void handleResumeTrackersAndSeek(VideoState previousState) {
            if (mResumeCanBeTracked
                    && previousState != VideoState.PLAYING
                    && previousState != VideoState.PLAYING_MUTED) {  // If we've played before, fire resume trackers.
                TrackingRequest.makeVastTrackingHttpRequest(
                        mVastVideoConfig.getResumeTrackers(),
                        null, // VastErrorCode
                        (int) mNativeVideoController.getCurrentPosition(),
                        null, // Asset URI
                        mContext
                );
                mResumeCanBeTracked = false;
            }

            mPauseCanBeTracked = true;

            // We force a seek here to get keyframe rendering in ExtractorSampleSource.
            if (mNeedsSeek) {
                mNeedsSeek = false;
                mNativeVideoController.seekTo(mNativeVideoController.getCurrentPosition());
            }
        }


        private boolean isImageKey(@Nullable final String name) {
            return name != null && name.toLowerCase(Locale.US).endsWith("image");
        }

        @NonNull
        private List<String> getExtrasImageUrls() {
            final List<String> extrasBitmapUrls = new ArrayList<String>(getExtras().size());
            for (final Map.Entry<String, Object> entry : getExtras().entrySet()) {
                if (isImageKey(entry.getKey()) && entry.getValue() instanceof String) {
                    extrasBitmapUrls.add((String) entry.getValue());
                }
            }

            return extrasBitmapUrls;
        }

        @NonNull
        private List<String> getAllImageUrls() {
            final List<String> imageUrls = new ArrayList<String>();
            if (getMainImageUrl() != null) {
                imageUrls.add(getMainImageUrl());
            }
            if (getIconImageUrl() != null) {
                imageUrls.add(getIconImageUrl());
            }

            imageUrls.addAll(getExtrasImageUrls());
            return imageUrls;
        }

        @Deprecated
        @VisibleForTesting
        boolean needsPrepare() {
            return mNeedsPrepare;
        }

        @Deprecated
        @VisibleForTesting
        boolean hasEnded() {
            return mEnded;
        }

        @Deprecated
        @VisibleForTesting
        boolean needsSeek() {
            return mNeedsSeek;
        }

        @Deprecated
        @VisibleForTesting
        boolean isMuted() {
            return mMuted;
        }

        @Deprecated
        @VisibleForTesting
        long getId() {
            return mId;
        }

        @Deprecated
        @VisibleForTesting
        VideoState getVideoState() {
            return mVideoState;
        }

        @Deprecated
        @VisibleForTesting
        void setLatestVisibility(boolean latestVisibility) {
            mLatestVisibility = latestVisibility;
        }

        @Deprecated
        @VisibleForTesting
        void setMuted(boolean muted) {
            mMuted = muted;
        }

        @Deprecated
        @VisibleForTesting
        MediaLayout getMediaLayout() {
            return mMediaLayout;
        }
    }

    @VisibleForTesting
    static class HeaderVisibilityStrategy implements VisibilityTrackingEvent.OnTrackedStrategy {
        @NonNull private final WeakReference<MoPubVideoNativeAd> mMoPubVideoNativeAd;

        HeaderVisibilityStrategy(@NonNull final MoPubVideoNativeAd moPubVideoNativeAd) {
            mMoPubVideoNativeAd = new WeakReference<MoPubVideoNativeAd>(moPubVideoNativeAd);
        }

        @Override
        public void execute() {
            final MoPubVideoNativeAd moPubVideoNativeAd = mMoPubVideoNativeAd.get();
            if (moPubVideoNativeAd != null) {
                moPubVideoNativeAd.notifyAdImpressed();
            }
        }
    }

    @VisibleForTesting
    static class PayloadVisibilityStrategy implements VisibilityTrackingEvent.OnTrackedStrategy {
        @NonNull private final Context mContext;
        @NonNull private final String mUrl;

        PayloadVisibilityStrategy(@NonNull final Context context, @NonNull final String url) {
            mContext = context.getApplicationContext();
            mUrl = url;
        }

        @Override
        public void execute() {
            TrackingRequest.makeTrackingHttpRequest(mUrl, mContext);
        }
    }

    /**
     * Created purely for the purpose of mocking to ease testing.
     */
    @VisibleForTesting
    static class NativeVideoControllerFactory {
        public NativeVideoController createForId(final long id,
                @NonNull final Context context,
                @NonNull final List<VisibilityTrackingEvent> visibilityTrackingEvents,
                @NonNull final VastVideoConfig vastVideoConfig) {
            return NativeVideoController.createForId(id, context, visibilityTrackingEvents,
                    vastVideoConfig);
        }
    }

    @VisibleForTesting
    static class VideoResponseHeaders {
        private boolean mHeadersAreValid;
        private int mPlayVisiblePercent;
        private int mPauseVisiblePercent;
        private int mImpressionMinVisiblePercent;
        private int mImpressionVisibleMs;
        private int mMaxBufferMs;
        private Integer mImpressionVisiblePx;
        private JSONObject mVideoTrackers;

        VideoResponseHeaders(@NonNull final Map<String, String> serverExtras) {
            try {
                mPlayVisiblePercent = Integer.parseInt(serverExtras.get(PLAY_VISIBLE_PERCENT));
                mPauseVisiblePercent = Integer.parseInt(serverExtras.get(PAUSE_VISIBLE_PERCENT));
                mImpressionVisibleMs = Integer.parseInt(serverExtras.get(IMPRESSION_VISIBLE_MS));
                mMaxBufferMs = Integer.parseInt(serverExtras.get(MAX_BUFFER_MS));
                mHeadersAreValid = true;
            } catch (NumberFormatException e) {
                mHeadersAreValid = false;
            }

            final String impressionVisiblePxString = serverExtras.get(IMPRESSION_MIN_VISIBLE_PX);
            if (!TextUtils.isEmpty(impressionVisiblePxString)) {
                try {
                    mImpressionVisiblePx = Integer.parseInt(impressionVisiblePxString);
                } catch (NumberFormatException e) {
                    MoPubLog.d("Unable to parse impression min visible px from server extras.");
                }
            }
            try {
                mImpressionMinVisiblePercent =
                        Integer.parseInt(serverExtras.get(IMPRESSION_MIN_VISIBLE_PERCENT));
            } catch (NumberFormatException e) {
                MoPubLog.d("Unable to parse impression min visible percent from server extras.");
                if (mImpressionVisiblePx == null || mImpressionVisiblePx < 0) {
                    mHeadersAreValid = false;
                }
            }


            final String videoTrackers = serverExtras.get(VIDEO_TRACKERS_KEY);
            if (TextUtils.isEmpty(videoTrackers)) {
                return;
            }

            try {
                mVideoTrackers = new JSONObject(videoTrackers);
            } catch (JSONException e) {
                MoPubLog.d("Failed to parse video trackers to JSON: " + videoTrackers, e);
                mVideoTrackers = null;
            }
        }

        boolean hasValidHeaders() {
            return mHeadersAreValid;
        }

        int getPlayVisiblePercent() {
            return mPlayVisiblePercent;
        }

        int getPauseVisiblePercent() {
            return mPauseVisiblePercent;
        }

        int getImpressionMinVisiblePercent() {
            return mImpressionMinVisiblePercent;
        }

        int getImpressionVisibleMs() {
            return mImpressionVisibleMs;
        }

        int getMaxBufferMs() {
            return mMaxBufferMs;
        }

        @Nullable
        Integer getImpressionVisiblePx() {
            return mImpressionVisiblePx;
        }

        JSONObject getVideoTrackers() {
            return mVideoTrackers;
        }
    }
}
