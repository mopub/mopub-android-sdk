package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestDateAndTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
public class AdAlertReporterTest {
    private final static String EMAIL_ADDRESS = "creative-review@mopub.com";
    private AdAlertReporter subject;
    private Context context;
    private View view;
    private AdConfiguration adConfiguration;
    private Intent emailIntent;
    private Bitmap bitmap;
    private ArrayList<Uri> emailAttachments;
    private Date now;

    @Before
    public void setup() {
        context = mock(Context.class);

        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        view = mock(View.class);
        stub(view.getRootView()).toReturn(view);
        stub(view.getDrawingCache()).toReturn(bitmap);

        adConfiguration = mock(AdConfiguration.class);

        now = new Date();
        TestDateAndTime.getInstance().setNow(now);
    }

    @Test
    public void constructor_shouldCreateSendToIntentWithEmailAddress() throws Exception {
        subject = new AdAlertReporter(context, view, adConfiguration);
        emailIntent = subject.getEmailIntent();

        assertThat(emailIntent.getAction()).isEqualTo(Intent.ACTION_SEND_MULTIPLE);
        assertThat(emailIntent.getType()).isEqualTo("plain/text");
        assertThat(emailIntent.getDataString()).isEqualTo("mailto:");
        assertThat(emailIntent.getStringArrayExtra(Intent.EXTRA_EMAIL)[0]).isEqualTo(EMAIL_ADDRESS);
    }

    @Test
    public void constructor_shouldCreateIntentWithDatestampInSubject() throws Exception {
        subject = new AdAlertReporter(context, view, adConfiguration);
        emailIntent = subject.getEmailIntent();

        String emailSubject = emailIntent.getStringExtra(Intent.EXTRA_SUBJECT);
        String subjectParts[] = emailSubject.split(" - ");

        String title = subjectParts[0];
        assertThat(title).isEqualTo("New creative violation report");

        String dateTimeString = subjectParts[1];
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy hh:mm:ss a z");

        Date date = dateFormat.parse(dateTimeString);

        assertThat(date.getTime() - now.getTime()).isLessThan(10000);
    }

    @Test
    public void constructor_shouldCreateIntentWithImageStringAndParametersAndResponseInBody() throws Exception {
        TextView textView = mock(TextView.class);
        Bitmap sampleBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ALPHA_8);
        stub(textView.getDrawingCache()).toReturn(sampleBitmap);
        stub(view.getRootView()).toReturn(textView);

        stub(adConfiguration.getResponseString()).toReturn("<html>a valid response</html>");
        stub(adConfiguration.getDspCreativeId()).toReturn("");
        stub(adConfiguration.getPlatformVersion()).toReturn(1);
        stub(adConfiguration.getDeviceModel()).toReturn("android");
        stub(adConfiguration.getAdUnitId()).toReturn("abc");
        stub(adConfiguration.getDeviceLocale()).toReturn("US");
        stub(adConfiguration.getHashedUdid()).toReturn("UDID");
        stub(adConfiguration.getNetworkType()).toReturn("unknown");
        stub(adConfiguration.getPlatform()).toReturn("android");
        stub(adConfiguration.getTimeStamp()).toReturn(now.getTime());
        stub(adConfiguration.getAdType()).toReturn("interstitial");
        stub(adConfiguration.getWidth()).toReturn(480);
        stub(adConfiguration.getHeight()).toReturn(320);

        subject = new AdAlertReporter(context, view, adConfiguration);

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
        stub(context.getFilesDir()).toReturn(new File("filesDir"));
        stub(context.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(context, view, adConfiguration);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_screenshot.png"));

        assertThat(emailAttachments).contains(fileUri);
    }

