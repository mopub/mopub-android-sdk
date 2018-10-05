// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

class Utils {
    static final String LOGTAG = "MoPub Sample App";

    private Utils() {}

    static void validateAdUnitId(String adUnitId) throws IllegalArgumentException {
        if (adUnitId == null) {
            throw new IllegalArgumentException("Invalid Ad Unit ID: null ad unit.");
        } else if (adUnitId.length() == 0) {
            throw new IllegalArgumentException("Invalid Ad Unit Id: empty ad unit.");
        } else if (adUnitId.length() > 256) {
            throw new IllegalArgumentException("Invalid Ad Unit Id: length too long.");
        } else if (!isAlphaNumeric(adUnitId)) {
            throw new IllegalArgumentException("Invalid Ad Unit Id: contains non-alphanumeric characters.");
        }
    }

    static void hideSoftKeyboard(final View view) {
        final InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    static boolean isAlphaNumeric(final String input) {
        return input.matches("^[a-zA-Z0-9-_]*$");
    }

    static void logToast(Context context, String message) {
        Log.d(LOGTAG, message);

        if (context != null && context.getApplicationContext() != null) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
