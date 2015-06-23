package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdUrlGenerator;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.LocationService;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.MoPubCollections;
import com.mopub.common.util.Reflection;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 */
public class MoPubRewardedVideoManager {
    private static MoPubRewardedVideoManager sInstance;
    private static final int DEFAULT_LOAD_TIMEOUT = Constants.THIRTY_SECONDS_MILLIS;

    @NonNull private final Handler mCallbackHandler;
    @NonNull private WeakReference<Activity> mMainActivity;
    @NonNull private final Context mContext;
    @NonNull private final AdRequestStatusMapping mAdRequestStatus;
    @NonNull private final RewardedVideoData mRewardedVideoData;
    @Nullable private MoPubRewardedVideoListener mVideoListener;
    
    @NonNull private final Set<MediationSettings> mGlobalMediationSettings;
    @NonNull private final Map<String, Set<MediationSettings>> mInstanceMediationSettings;

    @NonNull private final Handler mCustomEventTimeoutHandler;
    @NonNull private final Map<String, Runnable> mTimeoutMap;

    public static class RewardedVideoRequestListener implements AdRequest.Listener {
        public final String adUnitId;
        private final MoPubRewardedVideoManager mVideoManager;

        public RewardedVideoRequestListener(MoPubRewardedVideoManager videoManager, String adUnitId) {
            this.adUnitId = adUnitId;
            this.mVideoManager = videoManager;
        }

        @Override
        public void onSuccess(final AdResponse response) {
            mVideoManager.onAdSuccess(response, adUnitId);
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            mVideoManager.onAdError(volleyError, adUnitId);
        }
    }

    private MoPubRewardedVideoManager(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        mMainActivity = new WeakReference<Activity>(mainActivity);
        mContext = mainActivity.getApplicationContext();
        mRewardedVideoData = new RewardedVideoData();
        mCallbackHandler = new Handler(Looper.getMainLooper());
        mGlobalMediationSettings = new HashSet<MediationSettings>();
        MoPubCollections.addAllNonNull(mGlobalMediationSettings, mediationSettings);
        mInstanceMediationSettings = new HashMap<String, Set<MediationSettings>>();
        mCustomEventTimeoutHandler = new Handler();
        mTimeoutMap = new HashMap<String, Runnable>();
        
        mAdRequestStatus = new AdRequestStatusMapping();
    }

    public static synchronized void init(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        if (sInstance == null) {
            sInstance = new MoPubRewardedVideoManager(mainActivity, mediationSettings);
        } else {
            MoPubLog.e("Tried to call initializeRewardedVideo more than once. Only the first " +
                    "initialization call has any effect.");
        }
    }

    public static void updateActivity(@NonNull Activity activity) {
        if (sInstance != null) {
            sInstance.mMainActivity = new WeakReference<Activity>(activity);
        } else {
            logErrorNotInitialized();
        }
    }

