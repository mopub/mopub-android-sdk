// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ConsentDialogActivityTest {
    private static final String HTML = "some_html";

    private Context mContext;

    private ActivityController<ConsentDialogActivity> activityController;
    private ConsentDialogActivity subject;

    @Before
    public void setUp() throws Exception {
        mContext = Robolectric.buildActivity(Activity.class).create().get();
        activityController = Robolectric.buildActivity(ConsentDialogActivity.class);
    }

    @Test
    public void start_withValidParameters_shouldStartActivity() {
        Context context = spy(mContext);
        ConsentDialogActivity.start(context, HTML);
        verify(context).startActivity(any(Intent.class));
    }

    @Test
    public void createIntent_correctParameters_shouldCreateValidIntent() {
        Intent intent = ConsentDialogActivity.createIntent(mContext, HTML);
        assertThat(intent.getStringExtra("html-page-content")).isEqualTo(HTML);
        ComponentName componentName = intent.getComponent();
        assertNotNull(componentName);
        assertThat(componentName.getClassName()).isEqualTo(ConsentDialogActivity.class.getCanonicalName());
    }

    @Test
    public void onCreate_shouldSetContentView() {
        Intent intent = ConsentDialogActivity.createIntent(mContext, HTML);
        subject = activityController.get();
        subject.setIntent(intent);
        subject.onCreate(null);

        ConsentDialogLayout mView = (ConsentDialogLayout) getContentView();
        assertThat(mView).isNotNull();
    }

    @Test
    public void setCloseButtonVisible_shouldCallViewAndClearHandler() {
        subject = activityController.create().get();

        Handler handler = mock(Handler.class);
        ConsentDialogLayout dialogLayout = mock(ConsentDialogLayout.class);

        subject.mCloseButtonHandler = handler;
        subject.mView = dialogLayout;

        subject.setCloseButtonVisibility(true);

        verify(handler).removeCallbacks(any(Runnable.class));
        verify(dialogLayout).setCloseVisible(true);
    }

    @Test
    public void setCloseButtonInvisible_shouldCallViewAndClearHandler() {
        subject = activityController.create().get();

        Handler handler = mock(Handler.class);
        ConsentDialogLayout dialogLayout = mock(ConsentDialogLayout.class);

        subject.mCloseButtonHandler = handler;
        subject.mView = dialogLayout;

        subject.setCloseButtonVisibility(false);

        verify(handler).removeCallbacks(any(Runnable.class));
        verify(dialogLayout).setCloseVisible(false);
    }

    private FrameLayout getContentView() {
        return (FrameLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }

}
