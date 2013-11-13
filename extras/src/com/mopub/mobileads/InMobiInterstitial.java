package com.mopub.custom;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.inmobi.commons.EducationType;
import com.inmobi.commons.EthnicityType;
import com.inmobi.commons.GenderType;
import com.inmobi.commons.HasChildren;
import com.inmobi.commons.InMobi;
import com.inmobi.commons.MaritalStatus;
import com.inmobi.commons.SexualOrientation;
import com.inmobi.commons.InMobi.LOG_LEVEL;
import com.inmobi.monetization.IMErrorCode;
import com.inmobi.monetization.IMInterstitial;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPub;
import com.mopub.mobileads.MoPubErrorCode;
import com.inmobi.monetization.IMInterstitialListener;

/*
 * Tested with InMobi SDK  4.0.2
 */
public class InMobiInterstitial extends CustomEventInterstitial implements
		IMInterstitialListener {
	private CustomEventInterstitialListener mInterstitialListener;

	@Override
	protected void loadInterstitial(Context context,
			CustomEventInterstitialListener interstitialListener,
			Map<String, Object> localExtras, Map<String, String> serverExtras) {
		mInterstitialListener = interstitialListener;
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

		/*
		 * You may also pass this String down in the serverExtras Map by
		 * specifying Custom Event Data in MoPub's web interface.
		 */
		if (!isAppIntialize) {
			InMobi.initialize(activity, "INMOBI_APP_ID");
			isAppIntialize = true;
		}
		this.iMInterstitial = new IMInterstitial(activity, "INMOBI_APP_ID");
		InMobi.setAreaCode("areacode");
		InMobi.setEducation(EducationType.HIGHSCHOOLORLESS);
		InMobi.setGender(GenderType.MALE);
		InMobi.setIncome(1000);
		InMobi.setAge(23);
		InMobi.setSexualOrientation(SexualOrientation.STRAIGHT);
		InMobi.setPostalCode("postalcode");
		InMobi.setMaritalStatus(MaritalStatus.SINGLE);
		InMobi.setLogLevel(LOG_LEVEL.DEBUG);
		InMobi.setLocationWithCityStateCountry("bangalore", "karnataka",
				"india");
		InMobi.setLocationInquiryAllowed(true);
		InMobi.setLanguage("ENGLISH");
		InMobi.setInterests("dance");
		InMobi.setHasChildren(HasChildren.FALSE);
		InMobi.setEthnicity(EthnicityType.ASIAN);
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(format.parse("11-03-2013"));
			InMobi.setDateOfBirth(cal);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, String> map = new HashMap<String, String>();
		map.put("tp", "c_mopub");
		map.put("tp-ver", MoPub.SDK_VERSION);
		iMInterstitial.setRequestParams(map);
		iMInterstitial.setKeywords("keywords");
		iMInterstitial.setIMInterstitialListener(this);
		// to call disable hardware acceleration
		// mInMobiInterstitial.disableHardwareAcceleration();
		iMInterstitial.loadInterstitial();

	}

	// private CustomEventInterstitial mInterstitialListener;
	private IMInterstitial iMInterstitial;
	private static boolean isAppIntialize = false;

	/*
	 * Abstract methods from CustomEventInterstitial
	 */

	@Override
	public void showInterstitial() {
		if (iMInterstitial != null
				&& IMInterstitial.State.READY.equals(this.iMInterstitial
						.getState())) {
			iMInterstitial.show();
		}
	}

	@Override
	public void onInvalidate() {
		if (iMInterstitial != null) {
			iMInterstitial.setIMInterstitialListener(null);
			iMInterstitial.destroy();
		}
	}

	@Override
	public void onDismissInterstitialScreen(IMInterstitial arg0) {
		Log.d("MoPub", "InMobi interstitial ad dismissed.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialDismissed();
		}
	}

	@Override
	public void onInterstitialFailed(IMInterstitial arg0, IMErrorCode arg1) {
		Log.d("MoPub", "InMobi interstitial ad failed to load.");
		if (mInterstitialListener != null) {

			if (arg1 == IMErrorCode.INTERNAL_ERROR) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
			} else if (arg1 == IMErrorCode.INVALID_REQUEST) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			} else if (arg1 == IMErrorCode.NETWORK_ERROR) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
			} else if (arg1 == IMErrorCode.NO_FILL) {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.NO_FILL);
			} else {
				mInterstitialListener
						.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
			}
		}
	}

	@Override
	public void onInterstitialInteraction(IMInterstitial arg0,
			Map<String, String> arg1) {
		Log.d("MoPub", "InMobi interstitial interaction happening.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialClicked();
		}
	}

	@Override
	public void onInterstitialLoaded(IMInterstitial arg0) {
		Log.d("MoPub", "InMobi interstitial ad loaded successfully.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialLoaded();
		}
	}

	@Override
	public void onLeaveApplication(IMInterstitial arg0) {
		Log.d("MoPub", "InMobi interstitial ad leaving application.");
	}

	@Override
	public void onShowInterstitialScreen(IMInterstitial arg0) {
		Log.d("MoPub", "InMobi interstitial show on screen.");
		if (mInterstitialListener != null) {
			mInterstitialListener.onInterstitialShown();
		}
	}
}
