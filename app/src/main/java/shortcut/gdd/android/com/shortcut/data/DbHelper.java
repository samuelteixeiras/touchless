package shortcut.gdd.android.com.shortcut.data;

/**
 * Created by Samuel PC on 16/03/2015.
 */


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import shortcut.gdd.android.com.shortcut.data.QeaContract.QEAEntry;

/**
 * Manages a local database for weather data.
 */
public class DbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "Qea.db";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_Qea_TABLE = "CREATE TABLE " + QEAEntry.TABLE_NAME + " (" +
                QEAEntry._ID + " INTEGER PRIMARY KEY," +
                QEAEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                QEAEntry.COLUMN_QUESTION + " TEXT NOT NULL, " +
                QEAEntry.COLUMN_ANSWER + " TEXT NOT NULL " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_Qea_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QEAEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
