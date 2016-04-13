package net.beaconradar.logread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import net.beaconradar.R;
import net.beaconradar.dagger.App;
import net.beaconradar.database.MainContentProvider;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.details.DetailsActivity;
import net.beaconradar.fab.ProgressFAB;
import net.beaconradar.history.RecyclerViewCursorIdClickListener;
import net.beaconradar.utils.Prefs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class LogReadActivity extends AppCompatActivity
        implements RecyclerViewCursorIdClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    //General Views
    @Bind(R.id.fab) ProgressFAB mFab;
    @Bind(R.id.container) CoordinatorLayout mCoordinator;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.recycler) RecyclerView mRecycler;

    private LogAdapter mAdapter;
    private final String where_all = "";
    private final String where_beacons = "WHERE l._id != -1";
    private final String where_system = "WHERE l._id = -1";
    private String WHERE = where_all;

    private static final int BUFFER_SIZE = 1024;

    private SQLiteDatabase mDB;

    @Override @DebugLog
    protected void onCreate(Bundle savedInstanceState) {
        MainDatabaseHelper helper = MainDatabaseHelper.getInstance(getApplicationContext());
        mDB = helper.getWritableDatabase();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override @DebugLog
    protected void onResume() {
        super.onResume();
        ((App) getApplication()).onResume();
        mFab.onResume();
    }

    @Override @DebugLog
    protected void onPause() {
        super.onPause();
        mFab.onPause();
        ((App) getApplication()).onPause();
    }

    @DebugLog
    public void swapCursor(Cursor data) {
        if(mAdapter == null) {
            mAdapter = new LogAdapter(data, this, false);
            mRecycler.setAdapter(mAdapter);
            //Restore state if needed
            /*if(mRestorePosition != RecyclerView.NO_POSITION) {
                int size = mAdapter.getItemCount()-1;
                LinearLayoutManager manager = (LinearLayoutManager) mRecycler.getLayoutManager();
                if(mRestorePosition <= size) manager.scrollToPositionWithOffset(mRestorePosition, mRestoreOffset);
                else manager.scrollToPosition(size);
                mRestoreOffset = 0;
                mRestorePosition = RecyclerView.NO_POSITION;
            }*/
        } else {
            //Log.v(TAG, "Change");
            mAdapter.changeCursor(data);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query =
                "SELECT k.icon AS icon, k.color AS color, " +
                "k.name AS name, k.type AS type, k.eq_mode AS eq_mode, " +
                "l._id AS _id, l.time AS time, l.event AS event, l.rssi AS rssi, " +
                "l.id0 AS id0, l.id1 AS id1, l.id2 AS id2, l.mac AS mac, l.tx AS tx, " +
                "l.other AS other " +
                "FROM log_entries AS l " +
                "LEFT JOIN known AS k " +
                "ON k._id = l._id "+WHERE+" " +
                "ORDER BY l._ROWID_ ASC";
        String uri = "content://" + MainContentProvider.AUTHORITY + "/RAW_QUERY";
        CursorLoader loader = new CursorLoader(this, Uri.parse(uri), null, query, null, null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log, menu);
        //MenuItem item = menu.findItem(R.id.action_);
        //item.setIcon(getIconForMode(mPresenter.getMode()));
        //item.setChecked(mPresenter.getAverage());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.send:
                sendLog();
                return true;
            case R.id.toggle:
                if(WHERE.equals(where_all)) WHERE = where_beacons;
                else if(WHERE.equals(where_beacons)) WHERE = where_system;
                else if(WHERE.equals(where_system)) WHERE = where_all;
                getSupportLoaderManager().restartLoader(0, null, this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendLog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long started = prefs.getLong(Prefs.KEY_LOG_START_TIME, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String name = "log_"+sdf.format(new Date(started))+".zip";

        File logdir = new File(getFilesDir(), "logs");
        if(!logdir.exists()) logdir.mkdir();
        for (File file : logdir.listFiles()) file.delete(); //purge
        File logfile = new File(logdir, "log.db");
        File zipfile = new File(logdir, name);
        SQLiteDatabase logDB = SQLiteDatabase.openOrCreateDatabase(logfile, null);
        //mDB.beginTransaction();
        mDB.execSQL("ATTACH DATABASE '"+logfile.getAbsolutePath()+"' AS log");
        mDB.execSQL(MainDatabaseHelper.CREATE_LOG_TABLE_EXPORT);
        mDB.execSQL(
                "INSERT INTO log.log_entries(" +
                    "_id, time, event, name, color, icon, rssi, " +
                    "type, eq_mode, id0, id1, id2, mac, tx, other) " +
                "SELECT l._id, l.time, l.event, k.name, k.color, k.icon, l.rssi, k.type, " +
                    "k.eq_mode, l.id0, l.id1, l.id2, l.mac, l.tx, l.other " +
                    "FROM main.log_entries AS l " +
                    "LEFT JOIN main.known AS k " +
                    "ON k._id = l._id "+WHERE+" " +
                    "ORDER BY l._ROWID_ ASC");
        mDB.execSQL("DETACH log");
        logDB.close();
        try {
            zip(new String[]{logfile.getAbsolutePath()}, zipfile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(zipfile.exists()) {
            Uri uri = FileProvider.getUriForFile(this, "net.beaconradar.fileprovider", zipfile);
            //Log.v("LogReadActivity", "URI: " + uri.toString());
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            //shareIntent.setType("file/*");
            shareIntent.setType("application/zip");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(shareIntent, "Send "+name+" via:"));
        }
        //mDB.setTransactionSuccessful();
        //mDB.endTransaction();
    }

    public static void zip(String[] files, String zipFile) throws IOException {
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];
            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        } finally {
            out.close();
        }
    }

    @Override
    public void onClick(int type, View view, int position, long id, RecyclerView.ViewHolder holder) {
        if(id != -1) {  //Beacon related event
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("identifier", ((LogAdapter.CursorHolder) holder).identifier);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    public void onLongClick(int type, View view, int position, long id, RecyclerView.ViewHolder holder) {

    }

    @Override @DebugLog
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {  //There were changes
            setResult(1);
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }
}
