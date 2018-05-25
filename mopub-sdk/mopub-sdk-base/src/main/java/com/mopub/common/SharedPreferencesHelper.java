package com.mopub.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import static android.content.Context.MODE_PRIVATE;

public final class SharedPreferencesHelper {
    public static final String DEFAULT_PREFERENCE_NAME = "mopubSettings";

    private SharedPreferencesHelper() {}
    
    public static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        return context.getSharedPreferences(DEFAULT_PREFERENCE_NAME, MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences(
            @NonNull final Context context, @NonNull final String preferenceName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(preferenceName);

        return context.getSharedPreferences(preferenceName, MODE_PRIVATE);
    }
}
