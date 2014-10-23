package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestMraidViewFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_RESIZE_PROPERTIES;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.RESIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.SET_RESIZE_PROPERTIES;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MraidCommandResizeTest {
    private MraidCommandResize subjectResize;
    private MraidCommandGetResizeProperties subjectGetResizeProperties;
    private MraidCommandSetResizeProperties subjectSetResizeProperties;
    private MraidView mraidView;
    @Before
    public void setup() {
        mraidView = TestMraidViewFactory.getSingletonMock();
        subjectResize = new MraidCommandResize(new HashMap<String, String>(), mraidView);
        subjectGetResizeProperties = new MraidCommandGetResizeProperties(new HashMap<String, String>(), mraidView);
        subjectSetResizeProperties = new MraidCommandSetResizeProperties(new HashMap<String, String>(), mraidView);
    }

    @Test
    public void mraidCommandResizeExecute_shouldFireErrorEvent() throws Exception {
        reset(mraidView);
        subjectResize.execute();
        verify(mraidView).fireErrorEvent(eq(RESIZE), any(String.class));
    }

    @Test
    public void mraidCommandSetResizePropertiesExecute_shouldFireErrorEvent() throws Exception {
        reset(mraidView);
        subjectSetResizeProperties.execute();
        verify(mraidView).fireErrorEvent(eq(SET_RESIZE_PROPERTIES), any(String.class));
    }

    @Test
    public void mraidCommandGetResizePropertiesExecute_shouldFireErrorEvent() throws Exception {
        reset(mraidView);
        subjectGetResizeProperties.execute();
        verify(mraidView).fireErrorEvent(eq(GET_RESIZE_PROPERTIES), any(String.class));
    }
}
