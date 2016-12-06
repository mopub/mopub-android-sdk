package com.mopub.common;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public final class SharedPreferencesHelper {
    public static final String PREFERENCE_NAME = "mopubSettings";

    private SharedPreferencesHelper() {}
    
    public static SharedPreferences getSharedPreferences(Context context) {
    	return context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
    }
}
