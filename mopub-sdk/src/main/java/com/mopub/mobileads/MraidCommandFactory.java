package com.mopub.mobileads;

import java.util.Map;

class MraidCommandFactory {
    protected static MraidCommandFactory instance = new MraidCommandFactory();

    enum MraidJavascriptCommand {
        CLOSE("close"),
        EXPAND("expand"),
        USECUSTOMCLOSE("usecustomclose"),
        OPEN("open"),
        RESIZE("resize"),
        GET_RESIZE_PROPERTIES("getResizeProperties"),
        SET_RESIZE_PROPERTIES("setResizeProperties"),
        PLAY_VIDEO("playVideo"),
        STORE_PICTURE("storePicture"),
        GET_CURRENT_POSITION("getCurrentPosition"),
        GET_DEFAULT_POSITION("getDefaultPosition"),
        GET_MAX_SIZE("getMaxSize"),
        GET_SCREEN_SIZE("getScreenSize"),
        CREATE_CALENDAR_EVENT("createCalendarEvent"),
        UNSPECIFIED("");

        private String mCommand;

        private MraidJavascriptCommand(String command) {
            mCommand = command;
        }

        private static MraidJavascriptCommand fromString(String string) {
            for (MraidJavascriptCommand command : MraidJavascriptCommand.values()) {
                if (command.mCommand.equals(string)) {
                    return command;
                }
            }

            return UNSPECIFIED;
        }

        String getCommand() {
            return mCommand;
        }
    }

    @Deprecated // for testing
    public static void setInstance(MraidCommandFactory factory) {
        instance = factory;
    }

    public static MraidCommand create(String command, Map<String, String> params, MraidView view) {
        return instance.internalCreate(command, params, view);
    }

    protected MraidCommand internalCreate(String command, Map<String, String> params, MraidView view) {
        MraidJavascriptCommand mraidJavascriptCommand = MraidJavascriptCommand.fromString(command);

        switch (mraidJavascriptCommand) {
            case CLOSE:
                return new MraidCommandClose(params, view);
            case EXPAND:
                return new MraidCommandExpand(params, view);
            case USECUSTOMCLOSE:
                return new MraidCommandUseCustomClose(params, view);
            case OPEN:
                return new MraidCommandOpen(params, view);
            case RESIZE:
                return new MraidCommandResize(params, view);
            case GET_RESIZE_PROPERTIES:
                return new MraidCommandGetResizeProperties(params, view);
            case SET_RESIZE_PROPERTIES:
                return new MraidCommandSetResizeProperties(params, view);
            case PLAY_VIDEO:
                return new MraidCommandPlayVideo(params, view);
            case STORE_PICTURE:
                return new MraidCommandStorePicture(params, view);
            case GET_CURRENT_POSITION:
                return new MraidCommandGetCurrentPosition(params, view);
            case GET_DEFAULT_POSITION:
                return new MraidCommandGetDefaultPosition(params, view);
            case GET_MAX_SIZE:
                return new MraidCommandGetMaxSize(params, view);
            case GET_SCREEN_SIZE:
                return new MraidCommandGetScreenSize(params, view);
            case CREATE_CALENDAR_EVENT:
                return new MraidCommandCreateCalendarEvent(params, view);
            case UNSPECIFIED:
                return null;
            default:
                return null;
        }
    }
}
