package com.mopub.simpleadsdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType.BANNER;
import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType.CUSTOM_NATIVE;
import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType.INTERSTITIAL;
import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType.LIST_VIEW;
import static com.mopub.simpleadsdemo.MoPubSampleAdUnit.AdType.MRECT;

class MoPubSQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_AD_CONFIGURATIONS = "adConfigurations";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_AD_UNIT_ID = "adUnitId";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_USER_GENERATED = "userGenerated";
    public static final String COLUMN_AD_TYPE = "adType";

    private static final String DATABASE_NAME = "savedConfigurations.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table " + TABLE_AD_CONFIGURATIONS
            + " ("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_AD_UNIT_ID + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_USER_GENERATED + " integer not null, "
            + COLUMN_AD_TYPE + " text not null"
            + ");";

    private final Context mContext;

    public MoPubSQLiteHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Built-in sample ad units.
        final List<MoPubSampleAdUnit> adUnitList = new ArrayList<MoPubSampleAdUnit>();
        adUnitList.add(
                new MoPubSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_banner), BANNER)
                        .description("MoPub Banner Sample")
                        .build());

        adUnitList.add(
                new MoPubSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_mrect), MRECT)
                        .description("MoPub Mrect Sample")
                        .build());

        adUnitList.add(
                new MoPubSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_interstitial), INTERSTITIAL)
                        .description("MoPub Interstitial Sample")
                        .build());

        adUnitList.add(
                new MoPubSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_native), LIST_VIEW)
                        .description("MoPub Ad Placer Sample")
                        .build());
        adUnitList.add(
                new MoPubSampleAdUnit
                        .Builder(mContext.getString(R.string.ad_unit_id_native), CUSTOM_NATIVE)
                        .description("MoPub View Pager Sample")
                        .build());
        sqLiteDatabase.execSQL(DATABASE_CREATE);
        sqLiteDatabase.beginTransaction();

        // Populate the MoPub built-in samples.
        for (final MoPubSampleAdUnit adUnit : adUnitList) {
            final ContentValues values = new ContentValues();
            values.put(COLUMN_AD_UNIT_ID, adUnit.getAdUnitId());
            values.put(COLUMN_DESCRIPTION, adUnit.getDescription());
            values.put(COLUMN_USER_GENERATED, 0);
            values.put(COLUMN_AD_TYPE, adUnit.getFragmentClassName());

            sqLiteDatabase.insert(TABLE_AD_CONFIGURATIONS, null, values);
        }
        sqLiteDatabase.setTransactionSuccessful();
        sqLiteDatabase.endTransaction();
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(MoPubSQLiteHelper.class.getName(),
                "Downgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data"
        );
        recreateDb(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(MoPubSQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data"
        );
        recreateDb(database);
    }

    private void recreateDb(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_AD_CONFIGURATIONS);
        onCreate(database);
    }
}
