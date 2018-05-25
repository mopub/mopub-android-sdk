package com.mopub.common.privacy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

public class ConsentDialogActivity extends Activity {
    private static final int CLOSE_BUTTON_DELAY_MS = 10000;
    private static final String KEY_HTML_PAGE = "html-page-content";

    @Nullable
    private ConsentDialogLayout mView;
    @Nullable
    private Runnable mEnableCloseButtonRunnable;
    @Nullable
    private Handler mCloseButtonHandler;

    @Nullable
    private ConsentStatus mConsentStatus;

    static void start(@NonNull final Context context, @NonNull String htmlData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(htmlData);

        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.e("ConsentDialogActivity htmlData can't be empty string.");
            return;
        }

        Intent intent = createIntent(context, htmlData);
        try {
            Intents.startActivity(context, intent);
        } catch (ActivityNotFoundException | IntentNotResolvableException e) {
            MoPubLog.e("ConsentDialogActivity not found - did you declare it in AndroidManifest.xml?");
        }
    }

    @NonNull
    static Intent createIntent(@NonNull final Context context, @NonNull final String htmlPageContent) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(htmlPageContent);

        Bundle extra = new Bundle();
        extra.putString(KEY_HTML_PAGE, htmlPageContent);
        return Intents.getStartActivityIntent(context, ConsentDialogActivity.class, extra);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String htmlBody = intent.getStringExtra(KEY_HTML_PAGE);
        if (TextUtils.isEmpty(htmlBody)) {
            MoPubLog.e("Web page for ConsentDialogActivity is empty");
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mView = new ConsentDialogLayout(this);
        mView.setConsentClickListener(new ConsentDialogLayout.ConsentListener() {
            @Override
            public void onConsentClick(ConsentStatus status) {
                saveConsentStatus(status);
                setCloseButtonVisibility(false);
            }

            @Override
            public void onCloseClick() {
                finish();
            }
        });

        mEnableCloseButtonRunnable = new Runnable() {
            @Override
            public void run() {
                setCloseButtonVisibility(true);
            }
        };

        setContentView(mView);

        mView.startLoading(htmlBody, new ConsentDialogLayout.PageLoadListener() {
            @Override
            public void onLoadProgress(int progress) {
                if (progress == ConsentDialogLayout.FINISHED_LOADING) {
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCloseButtonHandler = new Handler();
        mCloseButtonHandler.postDelayed(mEnableCloseButtonRunnable, CLOSE_BUTTON_DELAY_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setCloseButtonVisibility(true);
    }

    @Override
    protected void onDestroy() {
        final PersonalInfoManager infoManager = MoPub.getPersonalInformationManager();
        if (infoManager != null && mConsentStatus != null) {
            infoManager.changeConsentStateFromDialog(mConsentStatus);
        }
        super.onDestroy();
    }


    void setCloseButtonVisibility(boolean visible) {
        if (mCloseButtonHandler != null) {
            mCloseButtonHandler.removeCallbacks(mEnableCloseButtonRunnable);
        }
        if (mView != null) {
            mView.setCloseVisible(visible);
        }
    }

    private void saveConsentStatus(@NonNull final ConsentStatus status) {
        Preconditions.checkNotNull(status);
        mConsentStatus = status;
    }
}
