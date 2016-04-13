package net.beaconradar.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainDatabaseHelper extends SQLiteOpenHelper {
    private static MainDatabaseHelper mInstance = null;
    public static final String DATABASE_NAME = "main.db";
    public static final int DATABASE_VERSION = 1;
    public static final String CREATE_LOG_TABLE =
            "CREATE TABLE IF NOT EXISTS log_entries(" +
                "_id INTEGER, " +   //-1 for non-beacon events
                "time INTEGER NOT NULL, " +
                "event INTEGER NOT NULL, " +
                "rssi INTEGER, " +
                "id0 TEXT, " +
                "id1 TEXT, " +
                "id2 TEXT, " +
                "mac TEXT, " +
                "tx INTEGER, " +
                "other TEXT " +
            ")";
    public static final String CREATE_LOG_TABLE_EXPORT =
            "CREATE TABLE IF NOT EXISTS log.log_entries(" +
                    "_id INTEGER, " +   //-1 for non-beacon events
                    "time INTEGER NOT NULL, " +
                    "event INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "color TEXT, " +
                    "icon INTEGER, " +
                    "rssi INTEGER, " +
                    "type INTEGER, " +
                    "eq_mode INTEGER, " +
                    "id0 TEXT, " +
                    "id1 TEXT, " +
                    "id2 TEXT, " +
                    "mac TEXT, " +
                    "tx INTEGER, " +
                    "other TEXT " +
                    ")";

    public MainDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public MainDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    //Not needed. Injected.
    //Actually, became needed when using cursor loader. TODO Workaround it with injection
    public static synchronized MainDatabaseHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new MainDatabaseHelper(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS known(" +
                "_id INTEGER PRIMARY KEY, " +
                //Timing
                "discovered INTEGER NOT NULL, " +   //First spotting and db insert
                "spotted INTEGER NOT NULL, " +      //Set on every reentry
                "last_seen INTEGER NOT NULL, " +    //Most recent seen time
                //Identification
                "type INTEGER NOT NULL, " +         //Index
                "eq_mode INTEGER NOT NULL, " +      //Index
                "id0 TEXT, " +
                "id1 TEXT, " +
                "id2 TEXT, " +
                "mac TEXT NOT NULL, " +
                //Additional
                "tx INTEGER NOT NULL, " +           //-127 for TLM and beacons without Tx power
                "other TEXT, " +                    //Pipe|separated|stuff
                //User assigned
                "color TEXT NOT NULL, " +
                "icon INTEGER NOT NULL, " +
                "name TEXT, " +
                "intent_on_appeared    INTEGER(1) NOT NULL DEFAULT 0, " +   //TODO string with actual intent instead of integer?
                "intent_on_visible     INTEGER(1) NOT NULL DEFAULT 0, " +
                "intent_on_disappeared INTEGER(1) NOT NULL DEFAULT 0, " +
                "user INTEGER(1) NOT NULL DEFAULT 0 " +       //1 = Do not delete on cleanup, has user intents / name / color / icon
            ")");
        //No id2 (lots of NULLs). id0, id1, and mac are updated, but only on beacon removal, so it should be ok.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_id ON known(_id, type, eq_mode, id0, id1, mac)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_discovered ON known(discovered)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_spotted ON known(spotted)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_user ON known(user)");
        db.execSQL(CREATE_LOG_TABLE);
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS nearby(" +
                "_id INTEGER PRIMARY KEY, " +
                //Timing
                "discovered INTEGER NOT NULL, " +   //First spotting and db insert
                "spotted INTEGER NOT NULL, " +      //Set on every reentry
                "last_seen INTEGER NOT NULL, " +    //Most recent seen time
                //Identification                    //Only changing parts are important, otherwise this serves no id purpose
                "type INTEGER NOT NULL, " +
                "eq_mode INTEGER NOT NULL, " +
                "id0 TEXT, " +
                "id1 TEXT, " +
                "id2 TEXT, " +
                "mac TEXT NOT NULL, " +
                //Additional
                "rssi INTEGER NOT NULL, " +
                "tx INTEGER NOT NULL, " +           //-127 for TLM and beacons without Tx power
                "other TEXT " +                    //Pipe|separated|stuff
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
