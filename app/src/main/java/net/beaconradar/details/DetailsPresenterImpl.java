package net.beaconradar.details;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.events.ScanEndEvent;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.altbeacon.ALT;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class DetailsPresenterImpl extends MvpBasePresenter<DetailsView> implements DetailsPresenter {
    private final Context mAppContext;
    private final SQLiteDatabase mDB;
    private final SharedPreferences mPrefs;
    private final EventBus mBus;

    private int mSamples;
    private boolean mMode;
    private boolean mAverage;
    private boolean mAutoscale;
    private boolean mExcludeMissing;

    private ID mID;                 //Non-live identifier.
    private Beacon mBeacon;         //Dataholder. May be live.

    private final String TAG = getClass().getName();

    //Casted IDs for convinience. May be live.  //TODO remove.
    @Nullable private IBC mIBC;
    @Nullable private UID mUID;
    @Nullable private URL mURL;
    @Nullable private TLM mTLM;
    @Nullable private ALT mALT;
    @Nullable private EDD mEDD;

    @Inject
    @DebugLog
    public DetailsPresenterImpl(@ForApplication Context context, MainDatabaseHelper helper, SharedPreferences prefs, EventBus bus) {
        mAppContext = context;
        mDB = helper.getWritableDatabase();
        mPrefs = prefs;
        mBus = bus;

        mSamples = mPrefs.getInt(Prefs.KEY_DETAILS_SAMPLES, Defaults.DETAILS_SAMPLES);
        mMode = mPrefs.getBoolean(Prefs.KEY_DETAILS_MODE, Defaults.DETAILS_MODE);
        mAverage = mPrefs.getBoolean(Prefs.KEY_DETAILS_AVERAGE, Defaults.DETAILS_AVERAGE);
        mAutoscale = mPrefs.getBoolean(Prefs.KEY_DETAILS_AUTOSCALE, Defaults.DETAILS_AUTOSCALE);
        mExcludeMissing = mPrefs.getBoolean(Prefs.KEY_DETAILS_EXCLUDE, Defaults.DETAILS_EXCLUDE);

    }

    @Override @DebugLog
    public void attachView(DetailsView view) {
        super.attachView(view);
        ID id = view.getIdentificator();
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(event != null) {
            Beacon beacon = event.beacons.get(id);
            if(beacon != null) {
                mBeacon = beacon;
                castBeaconID(beacon.getId());
            } else {
                mBeacon = loadBeaconFromDB(id);
            }
        } else {
            mBeacon = loadBeaconFromDB(id);
        }
        mID = id;
        getView().inflateUI();
        getView().updateBeaconFixed(mBeacon);
        getView().updateBeaconDynamic(mBeacon);
        getView().updateBeaconTick(mBeacon);
        mBus.register(this);
    }

    @Override @DebugLog
    public void setIdentificator(ID id) {
        //if(mID != null) return;
        mID = id;

    }

    /*@Nullable
    private Beacon findTLM(ID id, @Nullable ScanEndEvent event) {
        if(id.getType() == ID.T_UID || id.getType() == ID.T_URL) {
            Beacon result;
            if(event != null) {
                TLM dummyTLM = new TLM(EQ.M_TLM, 0, 0, 0, 0, id.getMac(), 0, 0);
                Beacon tlm = event.beacons.get(dummyTLM);
                if(tlm != null) result = tlm;
                else result = loadTeleFromDB(id);
            } else {
                result = loadTeleFromDB(id);
            }
            return result;
        } return  null;
    }*/

    @Nullable @DebugLog
    private Beacon loadBeaconFromDB(ID id) {
        Beacon result = null;
        Cursor cursor = mDB.rawQuery(
                "SELECT * FROM known WHERE " + id.getQueryWhere(),
                id.getQueryArgs());
        if(cursor.moveToNext()) {
            result = new Beacon(id);
            result.dressDummyFromDB(cursor);    //This also dresses id
        }
        cursor.close();
        if(result != null) castBeaconID(result.getId());
        return result;
    }

    @Nullable @DebugLog
    private Beacon loadTeleFromDB(ID id) {
        Beacon result = null;
        TLM dummyTLM = new TLM(EQ.M_TLM, 0, 0, 0, 0, id.getMac(), 0, 0);
        Cursor cursor = mDB.rawQuery(
                "SELECT * FROM known WHERE " + dummyTLM.getQueryWhere(),
                dummyTLM.getQueryArgs());
        if(cursor.moveToNext()) {
            result = new Beacon(dummyTLM);
            result.dressDummyFromDB(cursor);    //This also dresses id
        }
        return result;
    }

    private void castBeaconID(ID id) {
        int type = id.getType();
        switch (type) {
            case ID.T_IBC: mIBC = (IBC) id; break;
            case ID.T_UID: mUID = (UID) id; break;
            case ID.T_URL: mURL = (URL) id; break;
            case ID.T_TLM: mTLM = (TLM) id; break;
            case ID.T_ALT: mALT = (ALT) id; break;
            case ID.T_EDD: mEDD = (EDD) id; break;
        }
    }

    @Override @DebugLog
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        mBus.unregister(this);
    }

    @DebugLog
    public void onEvent(ScanEndEvent event) {
        if(isViewAttached()) {
            Beacon update = event.beacons.get(mID);
            if(update != null)  getView().updateBeaconDynamic(update);
            getView().updateChartTick(update);
            getView().updateBeaconTick(update);
        }
        //TODO castBeaconID, set mBeacon, mTLM etc.
    }

    @Override
    public void setMode(boolean rssi) {
        mMode = rssi;
        mPrefs.edit().putBoolean(Prefs.KEY_DETAILS_MODE, rssi).apply();
    }

    @Override
    public void setAverage(boolean average) {
        mAverage = average;
        mPrefs.edit().putBoolean(Prefs.KEY_DETAILS_AVERAGE, average).apply();
    }

    @Override
    public void setAutoscale(boolean autoscale) {
        mAutoscale = autoscale;
        mPrefs.edit().putBoolean(Prefs.KEY_DETAILS_AUTOSCALE, autoscale).apply();
    }

    @Override
    public void setExcludeMissing(boolean excludeMissing) {
        mExcludeMissing = excludeMissing;
        mPrefs.edit().putBoolean(Prefs.KEY_DETAILS_EXCLUDE, excludeMissing).apply();
    }

    @Override public int getSamplesCount() {
        return mSamples;
    }
    @Override public boolean getMode() {
        return mMode;
    }
    @Override public boolean getAverage() {
        return mAverage;
    }
    @Override public boolean getAutoscale() {
        return mAutoscale;
    }
    @Override public boolean getExcludeMissing() {
        return mExcludeMissing;
    }

    @Override
    @Nullable
    public Beacon getBeacon() {
        return mBeacon;
    }

    @Nullable
    @Override
    public Beacon getStickyBeacon() {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        Beacon beacon = null;
        if(event != null) beacon = event.beacons.get(mID);
        return beacon;
    }

    @Override public int getColor() {
        if(mBeacon != null) return mBeacon.getColor();
        else return 0;
    }

    @Override @DebugLog
    public String getName() {
        if(mBeacon != null) return mBeacon.getName();
        else return "N/A";
    }

    @Override
    public int getIcon() {
        if(mBeacon != null) return mBeacon.getIcon();
        else return 0;
    }

    @Override
    public boolean getOnAppeared() {
        if(mBeacon != null) return mBeacon.getOnAppeared();
        else return false;
    }

    @Override
    public boolean getOnVisible() {
        if(mBeacon != null) return mBeacon.getOnVisible();
        else return false;
    }

    @Override
    public boolean getOnDisappeared() {
        if(mBeacon != null) return mBeacon.getOnDisappeared();
        else return false;
    }

    @Override
    public void setColor(int color) {
        ContentValues cv = new ContentValues();
        cv.put("color", color);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs()); //TODO using dbid would be better. (Everywhere)
        if(mBeacon != null) mBeacon.setColor(color);
        if(isViewAttached()) getView().setColor(color);
    }

    @Override
    public void setName(String name) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs()); //TODO using dbid would be better.
        if(mBeacon != null) mBeacon.setName(name);
        if(isViewAttached()) getView().setName(name);
    }

    @Override
    public void setIcon(int icon) {
        ContentValues cv = new ContentValues();
        String iconResName = mAppContext.getResources().getResourceEntryName(icon);
        cv.put("icon", iconResName);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs()); //TODO using dbid would be better.
        if(mBeacon != null) mBeacon.setIcon(icon);
        if(isViewAttached()) getView().setIcon(icon);
    }

    @Override
    public void setOnAppeared(boolean checked) {
        if(mBeacon != null) mBeacon.sendOnAppeared(checked);
        ContentValues cv = new ContentValues();
        cv.put("intent_on_appeared", checked ? 1 : 0);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs());
    }

    @Override
    public void setOnVisible(boolean checked) {
        if(mBeacon != null) mBeacon.sendOnVisible(checked);
        ContentValues cv = new ContentValues();
        cv.put("intent_on_visible", checked ? 1 : 0);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs());
    }

    @Override
    public void setOnDisappeared(boolean checked) {
        if(mBeacon != null) mBeacon.sendOnDisappeared(checked);
        ContentValues cv = new ContentValues();
        cv.put("intent_on_disappeared", checked ? 1 : 0);
        cv.put("user", 1);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs());
    }

    @Override
    public void forgetBeacon() {
        ContentValues cv = new ContentValues();
        cv.put("intent_on_visible", 0);
        cv.put("intent_on_appeared", 0);
        cv.put("intent_on_disappeared", 0);
        cv.put("user", 0);
        mDB.update("known", cv, mID.getQueryWhere(), mID.getQueryArgs());
    }
}