    @Test
    public void constructor_shouldAddParametersTextFileToAttachmentArray() throws Exception {
        stub(context.getFilesDir()).toReturn(new File("filesDir"));
        stub(context.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(context, view, adConfiguration);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_parameters.txt"));

        assertThat(emailAttachments).contains(fileUri);
    }

    @Test
    public void constructor_shouldProperlyConstructParametersTextFile() throws Exception {
        String expectedParameters =
                "sdk_version : 1.15.2.2\n" +
                "creative_id : \n" +
                "platform_version : 1\n" +
                "device_model : android\n" +
                "ad_unit_id : abc\n" +
                "device_locale : US\n" +
                "device_id : UDID\n" +
                "network_type : unknown\n" +
                "platform : android\n" +
                "timestamp : " + getCurrentDateTime() + "\n" +
                "ad_type : interstitial\n" +
                "ad_size : {480, 320}\n";

        stub(adConfiguration.getSdkVersion()).toReturn("1.15.2.2");
        stub(adConfiguration.getDspCreativeId()).toReturn("");
        stub(adConfiguration.getPlatformVersion()).toReturn(1);
        stub(adConfiguration.getDeviceModel()).toReturn("android");
        stub(adConfiguration.getAdUnitId()).toReturn("abc");
        stub(adConfiguration.getDeviceLocale()).toReturn("US");
        stub(adConfiguration.getHashedUdid()).toReturn("UDID");
        stub(adConfiguration.getNetworkType()).toReturn("unknown");
        stub(adConfiguration.getPlatform()).toReturn("android");
        stub(adConfiguration.getTimeStamp()).toReturn(now.getTime());
        stub(adConfiguration.getAdType()).toReturn("interstitial");
        stub(adConfiguration.getWidth()).toReturn(480);
        stub(adConfiguration.getHeight()).toReturn(320);

        subject = new AdAlertReporter(context, view, adConfiguration);

        assertThat(subject.getParameters()).isEqualTo(expectedParameters);
    }

    @Test
    public void constructor_withInvalidAdConfigurationValues_shouldReturnSomethingSensible() throws Exception {
        String expectedParameters =
                "sdk_version : null\n" +
                "creative_id : null\n" +
                "platform_version : -1\n" +
                "device_model : null\n" +
                "ad_unit_id : null\n" +
                "device_locale : null\n" +
                "device_id : null\n" +
                "network_type : null\n" +
                "platform : null\n" +
                "timestamp : null" + "\n" +
                "ad_type : null\n" +
                "ad_size : {-1, -1}\n";

        stub(adConfiguration.getSdkVersion()).toReturn(null);
        stub(adConfiguration.getDspCreativeId()).toReturn(null);
        stub(adConfiguration.getPlatformVersion()).toReturn(-1);
        stub(adConfiguration.getDeviceModel()).toReturn(null);
        stub(adConfiguration.getAdUnitId()).toReturn(null);
        stub(adConfiguration.getDeviceLocale()).toReturn(null);
        stub(adConfiguration.getHashedUdid()).toReturn(null);
        stub(adConfiguration.getNetworkType()).toReturn(null);
        stub(adConfiguration.getPlatform()).toReturn(null);
        stub(adConfiguration.getTimeStamp()).toReturn(-1l);
        stub(adConfiguration.getAdType()).toReturn(null);
        stub(adConfiguration.getWidth()).toReturn(-1);
        stub(adConfiguration.getHeight()).toReturn(-1);

        subject = new AdAlertReporter(context, view, adConfiguration);

        assertThat(subject.getParameters()).isEqualTo(expectedParameters);
    }

    @Test
    public void constructor_whenAdConfigurationIsNull_shouldReturnEmptyString() throws Exception {
        subject = new AdAlertReporter(context, view, null);

        assertThat(subject.getParameters()).isEmpty();
        assertThat(subject.getResponse()).isEmpty();
    }

    @Test
    public void constructor_shouldReturnCorrectResponseString() throws Exception {
        String expectedResponse = "response";

        stub(adConfiguration.getResponseString()).toReturn(expectedResponse);
        subject = new AdAlertReporter(context, view, adConfiguration);

        assertThat(subject.getResponse()).isEqualTo(expectedResponse);
    }

    @Test
    public void constructor_shouldAddMarkupTextFileToAttachmentArray() throws Exception {
        stub(adConfiguration.getResponseString()).toReturn(" ");

        stub(context.getFilesDir()).toReturn(new File("filesDir"));
        stub(context.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));
        subject = new AdAlertReporter(context, view, adConfiguration);

        emailAttachments = subject.getEmailAttachments();
        Uri fileUri = Uri.fromFile(new File("filesDir/mp_adalert_markup.html"));

        assertThat(emailAttachments).contains(fileUri);
    }

    @Test
    public void send_shouldAddAttachmentsToIntent() throws Exception {
        stub(adConfiguration.getResponseString()).toReturn("response!");
        stub(context.getFilesDir()).toReturn(new File("filesDir"));
        stub(context.openFileOutput(any(String.class), any(int.class))).toReturn(mock(FileOutputStream.class));

        subject = new AdAlertReporter(context, view, adConfiguration);
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
        stub(adConfiguration.getResponseString()).toReturn("response!");

        subject = new AdAlertReporter(new Activity(), view, adConfiguration);
        subject.send();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_CHOOSER);
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Send Email...");
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
    }

    @Test
    public void getScreenshot_whenIsDrawingCacheEnabled_shouldKeepDrawingCacheEnabled() throws Exception {
//        reset(view);
//        stub(view.getRootView()).toReturn(view);
//        stub(view.isDrawingCacheEnabled()).toReturn(true);
//
//        subject = new AdAlertReporter(context, view, adConfiguration);
//
//        verify(view, never()).setDrawingCacheEnabled(false);
    }

    @Test
    public void getScreenshot_whenIsDrawingCacheDisabled_shouldKeepDrawingCacheDisabled() throws Exception {
//        reset(view);
//        stub(view.getRootView()).toReturn(view);
//        stub(view.isDrawingCacheEnabled()).toReturn(false);
//
//        subject = new AdAlertReporter(context, view, adConfiguration);
//
//        verify(view).setDrawingCacheEnabled(false);
    }

    @Test
    public void getScreenshot_whenViewIsNull_shouldPass() throws Exception {
        subject = new AdAlertReporter(context, null, adConfiguration);

        // pass
    }

    @Test
    public void getScreenshot_whenRootViewIsNull_shouldPass() throws Exception {
        stub(view.getRootView()).toReturn(null);

        subject = new AdAlertReporter(context, view, adConfiguration);

        // pass
    }

    @Test
    public void getScreenshot_whenRootViewDrawingCacheIsNull_shouldPass() throws Exception {
        stub(view.getDrawingCache()).toReturn(null);

        subject = new AdAlertReporter(context, view, adConfiguration);

        // pass
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy hh:mm:ss a z");
        return dateFormat.format(now);
    }
}

