package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.util.Dips;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.TextDrawable;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

class ToolbarWidget extends RelativeLayout {
    static class Builder {
        private final Context context;
        private float weight;
        private int widgetGravity;

        private boolean hasText;
        private String defaultText;

        private boolean hasDrawable;
        private Drawable drawable;

        private OnTouchListener onTouchListener;
        private int visibility;
        private int textAlign;
        private int drawableAlign;

        Builder(final Context context) {
            this.context = context;
            this.weight = 1f;
            this.widgetGravity = Gravity.CENTER;

            this.visibility = View.VISIBLE;

            this.textAlign = ALIGN_PARENT_LEFT;
            this.drawableAlign = ALIGN_PARENT_RIGHT;
        }

        Builder weight(final float weight) {
            this.weight = weight;
            return this;
        }

        Builder widgetGravity(final int widgetGravity) {
            this.widgetGravity = widgetGravity;
            return this;
        }

        Builder hasText() {
            this.hasText = true;
            return this;
        }

        Builder defaultText(final String defaultText) {
            this.hasText = true;
            this.defaultText = defaultText;
            return this;
        }

        Builder hasDrawable() {
            this.hasDrawable = true;
            return this;
        }

        Builder drawable(final Drawable drawable) {
            this.hasDrawable = true;
            this.drawable = drawable;
            return this;
        }

        Builder textAlign(final int rule) {
            this.textAlign = rule;
            return this;
        }

        Builder drawableAlign(final int rule) {
            this.drawableAlign = rule;
            return this;
        }

        Builder visibility(final int visibility) {
            this.visibility = visibility;
            return this;
        }

        Builder onTouchListener(final OnTouchListener onTouchListener) {
            this.onTouchListener = onTouchListener;
            return this;
        }

        ToolbarWidget build() {
            return new ToolbarWidget(this);
        }
    }

    private TextView mTextView;
    private ImageView mImageView;

    private static final int TEXT_PADDING_DIPS = 5;
    private static final int IMAGE_PADDING_DIPS = 5;
    private static final int IMAGE_SIDE_LENGTH_DIPS = 37;

    private final int mTextPadding;
    private final int mImagePadding;
    private final int mImageSideLength;

    private ToolbarWidget(Builder builder) {
        super(builder.context);

        final LinearLayout.LayoutParams toolbarLayoutParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, builder.weight);
        toolbarLayoutParams.gravity = builder.widgetGravity;
        setLayoutParams(toolbarLayoutParams);

        mTextPadding = Dips.dipsToIntPixels(TEXT_PADDING_DIPS, getContext());
        mImagePadding = Dips.dipsToIntPixels(IMAGE_PADDING_DIPS, getContext());
        mImageSideLength = Dips.dipsToIntPixels(IMAGE_SIDE_LENGTH_DIPS, getContext());

        setVisibility(builder.visibility);

        if (builder.hasDrawable) {
            if (builder.drawable != null) {
                mImageView = new ImageView(getContext());
                mImageView.setId((int) Utils.generateUniqueId());

                final RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                        mImageSideLength,
                        mImageSideLength);

                iconLayoutParams.addRule(CENTER_VERTICAL);
                iconLayoutParams.addRule(builder.drawableAlign);

                mImageView.setPadding(mImagePadding, mImagePadding, mImagePadding, mImagePadding);

                mImageView.setBackgroundColor(Color.BLACK);
                mImageView.getBackground().setAlpha(0);
                mImageView.setImageDrawable(builder.drawable);
                addView(mImageView, iconLayoutParams);
            }
        }

        if (builder.hasText) {
            mTextView = new TextView(getContext());
            mTextView.setSingleLine();
            mTextView.setEllipsize(TextUtils.TruncateAt.END);
            mTextView.setText(builder.defaultText);

            final RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            textLayoutParams.addRule(CENTER_VERTICAL);

            if (mImageView != null) {
                textLayoutParams.addRule(LEFT_OF, mImageView.getId());
            } else {
                textLayoutParams.addRule(builder.textAlign);
            }

            mTextView.setPadding(mTextPadding, mTextPadding, mTextPadding, mTextPadding);

            addView(mTextView, textLayoutParams);
        }

        if (builder.onTouchListener != null) {
            setOnTouchListener(builder.onTouchListener);
        }
    }

    void updateText(final String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    void updateImageText(final String text) {
        try {
            final TextDrawable textDrawable = (TextDrawable) mImageView.getDrawable();
            textDrawable.updateText(text);
        } catch (Exception e) {
            MoPubLog.d("Unable to update ToolbarWidget text.");
        }
    }

    @Deprecated // for testing
    TextDrawable getImageViewDrawable() {
        return (TextDrawable) mImageView.getDrawable();
    }

    @Deprecated // for testing
    void setImageViewDrawable(TextDrawable drawable) {
        mImageView.setImageDrawable((Drawable) drawable);
    }
}
