// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.base.R;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.ImageLoader;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;

public class VastVideoCloseButtonWidget extends RelativeLayout {
    @NonNull private TextView mTextView;
    @NonNull private ImageView mImageView;
    @NonNull private final ImageLoader mImageLoader;
    private boolean mHasCustomImage;

    private final int mEdgePadding;
    private final int mTextRightMargin;
    private final int mImagePadding;
    private final int mWidgetHeight;

    public VastVideoCloseButtonWidget(@NonNull final Context context) {
        super(context);

        setId(View.generateViewId());

        mEdgePadding = Dips.dipsToIntPixels(DrawableConstants.CloseButton.EDGE_PADDING, context);
        mImagePadding = Dips.dipsToIntPixels(DrawableConstants.CloseButton.IMAGE_PADDING_DIPS, context);
        mWidgetHeight = Dips.dipsToIntPixels(DrawableConstants.CloseButton.WIDGET_HEIGHT_DIPS, context);
        mTextRightMargin = Dips.dipsToIntPixels(DrawableConstants.CloseButton.TEXT_RIGHT_MARGIN_DIPS, context);

        mImageLoader = Networking.getImageLoader(context);

        createImageView();
        createTextView();

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                WRAP_CONTENT,
                mWidgetHeight);

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP | RelativeLayout.ALIGN_PARENT_RIGHT);
        setLayoutParams(layoutParams);
    }

    private void createImageView() {
        mImageView = new ImageView(getContext());
        mImageView.setId(View.generateViewId());

        final RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                mWidgetHeight,
                mWidgetHeight);

        iconLayoutParams.addRule(ALIGN_PARENT_RIGHT);
        mImageView.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_mopub_skip_button));
        mImageView.setPadding(mImagePadding, mImagePadding + mEdgePadding, mImagePadding + mEdgePadding, mImagePadding);
        addView(mImageView, iconLayoutParams);
    }

    private void createTextView() {
        mTextView = new TextView(getContext());
        mTextView.setSingleLine();
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        mTextView.setTextColor(DrawableConstants.CloseButton.TEXT_COLOR);
        mTextView.setTextSize(DrawableConstants.CloseButton.TEXT_SIZE_SP);
        mTextView.setTypeface(DrawableConstants.CloseButton.TEXT_TYPEFACE);
        mTextView.setText(DrawableConstants.CloseButton.DEFAULT_CLOSE_BUTTON_TEXT);

        final RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(
                WRAP_CONTENT,
                WRAP_CONTENT);

        textLayoutParams.addRule(CENTER_VERTICAL);
        textLayoutParams.addRule(LEFT_OF, mImageView.getId());

        mTextView.setPadding(0, mEdgePadding, 0, 0);
        // space between text and image
        textLayoutParams.setMargins(0, 0, mTextRightMargin, 0);

        addView(mTextView, textLayoutParams);
    }

    void updateCloseButtonText(@Nullable final String text) {
        if (mTextView != null) {
            mTextView.setText(text);
        }
    }

    void updateCloseButtonIcon(@NonNull final String imageUrl) {
        mImageLoader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(final ImageLoader.ImageContainer imageContainer,
                    final boolean isImmediate) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                    mHasCustomImage = true;
                } else {
                    MoPubLog.log(CUSTOM, String.format("%s returned null bitmap", imageUrl));
                }
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                MoPubLog.log(ERROR_WITH_THROWABLE, "Failed to load image.", volleyError);
            }
        });
    }

    void setOnTouchListenerToContent(@Nullable View.OnTouchListener onTouchListener) {
        mImageView.setOnTouchListener(onTouchListener);
        mTextView.setOnTouchListener(onTouchListener);
    }

    void notifyVideoComplete() {
        if (!mHasCustomImage) {
            mImageView.setImageDrawable(
                    ContextCompat.getDrawable(getContext(), R.drawable.ic_mopub_close_button));
        }
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ImageView getImageView() {
        return mImageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageView(ImageView imageView) {
        mImageView = imageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    TextView getTextView() {
        return mTextView;
    }
}
