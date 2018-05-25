package com.mopub.mraid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.ShadowMoPubHttpUrlConnection;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.FileUtils;
import com.mopub.mraid.MraidNativeCommandHandler.DownloadImageAsyncTask;
import com.mopub.mraid.MraidNativeCommandHandler.DownloadImageAsyncTask.DownloadImageAsyncTaskListener;
import com.mopub.mraid.MraidNativeCommandHandler.MraidCommandFailureListener;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.os.Environment.MEDIA_MOUNTED;
import static com.mopub.mraid.MraidNativeCommandHandler.ANDROID_CALENDAR_CONTENT_TYPE;
import static com.mopub.mraid.MraidNativeCommandHandler.MIME_TYPE_HEADER;
import static java.io.File.separator;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowAsyncTasks.class, ShadowMoPubHttpUrlConnection.class})
public class MraidNativeCommandHandlerTest {
    private static final String IMAGE_URI_VALUE = "file://tmp/expectedFile.jpg";
    private static final String REMOTE_IMAGE_URL = "https://www.mopub.com/expectedFile.jpg";
    private static final int TIME_TO_PAUSE_FOR_NETWORK = 300;
    private static final String FAKE_IMAGE_DATA = "imageFileData";
    //XXX: Robolectric or JUNIT doesn't support the correct suffix ZZZZZ in the parse pattern, so replacing xx:xx with xxxx for time.
    private static final String CALENDAR_START_TIME = "2013-08-14T20:00:00-0000";

    @Mock MraidCommandFailureListener mockMraidCommandFailureListener;
    @Mock DownloadImageAsyncTaskListener mockDownloadImageAsyncTaskListener;
    private MraidNativeCommandHandler subject;
    private Context context;
    private Map<String, String> params;

    private File expectedFile;
    private File pictureDirectory;
    private File fileWithoutExtension;

    @Before
    public void setUp() throws Exception {
        subject = new MraidNativeCommandHandler();
        context = Robolectric.buildActivity(Activity.class).create().get();

        FileUtils.copyFile("etc/expectedFile.jpg", "/tmp/expectedFile.jpg");
        expectedFile = new File(Environment.getExternalStorageDirectory(), "Pictures" + separator + "expectedFile.jpg");
        pictureDirectory = new File(Environment.getExternalStorageDirectory(), "Pictures");
        fileWithoutExtension = new File(pictureDirectory, "file");

        // Mount external storage and grant necessary permissions
        ShadowEnvironment.setExternalStorageState(MEDIA_MOUNTED);
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @After
    public void tearDown() {
        ShadowToast.reset();
    }

    @Test
    public void showUserDownloadImageAlert_withActivityContext_shouldDisplayAlertDialog() throws Exception {
        subject.storePicture(context, IMAGE_URI_VALUE, mockMraidCommandFailureListener);

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(alertDialog);

        assertThat(alertDialog.isShowing());

        assertThat(shadowAlertDialog.getTitle()).isEqualTo("Save Image");
        assertThat(shadowAlertDialog.getMessage()).isEqualTo("Download image to Picture gallery?");
        assertThat(shadowAlertDialog.isCancelable()).isTrue();

        assertThat(alertDialog.getButton(BUTTON_POSITIVE).hasOnClickListeners());
        assertThat(alertDialog.getButton(BUTTON_NEGATIVE)).isNotNull();
    }

    @Test
    public void showUserDownloadImageAlert_withAppContext_shouldToastAndStartDownloadImageAsyncTask() throws Exception {
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);

        subject.storePicture(context.getApplicationContext(), IMAGE_URI_VALUE, mockMraidCommandFailureListener);

        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Downloading image to Picture gallery...");

        assertThat(ShadowAsyncTasks.wasCalled()).isTrue();
        assertThat(ShadowAsyncTasks.getLatestAsyncTask()).isInstanceOf(DownloadImageAsyncTask.class);
        final List<?> latestParams = ShadowAsyncTasks.getLatestParams();
        assertThat(latestParams).hasSize(1);
        assertThat(latestParams.get(0)).isEqualTo(IMAGE_URI_VALUE);
    }

