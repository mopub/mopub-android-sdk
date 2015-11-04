package com.inmobi.showcase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiAdRequestStatus.StatusCode;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiInterstitial.InterstitialAdListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.InMobiSdk.Education;
import com.inmobi.sdk.InMobiSdk.Ethnicity;
import com.inmobi.sdk.InMobiSdk.Gender;
import com.inmobi.sdk.InMobiSdk.LogLevel;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoListener;

/*
 * Tested with InMobi SDK  5.0.0
 */
public class InMobiInterstitialCustomEvent extends CustomEventInterstitial implements InterstitialAdListener {
	private CustomEventInterstitialListener mInterstitialListener;
	private JSONObject serverParams;
	private String accountId="";
	private long placementId=-1;
	@Override
	protected void loadInterstitial(Context context,
			CustomEventInterstitialListener interstitialListener,
			Map<String, Object> localExtras, Map<String, String> serverExtras) {
		Log.v("InMobiInterstitialCustomEvent","Reached Interstitial adapter");
		mInterstitialListener = interstitialListener;
		try {
		    serverParams = new JSONObject(serverExtras);
		} catch (Exception e) {
		    Log.e("InMobiInterstitialCustomEvent", "Could not parse server parameters");
			e.printStackTrace();
		}
		Activity activity = null;
		if (context instanceof Activity) {
			activity = (Activity) context;
		} else {
		}

		if (activity == null) {
			mInterstitialListener
					.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
			return;
		}

		try {
			accountId = serverParams.getString("accountid");
			placementId = serverParams.getLong("placementid");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (!isAppIntialize) {
			InMobiSdk.init(activity,accountId);
			isAppIntialize = true;
		}
		
		/*
		 * You may also pass the Placement ID by
		 * specifying Custom Event Data in MoPub's web interface.
		 */
		
		
		iMInterstitial = new InMobiInterstitial(activity, placementId, this);
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
		iMInterstitial.setExtras(map);
		iMInterstitial.load();
	}
	
	private InMobiInterstitial iMInterstitial;
	private static boolean isAppIntialize = false;

	/*
	 * Abstract methods from CustomEventInterstitial
	 */

	@Override
	public void showInterstitial() {
		if (iMInterstitial != null
				&& iMInterstitial.isReady()) {
			iMInterstitial.show();
		}
	}

	@Override
	public void onInvalidate() {
	}

	@Override
	public void onAdDismissed(InMobiInterstitial arg0) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial ad dismissed.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialDismissed();
		}
	}

	@Override
	public void onAdDisplayed(InMobiInterstitial arg0) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial show on screen.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialShown();
		}
	}

	@Override
	public void onAdLoadFailed(InMobiInterstitial arg0,
			InMobiAdRequestStatus arg1) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial ad failed to load.");
		if (mInterstitialListener != null) {

			if (arg1.getStatusCode() == StatusCode.INTERNAL_ERROR) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
			} else if (arg1.getStatusCode() == StatusCode.REQUEST_INVALID) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			} else if (arg1.getStatusCode() == StatusCode.NETWORK_UNREACHABLE) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
			} else if (arg1.getStatusCode() == StatusCode.NO_FILL) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.NO_FILL);
			} else if (arg1.getStatusCode() == StatusCode.REQUEST_TIMED_OUT) {
				mInterstitialListener
				.onInterstitialFailed(MoPubErrorCode.NETWORK_TIMEOUT);
			} else if (arg1.getStatusCode() == StatusCode.SERVER_ERROR) {
				mInterstitialListener
				.onInterstitialFailed(MoPubErrorCode.SERVER_ERROR);
			} else {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
			}
		}
		
	}

	@Override
	public void onAdLoadSucceeded(InMobiInterstitial arg0) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial ad loaded successfully.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialLoaded();
		}
	}

	@Override
	public void onAdRewardActionCompleted(InMobiInterstitial arg0,
			Map<Object, Object> arg1) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial onRewardActionCompleted.");	
		
		Iterator<Object> iterator = arg1.keySet().iterator(); 
		while (iterator.hasNext()) {  
		   String key = iterator.next().toString();  
		   String value = arg1.get(key).toString();  
		   Log.d("Rewards: ", key+":"+value);
		}
	}

	@Override
	public void onUserLeftApplication(InMobiInterstitial arg0) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial ad leaving application.");
	}

	@Override
	public void onAdInteraction(InMobiInterstitial arg0,
			Map<Object, Object> arg1) {
		Log.d("InMobiInterstitialCustomEvent", "InMobi interstitial interaction happening.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialClicked();
		}		
	}
}
