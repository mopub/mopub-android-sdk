package com.mopub.mraid;

import android.support.annotation.NonNull;

public enum MraidJavascriptCommand {
    CLOSE("close"),
    EXPAND("expand") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return placementType == PlacementType.INLINE;
        }
    },
    USE_CUSTOM_CLOSE("usecustomclose"),
    OPEN("open") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return true;
        }
    },
    RESIZE("resize") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return true;
        }
    },
    SET_ORIENTATION_PROPERTIES("setOrientationProperties"),
    PLAY_VIDEO("playVideo") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return placementType == PlacementType.INLINE;
        }
    },
    STORE_PICTURE("storePicture") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return true;
        }
    },
    CREATE_CALENDAR_EVENT("createCalendarEvent") {
        @Override
        boolean requiresClick(@NonNull final PlacementType placementType) {
            return true;
        }
    },
    UNSPECIFIED("");

    @NonNull private final String mJavascriptString;

    MraidJavascriptCommand(@NonNull String javascriptString) {
        mJavascriptString = javascriptString;
    }

    static MraidJavascriptCommand fromJavascriptString(@NonNull String string) {
        for (MraidJavascriptCommand command : MraidJavascriptCommand.values()) {
            if (command.mJavascriptString.equals(string)) {
                return command;
            }
        }

        return UNSPECIFIED;
    }

    String toJavascriptString() {
        return mJavascriptString;
    }

    boolean requiresClick(@NonNull PlacementType placementType) {
        return false;
    }
}
