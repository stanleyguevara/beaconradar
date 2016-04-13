package net.beaconradar.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import net.beaconradar.dagger.ForApplication;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.utils.Const;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.IconsPalette;
import net.beaconradar.utils.Prefs;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;

import org.apache.commons.collections4.OrderedMapIterator;
import org.apache.commons.collections4.map.LRUMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;

@Singleton
public class Scanner {
    public static final int EVENT_SCAN_START = 0;
    public static final int EVENT_SCAN_STOP = 1;
    public static final int EVENT_USER_START = 2;
    public static final int EVENT_USER_STOP = 3;
    public static final int EVENT_SPOTTED = 4;
    public static final int EVENT_UPDATE = 5;
    public static final int EVENT_REMOVED_INACTIVE = 6;
    public static final int EVENT_REMOVED_FULL = 7;
    public static final int EVENT_LOG_START = 8;
    public static final int EVENT_LOG_STOP = 9;
    public static final int EVENT_MODE_CHANGE = 10;

    private String TAG = getClass().getName();

    //Concurrency
    private ConcurrentHashMap<ID, Beacon> mBeacons;
    private ThreadPoolExecutor mExecutor;

    //This is visible outside, non thread safe.
    private LRUMap<ID, Beacon> mPublished = new LRUMap<ID, Beacon>(MAX, 16, 0.75f, false) {
        @Override
        protected boolean removeLRU(LinkEntry<ID, Beacon> entry) {
            onPublishedRemove(entry.getKey(), entry.getValue());
            return true;
        }
    };
    //Helper map for non-immediate mode.
    private LRUMap<ID, Beacon> mDeferred = new LRUMap<>(MAX, 16, 0.75f, false); //Insertion order   //TODO could be smaller
    //Helper map for non-immediate mode.
    private LinkedList<ID> mHot = new LinkedList<>();
    //Helper map for TLM frames.
    //private LRUMap<String, RawBeacon> mFramesTLM = new LRUMap<>(MAX/2, 16, 0.75f);

    //Thread safety related
    private final Handler mHandler = new Handler();
    private AtomicInteger cScheduledTasks = new AtomicInteger(0);
    private SQLiteDatabase mDB, mLogDB;
    private Random mRnd = new Random();
    private BeaconParser mParser;
    private int mMinSens = Integer.MAX_VALUE;
    //private int vMinSens = Integer.MAX_VALUE;
    private long mScanStartTime;
    @SuppressWarnings("FieldCanBeLocal")
    private long mScanStopTime;
    private Runnable mStopSchedule, mStartSchedule;

    //Various, non thread safe.
    private int cExecutedTasks = 0;
    private int cVisibleBeacons = 0;       //How many detected beacons were in last cycle.
    private int cFresh = 0;                 //Count of beacons newly appeared in mBeacons. Updated continuously
    private int cDead = 0;                  //Count of beacons that were not seen for at least REMOVE time. Updated when scan ends.
    private int cFramesTLM = 0;
    private int cSubCycle = 0;
    private long mExecTimeSum = 0;
    private long mExecTimeMean = 0;         //Mean time from submitting task to result for current scan cycle.
    private boolean mRunning = false;       //Tells if scanner is running (has scheduled start/stop)
    private boolean mCycle = false;
    private boolean mLogging = false;
    private boolean mSave = true;           //Whether write to DB every time scan ends. Useful in background/locked mode.
    private long mLogStarted = 0;
    private boolean mBeaconIdModeChage = false;
    @SuppressWarnings("unused")
    private boolean mScanning = false;      //Tells if actual scan is in progress.
    @SuppressWarnings("FieldCanBeLocal")
    private int mWaitFactor = 2;            //Factor for waiting on results after stopScan() based on mean task exec time.
    private static final int MAX = 64;
    
    //For storing Beacon ID method changes until startScan() runs.
    private HashMap<String, Integer> mIdChanges = new HashMap<>(5);

    //System related
    private Context mAppContext;
    private BluetoothAdapter mBluetoothAdapter;
    private SharedPreferences mPrefs;
    private final PowerManager.WakeLock mWakeLock;

    //Modes
    private boolean mImmediateMode = false; //If true dispatches onScanResult for every beacon on main thread.

