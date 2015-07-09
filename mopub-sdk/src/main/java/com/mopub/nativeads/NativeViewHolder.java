package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

class NativeViewHolder {
    @Nullable View mainView;
    @Nullable TextView titleView;
    @Nullable TextView textView;
    @Nullable TextView callToActionView;
    @Nullable ImageView mainImageView;
    @Nullable ImageView iconImageView;

    @VisibleForTesting
    static final NativeViewHolder EMPTY_VIEW_HOLDER = new NativeViewHolder();

    // Use fromViewBinder instead of a constructor
    private NativeViewHolder() {}

    @NonNull
    static NativeViewHolder fromViewBinder(@NonNull final View view,
            @NonNull final ViewBinder viewBinder) {
        final NativeViewHolder nativeViewHolder = new NativeViewHolder();
        nativeViewHolder.mainView = view;
        try {
            nativeViewHolder.titleView = (TextView) view.findViewById(viewBinder.titleId);
            nativeViewHolder.textView = (TextView) view.findViewById(viewBinder.textId);
            nativeViewHolder.callToActionView = (TextView) view.findViewById(viewBinder.callToActionId);
            nativeViewHolder.mainImageView = (ImageView) view.findViewById(viewBinder.mainImageId);
            nativeViewHolder.iconImageView = (ImageView) view.findViewById(viewBinder.iconImageId);
            return nativeViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.w("Could not cast from id in ViewBinder to expected View type", exception);
            return EMPTY_VIEW_HOLDER;
        }
    }

    void update(@NonNull final NativeResponse nativeResponse) {
        addTextView(titleView, nativeResponse.getTitle());
        addTextView(textView, nativeResponse.getText());
        addTextView(callToActionView, nativeResponse.getCallToAction());
        nativeResponse.loadMainImage(mainImageView);
        nativeResponse.loadIconImage(iconImageView);
    }

    void updateExtras(@NonNull final NativeResponse nativeResponse,
                      @NonNull final ViewBinder viewBinder) {
        if (mainView == null) {
            MoPubLog.w("Attempted to bind extras on a null main view.");
            return;
        }
        for (final String key : viewBinder.extras.keySet()) {
            final int resourceId = viewBinder.extras.get(key);
            final View view = mainView.findViewById(resourceId);
            final Object content = nativeResponse.getExtra(key);

            if (view instanceof ImageView) {
                // Clear previous image
                ((ImageView) view).setImageDrawable(null);
                nativeResponse.loadExtrasImage(key, (ImageView) view);
            } else if (view instanceof TextView) {
                // Clear previous text value
                ((TextView) view).setText(null);
                if (content instanceof String) {
                    addTextView((TextView) view, (String) content);
                }
            } else {
                MoPubLog.d("View bound to " + key + " should be an instance of TextView or ImageView.");
            }
        }
    }

    private void addTextView(@Nullable final TextView textView, @Nullable final String contents) {
        if (textView == null) {
            MoPubLog.d("Attempted to add text (" + contents + ") to null TextView.");
            return;
        }

        // Clear previous value
        textView.setText(null);

        if (contents == null) {
            MoPubLog.d("Attempted to set TextView contents to null.");
        } else {
            textView.setText(contents);
        }
    }

    public void setViewVisibility(final int visibility) {
        if (mainView != null) {
            mainView.setVisibility(visibility);
        }
    }
}
