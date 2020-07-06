// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentDialogListener;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.ConsentStatusChangeListener;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.MoPubConversionTracker;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.ImpressionData;
import com.mopub.network.ImpressionListener;
import com.mopub.network.ImpressionsEmitter;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static com.mopub.common.Constants.UNUSED_REQUEST_CODE;
import static com.mopub.common.logging.MoPubLog.LogLevel.DEBUG;
import static com.mopub.common.logging.MoPubLog.LogLevel.INFO;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM_WITH_THROWABLE;

public class MoPubSampleActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final List<String> REQUIRED_DANGEROUS_PERMISSIONS = new ArrayList<>();
    private static final String SHOWING_CONSENT_DIALOG_KEY = "ShowingConsentDialog";

    static {
        REQUIRED_DANGEROUS_PERMISSIONS.add(ACCESS_COARSE_LOCATION);
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
    private boolean mShowingConsentDialog;
    @Nullable
    PersonalInfoManager mPersonalInfoManager;

    @Nullable
    DrawerLayout mDrawerLayout;

    @Nullable
    private ConsentStatusChangeListener mConsentStatusChangeListener;

    @NonNull
    private final LinkedBlockingDeque<String> mImpressionsList = new LinkedBlockingDeque<>();
    private ImpressionListener mImpressionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_title));

        setupNavigationDrawer(toolbar);

        List<String> permissionsToBeRequested = new ArrayList<>();
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

        if (savedInstanceState == null) {
            createMoPubListFragment(getIntent());
        } else {
            mShowingConsentDialog = savedInstanceState.getBoolean(SHOWING_CONSENT_DIALOG_KEY);
        }

        final SdkConfiguration.Builder configBuilder = new SdkConfiguration.Builder(
                "b195f8dd8ded45fe847ad89ed1d016da");
        if (BuildConfig.DEBUG) {
            configBuilder.withLogLevel(DEBUG);
        } else {
            configBuilder.withLogLevel(INFO);
        }

        SampleActivityUtils.addDefaultNetworkConfiguration(configBuilder);

        MoPub.initializeSdk(this, configBuilder.build(), initSdkListener());

        mConsentStatusChangeListener = initConsentChangeListener();
        mPersonalInfoManager = MoPub.getPersonalInformationManager();
        if (mPersonalInfoManager != null) {
            mPersonalInfoManager.subscribeConsentStatusChangeListener(mConsentStatusChangeListener);
        }

        // Intercepts all logs including Level.FINEST so we can show a toast
        // that is not normally user-facing. This is only used for native ads.
        LoggingUtils.enableCanaryLogging(this);


        mImpressionListener = createImpressionsListener();
        ImpressionsEmitter.addListener(mImpressionListener);
    }

    @Override
    protected void onDestroy() {
        if (mPersonalInfoManager != null) {
            // unsubscribe or memory leak will occur
            mPersonalInfoManager.unsubscribeConsentStatusChangeListener(mConsentStatusChangeListener);
        }
        mConsentStatusChangeListener = null;

        ImpressionsEmitter.removeListener(mImpressionListener);
        super.onDestroy();
    }

    private void createMoPubListFragment(@NonNull final Intent intent) {
        if (findViewById(R.id.fragment_container) != null) {
            mMoPubListFragment = new MoPubListFragment();
            mMoPubListFragment.setArguments(intent.getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mMoPubListFragment, LIST_FRAGMENT_TAG).commit();

            mDeeplinkIntent = intent;
        }
    }

    @Override
    public void onNewIntent(@NonNull final Intent intent) {
        super.onNewIntent(intent);
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

    private SdkInitializationListener initSdkListener() {
        return new SdkInitializationListener() {

            @Override
            public void onInitializationFinished() {
                syncNavigationMenu();
                Utils.logToast(MoPubSampleActivity.this, "SDK initialized.");
                if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                    mPersonalInfoManager.loadConsentDialog(initDialogLoadListener());
                }
                new MoPubConversionTracker(MoPubSampleActivity.this).reportAppOpen();
            }
        };
    }

    private ConsentStatusChangeListener initConsentChangeListener() {
        return new ConsentStatusChangeListener() {

            @Override
            public void onConsentStateChange(@NonNull ConsentStatus oldConsentStatus,
                                             @NonNull ConsentStatus newConsentStatus,
                                             boolean canCollectPersonalInformation) {
                Utils.logToast(MoPubSampleActivity.this, "Consent: " + newConsentStatus.name());
                if (mPersonalInfoManager != null && mPersonalInfoManager.shouldShowConsentDialog()) {
                    mPersonalInfoManager.loadConsentDialog(initDialogLoadListener());
                }
            }
        };
    }

    private ConsentDialogListener initDialogLoadListener() {
        return new ConsentDialogListener() {

            @Override
            public void onConsentDialogLoaded() {
                if (mPersonalInfoManager != null) {
                    mPersonalInfoManager.showConsentDialog();
                    mShowingConsentDialog = true;
                }
            }

            @Override
            public void onConsentDialogLoadFailed(@NonNull MoPubErrorCode moPubErrorCode) {
                Utils.logToast(MoPubSampleActivity.this, "Consent dialog failed to load.");
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*
        MoPub Sample specific test code
     */
    private static final String PRIVACY_FRAGMENT_TAG = "privacy_info_fragment";
    private static final String NETWORKS_FRAGMENT_TAG = "networks_info_fragment";
    private static final String LIST_FRAGMENT_TAG = "list_fragment";
    private static final String IMPRESSIONS_FRAGMENT_TAG = "impressions_info_fragment";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void setupNavigationDrawer(Toolbar toolbar) {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void syncNavigationMenu() {
        final NavigationView navigationView = findViewById(R.id.nav_view);

        SampleActivityInternalUtils.updateEndpointMenu(navigationView.getMenu());

        final PersonalInfoManager manager = MoPub.getPersonalInformationManager();
        if (manager != null) {
            final ConsentData consentData = manager.getConsentData();
            navigationView.getMenu().findItem(R.id.nav_force_gdpr).setChecked(consentData.isForceGdprApplies());

            final ConsentStatus consentStatus = manager.getPersonalInfoConsentStatus();

            if (consentStatus.equals(ConsentStatus.POTENTIAL_WHITELIST)) {
                navigationView.getMenu().findItem(R.id.nav_privacy_grant).setChecked(true);
            } else if (consentStatus.equals(ConsentStatus.EXPLICIT_NO)) {
                navigationView.getMenu().findItem(R.id.nav_privacy_revoke).setChecked(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_impressions:
                onImpressionsMenu();
                return true;
            case R.id.action_clear_logs:
                onClearLogs();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        SampleActivityInternalUtils.handleEndpointMenuSelection(menuItem);
        switch (menuItem.getItemId()) {
            case R.id.nav_privacy_info:
                onNavPrivacyInfo();
                break;
            case R.id.nav_privacy_grant:
                onNavChangeConsent(true);
                break;
            case R.id.nav_privacy_revoke:
                onNavChangeConsent(false);
                break;
            case R.id.nav_force_gdpr:
                onNavForceGdpr();
                break;
            case R.id.nav_adapters_info:
                onNavAdaptersInfo();
                break;
        }

        syncNavigationMenu();

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(SHOWING_CONSENT_DIALOG_KEY, mShowingConsentDialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mShowingConsentDialog) {
            mShowingConsentDialog = false;
            Utils.logToast(MoPubSampleActivity.this, "Consent dialog dismissed");
        }
    }

    private void onImpressionsMenu() {
        final FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(IMPRESSIONS_FRAGMENT_TAG) == null) {
            ImpressionsInfoFragment fragment = ImpressionsInfoFragment.newInstance(new ArrayList<>(mImpressionsList));
            manager.beginTransaction()
                    .replace(R.id.fragment_container, fragment, IMPRESSIONS_FRAGMENT_TAG)
                    .addToBackStack(IMPRESSIONS_FRAGMENT_TAG)
                    .commit();
        }
    }

    private void onNavPrivacyInfo() {
        final FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(PRIVACY_FRAGMENT_TAG) == null) {
            manager.beginTransaction()
                    .replace(R.id.fragment_container, new PrivacyInfoFragment(), PRIVACY_FRAGMENT_TAG)
                    .addToBackStack(PRIVACY_FRAGMENT_TAG)
                    .commit();
        }
    }

    private void onNavChangeConsent(boolean grant) {
        final FragmentManager manager = getSupportFragmentManager();
        final MoPubListFragment listFragment = (MoPubListFragment) manager.findFragmentByTag(LIST_FRAGMENT_TAG);
        if (listFragment == null) {
            MoPubLog.log(CUSTOM, getString(R.string.list_fragment_not_found));
            return; // fragment is not ready to update the consent
        }
        if (!listFragment.onChangeConsent(grant)) {
            return; // fragment is not ready to update the consent
        }

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().findItem(R.id.nav_privacy_grant).setChecked(grant);
        navigationView.getMenu().findItem(R.id.nav_privacy_revoke).setChecked(!grant);
    }

    private void onNavForceGdpr() {
        final PersonalInfoManager manager = MoPub.getPersonalInformationManager();
        if (manager != null) {
            manager.forceGdprApplies();
        }
    }

    private void onNavAdaptersInfo() {
        final FragmentManager manager = getSupportFragmentManager();
        if (manager.findFragmentByTag(NETWORKS_FRAGMENT_TAG) == null) {
            manager.beginTransaction()
                    .replace(R.id.fragment_container, new NetworksInfoFragment(), NETWORKS_FRAGMENT_TAG)
                    .addToBackStack(NETWORKS_FRAGMENT_TAG)
                    .commit();
        }
    }



    private void onClearLogs() {
        FragmentManager manager = getSupportFragmentManager();
        final ImpressionsInfoFragment fragment = (ImpressionsInfoFragment) manager.findFragmentByTag(IMPRESSIONS_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.onClear();
        }
        mImpressionsList.clear();
    }

    private ImpressionListener createImpressionsListener() {
        return new ImpressionListener() {
            @Override
            public void onImpression(@NonNull final String adUnitId, @Nullable final ImpressionData impressionData) {
                MoPubLog.log(CUSTOM, "impression for adUnitId: " + adUnitId);

                if (impressionData == null) {
                    mImpressionsList.addFirst("adUnitId: " + adUnitId + "\ndata= null");
                } else {
                    try {
                        mImpressionsList.addFirst(impressionData.getJsonRepresentation().toString(2));
                    } catch (JSONException e) {
                        MoPubLog.log(CUSTOM_WITH_THROWABLE, "Can't format impression data.", e);
                    }
                }
            }
        };
    }
}
