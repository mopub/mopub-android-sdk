package com.mopub.common.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.logging.MoPubLog;

public class Views {
    public static void removeFromParent(@Nullable View view) {
        if (view == null || view.getParent() == null) {
            return;
        }

        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    /**
     * Finds the topmost view in the current Activity or current view hierarchy.
     *
     * @param context If an Activity Context, used to obtain the Activity's DecorView. This is
     *                ignored if it is a non-Activity Context.
     * @param view A View in the currently displayed view hierarchy. If a null or non-Activity
     *             Context is provided, this View's topmost parent is used to determine the
     *             rootView.
     * @return The topmost View in the currency Activity or current view hierarchy. Null if no
     * applicable View can be found.
     */
    @Nullable
    public static View getTopmostView(@Nullable final Context context, @Nullable final View view) {
        final View rootViewFromActivity = getRootViewFromActivity(context);
        final View rootViewFromView = getRootViewFromView(view);

        // Prefer to use the rootView derived from the Activity's DecorView since it provides a
        // consistent value when the View is not attached to the Window. Fall back to the passed-in
        // View's hierarchy if necessary.
        return rootViewFromActivity != null
                ? rootViewFromActivity
                : rootViewFromView;
    }

    @Nullable
    private static View getRootViewFromActivity(@Nullable final Context context) {
        if (!(context instanceof Activity)) {
            return null;
        }

        return ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    @Nullable
    private static View getRootViewFromView(@Nullable final View view) {
        if (view == null) {
            return null;
        }

        if (!ViewCompat.isAttachedToWindow(view)) {
            MoPubLog.d("Attempting to call View#getRootView() on an unattached View.");
        }

        final View rootView = view.getRootView();

        if (rootView == null) {
            return null;
        }

        final View rootContentView = rootView.findViewById(android.R.id.content);
        return rootContentView != null
                ? rootContentView
                : rootView;
    }
}