    //Timing
    private long DURATION;
    private long INTERVAL;
    private long REMOVE;
    private long SUBDUR;
    private long SPLIT;

    //Result listener
    private ScanResultListener mListener;

    //Debug
    private boolean dExecTiming = false;
    private boolean dCycling = false;

    public void setLogging(boolean logging) {
        this.mLogging = logging;
        ContentValues cv = new ContentValues();
        cv.put("event", logging ? EVENT_LOG_START : EVENT_LOG_STOP);
        cv.put("_id", -1);
        cv.put("time", System.currentTimeMillis());
        mDB.insert("log_entries", null, cv);
    }

    public boolean isLogging() {
        return mLogging;
    }

    public void setTiming(long interval, long duration, long remove, long split, boolean save, int mode) {                //TODO make it safe (in context of calling start/stop)
        this.INTERVAL = interval;
        this.DURATION = duration;
        this.REMOVE = remove;
        this.mSave = save;
        this.SPLIT = split;
        this.SUBDUR = duration/split;
        if(mLogging) {
            ContentValues cv = new ContentValues();
            cv.put("time", System.currentTimeMillis());
            switch (mode) {
                case BeaconService.SCAN_FOREGROUND: cv.put("other", "Mode Foreground"); break;
                case BeaconService.SCAN_BACKGROUND: cv.put("other", "Mode Background"); break;
                case BeaconService.SCAN_LOCKED:     cv.put("other", "Mode Device Locked"); break;
            }
            cv.put("event", EVENT_MODE_CHANGE);
            cv.put("_id", -1);
            mDB.insert("log_entries", null, cv);
        }
    }

    public long getDuration() { return DURATION; }

    public boolean isRunning() {
        return mRunning;
    }

    public long getInterval() { return INTERVAL; }

    public long getSplit() { return SPLIT; }

    public int getFoundCount() {
        return mPublished.size();
    }

    public int getVisibleCount() {
        return cVisibleBeacons;
    }

    @Inject
    public Scanner(@ForApplication Context appContext, MainDatabaseHelper helper, SharedPreferences prefs, IconsPalette icons) {
        mAppContext = appContext;
        mDB = helper.getWritableDatabase();
        mPrefs = prefs;
        PowerManager PM = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = PM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        mLogging = mPrefs.getBoolean(Prefs.KEY_LOG_ON, false);
        int processors = Runtime.getRuntime().availableProcessors();
        mBeacons = new ConcurrentHashMap<>(64, 0.75f, processors+1);
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(processors);
        mParser = new BeaconParser();
        mMinSens = mPrefs.getInt(Prefs.KEY_MIN_SENS, 0);
        //vMinSens = mMinSens;
        BluetoothManager BTManager;
        BTManager = (BluetoothManager) mAppContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BTManager.getAdapter();
        initEQ();

        //Restore mBeacons / mPublished
        Cursor cur = mDB.rawQuery("SELECT " +
                "n.type AS type, n.eq_mode AS eq_mode, n.rssi AS rssi, n.tx AS tx, " +
                "n.last_seen AS last_seen, n.id0 AS id0, n.id1 AS id1, n.id2 AS id2, " +
                "n.mac AS mac, n.other AS other, n.spotted AS spotted, " +
                "k._id AS _id, k.color AS color, k.icon AS icon, k.name AS name, " +
                "k.intent_on_appeared AS intent_on_appeared, k.intent_on_visible AS intent_on_visible, " +
                "k.intent_on_disappeared AS intent_on_disappeared, k.discovered AS discovered " +
                "FROM nearby AS n LEFT JOIN known AS k ON n._id = k._id ORDER BY k.last_seen ASC", null);
        while (cur.moveToNext()) {
            Log.v(TAG, "Restoring beacon");
            Beacon beacon = new Beacon(cur);
            beacon.updateMinSens(mMinSens);
            mBeacons.put(beacon.getId(), beacon);
            mPublished.put(beacon.getId(), beacon);
        }
        cur.close();
    }

    public void setListener(ScanResultListener listener) {
        mListener = listener;
    }

