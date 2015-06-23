package com.mopub.mobileads;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.resource.CountdownDrawable;
import com.mopub.mobileads.resource.LearnMoreDrawable;

import static android.graphics.Color.BLACK;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

class VastVideoToolbar extends LinearLayout {
    private static final int TOOLBAR_HEIGHT_DIPS = 44;
    private static final int THRESHOLD_FOR_HIDING_VIDEO_DURATION = 200;

    private final ToolbarWidget mDurationWidget;
    private final ToolbarWidget mLearnMoreWidget;
    private final ToolbarWidget mCountdownWidget;
    private final ToolbarWidget mCloseButtonWidget;

    public VastVideoToolbar(final Context context) {
        super(context);

        setId((int) Utils.generateUniqueId());

        // Consume all click events on the video toolbar
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        final int videoToolbarHeight = Dips.dipsToIntPixels(TOOLBAR_HEIGHT_DIPS, getContext());
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                MATCH_PARENT,
                videoToolbarHeight);
        setLayoutParams(layoutParams);

        setBackgroundColor(BLACK);
        getBackground().setAlpha(180);

        mDurationWidget = createDurationWidget();
        mLearnMoreWidget = createLearnMoreWidget();
        mCountdownWidget = createCountdownWidget();
        mCloseButtonWidget = createCloseButtonWidget();

        addView(mDurationWidget);
        addView(mLearnMoreWidget);
        addView(mCountdownWidget);
        addView(mCloseButtonWidget);
    }

    /**
     * Rounds up to the nearest full second. Formally, this is the long
     * closest to negative infinity above or equal to millis, in milliseconds,
     * converted to seconds.
     *
     * @param millis Time in milliseconds
     * @return Time in seconds, rounded up.
     */
    @VisibleForTesting
    long convertMillisecondsToSecondsRoundedUp(final long millis) {
        return Math.round(Math.ceil(millis / 1000f));
    }

    @VisibleForTesting
    void updateDurationWidget(final int remainingTime) {
        if (remainingTime >= THRESHOLD_FOR_HIDING_VIDEO_DURATION) {
            mDurationWidget.updateText("Ends in " + formatTime(remainingTime));
        } else if (remainingTime >= 0) {
            mDurationWidget.updateText("Thanks for watching");
        }
    }

    @VisibleForTesting
    String formatTime(final long milliseconds) {
        final long seconds = convertMillisecondsToSecondsRoundedUp(milliseconds);

        if (seconds == 1) {
            return "1 second";
        } else {
            return String.valueOf(seconds) + " seconds";
        }
    }

    void updateCountdownWidget(final int remainingTime) {
        if (remainingTime >= 0 && mCountdownWidget.getVisibility() == View.INVISIBLE) {
            mCloseButtonWidget.setVisibility(View.GONE);
            mCountdownWidget.setVisibility(View.VISIBLE);
        }

        mCountdownWidget.updateImageText(String.valueOf(convertMillisecondsToSecondsRoundedUp(
                remainingTime)));
    }

    void updateLearnMoreButtonText(final String customCtaText) {
        mLearnMoreWidget.updateText(customCtaText);
    }

    void updateCloseButtonText(final String customSkipText) {
        mCloseButtonWidget.updateText(customSkipText);
    }

    void updateCloseButtonIcon(final String customCloseIconUrl) {
        mCloseButtonWidget.updateImage(customCloseIconUrl);
    }

    void makeInteractable() {
        // The countdown timer has ended and user can interact with close and learn more button
        mCountdownWidget.setVisibility(View.GONE);
        mLearnMoreWidget.setVisibility(View.VISIBLE);
        mCloseButtonWidget.setVisibility(View.VISIBLE);
    }

    void setCloseButtonOnTouchListener(final OnTouchListener onTouchListener) {
        mCloseButtonWidget.setOnTouchListener(onTouchListener);
    }

    void setLearnMoreButtonOnTouchListener(final OnTouchListener onTouchListener) {
        mLearnMoreWidget.setOnTouchListener(onTouchListener);
    }

    private ToolbarWidget createDurationWidget() {
        return new ToolbarWidget.Builder(getContext())
                .weight(2f)
                .widgetGravity(CENTER_VERTICAL | LEFT)
                .hasText()
                .textAlign(RelativeLayout.ALIGN_PARENT_LEFT)
                .build();
    }

    private ToolbarWidget createLearnMoreWidget() {
        return new ToolbarWidget.Builder(getContext())
                .weight(1f)
                .widgetGravity(CENTER_VERTICAL | RIGHT)
                .defaultText("Learn More")
                .drawable(new LearnMoreDrawable())
                .visibility(View.INVISIBLE)
                .build();
    }

    private ToolbarWidget createCountdownWidget() {
        final CountdownDrawable countdownDrawable = new CountdownDrawable(getContext());

        return new ToolbarWidget.Builder(getContext())
                .weight(1f)
                .widgetGravity(CENTER_VERTICAL | RIGHT)
                .defaultText("Skip in")
                .drawable(countdownDrawable)
                .visibility(View.INVISIBLE)
                .build();
    }

    private ToolbarWidget createCloseButtonWidget() {
        return new ToolbarWidget.Builder(getContext())
                .weight(1f)
                .widgetGravity(CENTER_VERTICAL | RIGHT)
                .defaultText("Close")
                .drawable(new CloseButtonDrawable())
                .visibility(View.GONE)
                .build();
    }

    @Deprecated // for testing
    ToolbarWidget getDurationWidget() {
        return mDurationWidget;
    }

    @Deprecated // for testing
    ToolbarWidget getLearnMoreWidget() {
        return mLearnMoreWidget;
    }

    @Deprecated // for testing
    ToolbarWidget getCountdownWidget() {
        return mCountdownWidget;
    }

    @Deprecated // for testing
    ToolbarWidget getCloseButtonWidget() {
        return mCloseButtonWidget;
    }
}
