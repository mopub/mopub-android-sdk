package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

class MediaViewHolder {
    @Nullable View mainView;
    @Nullable MediaLayout mediaLayout;
    @Nullable TextView titleView;
    @Nullable TextView textView;
    @Nullable ImageView iconImageView;
    @Nullable TextView callToActionView;
    @Nullable ImageView privacyInformationIconImageView;

    @VisibleForTesting
    static final MediaViewHolder EMPTY_MEDIA_VIEW_HOLDER = new MediaViewHolder();

    // Use fromViewBinder instead of a constructor
    private MediaViewHolder() {}

    @NonNull
    static MediaViewHolder fromViewBinder(@NonNull final View view,
            @NonNull final MediaViewBinder mediaViewBinder) {
        final MediaViewHolder mediaViewHolder = new MediaViewHolder();
        mediaViewHolder.mainView = view;
        try {
            mediaViewHolder.titleView = (TextView) view.findViewById(mediaViewBinder.titleId);
            mediaViewHolder.textView = (TextView) view.findViewById(mediaViewBinder.textId);
            mediaViewHolder.callToActionView =
                    (TextView) view.findViewById(mediaViewBinder.callToActionId);
            mediaViewHolder.mediaLayout = (MediaLayout) view.findViewById(mediaViewBinder.mediaLayoutId);
            mediaViewHolder.iconImageView =
                    (ImageView) view.findViewById(mediaViewBinder.iconImageId);
            mediaViewHolder.privacyInformationIconImageView =
                    (ImageView) view.findViewById(mediaViewBinder.privacyInformationIconImageId);
            return mediaViewHolder;
        } catch (ClassCastException exception) {
            MoPubLog.w("Could not cast from id in MediaViewBinder to expected View type",
                    exception);
            return EMPTY_MEDIA_VIEW_HOLDER;
        }
    }
}
