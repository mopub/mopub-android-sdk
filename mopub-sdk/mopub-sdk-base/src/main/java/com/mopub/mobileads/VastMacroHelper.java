package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handles macro substitution with actual data.
 */
public class VastMacroHelper {

    @NonNull private final List<String> mOriginalUris;
    @NonNull private final Map<VastMacro, String> mMacroDataMap;

    public VastMacroHelper(@NonNull final List<String> uris) {
        Preconditions.checkNotNull(uris, "uris cannot be null");
        mOriginalUris = uris;
        mMacroDataMap = new HashMap<VastMacro, String>();
        mMacroDataMap.put(VastMacro.CACHEBUSTING, getCachebustingString());
    }

    @NonNull
    public List<String> getUris() {
        List<String> modifiedUris = new ArrayList<String>();

        for(final String originalUri : mOriginalUris) {
            String modifiedUri = originalUri;
            if (TextUtils.isEmpty(modifiedUri)) {
                continue;
            }
            for (final VastMacro vastMacro : VastMacro.values()) {
                String value = mMacroDataMap.get(vastMacro);
                if (value == null) {
                    value = "";
                }

                modifiedUri = modifiedUri.replaceAll("\\[" + vastMacro.name() + "\\]",
                        value);
            }

            modifiedUris.add(modifiedUri);
        }

        return modifiedUris;
    }

    @NonNull
    public VastMacroHelper withErrorCode(@Nullable final VastErrorCode errorCode) {
        if (errorCode != null) {
            mMacroDataMap.put(VastMacro.ERRORCODE, errorCode.getErrorCode());
        }
        return this;
    }

    @NonNull
    public VastMacroHelper withContentPlayHead(@Nullable final Integer contentPlayHeadMS) {
        if (contentPlayHeadMS != null) {
            String contentPlayHeadMSStr = formatContentPlayHead(contentPlayHeadMS);
            if (!TextUtils.isEmpty(contentPlayHeadMSStr)) {
                mMacroDataMap.put(VastMacro.CONTENTPLAYHEAD, contentPlayHeadMSStr);
            }
        }
        return this;
    }

    @NonNull
    public VastMacroHelper withAssetUri(@Nullable String assetUri) {
        if (!TextUtils.isEmpty(assetUri)) {
            // URL-encode any URLs
            try {
                assetUri = URLEncoder.encode(assetUri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                MoPubLog.w("Failed to encode url", e);
            }
            mMacroDataMap.put(VastMacro.ASSETURI, assetUri);
        }
        return this;
    }

    @NonNull
    private String getCachebustingString() {
        return String.format(Locale.US, "%08d", Math.round(Math.random() * 100000000));
    }

    @NonNull
    private String formatContentPlayHead(int contentPlayHeadMS) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(contentPlayHeadMS),
                TimeUnit.MILLISECONDS.toMinutes(contentPlayHeadMS) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(contentPlayHeadMS) % TimeUnit.MINUTES.toSeconds(1),
                contentPlayHeadMS % 1000);
    }
}
