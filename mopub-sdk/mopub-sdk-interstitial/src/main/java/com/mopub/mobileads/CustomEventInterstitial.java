package com.mopub.mobileads;

import android.content.Context;

import java.util.Map;

/*
 * CustomEventInterstitial is a base class for custom events that support interstitials. By
 * implementing subclasses of CustomEventInterstitial, you can enable the MoPub SDK to natively
 * support a wider variety of third-party ad networks, or execute any of your application code on
 * demand.
 * 
 * At runtime, the MoPub SDK will find and instantiate a CustomEventInterstitial subclass as needed
 * and invoke its loadInterstitial() method.
 */
public abstract class CustomEventInterstitial implements Interstitial {
    
    /*
     * When the MoPub SDK receives a response indicating it should load a custom event, it will send
     * this message to your custom event class. Your implementation of this method can either load
     * an interstitial ad from a third-party ad network, or execute any application code.
     * It must also notify the provided CustomEventInterstitial.Listener Object of certain lifecycle
     * events.
     * 
     * The localExtras parameter is a Map containing additional custom data that is set within
     * your application by calling MoPubInterstitial.setLocalExtras(Map<String, Object>). Note that
     * the localExtras Map is a copy of the Map supplied to setLocalExtras().
     * 
     * The serverExtras parameter is a Map containing additional custom data configurable on the
     * MoPub website that you want to associate with a given custom event request. This data may be
     * used to pass dynamic information, such as publisher IDs, without changes in application code.
     */
    protected abstract void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras);
    
    /*
     * Display the interstitial ad.
     */
    protected abstract void showInterstitial();
    
    /*
     * Called when a Custom Event is being invalidated or destroyed. Perform any final cleanup here.
     */
    protected abstract void onInvalidate();
    
    public interface CustomEventInterstitialListener {
        /*
         * Your custom event subclass must call this method when it successfully loads an ad.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        void onInterstitialLoaded();
        
        /*
         * Your custom event subclass must call this method when it fails to load an ad.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        void onInterstitialFailed(MoPubErrorCode errorCode);
        
        /*
         * Your custom event subclass should call this method when the interstitial ad is displayed.
         * This method is optional. However, if you call this method, you should ensure that
         * onInterstitialDismissed is called at a later time.
         */
        void onInterstitialShown();
        
        /*
         * Your custom event subclass should call this method when a user taps on an interstitial
         * ad. This method is optional.
         */
        void onInterstitialClicked();
        
        /*
         * This is an alias for onInterstitialClicked().
         * Your custom event subclass should call this method if the ad will cause the user to leave
         * the application (e.g. for the Play Store or browser). This method is optional.
         */
        void onLeaveApplication();
        
        /*
         * Your custom event subclass should call this method when the interstitial ad is closed.
         * This method is optional.
         */
        void onInterstitialDismissed();
    }
}