    private void initEQ() {
        EQ.M_IBC = mPrefs.getInt(Prefs.KEY_IBC_ID_METHOD, Defaults.EQ_MODE_IBC);
        EQ.M_UID = mPrefs.getInt(Prefs.KEY_UID_ID_METHOD, Defaults.EQ_MODE_UID);
        EQ.M_URL = mPrefs.getInt(Prefs.KEY_URL_ID_METHOD, Defaults.EQ_MODE_URL);
        EQ.M_TLM = mPrefs.getInt(Prefs.KEY_TLM_ID_METHOD, Defaults.EQ_MODE_TLM);
        EQ.M_ALT = mPrefs.getInt(Prefs.KEY_ALT_ID_METHOD, Defaults.EQ_MODE_ALT);
    }

    @DebugLog
    public void start(boolean cycle, boolean log) {
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            //Log.v(TAG, "ServiceScanner WakeLock acquired");
        }
        mCycle = cycle;//INTERVAL + DURATION <= 60 * 1000;
        if(mRunning) return;    //If already running do nothing
        mRunning = true;
        if(dCycling) //noinspection ConstantConditions
            Log.v(TAG, ">>>>>>>>>>>> START CALLED, " +
                "mCycle: "+mCycle+" " +
                "mRunning: "+mRunning);
        if(mLogging && log) {
            ContentValues cv = new ContentValues();
            cv.put("event", EVENT_USER_START);
            cv.put("_id", -1);
            cv.put("time", System.currentTimeMillis());
            mDB.insert("log_entries", null, cv);
        }
        cancelPendingStartStop();
        startScan();
    }

    @DebugLog
    public void stop(boolean log) {
        if(dCycling) //noinspection ConstantConditions
            Log.v(TAG, ">>>>>>>>>>>> STOP CALLED");
        if(mLogging && log) {
            ContentValues cv = new ContentValues();
            cv.put("event", EVENT_USER_STOP);
            cv.put("_id", -1);
            cv.put("time", System.currentTimeMillis());
            mDB.insert("log_entries", null, cv);
        }
        cancelPendingStartStop();
        if(mRunning) { stopScan(); }
        mRunning = false;
    }

    public void setBeaconID(String key, int value) {
        mIdChanges.put(key, value);
        mBeaconIdModeChage = true;
    }

    private void cancelPendingStartStop() {
        mHandler.removeCallbacks(mStartSchedule);
        mHandler.removeCallbacks(mStopSchedule);
    }

    private void scheduleStop() {
        if(SPLIT > 1 && cSubCycle < SPLIT) {
            mStopSchedule = new Runnable() {
                @Override
                public void run() {
                    cSubCycle++;
                    mBluetoothAdapter.stopLeScan(mCallback);
                    if(dCycling) Log.v(TAG, ">>>>>>>>>>>> SPLIT SCAN");
                    mBluetoothAdapter.startLeScan(mCallback);
                    scheduleStop();
                }
            };
        } else {
            mStopSchedule = new Runnable() {
                @Override
                public void run() {
                    if(!mRunning) return;
                    stopScan();
                    if(mCycle) scheduleStart();
                    else mRunning = false;
                }
            };
        }
        mHandler.postDelayed(mStopSchedule, SUBDUR);
    }

    private void scheduleStart() {
        mStartSchedule = new Runnable() {
            @Override
            public void run() {
                if(!mRunning) return;
                startScan();
            }
        };
        mHandler.postDelayed(mStartSchedule, INTERVAL);
    }

    private void startScan() {
        mScanning = true;
        if(mBeaconIdModeChage) {
            //purge();                //This may be overkill, we need to remove only kinds of beacons that changed, e.g. UID, IBC, EDD (?)
            //mListener.purge();
            for (Map.Entry<String, Integer> entry: mIdChanges.entrySet()){
                String key = entry.getKey();
                int value = entry.getValue();
                switch (key) {
                    case Prefs.KEY_IBC_ID_METHOD: EQ.M_IBC = value; break;
                    case Prefs.KEY_UID_ID_METHOD: EQ.M_UID = value; break;
                    case Prefs.KEY_URL_ID_METHOD: EQ.M_URL = value; break;
                    case Prefs.KEY_TLM_ID_METHOD: EQ.M_TLM = value; break;
                    case Prefs.KEY_ALT_ID_METHOD: EQ.M_ALT = value; break;
                }
                //Write to local variables if we need to avoid crossing memory barrier.
            }
            mIdChanges.clear();
            mBeaconIdModeChage = false;
        }
        cDead = 0;
        //cDead = removeOldBeacons(System.currentTimeMillis());
        if(mImmediateMode) setBeaconsCold();
        mListener.onScanStart(ScanManager.REQ_NULL, DURATION);
        mScanStartTime = System.currentTimeMillis();
        cScheduledTasks.set(0);
        cExecutedTasks = 0;
        mExecTimeMean = 0;
        mExecTimeSum = 0;
        cVisibleBeacons = 0;
        cFresh = 0;
        if(mLogging) {
            ContentValues cv = new ContentValues();
            cv.put("time", mScanStartTime);
            cv.put("event", EVENT_SCAN_START);
            cv.put("_id", -1);
            mDB.insert("log_entries", null, cv);
        }
        if(dCycling) Log.v(TAG, ">>>>>>>>>>>> START SCAN");
        mBluetoothAdapter.startLeScan(mCallback); //comment for testing on emulator
        scheduleStop();
    }

    private void stopScan() {
        mScanning = false;
        cSubCycle = 0;
        //Returns immediately and runs callback on a few (3?) threads (not a word in docs on this)
        mBluetoothAdapter.stopLeScan(mCallback);  //comment for testing on emulator
        mScanStopTime = System.currentTimeMillis();
        if(dCycling) Log.v(TAG, ">>>>>>>>>>>> END SCAN, " +
                "scheduled tasks: " + cScheduledTasks + " " +
                "executed tasks: " + cExecutedTasks+" " +
                "Mean exec time: "+mExecTimeMean);
        if(!mImmediateMode) {
            if(cScheduledTasks.get() != cExecutedTasks) {
                //Not all tasks done on time
                int awaitingTasks =  cScheduledTasks.get() - cExecutedTasks;
                //Clear executors queue
                mExecutor.getQueue().clear();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() { onResultsReady();
                    }
                }, Math.min(mWaitFactor * mExecTimeMean * awaitingTasks, INTERVAL));
                if(dExecTiming) Log.w(TAG, "WARNING: " +
                        "Scheduled: " + cScheduledTasks + " " +
                        "Executed: " + cExecutedTasks);
            } else {
                //All tasks done on time
                onResultsReady();
            }
        } else {
            cDead += removeOldBeacons(mScanStopTime);
        }
    }

    private void setBeaconsCold() {
        for (Iterator<ID> itc = mHot.iterator(); itc.hasNext();) {
            ID id = itc.next();
            Beacon hot = mPublished.get(id);
            if(hot != null) hot.setCold();
            itc.remove();
        }
    }

    /**
     * Called on main thread only, for bulk mode only.
     */
    private void onResultsReady() {
        //Cool down hot beacons.
        setBeaconsCold();

        //Insert new/updated beacons.
        OrderedMapIterator<ID, Beacon> itd = mDeferred.mapIterator();
        boolean newMinSens = false;
        boolean fresh;
        //if(mSave) mDB.beginTransaction();
        //if(mLogging) mLogDB.beginTransaction();
        while (itd.hasNext()) {             //Iterates from LRU to MRU
            ID id = itd.next();
            Beacon beacon = itd.getValue();
            fresh = beacon.update();                //Volatile read
            if(beacon.getRssi() < mMinSens) {
                mMinSens = beacon.getRssi();
                //vMinSens = mMinSens;
                newMinSens = true;
            }
            if(fresh) {
                cFresh++;
                beacon.updatePrevValues(mMinSens);  //This may be inaccurate, but levels as mMinSens stabilizes.
                beacon.updateMinSens(mMinSens);     //This may be inaccurate, but levels as mMinSens stabilizes.
                //if(beacon.on_appeared) sendBroadcast(Const.INTENT_APPEARED, beacon);
            } else {
                //if(beacon.on_update) sendBroadcast(Const.INTENT_UPDATED, beacon);
            }
            Beacon inserted = mPublished.put(id, beacon);
            Log.v(TAG, "inserted: "+(inserted == null ? "null" : "not null")+" type: "+id.getType());
            if(inserted == null && id.getType() == ID.T_TLM) cFramesTLM++;
            mHot.add(id);
            cVisibleBeacons++;
            //This would update DB on every beacon frame. Overkill. (Instead done when removing beacons)
            /*if(mSave) {
                mDB.update("known",
                        beacon.getUpdateCV(),
                        beacon.getId().getQueryWhere(),
                        beacon.getId().getQueryArgs());
            }*/
            //if(mLogging) mLogDB.insert("log_entries", null, beacon.getLogCV());
            itd.remove();
        }
        /*if(mLogging) {
            mLogDB.setTransactionSuccessful();
            mLogDB.endTransaction();
        }*/
        /*if(mSave) {
            mDB.setTransactionSuccessful();
            mDB.endTransaction();
        }*/

        //Update every beacon min sens if needed, stick TLM frame to UID/URL beacons.
        //Log.v(TAG, "cFramesTLM: "+cFramesTLM+" newMinSens: "+newMinSens);
        if(newMinSens || cFramesTLM > 0) {   //Keep count of TLM instead
            OrderedMapIterator<ID, Beacon> itp = mPublished.mapIterator();
            TLM dummyTLM = new TLM(EQ.M_TLM, 0, 0, 0, 0, "", 0, 0);
            Beacon tlm;
            while (itp.hasNext()) {
                itp.next();
                Beacon beacon = itp.getValue();
                beacon.updateMinSens(mMinSens);
                tlm = null;
                //Log.v(TAG, "Adding TLM...");
                if (beacon.getId().getType() == ID.T_UID) {
                    dummyTLM.setMac(beacon.getId().getMac());
                    tlm = mPublished.get(dummyTLM, false);
                    if(tlm != null) {
                        //Log.v(TAG, "Added TLM to UID");
                        UID uid = (UID) beacon.getId();
                        uid.beaconTLM = tlm;
                        tlm.setTx(uid.tx);
                    }
                } else if (beacon.getId().getType() == ID.T_URL) {
                    //If logical URL beacon consist of multiple physical URL beacons
                    //with same url content, then assignment of TLM will be essentially random.
                    //Don't care, maybe TODO someday.
                    dummyTLM.setMac(beacon.getId().getMac());
                    tlm = mPublished.get(dummyTLM, false);
                    if(tlm != null) {
                        //Log.v(TAG, "Added TLM to URL");
                        URL url = (URL) beacon.getId();
                        url.beaconTLM = tlm;
                        tlm.setTx(url.tx);
                    }
                } else if (beacon.getId().getType() == ID.T_EDD) {
                    dummyTLM.setMac(beacon.getId().getMac());
                    tlm = mPublished.get(dummyTLM, false);
                    if(tlm != null) {
                        //Log.v(TAG, "Added TLM to EDD");
                        EDD edd = (EDD) beacon.getId();
                        edd.updateID(tlm.getId());
                        //Log.v(TAG, "beacon.getTX "+beacon.getTx()+" edd.tx "+edd.tx);
                        tlm.setTx(beacon.getTx());
                        //We can't infer tx for EDD built on TLM only frame.
                        //That would require looking for UID/URL with matching mac.
                        //Actually we can, but only for EQ.M_UID / EQ.M_URL == EQ.MAC
                    }
                }
            }
            //if(mFramesTLM.size() > 0) mFramesTLM.clear();
            if(newMinSens) mPrefs.edit().putInt(Prefs.KEY_MIN_SENS, mMinSens).apply();
        }

        //Remove old beacons
        long stop = System.currentTimeMillis();
        cDead += removeOldBeacons(stop);

        if(dCycling) Log.v(TAG, ">>>>>>>>>>>> SCAN RESULTS >>>>>>>>>>>>, " +
                "scheduled tasks: " + cScheduledTasks + " " +
                "executed tasks: " + cExecutedTasks + " " +
                "cFresh: " + cFresh + " cDead: " + cDead
        );
        if(dCycling) Log.v(TAG, "Sizes: mPublished " + mPublished.size()+
                                      " mBeacons " +   mBeacons.size()+
                                      " mDeferred " +  mDeferred.size());

        if(mLogging) {
            ContentValues cv = new ContentValues();
            cv.put("time", stop);
            cv.put("event", EVENT_SCAN_STOP);
            cv.put("_id", -1);
            mDB.insert("log_entries", null, cv);
        }

        //Send results              //TODO Second parameter = any, not used in mListener.
        mListener.onScanEnd(mPublished, ScanManager.REQ_NULL, INTERVAL, cFresh, cDead);    //This takes 90% of time of onResultsReady
        if(!mCycle && mWakeLock.isHeld()) {
            //Log.v(TAG, "ServiceScanner WakeLock released");
            mWakeLock.release();
        }
    }

    /**
     * Call only on main thread
     * @param now Timestamp to calculate if beacon qualifies for removal.
     * @return Removed beacons count.
     */
    @DebugLog
    private int removeOldBeacons(long now) {
        OrderedMapIterator<ID, Beacon> itr = mPublished.mapIterator();
        int dead = 0;
        //Iterates from LRU to MRU.
        while (itr.hasNext()) {
            ID id = itr.next();
            Beacon beacon = itr.getValue();
            if(now - beacon.getLastSeen() > REMOVE) {                                               //TODO force REMOVE > INTERVAL + DURATION ?
                itr.remove();
                mBeacons.remove(id);
                if(id.getType() == ID.T_TLM) cFramesTLM--;
                if(cFramesTLM < 0) cFramesTLM = 0;  //TODO this should not occur, but did while switching eq_modes
                updateBeaconDB(id, beacon);
                mDB.delete("nearby", "_id = ?", new String[]{String.valueOf(beacon.getDbId())});
                if(mLogging) {
                    ContentValues cv = beacon.getLogCV(mAppContext);
                    cv.put("event", EVENT_REMOVED_INACTIVE);
                    cv.put("time", now);
                    mDB.insert("log_entries", null, cv);
                }
                if(beacon.getOnDisappeared()) sendBroadcast(Const.INTENT_DISAPPEARED, beacon, null);
                dead++;
            } else break;   //Beacons are sorted LRU to MRU so we can stop at first non-removable beacon.
        }
        return dead;
    }

    private void purge() {  //Theoretically purge should sendBroadcast too. TODO later.
        mBeacons.clear();
        mPublished.clear();
        cFramesTLM = 0;
        mDeferred.clear();
        mHot.clear();
        //mFramesTLM.clear();
    }

    //TODO check if this shouldn't be called via handler on main thread for thread safety
    private void onPublishedRemove(ID id, Beacon removed) {
        if(id.getType() == ID.T_TLM) cFramesTLM--;
        if(cFramesTLM < 0) cFramesTLM = 0;  //TODO this should not occur, but did while switching eq_modes
        //Possible cause: When we start in MERGE mode TLM frame is not counted up, but when removing is substracted?
        updateBeaconDB(id, removed);
        if(mLogging) {
            ContentValues cv = removed.getLogCV(mAppContext);
            cv.put("event", EVENT_REMOVED_FULL);
            cv.put("time", System.currentTimeMillis());
            mDB.insert("log_entries", null, cv);
        }
        mDB.delete("nearby", "_id = ?", new String[]{String.valueOf(removed.getDbId())});
        if(removed.getOnDisappeared()) sendBroadcast(Const.INTENT_DISAPPEARED, removed, null);
    }

    private void updateBeaconDB(ID id, Beacon beacon) {
        mDB.update("known", beacon.getUpdateCV(), "_id = ?", new String[]{String.valueOf(beacon.getDbId())});
    }

    /**
     * Should be called on main thread only.
     * @param beacon
     * @param started
     * @param submitted
     */
    private void onParsed(@NonNull Beacon beacon, long started, long submitted) {
        long now = System.currentTimeMillis();
        if(submitted >= mScanStartTime) {  //Task done on time
            cExecutedTasks++;
            if(dExecTiming) Log.v(TAG, "Task OK. " +
                    "Total: "+(now - submitted)+" " +
                    "Exec: "+(now - started)+" " +
                    "Queued: "+(started - submitted));
            mExecTimeSum += now - started;
            mExecTimeMean = mExecTimeSum / cExecutedTasks;
            if(mImmediateMode) {
                cVisibleBeacons++;
                boolean fresh = beacon.update();            //Volatile read inside.
                if(beacon.getRssi() < mMinSens) {
                    mMinSens = beacon.getRssi();
                    //vMinSens = mMinSens;
                    beacon.updateMinSens(mMinSens);
                    mPrefs.edit().putInt(Prefs.KEY_MIN_SENS, mMinSens).apply();
                }
                if(fresh) {
                    cFresh++;
                    beacon.updatePrevValues(mMinSens);
                }
                mPublished.put(beacon.getId(), beacon);     //Moves Beacon to MRU.
                mHot.add(beacon.getId());
                mListener.onScanResult(beacon);
            } else {
                mDeferred.put(beacon.getId(), beacon);
            }
        } else {                            //Task result arrived in wrong scan cycle, rejecting.
            if(dExecTiming) Log.w(TAG,
                    "Rejected Result: Task result arrived in wrong scan cycle. " +
                            "Total: " + (now - submitted) + " " +
                            "Exec: " + (now - started) + " " +
                            "Queued: " + (started - submitted));
        }
    }

    //This should always be called on main thread.
    private void onParseFail(@SuppressWarnings("UnusedParameters") ScanResult mScan, long started, long submitted) {
        long now = System.currentTimeMillis();
        if(submitted >= mScanStartTime) {  //Task done on time
            cExecutedTasks++;
            if(dExecTiming) Log.v(TAG, "Task FAILED. " +
                    "Total: "+(now - submitted)+" " +
                    "Exec: "+(now - started)+" " +
                    "Queued: "+(started - submitted));
            mExecTimeSum += now - started;
            mExecTimeMean = mExecTimeSum / cExecutedTasks;
        } else {                            //Task result arrived in wrong scan cycle, rejecting.
            if(dExecTiming) Log.w(TAG,
                    "Rejected & FAILED Result: Task result arrived in wrong scan cycle. " +
                            "Total: " + (now - submitted) + " " +
                            "Exec: " + (now - started) + " " +
                            "Queued: " + (started - submitted));
        }
    }

    private LeScanCallback mCallback = new LeScanCallback() {
        //This callback does NOT run on main thread (at least on 4.4).
        //It runs on 2 or 3 different threads between startLeScan and stopLeScan
        //We submit every callback to ExecutorService as Runnable for processing.
        //Results are posted to main thread.
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            cScheduledTasks.incrementAndGet();
            //Submitting to executor is thread safe.
            mExecutor.submit(new Processor(
                            new ScanResult(device.getAddress(), rssi, scanRecord),
                            System.currentTimeMillis(), mLogging)
            );
        }
    };

    public class Processor implements Runnable {
        final ScanResult mScan;
        final Handler mHandler = new Handler(Looper.getMainLooper());
        final long mSubmitted;
        final boolean mLog;
        long mStarted;

        public Processor(ScanResult scan, long submitted, boolean log) {
            this.mScan = scan;
            this.mSubmitted = submitted;
            this.mLog = log;
        }

        @Override
        public void run() {
            mStarted = System.currentTimeMillis();
            if(dExecTiming)
                try { Thread.sleep((long)(Math.random()*1000)); }
                catch (InterruptedException e) { e.printStackTrace(); }

            //This produces IBC, UID, URL, TLM, ALT, but never EDD.
            final ID raw = mParser.parse(mScan.scanRecord, mScan.address, mScan.rssi);

            if(raw == null) {   //Invalid parser output handling & reporting.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { onParseFail(mScan, mStarted, mSubmitted); }
                });
                return;
            }

            ID id = raw;

            //Special case for EDD.
            if(id.getType() == ID.T_UID ) {
                if(EQ.M_UID == EQ.MERGE) id = new EDD(id.getMac(), id.rssi, id.tx);
            } else if(id.getType() == ID.T_URL) {
                if(EQ.M_URL == EQ.MERGE) id = new EDD(id.getMac(), id.rssi, id.tx);
            } else if(id.getType() == ID.T_TLM) {
                if(EQ.M_TLM == EQ.MERGE) id = new EDD(id.getMac(), id.rssi, id.tx);
            }

            /*if(EQ.M_UID == EQ.MERGE || EQ.M_URL == EQ.MAC) {
                if(id.getType() == ID.T_UID || id.getType() == ID.T_URL) {
                    //EDD will take last vUpdate. This means some updates may get lost.
                    id = new EDD(id.getMac(), id.rssi, id.tx);
                }
            }*/

            final Beacon candidate = new Beacon(id/*, raw.getRssi(), vMinSens*/);
            //Check in synchronized map if beacon exists.
            final Beacon existing = mBeacons.putIfAbsent(id, candidate);
            if(existing == null) {
                //If beacon doesn't exist we may put it in DB. This guarantees no duplicates.
                String where = id.getQueryWhere();
                String query = "SELECT _id, color, icon, name, discovered, " +
                        "intent_on_appeared, intent_on_visible, intent_on_disappeared " +
                        "FROM known WHERE " + where;
                String[] args = id.getQueryArgs();                  //Based on EQ mode
                Cursor cursor = mDB.rawQuery(query, args);
                if(cursor.moveToNext()) {                   //We know this beacon
                    candidate.dressFromDB(cursor);
                } else {                                    //It's a brand new beacon! Yay!
                    candidate.dressBrandNew(mRnd, id.getSpotted());  //Set icon, name, etc.
                    ContentValues cv = candidate.getCV(mAppContext);
                    long insert = mDB.insert("known", null, cv);
                    if(insert != -1) {
                        //Successful insert
                        candidate.setDbId(insert);
                        //if(global.on_new) sendBroadcast();    //TODO future
                    } else {
                        //Insert failed.
                        mBeacons.remove(id);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() { onParseFail(mScan, mStarted, mSubmitted);
                            }
                        });
                        return;
                    }
                }
                cursor.close();
                candidate.setPendingUpdate(id);    //Volatile write, makes sure all fields written to this point are visible to main thread. (if it reads update)
                ContentValues cval = candidate.getUpdateCV();
                cval.put("_id", candidate.getDbId());
                cval.put("rssi", id.rssi);
                cval.put("discovered", candidate.getDiscovered());
                mDB.insertWithOnConflict("nearby", null, cval, SQLiteDatabase.CONFLICT_REPLACE);
                if(mLog) {
                    ContentValues cv = candidate.getLogCV(mAppContext);
                    cv.put("event", EVENT_SPOTTED);
                    mDB.insert("log_entries", null, cv);
                }
                if(candidate.getOnAppeared()) sendBroadcast(Const.INTENT_APPEARED, candidate, null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { onParsed(candidate, mStarted, mSubmitted); }
                });
                return;
            } else {
                //We got this beacon in ConcurrentHashMap already (so it exists in DB)
                //We can't modify candidate fields now (due to concurrency from other threads)
                //So...
                existing.setPendingUpdate(raw);

                //Volatile read may be needed
                ContentValues cval = existing.getUpdateCV(id);
                mDB.insertWithOnConflict("nearby", null, cval, SQLiteDatabase.CONFLICT_REPLACE);
                if(mLog) {
                    ContentValues cv = existing.getLogCV(id, mAppContext);
                    cv.put("time", raw.getSpotted());
                    cv.put("event", EVENT_UPDATE);
                    mDB.insert("log_entries", null, cv);
                }
                if(existing.getOnVisible()) sendBroadcast(Const.INTENT_VISIBLE, existing, id);
                //Volatile read may be needed end

                mHandler.post(new Runnable() {
                    @Override
                    public void run() { onParsed(existing, mStarted, mSubmitted); }
                });
                return;
            }
        }
    }

    /**
     * Sends Intents to 3rd party software.
     * This method should be thread safe,
     * provided no one modifies objects passed as parameters.
     * @param action
     * @param beacon
     * @param update Whether we can use RawBeacon update to get last RSSI etc.
     */
    @DebugLog
    private void sendBroadcast(@NonNull String action, @NonNull Beacon beacon, ID update) {
        Intent intent = new Intent(action);
        if(update != null) {
            beacon.putExtras(intent, update);
        } else {
            beacon.putExtras(intent);
        }
        mAppContext.sendBroadcast(intent);
    }
}
