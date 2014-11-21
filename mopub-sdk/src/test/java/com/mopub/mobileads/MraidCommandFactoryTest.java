package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class MraidCommandFactoryTest {
    private MraidCommandFactory subject;
    private Map params;
    private MraidView mraidView;

    @Before
    public void setUp() throws Exception {
        subject = new MraidCommandFactory();
        params = mock(Map.class);
        mraidView = mock(MraidView.class);

    }

    @Test
    public void create_withCommandName_shouldInstantiateCorrectSubclass() throws Exception {
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("close", MraidCommandClose.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("expand", MraidCommandExpand.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("usecustomclose", MraidCommandUseCustomClose.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("open", MraidCommandOpen.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("resize", MraidCommandResize.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("getResizeProperties", MraidCommandGetResizeProperties.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("setResizeProperties", MraidCommandSetResizeProperties.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("playVideo", MraidCommandPlayVideo.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("storePicture", MraidCommandStorePicture.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("getCurrentPosition", MraidCommandGetCurrentPosition.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("getDefaultPosition", MraidCommandGetDefaultPosition.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("getMaxSize", MraidCommandGetMaxSize.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("getScreenSize", MraidCommandGetScreenSize.class);
        assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass("createCalendarEvent", MraidCommandCreateCalendarEvent.class);
    }

    @Test
    public void create_withInvalidCommandString_shouldReturnNull() throws Exception {
        MraidCommand command = MraidCommandFactory.create("dog", params, mraidView);

        assertThat(command).isNull();
    }

    @Test
    public void create_withNullCommandString_shouldReturnNull() throws Exception {
        MraidCommand command = MraidCommandFactory.create(null, params, mraidView);

        assertThat(command).isNull();
    }

    @Test
    public void create_withNullParams_shouldNotBlowUp() throws Exception {
        MraidCommand command = MraidCommandFactory.create("close", null, mraidView);

        // pass
    }

    @Test
    public void create_withNullMraidView_shouldNotBlowUp() throws Exception {
        MraidCommand command = MraidCommandFactory.create("close", params, null);

        // pass
    }

    private void assertMraidCommandFactoryCreatesCorrectMraidCommandSubclass(String command, Class type) {
        MraidCommand mraidCommand = MraidCommandFactory.create(command, params, mraidView);

        assertThat(mraidCommand).isNotNull();
        assertThat(mraidCommand).isInstanceOf(type);
    }
}
