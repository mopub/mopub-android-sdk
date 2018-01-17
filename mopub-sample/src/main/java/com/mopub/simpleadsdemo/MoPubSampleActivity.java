package com.mopub.simpleadsdemo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.webkit.WebView;

import com.mopub.common.MoPub;
import com.mopub.common.util.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.mopub.common.Constants.UNUSED_REQUEST_CODE;

public class MoPubSampleActivity extends FragmentActivity {
    private static final List<String> REQUIRED_DANGEROUS_PERMISSIONS = new ArrayList<String>();
    static {
        REQUIRED_DANGEROUS_PERMISSIONS.add(ACCESS_COARSE_LOCATION);
        REQUIRED_DANGEROUS_PERMISSIONS.add(WRITE_EXTERNAL_STORAGE);
    }

    // Sample app web views are debuggable.
    static {
        setWebDebugging();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void setWebDebugging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private MoPubListFragment mMoPubListFragment;
    private Intent mDeeplinkIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (savedInstanceState != null) {
            return;
        }

        List<String> permissionsToBeRequested = new ArrayList<String>();
        for (String permission : REQUIRED_DANGEROUS_PERMISSIONS) {
            if (!DeviceUtils.isPermissionGranted(this, permission)) {
                permissionsToBeRequested.add(permission);
            }
        }

        // Request dangerous permissions
        if (!permissionsToBeRequested.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToBeRequested.toArray(
                    new String[permissionsToBeRequested.size()]), UNUSED_REQUEST_CODE);
        }

        // Set location awareness and precision globally for your app:
        MoPub.setLocationAwareness(MoPub.LocationAwareness.TRUNCATED);
        MoPub.setLocationPrecision(4);

        createMoPubListFragment(getIntent());

        // Intercepts all logs including Level.FINEST so we can show a toast
        // that is not normally user-facing. This is only used for native ads.
        LoggingUtils.enableCanaryLogging(this);
    }

    private void createMoPubListFragment(@NonNull final Intent intent) {
        if (findViewById(R.id.fragment_container) != null) {
            mMoPubListFragment = new MoPubListFragment();
            mMoPubListFragment.setArguments(intent.getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMoPubListFragment).commit();

            mDeeplinkIntent = intent;
        }
    }

    @Override
    public void onNewIntent(@NonNull final Intent intent) {
        mDeeplinkIntent = intent;
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        if (mMoPubListFragment != null && mDeeplinkIntent != null) {
            mMoPubListFragment.addAdUnitViaDeeplink(mDeeplinkIntent.getData());
            mDeeplinkIntent = null;
        }
    }
}
