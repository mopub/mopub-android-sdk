package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.ImageLoader;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class VastVideoCloseButtonWidget extends RelativeLayout {
    @NonNull private TextView mTextView;
    @NonNull private ImageView mImageView;
    @NonNull private final ImageLoader mImageLoader;
    @NonNull private CloseButtonDrawable mCloseButtonDrawable;

    private final int mEdgePadding;
    private final int mTextRightMargin;
    private final int mImagePadding;
    private final int mWidgetHeight;

    public VastVideoCloseButtonWidget(@NonNull final Context context) {
        super(context);

        setId((int) Utils.generateUniqueId());

        mEdgePadding = Dips.dipsToIntPixels(DrawableConstants.CloseButton.EDGE_PADDING, context);
        mImagePadding = Dips.dipsToIntPixels(DrawableConstants.CloseButton.IMAGE_PADDING_DIPS, context);
        mWidgetHeight = Dips.dipsToIntPixels(DrawableConstants.CloseButton.WIDGET_HEIGHT_DIPS, context);
        mTextRightMargin = Dips.dipsToIntPixels(DrawableConstants.CloseButton.TEXT_RIGHT_MARGIN_DIPS, context);

        mCloseButtonDrawable = new CloseButtonDrawable();
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
        mImageView.setId((int) Utils.generateUniqueId());

        final RelativeLayout.LayoutParams iconLayoutParams = new RelativeLayout.LayoutParams(
                mWidgetHeight,
                mWidgetHeight);

        iconLayoutParams.addRule(ALIGN_PARENT_RIGHT);

        mImageView.setImageDrawable(mCloseButtonDrawable);
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
                } else {
                    MoPubLog.d(String.format("%s returned null bitmap", imageUrl));
                }
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                MoPubLog.d("Failed to load image.", volleyError);
            }
        });
    }

    void setOnTouchListenerToContent(@Nullable View.OnTouchListener onTouchListener) {
        mImageView.setOnTouchListener(onTouchListener);
        mTextView.setOnTouchListener(onTouchListener);
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
