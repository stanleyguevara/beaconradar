package net.beaconradar.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class MainContentProvider extends ContentProvider {
    //@Inject
    protected MainDatabaseHelper mDatabase;
    public static final String AUTHORITY = "net.beaconradar.maincontentprovider";
    private static final String BASE_PATH = "main";
    private static final int RAW_QUERY = 77;
    private long start;

    /*private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, RAW_QUERY);
    }*/

    @Override
    public boolean onCreate() {
        mDatabase = MainDatabaseHelper.getInstance(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        start = System.currentTimeMillis();
        Cursor cursor;
        String table = uri.getLastPathSegment();
        if("RAW_QUERY".equals(table)) cursor = mDatabase.getWritableDatabase().rawQuery(selection, selectionArgs);
        else cursor = mDatabase.getReadableDatabase().query(table, projection, selection, selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        //Log.v("MainContentProvider", "Query time: " + String.valueOf(System.currentTimeMillis() - start));
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = uri.getLastPathSegment();
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        long id = 0;
        id = db.insert(table, null, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table = uri.getLastPathSegment();
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        int rowsDeleted = 0;
        rowsDeleted = db.delete(table, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table = uri.getLastPathSegment();
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        int rowsUpdated = 0;
        rowsUpdated = db.update(table, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
