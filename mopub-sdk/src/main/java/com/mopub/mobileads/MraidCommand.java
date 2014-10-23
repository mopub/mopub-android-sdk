package com.mopub.mobileads;

import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_RESIZE_PROPERTIES;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.OPEN;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.PLAY_VIDEO;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.RESIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.SET_RESIZE_PROPERTIES;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.STORE_PICTURE;
import static com.mopub.mobileads.MraidView.PlacementType;

abstract class MraidCommand {
    protected static final String URI_KEY = "uri";
    protected Map<String, String> mParams;
    protected MraidView mView;

    MraidCommand(Map<String, String> params, MraidView view) {
        mParams = params;
        mView = view;
    }
    
    abstract void execute();
    
    protected int getIntFromParamsForKey(String key) {
        String s = mParams.get(key);
        if (s == null) return -1;
        else {
            try {
                return Integer.parseInt(s, 10);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }
    
    protected String getStringFromParamsForKey(String key) {
        return mParams.get(key);
    }
    
    protected float getFloatFromParamsForKey(String key) {
        String s = mParams.get(key);
        if (s == null) return 0.0f;
        else {
            try {
                return Float.parseFloat(key);
            } catch (NumberFormatException e) {
                return 0.0f;
            }
        }
    }
    
    protected boolean getBooleanFromParamsForKey(String key) {
        return "true".equals(mParams.get(key));
    }

    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        return false;
    }
}

class MraidCommandPlayVideo extends MraidCommand {
    public MraidCommandPlayVideo(Map<String,String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        String url = getStringFromParamsForKey(URI_KEY);
        if (url != null && !url.equals("")){
            mView.getDisplayController().showVideo(url);
        } else {
            mView.fireErrorEvent(PLAY_VIDEO, "Video can't be played with null or empty URL");
        }
    }

    @Override
    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        switch (placementType) {
            case INLINE:
                return true;
            case INTERSTITIAL:
                return false;
            default:
                return super.isCommandDependentOnUserClick(placementType);
        }
    }
}

class MraidCommandStorePicture extends MraidCommand {
    public static final String MIME_TYPE_HEADER = "Content-Type";

    public MraidCommandStorePicture(Map<String,String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        String url = getStringFromParamsForKey(URI_KEY);

        if (url != null && !url.equals("")) {
            mView.getDisplayController().showUserDownloadImageAlert(url);
        } else {
            mView.fireErrorEvent(STORE_PICTURE, "Image can't be stored with null or empty URL");
            MoPubLog.d("Invalid URI for Mraid Store Picture.");
        }
    }

    @Override
    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        return true;
    }
}

class MraidCommandClose extends MraidCommand {
    MraidCommandClose(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().close();
    }
}

class MraidCommandExpand extends MraidCommand {
    MraidCommandExpand(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        int width = getIntFromParamsForKey("w");
        int height = getIntFromParamsForKey("h");
        String url = getStringFromParamsForKey("url");
        boolean shouldUseCustomClose = getBooleanFromParamsForKey("shouldUseCustomClose");
        boolean shouldLockOrientation = getBooleanFromParamsForKey("lockOrientation");
        
        if (width <= 0) width = mView.getDisplayController().mScreenWidth;
        if (height <= 0) height = mView.getDisplayController().mScreenHeight;
        
        mView.getDisplayController().expand(url, width, height, shouldUseCustomClose,
                shouldLockOrientation);
    }

    @Override
    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        switch (placementType) {
            case INLINE:
                return true;
            case INTERSTITIAL:
                return false;
            default:
                return super.isCommandDependentOnUserClick(placementType);
        }
    }
}

class MraidCommandUseCustomClose extends MraidCommand {
    MraidCommandUseCustomClose(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        boolean shouldUseCustomClose = getBooleanFromParamsForKey("shouldUseCustomClose");
        mView.getDisplayController().useCustomClose(shouldUseCustomClose);
    }
}

class MraidCommandOpen extends MraidCommand {
    MraidCommandOpen(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        String url = getStringFromParamsForKey("url");
        if (url == null) {
            mView.fireErrorEvent(OPEN, "Url can not be null.");
            return;
        }
        mView.getBrowserController().open(url);
    }

    @Override
    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        return true;
    }
}

//As of version 1.15, we've decided to stub the resize command. However, this should be implemented in future versions
class MraidCommandResize extends MraidCommand {
    MraidCommandResize(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.fireErrorEvent(RESIZE, "Unsupported action resize.");
    }
}

class MraidCommandGetResizeProperties extends MraidCommand {
    MraidCommandGetResizeProperties(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.fireErrorEvent(GET_RESIZE_PROPERTIES, "Unsupported action getResizeProperties.");
    }
}

class MraidCommandSetResizeProperties extends MraidCommand {
    MraidCommandSetResizeProperties(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.fireErrorEvent(SET_RESIZE_PROPERTIES, "Unsupported action setResizeProperties.");
    }
}

class MraidCommandGetCurrentPosition extends MraidCommand {
    MraidCommandGetCurrentPosition(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().getCurrentPosition();
    }
}


class MraidCommandGetDefaultPosition extends MraidCommand {
    MraidCommandGetDefaultPosition(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().getDefaultPosition();
    }
}

class MraidCommandGetMaxSize extends MraidCommand {
    MraidCommandGetMaxSize(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().getMaxSize();
    }
}

class MraidCommandGetScreenSize extends MraidCommand {
    MraidCommandGetScreenSize(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().getScreenSize();
    }
}

class MraidCommandCreateCalendarEvent extends MraidCommand {
    MraidCommandCreateCalendarEvent(Map<String, String> params, MraidView view) {
        super(params, view);
    }

    @Override
    void execute() {
        mView.getDisplayController().createCalendarEvent(mParams);
    }

    @Override
    protected boolean isCommandDependentOnUserClick(PlacementType placementType) {
        return true;
    }
}

