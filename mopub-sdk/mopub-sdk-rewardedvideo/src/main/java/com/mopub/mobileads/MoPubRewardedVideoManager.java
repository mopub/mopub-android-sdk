package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.AdFormat;
import com.mopub.common.AdReport;
import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.common.util.MoPubCollections;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.ReflectionTarget;
import com.mopub.common.util.Utils;
import com.mopub.network.AdRequest;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;

/**
 * Handles requesting Rewarded ads and mapping Rewarded Ad SDK settings to the CustomEvent
 * that is being loaded.
 */
public class MoPubRewardedVideoManager {
    private static MoPubRewardedVideoManager sInstance;
    @NonNull private static SharedPreferences sCustomEventSharedPrefs;
    private static final String CUSTOM_EVENT_PREF_NAME = "mopubCustomEventSettings";
    private static final int DEFAULT_LOAD_TIMEOUT = Constants.THIRTY_SECONDS_MILLIS;
    private static final String CURRENCIES_JSON_REWARDS_MAP_KEY = "rewards";
    private static final String CURRENCIES_JSON_REWARD_NAME_KEY = "name";
    private static final String CURRENCIES_JSON_REWARD_AMOUNT_KEY = "amount";
    @VisibleForTesting
    static final int CUSTOM_DATA_MAX_LENGTH_BYTES = 8192;

    /**
     * This must an integer because the backend only supports int types for api version.
     */
    public static final int API_VERSION = 1;

    @NonNull private final Handler mCallbackHandler;
    @NonNull private WeakReference<Activity> mMainActivity;
    @NonNull private final Context mContext;
    @NonNull private final AdRequestStatusMapping mAdRequestStatus;
    @NonNull private final RewardedAdData mRewardedAdData;
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

    public static final class RequestParameters {
        @Nullable public final String mKeywords;
        @Nullable public final String mUserDataKeywords;
        @Nullable public final Location mLocation;
        @Nullable public final String mCustomerId;

        public RequestParameters(@Nullable final String keywords) {
            this(keywords, null);
        }

        public RequestParameters(@Nullable final String keywords, @Nullable final String userDataKeywords) {
            this(keywords, userDataKeywords,null);
        }

        public RequestParameters(@Nullable final String keywords,
                                 @Nullable final String userDataKeywords,
                                 @Nullable final Location location) {
            this(keywords, userDataKeywords, location, null);
        }

        public RequestParameters(@Nullable final String keywords,
                                 @Nullable final String userDataKeywords,
                                 @Nullable final Location location,
                                 @Nullable final String customerId) {
            mKeywords = keywords;
            mCustomerId = customerId;

            // Only add userDataKeywords and location to RequestParameters if we are allowed to collect
            // personal information from a user
            final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();
            mUserDataKeywords = canCollectPersonalInformation ? userDataKeywords: null;
            mLocation = canCollectPersonalInformation ? location : null;
        }
    }


    private MoPubRewardedVideoManager(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        mMainActivity = new WeakReference<Activity>(mainActivity);
        mContext = mainActivity.getApplicationContext();
        mRewardedAdData = new RewardedAdData();
        mCallbackHandler = new Handler(Looper.getMainLooper());
        mGlobalMediationSettings = new HashSet<MediationSettings>();
        MoPubCollections.addAllNonNull(mGlobalMediationSettings, mediationSettings);
        mInstanceMediationSettings = new HashMap<String, Set<MediationSettings>>();
        mCustomEventTimeoutHandler = new Handler();
        mTimeoutMap = new HashMap<String, Runnable>();

        mAdRequestStatus = new AdRequestStatusMapping();

        sCustomEventSharedPrefs =
                SharedPreferencesHelper.getSharedPreferences(mContext, CUSTOM_EVENT_PREF_NAME);
    }

