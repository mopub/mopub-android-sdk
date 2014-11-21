package com.mopub.simpleadsdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mopub.common.logging.MoPubLog;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.COLUMN_AD_TYPE;
import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.COLUMN_AD_UNIT_ID;
import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.COLUMN_DESCRIPTION;
import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.COLUMN_ID;
import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.COLUMN_USER_GENERATED;
import static com.mopub.simpleadsdemo.MoPubSQLiteHelper.TABLE_AD_CONFIGURATIONS;
import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType;

class AdUnitDataSource {
    private MoPubSQLiteHelper mDatabaseHelper;
    private String[] mAllColumns = {
            COLUMN_ID,
            COLUMN_AD_UNIT_ID,
            COLUMN_DESCRIPTION,
            COLUMN_USER_GENERATED,
            COLUMN_AD_TYPE
    };

    AdUnitDataSource(final Context context) {
        mDatabaseHelper = new MoPubSQLiteHelper(context);
    }

    MoPubSampleAdUnit createSampleAdUnit(final MoPubSampleAdUnit sampleAdUnit) {
        final ContentValues values = new ContentValues();
        values.put(COLUMN_AD_UNIT_ID, sampleAdUnit.getAdUnitId());
        values.put(COLUMN_DESCRIPTION, sampleAdUnit.getDescription());
        values.put(COLUMN_USER_GENERATED, 1);
        values.put(COLUMN_AD_TYPE, sampleAdUnit.getFragmentClassName());
        final SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        final long insertId = database.insert(TABLE_AD_CONFIGURATIONS, null, values);
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS, mAllColumns,
                COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        final MoPubSampleAdUnit newAdConfiguration = cursorToAdConfiguration(cursor);
        cursor.close();
        database.close();
        return newAdConfiguration;
    }

    void deleteSampleAdUnit(final MoPubSampleAdUnit adConfiguration) {
        final long id = adConfiguration.getId();
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.delete(TABLE_AD_CONFIGURATIONS, COLUMN_ID + " = " + id, null);
        MoPubLog.d("Ad Configuration deleted with id: " + id);
        database.close();
    }

    List<MoPubSampleAdUnit> getAllAdUnits() {
        final List<MoPubSampleAdUnit> adConfigurations =
                new ArrayList<MoPubSampleAdUnit>();
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        final Cursor cursor = database.query(TABLE_AD_CONFIGURATIONS,
                mAllColumns, null, null, null, null, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            final MoPubSampleAdUnit adConfiguration = cursorToAdConfiguration(cursor);
            adConfigurations.add(adConfiguration);
            cursor.moveToNext();
        }

        cursor.close();
        database.close();
        return adConfigurations;
    }

    private MoPubSampleAdUnit cursorToAdConfiguration(final Cursor cursor) {
        final long id = cursor.getLong(0);
        final String adUnitId = cursor.getString(1);
        final String description = cursor.getString(2);
        final int userGenerated = cursor.getInt(3);
        final AdType adType = AdType.fromFragmentClassName(cursor.getString(4));

        if (adType == null) {
            return null;
        }

        return new MoPubSampleAdUnit.Builder(adUnitId, adType)
                .description(description)
                .isUserDefined(userGenerated == 1)
                .id(id)
                .build();
    }
}
