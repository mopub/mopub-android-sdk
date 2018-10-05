// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import com.mopub.mobileads.VastVideoView;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowVideoView;

@Implements(VastVideoView.class)
public class ShadowVastVideoView extends ShadowVideoView {
}
