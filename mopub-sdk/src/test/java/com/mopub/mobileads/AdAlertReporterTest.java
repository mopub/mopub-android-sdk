// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.mopub.common.AdReport;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestDateAndTime;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class AdAlertReporterTest {
    private final static String EMAIL_ADDRESS = "creative-review@mopub.com";
    private AdAlertReporter subject;
    @Mock
    private AdReport mockAdReport;
    @Mock
    private Context mockContext;
    @Mock
    private View mockView;
    private Intent emailIntent;
    private Bitmap bitmap;
    private ArrayList<Uri> emailAttachments;
    private Date now;

    @Before
    public void setup() {
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        when(mockView.getRootView()).thenReturn(mockView);
        when(mockView.getDrawingCache()).thenReturn(bitmap);

        now = new Date();
        TestDateAndTime.getInstance().setNow(now);
    }

    @Test
    public void constructor_shouldCreateSendToIntentWithEmailAddress() throws Exception {
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);
        emailIntent = subject.getEmailIntent();

        assertThat(emailIntent.getAction()).isEqualTo(Intent.ACTION_SENDTO);
        assertThat(emailIntent.getData()).isEqualTo(Uri.parse("mailto:creative-review@mopub.com"));
    }

    @Test
    public void constructor_shouldCreateIntentWithDatestampInSubject() throws Exception {
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);
        emailIntent = subject.getEmailIntent();

        String emailSubject = emailIntent.getStringExtra(Intent.EXTRA_SUBJECT);
        String subjectParts[] = emailSubject.split(" - ");

        String title = subjectParts[0];
        assertThat(title).isEqualTo("New creative violation report");

        String dateTimeString = subjectParts[1];
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy hh:mm:ss a z", Locale.US);

        Date date = dateFormat.parse(dateTimeString);

        assertThat(date.getTime() - now.getTime()).isLessThan(10000);
    }

    @Test
    public void constructor_shouldCreateIntentWithImageStringAndParametersAndResponseInBody() throws Exception {
        TextView textView = mock(TextView.class);
        Bitmap sampleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        when(textView.getDrawingCache()).thenReturn(sampleBitmap);
        when(mockView.getRootView()).thenReturn(textView);

        when(mockAdReport.toString()).thenReturn("Ad Report data - this is a long list of newlined params.");
        when(mockAdReport.getResponseString()).thenReturn("Test ad string.");
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        emailIntent = subject.getEmailIntent();
        String emailSubject = emailIntent.getStringExtra(Intent.EXTRA_TEXT);
        String bodyParts[] = emailSubject.split("\n=================\n");
        String message = bodyParts[0];
        String parameters = bodyParts[1];
        String response = bodyParts[2];
        String imageString = bodyParts[3];

        assertThat(bodyParts.length).isEqualTo(4);
        //this string is the JPEG encoded version
        assertThat(message).isEqualTo(AdAlertReporter.MESSAGE);
        assertThat(parameters).isEqualTo(subject.getParameters());
        assertThat(response).isEqualTo(subject.getResponse());
        assertThat(imageString).isEqualTo("/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDACAWGBwYFCAcGhwkIiAmMFA0MCwsMGJGSjpQdGZ6eHJm\n" +
                "cG6AkLicgIiuim5woNqirr7EztDOfJri8uDI8LjKzsb/2wBDASIkJDAqMF40NF7GhHCExsbGxsbG\n" +
                "xsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsbGxsb/wAARCAAKAAoDASIA\n" +
                "AhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQA\n" +
                "AAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3\n" +
                "ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWm\n" +
                "p6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEA\n" +
                "AwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSEx\n" +
                "BhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElK\n" +
                "U1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3\n" +
                "uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDn6KKK\n" +
                "AP/Z\n");
    }

    @Test
    public void constructor_whenAdReportIsNull_shouldReturnEmptyString() throws Exception {
        subject = new AdAlertReporter(mockContext, mockView, null);

        assertThat(subject.getParameters()).isEmpty();
        assertThat(subject.getResponse()).isEmpty();
    }

    @Test
    public void constructor_shouldSetCorrectResponseString() throws Exception {
        String expectedResponse = "response";

        when(mockAdReport.getResponseString()).thenReturn(expectedResponse);
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        assertThat(subject.getResponse()).isEqualTo(expectedResponse);
    }

    @Test
    public void send_shouldCreateEmailChooserIntent() throws Exception {
        final Context applicationContext = RuntimeEnvironment.application;
        // A real device uses application context here, which causes Intents.startActivity to add
        // FLAG_ACTIVITY_NEW_TASK (and thus we assert for it below)
        subject = new AdAlertReporter(applicationContext, mockView, mockAdReport);
        subject.send();

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_SENDTO);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
    }

    @Ignore("pending")
    @Test
    public void getScreenshot_whenIsDrawingCacheEnabled_shouldKeepDrawingCacheEnabled() throws Exception {
        reset(mockView);
        when(mockView.getRootView()).thenReturn(mockView);
        when(mockView.isDrawingCacheEnabled()).thenReturn(true);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        verify(mockView, never()).setDrawingCacheEnabled(false);
    }

    @Ignore("pending")
    @Test
    public void getScreenshot_whenIsDrawingCacheDisabled_shouldKeepDrawingCacheDisabled() throws Exception {
        reset(mockView);
        when(mockView.getRootView()).thenReturn(mockView);
        when(mockView.isDrawingCacheEnabled()).thenReturn(false);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        verify(mockView).setDrawingCacheEnabled(false);
    }

    @Test
    public void getScreenshot_whenViewIsNull_shouldPass() throws Exception {
        subject = new AdAlertReporter(mockContext, null, mockAdReport);

        // pass
    }

    @Test
    public void getScreenshot_whenRootViewIsNull_shouldPass() throws Exception {
        when(mockView.getRootView()).thenReturn(null);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        // pass
    }

    @Test
    public void getScreenshot_whenRootViewDrawingCacheIsNull_shouldPass() throws Exception {
        when(mockView.getDrawingCache()).thenReturn(null);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        // pass
    }
}
