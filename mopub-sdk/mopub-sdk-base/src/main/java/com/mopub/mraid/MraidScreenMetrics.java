package com.mopub.mraid;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;

import com.mopub.common.util.Dips;

/**
 * Screen metrics needed by the MRAID container.
 *
 * Each rectangle is stored using both it's original and scaled coordinates to avoid allocating
 * extra memory that would otherwise be needed to do these conversions.
 */
class MraidScreenMetrics {
    @NonNull private final Context mContext;
    @NonNull private final Rect mScreenRect;
    @NonNull private final Rect mScreenRectDips;

    @NonNull private final Rect mRootViewRect;
    @NonNull private final Rect mRootViewRectDips;

    @NonNull private final Rect mCurrentAdRect;
    @NonNull private final Rect mCurrentAdRectDips;

    @NonNull private final Rect mDefaultAdRect;
    @NonNull private final Rect mDefaultAdRectDips;

    private final float mDensity;

    MraidScreenMetrics(Context context, float density) {
        mContext = context.getApplicationContext();
        mDensity = density;

        mScreenRect = new Rect();
        mScreenRectDips = new Rect();

        mRootViewRect = new Rect();
        mRootViewRectDips = new Rect();

        mCurrentAdRect = new Rect();
        mCurrentAdRectDips = new Rect();

        mDefaultAdRect = new Rect();
        mDefaultAdRectDips = new Rect();
    }

    private void convertToDips(Rect sourceRect, Rect outRect) {
        outRect.set(Dips.pixelsToIntDips(sourceRect.left, mContext),
                Dips.pixelsToIntDips(sourceRect.top, mContext),
                Dips.pixelsToIntDips(sourceRect.right, mContext),
                Dips.pixelsToIntDips(sourceRect.bottom, mContext));
    }

    public float getDensity() {
        return mDensity;
    }

    void setScreenSize(int width, int height) {
        mScreenRect.set(0, 0, width, height);
        convertToDips(mScreenRect, mScreenRectDips);
    }

    @NonNull
    Rect getScreenRect() {
        return mScreenRect;
    }

    @NonNull
    Rect getScreenRectDips() {
        return mScreenRectDips;
    }

    void setRootViewPosition(int x, int y, int width, int height) {
        mRootViewRect.set(x, y, x + width, y + height);
        convertToDips(mRootViewRect, mRootViewRectDips);
    }

    @NonNull
    Rect getRootViewRect() {
        return mRootViewRect;
    }

    @NonNull
    Rect getRootViewRectDips() {
        return mRootViewRectDips;
    }

    void setCurrentAdPosition(int x, int y, int width, int height) {
        mCurrentAdRect.set(x, y, x + width, y + height);
        convertToDips(mCurrentAdRect, mCurrentAdRectDips);
    }

    @NonNull
    Rect getCurrentAdRect() {
        return mCurrentAdRect;
    }

    @NonNull
    Rect getCurrentAdRectDips() {
        return mCurrentAdRectDips;
    }

    void setDefaultAdPosition(int x, int y, int width, int height) {
        mDefaultAdRect.set(x, y, x + width, y + height);
        convertToDips(mDefaultAdRect, mDefaultAdRectDips);
    }

    @NonNull
    Rect getDefaultAdRect() {
        return mDefaultAdRect;
    }

    @NonNull
    Rect getDefaultAdRectDips() {
        return mDefaultAdRectDips;
    }
}
