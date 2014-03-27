/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.Date;

/*
 * This class enables the MoPub SDK to deliver targeted ads from Facebook via MoPub Marketplace
 * (MoPub's real-time bidding ad exchange) as part of a test program. This class sends an identifier
 * to Facebook so Facebook can select the ad MoPub will serve in your app through MoPub Marketplace.
 * If this class is removed from the SDK, your application will not receive targeted ads from
 * Facebook.
 */

public class FacebookKeywordProvider {
    private static final Uri ID_URL = Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ID_COLUMN_NAME = "aid";
    private static final String ID_PREFIX = "FBATTRID:";

    private static final long QUIET_PERIOD = 1000L * 60L;

    private static String cachedId;
    private static long lastQueryTime;

    public static String getKeyword(Context context) {
        if (cachedId != null) return ID_PREFIX + cachedId;

        if (new Date().getTime() - lastQueryTime < QUIET_PERIOD) return null;

        lastQueryTime = new Date().getTime();

        Cursor cursor = null;

        try {
            String projection[] = {ID_COLUMN_NAME};
            cursor = context.getContentResolver().query(ID_URL, projection, null, null, null);
            
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            
            String attributionId = cursor.getString(cursor.getColumnIndex(ID_COLUMN_NAME));
            
            if (attributionId == null || attributionId.length() == 0) {
                return null;
            }

            cachedId = attributionId;

            return ID_PREFIX + attributionId;
        } catch (Exception exception) {
            Log.d("MoPub", "Unable to retrieve FBATTRID: " + exception.toString());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
