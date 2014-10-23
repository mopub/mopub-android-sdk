package com.mopub.mobileads.util.vast;

import android.content.Context;
import android.view.Display;
import android.view.WindowManager;
import com.mopub.common.CacheService;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.mobileads.VastVideoDownloadTask;

import java.util.*;

import static com.mopub.mobileads.VastVideoDownloadTask.VastVideoDownloadTaskListener;
import static com.mopub.mobileads.util.vast.VastXmlManagerAggregator.VastXmlManagerAggregatorListener;

public class VastManager implements VastXmlManagerAggregatorListener {
    public interface VastManagerListener {
        public void onVastVideoConfigurationPrepared(final VastVideoConfiguration vastVideoConfiguration);
    }

    private static final double ASPECT_RATIO_WEIGHT = 40;
    private static final double AREA_WEIGHT = 60;
    private static final List<String> VIDEO_MIME_TYPES =
            Arrays.asList("video/mp4", "video/3gpp");
    private static final List<String> COMPANION_IMAGE_MIME_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/bmp", "image/gif");

    private VastManagerListener mVastManagerListener;

    private VastXmlManagerAggregator mVastXmlManagerAggregator;
    private double mScreenAspectRatio;
    private int mScreenArea;

    public VastManager(final Context context) {
        initializeScreenDimensions(context);
    }

    public void prepareVastVideoConfiguration(final String vastXml, final VastManagerListener vastManagerListener) {
        if (mVastXmlManagerAggregator == null) {
            mVastManagerListener = vastManagerListener;
            mVastXmlManagerAggregator = new VastXmlManagerAggregator(this);

            try {
                AsyncTasks.safeExecuteOnExecutor(mVastXmlManagerAggregator, vastXml);
            } catch (Exception e) {
                MoPubLog.d("Failed to aggregate vast xml", e);

                if (mVastManagerListener != null) {
                    mVastManagerListener.onVastVideoConfigurationPrepared(null);
                }
            }
        }
    }

    public void cancel() {
        if (mVastXmlManagerAggregator != null) {
            mVastXmlManagerAggregator.cancel(true);
            mVastXmlManagerAggregator = null;
        }
    }

    @Override
    public void onAggregationComplete(final List<VastXmlManager> vastXmlManagers) {
        mVastXmlManagerAggregator = null;
        if (vastXmlManagers == null) {
            if (mVastManagerListener != null) {
                mVastManagerListener.onVastVideoConfigurationPrepared(null);
            }
            return;
        }

        final VastVideoConfiguration vastVideoConfiguration =
                createVastVideoConfigurationFromXml(vastXmlManagers);

        if (updateDiskMediaFileUrl(vastVideoConfiguration)) {
            if (mVastManagerListener != null) {
                mVastManagerListener.onVastVideoConfigurationPrepared(vastVideoConfiguration);
            }
            return;
        }

        final VastVideoDownloadTask vastVideoDownloadTask = new VastVideoDownloadTask(
                new VastVideoDownloadTaskListener() {
                    @Override
                    public void onComplete(boolean success) {
                        if (success && updateDiskMediaFileUrl(vastVideoConfiguration)) {
                            if (mVastManagerListener != null) {
                                mVastManagerListener.onVastVideoConfigurationPrepared(vastVideoConfiguration);
                            }
                        } else {
                            if (mVastManagerListener != null) {
                                mVastManagerListener.onVastVideoConfigurationPrepared(null);
                            }
                        }
                    }
                }
        );

        try {
            AsyncTasks.safeExecuteOnExecutor(
                    vastVideoDownloadTask,
                    vastVideoConfiguration.getNetworkMediaFileUrl()
            );
        } catch (Exception e) {
            MoPubLog.d("Failed to download vast video", e);

            if (mVastManagerListener != null) {
                mVastManagerListener.onVastVideoConfigurationPrepared(null);
            }
        }
    }

    private boolean updateDiskMediaFileUrl(final VastVideoConfiguration vastVideoConfiguration) {
        final String networkMediaFileUrl = vastVideoConfiguration.getNetworkMediaFileUrl();
        if (CacheService.containsKeyDiskCache(networkMediaFileUrl)) {
            final String filePathDiskCache = CacheService.getFilePathDiskCache(networkMediaFileUrl);
            vastVideoConfiguration.setDiskMediaFileUrl(filePathDiskCache);
            return true;
        }
        return false;
    }

    private void initializeScreenDimensions(final Context context) {
        // This currently assumes that all vast videos will be played in landscape
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int x = display.getWidth();
        int y = display.getHeight();

        // For landscape, width is always greater than height
        int screenWidth = Math.max(x, y);
        int screenHeight = Math.min(x, y);
        mScreenAspectRatio = (double) screenWidth / screenHeight;
        mScreenArea = screenWidth * screenHeight;
    }

