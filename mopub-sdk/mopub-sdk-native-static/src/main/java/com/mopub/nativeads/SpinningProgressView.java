package com.mopub.nativeads;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.mopub.common.Preconditions;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Views;

import static android.widget.RelativeLayout.LayoutParams.MATCH_PARENT;

class SpinningProgressView extends ViewGroup {
    @NonNull private final ProgressBar mProgressBar;
    private int mProgressIndicatorRadius;

    SpinningProgressView(@NonNull final Context context) {
        super(context);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        setLayoutParams(params);

        setVisibility(GONE);
        setBackgroundColor(Color.BLACK);
        getBackground().setAlpha(180);

        mProgressBar = new ProgressBar(context);
        mProgressIndicatorRadius = Dips.asIntPixels(25, getContext());
        mProgressBar.setIndeterminate(true);
        addView(mProgressBar);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            final int centerX = (left + right) / 2;
            final int centerY = (top + bottom) / 2;
            mProgressBar.layout(
                    centerX - mProgressIndicatorRadius,
                    centerY - mProgressIndicatorRadius,
                    centerX + mProgressIndicatorRadius,
                    centerY + mProgressIndicatorRadius
            );
        }

    }

    boolean addToRoot(@NonNull final View view) {
        Preconditions.checkNotNull(view);

        final View rootView = view.getRootView();

        if (rootView != null && rootView instanceof ViewGroup) {
            final ViewGroup rootViewGroup = (ViewGroup) rootView;

            setVisibility(VISIBLE);
            setMeasuredDimension(rootView.getWidth(), rootView.getHeight());

            rootViewGroup.addView(this);
            forceLayout();
            return true;
        }

        return false;
    }

    boolean removeFromRoot() {
        Views.removeFromParent(this);
        setVisibility(GONE);
        return true;
    }
}
