package com.mopub.simpleadsdemo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class MoPubSQLiteHelper extends SQLiteOpenHelper {
    public static final String TABLE_AD_CONFIGURATIONS = "adConfigurations";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_AD_UNIT_ID = "adUnitId";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_USER_GENERATED = "userGenerated";
    public static final String COLUMN_AD_TYPE = "adType";

    private static final String DATABASE_NAME = "savedConfigurations.db";
    private static final int DATABASE_VERSION = 3;

    private static final String DATABASE_CREATE = "create table " + TABLE_AD_CONFIGURATIONS
            + " ("
            + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_AD_UNIT_ID + " text not null, "
            + COLUMN_DESCRIPTION + " text not null, "
            + COLUMN_USER_GENERATED + " integer not null, "
            + COLUMN_AD_TYPE + " text not null"
            + ");";

    public MoPubSQLiteHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
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
