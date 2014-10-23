package com.mopub.mobileads;

import com.mopub.mobileads.MraidView.PlacementType;
import com.mopub.mobileads.MraidView.ViewState;

abstract class MraidProperty {
    private String sanitize(String str) {
        return (str != null) ? str.replaceAll("[^a-zA-Z0-9_,:\\s\\{\\}\\\'\\\"]", "") : "";
    }

    @Override
    public String toString() {
        return sanitize(toJsonPair());
    }

    public abstract String toJsonPair();
}

class MraidPlacementTypeProperty extends MraidProperty {
    private final PlacementType mPlacementType;

    MraidPlacementTypeProperty(PlacementType placementType) {
        mPlacementType = placementType;
    }

    public static MraidPlacementTypeProperty createWithType(
            PlacementType placementType) {
        return new MraidPlacementTypeProperty(placementType);
    }

    @Override
    public String toJsonPair() {
        return "placementType: '" + mPlacementType.toString().toLowerCase() + "'";
    }
}

class MraidScreenSizeProperty extends MraidProperty {
    private final int mScreenWidth;
    private final int mScreenHeight;

    MraidScreenSizeProperty(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }

    public static MraidScreenSizeProperty createWithSize(int width, int height) {
        return new MraidScreenSizeProperty(width, height);
    }

    @Override
    public String toJsonPair() {
        return "screenSize: { width: " + mScreenWidth + ", height: " + mScreenHeight + " }";
    }
}

class MraidStateProperty extends MraidProperty {
    private final ViewState mViewState;

    MraidStateProperty(ViewState viewState) {
        mViewState = viewState;
    }

    public static MraidStateProperty createWithViewState(ViewState viewState) {
        return new MraidStateProperty(viewState);
    }

    @Override
    public String toJsonPair() {
        return "state: '" + mViewState.toString().toLowerCase() + "'";
    }
}

class MraidViewableProperty extends MraidProperty {
    private final boolean mViewable;

    MraidViewableProperty(boolean viewable) {
        mViewable = viewable;
    }

    public static MraidViewableProperty createWithViewable(boolean viewable) {
        return new MraidViewableProperty(viewable);
    }

    @Override
    public String toJsonPair() {
        return "viewable: " + (mViewable ? "true" : "false");
    }
}

class MraidSupportsProperty extends MraidProperty{
    private boolean sms;
    private boolean tel;
    private boolean calendar;
    private boolean storePicture;
    private boolean inlineVideo;

    @Override
    public String toJsonPair() {
        return "supports: {" +
                "sms: " + String.valueOf(sms) + ", " +
                "tel: " + String.valueOf(tel) + ", " +
                "calendar: " + String.valueOf(calendar) + ", " +
                "storePicture: " + String.valueOf(storePicture) + ", " +
                "inlineVideo: " + String.valueOf(inlineVideo) + "}";
    }

    public MraidSupportsProperty withSms(boolean value) {
        sms = value;
        return this;
    }


    public MraidSupportsProperty withTel(boolean value) {
        tel = value;
        return this;
    }

    public MraidSupportsProperty withCalendar(boolean value) {
        calendar = value;
        return this;
    }

    public MraidSupportsProperty withStorePicture(boolean value) {
        storePicture = value;
        return this;
    }

    public MraidSupportsProperty withInlineVideo(boolean value) {
        inlineVideo = value;
        return this;
    }
}
