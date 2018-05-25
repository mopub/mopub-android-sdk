package com.mopub.network;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.volley.Request;
import com.mopub.volley.toolbox.HurlStack;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps utility methods regarding MoPubRequests in one place.
 */
public class MoPubRequestUtils {

    public static String truncateQueryParamsIfPost(@NonNull final String url) {
        Preconditions.checkNotNull(url);
        if (!isMoPubRequest(url)) {
            return url;
        }

        final int queryPosition = url.indexOf('?');
        if (queryPosition == -1) {
            return url;
        }

        return url.substring(0, queryPosition);
    }

    public static boolean isMoPubRequest(@NonNull final String url) {
        Preconditions.checkNotNull(url);

        final String httpHost = Constants.HTTP + "://" + Constants.HOST;
        final String httpsHost = Constants.HTTPS + "://" + Constants.HOST;

        return url.startsWith(httpHost) || url.startsWith(httpsHost);
    }

    public static int chooseMethod(String url) {
        if (isMoPubRequest(url)) {
            return Request.Method.POST;
        } else {
            return Request.Method.GET;
        }
    }

    @NonNull
    public static Map<String, String> convertQueryToMap(@NonNull final Context context,
            @NonNull final String url) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(url);

        final Map<String, String> params = new HashMap<>();
        HurlStack.UrlRewriter rewriter = Networking.getUrlRewriter(context);
        final Uri uri = Uri.parse(rewriter.rewriteUrl(url));
        for (final String queryParam : uri.getQueryParameterNames()) {
            params.put(queryParam, TextUtils.join(",", uri.getQueryParameters(queryParam)));
        }

        return params;
    }

    @Nullable
    public static String generateBodyFromParams(@Nullable final Map<String, String> params,
            @NonNull final String url) {
        Preconditions.checkNotNull(url);

        if (!MoPubRequestUtils.isMoPubRequest(url) || params == null || params.isEmpty()) {
            return null;
        }

        final JSONObject jsonBody = new JSONObject();
        for (final String queryName : params.keySet()) {
            try {
                jsonBody.put(queryName, params.get(queryName));
            } catch (JSONException e) {
                MoPubLog.d("Unable to add " + queryName + " to JSON body.");
            }
        }
        return jsonBody.toString();
    }

    /**
     * This is a helper class and should not be instantiated.
     */
    private MoPubRequestUtils() {
    }
}
