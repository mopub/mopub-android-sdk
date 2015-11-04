package com.inmobi.showcase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiAdRequestStatus.StatusCode;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiBanner.AnimationType;
import com.inmobi.ads.InMobiBanner.BannerAdListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.Education;
import com.inmobi.sdk.InMobiSdk.Ethnicity;
import com.inmobi.sdk.InMobiSdk.Gender;
import com.inmobi.sdk.InMobiSdk.LogLevel;
import com.mopub.common.MoPub;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.MoPubErrorCode;

/*
 * Tested with InMobi SDK 5.0.0
 */
public class InMobiBannerCustomEvent extends CustomEventBanner implements BannerAdListener  {


    private CustomEventBannerListener mBannerListener;
    private InMobiBanner imbanner;
    private static boolean isAppIntialize = false;
    private JSONObject serverParams;
    private String accountId="";
    private long placementId=-1;

    @Override
    public void onAdDismissed(InMobiBanner arg0) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","Ad Dismissed");
    }

    @Override
    public void onAdDisplayed(InMobiBanner arg0) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","Ad displayed");
    }

    @Override
    public void onAdInteraction(InMobiBanner arg0, Map<Object, Object> arg1) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","Ad interaction");
    }

    @Override
    public void onAdLoadFailed(InMobiBanner arg0, InMobiAdRequestStatus arg1) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","Ad failed to load");

    }

    @Override
    public void onAdLoadSucceeded(InMobiBanner arg0) {
        Log.d("InMobiBannerCustomEvent", "InMobi banner ad loaded successfully.");
        if(mBannerListener!=null){
            if (arg0 != null) {
                mBannerListener.onBannerLoaded(arg0);
            } else {
                mBannerListener
                        .onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            }
        }
    }

    @Override
    public void onAdRewardActionCompleted(InMobiBanner arg0, Map<Object, Object> arg1) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","Ad rewarded");
    }

    @Override
    public void onUserLeftApplication(InMobiBanner arg0) {
        // TODO Auto-generated method stub
    	Log.v("InMobiBannerCustomEvent","User left applicaton");
    }

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener arg1, Map<String, Object> arg2,
                              Map<String, String> arg3) {
    	Log.v("InMobiBannerCustomEvent","Reached native adapter");
    	mBannerListener = arg1;
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            // You may also pass in an Activity Context in the localExtras map
            // and retrieve it here.
        }
        if (activity == null) {
            mBannerListener.onBannerFailed(null);
            return;
        }
        
		try {
			serverParams = new JSONObject(arg3);
			accountId = serverParams.getString("accountid");
			placementId = serverParams.getLong("placementid");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


        if (!isAppIntialize) {
            try {
                InMobiSdk.init(activity,accountId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isAppIntialize = true;
        }
		InMobiSdk.setAreaCode("areacode");
		InMobiSdk.setEducation(Education.HIGH_SCHOOL_OR_LESS);
		InMobiSdk.setGender(Gender.MALE);
		InMobiSdk.setIncome(1000);
		InMobiSdk.setAge(23);
		InMobiSdk.setPostalCode("postalcode");
		InMobiSdk.setLogLevel(LogLevel.DEBUG);
		InMobiSdk.setLocationWithCityStateCountry("blore", "kar", "india");
		InMobiSdk.setLanguage("ENG");
		InMobiSdk.setInterests("dance");
		InMobiSdk.setEthnicity(Ethnicity.ASIAN);
		InMobiSdk.setYearOfBirth(1980);

        imbanner = new InMobiBanner(activity, placementId);
        imbanner.setListener(this);
        imbanner.setEnableAutoRefresh(false);
        imbanner.setAnimationType(InMobiBanner.AnimationType.ANIMATION_OFF);

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(dm);
		Map<String, String> map = new HashMap<String, String>();
		map.put("tp", "c_mopub");
		map.put("tp-ver", MoPub.SDK_VERSION);
		imbanner.setExtras(map);

        imbanner.setLayoutParams(new LinearLayout.LayoutParams(Math.round(320*dm.density), Math.round(50*dm.density)));
        imbanner.load();
    }

    @Override
    protected void onInvalidate() {
        // TODO Auto-generated method stub

    }
}

