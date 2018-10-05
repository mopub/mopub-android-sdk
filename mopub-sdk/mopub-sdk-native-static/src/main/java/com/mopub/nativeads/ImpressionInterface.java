// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.view.View;

/**
 * This interface should be implemented by native ad formats that want to make use of the
 * {@link ImpressionTracker} to track impressions.
 */
public interface ImpressionInterface {
    int getImpressionMinPercentageViewed();
    Integer getImpressionMinVisiblePx();
    int getImpressionMinTimeViewed();
    void recordImpression(View view);
    boolean isImpressionRecorded();
    void setImpressionRecorded();
}
