package com.mopub.nativeads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mopub.common.DownloadResponse;
import com.mopub.common.HttpClient;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.IntentUtils;
import com.mopub.common.util.ResponseHeader;
import com.mopub.nativeads.MoPubNative.MoPubNativeEventListener;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.view.View.OnClickListener;
import static com.mopub.nativeads.BaseForwardingNativeAd.NativeEventListener;
import static com.mopub.nativeads.NativeResponse.Parameter.CALL_TO_ACTION;
import static com.mopub.nativeads.NativeResponse.Parameter.CLICK_DESTINATION;
import static com.mopub.nativeads.NativeResponse.Parameter.CLICK_TRACKER;
import static com.mopub.nativeads.NativeResponse.Parameter.ICON_IMAGE;
import static com.mopub.nativeads.NativeResponse.Parameter.IMPRESSION_TRACKER;
import static com.mopub.nativeads.NativeResponse.Parameter.MAIN_IMAGE;
import static com.mopub.nativeads.NativeResponse.Parameter.STAR_RATING;
import static com.mopub.nativeads.NativeResponse.Parameter.TEXT;
import static com.mopub.nativeads.NativeResponse.Parameter.TITLE;
import static com.mopub.nativeads.UrlResolutionTask.UrlResolutionListener;
import static com.mopub.nativeads.UrlResolutionTask.getResolvedUrl;

public class NativeResponse {
    enum Parameter {
        IMPRESSION_TRACKER("imptracker", true),
        CLICK_TRACKER("clktracker", true),

        TITLE("title", false),
        TEXT("text", false),
        MAIN_IMAGE("mainimage", false),
        ICON_IMAGE("iconimage", false),

        CLICK_DESTINATION("clk", false),
        FALLBACK("fallback", false),
        CALL_TO_ACTION("ctatext", false),
        STAR_RATING("starrating", false);

        final String name;
        final boolean required;

        Parameter(final String name, boolean required) {
            this.name = name;
            this.required = required;
        }

        static Parameter from(final String name) {
            for (final Parameter parameter : values()) {
                if (parameter.name.equals(name)) {
                    return parameter;
                }
            }

            return null;
        }

        @VisibleForTesting
        static final Set<String> requiredKeys = new HashSet<String>();
        static {
            for (final Parameter parameter : values()) {
                if (parameter.required) {
                    requiredKeys.add(parameter.name);
                }
            }
        }
    }

    private final Context mContext;
    private MoPubNativeEventListener mMoPubNativeEventListener;
    private final NativeAdInterface mNativeAd;

    // Impression and click trackers for the MoPub adserver
    private final Set<String> mMoPubImpressionTrackers;
    private final String mMoPubClickTracker;
    private final String mAdUnitId;

    private boolean mRecordedImpression;
    private boolean mIsClicked;
    private boolean mIsDestroyed;

    public NativeResponse(final Context context,
            final DownloadResponse downloadResponse,
            final String adUnitId,
            final NativeAdInterface nativeAd,
            final MoPubNativeEventListener moPubNativeEventListener) {
        mContext = context.getApplicationContext();
        mAdUnitId = adUnitId;
        mMoPubNativeEventListener = moPubNativeEventListener;
        mNativeAd = nativeAd;
        mNativeAd.setNativeEventListener(new NativeEventListener() {
            @Override
            public void onAdImpressed() {
                recordImpression(null);
            }

            @Override
            public void onAdClicked() {
                handleClick(null);
            }
        });

        mMoPubImpressionTrackers = new HashSet<String>();
        mMoPubImpressionTrackers.add(downloadResponse.getFirstHeader(ResponseHeader.IMPRESSION_URL));
        mMoPubClickTracker = downloadResponse.getFirstHeader(ResponseHeader.CLICKTHROUGH_URL);
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder("\n");

        stringBuilder.append(TITLE.name).append(":").append(getTitle()).append("\n");
        stringBuilder.append(TEXT.name).append(":").append(getText()).append("\n");
        stringBuilder.append(ICON_IMAGE.name).append(":").append(getIconImageUrl()).append("\n");
        stringBuilder.append(MAIN_IMAGE.name).append(":").append(getMainImageUrl()).append("\n");
        stringBuilder.append(STAR_RATING.name).append(":").append(getStarRating()).append("\n");
        stringBuilder.append(IMPRESSION_TRACKER.name).append(":").append(getImpressionTrackers()).append("\n");
        stringBuilder.append(CLICK_TRACKER.name).append(":").append(mMoPubClickTracker).append("\n");
        stringBuilder.append(CLICK_DESTINATION.name).append(":").append(getClickDestinationUrl()).append("\n");
        stringBuilder.append(CALL_TO_ACTION.name).append(":").append(getCallToAction()).append("\n");
        stringBuilder.append("recordedImpression").append(":").append(mRecordedImpression).append("\n");
        stringBuilder.append("extras").append(":").append(getExtras());

        return stringBuilder.toString();
    }