    private VastVideoConfiguration createVastVideoConfigurationFromXml(final List<VastXmlManager> xmlManagers) {
        final VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();

        final List<VastXmlManager.MediaXmlManager> mediaXmlManagers = new ArrayList<VastXmlManager.MediaXmlManager>();
        final List<VastXmlManager.ImageCompanionAdXmlManager> companionXmlManagers = new ArrayList<VastXmlManager.ImageCompanionAdXmlManager>();
        for (VastXmlManager xmlManager : xmlManagers) {
            vastVideoConfiguration.addImpressionTrackers(xmlManager.getImpressionTrackers());

            vastVideoConfiguration.addStartTrackers(xmlManager.getVideoStartTrackers());
            vastVideoConfiguration.addFirstQuartileTrackers(xmlManager.getVideoFirstQuartileTrackers());
            vastVideoConfiguration.addMidpointTrackers(xmlManager.getVideoMidpointTrackers());
            vastVideoConfiguration.addThirdQuartileTrackers(xmlManager.getVideoThirdQuartileTrackers());
            vastVideoConfiguration.addCompleteTrackers(xmlManager.getVideoCompleteTrackers());

            vastVideoConfiguration.addClickTrackers(xmlManager.getClickTrackers());

            if (vastVideoConfiguration.getClickThroughUrl() == null) {
                vastVideoConfiguration.setClickThroughUrl(xmlManager.getClickThroughUrl());
            }

            mediaXmlManagers.addAll(xmlManager.getMediaXmlManagers());
            companionXmlManagers.addAll(xmlManager.getCompanionAdXmlManagers());
        }

        vastVideoConfiguration.setNetworkMediaFileUrl(getBestMediaFileUrl(mediaXmlManagers));
        vastVideoConfiguration.setVastCompanionAd(getBestCompanionAd(companionXmlManagers));

        return vastVideoConfiguration;
    }

    String getBestMediaFileUrl(final List<VastXmlManager.MediaXmlManager> managers) {
        final List<VastXmlManager.MediaXmlManager> mediaXmlManagers = new ArrayList<VastXmlManager.MediaXmlManager>(managers);
        double bestMediaFitness = Double.POSITIVE_INFINITY;
        String bestMediaFileUrl = null;

        final Iterator<VastXmlManager.MediaXmlManager> xmlManagerIterator = mediaXmlManagers.iterator();
        while (xmlManagerIterator.hasNext()) {
            final VastXmlManager.MediaXmlManager mediaXmlManager = xmlManagerIterator.next();

            final String mediaType = mediaXmlManager.getType();
            final String mediaUrl = mediaXmlManager.getMediaUrl();
            if (!VIDEO_MIME_TYPES.contains(mediaType) || mediaUrl == null) {
                xmlManagerIterator.remove();
                continue;
            }

            final Integer mediaWidth = mediaXmlManager.getWidth();
            final Integer mediaHeight = mediaXmlManager.getHeight();
            if (mediaWidth == null || mediaWidth <= 0 || mediaHeight == null || mediaHeight <= 0) {
                continue;
            }

            final double mediaFitness = calculateFitness(mediaWidth, mediaHeight);
            if (mediaFitness < bestMediaFitness) {
                bestMediaFitness = mediaFitness;
                bestMediaFileUrl = mediaUrl;
            }
        }

        if (bestMediaFileUrl == null && !mediaXmlManagers.isEmpty()) {
            bestMediaFileUrl = mediaXmlManagers.get(0).getMediaUrl();
        }

        return bestMediaFileUrl;
    }

    VastCompanionAd getBestCompanionAd(final List<VastXmlManager.ImageCompanionAdXmlManager> managers) {
        final List<VastXmlManager.ImageCompanionAdXmlManager> companionXmlManagers =
                new ArrayList<VastXmlManager.ImageCompanionAdXmlManager>(managers);
        double bestCompanionFitness = Double.POSITIVE_INFINITY;
        VastXmlManager.ImageCompanionAdXmlManager bestCompanionXmlManager = null;

        final Iterator<VastXmlManager.ImageCompanionAdXmlManager> xmlManagerIterator = companionXmlManagers.iterator();
        while (xmlManagerIterator.hasNext()) {
            final VastXmlManager.ImageCompanionAdXmlManager companionXmlManager = xmlManagerIterator.next();

            final String imageType = companionXmlManager.getType();
            final String imageUrl = companionXmlManager.getImageUrl();
            if (!COMPANION_IMAGE_MIME_TYPES.contains(imageType) || imageUrl == null) {
                xmlManagerIterator.remove();
                continue;
            }

            final Integer imageWidth = companionXmlManager.getWidth();
            final Integer imageHeight = companionXmlManager.getHeight();
            if (imageWidth == null || imageWidth <= 0 || imageHeight == null || imageHeight <= 0) {
                continue;
            }

            final double companionFitness = calculateFitness(imageWidth, imageHeight);
            if (companionFitness < bestCompanionFitness) {
                bestCompanionFitness = companionFitness;
                bestCompanionXmlManager = companionXmlManager;
            }
        }

        if (bestCompanionXmlManager == null && !companionXmlManagers.isEmpty()) {
            bestCompanionXmlManager = companionXmlManagers.get(0);
        }

        if (bestCompanionXmlManager != null) {
            return new VastCompanionAd(
                    bestCompanionXmlManager.getWidth(),
                    bestCompanionXmlManager.getHeight(),
                    bestCompanionXmlManager.getImageUrl(),
                    bestCompanionXmlManager.getClickThroughUrl(),
                    new ArrayList<String>(bestCompanionXmlManager.getClickTrackers())
            );
        }
        return null;
    }

    private double calculateFitness(final int width, final int height) {
        final double mediaAspectRatio = (double) width / height;
        final int mediaArea = width * height;
        final double aspectRatioRatio = mediaAspectRatio / mScreenAspectRatio;
        final double areaRatio = (double) mediaArea / mScreenArea;
        return ASPECT_RATIO_WEIGHT * Math.abs(Math.log(aspectRatioRatio))
                + AREA_WEIGHT * Math.abs(Math.log(areaRatio));
    }

    @Deprecated // for testing
    int getScreenArea() {
        return mScreenArea;
    }

    @Deprecated // for testing
    double getScreenAspectRatio() {
        return mScreenAspectRatio;
    }
}