    @NonNull
    public static synchronized List<CustomEventRewardedVideo> initNetworks(
            @NonNull final Activity mainActivity,
            @NonNull final List<Class<? extends CustomEventRewardedVideo>> networksToInit) {
        Preconditions.checkNotNull(mainActivity);
        Preconditions.checkNotNull(networksToInit);

        if (sInstance == null) {
            logErrorNotInitialized();
            return Collections.emptyList();
        }

        // List of networks that end up getting initialized.
        List<CustomEventRewardedVideo> initializedNetworksList = new LinkedList<>();

        // Fetch saved network init settings from SharedPrefs.
        final Map<String, ?> networkInitSettings = sCustomEventSharedPrefs.getAll();
        MoPubLog.d(String.format(Locale.US, "fetched init settings for %s networks: %s",
                networkInitSettings.size(), networkInitSettings.keySet()));

        // Dedupe array of networks to init.
        final LinkedHashSet<Class<? extends CustomEventRewardedVideo>> uniqueNetworksToInit =
                new LinkedHashSet<>(networksToInit);

        for (Class<? extends CustomEventRewardedVideo> networkClass : uniqueNetworksToInit) {
            final String networkClassName = networkClass.getName();
            if (networkInitSettings.containsKey(networkClassName)) {
                try {
                    final String networkInitParamsJsonString =
                            (String) networkInitSettings.get(networkClassName);

                    final Map<String, String> networkInitParamsMap =
                            Json.jsonStringToMap(networkInitParamsJsonString);

                    final CustomEventRewardedVideo customEvent =
                            Reflection.instantiateClassWithEmptyConstructor(
                                    networkClassName,
                                    CustomEventRewardedVideo.class);

                    MoPubLog.d(String.format(Locale.US, "Initializing %s with params %s",
                            networkClassName, networkInitParamsMap));

                    customEvent.checkAndInitializeSdk(
                            mainActivity,
                            Collections.<String, Object>emptyMap(),
                            networkInitParamsMap);

                    initializedNetworksList.add(customEvent);
                } catch (Exception e) {
                    MoPubLog.e("Error fetching init settings for network " + networkClassName);
                }
            } else {
                MoPubLog.d("Init settings not found for " + networkClassName);
            }
        }

        return initializedNetworksList;
    }

    public static synchronized void init(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        if (sInstance == null) {
            sInstance = new MoPubRewardedVideoManager(mainActivity, mediationSettings);
        } else {
            MoPubLog.e("Tried to call initializeRewardedVideo more than once. Only the first " +
                    "initialization call has any effect.");
        }
    }

