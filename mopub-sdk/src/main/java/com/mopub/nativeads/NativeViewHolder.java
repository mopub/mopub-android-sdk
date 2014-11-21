package com.mopub.nativeads;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mopub.common.logging.MoPubLog;

class NativeViewHolder {
    TextView titleView;
    TextView textView;
    TextView callToActionView;
    ImageView mainImageView;
    ImageView iconImageView;

    // Use fromViewBinder instead of a constructor
    private NativeViewHolder() {}

    static NativeViewHolder fromViewBinder(final View view, final ViewBinder viewBinder) {
        final NativeViewHolder nativeViewHolder = new NativeViewHolder();

        try {
            nativeViewHolder.titleView = (TextView) view.findViewById(viewBinder.titleId);
            nativeViewHolder.textView = (TextView) view.findViewById(viewBinder.textId);
            nativeViewHolder.callToActionView = (TextView) view.findViewById(viewBinder.callToActionId);
            nativeViewHolder.mainImageView = (ImageView) view.findViewById(viewBinder.mainImageId);
            nativeViewHolder.iconImageView = (ImageView) view.findViewById(viewBinder.iconImageId);
        } catch (ClassCastException e) {
            MoPubLog.d("Could not cast View from id in ViewBinder to expected View type", e);
            return null;
        }

        return nativeViewHolder;
    }

    void update(final NativeResponse nativeResponse) {
        addTextView(titleView, nativeResponse.getTitle());
        addTextView(textView, nativeResponse.getText());
        addTextView(callToActionView, nativeResponse.getCallToAction());

        nativeResponse.loadMainImage(mainImageView);
        nativeResponse.loadIconImage(iconImageView);
    }

    void updateExtras(final View outerView,
                      final NativeResponse nativeResponse,
                      final ViewBinder viewBinder) {
        for (final String key : viewBinder.extras.keySet()) {
            final int resourceId = viewBinder.extras.get(key);
            final View view = outerView.findViewById(resourceId);
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

    private void addTextView(final TextView textView, final String contents) {
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
}
