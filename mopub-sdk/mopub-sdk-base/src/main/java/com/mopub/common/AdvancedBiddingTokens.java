package com.mopub.common;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets Advanced Bidders through an Async Task and stores it in memory for retrieval.
 */
public class AdvancedBiddingTokens implements AdvancedBiddersInitializedListener {

    private static final String TOKEN_KEY = "token";

    @NonNull private List<MoPubAdvancedBidder> mAdvancedBidders;
    @Nullable private final SdkInitializationListener mSdkInitializationListener;

    public AdvancedBiddingTokens(@Nullable final SdkInitializationListener sdkInitializationListener) {
        mAdvancedBidders = new ArrayList<>();
        mSdkInitializationListener = sdkInitializationListener;
    }

    public void addAdvancedBidders(
            @NonNull final List<Class<? extends MoPubAdvancedBidder>> advancedBidderClasses) {
        Preconditions.checkNotNull(advancedBidderClasses);

        new AdvancedBiddersInitializationAsyncTask(advancedBidderClasses, this).execute();
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

        if (mAdvancedBidders.isEmpty()) {
            return null;
        }

        final JSONObject jsonObject = new JSONObject();
        for (final MoPubAdvancedBidder bidder : mAdvancedBidders) {
            try {
                final JSONObject bidderJsonObject = new JSONObject();
                bidderJsonObject.put(TOKEN_KEY, bidder.getToken(context));
                jsonObject.put(bidder.getCreativeNetworkName(), bidderJsonObject);
            } catch (JSONException e) {
                MoPubLog.d("JSON parsing failed for creative network name: " +
                        bidder.getCreativeNetworkName());
            }
        }
        return jsonObject;
    }

    @Override
    public void onAdvancedBiddersInitialized(
            @NonNull final List<MoPubAdvancedBidder> advancedBidders) {
        Preconditions.checkNotNull(advancedBidders);

        mAdvancedBidders = advancedBidders;

        if (mSdkInitializationListener != null) {
            mSdkInitializationListener.onInitializationFinished();
        }
    }

    private static class AdvancedBiddersInitializationAsyncTask extends AsyncTask<Void, Void, List<MoPubAdvancedBidder>> {

        @NonNull private final List<Class<? extends MoPubAdvancedBidder>> advancedBidderClasses;
        @NonNull private final AdvancedBiddersInitializedListener mAdvancedBiddersInitializedListener;

        AdvancedBiddersInitializationAsyncTask(
                @NonNull List<Class<? extends MoPubAdvancedBidder>> advancedBidderClasses,
                @NonNull final AdvancedBiddersInitializedListener advancedBiddersInitializedListener) {
            Preconditions.checkNotNull(advancedBidderClasses);
            Preconditions.checkNotNull(advancedBiddersInitializedListener);

            this.advancedBidderClasses = advancedBidderClasses;
            this.mAdvancedBiddersInitializedListener = advancedBiddersInitializedListener;
        }

        @Override
        protected List<MoPubAdvancedBidder> doInBackground(final Void... voids) {
            final List<MoPubAdvancedBidder> advancedBidders = new ArrayList<>();
            for (final Class<? extends MoPubAdvancedBidder> advancedBidderClass : advancedBidderClasses) {
                try {
                    final MoPubAdvancedBidder advancedBidder = Reflection.instantiateClassWithEmptyConstructor(
                            advancedBidderClass.getName(), MoPubAdvancedBidder.class);
                    advancedBidders.add(advancedBidder);
                } catch (Exception e) {
                    MoPubLog.e("Unable to find class " + advancedBidderClass.getName());
                }
            }
            return advancedBidders;
        }

        @Override
        protected void onPostExecute(final List<MoPubAdvancedBidder> advancedBidders) {
            mAdvancedBiddersInitializedListener.onAdvancedBiddersInitialized(advancedBidders);
        }
    }
}
