package com.mopub.common.util;

import android.view.View;
import android.view.ViewGroup;

public class Views {
    public static void removeFromParent(View view) {
        if (view == null || view.getParent() == null) {
            return;
        }

        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
