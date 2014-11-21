package com.mopub.mobileads;

import org.junit.Test;

import java.util.Map;

import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.CLOSE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.CREATE_CALENDAR_EVENT;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.EXPAND;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_CURRENT_POSITION;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_DEFAULT_POSITION;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_MAX_SIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_RESIZE_PROPERTIES;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_SCREEN_SIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.OPEN;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.PLAY_VIDEO;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.RESIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.SET_RESIZE_PROPERTIES;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.STORE_PICTURE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.USECUSTOMCLOSE;
import static com.mopub.mobileads.MraidCommandFactory.create;
import static com.mopub.mobileads.MraidView.PlacementType;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MraidCommandTest {
    @Test
    public void createCommand_shouldReturnTheRightKindOfCommand() throws Exception {
        assertThat(create("bogus", null, null)).isNull();

        assertThat(create(CLOSE.getCommand(), null, null)).isInstanceOf(MraidCommandClose.class);
        assertThat(create(CLOSE.getCommand(), null, null)).isNotSameAs(create("close", null, null));

        assertThat(create(EXPAND.getCommand(), null, null)).isInstanceOf(MraidCommandExpand.class);
        assertThat(create(USECUSTOMCLOSE.getCommand(), null, null)).isInstanceOf(MraidCommandUseCustomClose.class);
        assertThat(create(OPEN.getCommand(), null, null)).isInstanceOf(MraidCommandOpen.class);
        assertThat(create(RESIZE.getCommand(), null, null)).isInstanceOf(MraidCommandResize.class);
        assertThat(create(SET_RESIZE_PROPERTIES.getCommand(), null, null)).isInstanceOf(MraidCommandSetResizeProperties.class);
        assertThat(create(GET_RESIZE_PROPERTIES.getCommand(), null, null)).isInstanceOf(MraidCommandGetResizeProperties.class);
        assertThat(create(PLAY_VIDEO.getCommand(), null, null)).isInstanceOf(MraidCommandPlayVideo.class);
        assertThat(create(STORE_PICTURE.getCommand(), null, null)).isInstanceOf(MraidCommandStorePicture.class);
        assertThat(create(GET_CURRENT_POSITION.getCommand(), null, null)).isInstanceOf(MraidCommandGetCurrentPosition.class);
        assertThat(create(GET_DEFAULT_POSITION.getCommand(), null, null)).isInstanceOf(MraidCommandGetDefaultPosition.class);
        assertThat(create(GET_MAX_SIZE.getCommand(), null, null)).isInstanceOf(MraidCommandGetMaxSize.class);
        assertThat(create(GET_SCREEN_SIZE.getCommand(), null, null)).isInstanceOf(MraidCommandGetScreenSize.class);
        assertThat(create(CREATE_CALENDAR_EVENT.getCommand(), null, null)).isInstanceOf(MraidCommandCreateCalendarEvent.class);
    }

    @Test
    public void createCommand_shouldPassParameters() throws Exception {
        MraidView expectedView = mock(MraidView.class);
        Map<String, String> expectedMap = mock(Map.class);

        MraidCommand command = create(EXPAND.getCommand(), expectedMap, expectedView);
        assertThat(command.mParams).isEqualTo(expectedMap);
        assertThat(command.mView).isEqualTo(expectedView);
    }


    @Test
    public void createCommand_close_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(CLOSE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(CLOSE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_expand_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(EXPAND.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isTrue();
        assertThat(create(EXPAND.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_useCustomClose_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(USECUSTOMCLOSE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(USECUSTOMCLOSE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_open_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(OPEN.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isTrue();
        assertThat(create(OPEN.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isTrue();
    }

    @Test
    public void createCommand_resize_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(RESIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(RESIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_setResizeProperties_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(SET_RESIZE_PROPERTIES.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(SET_RESIZE_PROPERTIES.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_getResizeProperties_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(GET_RESIZE_PROPERTIES.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(GET_RESIZE_PROPERTIES.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_playVideo_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(PLAY_VIDEO.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isTrue();
        assertThat(create(PLAY_VIDEO.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_storePicture_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(STORE_PICTURE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isTrue();
        assertThat(create(STORE_PICTURE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isTrue();
    }

    @Test
    public void createCommand_getCurrentPosition_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(GET_CURRENT_POSITION.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(GET_CURRENT_POSITION.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_getDefaultPosition_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(GET_DEFAULT_POSITION.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(GET_DEFAULT_POSITION.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_getMaxSize_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(GET_MAX_SIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(GET_MAX_SIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_getScreenSize_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(GET_SCREEN_SIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isFalse();
        assertThat(create(GET_SCREEN_SIZE.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isFalse();
    }

    @Test
    public void createCommand_createCalendarEvent_shouldSetDependentOnUserClick() throws Exception {
        assertThat(create(CREATE_CALENDAR_EVENT.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INLINE)).isTrue();
        assertThat(create(CREATE_CALENDAR_EVENT.getCommand(), null, null).isCommandDependentOnUserClick(PlacementType.INTERSTITIAL)).isTrue();
    }
}
