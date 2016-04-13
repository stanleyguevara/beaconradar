package net.beaconradar.history;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.events.ScanEndEvent;
import net.beaconradar.main.ServiceManager;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.utils.Prefs;

import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class HistoryPresenterImpl extends MvpBasePresenter<HistoryView>
        implements HistoryPresenter//, android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>
{
    private String TAG = getClass().getName();

    private final Context mAppContext;
    private final EventBus mBus;
    private final SharedPreferences mPrefs;
    private final SQLiteDatabase mDB;
    private final ServiceManager mManager;

    private boolean mIsStoringLog = false;

    @Inject
    public HistoryPresenterImpl(@ForApplication Context context, EventBus bus,
                                SharedPreferences prefs, MainDatabaseHelper helper,
                                ServiceManager manager) {
        this.mAppContext = context;
        this.mBus = bus;
        this.mPrefs = prefs;
        this.mDB = helper.getReadableDatabase();
        this.mManager = manager;

        long started = mPrefs.getLong(Prefs.KEY_LOG_START_TIME, 0);
        mIsStoringLog = started != 0;
    }

    @Override
    public void onResume() {
        mBus.registerSticky(this);
    }

    @Override
    public void onPause() {
        mBus.unregister(this);
    }

    @DebugLog
    public void onEvent(ScanEndEvent event) {
        //TODO actually should be event.brandNewBeacons. Otherwise this generates unnecessary loading which is heavy.
        if(isViewAttached()) getView().setBeacons(event.beacons, event.newBeacons, event.oldBeacons);
    }

    @Override @Nullable
    public LinkedHashMap<ID, Beacon> getBeacons() {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(event != null) return event.beacons;
        else return null;
    }

    @Override
    public void setRecording(boolean recording) {
        long started = mPrefs.getLong(Prefs.KEY_LOG_START_TIME, 0);
        long paused = mPrefs.getLong(Prefs.KEY_LOG_PAUSE_TIME, 0);
        if(recording) {
            if(started == 0) {
                //Starting new log
                started = System.currentTimeMillis();
                mPrefs.edit().putLong(Prefs.KEY_LOG_START_TIME, started).apply();
            }
        } else {
            paused = System.currentTimeMillis();
            mPrefs.edit().putLong(Prefs.KEY_LOG_PAUSE_TIME, paused).apply();
        }
        mIsStoringLog = true;
        mPrefs.edit().putBoolean(Prefs.KEY_LOG_ON, recording).apply();
        mManager.setLogging(recording);
        if(isViewAttached()) getView().showRecording(recording, started, paused);
    }

    @Override
    public boolean isRecording() {
        return mManager.isLogging();
    }

    @Override
    public boolean isStoringLog() {
        return mIsStoringLog;
    }

    @Override
    public long getLogStarted() {
        return mPrefs.getLong(Prefs.KEY_LOG_START_TIME, 0);
    }

    @Override
    public long getLogPaused() {
        return mPrefs.getLong(Prefs.KEY_LOG_PAUSE_TIME, 0);
    }

    @Override @Nullable
    public Beacon getDummyBeacon(ID id) {
        Cursor cursor = mDB.rawQuery("SELECT * FROM known WHERE " + id.getQueryWhere(), id.getQueryArgs());
        Beacon result = null;
        if(cursor.moveToNext()) {
            result = new Beacon(id);
            result.dressDummyFromDB(cursor);
        }
        cursor.close();
        return result;
    }

    @Override
    public void deleteLog() {
        mPrefs.edit().putBoolean(Prefs.KEY_LOG_ON, false).apply();
        long started = mPrefs.getLong(Prefs.KEY_LOG_START_TIME, 0);
        mManager.setLogging(false);
        mDB.delete("log_entries", null, null);
        mPrefs.edit().putLong(Prefs.KEY_LOG_START_TIME, 0).apply();
        mPrefs.edit().putLong(Prefs.KEY_LOG_PAUSE_TIME, 0).apply();
        mIsStoringLog = false;
        if(isViewAttached()) getView().showRecording(false, 0, 0);
    }

    @Override
    public void setBeaconName(Beacon wrapper, String name) {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(event != null) {
            Beacon changed = event.beacons.get(wrapper.getId());
            if(changed != null) changed.setName(name);
        }
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("user", 1);
        mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
    }

    @Override
    public void setBeaconColor(Beacon wrapper, @ColorInt int color) {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(event != null) {
            Beacon changed = event.beacons.get(wrapper.getId());
            if(changed != null) changed.setColor(color);
        }
        ContentValues cv = new ContentValues();
        cv.put("color", color);
        cv.put("user", 1);
        mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
    }

    @Override
    public void setBeaconIcon(Beacon wrapper, int icon) {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(event != null) {
            Beacon changed = event.beacons.get(wrapper.getId());
            if(changed != null) changed.setIcon(icon);
        }
        ContentValues cv = new ContentValues();
        String iconResName = mAppContext.getResources().getResourceEntryName(icon);
        cv.put("icon", iconResName);
        cv.put("user", 1);
        int updated = mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
        //Log.v(TAG, "updated: " + updated);
    }
}