    /**
     * Returns a global {@link MediationSettings} object of the type 'clazz', if one is registered.
     * This method will only return an object if its type is identical to 'clazz', not if it is a
     * subtype.
     *
     * @param clazz the exact Class of the {@link MediationSettings} instance to retrieve
     * @return an instance of Class<T> or null if none is registered.
     */
    @Nullable
    public static <T extends MediationSettings> T getGlobalMediationSettings(@NonNull final Class<T> clazz) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return null;
        }

        for (final MediationSettings mediationSettings : sInstance.mGlobalMediationSettings) {
            // The two classes must be of exactly equal types
            if (clazz.equals(mediationSettings.getClass())) {
                return clazz.cast(mediationSettings);
            }
        }

        return null;
    }

    /**
     * Returns an instance {@link MediationSettings} object of the type 'clazz', if one is
     * registered. This method will only return an object if its type is identical to 'clazz', not
     * if it is a subtype.
     *
     * @param clazz the exact Class of the {@link MediationSettings} instance to retrieve
     * @param adUnitId String identifier used to obtain the appropriate instance MediationSettings
     * @return an instance of Class<T> or null if none is registered.
     */
    @Nullable
    public static <T extends MediationSettings> T getInstanceMediationSettings(
            @NonNull final Class<T> clazz, @NonNull final String adUnitId) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return null;
        }

        final Set<MediationSettings> instanceMediationSettings =
                sInstance.mInstanceMediationSettings.get(adUnitId);
        if (instanceMediationSettings == null) {
            return null;
        }

        for (final MediationSettings mediationSettings : instanceMediationSettings) {
            // The two classes must be of exactly equal types
            if (clazz.equals(mediationSettings.getClass())) {
                return clazz.cast(mediationSettings);
            }
        }

        return null;
    }

    /**
     * Sets the {@link MoPubRewardedVideoListener} that will receive events from the
     * rewarded video system. Set this to null to stop receiving event callbacks.
     */
    public static void setVideoListener(@Nullable MoPubRewardedVideoListener listener) {
        if (sInstance != null) {
            sInstance.mVideoListener = listener;
        } else {
            logErrorNotInitialized();
        }
    }

    /**
     * Builds an AdRequest for the given adUnitId and adds it to the singleton RequestQueue. This
     * method will not make a new request if there is already a video loading for this adUnitId.
     *
     * @param adUnitId MoPub adUnitId String
     * @param mediationSettings Optional instance-level MediationSettings to associate with the
     *                          above adUnitId.
     */
    public static void loadVideo(@NonNull String adUnitId, @Nullable final MediationSettings... mediationSettings) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        // If any instance MediationSettings have been specified, update the internal map.
        // Note: This always clears the MediationSettings for the ad unit, whether or not any
        // MediationSettings have been provided.
        final Set<MediationSettings> newInstanceMediationSettings = new HashSet<MediationSettings>();
        MoPubCollections.addAllNonNull(newInstanceMediationSettings, mediationSettings);
        sInstance.mInstanceMediationSettings.put(adUnitId, newInstanceMediationSettings);

        final AdUrlGenerator urlGenerator = new WebViewAdUrlGenerator(sInstance.mContext, false);
        final String adUrlString = urlGenerator.withAdUnitId(adUnitId)
                .withLocation(
                        LocationService.getLastKnownLocation(
                                sInstance.mContext,
                                MoPub.getLocationPrecision(),
                                MoPub.getLocationAwareness()
                        )
                )
                .generateUrlString(Constants.HOST);

        loadVideo(adUnitId, adUrlString);
    }

    private static void loadVideo(@NonNull String adUnitId, @NonNull String adUrlString) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        if (sInstance.mAdRequestStatus.isLoading(adUnitId)) {
            MoPubLog.d(String.format(Locale.US, "Did not queue rewarded video request for ad " +
                    "unit %s. A request is already pending.", adUnitId));
            return;
        }

        // Issue MoPub request
        final AdRequest request = new AdRequest(
                adUrlString,
                AdFormat.REWARDED_VIDEO,
                adUnitId,
                sInstance.mContext,
                new RewardedVideoRequestListener(sInstance, adUnitId)
        );
        final RequestQueue requestQueue = Networking.getRequestQueue(sInstance.mContext);
        requestQueue.add(request);
        sInstance.mAdRequestStatus.markLoading(adUnitId);
    }

    public static boolean hasVideo(@NonNull String adUnitId) {
        if (sInstance != null) {
            final CustomEventRewardedVideo customEvent = sInstance.mRewardedVideoData.getCustomEvent(adUnitId);
            return isPlayable(adUnitId, customEvent);
        } else {
            logErrorNotInitialized();
            return false;
        }
    }

    public static void showVideo(@NonNull String adUnitId) {
        if (sInstance != null) {
            final CustomEventRewardedVideo customEvent = sInstance.mRewardedVideoData.getCustomEvent(adUnitId);
            if (isPlayable(adUnitId, customEvent)) {
                sInstance.mAdRequestStatus.markPlayed(adUnitId);
                customEvent.showVideo();
            } else {
                sInstance.failover(adUnitId, MoPubErrorCode.VIDEO_NOT_AVAILABLE);
            }
        } else {
            logErrorNotInitialized();
        }
    }

    private static boolean isPlayable(String adUnitId, @Nullable CustomEventRewardedVideo customEvent) {
        return (sInstance != null
                && sInstance.mAdRequestStatus.canPlay(adUnitId)
                && customEvent != null
                && customEvent.hasVideoAvailable());
    }

    ///// Ad Request / Response methods /////
    private void onAdSuccess(AdResponse adResponse, String adUnitId) {
        mAdRequestStatus.markLoaded(adUnitId,
                adResponse.getFailoverUrl(),
                adResponse.getImpressionTrackingUrl(),
                adResponse.getClickTrackingUrl());

        Integer timeoutMillis = adResponse.getAdTimeoutMillis();
        if (timeoutMillis == null || timeoutMillis <= 0) {
            timeoutMillis = DEFAULT_LOAD_TIMEOUT;
        }

        final String customEventClassName = adResponse.getCustomEventClassName();
        if (customEventClassName == null) {
            MoPubLog.e("Couldn't create custom event, class name was null.");
            failover(adUnitId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        try {
            // Instantiate a custom event
            final CustomEventRewardedVideo customEvent =
                    Reflection.instantiateClassWithEmptyConstructor(
                            customEventClassName,
                            CustomEventRewardedVideo.class);

            // Put important data into localExtras...
            final Map<String, Object> localExtras = new TreeMap<String, Object>();
            localExtras.put(DataKeys.AD_UNIT_ID_KEY, adUnitId);

            Activity mainActivity = mMainActivity.get();
            if (mainActivity == null) {
                MoPubLog.d("Could not load custom event because Activity reference was null. Call" +
                        " MoPub#updateActivity before requesting more rewarded videos.");

                // Don't go through the ordinary failover process since we have
                // no activity for the failover to use.
                mAdRequestStatus.markFail(adUnitId);
                return;
            }

            // Set up timeout calls.
            Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    MoPubLog.d("Custom Event failed to load rewarded video in a timely fashion.");
                    onRewardedVideoLoadFailure(customEvent.getClass(), customEvent.getAdNetworkId(),
                            MoPubErrorCode.NETWORK_TIMEOUT);
                    customEvent.onInvalidate();
                }
            };
            mCustomEventTimeoutHandler.postDelayed(timeout, timeoutMillis);
            mTimeoutMap.put(adUnitId, timeout);

            // Load custom event
            customEvent.loadCustomEvent(mainActivity, localExtras, adResponse.getServerExtras());

            final CustomEventRewardedVideo.CustomEventRewardedVideoListener listener =
                    customEvent.getVideoListenerForSdk();
            final String adNetworkId = customEvent.getAdNetworkId();
            mRewardedVideoData.updateAdUnitCustomEventMapping(adUnitId, customEvent, listener, adNetworkId);
        } catch (Exception e) {
            MoPubLog.e(String.format(Locale.US, "Couldn't create custom event with class name %s", customEventClassName));
            failover(adUnitId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    private void onAdError(@NonNull VolleyError volleyError, @NonNull String adUnitId) {
        MoPubErrorCode errorCode = MoPubErrorCode.INTERNAL_ERROR;
        if (volleyError instanceof MoPubNetworkError) {
            MoPubNetworkError err = (MoPubNetworkError) volleyError;
            switch (err.getReason()) {
                case NO_FILL:
                case WARMING_UP:
                    errorCode = MoPubErrorCode.NO_FILL;
                    break;
                case BAD_BODY:
                case BAD_HEADER_DATA:
                default:
                    errorCode = MoPubErrorCode.INTERNAL_ERROR;
            }
        }
        if (volleyError instanceof com.mopub.volley.NoConnectionError) {
            errorCode = MoPubErrorCode.NO_CONNECTION;
        }
        failover(adUnitId, errorCode);
    }

    private void failover(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        final String failoverUrl = mAdRequestStatus.getFailoverUrl(adUnitId);
        mAdRequestStatus.markFail(adUnitId);

        if (failoverUrl != null) {
            loadVideo(adUnitId, failoverUrl);
        } else if (mVideoListener != null) {
            mVideoListener.onRewardedVideoLoadFailure(adUnitId, errorCode);
        }
    }

    private void cancelTimeouts(@NonNull String moPubId) {
        final Runnable runnable = mTimeoutMap.remove(moPubId);
        if (runnable != null) {  // We can't pass null or all callbacks will be removed.
            mCustomEventTimeoutHandler.removeCallbacks(runnable);
        }
    }

    //////// Listener methods that should be called by third-party SDKs. //////////

    /**
     * Notify the manager that a rewarded video loaded successfully.
     *
     * @param customEventClass - the Class of the third-party custom event object.
     * @param thirdPartyId - the ad id of the third party SDK. This may be an empty String if the
     *                     SDK does not use ad ids, zone ids, or a analogous concept.
     * @param <T> - a class that extends {@link CustomEventRewardedVideo}. Only rewarded video
     *           custom events should use these methods.
     */
    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoLoadSuccess(@NonNull final Class<T> customEventClass, @NonNull final String thirdPartyId) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                sInstance.cancelTimeouts(moPubId);
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoLoadSuccess(moPubId);
                }
            }
        });
    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoLoadFailure(@NonNull final Class<T> customEventClass, final String thirdPartyId, final MoPubErrorCode errorCode) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                   sInstance.cancelTimeouts(moPubId);
                   sInstance.failover(moPubId, errorCode);
            }
        });
    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoStarted(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoStarted(moPubId);
                }
                TrackingRequest.makeTrackingHttpRequest(
                        sInstance.mAdRequestStatus.getImpressionTrackerUrlString(moPubId),
                        sInstance.mContext);
                sInstance.mAdRequestStatus.clearImpressionUrl(moPubId);
            }
        });
    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoPlaybackError(@NonNull final Class<T> customEventClass, final String thirdPartyId, final MoPubErrorCode errorCode) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoPlaybackError(moPubId, errorCode);
                }
            }
        });

    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoClicked(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                TrackingRequest.makeTrackingHttpRequest(
                        sInstance.mAdRequestStatus.getClickTrackerUrlString(moPubId),
                        sInstance.mContext);
                sInstance.mAdRequestStatus.clearClickUrl(moPubId);
            }
        });
    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoClosed(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoClosed(moPubId);
                }
            }
        });
    }

    public static <T extends CustomEventRewardedVideo>
    void onRewardedVideoCompleted(@NonNull final Class<T> customEventClass, final String thirdPartyId, @NonNull final MoPubReward moPubReward) {
        // Unlike other callbacks in this class, only call the listener once with all the MoPubIds in the matching set.
        postToInstance(new Runnable() {
            @Override
            public void run() {
                final Set<String> moPubIds = sInstance.mRewardedVideoData.getMoPubIdsForAdNetwork(customEventClass, thirdPartyId);
                Set<String> rewarded = new HashSet<String>(moPubIds);
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoCompleted(rewarded, moPubReward);
                }
            }
        });
    }

    /**
     * Posts the runnable to the static instance's handler. Does nothing if sInstance is null.
     * Useful for ensuring that all event callbacks run on the main thread.
     * The {@link Runnable} can assume that sInstance is non-null.
     */
    private static void postToInstance(@NonNull Runnable runnable) {
        if (sInstance != null) {
            sInstance.mCallbackHandler.post(runnable);
        }
    }

    private static void logErrorNotInitialized() {
        MoPubLog.e("MoPub rewarded video was not initialized. You must call " +
                "MoPub.initializeRewardedVideo() before loading or attempting " +
                "to play video ads.");
    }

    /**
     * A runnable that calls forEach on each member of the rewarded video data passed to the runnable.
     */
    private static abstract class ForEachMoPubIdRunnable implements Runnable {

        @NonNull private final Class<? extends CustomEventRewardedVideo> mCustomEventClass;
        @NonNull private final String mThirdPartyId;

        ForEachMoPubIdRunnable(@NonNull final Class<? extends CustomEventRewardedVideo> customEventClass,
                @NonNull final String thirdPartyId) {
            Preconditions.checkNotNull(customEventClass);
            Preconditions.checkNotNull(thirdPartyId);
            mCustomEventClass = customEventClass;
            mThirdPartyId = thirdPartyId;
        }

        protected abstract void forEach(@NonNull final String moPubId);

        @Override
        public void run() {
            final Set<String> moPubIds = sInstance.mRewardedVideoData
                    .getMoPubIdsForAdNetwork(mCustomEventClass, mThirdPartyId);
            for (String moPubId : moPubIds) {
                forEach(moPubId);
            }
        }
    }
}
