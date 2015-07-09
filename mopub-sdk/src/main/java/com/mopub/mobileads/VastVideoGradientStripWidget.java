package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.util.Dips;
import com.mopub.mobileads.resource.DrawableConstants;

public class VastVideoGradientStripWidget extends ImageView {
    public VastVideoGradientStripWidget(@NonNull final Context context,
            @NonNull final GradientDrawable.Orientation gradientOrientation, final int layoutVerb,
            final int layoutAnchor) {
        super(context);

        final GradientDrawable gradientDrawable = new GradientDrawable(gradientOrientation,
                new int[] {DrawableConstants.GradientStrip.START_COLOR,
                        DrawableConstants.GradientStrip.END_COLOR});
        setImageDrawable(gradientDrawable);

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                Dips.dipsToIntPixels(DrawableConstants.GradientStrip.GRADIENT_STRIP_HEIGHT_DIPS,
                        context));
        layoutParams.addRule(layoutVerb, layoutAnchor);
        setLayoutParams(layoutParams);
    }
}
