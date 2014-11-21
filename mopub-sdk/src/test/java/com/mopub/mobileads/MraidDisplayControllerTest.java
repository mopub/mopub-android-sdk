package com.mopub.mobileads;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.test.support.FileUtils;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import com.mopub.mobileads.test.support.TestMraidViewFactory;
import com.mopub.mobileads.test.support.ThreadUtils;
import com.mopub.mobileads.util.MraidsTest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import java.io.*;
import java.net.URI;
import java.util.*;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static com.mopub.mobileads.BaseVideoPlayerActivitiyTest.assertMraidVideoPlayerActivityStarted;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.CREATE_CALENDAR_EVENT;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_CURRENT_POSITION;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_DEFAULT_POSITION;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_MAX_SIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.GET_SCREEN_SIZE;
import static com.mopub.mobileads.MraidCommandFactory.MraidJavascriptCommand.STORE_PICTURE;
import static com.mopub.mobileads.MraidCommandStorePicture.MIME_TYPE_HEADER;
import static com.mopub.mobileads.util.Mraids.ANDROID_CALENDAR_CONTENT_TYPE;
import static java.io.File.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class MraidDisplayControllerTest {
    private static final String IMAGE_URI_VALUE = "file://tmp/expectedFile.jpg";
    private static final int TIME_TO_PAUSE_FOR_NETWORK = 300;
    private static final String VIDEO_URL = "http://video";
    public static final String FAKE_IMAGE_DATA = "imageFileData";
    //XXX: Robolectric or JUNIT doesn't support the correct suffix ZZZZZ in the parse pattern, so replacing xx:xx with xxxx for time.
    public static final String CALENDAR_START_TIME = "2013-08-14T20:00:00-0000";

    private MraidView mraidView;
    private MraidDisplayController subject;
    private View rootView;
    private FrameLayout contentView;
    private MoPubView moPubView;
    private FrameLayout adContainerLayout;
    private RelativeLayout expansionLayout;
    private FrameLayout placeholderView;
    private File expectedFile;
    private File pictureDirectory;
    private File fileWithoutExtension;
    private TestHttpResponseWithHeaders response;
    private Map<String, String> params;
    private AdConfiguration adConfiguration;
    private long testBroadcastIdentifier;


    @Before
    public void setup() {
        mraidView = TestMraidViewFactory.getSingletonMock();
        moPubView = mock(MoPubView.class);
        rootView = mock(View.class);
        contentView = mock(FrameLayout.class);
        adContainerLayout = mock(FrameLayout.class);
        expansionLayout = mock(RelativeLayout.class);
        placeholderView = mock(FrameLayout.class);
        params = new HashMap<String, String>();

        resetMockMraidView(new Activity());
        stub(rootView.findViewById(eq(android.R.id.content))).toReturn(contentView);
        stub(contentView.getContext()).toReturn(new Activity());

        subject = new TestMraidDisplayController(mraidView, null, null);

        testBroadcastIdentifier = 1235;
        adConfiguration = mock(AdConfiguration.class, withSettings().serializable());
        stub(adConfiguration.getBroadcastIdentifier()).toReturn(testBroadcastIdentifier);
        stub(mraidView.getAdConfiguration()).toReturn(adConfiguration);

        FileUtils.copyFile("etc/expectedFile.jpg", "/tmp/expectedFile.jpg");
        expectedFile = new File(Environment.getExternalStorageDirectory(), "Pictures" + separator + "expectedFile.jpg");
        pictureDirectory = new File(Environment.getExternalStorageDirectory(), "Pictures");
        fileWithoutExtension = new File(pictureDirectory, "file");

        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        Robolectric.getShadowApplication().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Test
    public void initialization_shouldSetupStartingState() throws Exception {
        assertThat(subject.getMraidView()).isSameAs(mraidView);
    }

    @Test
    public void initializeJavaScriptState_shouldSetMraidSupportsProperties() throws Exception {
        subject.initializeJavaScriptState();
        verify(mraidView).fireChangeEventForProperty(isA(MraidSupportsProperty.class));
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenPhoneIsAvailable_shouldReportPhoneAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData("tel", null, null, "android.intent.action.DIAL");

        resetMockMraidView(mockContext);
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("tel: true");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenPhoneNotAvailable_shouldReportPhoneNotAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.DIAL");

        resetMockMraidView(mockContext);
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("tel: false");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenSmsIsAvailable_shouldReportSmsAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData("sms", null, null, "android.intent.action.VIEW");

        resetMockMraidView(mockContext);
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("sms: true");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenSmsNotAvailable_shouldReportSmsNotAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.VIEW");

        resetMockMraidView(mockContext);
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("sms: false");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenWriteExternalStoragePermissionNotGranted_shouldReportStorePictureNotAvailable() throws Exception {
        Robolectric.getShadowApplication().denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        resetMockMraidView(new Activity());
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("storePicture: false");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenSDCardNotMounted_shouldReportStorePictureNotAvailable() throws Exception {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED);

        resetMockMraidView(new Activity());
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("storePicture: false");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenSDCardMounted_andWriteExternalStoragePermissionGranted_shouldReportStorePictureAvailable() throws Exception {
        resetMockMraidView(new Activity());
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("storePicture: true");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void initializeSupportedFunctionsProperty_atLeastIcs_shouldReportCalendarAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData(null, null, ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");
        resetMockMraidView(mockContext);

        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("calendar: true");
    }

    @Config(reportSdk = VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void initializeSupportedFunctionsProperty_beforeIcs_shouldNotReportCalendarAvailable() throws Exception {
        resetMockMraidView(new Activity());
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("calendar: false");
    }

    @Test
    public void initializeSupportedFunctionsProperty_whenMraidVideoPlayerActivityDeclared_shouldReportInlineVideoAvailable() throws Exception {
        Context mockContext = createMockContextWithSpecificIntentData(null, "com.mopub.mobileads.MraidVideoPlayerActivity", null, null);

        resetMockMraidView(mockContext);
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("inlineVideo: true");
    }

    @Test
    public void initializeSupportedFunctionsProperty_MraidVideoPlayerActivityNotDeclared_shouldReportInlineVideoNotAvailable() throws Exception {
        resetMockMraidView(new Activity());
        subject.initializeSupportedFunctionsProperty();

        MraidSupportsProperty mraidSupportsProperty = captureMraidSupportProperties();

        assertThat(mraidSupportsProperty.toJsonPair()).contains("inlineVideo: false");
    }

    @Test
    public void expand_shouldSwapWithPlaceholderView() throws Exception {
        stub(moPubView.getChildAt(eq(0))).toReturn(mraidView);
        subject.expand(null, 320, 50, false, false);

        verify(moPubView).addView(any(FrameLayout.class), eq(0), any(ViewGroup.LayoutParams.class));
        verify(moPubView).removeView(eq(mraidView));
        verify(adContainerLayout, times(2)).addView(any(ImageView.class), any(FrameLayout.LayoutParams.class));
    }

    @Test
    public void close_shouldUnexpandView() throws Exception {
        subject.expand(null, 320, 50, false, false);
        stub(placeholderView.getParent()).toReturn(moPubView);

        subject.close();

        verify(adContainerLayout).removeAllViewsInLayout();
        verify(expansionLayout).removeAllViewsInLayout();
        verify(contentView).removeView(eq(expansionLayout));
        verify(moPubView).addView(eq(mraidView), any(int.class));
        verify(moPubView).removeView(eq(placeholderView));
        verify(moPubView).invalidate();
    }

    @Test
    public void showUserDownloadImageAlert_withActivityContext_shouldDisplayAlertDialog() throws Exception {
        stub(mraidView.getContext()).toReturn(new Activity());
        subject = new TestMraidDisplayController(mraidView, null, null);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);

        subject.showUserDownloadImageAlert(IMAGE_URI_VALUE);

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        ShadowAlertDialog shadowAlertDialog = shadowOf(alertDialog);

        assertThat(alertDialog.isShowing());

        assertThat(shadowAlertDialog.getTitle()).isEqualTo("Save Image");
        assertThat(shadowAlertDialog.getMessage()).isEqualTo("Download image to Picture gallery?");
        assertThat(shadowAlertDialog.isCancelable()).isTrue();

        assertThat(alertDialog.getButton(BUTTON_POSITIVE).hasOnClickListeners());
        assertThat(alertDialog.getButton(BUTTON_NEGATIVE)).isNotNull();
    }

    @Test
    public void showUserDownloadImageAlert_whenOkayClicked_shouldDownloadImage() throws Exception {
        stub(mraidView.getContext()).toReturn(new Activity());
        subject = new TestMraidDisplayController(mraidView, null, null);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);
        Robolectric.addPendingHttpResponse(response);

        subject.showUserDownloadImageAlert(IMAGE_URI_VALUE);

        ShadowAlertDialog.getLatestAlertDialog().getButton(BUTTON_POSITIVE).performClick();
        ThreadUtils.pause(TIME_TO_PAUSE_FOR_NETWORK);

        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.length()).isEqualTo(FAKE_IMAGE_DATA.length());
    }

    @Test
    public void showUserDownloadImageAlert_whenCancelClicked_shouldDismissDialog() throws Exception {
        stub(mraidView.getContext()).toReturn(new Activity());
        subject = new TestMraidDisplayController(mraidView, null, null);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);

        subject.showUserDownloadImageAlert(IMAGE_URI_VALUE);

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        ShadowAlertDialog shadowAlertDialog = shadowOf(alertDialog);

        alertDialog.getButton(BUTTON_NEGATIVE).performClick();
        assertThat(shadowAlertDialog.hasBeenDismissed()).isTrue();

        assertThat(expectedFile.exists()).isFalse();
        assertThat(expectedFile.length()).isEqualTo(0);
    }

    @Test
    public void showUserDownloadImageAlert_withAppContext_shouldToastAndDownloadImage() throws Exception {
        stub(mraidView.getContext()).toReturn(Robolectric.application);
        subject = new TestMraidDisplayController(mraidView, null, null);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);
        Robolectric.addPendingHttpResponse(response);

        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);

        subject.showUserDownloadImageAlert(IMAGE_URI_VALUE);
        ThreadUtils.pause(TIME_TO_PAUSE_FOR_NETWORK);

        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Downloading image to Picture gallery...");

        Robolectric.runUiThreadTasks();

        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.length()).isEqualTo(FAKE_IMAGE_DATA.length());
    }

    @Test
    public void showUserDownloadImageAlert_withAppContext_whenDownloadImageFails_shouldDisplayFailureToastAndNotDownloadImage() throws Exception {
        stub(mraidView.getContext()).toReturn(Robolectric.application);
        subject = new TestMraidDisplayController(mraidView, null, null);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);
        Robolectric.addPendingHttpResponse(response);

        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);

        subject.showUserDownloadImageAlert("this is an invalid image url and cannot be downloaded");
        ThreadUtils.pause(TIME_TO_PAUSE_FOR_NETWORK);

        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Downloading image to Picture gallery...");

        Robolectric.runUiThreadTasks();

        assertThat(ShadowToast.shownToastCount()).isEqualTo(2);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Image failed to download.");

        assertThat(expectedFile.exists()).isFalse();
        assertThat(expectedFile.length()).isEqualTo(0);
    }

    @Test
    public void showUserDownloadImageAlert_whenStorePictureNotSupported_shouldFireErrorEvent_andNotToastNorAlertDialog() throws Exception {
        Robolectric.getShadowApplication().denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        subject.showUserDownloadImageAlert("http://image.jpg");

        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        assertThat(ShadowAlertDialog.getLatestAlertDialog()).isNull();
        verify(mraidView).fireErrorEvent(eq(STORE_PICTURE), any(String.class));
    }

    @Test
    public void showUserDownloadImageAlert_withMimeTypeAndNoFileExtension_shouldSavePictureWithMimeType() throws Exception {
        String fileNameWithNoExtension = "https://www.somewhere.com/images/blah/file";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithNoExtension,
                "image/jpg",
                "file.jpg",
                ".jpg");
    }

    @Test
    public void showUserDownloadImageAlert_withMultipleContentTypesAndNoFileExtension_shouldSavePictureWithMimeType() throws Exception {
        String fileNameWithNoExtension = "https://www.somewhere.com/images/blah/file";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithNoExtension,
                "text/html; image/png",
                "file.png",
                ".png");
    }

    @Test
    public void showUserDownloadImageAlert_withMimeTypeAndFileExtension_shouldSavePictureWithFileExtension() throws Exception {
        String fileNameWithExtension = "https://www.somewhere.com/images/blah/file.extension";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithExtension,
                "image/extension",
                "file.extension",
                ".extension");

        assertThat((expectedFile.getName()).endsWith(".extension.extension")).isFalse();
    }

    @Test
    public void showUserDownloadImageAlert_withHttpUri_shouldRequestPictureFromNetwork() throws Exception {
        response = new TestHttpResponseWithHeaders(200, "OK");
        downloadImageForPendingResponse("https://www.google.com/images/srpr/logo4w.png", response);

        HttpUriRequest latestRequest = (HttpUriRequest) Robolectric.getLatestSentHttpRequest();
        assertThat(latestRequest.getURI()).isEqualTo(URI.create("https://www.google.com/images/srpr/logo4w.png"));
    }

    @Test
    public void orientationBroadcastReceiver_whenUnregistered_shouldIgnoreOnReceive() throws Exception {
        Intent intent = mock(Intent.class);
        stub(intent.getAction()).toReturn("some bogus action which we hope never to see");
        Context context = new Activity();
        MraidDisplayController.OrientationBroadcastReceiver receiver = subject.new OrientationBroadcastReceiver();
        receiver.register(context);

        receiver.unregister();
        receiver.onReceive(context, intent);

        verify(intent, never()).getAction();
    }

    @Test
    public void showVideo_shouldStartVideoPlayerActivity() throws Exception {
        subject.showVideo(VIDEO_URL);
        assertMraidVideoPlayerActivityStarted(MraidVideoPlayerActivity.class, VIDEO_URL, adConfiguration);
    }

    @Test
    public void getCurrentPosition_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());

        subject.getCurrentPosition();

        verify(mraidView).fireErrorEvent(eq(GET_CURRENT_POSITION), any(String.class));
    }

    @Test
    public void getDefaultPosition_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());

        subject.getDefaultPosition();

        verify(mraidView).fireErrorEvent(eq(GET_DEFAULT_POSITION), any(String.class));
    }

    @Test
    public void getMaxSize_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());

        subject.getMaxSize();

        verify(mraidView).fireErrorEvent(eq(GET_MAX_SIZE), any(String.class));
    }
    @Test
    public void getScreenSize_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());

        subject.getScreenSize();

        verify(mraidView).fireErrorEvent(eq(GET_SCREEN_SIZE), any(String.class));
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withMinimumValidParams_atLeastICS_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();

        subject.createCalendarEvent(params);

        verify(mraidView, never()).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.TITLE)).isNotNull();
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1)).isNotEqualTo(-1);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withoutSecondsOnStartDate_atLeastICS_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();
        params.put("start", "2012-12-21T00:00-0500");

        subject.createCalendarEvent(params);

        verify(mraidView, never()).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.TITLE)).isNotNull();
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1)).isNotEqualTo(-1);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withDailyRecurrence_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withDailyRecurrence_withInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");
        params.put("interval", "2");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;INTERVAL=2;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInterval_withOutWeekday_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("interval", "7");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;INTERVAL=7;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_onAllWeekDays_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "0,1,2,3,4,5,6");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_onDuplicateWeekDays_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "3,2,3,3,7,0");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;BYDAY=WE,TU,SU;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInterval_withWeekDay_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("interval", "1");
        params.put("daysInWeek", "1");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withDailyRecurrence_withWeeklyRecurrence_withMonthlyOccurence_shouldCreateDailyCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");
        params.put("frequency", "daily");
        params.put("frequency", "daily");
        params.put("interval", "2");
        params.put("daysInWeek", "1");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;INTERVAL=2;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withOutInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("interval", "2");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;INTERVAL=2;");
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withOutInterval_withDaysOfMonth_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("daysInMonth", "2,-15");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=2,-15;");
    }

    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withInvalidDaysOfMonth_shouldNotCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("daysInMonth", "55");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        assertThat(intent).isNull();
        assertThat(ShadowLog.getLogs().size()).isEqualTo(1);
    }

    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInvalidDaysOfWeek_shouldNotCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "-1,20");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        assertThat(intent).isNull();
        assertThat(ShadowLog.getLogs().size()).isEqualTo(1);
    }

    @Config(reportSdk = VERSION_CODES.HONEYCOMB_MR2)
    @Test
    public void createCalendarEvent_beforeIcs_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());

        subject.createCalendarEvent(params);

        verify(mraidView).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));
    }

    @Test
    public void createCalendarEvent_withInvalidDate_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());
        params.put("start", "2013-08-14T09:00.-08:00");
        params.put("description", "Some Event");

        subject.createCalendarEvent(params);

        verify(mraidView).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));
    }

    @Test
    public void createCalendarEvent_withMissingParameters_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());
        //it needs a start time
        params.put("description", "Some Event");

        subject.createCalendarEvent(params);

        verify(mraidView).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));
    }

    @Test
    public void createCalendarEvent_withNullDate_shouldFireErrorEvent() throws Exception {
        resetMockMraidView(new Activity());
        params.put("start", null);
        params.put("description", "Some Event");

        subject.createCalendarEvent(params);

        verify(mraidView).fireErrorEvent(eq(CREATE_CALENDAR_EVENT), any(String.class));
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void
    createCalendarEvent_withValidParamsAllExceptRecurrence_atLeastICS_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();
        params.put("location", "my house");
        params.put("end", "2013-08-14T22:01:01-0000");
        params.put("summary", "some description actually");
        params.put("transparency", "transparent");

        subject.createCalendarEvent(params);

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.TITLE)).isNotNull();
        assertThat(intent.getStringExtra(CalendarContract.Events.DESCRIPTION)).isNotNull();
        assertThat(intent.getStringExtra(CalendarContract.Events.EVENT_LOCATION)).isNotNull();
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1)).isNotEqualTo(-1);
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1)).isNotEqualTo(-1);
        assertThat(intent.getIntExtra(CalendarContract.Events.AVAILABILITY, -1)).isEqualTo(CalendarContract.Events.AVAILABILITY_FREE);
    }

    @Test
    public void addCloseEventRegion_shouldAddCloseEventRegionToFrameLayout() throws Exception {
        Activity activity = new Activity();
        FrameLayout frameLayout = new FrameLayout(activity);
        subject.addCloseEventRegion(frameLayout);

        final Button closeEventRegion = (Button) frameLayout.getChildAt(0);
        assertThat(closeEventRegion.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(closeEventRegion).getBackgroundColor()).isEqualTo(Color.TRANSPARENT);
        assertThat(shadowOf(closeEventRegion).getOnClickListener()).isEqualTo(subject.getCloseOnClickListener());
        assertThat(Dips.pixelsToIntDips((float)closeEventRegion.getLayoutParams().width, activity)).isEqualTo(50);
        assertThat(Dips.pixelsToIntDips((float)closeEventRegion.getLayoutParams().height, activity)).isEqualTo(50);
        assertThat(((FrameLayout.LayoutParams)closeEventRegion.getLayoutParams()).gravity).isEqualTo(Gravity.TOP | Gravity.RIGHT);
    }

    private void resetMockMraidView(Context context) {
        reset(mraidView);
        stub(mraidView.getContext()).toReturn(context);
        when(mraidView.getParent()).thenReturn(moPubView).thenReturn(null);
        stub(mraidView.getRootView()).toReturn(rootView);
        stub(mraidView.getAdConfiguration()).toReturn(adConfiguration);
    }

    private MraidSupportsProperty captureMraidSupportProperties() {
        ArgumentCaptor<MraidSupportsProperty> propertiesCaptor = ArgumentCaptor.forClass(MraidSupportsProperty.class);
        verify(mraidView).fireChangeEventForProperty(propertiesCaptor.capture());
        return propertiesCaptor.getValue();
    }

    private void downloadImageForPendingResponse(String uri, HttpResponse response){
        Robolectric.addPendingHttpResponse(response);

        stub(mraidView.getContext()).toReturn(Robolectric.application);
        subject = new TestMraidDisplayController(mraidView, null, null);
        subject.showUserDownloadImageAlert(uri);

        ThreadUtils.pause(TIME_TO_PAUSE_FOR_NETWORK);
    }

    private void assertThatMimeTypeWasAddedCorrectly(String originalFileName, String contentType, String expectedFileName, String expectedExtension) {
        expectedFile = new File(pictureDirectory, expectedFileName);
        response = new TestHttpResponseWithHeaders(200, FAKE_IMAGE_DATA);
        response.addHeader(MIME_TYPE_HEADER, contentType);

        downloadImageForPendingResponse(originalFileName, response);

        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.getName()).endsWith(expectedExtension);
        assertThat(fileWithoutExtension.exists()).isFalse();
    }

    private void setupCalendarParams() {
        //we need mock Context so that we can validate that isCalendarAvailable() is true
        Context mockContext = createMockContextWithSpecificIntentData(null, null, ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");

        //but a mock context does't know how to startActivity(), so we stub it to use ShadowContext for starting activity
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (!(invocation.getArguments()[0] instanceof Intent)) {
                    throw new ClassCastException("For some reason you are not passing the calendar intent properly");
                }
                Context shadowContext = Robolectric.getShadowApplication().getApplicationContext();
                shadowContext.startActivity((Intent) invocation.getArguments()[0]);
                return null;
            }
        }).when(mockContext).startActivity(any(Intent.class));

        resetMockMraidView(mockContext);
        params.put("description", "Some Event");
        params.put("start", CALENDAR_START_TIME);
    }

    private Context createMockContextWithSpecificIntentData(final String scheme, final String componentName, final String type, final String action) {
        return MraidsTest.createMockContextWithSpecificIntentData(scheme, componentName, type, action);
    }

    private class TestMraidDisplayController extends MraidDisplayController {
        public TestMraidDisplayController(MraidView mraidView, MraidView.ExpansionStyle expStyle,
                                          MraidView.NativeCloseButtonStyle buttonStyle) {
            super(mraidView, expStyle, buttonStyle);
        }

        @Override
        FrameLayout createAdContainerLayout() {
            return adContainerLayout;
        }

        @Override
        RelativeLayout createExpansionLayout() {
            return expansionLayout;
        }

        @Override
        FrameLayout createPlaceholderView() {
            return placeholderView;
        }
    }
}
