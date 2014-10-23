package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.FileUtils;
import com.mopub.mobileads.test.support.TestMraidViewFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.MraidCommand.URI_KEY;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.STORE_PICTURE;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MraidCommandStorePictureTest {

    public static final String EXPECTED_FILE = "file://tmp/expectedFile.jpg";
    private MraidCommandStorePicture subject;
    private MraidView mraidView;
    private Map<String,String> params;
    private MraidDisplayController mraidDisplayController;

    @Before
    public void setUp() {
        FileUtils.copyFile("etc/expectedFile.jpg", "/tmp/expectedFile.jpg");

        mraidView = TestMraidViewFactory.getSingletonMock();
        mraidDisplayController = mock(MraidDisplayController.class);
        stub(mraidView.getDisplayController()).toReturn(mraidDisplayController);

        params = new HashMap<String, String>();
        subject = new MraidCommandStorePicture(params, mraidView);
    }

    @Test
    public void execute_withImageUri_shouldShowUserDownloadImageAlert() throws Exception {
        params.put(URI_KEY, EXPECTED_FILE);

        subject.execute();

        verify(mraidDisplayController).showUserDownloadImageAlert(eq(EXPECTED_FILE));
    }

    @Test
    public void execute_withEmptyUriKey_shouldFireErrorEvent() throws Exception {
        params.put(URI_KEY, "");

        subject.execute();

        verify(mraidView).fireErrorEvent(
                eq(STORE_PICTURE),
                anyString());
        verify(mraidDisplayController, never()).showUserDownloadImageAlert(anyString());
    }

    @Test
     public void execute_withMissingUriKey_shouldFireErrorEvent() throws Exception {
        // don't add URI_KEY to params

        subject.execute();

        verify(mraidView).fireErrorEvent(
                eq(STORE_PICTURE),
                anyString());
        verify(mraidDisplayController, never()).showUserDownloadImageAlert(anyString());
    }
}
