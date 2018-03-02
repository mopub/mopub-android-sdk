package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinSdk;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK rewarded video adapter for MoPub.
 * <p>
 * Created by Thomas So on 5/27/17.
 */

public class AppLovinRewardedVideo
        extends CustomEventRewardedVideo
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener
{
    private static final String DEFAULT_ZONE = "";

    // A map of Zone -> `AppLovinIncentivizedInterstitial` to be shared by instances of the custom event.
    // This prevents skipping of ads as this adapter will be re-created and preloaded (along with underlying `AppLovinIncentivizedInterstitial`)
    // on every ad load regardless if ad was actually displayed or not.
    private static final Map<String, AppLovinIncentivizedInterstitial> GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS = new HashMap<String, AppLovinIncentivizedInterstitial>();

    private static boolean initialized;
    private String zoneId;

    private AppLovinIncentivizedInterstitial incentivizedInterstitial;
    private Activity                         parentActivity;

    private boolean     fullyWatched;
    private MoPubReward reward;


    //
    // MoPub Custom Event Methods
    //

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception
    {
        log( DEBUG, "Initializing AppLovin rewarded video..." );

        if ( !initialized )
        {
            AppLovinSdk.initializeSdk( activity );
            AppLovinSdk.getInstance( activity ).setPluginVersion( "MoPub-Certified-2.1.0" );

            // Preload an incentivized ad for default zone
            AppLovinIncentivizedInterstitial incentivizedInterstitial = AppLovinIncentivizedInterstitial.create( activity );
            incentivizedInterstitial.preload( null );
            GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.put( DEFAULT_ZONE, incentivizedInterstitial );
            
            initialized = true;

            return true;
        }

        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception
    {
        log( DEBUG, "Requesting AppLovin rewarded video with serverExtras: " + serverExtras );

        parentActivity = activity;

        // Zones support is available on AppLovin SDK 7.5.0 and higher
        if ( AppLovinSdk.VERSION_CODE >= 750 && serverExtras != null && serverExtras.containsKey( "zone_id" ) )
        {
            zoneId = serverExtras.get( "zone_id" );
        }
        else
        {
            zoneId = DEFAULT_ZONE;
        }


        // Check if incentivized ad for zone already exists
        if ( GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.containsKey( zoneId ) )
        {
            incentivizedInterstitial = GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.get( zoneId );
        }
        else
        {
            // If this is a default Zone, create the incentivized ad normally
            if ( DEFAULT_ZONE.equals( zoneId ) )
            {
                incentivizedInterstitial = AppLovinIncentivizedInterstitial.create( activity );
            }
            // Otherwise, use the Zones API
            else
            {
                incentivizedInterstitial = createIncentivizedInterstitialForZoneId( zoneId, AppLovinSdk.getInstance( activity ) );
            }

            GLOBAL_INCENTIVIZED_INTERSTITIAL_ADS.put( zoneId, incentivizedInterstitial );
        }

        incentivizedInterstitial.preload( this );
    }

    @Override
    protected void showVideo()
    {
        if ( hasVideoAvailable() )
        {
            fullyWatched = false;
            reward = null;

            incentivizedInterstitial.show( parentActivity, null, this, this, this, this );
        }
        else
        {
            log( ERROR, "Failed to show an AppLovin rewarded video before one was loaded" );
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError( this.getClass(), getAdNetworkId(), MoPubErrorCode.VIDEO_PLAYBACK_ERROR );
        }
    }

    @Override
    protected boolean hasVideoAvailable()
    {
        return incentivizedInterstitial.isAdReadyToDisplay();
    }

    @Override
    @Nullable
    protected LifecycleListener getLifecycleListener() { return null; }

    @Override
    @NonNull
    protected String getAdNetworkId()
    {
        return zoneId;
    }

    @Override
    protected void onInvalidate() {}

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video did load ad: " + ad.getAdIdNumber() );
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess( this.getClass(), getAdNetworkId() );
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        log( DEBUG, "Rewarded video failed to load with error: " + errorCode );
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure( this.getClass(), getAdNetworkId(), toMoPubErrorCode( errorCode ) );
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video displayed" );
        MoPubRewardedVideoManager.onRewardedVideoStarted( this.getClass(), getAdNetworkId() );
    }

    @Override
    public void adHidden(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video dismissed" );

        if ( fullyWatched && reward != null )
        {
            log( DEBUG, "Rewarded" + reward.getAmount() + " " + reward.getLabel() );
            MoPubRewardedVideoManager.onRewardedVideoCompleted( this.getClass(), getAdNetworkId(), reward );
        }

        MoPubRewardedVideoManager.onRewardedVideoClosed( this.getClass(), getAdNetworkId() );
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video clicked" );
        MoPubRewardedVideoManager.onRewardedVideoClicked( this.getClass(), getAdNetworkId() );
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video playback began" );
    }

    @Override
    public void videoPlaybackEnded(final AppLovinAd ad, final double percentViewed, final boolean fullyWatched)
    {
        log( DEBUG, "Rewarded video playback ended at playback percent: " + percentViewed );

        this.fullyWatched = fullyWatched;
    }

    //
    // Reward Listener
    //

    @Override
    public void userOverQuota(final AppLovinAd appLovinAd, final Map map)
    {
        log( ERROR, "Rewarded video validation request for ad did exceed quota with response: " + map );
    }

    @Override
    public void validationRequestFailed(final AppLovinAd appLovinAd, final int errorCode)
    {
        log( ERROR, "Rewarded video validation request for ad failed with error code: " + errorCode );
    }

    @Override
    public void userRewardRejected(final AppLovinAd appLovinAd, final Map map)
    {
        log( ERROR, "Rewarded video validation request was rejected with response: " + map );
    }

    @Override
    public void userDeclinedToViewAd(final AppLovinAd appLovinAd)
    {
        log( DEBUG, "User declined to view rewarded video" );
        MoPubRewardedVideoManager.onRewardedVideoClosed( this.getClass(), getAdNetworkId() );
    }

    @Override
    public void userRewardVerified(final AppLovinAd appLovinAd, final Map map)
    {
        final String currency = (String) map.get( "currency" );
        final int amount = (int) Double.parseDouble( (String) map.get( "amount" ) ); // AppLovin returns amount as double

        log( DEBUG, "Verified " + amount + " " + currency );

        reward = MoPubReward.success( currency, amount );
    }

    //
    // Dynamically create an instance of AppLovinIncentivizedInterstitial with a given zone without breaking backwards compatibility for publishers on older SDKs.
    //
    private AppLovinIncentivizedInterstitial createIncentivizedInterstitialForZoneId(final String zoneId, final AppLovinSdk sdk)
    {
        AppLovinIncentivizedInterstitial incent = null;

        try
        {
            final Method method = AppLovinIncentivizedInterstitial.class.getMethod( "create", String.class, AppLovinSdk.class );
            incent = (AppLovinIncentivizedInterstitial) method.invoke( null, zoneId, sdk );
        }
        catch ( Throwable th )
        {
            log( ERROR, "Unable to load ad for zone: " + zoneId + "..." );
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure( getClass(), getAdNetworkId(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR );
        }

        return incent;
    }

    //
    // Utility Methods
    //

    private static void log(final int priority, final String message)
    {
        if ( priority == DEBUG )
        {
            MoPubLog.d( "AppLovinRewardedVideo: " + message );
        }
        else
        {
            MoPubLog.e( "AppLovinRewardedVideo: " + message );
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
            return MoPubErrorCode.UNSPECIFIED;
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
