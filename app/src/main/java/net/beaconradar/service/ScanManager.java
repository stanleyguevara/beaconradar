package net.beaconradar.service;

//Responsibility: Manage scan timing

import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.beaconradar.dagger.ForApplication;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import org.apache.commons.collections4.map.LRUMap;

import hugo.weaving.DebugLog;

@Singleton
public class ScanManager implements ScanResultListener, BluetoothListener {
    private String TAG = getClass().getName();

    protected static final int REQ_NULL  = 0;
    public    static final int REQ_SCAN  = 1;
    public    static final int REQ_PAUSE = 2;
    public    static final int REQ_KILL  = 3;

    private long[] intervals = new long[3];
    private long[] durations = new long[3];
    private long[] removes   = new long[3];
    private long[] splits    = new long[3];

    private int mMode = BeaconService.SCAN_UNKNOWN;

    //Dependencies
    private final Context mAppContext;
    private final SharedPreferences mPrefs;
    private final Scanner mScanner;
    private final NotifyHelper mNotify;
    private final BTManager mBTManager;
    private final Scheduler mScheduler;
    private BeaconService mService;

    //Listener
    private ScanResultListener mListener;

    //State
    private int mUserRequest = REQ_NULL;    //No need to restore. Set in BeaconService onCreate
    private boolean mExact;

    @Inject @DebugLog
    public ScanManager(@ForApplication Context appContext, SharedPreferences prefs,
                       Scanner scanner, NotifyHelper notify, BTManager btManager) {
        mAppContext = appContext;
        mPrefs = prefs;
        mScanner = scanner;
        mScanner.setListener(this);
        mBTManager = btManager;
        mBTManager.setListenerService(this);
        AlarmManager AM = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        mNotify = notify;

        int SDK = Build.VERSION.SDK_INT;
        if(SDK < Build.VERSION_CODES.KITKAT) {
            mScheduler = new Scheduler18(mAppContext, AM);
        } else if(SDK >= Build.VERSION_CODES.KITKAT && SDK <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mScheduler = new Scheduler22(mAppContext, AM);
        } else if(SDK >= Build.VERSION_CODES.M) {
            mScheduler = new SchedulerXX(mAppContext, AM);
        } else mScheduler = null;

        //Read timing
        for(int m = 0; m < 3; m++) {
            String keyInterval = Prefs.KEYS_INTERVAL[m];
            intervals[m] = mPrefs.getLong(keyInterval, Defaults.INTERVAL[m]);
            String keyDuration = Prefs.KEYS_DURATION[m];
            durations[m] = mPrefs.getLong(keyDuration, Defaults.DURATION[m]);
            String keyRemove = Prefs.KEYS_REMOVE[m];
            removes[m] = mPrefs.getLong(keyRemove, Defaults.REMOVE[m]);
            String keySplit = Prefs.KEYS_SPLIT[m];
            splits[m] = mPrefs.getLong(keySplit, Defaults.SPLIT[m]);
        }
        mExact = mPrefs.getBoolean(Prefs.KEY_EXACT_SCHEDULING, Defaults.EXACT_SCHEDULING);
    }

    public void setListener(ScanResultListener listener, BeaconService service) {
        mListener = listener;
        mService = service;
    }

    @DebugLog
    public void setExact(boolean exact) {
        mExact = exact;
    }

