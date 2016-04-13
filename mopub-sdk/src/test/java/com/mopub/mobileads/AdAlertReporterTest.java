package com.mopub.mobileads;

import android.app.Activity;
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
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
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

        stub(mockView.getRootView()).toReturn(mockView);
        stub(mockView.getDrawingCache()).toReturn(bitmap);

        now = new Date();
        TestDateAndTime.getInstance().setNow(now);
    }

    @Test
    public void constructor_shouldCreateSendToIntentWithEmailAddress() throws Exception {
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);
        emailIntent = subject.getEmailIntent();

        assertThat(emailIntent.getAction()).isEqualTo(Intent.ACTION_SEND_MULTIPLE);
        assertThat(emailIntent.getType()).isEqualTo("plain/text");
        assertThat(emailIntent.getData().toString()).isEqualTo("mailto:");
        assertThat(emailIntent.getStringArrayExtra(Intent.EXTRA_EMAIL)[0]).isEqualTo(EMAIL_ADDRESS);
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
        stub(textView.getDrawingCache()).toReturn(sampleBitmap);
        stub(mockView.getRootView()).toReturn(textView);

        stub(mockAdReport.toString()).toReturn("Ad Report data - this is a long list of newlined params.");
        stub(mockAdReport.getResponseString()).toReturn("Test ad string.");
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        emailIntent = subject.getEmailIntent();
        String emailSubject = emailIntent.getStringExtra(Intent.EXTRA_TEXT);
        String bodyParts[] = emailSubject.split("\n=================\n");
        String parameters = bodyParts[0];
        String response = bodyParts[1];
        String imageString = bodyParts[2];

        assertThat(bodyParts.length).isEqualTo(3);
        //this string is the JPEG encoded version
        assertThat(parameters).isEqualTo(subject.getParameters());
        assertThat(response).isEqualTo(subject.getResponse());
        assertThat(imageString).isEqualTo("Qml0bWFwICgxMCB4IDEwKSBjcmVhdGVkIGZyb20gQml0bWFwIG9iamVjdCBjb21wcmVzc2VkIGFz\nIEpQRUcgd2l0aCBxdWFsaXR5IDI1\n");
    }

    @Test
    public void constructor_shouldAddBitmapToAttachmentArray() throws Exception {
        stub(mockContext.getFilesDir()).toReturn(new File("filesDir"));
        stub(mockContext.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_screenshot.png"));

        assertThat(emailAttachments).contains(fileUri);
    }

    @Test
    public void constructor_shouldAddParametersTextFileToAttachmentArray() throws Exception {
        stub(mockContext.getFilesDir()).toReturn(new File("filesDir"));
        stub(mockContext.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_parameters.txt"));

        assertThat(emailAttachments).contains(fileUri);
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

        stub(mockAdReport.getResponseString()).toReturn(expectedResponse);
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        assertThat(subject.getResponse()).isEqualTo(expectedResponse);
    }

    @Test
    public void constructor_shouldAddMarkupTextFileToAttachmentArray() throws Exception {
        stub(mockAdReport.getResponseString()).toReturn("anything!");
        stub(mockContext.getFilesDir()).toReturn(new File("filesDir"));
        stub(mockContext.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_markup.html"));

        assertThat(emailAttachments).contains(fileUri);
    }

    @Test
    public void send_shouldAddAttachmentsToIntent() throws Exception {
        stub(mockContext.getFilesDir()).toReturn(new File("filesDir"));
        stub(mockContext.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        stub(mockAdReport.getResponseString()).toReturn("anything!");
        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);
        subject.send();

        emailIntent = subject.getEmailIntent();
        ArrayList<Uri> attachments = emailIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        assertThat(attachments.size()).isEqualTo(3);
        assertThat(attachments).contains(Uri.fromFile(new File("filesDir/mp_adalert_screenshot.png")));
        assertThat(attachments).contains(Uri.fromFile(new File("filesDir/mp_adalert_parameters.txt")));
        assertThat(attachments).contains(Uri.fromFile(new File("filesDir/mp_adalert_markup.html")));
    }

    @Test
    public void send_shouldCreateEmailChooserIntent() throws Exception {

        subject = new AdAlertReporter(Robolectric.buildActivity(Activity.class).create().get(), mockView, mockAdReport);
        subject.send();

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_CHOOSER);
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Send Email...");
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
    }

    @Ignore("pending")
    @Test
    public void getScreenshot_whenIsDrawingCacheEnabled_shouldKeepDrawingCacheEnabled() throws Exception {
        reset(mockView);
        stub(mockView.getRootView()).toReturn(mockView);
        stub(mockView.isDrawingCacheEnabled()).toReturn(true);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        verify(mockView, never()).setDrawingCacheEnabled(false);
    }

    @Ignore("pending")
    @Test
    public void getScreenshot_whenIsDrawingCacheDisabled_shouldKeepDrawingCacheDisabled() throws Exception {
        reset(mockView);
        stub(mockView.getRootView()).toReturn(mockView);
        stub(mockView.isDrawingCacheEnabled()).toReturn(false);

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
        stub(mockView.getRootView()).toReturn(null);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        // pass
    }

    @Test
    public void getScreenshot_whenRootViewDrawingCacheIsNull_shouldPass() throws Exception {
        stub(mockView.getDrawingCache()).toReturn(null);

        subject = new AdAlertReporter(mockContext, mockView, mockAdReport);

        // pass
    }
}
