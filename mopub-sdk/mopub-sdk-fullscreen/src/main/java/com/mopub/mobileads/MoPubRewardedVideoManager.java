// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.mopub.common.util.ReflectionTarget;
import com.mopub.common.util.Utils;
import com.mopub.network.AdResponse;
import com.mopub.network.MoPubNetworkError;
import com.mopub.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOULD_REWARD;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.mobileads.MoPubErrorCode.EXPIRED;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;

/**
 * Handles requesting Rewarded ads and mapping Rewarded Ad SDK settings to the BaseAd
 * that is being loaded.
 */
public class MoPubRewardedVideoManager {
    private static MoPubRewardedVideoManager sInstance;

    private static final String FULLSCREEN_AD_ADAPTER =
            "com.mopub.mobileads.FullscreenAdAdapter";

    @NonNull
    private static SharedPreferences sBaseAdSharedPrefs;
    private static final String CUSTOM_EVENT_PREF_NAME = "mopubBaseAdSettings";
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

    @NonNull
    private final Handler mCallbackHandler;
    @NonNull
    private WeakReference<Activity> mMainActivity;
    @NonNull
    private final Context mContext;
    @NonNull
    private final RewardedAdData mRewardedAdData;
    @Nullable
    private MoPubRewardedVideoListener mVideoListener;

    @NonNull
    private final Set<MediationSettings> mGlobalMediationSettings;
    @NonNull
    private final Map<String, Set<MediationSettings>> mInstanceMediationSettings;

    @NonNull
    private final Handler mBaseAdTimeoutHandler;
    @NonNull
    private final Map<String, Runnable> mTimeoutMap;

    @NonNull
    private final RewardedAdsLoaders rewardedAdsLoaders;

    public static final class RequestParameters {
        @Nullable
        public final String mKeywords;
        @Nullable
        public final String mUserDataKeywords;
        @Nullable
        public final Location mLocation;
        @Nullable
        public final String mCustomerId;

        public RequestParameters(@Nullable final String keywords) {
            this(keywords, null);
        }

        public RequestParameters(@Nullable final String keywords, @Nullable final String userDataKeywords) {
            this(keywords, userDataKeywords, null);
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
            mUserDataKeywords = canCollectPersonalInformation ? userDataKeywords : null;
            mLocation = canCollectPersonalInformation ? location : null;
        }
    }


    private MoPubRewardedVideoManager(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        mMainActivity = new WeakReference<>(mainActivity);
        mContext = mainActivity.getApplicationContext();
        mRewardedAdData = new RewardedAdData();
        mCallbackHandler = new Handler(Looper.getMainLooper());
        mGlobalMediationSettings = new HashSet<>();
        MoPubCollections.addAllNonNull(mGlobalMediationSettings, mediationSettings);
        mInstanceMediationSettings = new HashMap<>();
        mBaseAdTimeoutHandler = new Handler();
        mTimeoutMap = new HashMap<>();

        rewardedAdsLoaders = new RewardedAdsLoaders(this);

        sBaseAdSharedPrefs =
                SharedPreferencesHelper.getSharedPreferences(mContext, CUSTOM_EVENT_PREF_NAME);
    }

    public static synchronized void init(@NonNull Activity mainActivity, MediationSettings... mediationSettings) {
        if (sInstance == null) {
            sInstance = new MoPubRewardedVideoManager(mainActivity, mediationSettings);
        } else {
            MoPubLog.log(CUSTOM, "Tried to call initializeRewardedVideo more than once. Only the first " +
                    "initialization call has any effect.");
        }
    }

