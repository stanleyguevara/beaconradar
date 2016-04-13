package net.beaconradar.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import net.beaconradar.base.BaseService;
import net.beaconradar.dagger.App;
import net.beaconradar.utils.Defaults;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.utils.Prefs;

import org.apache.commons.collections4.map.LRUMap;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

public class BeaconService extends BaseService
        implements ScanResultListener, ReceiverDynamic.ScreenListener {
    private final IBinder mBinder = new MyBinder();
    public static final String ACTION_WAKEUP    = "net.beaconradar.WAKEUP";

    public static final String EXTRA_KIND       = "kind";   //Either Scheduler.WAKEUP_SCAN or Scheduler.WAKEUP_JIC
    public static final String ACTION_NOTIF_SCAN = "net.beaconradar.NOTIF_REQ_SCAN";
    public static final String ACTION_NOTIF_PAUSE = "net.beaconradar.NOTIF_REQ_PAUSE";
    public static final String ACTION_NOTIF_KILL = "net.beaconradar.NOTIF_REQ_KILL";
    public static final String ACTION_APP_INIT  = "net.beaconradar.SERVICE_APP_INIT";
    public static final String EXTRA_INIT       = "start";
    public static final String ACTION_SCAN_BOOT = "net.beaconradar.SCAN_ON_BOOT";
    public static final String ACTION_SCAN_BT   = "net.beaconradar.SCAN_ON_BLUETOOTH";
    public static final String ACTION_RESTORE   = "net.beaconradar.RESTART_RESTORE";
    public static final String ACTION_DISMISS_NOTIF = "net.beaconradar.DISMISS_NOTIFICATION";

    public static final int SCAN_UNKNOWN    = -1;
    public static final int SCAN_FOREGROUND =  0;
    public static final int SCAN_BACKGROUND =  1;
    public static final int SCAN_LOCKED     =  2;

    //Listener
    private ServiceScanResultListener mListener;

    //Dependencies
    @Inject protected ScanManager mScanManager;
    @Inject protected SharedPreferences mPrefs;
    @Inject protected ReceiverDynamic mReceiverDynamic;
    @Inject protected BTManager mBTManager;
    @Inject protected NotifyHelper mNotify;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    //State
    private boolean mLocked;
    //This could be unified to mUserRequest with 4 states, like in ScanManager.
    private boolean mScanRequest;
    private boolean mKillRequest;

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Override @DebugLog
    public void onCreate() {
        super.onCreate();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mScanRequest = mPrefs.getBoolean(Prefs.KEY_SCAN_REQUESTED, false);
        mKillRequest = mPrefs.getBoolean(Prefs.KEY_SCAN_KILLED, false);

        mLocked = !isDeviceUnlocked();
        mScanManager.setListener(this, this);
        mReceiverDynamic.setListener(this);
        mReceiverDynamic.register(this);
        int mode = (mLocked ? SCAN_LOCKED : SCAN_BACKGROUND);

        mNotify.setScanManager(mScanManager);
        mScanManager.setMode(mode);
        if(!mKillRequest && mNotify.isHighPriority()) mNotify.updateHighPriority(this, true);

        /*mScanManager.setMode(mode);
        if(mScanRequest && !mKillRequest) {
            mScanManager.setUserRequest(ScanManager.REQ_SCAN);
        }

        mNotify.setScanManager(mScanManager);
        if(mNotify.isHighPriority() && !mKillRequest) mNotify.updateHighPriority(this, true);*/
    }

    @DebugLog
    public void startScan(boolean notify) {
        mScanRequest = true;
        mKillRequest = false;
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, true).apply();
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, false).apply();
        if(notify && mListener != null) mListener.scanRequestChanged(true);
        mScanManager.setUserRequest(ScanManager.REQ_SCAN);
        mNotify.updateHighPriority(this, true);
    }

    @DebugLog
    public void pauseScan(boolean notify) {
        mScanRequest = false;
        mKillRequest = false;
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, false).apply();
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, false).apply();
        if(notify && mListener != null) mListener.scanRequestChanged(false);
        mScanManager.setUserRequest(ScanManager.REQ_PAUSE);
        mNotify.updateHighPriority(this, true);
    }

    @DebugLog
    public void killScan(boolean notify) {    //Ends service.
        mScanRequest = false;
        mKillRequest = true;
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, false).apply();
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, true).apply();
        if(notify && mListener != null) mListener.scanRequestChanged(false);
        mScanManager.setUserRequest(ScanManager.REQ_KILL);
        stopSelfAndCleanup();
    }

    @DebugLog
    public void goForeground() {
        mScanManager.setMode(SCAN_FOREGROUND);
    }

    @DebugLog
    public int goBackground() {
        //App part cannot request background while phone is locked,
        //but it will (to avoid resume/pause jitter when starting new activity).
        //So, setting background when phone is locked is forbidden.
        if(mLocked) return mScanManager.getMode();
        mScanManager.setMode(SCAN_BACKGROUND);
        return mScanManager.getMode();
    }

    @DebugLog
    public void goLocked() {
        mLocked = true;
        mScanManager.setMode(SCAN_LOCKED);
    }

    @Override @DebugLog
    public void onLocked() {
        goLocked();
    }

    @Override @DebugLog
    public void onUnlocked() {
        mLocked = false;
        if(mListener != null) {
            if(mListener.isAppOnTop()) {
                goForeground();
            } else {
                goBackground();
            }
        } else {
            goBackground();
        }
    }

    @DebugLog
    public void setTiming(int mode, long duration, long interval, long remove, long split) {
        mScanManager.setTimingForMode(mode, duration, interval, remove, split);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override @DebugLog
    public void onIdle() {
        if(mPowerManager.isDeviceIdleMode()) {
            if(mListener != null && mListener.isAppOnTop()) mListener.goBackground();
        }
    }

    @DebugLog
    public void setBeaconIdMethod(String key, int value) {
        mScanManager.setBeaconID(key, value);
    }

    public void setLogging(boolean logging) {
        mScanManager.setLogging(logging);
    }

    public void setHighPriority(boolean high) {
        mNotify.setHighPriority(this, high);
    }

    public void setExactScheduling(boolean exact) {
        mScanManager.setExact(exact);
    }

    @Override @DebugLog
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = Service.START_NOT_STICKY;
        try {
            mWakeLock.acquire();
            //Log.v(TAG, "WakeLock acquired");
            if (intent == null) {
                //Resurrection via system restart due to freed memory
                //Log.v(TAG, "Resurrection after system kill");
                if(mScanRequest && !mKillRequest) {
                    startScan(true);
                } else {
                    stopSelfAndCleanup();
                }
            } else {
                String action = intent.getAction();
                switch (action) {
                    case BeaconService.ACTION_WAKEUP:
                        String kind = intent.getStringExtra(EXTRA_KIND);
                        mScanManager.wakeup(kind);
                        ReceiverStatic.completeWakefulIntent(intent);
                        break;
                    case BeaconService.ACTION_NOTIF_SCAN: startScan(true); break;
                    case BeaconService.ACTION_NOTIF_PAUSE: pauseScan(true); break;
                    case BeaconService.ACTION_NOTIF_KILL: killScan(true); break;
                    case BeaconService.ACTION_APP_INIT:
                        boolean start = intent.getBooleanExtra(EXTRA_INIT, false);
                        if(start || mScanRequest) startScan(false);
                        break;
                    case BeaconService.ACTION_SCAN_BT: //Sent only when BT appropriate to scan.
                        startScan(true);
                        ReceiverStatic.completeWakefulIntent(intent);
                        break;
                    case BeaconService.ACTION_SCAN_BOOT:
                        boolean bluetoothOnBoot = mPrefs.getBoolean(
                                Prefs.KEY_BLUETOOTH_ON_BOOT,
                                Defaults.BLUETOOTH_ON_BOOT);
                        if(bluetoothOnBoot) mBTManager.enable();
                        startScan(true);
                        ReceiverStatic.completeWakefulIntent(intent);
                        break;
                    case BeaconService.ACTION_RESTORE:  //Restore scan after phone restart
                        startScan(true);
                        ReceiverStatic.completeWakefulIntent(intent);
                        break;
                    default:
                        Log.e(TAG, "Got intent without action. Should not happen");
                        if(!mScanRequest || mKillRequest) stopSelfAndCleanup();         //TODO ?
                        break;
                }
            }
            if(mScanRequest) result = START_STICKY;
            else if(!mKillRequest && mNotify.isHighPriority()) result = START_STICKY;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWakeLock.release();
            //Log.v(TAG, "WakeLock released");
        }
        return result;
    }

    @Override @DebugLog
    public void onDestroy() {
        super.onDestroy();
        mReceiverDynamic.unregister(this);
    }

    @DebugLog
    private void stopSelfAndCleanup() {
        if(mListener != null) mListener.unbind();
        //TODO remove notification
        mNotify.remove(this);
        stopSelf();
    }

    public boolean isScanRequested() {
        return mScanRequest;
    }

    public boolean isLogging() {
        return mScanManager.isLogging();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onScanStart(int reqState, long duration) {
        if(mListener != null) {
            mListener.onScanStart(reqState, duration);
        }
    }

    @Override
    public void onScanEnd(LRUMap<ID, Beacon> beacons, int reqState, long interval, int fresh, int dead) {
        if(mListener != null) {
            mListener.onScanEnd(beacons, reqState, interval, fresh, dead);
        }
        int mode = mScanManager.getMode();
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            //Below API 21 (including) we should stop service,
            //except when app is visible / extremely short interval+duration (below 5s)
            //if(mode != SCAN_FOREGROUND && )
        } else {

        }
        //if(mode == SCAN_BACKGROUND || mode == SCAN_LOCKED && interval+duration < 60 000) {
        //    stopSelfAndCleanup();
        //}
    }

    @Override
    public void onScanResult(Beacon beacon) {
        if(mListener != null) mListener.onScanResult(beacon);
    }

    public class MyBinder extends Binder {
        public BeaconService getService() { return BeaconService.this; }
    }

    @DebugLog
    public void registerListener(ServiceScanResultListener listener) {
        mListener = listener;
        if(mLocked) return;
        if(listener.isAppOnTop()) {
            goForeground();
        } else {
            goBackground();
        }
    }

    @DebugLog
    public void removeListener(ServiceScanResultListener listener) {
        mListener = null;
    }

    @SuppressWarnings("deprecation") @DebugLog
    private boolean isDeviceUnlocked() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return mPowerManager.isInteractive();
        } else return mPowerManager.isScreenOn();
    }
}