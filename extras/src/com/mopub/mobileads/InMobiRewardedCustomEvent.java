package com.inmobi.showcase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiAdRequestStatus.StatusCode;
import com.inmobi.ads.InMobiInterstitial.InterstitialAdListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.Education;
import com.inmobi.sdk.InMobiSdk.Ethnicity;
import com.inmobi.sdk.InMobiSdk.Gender;
import com.inmobi.sdk.InMobiSdk.LogLevel;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

public class InMobiRewardedCustomEvent extends CustomEventRewardedVideo implements
InterstitialAdListener {

	boolean isInitialized=false;
	private InMobiInterstitial inmobiInterstitial;
	private JSONObject serverParams;
	private String accountId="";
	private long placementId=-1;

	@Override
	protected boolean checkAndInitializeSdk(@NonNull Activity arg0,
			@NonNull Map<String, Object> arg1, @NonNull Map<String, String> arg2)
					throws Exception {
		// TODO Auto-generated method stub
		if(isInitialized){
			return false;
		}
		try {
		    serverParams = new JSONObject(arg2);
		    accountId = serverParams.getString("accountid");
		    placementId = serverParams.getLong("placementid");
		} catch (Exception e) {
		    Log.e("InMobiInterstitialCustomEvent", "Could not parse server parameters");
			e.printStackTrace();
		}

		InMobiSdk.init(arg0,accountId);
		Log.v("InMobiMopubRewardedCustomEvent","SDK Initialized");
		isInitialized = true;
		return true;

	}
	@Override
	@NonNull
	protected String getAdNetworkId() {
		// TODO Auto-generated method stub
		return String.valueOf(placementId);
	}

	@Override
	@Nullable
	protected LifecycleListener getLifecycleListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Nullable
	protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean hasVideoAvailable() {
		// TODO Auto-generated method stub
		return inmobiInterstitial!=null && inmobiInterstitial.isReady();
	}

	@Override
	protected void loadWithSdkInitialized(@NonNull Activity arg0,
			@NonNull Map<String, Object> arg1, @NonNull Map<String, String> arg2)
					throws Exception {
		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Calling rewarded video");
		
		
		inmobiInterstitial = new InMobiInterstitial(arg0,placementId,this);
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
		Map<String, String> map = new HashMap<String, String>();
		map.put("tp", "c_mopub");
		map.put("tp-ver", MoPub.SDK_VERSION);
		inmobiInterstitial.setExtras(map);
		inmobiInterstitial.load();
	}

	@Override
	protected void onInvalidate() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void showVideo() {
		// TODO Auto-generated method stub
		if(this.hasVideoAvailable()){
			inmobiInterstitial.show();
		}
		else{
			MoPubRewardedVideoManager.onRewardedVideoPlaybackError(InMobiRewardedCustomEvent.class, String.valueOf(placementId), MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
		}
	}

	@Override
	public void onAdDismissed(InMobiInterstitial arg0) {
		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Ad dismissed");
		MoPubRewardedVideoManager.onRewardedVideoClosed(InMobiRewardedCustomEvent.class, String.valueOf(placementId));
	}

	@Override
	public void onAdDisplayed(InMobiInterstitial arg0) {
		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Ad displayed");
		MoPubRewardedVideoManager.onRewardedVideoStarted(InMobiRewardedCustomEvent.class, String.valueOf(placementId));
	}

	@Override
	public void onAdInteraction(InMobiInterstitial arg0,
			Map<Object, Object> arg1) {
		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Ad interaction");
		MoPubRewardedVideoManager.onRewardedVideoClicked(InMobiRewardedCustomEvent.class, String.valueOf(placementId));
	}

	@Override
	public void onAdLoadFailed(InMobiInterstitial arg0,
			InMobiAdRequestStatus arg1) {

		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Ad failed to load:"+arg1.getStatusCode().toString());
		if (arg1.getStatusCode() == StatusCode.INTERNAL_ERROR) {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.INTERNAL_ERROR);
		} else if (arg1.getStatusCode() == StatusCode.REQUEST_INVALID) {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
		} else if (arg1.getStatusCode() == StatusCode.NETWORK_UNREACHABLE) {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.NETWORK_INVALID_STATE);
		} else if (arg1.getStatusCode() == StatusCode.NO_FILL) {
			Log.v("InMobiMopubRewardedCustomEvent","No Fill");
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class,"1445550471285", MoPubErrorCode.NO_FILL);
		} else if (arg1.getStatusCode() == StatusCode.REQUEST_TIMED_OUT) {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.NETWORK_TIMEOUT);
		} else if (arg1.getStatusCode() == StatusCode.SERVER_ERROR) {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.SERVER_ERROR);
		} else {
			MoPubRewardedVideoManager
			.onRewardedVideoLoadFailure(InMobiRewardedCustomEvent.class, "1445550471285", MoPubErrorCode.UNSPECIFIED);
		}
		Log.v("InMobiMopubRewardedCustomEvent","callback executed");
	}

	@Override
	public void onAdLoadSucceeded(InMobiInterstitial arg0) {
		Log.v("InMobiMopubRewardedCustomEvent","Ad load succeeded");
		if(arg0!=null){
			MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(InMobiRewardedCustomEvent.class, String.valueOf(placementId));
		}
	}

	@Override
	public void onAdRewardActionCompleted(InMobiInterstitial arg0,
			Map<Object, Object> arg1) {
		// TODO Auto-generated method stub
		Log.v("InMobiMopubRewardedCustomEvent","Rewards");
		Iterator<Object> iterator = arg1.keySet().iterator(); 
		String key="",value="";
		while (iterator.hasNext()) {  
			key = iterator.next().toString();  
			value = arg1.get(key).toString();  
			Log.d("Rewards: ", key+":"+value);
		}
		MoPubRewardedVideoManager.onRewardedVideoCompleted(InMobiRewardedCustomEvent.class, null, MoPubReward.success(key, Integer.parseInt(value)));
	}

	@Override
	public void onUserLeftApplication(InMobiInterstitial arg0) {
		Log.v("InMobiMopubRewardedCustomEvent","User left application");
	}

}
