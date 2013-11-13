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
import com.inmobi.commons.InMobi.LOG_LEVEL;
import com.inmobi.commons.MaritalStatus;
import com.inmobi.commons.SexualOrientation;
import com.inmobi.monetization.IMBanner;
import com.inmobi.monetization.IMBannerListener;
import com.inmobi.monetization.IMErrorCode;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import com.mopub.mobileads.MoPub;
import com.mopub.mobileads.MoPubErrorCode;

/*
 * Tested with InMobi SDK 4.0.2
 */
public class InMobiBanner extends CustomEventBanner implements IMBannerListener {

	@Override
	protected void loadBanner(Context context,
			CustomEventBannerListener bannerListener,
			Map<String, Object> localExtras, Map<String, String> serverExtras) {
		mBannerListener = bannerListener;
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
		if (!isAppIntialize) {
			InMobi.initialize(activity, "INMOBI_APP_ID");
			isAppIntialize = true;
		}

		/*
		 * You may also pass this String down in the serverExtras Map by
		 * specifying Custom Event Data in MoPub's web interface.
		 */
		iMBanner = new IMBanner(activity, "INMOBI_APP_ID",
				IMBanner.INMOBI_AD_UNIT_320X50);
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
			e.printStackTrace();
		}
		Map<String, String> map = new HashMap<String, String>();
		map.put("tp", "c_mopub");
		map.put("tp-ver", MoPub.SDK_VERSION);
		iMBanner.setRequestParams(map);
		iMBanner.setKeywords("keywords");
		iMBanner.setRefTagParam("key", "value");
		iMBanner.setIMBannerListener(this);
		// to call disable hardware acceleration
		// mInMobiBanner.disableHardwareAcceleration();
		iMBanner.setRefreshInterval(-1);
		iMBanner.loadBanner();

	}

	private CustomEventBannerListener mBannerListener;
	private IMBanner iMBanner;
	private static boolean isAppIntialize = false;

	/*
	 * Abstract methods from CustomEventBanner
	 */

	@Override
	public void onInvalidate() {
		if (iMBanner != null) {
			iMBanner.setIMBannerListener(null);
			iMBanner.destroy();
		}

	}

	@Override
	public void onBannerInteraction(IMBanner arg0, Map<String, String> arg1) {
		Log.d("MoPub", "InMobi banner interaction happening.");
		if (mBannerListener != null) {
			mBannerListener.onBannerClicked();
		}
	}

	@Override
	public void onBannerRequestFailed(IMBanner arg0, IMErrorCode arg1) {
		Log.d("MoPub", "InMobi banner ad failed to load.");
		if (mBannerListener != null) {
			if (arg1 == IMErrorCode.INTERNAL_ERROR) {
				mBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
			} else if (arg1 == IMErrorCode.INVALID_REQUEST) {
				mBannerListener
						.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
			} else if (arg1 == IMErrorCode.NETWORK_ERROR) {
				mBannerListener
						.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
			} else if (arg1 == IMErrorCode.NO_FILL) {
				mBannerListener.onBannerFailed(MoPubErrorCode.NO_FILL);
			} else {
				mBannerListener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
			}
		}
	}

	@Override
	public void onBannerRequestSucceeded(IMBanner arg0) {
		Log.d("MoPub", "InMobi banner ad loaded successfully.");
		if(mBannerListener!=null){
			if (iMBanner != null) {
				mBannerListener.onBannerLoaded(iMBanner);
			} else {
				mBannerListener
					.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
			}
		}
	}

	@Override
	public void onDismissBannerScreen(IMBanner arg0) {
		Log.d("MoPub", "InMobi banner ad screen dismissed");
		if (mBannerListener != null) {
			mBannerListener.onBannerCollapsed();
		}
	}

	@Override
	public void onLeaveApplication(IMBanner arg0) {
		Log.d("MoPub", "InMobi banner ad leaving application.");
	}

	@Override
	public void onShowBannerScreen(IMBanner arg0) {
		Log.d("MoPub", "InMobi banner ad modal shown.");
		if (mBannerListener != null) {
			mBannerListener.onBannerExpanded();
		}
	}

}