   public String getAdUnitId() {
       return mAdUnitId;
   }

    // Interface Methods
    // Getters
    public String getMainImageUrl() {
        return mNativeAd.getMainImageUrl();
    }

    public String getIconImageUrl() {
        return mNativeAd.getIconImageUrl();
    }

    public String getClickDestinationUrl() {
        return mNativeAd.getClickDestinationUrl();
    }

    public String getCallToAction() {
        return mNativeAd.getCallToAction();
    }

    public String getTitle() {
        return mNativeAd.getTitle();
    }

    public String getText() {
        return mNativeAd.getText();
    }

    public List<String> getImpressionTrackers() {
        final Set<String> allImpressionTrackers = new HashSet<String>();
        allImpressionTrackers.addAll(mMoPubImpressionTrackers);
        allImpressionTrackers.addAll(mNativeAd.getImpressionTrackers());
        return new ArrayList<String>(allImpressionTrackers);
    }

    public String getClickTracker() {
        return mMoPubClickTracker;
    }

    public Double getStarRating() {
        return mNativeAd.getStarRating();
    }

    public int getImpressionMinTimeViewed() {
        return mNativeAd.getImpressionMinTimeViewed();
    }

    public int getImpressionMinPercentageViewed() {
        return mNativeAd.getImpressionMinPercentageViewed();
    }

    // Extras Getters
    public Object getExtra(final String key) {
        return mNativeAd.getExtra(key);
    }

    public Map<String, Object> getExtras() {
        return mNativeAd.getExtras();
    }

    public boolean isOverridingImpressionTracker() {
        return mNativeAd.isOverridingImpressionTracker();
    }

    public boolean isOverridingClickTracker() {
        return mNativeAd.isOverridingClickTracker();
    }

    // Event Handlers
    public void prepare(final View view) {
        if (isDestroyed()) {
            return;
        }

        if (!isOverridingClickTracker()) {
            setOnClickListener(view, new NativeViewClickListener());
        }

        mNativeAd.prepare(view);
    }

    public void recordImpression(final View view) {
        if (getRecordedImpression() || isDestroyed()) {
            return;
        }

        for (final String impressionTracker : getImpressionTrackers()) {
            HttpClient.makeTrackingHttpRequest(impressionTracker, mContext);
        }

        mNativeAd.recordImpression();
        mRecordedImpression = true;

        mMoPubNativeEventListener.onNativeImpression(view);
    }

    public void handleClick(final View view) {
        if (isDestroyed()) {
            return;
        }

        if (!isClicked()) {
            HttpClient.makeTrackingHttpRequest(mMoPubClickTracker, mContext);
        }

        openClickDestinationUrl(view);
        mNativeAd.handleClick(view);
        mIsClicked = true;

        mMoPubNativeEventListener.onNativeClick(view);
    }

    public void clear(final View view) {
        setOnClickListener(view, null);

        mNativeAd.clear(view);
    }

    public void destroy() {
        if (isDestroyed()) {
            return;
        }

        mMoPubNativeEventListener = MoPubNative.EMPTY_EVENT_LISTENER;

        mNativeAd.destroy();
        mIsDestroyed = true;
    }

