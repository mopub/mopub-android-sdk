package com.mopub.common.util;

import android.os.Build;

public enum VersionCode {
    BASE(1),
    BASE_1_1(2),
    CUPCAKE(3),
    DONUT(4),
    ECLAIR(5),
    ECLAIR_0_1(6),
    ECLAIR_MR1(7),
    FROYO(8),
    GINGERBREAD(9),
    GINGERBREAD_MR1(10),
    HONEYCOMB(11),
    HONEYCOMB_MR1(12),
    HONEYCOMB_MR2(13),
    ICE_CREAM_SANDWICH(14),
    ICE_CREAM_SANDWICH_MR1(15),
    JELLY_BEAN(16),
    JELLY_BEAN_MR1(17),
    JELLY_BEAN_MR2(18),
    KITKAT(19),
    CUR_DEVELOPMENT(10000);

    private int mApiLevel;

    public static VersionCode currentApiLevel() {
        return forApiLevel(Build.VERSION.SDK_INT);
    }

    private static VersionCode forApiLevel(int targetApiLevel) {
        for (VersionCode versionCode : VersionCode.values()) {
            if (versionCode.getApiLevel() == targetApiLevel) {
                return versionCode;
            }
        }
        return CUR_DEVELOPMENT;
    }

    private VersionCode(int apiLevel) {
        this.mApiLevel = apiLevel;
    }

    public int getApiLevel() {
        return mApiLevel;
    }

    public boolean isAtMost(VersionCode that) {
        return this.getApiLevel() <= that.getApiLevel();
    }

    public boolean isAtLeast(VersionCode that) {
        return this.getApiLevel() >= that.getApiLevel();
    }

    public boolean isBelow(VersionCode that) {
        return this.getApiLevel() < that.getApiLevel();
    }
}