    @Test(expected = MraidCommandException.class)
    public void showUserDownloadImageAlert_whenStorePictureNotSupported_shouldThrowMraidCommandException() throws Exception {
        ShadowApplication.getInstance().denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        subject.storePicture(context, IMAGE_URI_VALUE, mockMraidCommandFailureListener);
    }

    @Test
    public void showUserDownloadImageAlert_whenOkayClicked_shouldStartDownloadImageAsyncTask() throws Exception {
        subject.storePicture(context, IMAGE_URI_VALUE, mockMraidCommandFailureListener);

        ShadowAlertDialog.getLatestAlertDialog().getButton(BUTTON_POSITIVE).performClick();

        assertThat(ShadowAsyncTasks.wasCalled()).isTrue();
        assertThat(ShadowAsyncTasks.getLatestAsyncTask()).isInstanceOf(DownloadImageAsyncTask.class);
        final List<?> latestParams = ShadowAsyncTasks.getLatestParams();
        assertThat(latestParams).hasSize(1);
        assertThat(latestParams.get(0)).isEqualTo(IMAGE_URI_VALUE);
    }

    @Test
    public void showUserDownloadImageAlert_whenCancelClicked_shouldDismissDialog_shouldNotStartDownloadImageAsyncTask() throws Exception {
        subject.storePicture(context, IMAGE_URI_VALUE, mockMraidCommandFailureListener);

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        ShadowAlertDialog shadowAlertDialog = Shadows.shadowOf(alertDialog);

        alertDialog.getButton(BUTTON_NEGATIVE).performClick();
        assertThat(shadowAlertDialog.hasBeenDismissed()).isTrue();
        assertThat(ShadowAsyncTasks.wasCalled()).isFalse();
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_shouldReturnTrueAndCreateFile() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>("Content-Type", "image/jpg")));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);

        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{REMOTE_IMAGE_URL});

        assertThat(result).isTrue();
        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.length()).isEqualTo(FAKE_IMAGE_DATA.length());
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_withLocationHeaderSet_shouldUseLocationHeaderAsFilename() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(
                        new Pair<String, String>("Content-Type", "image/jpg"),
                        new Pair<String, String>("Location", "https://www.newhost.com/images/blah/file.wow")
                )
        );

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);
        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{REMOTE_IMAGE_URL});

        expectedFile = new File(Environment.getExternalStorageDirectory(), "Pictures" + separator + "file.wow.jpg");

        assertThat(result).isTrue();
        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.length()).isEqualTo(FAKE_IMAGE_DATA.length());
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_withMissingMimeTypeHeaders_shouldUseDefaultFilename() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA);

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);
        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{REMOTE_IMAGE_URL});

        assertThat(result).isTrue();
        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.length()).isEqualTo(FAKE_IMAGE_DATA.length());
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_withNullArray_shouldReturnFalseAndNotCreateFile() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>("Content-Type", "image/jpg")));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);

        final Boolean result =
                downloadImageAsyncTask.doInBackground(null);

        assertThat(result).isFalse();
        assertThat(expectedFile.exists()).isFalse();
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_withEmptyArray_shouldReturnFalseAndNotCreateFile() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>("Content-Type", "image/jpg")));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);

        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{});

        assertThat(result).isFalse();
        assertThat(expectedFile.exists()).isFalse();
    }

    @Test
    public void downloadImageAsyncTask_doInBackground_withArrayContainingNull_shouldReturnFalseAndNotCreateFile() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>("Content-Type", "image/jpg")));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);

        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{null});

        assertThat(result).isFalse();
        assertThat(expectedFile.exists()).isFalse();
    }

    @Test
    public void downloadImage_withFailedImageDownload_shouldToastErrorMessageAndNotifyOnFailure() {
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        subject.downloadImage(context.getApplicationContext(), IMAGE_URI_VALUE, mockMraidCommandFailureListener);

        DownloadImageAsyncTask latestAsyncTask = (DownloadImageAsyncTask) ShadowAsyncTasks.getLatestAsyncTask();
        latestAsyncTask.getListener().onFailure();

        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Image failed to download.");
        verify(mockMraidCommandFailureListener).onFailure(any(MraidCommandException.class));
    }

    @Test
    public void downloadImage_withMimeTypeAndNoFileExtension_shouldSavePictureWithMimeType() throws Exception {
        String fileNameWithNoExtension = "https://www.somewhere.com/images/blah/file";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithNoExtension,
                "image/jpg",
                "file.jpg",
                ".jpg");
    }

    @Test
    public void downloadImage_withMultipleContentTypesAndNoFileExtension_shouldSavePictureWithMimeType() throws Exception {
        String fileNameWithNoExtension = "https://www.somewhere.com/images/blah/file";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithNoExtension,
                "text/html; image/png",
                "file.png",
                ".png");
    }

    @Test
    public void downloadImage_withMimeTypeAndFileExtension_shouldSavePictureWithFileExtension() throws Exception {
        String fileNameWithExtension = "https://www.somewhere.com/images/blah/file.extension";

        assertThatMimeTypeWasAddedCorrectly(
                fileNameWithExtension,
                "image/extension",
                "file.extension",
                ".extension");

        assertThat((expectedFile.getName()).endsWith(".extension.extension")).isFalse();
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMinimumValidParams_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();

        subject.createCalendarEvent(context, params);

        verify(mockMraidCommandFailureListener, never()).onFailure(any(MraidCommandException.class));

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.TITLE)).isNotNull();
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1)).isNotEqualTo(-1);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withoutSecondsOnStartDate_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();
        params.put("start", "2012-12-21T00:00-0500");

        subject.createCalendarEvent(context, params);

        verify(mockMraidCommandFailureListener, never()).onFailure(any(MraidCommandException.class));

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.TITLE)).isNotNull();
        assertThat(intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1)).isNotEqualTo(-1);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withDailyRecurrence_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getType()).isEqualTo(ANDROID_CALENDAR_CONTENT_TYPE);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withDailyRecurrence_withInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");
        params.put("interval", "2");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;INTERVAL=2;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInterval_withOutWeekday_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("interval", "7");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;INTERVAL=7;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_onAllWeekDays_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "0,1,2,3,4,5,6");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_onDuplicateWeekDays_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "3,2,3,3,7,0");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;BYDAY=WE,TU,SU;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInterval_withWeekDay_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("interval", "1");
        params.put("daysInWeek", "1");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=WEEKLY;INTERVAL=1;BYDAY=MO;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withDailyRecurrence_withWeeklyRecurrence_withMonthlyOccurence_shouldCreateDailyCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "daily");
        params.put("frequency", "daily");
        params.put("frequency", "daily");
        params.put("interval", "2");
        params.put("daysInWeek", "1");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=DAILY;INTERVAL=2;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withOutInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withInterval_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("interval", "2");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;INTERVAL=2;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withOutInterval_withDaysOfMonth_shouldCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("daysInMonth", "2,-15");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(CalendarContract.Events.RRULE)).isEqualTo("FREQ=MONTHLY;BYMONTHDAY=2,-15;");
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMonthlyRecurrence_withInvalidDaysOfMonth_shouldNotCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "monthly");
        params.put("daysInMonth", "55");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(intent).isNull();
        assertThat(ShadowLog.getLogs().size()).isEqualTo(1);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withWeeklyRecurrence_withInvalidDaysOfWeek_shouldNotCreateCalendarIntent() throws Exception {
        setupCalendarParams();
        params.put("frequency", "weekly");
        params.put("daysInWeek", "-1,20");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();

        assertThat(intent).isNull();
        assertThat(ShadowLog.getLogs().size()).isEqualTo(1);
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withInvalidDate_shouldFireErrorEvent() throws Exception {
        params.put("start", "2013-08-14T09:00.-08:00");
        params.put("description", "Some Event");

        subject.createCalendarEvent(context, params);

        verify(mockMraidCommandFailureListener).onFailure(any(MraidCommandException.class));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withMissingParameters_shouldFireErrorEvent() throws Exception {
        //it needs a start time
        params.put("description", "Some Event");

        subject.createCalendarEvent(context, params);

        verify(mockMraidCommandFailureListener).onFailure(any(MraidCommandException.class));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void createCalendarEvent_withNullDate_shouldFireErrorEvent() throws Exception {
        params.put("start", null);
        params.put("description", "Some Event");

        subject.createCalendarEvent(context, params);

        verify(mockMraidCommandFailureListener).onFailure(any(MraidCommandException.class));
    }

    @Ignore("Mraid 2.0")
    @Test
    public void
    createCalendarEvent_withValidParamsAllExceptRecurrence_shouldCreateEventIntent() throws Exception {
        setupCalendarParams();
        params.put("location", "my house");
        params.put("end", "2013-08-14T22:01:01-0000");
        params.put("summary", "some description actually");
        params.put("transparency", "transparent");

        subject.createCalendarEvent(context, params);

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();

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
    public void isTelAvailable_whenCanAcceptIntent_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData("tel", null, null, "android.intent.action.DIAL");

        assertThat(subject.isTelAvailable(context)).isTrue();
    }

    @Test
    public void isTelAvailable_whenCanNotAcceptIntent_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.DIAL");

        assertThat(subject.isTelAvailable(context)).isFalse();
    }

    @Test
    public void isSmsAvailable_whenCanAcceptIntent_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData("sms", null, null, "android.intent.action.VIEW");

        assertThat(subject.isSmsAvailable(context)).isTrue();
    }

    @Test
    public void isSmsAvailable_whenCanNotAcceptIntent_shouldReturnFalse() throws Exception {
        context = createMockContextWithSpecificIntentData("", null, null, "android.intent.action.VIEW");

        assertThat(subject.isSmsAvailable(context)).isFalse();
    }

    @Test
    public void isStorePictureAvailable_whenPermissionDeclaredAndMediaMounted_shouldReturnTrue() throws Exception {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

        assertThat(subject.isStorePictureSupported(context)).isTrue();
    }

    @Test
    public void isStorePictureAvailable_whenPermissionDenied_shouldReturnFalse() throws Exception {
        ShadowApplication.getInstance().denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);

        assertThat(subject.isStorePictureSupported(context)).isFalse();
    }

    @Test
    public void isStorePictureAvailable_whenMediaUnmounted_shouldReturnFalse() throws Exception {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTED);

        assertThat(subject.isStorePictureSupported(context)).isFalse();
    }

    @Test
    public void isCalendarAvailable_shouldReturnTrue() throws Exception {
        context = createMockContextWithSpecificIntentData(null, null, ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");
        assertThat(subject.isCalendarAvailable(context)).isTrue();
    }

    @Test
    public void isCalendarAvailable_butCanNotAcceptIntent_shouldReturnFalse() throws
            Exception {
        context = createMockContextWithSpecificIntentData(null, null, "vnd.android.cursor.item/NOPE", "android.intent.action.INSERT");
        assertThat(subject.isCalendarAvailable(context)).isFalse();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnTrue() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isTrue();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreHardwareAccelerated_whenWindowIsNotHardwareAccelerated_shouldReturnFalse() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isFalse();
    }

    @Test
    public void isInlineVideoAvailable_whenViewsAreNotHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnFalse() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(false);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isFalse();
    }

    @Test
    public void isInlineVideoAvailable_whenViewParentIsNotHardwareAccelerated_whenWindowIsHardwareAccelerated_shouldReturnFalse() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // ViewParent
        LinearLayout mockLinearLayout = mock(LinearLayout.class);
        when(mockLinearLayout.isHardwareAccelerated()).thenReturn(false);
        when(mockLinearLayout.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);

        // View
        View mockView = mock(View.class);
        when(mockView.isHardwareAccelerated()).thenReturn(true);
        when(mockView.getLayerType()).thenReturn(View.LAYER_TYPE_HARDWARE);
        when(mockView.getParent()).thenReturn(mockLinearLayout);

        assertThat(subject.isInlineVideoAvailable(activity, mockView)).isFalse();
    }

    private static Context createMockContextWithSpecificIntentData(final String scheme, final String componentName, final String type, final String action) {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);

        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        resolveInfos.add(new ResolveInfo());

        stub(context.getPackageManager()).toReturn(packageManager);

        BaseMatcher intentWithSpecificData = new BaseMatcher() {
            // check that the specific intent has the special data, i.e. "tel:", or a component name, or string type, based on a particular data

            @Override
            public boolean matches(Object item) {
                if (item != null && item instanceof Intent ){
                    boolean result = action != null || type != null || componentName != null || scheme != null;
                    if (action != null) {
                        if (((Intent) item).getAction() != null) {
                            result = result && action.equals(((Intent) item).getAction());
                        }
                    }

                    if (type != null) {
                        if (((Intent) item).getType() != null) {
                            result = result && type.equals(((Intent) item).getType());
                        }
                    }

                    if (componentName != null) {
                        if (((Intent) item).getComponent() != null) {
                            result = result && componentName.equals(((Intent) item).getComponent().getClassName());
                        }
                    }

                    if (scheme != null) {
                        if (((Intent) item).getData() != null) {
                            result = result && scheme.equals(((Intent) item).getData().getScheme());
                        }
                    }
                    return result;
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {

            }
        };

        // It is okay to query with specific intent or nothing, because by default, none of the query would normally any resolveInfo anyways
        stub(packageManager.queryIntentActivities((Intent) argThat(intentWithSpecificData), eq(0))).toReturn(resolveInfos);
        return context;
    }

    private Map<String, List<String>> createHeaders(@NonNull final Pair<String, String>... pairs) {
        final TreeMap<String, List<String>> headers = new TreeMap<String, List<String>>();
        for (final Pair<String, String> pair : pairs) {
            String key = pair.first;
            String value = pair.second;

            if (!headers.containsKey(key)) {
                headers.put(key, new ArrayList<String>());
            }
            headers.get(key).add(value);
        }

        return headers;
    }

    private void assertThatMimeTypeWasAddedCorrectly(String originalFileName, String contentType,
            String expectedFileName, String expectedExtension) throws Exception {
        expectedFile = new File(pictureDirectory, expectedFileName);

        ShadowMoPubHttpUrlConnection.addPendingResponse(200, FAKE_IMAGE_DATA,
                createHeaders(new Pair<String, String>(MIME_TYPE_HEADER, contentType)));

        final DownloadImageAsyncTask downloadImageAsyncTask =
                new DownloadImageAsyncTask(context, mockDownloadImageAsyncTaskListener);
        final Boolean result =
                downloadImageAsyncTask.doInBackground(new String[]{originalFileName});

        assertThat(result).isTrue();
        assertThat(expectedFile.exists()).isTrue();
        assertThat(expectedFile.getName()).endsWith(expectedExtension);
        assertThat(fileWithoutExtension.exists()).isFalse();
    }

    private void setupCalendarParams() {
        //we need mock Context so that we can validate that isCalendarAvailable() is true
        Context mockContext = createMockContextWithSpecificIntentData(null,
                null, ANDROID_CALENDAR_CONTENT_TYPE, "android.intent.action.INSERT");

        //but a mock context doesn't know how to startActivity(), so we stub it to use ShadowContext for starting activity
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                if (!(invocation.getArguments()[0] instanceof Intent)) {
                    throw new ClassCastException("For some reason you are not passing the calendar intent properly");
                }
                Context shadowContext = ShadowApplication.getInstance().getApplicationContext();
                shadowContext.startActivity((Intent) invocation.getArguments()[0]);
                return null;
            }
        }).when(mockContext).startActivity(any(Intent.class));

        params.put("description", "Some Event");
        params.put("start", CALENDAR_START_TIME);
    }
}
