package com.mopub.common.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.mopub.common.Preconditions;

public class Dips {
    public static float pixelsToFloatDips(final float pixels, final Context context) {
        return pixels / getDensity(context);
    }

    public static int pixelsToIntDips(final float pixels, final Context context) {
        return (int) (pixelsToFloatDips(pixels, context) + 0.5f);
    }

    public static float dipsToFloatPixels(final float dips, final Context context) {
        return dips * getDensity(context);
    }

    public static int dipsToIntPixels(final float dips, final Context context) {
        return (int) (dipsToFloatPixels(dips, context) + 0.5f);
    }

    private static float getDensity(final Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static float asFloatPixels(float dips, Context context) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, displayMetrics);
    }

    public static int asIntPixels(float dips, Context context) {
        return (int) (asFloatPixels(dips, context) + 0.5f);
    }

    public static int screenWidthAsIntDips(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        return pixelsToIntDips(context.getResources().getDisplayMetrics().widthPixels, context);
    }

    public static int screenHeightAsIntDips(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        return pixelsToIntDips(context.getResources().getDisplayMetrics().heightPixels, context);
    }
}