    @DebugLog
    public void onBluetoothStateChanged(int state) {
        //We're inside onReceive. No need for wakelock
        if(state == BluetoothAdapter.STATE_ON) {
            boolean cycle = mScheduler.cycle(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
            if(mUserRequest == REQ_SCAN) mScanner.start(cycle, true);
        } else {
            //Bluetooth is off/turning off/turning on - we can't scan.
            mScheduler.cancelScheduled();
            mScanner.stop(true);
        }
        mNotify.issueIfShown(mService);
    }

    @DebugLog
    public void setUserRequest(int request) {
        if(request == mUserRequest) return;
        mUserRequest = request;
        switch (request) {
            case REQ_SCAN:
                if(mBTManager.canScan()) {  //comment for testing on emulator
                    boolean cycle = mScheduler.cycle(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
                    mScanner.start(cycle, true);
                }
                break;
            case REQ_PAUSE:
                mScheduler.cancelScheduled();
                mScanner.stop(true);
                break;
            case REQ_KILL:
                mScheduler.cancelScheduled();
                mScanner.stop(true);
                break;
        }
    }

    @DebugLog
    public void setMode(final int mode) {
        if(mMode == mode) return;
        mMode = mode;
        //Log.v(TAG, "mScanner.isRunning "+mScanner.isRunning());
        boolean save = (mode == BeaconService.SCAN_BACKGROUND || mode == BeaconService.SCAN_LOCKED);
        if(mUserRequest == REQ_SCAN && mBTManager.canScan()) {
            if(mScanner.isRunning()) mScanner.stop(false);
            mScanner.setTiming(intervals[mode], durations[mode], removes[mode], splits[mode], save, mode);
            boolean cycle = mScheduler.cycle(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
            mScanner.start(cycle, false);
        } else {
            mScanner.setTiming(intervals[mode], durations[mode], removes[mode], splits[mode], save, mode);
        }
    }

    public void wakeup(String kind) {
        if(Scheduler.WAKEUP_SCAN.equals(kind)) {
            boolean cycle = mScheduler.cycle(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
            if(mScanner.isRunning()) {
                //TODO this will happen due to onCreate calling setUserRequest after stopSelf and getting wakeup in onStartCommand.
                //TODO not starting in onCreate now. Check if works.
                Log.e(TAG, "Scan running on wakeup. Should not happen.");
                mScanner.stop(true);
                mScanner.start(cycle, true);
            } else {
                mScanner.start(cycle, false);
            }
        } else if(Scheduler.WAKEUP_JIC.equals(kind)) {
            //TODO WAKEUP_JIC - nothing. Or reschedule?
        }
    }

    @DebugLog
    public void setTimingForMode(int mode, long duration, long interval, long remove, long split) {
        durations[mode] = duration;
        intervals[mode] = interval;
        removes[mode] = remove;
        splits[mode] = split;
        if(mMode == mode) {
            boolean save = (mode == BeaconService.SCAN_BACKGROUND || mode == BeaconService.SCAN_LOCKED);
            if(mScanner.isRunning()) {
                mScanner.stop(true);
                mScanner.setTiming(intervals[mode], durations[mode], removes[mode], splits[mode], save, mode);
                boolean cycle = mScheduler.cycle(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
                mScanner.start(cycle, true);
            } else {
                mScanner.setTiming(intervals[mode], durations[mode], removes[mode], splits[mode], save, mode);
            }
        }
    }

    public void setBeaconID(String key, int value) {
        mScanner.setBeaconID(key, value);
    }

    public void setLogging(boolean logging) {
        mScanner.setLogging(logging);
    }

    public int getMode() {
        return mMode;
    }

    public int getUserRequest() {
        return mUserRequest;
    }

    public boolean isLogging() {
        return mScanner.isLogging();
    }

    @Override
    public void onScanStart(int reqState, long duration) {
        mListener.onScanStart(mUserRequest, duration);
    }

    @Override
    public void onScanEnd(LRUMap<ID, Beacon> beacons, int reqState, long interval, int fresh, int dead) {
        mListener.onScanEnd(beacons, mUserRequest, interval, fresh, dead);
        mScheduler.schedule(intervals[mMode], mMode, mNotify.isHighPriority(), mExact);
        mNotify.issueIfShown(mService);
    }

    @Override
    public void onScanResult(Beacon beacon) {
        mListener.onScanResult(beacon);
    }

    private void scheduleWakeupAlarm() {
        long interval = intervals[mMode];
        long duration = durations[mMode];
        boolean cycle = !mScheduler.schedule(interval+duration, mMode, mNotify.isHighPriority(), true);
    }

    //TODO Future - USER_REQ_STOP = Empty mBeacons list in Scanner
    //TODO Future - Separate timing from state and requests
}