    @ReflectionTarget
    public static void updateActivity(@Nullable Activity activity) {
        if (sInstance != null) {
            sInstance.mMainActivity = new WeakReference<>(activity);
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
     * @param clazz    the exact Class of the {@link MediationSettings} instance to retrieve
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
     * Builds a MultiAdRequest for the given adUnitId and adds it to the singleton RequestQueue. This
     * method will not make a new request if there is already a video loading for this adUnitId.
     *
     * @param adUnitId          MoPub adUnitId String
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
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Did not queue rewarded ad request for ad " +
                    "unit %s. The ad is already showing.", adUnitId));
            return;
        }

        if (sInstance.rewardedAdsLoaders.canPlay(adUnitId)) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Did not queue rewarded ad request for ad " +
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
        final Set<MediationSettings> newInstanceMediationSettings = new HashSet<>();
        MoPubCollections.addAllNonNull(newInstanceMediationSettings, mediationSettings);
        sInstance.mInstanceMediationSettings.put(adUnitId, newInstanceMediationSettings);

        final String customerId = requestParameters == null ? null : requestParameters.mCustomerId;
        if (!TextUtils.isEmpty(customerId)) {
            sInstance.mRewardedAdData.setCustomerId(customerId);
        }

        final AdUrlGenerator urlGenerator = new WebViewAdUrlGenerator(sInstance.mContext);
        urlGenerator.withAdUnitId(adUnitId)
                .withKeywords(requestParameters == null ? null : requestParameters.mKeywords)
                .withUserDataKeywords((requestParameters == null ||
                        !MoPub.canCollectPersonalInformation()) ? null : requestParameters.mUserDataKeywords);

        setSafeAreaValues(urlGenerator);

        loadVideo(adUnitId, urlGenerator.generateUrlString(Constants.HOST), null);
    }

    private static void loadVideo(@NonNull String adUnitId, @NonNull String adUrlString, @Nullable MoPubErrorCode errorCode) {
        if (sInstance == null) {
            logErrorNotInitialized();
            return;
        }

        sInstance.fetchAd(adUnitId, adUrlString, errorCode);
    }

    private void fetchAd(@NonNull String adUnitId, @NonNull String adUrlString, @Nullable MoPubErrorCode errorCode) {
        if (rewardedAdsLoaders.isLoading(adUnitId)) {
            MoPubLog.log(CUSTOM, String.format(Locale.US, "Did not queue rewarded ad request for ad " +
                    "unit %s. A request is already pending.", adUnitId));
            return;
        }

        // Issue MoPub request
        MoPubLog.log(CUSTOM, String.format(Locale.US,
                "Loading rewarded ad request for ad unit %s with URL %s", adUnitId, adUrlString));
        rewardedAdsLoaders.loadNextAd(mContext, adUnitId, adUrlString, errorCode);
    }

    public static boolean hasVideo(@NonNull String adUnitId) {
        if (sInstance != null) {
            final AdAdapter adAdapter = sInstance.mRewardedAdData.getAdAdapter(adUnitId);
            return isPlayable(adUnitId, adAdapter);
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
            MoPubLog.log(CUSTOM, String.format(
                    Locale.US,
                    "Provided rewarded ad custom data parameter longer than supported" +
                            "(%d bytes, %d maximum)",
                    customData.length(), CUSTOM_DATA_MAX_LENGTH_BYTES));
        }

        final AdAdapter adAdapter = sInstance.mRewardedAdData.getAdAdapter(adUnitId);
        if (isPlayable(adUnitId, adAdapter)) {
            // If there are rewards available but no reward is selected, fail over.
            if (!sInstance.mRewardedAdData.getAvailableRewards(adUnitId).isEmpty()
                    && sInstance.mRewardedAdData.getMoPubReward(adUnitId) == null) {
                sInstance.failover(adUnitId, MoPubErrorCode.REWARD_NOT_SELECTED);
                return;
            }

            sInstance.mRewardedAdData.updateLastShownRewardMapping(
                    adAdapter,
                    sInstance.mRewardedAdData.getMoPubReward(adUnitId));
            sInstance.mRewardedAdData.updateAdUnitToCustomDataMapping(adUnitId, customData);
            sInstance.mRewardedAdData.setCurrentlyShowingAdUnitId(adUnitId);
            adAdapter.show(null); // need a MoPubAd (and a rework) if we get to rewarded banners
        } else {
            if (sInstance.rewardedAdsLoaders.isLoading(adUnitId)) {
                MoPubLog.log(CUSTOM, "Rewarded ad is not ready to be shown yet.");
            } else {
                MoPubLog.log(CUSTOM, "No rewarded ad loading or loaded.");
            }

            sInstance.failover(adUnitId, MoPubErrorCode.VIDEO_NOT_AVAILABLE);
        }
    }

    private static boolean isPlayable(String adUnitId, @Nullable AdAdapter adAdapter) {
        return (sInstance != null
                && sInstance.rewardedAdsLoaders.canPlay(adUnitId)
                && adAdapter != null
                && adAdapter.isReady());
    }

    /**
     * Retrieves the set of available {@link MoPubReward} instance(s) for this AdUnit.
     *
     * @param adUnitId MoPub adUnitId String
     * @return a set of {@link MoPubReward} instance(s) if available, else an empty set.
     */
    @NonNull
    public static Set<MoPubReward> getAvailableRewards(@NonNull String adUnitId) {
        if (sInstance != null) {
            return sInstance.mRewardedAdData.getAvailableRewards(adUnitId);
        } else {
            logErrorNotInitialized();
            return Collections.emptySet();
        }
    }

    /**
     * Selects the reward for this AdUnit from available {@link MoPubReward} instances.
     * If this AdUnit does not have any rewards, or if the selected reward is not available
     * for this AdUnit, then no reward will be selected for this AdUnit.
     *
     * @param adUnitId       MoPub adUnitId String
     * @param selectedReward selected {@link MoPubReward}
     */
    public static void selectReward(@NonNull String adUnitId, @NonNull MoPubReward selectedReward) {
        if (sInstance != null) {
            sInstance.mRewardedAdData.selectReward(adUnitId, selectedReward);
        } else {
            logErrorNotInitialized();
        }
    }

    private static void setSafeAreaValues(@NonNull final AdUrlGenerator urlGenerator) {
        Preconditions.checkNotNull(urlGenerator);

        // Set the requested ad size as screen size
        final Point dimens = ClientMetadata.getInstance(sInstance.mContext).getDeviceDimensions();
        urlGenerator.withRequestedAdSize(dimens);

        // Set the window insets if we can get them
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final Activity activity = sInstance.mMainActivity.get();
            if (activity == null) {
                return;
            }
            final Window window = sInstance.mMainActivity.get().getWindow();
            if (window == null) {
                return;
            }
            final WindowInsets insets = window.getDecorView().getRootWindowInsets();
            if (insets == null) {
                return;
            }
            urlGenerator.withWindowInsets(insets);
        }
    }

    ///// Ad Request / Response methods /////
    void onAdSuccess(AdResponse adResponse) {
        final String adUnitId = adResponse.getAdUnitId();

        Integer timeoutMillis = adResponse.getAdTimeoutMillis(DEFAULT_LOAD_TIMEOUT);
        final String baseAdClassName = adResponse.getBaseAdClassName();

        if (baseAdClassName == null) {
            MoPubLog.log(CUSTOM, "Couldn't create base ad, class name was null.");
            failover(adUnitId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        // We only allow one rewarded ad to be loaded at a time for each ad unit. This should
        // clear out the old rewarded ad if there already was one loaded and not played.
        final AdAdapter currentAdAdapter = mRewardedAdData.getAdAdapter(adUnitId);
        if (currentAdAdapter != null) {
            currentAdAdapter.invalidate();
        }

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
                MoPubLog.log(CUSTOM, "Error parsing rewarded currencies JSON header: " + rewardedCurrencies);
                failover(adUnitId, MoPubErrorCode.REWARDED_CURRENCIES_PARSING_ERROR);
                return;
            }
        }

        mRewardedAdData.updateAdUnitToServerCompletionUrlMapping(adUnitId,
                adResponse.getRewardedVideoCompletionUrl());

        Activity mainActivity = mMainActivity.get();
        if (mainActivity == null) {
            MoPubLog.log(CUSTOM, "Could not load base ad because Activity reference was null. Call" +
                    " MoPub#updateActivity before requesting more rewarded ads.");

            // Don't go through the ordinary failover process since we have
            // no activity for the failover to use.
            rewardedAdsLoaders.markFail(adUnitId);
            return;
        }

        // Fetch the server extras mappings.
        final Map<String, String> serverExtras = adResponse.getServerExtras();

        // If the base ad is a third-party rewarded video, the server extras mappings
        // contain init parameters for this base ad class. Serialize the mappings into a
        // JSON string, then update SharedPreferences keying on the base ad class name.
        final String serverExtrasJsonString = (new JSONObject(serverExtras)).toString();
        final String impressionMinVisibleDipsString = adResponse.getImpressionMinVisibleDips();
        final String impressionMinVisibleMsString = adResponse.getImpressionMinVisibleMs();
        final int timeoutDelayMillis = adResponse.getAdTimeoutMillis(AdData.DEFAULT_FULLSCREEN_TIMEOUT_DELAY);

        MoPubLog.log(CUSTOM, String.format(Locale.US,
                "Updating init settings for base ad %s with params %s",
                baseAdClassName, serverExtrasJsonString));

        sBaseAdSharedPrefs
                .edit()
                .putString(baseAdClassName, serverExtrasJsonString)
                .apply();

        final String adPayload = serverExtras.remove(DataKeys.HTML_RESPONSE_BODY_KEY);

        final AdData.Builder adDataBuilder = new AdData.Builder()
                .adUnit(adUnitId)
                .isRewarded(true)
                // Rewarded ad responses are different than non-rewarded and require the
                // `fullAdType`` to be passed as `adType`.
                .adType(adResponse.getFullAdType())
                .adPayload(adPayload != null ? adPayload : "")
                .currencyName(adResponse.getRewardedVideoCurrencyName())
                .impressionMinVisibleDips(impressionMinVisibleDipsString)
                .impressionMinVisibleMs(impressionMinVisibleMsString)
                .dspCreativeId(adResponse.getDspCreativeId())
                .broadcastIdentifier(Utils.generateUniqueId())
                .timeoutDelayMillis(timeoutDelayMillis)
                .customerId(mRewardedAdData.getCustomerId())
                .allowCustomClose(false)
                .fullAdType(adResponse.getFullAdType())
                .extras(serverExtras);

        final Integer rewardedDuration = adResponse.getRewardedDuration();
        if (rewardedDuration != null) {
            adDataBuilder.rewardedDurationSeconds(rewardedDuration);
        }

        final String currencyAmountString = adResponse.getRewardedVideoCurrencyAmount();
        int currencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
        if (!TextUtils.isEmpty(currencyAmountString)) {
            try {
                currencyAmount = Integer.parseInt(currencyAmountString);
            } catch (NumberFormatException e) {
                MoPubLog.log(CUSTOM,
                        "Unable to convert currency amount: " + currencyAmountString +
                                ". Using the default reward amount: " +
                                MoPubReward.DEFAULT_REWARD_AMOUNT);
            }
        }
        adDataBuilder.currencyAmount(currencyAmount);

        // Load base ad
        MoPubLog.log(CUSTOM, String.format(Locale.US,
                "Loading base ad with class name %s", baseAdClassName));

        try {
            // Instantiate ad adapter
            Class<? extends AdAdapter> adAdapterClass = Class.forName(FULLSCREEN_AD_ADAPTER)
                    .asSubclass(AdAdapter.class);
            Constructor<?> adAdapterConstructor = adAdapterClass.getDeclaredConstructor(
                    new Class[]{
                            Context.class,
                            String.class,
                            AdData.class
                    }
            );
            adAdapterConstructor.setAccessible(true);
            final AdAdapter adAdapter = (AdAdapter) adAdapterConstructor.newInstance(
                    sInstance.mMainActivity.get(),
                    baseAdClassName,
                    adDataBuilder.build()
            );

            final InternalRewardedVideoAdListener listener = new InternalRewardedVideoAdListener(adAdapter);

            // Set up timeout calls.
            final Runnable timeout = () -> {
                MoPubLog.log(CUSTOM, "Base Ad failed to load rewarded ad in a timely fashion.");
                adAdapter.onAdLoadFailed(NETWORK_TIMEOUT);
                adAdapter.invalidate();
            };

            mBaseAdTimeoutHandler.postDelayed(timeout, timeoutMillis);
            mTimeoutMap.put(adUnitId, timeout);

            adAdapter.load(listener);
            adAdapter.setInteractionListener(listener);

            final String adNetworkId = adAdapter.getAdNetworkId();
            mRewardedAdData.updateAdUnitAdAdapterMapping(adUnitId, adAdapter);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, String.format(Locale.US,
                    "Couldn't create base ad with class name %s", baseAdClassName));
            failover(adUnitId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    void onAdError(@NonNull VolleyError volleyError, @NonNull String adUnitId) {
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

        if (rewardedAdsLoaders.hasMoreAds(adUnitId) && !errorCode.equals(EXPIRED)) {
            loadVideo(adUnitId, "", errorCode);
        } else if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoLoadFailure(adUnitId, errorCode);
        }
    }

    private void cancelTimeouts(@NonNull String adUnitId) {
        final Runnable runnable = mTimeoutMap.remove(adUnitId);
        if (runnable != null) {  // We can't pass null or all callbacks will be removed.
            mBaseAdTimeoutHandler.removeCallbacks(runnable);
        }
    }

    //////// Listener methods that should be called by third-party SDKs. //////////

    /**
     * Notify the manager that a rewarded ad loaded successfully.
     *
     * @param adAdapter    - the ad adapter for the third-party base ad object.
     * @param thirdPartyId - the ad id of the third party SDK. This may be an empty String if the
     *                     SDK does not use ad ids, zone ids, or a analogous concept.
     */
    public static void onRewardedVideoLoadSuccess(@NonNull final AdAdapter adAdapter, @NonNull final String thirdPartyId) {
        postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
            @Override
            protected void forEach(@NonNull final String adUnitId) {
                sInstance.cancelTimeouts(adUnitId);
                sInstance.rewardedAdsLoaders.creativeDownloadSuccess(adUnitId);
                if (sInstance.mVideoListener != null) {
                    sInstance.mVideoListener.onRewardedVideoLoadSuccess(adUnitId);
                }
            }
        });
    }

    public static void onRewardedVideoLoadFailure(@NonNull final AdAdapter adAdapter, final String thirdPartyId, final MoPubErrorCode errorCode) {
        postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
            @Override
            protected void forEach(@NonNull final String adUnitId) {
                sInstance.cancelTimeouts(adUnitId);
                sInstance.failover(adUnitId, errorCode);
            }
        });
    }

    public static void onRewardedVideoStarted(@NonNull final AdAdapter adAdapter, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
                @Override
                protected void forEach(@NonNull final String adUnitId) {
                    onRewardedVideoStartedAction(adUnitId);
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
        sInstance.rewardedAdsLoaders.onRewardedVideoStarted(adUnitId, sInstance.mContext);
    }

    public static void onRewardedVideoPlaybackError(@NonNull final AdAdapter adAdapter, final String thirdPartyId, final MoPubErrorCode errorCode) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
                @Override
                protected void forEach(@NonNull final String adUnitId) {
                    onRewardedVideoPlaybackErrorAction(adUnitId, errorCode);
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
        sInstance.mRewardedAdData.setCurrentlyShowingAdUnitId(null);
    }

    private static void onRewardedVideoPlaybackErrorAction(@NonNull final String adUnitId, @NonNull final MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(errorCode);
        sInstance.rewardedAdsLoaders.markFail(adUnitId);
        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoPlaybackError(adUnitId, errorCode);
        }
    }

    public static void onRewardedVideoClicked(@NonNull final AdAdapter adAdapter, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
                @Override
                protected void forEach(@NonNull final String adUnitId) {
                    onRewardedVideoClickedAction(adUnitId);
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

        sInstance.rewardedAdsLoaders.onRewardedVideoClicked(adUnitId, sInstance.mContext);
    }

    public static void onRewardedVideoClosed(@NonNull final AdAdapter adAdapter, final String thirdPartyId) {
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();
        if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
            postToInstance(new ForEachAdUnitIdRunnable(adAdapter) {
                @Override
                protected void forEach(@NonNull final String adUnitId) {
                    onRewardedVideoClosedAction(adUnitId);
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
        // remove adloader from map
        sInstance.rewardedAdsLoaders.markPlayed(adUnitId);
        if (sInstance.mVideoListener != null) {
            sInstance.mVideoListener.onRewardedVideoClosed(adUnitId);
        }
    }

    public static void onRewardedVideoCompleted(@NonNull final AdAdapter adAdapter,
                                                final String thirdPartyId, @NonNull final MoPubReward moPubReward) {
        // Unlike other callbacks in this class, only call the listener once with all the AdUnitIds
        // in the matching set.
        final String currentlyShowingAdUnitId =
                sInstance.mRewardedAdData.getCurrentlyShowingAdUnitId();

        rewardOnClient(adAdapter, moPubReward, currentlyShowingAdUnitId);
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

                    final AdAdapter adAdapter =
                            sInstance.mRewardedAdData.getAdAdapter(currentlyShowingAdUnitId);

                    final String className = (adAdapter == null)
                            ? null
                            : adAdapter.getBaseAdClassName();

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

    private static void rewardOnClient(
            @NonNull final AdAdapter adAdapter,
            @NonNull final MoPubReward moPubReward,
            @Nullable final String currentlyShowingAdUnitId) {
        postToInstance(() -> {
            final MoPubReward chosenReward = chooseReward(
                    sInstance.mRewardedAdData.getLastShownMoPubReward(adAdapter),
                    moPubReward);

            Set<String> rewardedIds = new HashSet<>();
            if (TextUtils.isEmpty(currentlyShowingAdUnitId)) {
                final Set<String> adUnitIds = sInstance.mRewardedAdData.getAdUnitIdsForAdAdapter(
                        adAdapter);
                rewardedIds.addAll(adUnitIds);
            } else {
                // If we know which ad unit is showing, only reward the currently showing
                // ad unit.
                rewardedIds.add(currentlyShowingAdUnitId);
            }

            MoPubLog.log(SHOULD_REWARD, chosenReward.getAmount(), chosenReward.getLabel());
            if (sInstance.mVideoListener != null) {
                sInstance.mVideoListener.onRewardedVideoCompleted(rewardedIds,
                        chosenReward);
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
        MoPubLog.log(CUSTOM, "MoPub rewarded ad was not initialized. You must call " +
                "MoPub.initializeSdk() with an Activity Context before loading or attempting " +
                "to play rewarded ads.");
    }

    /**
     * A runnable that calls forEach on each member of the rewarded ad data passed to the runnable.
     */
    private static abstract class ForEachAdUnitIdRunnable implements Runnable {

        @NonNull
        private final AdAdapter mAdAdapter;

        ForEachAdUnitIdRunnable(@NonNull final AdAdapter adAdapter) {
            Preconditions.checkNotNull(adAdapter);
            mAdAdapter = adAdapter;
        }

        protected abstract void forEach(@NonNull final String adUnitId);

        @Override
        public void run() {
            final Set<String> adUnitIds = sInstance.mRewardedAdData
                    .getAdUnitIdsForAdAdapter(mAdAdapter);
            for (String adUnitId : adUnitIds) {
                forEach(adUnitId);
            }
        }
    }

    private static class InternalRewardedVideoAdListener implements AdLifecycleListener.LoadListener, AdLifecycleListener.InteractionListener {

        final AdAdapter adAdapter;

        InternalRewardedVideoAdListener(AdAdapter adAdapter) {
            this.adAdapter = adAdapter;
        }

        @Override
        public void onAdLoaded() {
            MoPubLog.log(LOAD_SUCCESS);
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(adAdapter,
                    adAdapter.getAdNetworkId());
        }

        @Override
        public void onAdLoadFailed(@NonNull MoPubErrorCode errorCode) {
            MoPubLog.log(LOAD_FAILED, errorCode.getIntCode(), errorCode);
            onAdFailed(errorCode);
        }

        @Override
        public void onAdFailed(@NonNull MoPubErrorCode errorCode) {
            MoPubLog.log(SHOW_FAILED, errorCode.getIntCode(), errorCode);
            switch (errorCode) {
                case VIDEO_PLAYBACK_ERROR:
                    MoPubRewardedVideoManager.onRewardedVideoPlaybackError(adAdapter,
                            adAdapter.getAdNetworkId(), errorCode);
                    break;
                default:
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(adAdapter,
                            adAdapter.getAdNetworkId(), errorCode);
            }
        }

        @Override
        public void onAdShown() {
            MoPubLog.log(SHOW_SUCCESS);
            MoPubRewardedVideoManager.onRewardedVideoStarted(adAdapter, adAdapter.getAdNetworkId());
        }

        @Override
        public void onAdClicked() {
            MoPubLog.log(CLICKED);
            MoPubRewardedVideoManager.onRewardedVideoClicked(adAdapter, adAdapter.getAdNetworkId());
        }

        @Override
        public void onAdImpression() { /* no-op for rewarded */ }

        @Override
        public void onAdDismissed() {
            MoPubLog.log(DID_DISAPPEAR);
            MoPubRewardedVideoManager.onRewardedVideoClosed(adAdapter, adAdapter.getAdNetworkId());
        }

        @Override
        public void onAdComplete(@Nullable final MoPubReward moPubReward) {
            MoPubReward actualReward = moPubReward;
            if (actualReward == null) {
                actualReward = MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                        MoPubReward.DEFAULT_REWARD_AMOUNT);
            }
            MoPubRewardedVideoManager.onRewardedVideoCompleted(adAdapter,
                    adAdapter.getAdNetworkId(),
                    actualReward);
        }

        @Override
        public void onAdCollapsed() { /* NO-OP for Rewarded */ }

        @Override
        public void onAdExpanded() { /* NO-OP for Rewarded */ }

        @Override
        public void onAdPauseAutoRefresh() { /* NO-OP for Rewarded */ }

        @Override
        public void onAdResumeAutoRefresh() { /* NO-OP for Rewarded */ }
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
    static RewardedAdsLoaders getAdRequestStatusMapping() {
        if (sInstance != null) {
            return sInstance.rewardedAdsLoaders;
        }
        return null;
    }

    @Deprecated
    @VisibleForTesting
    static void setBaseAdSharedPrefs(@NonNull SharedPreferences sharedPrefs) {
        Preconditions.checkNotNull(sharedPrefs);

        sBaseAdSharedPrefs = sharedPrefs;
    }
}

