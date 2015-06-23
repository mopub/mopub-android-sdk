package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.Streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AdAlertReporter {
    private static final String EMAIL_RECIPIENT = "creative-review@mopub.com";
    private static final String EMAIL_SCHEME = "mailto:";
    private static final String SCREEN_SHOT_FILENAME = "mp_adalert_screenshot.png";
    private static final String PARAMETERS_FILENAME = "mp_adalert_parameters.txt";
    private static final String MARKUP_FILENAME = "mp_adalert_markup.html";
    private static final String DATE_FORMAT_PATTERN = "M/d/yy hh:mm:ss a z";
    private static final int IMAGE_QUALITY = 25;
    private static final String BODY_SEPARATOR = "\n=================\n";

    private final String mDateString;

    private final View mView;
    private final Context mContext;
    private Intent mEmailIntent;
    private ArrayList<Uri> mEmailAttachments;
    private String mParameters;
    private String mResponse;

    public AdAlertReporter(final Context context, final View view, @Nullable final AdReport adReport) {
        mView = view;
        mContext = context;

        mEmailAttachments = new ArrayList<Uri>();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US);
        mDateString = dateFormat.format(DateAndTime.now());

        initEmailIntent();
        Bitmap screenShot = takeScreenShot();
        String screenShotString = convertBitmapInWEBPToBase64EncodedString(screenShot);
        mParameters = "";
        mResponse = "";
        if (adReport != null) {
            mParameters = adReport.toString();
            mResponse = adReport.getResponseString();
        }

        addEmailSubject();
        addEmailBody(mParameters, mResponse, screenShotString);
        addTextAttachment(PARAMETERS_FILENAME, mParameters);
        addTextAttachment(MARKUP_FILENAME, mResponse);
        addImageAttachment(SCREEN_SHOT_FILENAME, screenShot);
    }

    public void send() {
        mEmailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mEmailAttachments);

        Intent chooserIntent = Intent.createChooser(mEmailIntent, "Send Email...");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(chooserIntent);
    }

    private void initEmailIntent() {
        Uri emailScheme = Uri.parse(EMAIL_SCHEME);
        mEmailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        mEmailIntent.setDataAndType(emailScheme, "plain/text");
        mEmailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{EMAIL_RECIPIENT});
    }

    private Bitmap takeScreenShot() {
        if (mView == null || mView.getRootView() == null) {
            return null;
        }

        View rootView = mView.getRootView();
        boolean wasDrawingCacheEnabled = rootView.isDrawingCacheEnabled();
        rootView.setDrawingCacheEnabled(true);

        Bitmap drawingCache = rootView.getDrawingCache();
        if (drawingCache == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(drawingCache);
        rootView.setDrawingCacheEnabled(wasDrawingCacheEnabled);

        return bitmap;
    }

    private String convertBitmapInWEBPToBase64EncodedString(Bitmap bitmap) {
        String result = null;
        if (bitmap != null) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                result = Base64.encodeToString(bytes, Base64.DEFAULT);
            } catch (Exception e) {
                // should we log something here?
            }
        }
        return result;
    }

    private void addEmailSubject() {
        mEmailIntent.putExtra(Intent.EXTRA_SUBJECT, "New creative violation report - " + mDateString);
    }

    private void addEmailBody(String... data) {
        StringBuilder body = new StringBuilder();
        int i = 0;
        while (i<data.length) {
            body.append(data[i]);
            if (i!=data.length-1) {
                body.append(BODY_SEPARATOR);
            }
            i++;
        }
        mEmailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
    }

    private void addImageAttachment(String fileName, Bitmap bitmap) {
        FileOutputStream fileOutputStream = null;

        if (fileName == null || bitmap == null) {
            return;
        }

        try {
            fileOutputStream = mContext.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            // image quality is okay to be 0 here, since PNG is lossless and will ignore compression quality
            bitmap.compress(Bitmap.CompressFormat.PNG, IMAGE_QUALITY, fileOutputStream);

            Uri fileUri = Uri.fromFile(new File(mContext.getFilesDir() + File.separator + fileName));
            mEmailAttachments.add(fileUri);
        } catch (Exception exception) {
            MoPubLog.d("Unable to write text attachment to file: " + fileName);
        } finally {
            Streams.closeStream(fileOutputStream);
        }
    }

    private void addTextAttachment(String fileName, String body) {
        FileOutputStream fileOutputStream = null;

        if (fileName == null || body == null) {
            return;
        }

        try {
            fileOutputStream = mContext.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            fileOutputStream.write(body.getBytes());

            Uri fileUri = Uri.fromFile(new File(mContext.getFilesDir() + File.separator + fileName));
            mEmailAttachments.add(fileUri);
        } catch (Exception exception) {
            MoPubLog.d("Unable to write text attachment to file: " + fileName);
        } finally {
            Streams.closeStream(fileOutputStream);
        }
    }

    @Deprecated // for testing
    Intent getEmailIntent() {
        return mEmailIntent;
    }

    @Deprecated // for testing
    ArrayList<Uri> getEmailAttachments() {
        return mEmailAttachments;
    }

    @Deprecated // for testing
    String getParameters() {
        return mParameters;
    }

    @Deprecated
    String getResponse(){
        return mResponse;
    }
}

