package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.applovin.adview.AppLovinAdView;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;

import java.lang.reflect.Constructor;
import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK banner adapter for MoPub.
 * <p>
 * Created by Thomas So on 3/6/17.
 *
 * @version 2.0
 */

// Please note: We have renamed this class from "AppLovinBannerAdapter" to "AppLovinCustomEventBanner".
// If this is your first time integrating, please use "YOUR_PACKAGE_NAME.AppLovinCustomEventBanner" as the custom event classname in the MoPub dashboard.
// If you have integrated this before, please rename this class back to "AppLovinBannerAdapter" and use "YOUR_PACKAGE_NAME.AppLovinBannerAdapter" as the custom event classname in the MoPub dashboard.
public class AppLovinCustomEventBanner
        extends CustomEventBanner
{
    private static final boolean LOGGING_ENABLED = true;

    private static final String AD_WIDTH_KEY  = "com_mopub_ad_width";
    private static final String AD_HEIGHT_KEY = "com_mopub_ad_height";

    //
    // MoPub Custom Event Methods
    //

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener, final Map<String, Object> localExtras, final Map<String, String> serverExtras)
    {
        // SDK versions BELOW 7.1.0 require a instance of an Activity to be passed in as the context
        if ( AppLovinSdk.VERSION_CODE < 710 && !( context instanceof Activity ) )
        {
            log( ERROR, "Unable to request AppLovin banner. Invalid context provided." );
            customEventBannerListener.onBannerFailed( MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );

            return;
        }

        log( DEBUG, "Requesting AppLovin banner with localExtras: " + localExtras );

        final AppLovinAdSize adSize = appLovinAdSizeFromServerExtras( localExtras );
        if ( adSize != null )
        {
            final AppLovinSdk sdk = AppLovinSdk.getInstance( context );
            sdk.setPluginVersion( "MoPub-2.0" );

            final AppLovinAdView adView = createAdView( adSize, context, customEventBannerListener );
            adView.setAdLoadListener( new AppLovinAdLoadListener()
            {
                @Override
                public void adReceived(final AppLovinAd ad)
                {
                    log( DEBUG, "Successfully loaded banner ad" );
                    customEventBannerListener.onBannerLoaded( adView );
                }

                @Override
                public void failedToReceiveAd(final int errorCode)
                {
                    log( ERROR, "Failed to load banner ad with code: " + errorCode );
                    customEventBannerListener.onBannerFailed( toMoPubErrorCode( errorCode ) );
                }
            } );
            adView.setAdDisplayListener( new AppLovinAdDisplayListener()
            {
                @Override
                public void adDisplayed(final AppLovinAd ad)
                {
                    log( DEBUG, "Banner displayed" );
                }

                @Override
                public void adHidden(final AppLovinAd ad)
                {
                    log( DEBUG, "Banner dismissed" );
                }
            } );
            adView.setAdClickListener( new AppLovinAdClickListener()
            {
                @Override
                public void adClicked(final AppLovinAd ad)
                {
                    log( DEBUG, "Banner clicked" );

                    customEventBannerListener.onBannerClicked();
                    customEventBannerListener.onLeaveApplication();
                }
            } );
            adView.loadNextAd();
        }
        else
        {
            log( ERROR, "Unable to request AppLovin banner" );

            customEventBannerListener.onBannerFailed( MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );
        }
    }

    @Override
    protected void onInvalidate() {}

    //
    // Utility Methods
    //

    private AppLovinAdSize appLovinAdSizeFromServerExtras(final Map<String, Object> serverExtras)
    {
        // Handle trivial case
        if ( serverExtras == null || serverExtras.isEmpty() )
        {
            log( ERROR, "No serverExtras provided" );
            return null;
        }

        try
        {
            final int width = (Integer) serverExtras.get( AD_WIDTH_KEY );
            final int height = (Integer) serverExtras.get( AD_HEIGHT_KEY );

            // We have valid dimensions
            if ( width > 0 && height > 0 )
            {
                log( DEBUG, "Valid width (" + width + ") and height (" + height + ") provided" );

                // Use the smallest AppLovinAdSize that will properly contain the adView

                if ( height <= AppLovinAdSize.BANNER.getHeight() )
                {
                    return AppLovinAdSize.BANNER;
                }
                else if ( height <= AppLovinAdSize.MREC.getHeight() )
                {
                    return AppLovinAdSize.MREC;
                }
                else
                {
                    log( ERROR, "Provided dimensions does not meet the dimensions required of banner or mrec ads" );
                }
            }
            else
            {
                log( ERROR, "Invalid width (" + width + ") and height (" + height + ") provided" );
            }
        }
        catch ( Throwable th )
        {
            log( ERROR, "Encountered error while parsing width and height from serverExtras", th );
        }

        return null;
    }

    //
    // Utility Methods
    //

    private AppLovinAdView createAdView(final AppLovinAdSize size, final Context parentContext, final CustomEventBannerListener customEventBannerListener)
    {
        AppLovinAdView adView = null;

        try
        {
            // AppLovin SDK < 7.1.0 uses an Activity, as opposed to Context in >= 7.1.0
            final Class<?> contextClass = ( AppLovinSdk.VERSION_CODE < 710 ) ? Activity.class : Context.class;
            final Constructor<?> constructor = AppLovinAdView.class.getConstructor( AppLovinAdSize.class, contextClass );

            adView = (AppLovinAdView) constructor.newInstance( size, parentContext );
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to get create AppLovinAdView." );
            customEventBannerListener.onBannerFailed( MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );
        }

        return adView;
    }

    private static void log(final int priority, final String message)
    {
        log( priority, message, null );
    }

    private static void log(final int priority, final String message, final Throwable th)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinBanner", message + ( ( th == null ) ? "" : Log.getStackTraceString( th ) ) );
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