    @ReflectionTarget
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
     * @param requestParameters Optional RequestParameters object containing optional keywords
     *                          Optional RequestParameters object containing optional user data keywords
     *                          optional location value, and optional customer id.
     * @param mediationSettings Optional instance-level MediationSettings to associate with the
     *                          above adUnitId.
     */
    public static void loadVideo(@NonNull final String adUnitId,
            @Nullable final RequestParameters requestParameters,
            @Nullable final MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(adUnitId);

        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (adUnitId.equals(currentlyShowingAdUnitId)) {
            MoPubLog.d(String.format(Locale.US, "Did not queue rewarded ad request for ad " +
                    "unit %s. The ad is already showing.", adUnitId));
            return;
        }

        if (sInstance.mAdRequestStatus.canPlay(adUnitId)) {
            MoPubLog.d(String.format(Locale.US, "Did not queue rewarded ad request for ad " +
            "unit %s. This ad unit already finished loading and is ready to show.", adUnitId));
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    if (sInstance.mVideoListener != null) {
                        sInstance.mVideoListener.onRewardedVideoLoadSuccess(adUnitId);
                    }
                }
            });
            return;
        }


        // If any instance MediationSettings have been specified, update the internal map.
        // Note: This always clears the MediationSettings for the ad unit, whether or not any
        // MediationSettings have been provided.
        final Set<MediationSettings> newInstanceMediationSettings = new HashSet<MediationSettings>();
        MoPubCollections.addAllNonNull(newInstanceMediationSettings, mediationSettings);
        sInstance.mInstanceMediationSettings.put(adUnitId, newInstanceMediationSettings);

        final String customerId = requestParameters == null ? null : requestParameters.mCustomerId;
        if (!TextUtils.isEmpty(customerId)) {
            sInstance.mRewardedAdData.setCustomerId(customerId);
        }

        final AdUrlGenerator urlGenerator = new WebViewAdUrlGenerator(sInstance.mContext, false);
        final String adUrlString = urlGenerator.withAdUnitId(adUnitId)
                .withKeywords(requestParameters == null ? null : requestParameters.mKeywords)
                .withUserDataKeywords((requestParameters == null ||
                        !MoPub.canCollectPersonalInformation()) ? null : requestParameters.mUserDataKeywords)
                .withLocation(requestParameters == null ? null : requestParameters.mLocation)
                .generateUrlString(Constants.HOST);

        loadVideo(adUnitId, adUrlString);
    }

    private static void loadVideo(@NonNull String adUnitId, @NonNull String adUrlString) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        if (sInstance.mAdRequestStatus.isLoading(adUnitId)) {
            MoPubLog.d(String.format(Locale.US, "Did not queue rewarded ad request for ad " +
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
        MoPubLog.d(String.format(Locale.US,
                "Loading rewarded ad request for ad unit %s with URL %s", adUnitId, adUrlString));
    }

    public static boolean hasVideo(@NonNull String adUnitId) {
        if (sInstance != null) {
            final CustomEventRewardedAd customEvent = sInstance.mRewardedAdData.getCustomEvent(adUnitId);
            return isPlayable(adUnitId, customEvent);
        } else {
            logErrorNotInitialized();
            return false;
        }
    }

    public static void showVideo(@NonNull String adUnitId) {
        showVideo(adUnitId, null);
    }

    public static void showVideo(@NonNull String adUnitId,
            @Nullable String customData) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        if (customData != null && customData.length() > CUSTOM_DATA_MAX_LENGTH_BYTES) {
            MoPubLog.w(String.format(
                    Locale.US,
                    "Provided rewarded ad custom data parameter longer than supported" +
                            "(%d bytes, %d maximum)",
                    customData.length(), CUSTOM_DATA_MAX_LENGTH_BYTES));
        }

        final CustomEventRewardedAd customEvent = sInstance.mRewardedAdData.getCustomEvent(adUnitId);
        if (isPlayable(adUnitId, customEvent)) {
            // If there are rewards available but no reward is selected, fail over.
            if (!sInstance.mRewardedAdData.getAvailableRewards(adUnitId).isEmpty()
                    && sInstance.mRewardedAdData.getMoPubReward(adUnitId) == null) {
                sInstance.failover(adUnitId, MoPubErrorCode.REWARD_NOT_SELECTED);
                return;
            }

            sInstance.mRewardedAdData.updateCustomEventLastShownRewardMapping(
                    customEvent.getClass(),
                    sInstance.mRewardedAdData.getMoPubReward(adUnitId));
            sInstance.mRewardedAdData.updateAdUnitToCustomDataMapping(adUnitId, customData);
            sInstance.mRewardedAdData.setCurrentlyShowingAdUnitId(adUnitId);
            sInstance.mAdRequestStatus.markPlayed(adUnitId);
            customEvent.show();
        } else {
            if (sInstance.mAdRequestStatus.isLoading(adUnitId)) {
                MoPubLog.d("Rewarded ad is not ready to be shown yet.");
            } else {
                MoPubLog.d("No rewarded ad loading or loaded.");
            }

            sInstance.failover(adUnitId, MoPubErrorCode.VIDEO_NOT_AVAILABLE);
        }
    }

    private static boolean isPlayable(String adUnitId, @Nullable CustomEventRewardedAd customEvent) {
        return (sInstance != null
                && sInstance.mAdRequestStatus.canPlay(adUnitId)
                && customEvent != null
                && customEvent.isReady());
    }

    /**
     * Retrieves the set of available {@link MoPubReward} instance(s) for this AdUnit.
     * @param adUnitId MoPub adUnitId String
     * @return a set of {@link MoPubReward} instance(s) if available, else an empty set.
     */
    @NonNull
    public static Set<MoPubReward> getAvailableRewards(@NonNull String adUnitId) {
        if (sInstance != null) {
            return sInstance.mRewardedAdData.getAvailableRewards(adUnitId);
        } else {
            logErrorNotInitialized();
            return Collections.<MoPubReward>emptySet();
        }
    }

    /**
     * Selects the reward for this AdUnit from available {@link MoPubReward} instances.
     * If this AdUnit does not have any rewards, or if the selected reward is not available
     * for this AdUnit, then no reward will be selected for this AdUnit.
     * @param adUnitId MoPub adUnitId String
     * @param selectedReward selected {@link MoPubReward}
     */
    public static void selectReward(@NonNull String adUnitId, @NonNull MoPubReward selectedReward) {
        if (sInstance != null) {
            sInstance.mRewardedAdData.selectReward(adUnitId, selectedReward);
        } else {
            logErrorNotInitialized();
        }
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

        // We only allow one rewarded ad to be loaded at a time for each ad unit. This should
        // clear out the old rewarded ad if there already was one loaded and not played.
        final CustomEventRewardedAd oldRewardedVideo = mRewardedAdData.getCustomEvent(
                adUnitId);
        if (oldRewardedVideo != null) {
            oldRewardedVideo.onInvalidate();
        }

        try {
            // Instantiate a custom event
            final CustomEventRewardedAd customEvent =
                    Reflection.instantiateClassWithEmptyConstructor(
                            customEventClassName,
                            CustomEventRewardedAd.class);

            // Put important data into localExtras...
            final Map<String, Object> localExtras = new TreeMap<String, Object>();
            localExtras.put(DataKeys.AD_UNIT_ID_KEY, adUnitId);
            localExtras.put(DataKeys.REWARDED_AD_CURRENCY_NAME_KEY,
                    adResponse.getRewardedVideoCurrencyName());
            localExtras.put(DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY,
                    adResponse.getRewardedVideoCurrencyAmount());
            localExtras.put(DataKeys.REWARDED_AD_DURATION_KEY,
                    adResponse.getRewardedDuration());
            localExtras.put(DataKeys.SHOULD_REWARD_ON_CLICK_KEY,
                    adResponse.shouldRewardOnClick());
            localExtras.put(DataKeys.AD_REPORT_KEY,
                    new AdReport(adUnitId, ClientMetadata.getInstance(mContext), adResponse));
            localExtras.put(DataKeys.BROADCAST_IDENTIFIER_KEY, Utils.generateUniqueId());

            localExtras.put(DataKeys.REWARDED_AD_CUSTOMER_ID_KEY,
                    mRewardedAdData.getCustomerId());

            // Check for new multi-currency header X-Rewarded-Currencies.
            final String rewardedCurrencies = adResponse.getRewardedCurrencies();

            // Clear any available rewards for this AdUnit.
            mRewardedAdData.resetAvailableRewards(adUnitId);

            // Clear any reward previously selected for this AdUnit.
            mRewardedAdData.resetSelectedReward(adUnitId);

            // If the new multi-currency header doesn't exist, fallback to parsing legacy headers
            // X-Rewarded-Video-Currency-Name and X-Rewarded-Video-Currency-Amount.
            if (TextUtils.isEmpty(rewardedCurrencies)) {
                mRewardedAdData.updateAdUnitRewardMapping(adUnitId,
                        adResponse.getRewardedVideoCurrencyName(),
                        adResponse.getRewardedVideoCurrencyAmount());
            } else {
                try {
                    parseMultiCurrencyJson(adUnitId, rewardedCurrencies);
                } catch (Exception e) {
                    MoPubLog.e("Error parsing rewarded currencies JSON header: " + rewardedCurrencies);
                    failover(adUnitId, MoPubErrorCode.REWARDED_CURRENCIES_PARSING_ERROR);
                    return;
                }
            }

            mRewardedAdData.updateAdUnitToServerCompletionUrlMapping(adUnitId,
                    adResponse.getRewardedVideoCompletionUrl());

            Activity mainActivity = mMainActivity.get();
            if (mainActivity == null) {
                MoPubLog.d("Could not load custom event because Activity reference was null. Call" +
                        " MoPub#updateActivity before requesting more rewarded ads.");

                // Don't go through the ordinary failover process since we have
                // no activity for the failover to use.
                mAdRequestStatus.markFail(adUnitId);
                return;
            }

            // Set up timeout calls.
            Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    MoPubLog.d("Custom Event failed to load rewarded ad in a timely fashion.");
                    onRewardedVideoLoadFailure(customEvent.getClass(), customEvent.getAdNetworkId(),
                            MoPubErrorCode.NETWORK_TIMEOUT);
                    customEvent.onInvalidate();
                }
            };
            mCustomEventTimeoutHandler.postDelayed(timeout, timeoutMillis);
            mTimeoutMap.put(adUnitId, timeout);

            // Fetch the server extras mappings.
            final Map<String, String> serverExtras = adResponse.getServerExtras();

            // If the custom event is a third-party rewarded video, the server extras mappings
            // contain init parameters for this custom event class. Serialize the mappings into a
            // JSON string, then update SharedPreferences keying on the custom event class name.
            if (customEvent instanceof CustomEventRewardedVideo) {
                final String serverExtrasJsonString = (new JSONObject(serverExtras)).toString();

                MoPubLog.d(String.format(Locale.US,
                        "Updating init settings for custom event %s with params %s",
                        customEventClassName, serverExtrasJsonString));

                // https://github.com/robolectric/robolectric/issues/3641
                sCustomEventSharedPrefs
                        .edit()
                        .putString(customEventClassName, serverExtrasJsonString)
                        .commit();
            }

            // Load custom event
            MoPubLog.d(String.format(Locale.US,
                    "Loading custom event with class name %s", customEventClassName));
            customEvent.loadCustomEvent(mainActivity, localExtras, serverExtras);

            final String adNetworkId = customEvent.getAdNetworkId();
            mRewardedAdData.updateAdUnitCustomEventMapping(adUnitId, customEvent, adNetworkId);
        } catch (Exception e) {
            MoPubLog.e(String.format(Locale.US,
                    "Couldn't create custom event with class name %s", customEventClassName));
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

    private void parseMultiCurrencyJson(@NonNull String adUnitId,
            @NonNull String rewardedCurrencies) throws JSONException {
        /* Parse multi-currency JSON string, an example below:
            {
                "rewards": [
                    { "name": "Coins", "amount": 8 },
                    { "name": "Diamonds", "amount": 1 },
                    { "name": "Diamonds", "amount": 10 },
                    { "name": "Energy", "amount": 20 }
                ]
            }
         */

        final Map<String, String> rewardsMap = Json.jsonStringToMap(rewardedCurrencies);
        final String[] rewardsArray =
                Json.jsonArrayToStringArray(rewardsMap.get(CURRENCIES_JSON_REWARDS_MAP_KEY));

        // If there's only one reward, update adunit-to-reward mapping now
        if (rewardsArray.length == 1) {
            Map<String, String> rewardData = Json.jsonStringToMap(rewardsArray[0]);
            mRewardedAdData.updateAdUnitRewardMapping(
                    adUnitId,
                    rewardData.get(CURRENCIES_JSON_REWARD_NAME_KEY),
                    rewardData.get(CURRENCIES_JSON_REWARD_AMOUNT_KEY));
        }

        // Loop through awards array and create a set of available reward(s) for this adunit
        for (String rewardDataStr : rewardsArray) {
            Map<String, String> rewardData = Json.jsonStringToMap(rewardDataStr);
            mRewardedAdData.addAvailableReward(
                    adUnitId,
                    rewardData.get(CURRENCIES_JSON_REWARD_NAME_KEY),
                    rewardData.get(CURRENCIES_JSON_REWARD_AMOUNT_KEY));
        }
    }

    private void failover(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(errorCode);

        final String failoverUrl = mAdRequestStatus.getFailoverUrl(adUnitId);
        mAdRequestStatus.markFail(adUnitId);

        if (failoverUrl != null && !errorCode.equals(EXPIRED)) {
            loadVideo(adUnitId, failoverUrl);
        } else if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoLoadFailure(adUnitId, errorCode);
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
     * Notify the manager that a rewarded ad loaded successfully.
     *
     * @param customEventClass - the Class of the third-party custom event object.
     * @param thirdPartyId - the ad id of the third party SDK. This may be an empty String if the
     *                     SDK does not use ad ids, zone ids, or a analogous concept.
     * @param <T> - a class that extends {@link CustomEventRewardedAd}. Only rewarded ad
     *           custom events should use these methods.
     */
    public static <T extends CustomEventRewardedAd>
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

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoLoadFailure(@NonNull final Class<T> customEventClass, final String thirdPartyId, final MoPubErrorCode errorCode) {
        postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
            @Override
            protected void forEach(@NonNull final String moPubId) {
                   sInstance.cancelTimeouts(moPubId);
                   sInstance.failover(moPubId, errorCode);
            }
        });
    }

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoStarted(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
                @Override
                protected void forEach(@NonNull final String moPubId) {
                    onRewardedVideoStartedAction(moPubId);
                }
            });
        } else {
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    onRewardedVideoStartedAction(currentlyShowingAdUnitId);
                }
            });
        }
    }

    private static void onRewardedVideoStartedAction(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);
        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoStarted(adUnitId);
        }
        TrackingRequest.makeTrackingHttpRequest(
                sInstance.mAdRequestStatus.getImpressionTrackerUrlString(adUnitId),
                sInstance.mContext);
        sInstance.mAdRequestStatus.clearImpressionUrl(adUnitId);
    }

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoPlaybackError(@NonNull final Class<T> customEventClass, final String thirdPartyId, final MoPubErrorCode errorCode) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
                @Override
                protected void forEach(@NonNull final String moPubId) {
                    onRewardedVideoPlaybackErrorAction(moPubId, errorCode);
                }
            });
        } else {
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    onRewardedVideoPlaybackErrorAction(currentlyShowingAdUnitId, errorCode);
                }
            });
        }
    }

    private static void onRewardedVideoPlaybackErrorAction(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(errorCode);
        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoPlaybackError(adUnitId, errorCode);
        }
    }

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoClicked(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
                @Override
                protected void forEach(@NonNull final String moPubId) {
                    onRewardedVideoClickedAction(moPubId);
                }
            });
        } else {
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    onRewardedVideoClickedAction(currentlyShowingAdUnitId);
                }
            });
        }
    }

    private static void onRewardedVideoClickedAction(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoClicked(adUnitId);
        }

        TrackingRequest.makeTrackingHttpRequest(
                sInstance.mAdRequestStatus.getClickTrackerUrlString(adUnitId),
                sInstance.mContext);
        sInstance.mAdRequestStatus.clearClickUrl(adUnitId);
    }

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoClosed(@NonNull final Class<T> customEventClass, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachMoPubIdRunnable(customEventClass, thirdPartyId) {
                @Override
                protected void forEach(@NonNull final String moPubId) {
                    onRewardedVideoClosedAction(moPubId);
                }
            });
        } else {
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    onRewardedVideoClosedAction(currentlyShowingAdUnitId);
                }
            });
        }
        sInstance.mRewardedAdData.setCurrentlyShowingAdUnitId(null);
    }

    private static void onRewardedVideoClosedAction(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);
        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoClosed(adUnitId);
        }
    }

    public static <T extends CustomEventRewardedAd>
    void onRewardedVideoCompleted(@NonNull final Class<T> customEventClass,
            final String thirdPartyId, @NonNull final MoPubReward moPubReward) {
        // Unlike other callbacks in this class, only call the listener once with all the MoPubIds
        // in the matching set.
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();

        rewardOnClient(customEventClass, thirdPartyId, moPubReward, currentlyShowingAdUnitId);
        rewardOnServer(currentlyShowingAdUnitId);
    }

    private static void rewardOnServer(@Nullable final String currentlyShowingAdUnitId) {
        final String serverCompletionUrl = sInstance.mRewardedAdData.getServerCompletionUrl(
                currentlyShowingAdUnitId);
        if (!TextUtils.isEmpty(serverCompletionUrl)) {
            postToInstance(new Runnable() {
                @Override
                public void run() {
                    final MoPubReward reward
                            = sInstance.mRewardedAdData.getMoPubReward(currentlyShowingAdUnitId);

                    final String rewardName = (reward == null)
                            ? MoPubReward.NO_REWARD_LABEL
                            : reward.getLabel();
                    final String rewardAmount = (reward == null)
                            ? Integer.toString(MoPubReward.DEFAULT_REWARD_AMOUNT)
                            : Integer.toString(reward.getAmount());

                    final CustomEventRewardedAd customEvent =
                            sInstance.mRewardedAdData.getCustomEvent(currentlyShowingAdUnitId);
                    final String className = (customEvent == null || customEvent.getClass() == null)
                            ? null
                            : customEvent.getClass().getName();

                    final String customData = sInstance.mRewardedAdData.getCustomData(
                            currentlyShowingAdUnitId);

                    RewardedVideoCompletionRequestHandler.makeRewardedVideoCompletionRequest(
                            sInstance.mContext,
                            serverCompletionUrl,
                            sInstance.mRewardedAdData.getCustomerId(),
                            rewardName,
                            rewardAmount,
                            className,
                            customData);
                }
            });
        }
    }

    private static <T extends CustomEventRewardedAd> void rewardOnClient(
            @NonNull final Class<T> customEventClass,
            @Nullable final String thirdPartyId,
            @NonNull final MoPubReward moPubReward,
            @Nullable final String currentlyShowingAdUnitId) {
        postToInstance(new Runnable() {
            @Override
            public void run() {
                final MoPubReward chosenReward = chooseReward(
                        sInstance.mRewardedAdData.getLastShownMoPubReward(customEventClass),
                        moPubReward);

                Set<String> rewardedIds = new HashSet<String>();
                if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
                    final Set<String> moPubIds = sInstance.mRewardedAdData.getMoPubIdsForAdNetwork(
                            customEventClass, thirdPartyId);
                    rewardedIds.addAll(moPubIds);
                } else {
                    // If we know which ad unit is showing, only reward the currently showing
                    // ad unit.
                    rewardedIds.add(currentlyShowingAdUnitId);
                }

                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoCompleted(rewardedIds,
                            chosenReward);
                }
            }
        });
    }

    @VisibleForTesting
    static MoPubReward chooseReward(@Nullable final MoPubReward moPubReward, @NonNull final MoPubReward networkReward) {
        if (!networkReward.isSuccessful()) {
            return networkReward;
        }

        return moPubReward != null ? moPubReward : networkReward;
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
        MoPubLog.e("MoPub rewarded ad was not initialized. You must call " +
                "MoPub.initializeRewardedVideo() before loading or attempting " +
                "to play rewarded ads.");
    }

    /**
     * A runnable that calls forEach on each member of the rewarded ad data passed to the runnable.
     */
    private static abstract class ForEachMoPubIdRunnable implements Runnable {

        @NonNull private final Class<? extends CustomEventRewardedAd> mCustomEventClass;
        @NonNull private final String mThirdPartyId;

        ForEachMoPubIdRunnable(@NonNull final Class<? extends CustomEventRewardedAd> customEventClass,
                @NonNull final String thirdPartyId) {
            Preconditions.checkNotNull(customEventClass);
            Preconditions.checkNotNull(thirdPartyId);
            mCustomEventClass = customEventClass;
            mThirdPartyId = thirdPartyId;
        }

        protected abstract void forEach(@NonNull final String moPubId);

        @Override
        public void run() {
            final Set<String> moPubIds = sInstance.mRewardedAdData
                    .getMoPubIdsForAdNetwork(mCustomEventClass, mThirdPartyId);
            for (String moPubId : moPubIds) {
                forEach(moPubId);
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    static RewardedAdData getRewardedAdData() {
        if (sInstance != null) {
            return sInstance.mRewardedAdData;
        }
        return null;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    static AdRequestStatusMapping getAdRequestStatusMapping() {
        if (sInstance != null) {
            return sInstance.mAdRequestStatus;
        }
        return null;
    }

    @Deprecated
    @VisibleForTesting
    static void setCustomEventSharedPrefs(@NonNull SharedPreferences sharedPrefs) {
        Preconditions.checkNotNull(sharedPrefs);

        sCustomEventSharedPrefs = sharedPrefs;
    }
}
