package com.mopub.common.privacy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ConsentDialogActivityTest {
    private static final String HTML = "some_html";

    private Context context;

    private ActivityController<ConsentDialogActivity> activityController;
    private ConsentDialogActivity subject;
    private Intent intent;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        intent = ConsentDialogActivity.createIntent(context, HTML);
        activityController = Robolectric.buildActivity(ConsentDialogActivity.class);
    }

    @Test
    public void createIntent_correctParameters_shouldCreateValidIntent() {
        intent = ConsentDialogActivity.createIntent(context, HTML);
        assertThat(intent.getStringExtra("html-page-content")).isEqualTo(HTML);
        assertThat(intent.getComponent()).isNotNull();
        assertThat(intent.getComponent().getClassName()).isEqualTo(ConsentDialogActivity.class.getCanonicalName());
    }

    @Test
    public void onCreate_shouldSetContentView() {
        subject = activityController.get();
        subject.setIntent(intent);
        subject.onCreate(null);

        ConsentDialogLayout mView = (ConsentDialogLayout) getContentView();
        assertThat(mView).isNotNull();
    }

    @Test
    public void setCloseButtonVisible_shouldCallViewAndClearHandler() throws NoSuchFieldException, IllegalAccessException {
        subject = activityController.create().get();

        Handler handler = mock(Handler.class);
        ConsentDialogLayout dialogLayout = mock(ConsentDialogLayout.class);

        Field fieldHandler = Reflection.getPrivateField(ConsentDialogActivity.class, "mCloseButtonHandler");
        fieldHandler.set(subject, handler);

        Field fieldLayout = Reflection.getPrivateField(ConsentDialogActivity.class, "mView");
        fieldLayout.set(subject, dialogLayout);

        subject.setCloseButtonVisibility(true);

        verify(handler, times(1)).removeCallbacks(any(Runnable.class));
        verify(dialogLayout, times(1)).setCloseVisible(true);
    }

    @Test
    public void setCloseButtonInvisible_shouldCallViewAndClearHandler() throws NoSuchFieldException, IllegalAccessException {
        subject = activityController.create().get();

        Handler handler = mock(Handler.class);
        ConsentDialogLayout dialogLayout = mock(ConsentDialogLayout.class);

        Field fieldHandler = Reflection.getPrivateField(ConsentDialogActivity.class, "mCloseButtonHandler");
        fieldHandler.set(subject, handler);

        Field fieldLayout = Reflection.getPrivateField(ConsentDialogActivity.class, "mView");
        fieldLayout.set(subject, dialogLayout);

        subject.setCloseButtonVisibility(false);

        verify(handler, times(1)).removeCallbacks(any(Runnable.class));
        verify(dialogLayout, times(1)).setCloseVisible(false);
    }

    private FrameLayout getContentView() {
        return (FrameLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }
}