    // Non Interface Public Methods
    public void loadMainImage(final ImageView imageView) {
        loadImageView(getMainImageUrl(), imageView);
    }

    public void loadIconImage(final ImageView imageView) {
        loadImageView(getIconImageUrl(), imageView);
    }

    public void loadExtrasImage(final String key, final ImageView imageView) {
        final Object object = getExtra(key);
        if (object != null && object instanceof String) {
            loadImageView((String) object, imageView);
        }
    }

    public boolean getRecordedImpression() {
        return mRecordedImpression;
    }

    public boolean isClicked() {
        return mIsClicked;
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
    }

    // Helpers
    private void loadImageView(final String url, final ImageView imageView) {
        ImageViewService.loadImageView(url, imageView);
    }

    private void openClickDestinationUrl(final View view) {
        if (getClickDestinationUrl() == null) {
            return;
        }

        SpinningProgressView spinningProgressView = null;
        if (view != null) {
            spinningProgressView = new SpinningProgressView(mContext);
            spinningProgressView.addToRoot(view);
        }

        final Iterator<String> urlIterator = Arrays.asList(getClickDestinationUrl()).iterator();
        final ClickDestinationUrlResolutionListener urlResolutionListener = new ClickDestinationUrlResolutionListener(
                mContext,
                urlIterator,
                spinningProgressView
        );

        getResolvedUrl(urlIterator.next(), urlResolutionListener);
    }

    private void setOnClickListener(final View view, final OnClickListener onClickListener) {
        view.setOnClickListener(onClickListener);
        if ((view instanceof ViewGroup)) {
            ViewGroup viewGroup = (ViewGroup)view;
            for (int i = 0; i < viewGroup.getChildCount(); i++)
                setOnClickListener(viewGroup.getChildAt(i), onClickListener);
        }
    }

    private static class ClickDestinationUrlResolutionListener implements UrlResolutionListener {
        private final Context mContext;
        private final Iterator<String> mUrlIterator;
        private final SoftReference<SpinningProgressView> mSpinningProgressView;

        public ClickDestinationUrlResolutionListener(final Context context,
                final Iterator<String> urlIterator,
                final SpinningProgressView spinningProgressView) {
            mContext = context.getApplicationContext();
            mUrlIterator = urlIterator;
            mSpinningProgressView = new SoftReference<SpinningProgressView>(spinningProgressView);
        }

        @Override
        public void onSuccess(final String resolvedUrl) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(resolvedUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (IntentUtils.isDeepLink(resolvedUrl) && IntentUtils.deviceCanHandleIntent(mContext, intent)) {
                // Open another Android app from deep link
                mContext.startActivity(intent);
            } else if (mUrlIterator.hasNext()) {
                // If we can't handle a deep link then try the fallback url
                getResolvedUrl(mUrlIterator.next(), this);
                return;
            } else {
                // If we can't open the deep link and there are no backup links
                // Or the link is a browser link then handle it here
                MoPubBrowser.open(mContext, resolvedUrl);
            }

            removeSpinningProgressView();
        }

        @Override
        public void onFailure() {
            MoPubLog.d("Failed to resolve URL for click.");
            removeSpinningProgressView();
        }

        private void removeSpinningProgressView() {
            final SpinningProgressView spinningProgressView = mSpinningProgressView.get();
            if (spinningProgressView != null) {
                spinningProgressView.removeFromRoot();
            }
        }
    }

    @VisibleForTesting
    class NativeViewClickListener implements OnClickListener {
        @Override
        public void onClick(final View view) {
            handleClick(view);
        }
    }

    @Deprecated
    public String getSubtitle() {
        return mNativeAd.getText();
    }

    @VisibleForTesting
    @Deprecated
    MoPubNativeEventListener getMoPubNativeEventListener() {
        return mMoPubNativeEventListener;
    }

    @VisibleForTesting
    @Deprecated
    void setRecordedImpression(final boolean recordedImpression) {
        mRecordedImpression = recordedImpression;
    }
}
