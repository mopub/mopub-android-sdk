package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK rewarded video adapter for MoPub.
 * <p>
 * Created by Thomas So on 5/27/17.
 *
 * @version 2.0
 */

// Please note: We have renamed this class from "AppLovinRewardedAdapter" to "AppLovinCustomEventRewardedVideo".
// If this is your first time integrating, please use "YOUR_PACKAGE_NAME.AppLovinCustomEventRewardedVideo" as the custom event classname in the MoPub dashboard.
// If you have integrated this before, please rename this class back to "AppLovinRewardedAdapter" and use "YOUR_PACKAGE_NAME.AppLovinRewardedAdapter" as the custom event classname in the MoPub dashboard.
public class AppLovinCustomEventRewardedVideo
        extends CustomEventRewardedVideo
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener
{
    private static final boolean LOGGING_ENABLED = true;

    private static boolean initialized;

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
            AppLovinSdk.getInstance( activity ).setPluginVersion( "MoPub-2.0" );

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

        if ( hasVideoAvailable() )
        {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess( AppLovinCustomEventRewardedVideo.class, "" );
        }
        else
        {
            getIncentivizedInterstitial().preload( this );
        }
    }

    @Override
    protected void showVideo()
    {
        if ( hasVideoAvailable() )
        {
            fullyWatched = false;
            reward = null;

            getIncentivizedInterstitial().show( parentActivity, null, this, this, this, this );
        }
        else
        {
            log( ERROR, "Failed to show an AppLovin rewarded video before one was loaded" );
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError( AppLovinCustomEventRewardedVideo.class, "", MoPubErrorCode.VIDEO_PLAYBACK_ERROR );
        }
    }

    @Override
    protected boolean hasVideoAvailable()
    {
        return getIncentivizedInterstitial().isAdReadyToDisplay();
    }

    @Override
    @Nullable
    protected LifecycleListener getLifecycleListener() { return null; }

    @Override
    @NonNull
    protected String getAdNetworkId() { return ""; }

    @Override
    protected void onInvalidate() {}

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video did load ad: " + ad.getAdIdNumber() );
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess( AppLovinCustomEventRewardedVideo.class, "" );
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        log( DEBUG, "Rewarded video failed to load with error: " + errorCode );
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure( AppLovinCustomEventRewardedVideo.class, "", toMoPubErrorCode( errorCode ) );
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video displayed" );
        MoPubRewardedVideoManager.onRewardedVideoStarted( AppLovinCustomEventRewardedVideo.class, "" );
    }

    @Override
    public void adHidden(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video dismissed" );

        if ( fullyWatched && reward != null )
        {
            log( DEBUG, "Rewarded" + reward.getAmount() + " " + reward.getLabel() );
            MoPubRewardedVideoManager.onRewardedVideoCompleted( AppLovinCustomEventRewardedVideo.class, "", reward );
        }

        MoPubRewardedVideoManager.onRewardedVideoClosed( AppLovinCustomEventRewardedVideo.class, "" );
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd ad)
    {
        log( DEBUG, "Rewarded video clicked" );
        MoPubRewardedVideoManager.onRewardedVideoClicked( AppLovinCustomEventRewardedVideo.class, "" );
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
        MoPubRewardedVideoManager.onRewardedVideoClosed( this.getClass(), "" );
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
    // Incentivized Ad Getter
    //

    private AppLovinIncentivizedInterstitial getIncentivizedInterstitial()
    {
        if ( incentivizedInterstitial == null )
        {
            incentivizedInterstitial = AppLovinIncentivizedInterstitial.create( parentActivity );
        }

        return incentivizedInterstitial;
    }

    //
    // Utility Methods
    //

    private static void log(final int priority, final String message)
    {
        if ( LOGGING_ENABLED )
        {
            Log.println( priority, "AppLovinRewardedVideo", message );
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
