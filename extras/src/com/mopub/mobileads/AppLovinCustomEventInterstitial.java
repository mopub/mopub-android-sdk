package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.lang.reflect.Method;
import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK interstitial adapter for MoPub.
 * <p>
 * Created by Thomas So on 5/27/17.
 *
 * @version 2.0
 */

// Please note: We have renamed this class from "AppLovinInterstitialAdapter" to "AppLovinCustomEventInterstitial".
// If this is your first time integrating, please use "YOUR_PACKAGE_NAME.AppLovinCustomEventInterstitial" as the custom event classname in the MoPub dashboard.
// If you have integrated this before, please rename this class back to "AppLovinInterstitialAdapter" and use "YOUR_PACKAGE_NAME.AppLovinInterstitialAdapter" as the custom event classname in the MoPub dashboard.
public class AppLovinCustomEventInterstitial
        extends CustomEventInterstitial
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private static final boolean LOGGING_ENABLED = true;

    private CustomEventInterstitialListener listener;
    private Context                         context;

    private AppLovinAd loadedAd;

    //
    // MoPub Custom Event Methods
    //

    @Override
    public void loadInterstitial(final Context context, final CustomEventInterstitialListener listener, final Map<String, Object> localExtras, final Map<String, String> serverExtras)
    {
        log( DEBUG, "Requesting AppLovin interstitial with localExtras: " + localExtras );

        // SDK versions BELOW 7.2.0 require a instance of an Activity to be passed in as the context
        if ( AppLovinSdk.VERSION_CODE < 720 && !( context instanceof Activity ) )
        {
            log( ERROR, "Unable to request AppLovin banner. Invalid context provided." );
            listener.onInterstitialFailed( MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );

            return;
        }

        // Store parent objects
        this.listener = listener;
        this.context = context;

        final AppLovinSdk sdk = AppLovinSdk.getInstance( context );
        sdk.setPluginVersion( "MoPub-2.0" );
        sdk.getAdService().loadNextAd( AppLovinAdSize.INTERSTITIAL, this );
    }

    @Override
    public void showInterstitial()
    {
        if ( loadedAd != null )
        {
            final AppLovinSdk sdk = AppLovinSdk.getInstance( context );

            final AppLovinInterstitialAdDialog interstitialAd = createInterstitial( context, sdk );
            interstitialAd.setAdDisplayListener( this );
            interstitialAd.setAdClickListener( this );
            interstitialAd.setAdVideoPlaybackListener( this );
            interstitialAd.showAndRender( loadedAd );
        }
        else
        {
            log( ERROR, "Failed to show an AppLovin interstitial before one was loaded" );
            listener.onInterstitialFailed( MoPubErrorCode.NETWORK_INVALID_STATE );
        }
    }

    @Override
    public void onInvalidate() {}

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        log( DEBUG, "Interstitial did load ad: " + ad.getAdIdNumber() );

        loadedAd = ad;

        listener.onInterstitialLoaded();
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        log( ERROR, "Interstitial failed to load with error: " + errorCode );
        listener.onInterstitialFailed( toMoPubErrorCode( errorCode ) );
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd appLovinAd)
    {
        log( DEBUG, "Interstitial displayed" );
        listener.onInterstitialShown();
    }

    @Override
    public void adHidden(final AppLovinAd appLovinAd)
    {
        log( DEBUG, "Interstitial dismissed" );
        listener.onInterstitialDismissed();
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd appLovinAd)
    {
        log( DEBUG, "Interstitial clicked" );
        listener.onLeaveApplication();
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad)
    {
        log( DEBUG, "Interstitial video playback began" );
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched)
    {
        log( DEBUG, "Interstitial video playback ended at playback percent: " + percentViewed );
    }

    //
    // Utility Methods
    //

    private AppLovinInterstitialAdDialog createInterstitial(final Context context, final AppLovinSdk sdk)
    {
        AppLovinInterstitialAdDialog inter = null;

        try
        {
            // AppLovin SDK < 7.2.0 uses an Activity, as opposed to Context in >= 7.2.0
            final Class<?> contextClass = ( AppLovinSdk.VERSION_CODE < 720 ) ? Activity.class : Context.class;
            final Method method = AppLovinInterstitialAd.class.getMethod( "create", AppLovinSdk.class, contextClass );

            inter = (AppLovinInterstitialAdDialog) method.invoke( null, sdk, context );
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to create AppLovinInterstitialAd." );
            listener.onInterstitialFailed( MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );
        }

        return inter;
    }

    private static void log(final int priority, final String message)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinInterstitial", message );
        }
    }

    private static MoPubErrorCode toMoPubErrorCode(final int applovinErrorCode)
    {
        if ( applovinErrorCode == AppLovinErrorCodes.NO_FILL )
        {
            return MoPubErrorCode.NETWORK_NO_FILL;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.UNSPECIFIED_ERROR )
        {
            return MoPubErrorCode.NETWORK_INVALID_STATE;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.NO_NETWORK )
        {
            return MoPubErrorCode.NO_CONNECTION;
        }
        else if ( applovinErrorCode == AppLovinErrorCodes.FETCH_AD_TIMEOUT )
        {
            return MoPubErrorCode.NETWORK_TIMEOUT;
        }
        else
        {
            return MoPubErrorCode.UNSPECIFIED;
        }
    }
}
