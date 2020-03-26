// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.24 add method noExistGoogleAds
// Huawei Technologies Co., Ltd.

package com.mopub.common;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.MoPubErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.mopub.common.DefaultAdapterClasses.GOOGLE_PLAY_SERVICES_ADAPTER_CONFIGURATION;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR;

/**
 * Gets Advanced Bidders through an Async Task and stores it in memory for retrieval.
 */
public class AdapterConfigurationManager implements AdapterConfigurationsInitializationListener {

    private static final String TOKEN_KEY = "token";

    @Nullable private volatile Map<String, AdapterConfiguration> mAdapterConfigurations;
    @Nullable private SdkInitializationListener mSdkInitializationListener;

    AdapterConfigurationManager(
            @Nullable final SdkInitializationListener sdkInitializationListener) {
        mSdkInitializationListener = sdkInitializationListener;
    }

    public void initialize(@NonNull final Context context,
            @NonNull final Set<String> adapterConfigurationClasses,
            @NonNull final Map<String, Map<String, String>> networkMediationConfigurations,
            @NonNull final Map<String, Map<String, String>> moPubRequestOptions) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adapterConfigurationClasses);
        Preconditions.checkNotNull(networkMediationConfigurations);
        Preconditions.checkNotNull(moPubRequestOptions);

        AsyncTasks.safeExecuteOnExecutor(
                new AdapterConfigurationsInitializationAsyncTask(context.getApplicationContext(),
                        adapterConfigurationClasses, networkMediationConfigurations,
                        moPubRequestOptions, this));
    }

    @Nullable
    public List<String> getAdvancedBidderNames() {
        final Map<String, AdapterConfiguration> adapterConfigurations = mAdapterConfigurations;
        if (adapterConfigurations == null || adapterConfigurations.isEmpty()) {
            return null;
        }

        final List<String> abNames = new ArrayList<>();
        for (final AdapterConfiguration adapterConfiguration : adapterConfigurations.values()) {
            abNames.add(adapterConfiguration.getMoPubNetworkName());
        }
        return abNames;
    }

    @Nullable
    public List<String> getAdapterConfigurationInfo() {
        final Map<String, AdapterConfiguration> adapterConfigurations = mAdapterConfigurations;
        if (adapterConfigurations == null || adapterConfigurations.isEmpty()) {
            return null;
        }

        final List<String> abNames = new ArrayList<>();
        for (final Map.Entry<String, AdapterConfiguration> entry : adapterConfigurations.entrySet()) {
            final StringBuilder configDetails = new StringBuilder();

            final String adapterName = entry.getKey();
            final int lastDotIndex = adapterName.lastIndexOf(".");
            configDetails.append(adapterName.substring(lastDotIndex + 1));

            configDetails.append(": Adapter version ");
            configDetails.append(entry.getValue().getAdapterVersion());

            configDetails.append(", SDK version ");
            configDetails.append(entry.getValue().getNetworkSdkVersion());
            
            abNames.add(configDetails.toString());
        }
        return abNames;
    }

    @Nullable
    String getTokensAsJsonString(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final JSONObject tokens = getTokensAsJsonObject(context);
        if (tokens == null) {
            return null;
        }
        return tokens.toString();
    }

    @Nullable
    private JSONObject getTokensAsJsonObject(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        final Map<String, AdapterConfiguration>
                adapterConfigurations = mAdapterConfigurations;
        if (adapterConfigurations == null || adapterConfigurations.isEmpty()) {
            return null;
        }

        JSONObject jsonObject = null;
        for (final AdapterConfiguration adapterConfiguration : adapterConfigurations.values()) {
            try {
                final String token = adapterConfiguration.getBiddingToken(context);
                if (TextUtils.isEmpty(token)) {
                    continue;
                }
                final JSONObject bidderJsonObject = new JSONObject();
                bidderJsonObject.put(TOKEN_KEY, token);
                if (jsonObject == null) {
                    jsonObject = new JSONObject();
                }
                jsonObject.put(adapterConfiguration.getMoPubNetworkName(), bidderJsonObject);
            } catch (JSONException e) {
                MoPubLog.log(ERROR, "JSON parsing failed for MoPub network name: " +
                        adapterConfiguration.getMoPubNetworkName());
            }
        }
        return jsonObject;
    }

    @VisibleForTesting
    AdapterConfiguration getAdapterConfiguration(
            @NonNull final Class<? extends AdapterConfiguration> clazz) {
        Preconditions.checkNotNull(clazz);

        final Map<String, AdapterConfiguration> adapterConfigurations = mAdapterConfigurations;
        if (adapterConfigurations == null) {
            return null;
        }
        return adapterConfigurations.get(clazz.getName());
    }

    @Override
    public void onAdapterConfigurationsInitialized(@NonNull final
    Map<String, AdapterConfiguration> adapterConfigurations) {
        Preconditions.checkNotNull(adapterConfigurations);

        mAdapterConfigurations = adapterConfigurations;

        final SdkInitializationListener sdkInitializationListener = mSdkInitializationListener;
        if (sdkInitializationListener != null) {
            sdkInitializationListener.onInitializationFinished();
            mSdkInitializationListener = null;
        }
    }

    @Override
    public void onNetworkInitializationFinished(
            @NonNull final Class<? extends AdapterConfiguration> adapterConfigurationClass,
            @NonNull final MoPubErrorCode moPubErrorCode) {
        Preconditions.checkNotNull(adapterConfigurationClass);
        Preconditions.checkNotNull(moPubErrorCode);

        MoPubLog.log(CUSTOM, adapterConfigurationClass + " initialized with error code " +
                moPubErrorCode);
    }

    private static class AdapterConfigurationsInitializationAsyncTask extends AsyncTask<Void, Void, Map<String, AdapterConfiguration>> {

        @NonNull private final WeakReference<Context> weakContext;
        @NonNull private final Set<String> adapterConfigurationClasses;
        @NonNull private final Map<String, Map<String, String>> networkMediationConfigurations;
        @NonNull private final Map<String, Map<String, String>> moPubRequestOptions;
        @NonNull private final AdapterConfigurationsInitializationListener
                adapterConfigurationsInitializationListener;

        AdapterConfigurationsInitializationAsyncTask(
                @NonNull final Context context,
                @NonNull final Set<String> adapterConfigurationClasses,
                @NonNull final Map<String, Map<String, String>> networkMediationConfigurations,
                @NonNull final Map<String, Map<String, String>> moPubRequestOptions,
                @NonNull final AdapterConfigurationsInitializationListener adapterConfigurationsInitializationListener) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(adapterConfigurationClasses);
            Preconditions.checkNotNull(networkMediationConfigurations);
            Preconditions.checkNotNull(moPubRequestOptions);
            Preconditions.checkNotNull(adapterConfigurationsInitializationListener);

            this.weakContext = new WeakReference<>(context);
            this.adapterConfigurationClasses = adapterConfigurationClasses;
            this.networkMediationConfigurations = networkMediationConfigurations;
            this.moPubRequestOptions = moPubRequestOptions;
            this.adapterConfigurationsInitializationListener = adapterConfigurationsInitializationListener;
        }

        @Override
        protected Map<String, AdapterConfiguration> doInBackground(final Void... voids) {
            final Map<String, AdapterConfiguration> adapterConfigurations = new HashMap<>();
            for (final String adapterConfigurationClass : adapterConfigurationClasses) {
                AdapterConfiguration adapterConfiguration;
                try {
                    adapterConfiguration = Reflection.instantiateClassWithEmptyConstructor(
                            adapterConfigurationClass, AdapterConfiguration.class);
                } catch (Exception e) {
                    MoPubLog.log(CUSTOM_WITH_THROWABLE,
                            "Unable to find class " + adapterConfigurationClass, e);
                    continue;
                }

                final Context context = weakContext.get();
                if (context == null) {
                    MoPubLog.log(CUSTOM,
                            "Context null. Unable to initialize adapter configuration " +
                                    adapterConfigurationClass);
                    continue;
                }

                // Merge and overwrite configuration from the cache with newly passed in values.
                final Map<String, String> networkMediationConfiguration =
                        networkMediationConfigurations.get(adapterConfigurationClass);
                // Making a new HashMap here because getCachedInitializationParameters may
                // return an unmodifiable map.
                final Map<String, String> mergedParameters = new HashMap<>(
                        adapterConfiguration.getCachedInitializationParameters(context));;
                if (networkMediationConfiguration != null) {
                    mergedParameters.putAll(networkMediationConfiguration);
                    adapterConfiguration.setCachedInitializationParameters(context,
                            mergedParameters);
                }

                final Map<String, String> moPubRequestOption =
                        moPubRequestOptions.get(adapterConfigurationClass);
                if (moPubRequestOption != null) {
                    adapterConfiguration.setMoPubRequestOptions(moPubRequestOption);
                }

                MoPubLog.log(CUSTOM, String.format(Locale.US, "Initializing %s version %s " +
                                "with network sdk version %s and with params %s",
                        adapterConfigurationClass, adapterConfiguration.getAdapterVersion(),
                        adapterConfiguration.getNetworkSdkVersion(), mergedParameters));

                if (adapterConfigurationClass.equals(GOOGLE_PLAY_SERVICES_ADAPTER_CONFIGURATION.name())
                        && noExistGoogleAds()) {
                    continue;
                }
                adapterConfiguration.initializeNetwork(context, mergedParameters,
                        adapterConfigurationsInitializationListener);

                adapterConfigurations.put(adapterConfigurationClass, adapterConfiguration);
            }
            return adapterConfigurations;
        }

        private boolean noExistGoogleAds() {
            try {
                Class.forName("com.google.android.gms.ads.MobileAds");
                return false;
            } catch (Exception e) {
                MoPubLog.log(CUSTOM, "class 'com.google.android.gms.ads.MobileAds' not find");
                return true;
            }
        }

        @Override
        protected void onPostExecute(
                @NonNull final Map<String, AdapterConfiguration> adapterConfigurations) {
            adapterConfigurationsInitializationListener.onAdapterConfigurationsInitialized(
                    adapterConfigurations);
        }
    }
}
