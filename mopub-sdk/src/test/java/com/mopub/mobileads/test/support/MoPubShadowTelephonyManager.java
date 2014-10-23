package com.mopub.mobileads.test.support;

import android.telephony.TelephonyManager;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowTelephonyManager;

@Implements(TelephonyManager.class)
public class MoPubShadowTelephonyManager extends ShadowTelephonyManager {

    private boolean mNeedsReadPhoneState;
    private boolean mHasReadPhoneState;

    public MoPubShadowTelephonyManager() {
        mNeedsReadPhoneState = false;
        mHasReadPhoneState = false;
    }

    /**
     * Some Lenovo & other phones require READ_PHONE_STATE on getNetworkOperatorName().
     */
    public void setNeedsReadPhoneState(boolean needsReadPhoneState) {
        mNeedsReadPhoneState = needsReadPhoneState;
    }

    @Override
    public void setReadPhoneStatePermission(final boolean readPhoneStatePermission) {
        // Robolectric hides its checkReadPhoneState method so we need to hack our own.
        mHasReadPhoneState = readPhoneStatePermission;
        super.setReadPhoneStatePermission(readPhoneStatePermission);
    }

    @Override
    public String getNetworkOperatorName() {
        if (!mNeedsReadPhoneState || mHasReadPhoneState) {
            return super.getNetworkOperatorName();
        } else {
            throw new SecurityException("READ_PHONE_STATE is required.");
        }
    }
}
