package net.beaconradar.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;

import net.beaconradar.R;
import net.beaconradar.dagger.App;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.events.ScanEndEvent;
import net.beaconradar.events.ScanFeedbackEvent;
import net.beaconradar.events.ScanRequestEvent;
import net.beaconradar.events.ScanStartEvent;
import net.beaconradar.events.ServiceExactChange;
import net.beaconradar.events.ServicePriorityChange;
import net.beaconradar.events.ScanTimingChangedEvent;
import net.beaconradar.events.SettingChangedEvent;
import net.beaconradar.utils.Prefs;
import net.beaconradar.service.BeaconService;
import net.beaconradar.service.ServiceScanResultListener;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import org.apache.commons.collections4.map.LRUMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class ServiceManager implements ServiceScanResultListener {
    private final Context mAppContext;
    private final EventBus mBus;
    private final SQLiteDatabase mDB;
    private final SharedPreferences mPrefs;
    private BeaconService service;
    private boolean mNewBeacons = false;
    private boolean mOldBeacons = false;
    private Random mRnd;
    private LinkedHashMap<ID, Beacon> mBeacons = new LinkedHashMap<>();
    private ArrayList<Beacon> mSorted = new ArrayList<>();
    private int[] mIcons = new int[] { R.drawable.ic_lock, R.drawable.ic_bluetooth };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            //Log.v("ServiceManager", "service connected");
            BeaconService.MyBinder b = (BeaconService.MyBinder) binder;
            service = b.getService();
            service.registerListener(ServiceManager.this);
            scanRequestChanged(service.isScanRequested());
        }

        public void onServiceDisconnected(ComponentName className) {
            //Log.v("ServiceManager", "service disconnected");
            service.removeListener(ServiceManager.this);
            service = null;
        }
    };

    @Inject
    public ServiceManager(@ForApplication Context context, EventBus bus, MainDatabaseHelper helper, SharedPreferences prefs) {
        mAppContext = context;
        mBus = bus;
        mDB = helper.getWritableDatabase();
        mBus.register(this);    //No unregister here. Lives as long as the application exists.
        mPrefs = prefs;
        mRnd = new Random();
        initService(context, false);
    }

    private void initService(Context context, boolean start) {
        Intent init = new Intent(context, BeaconService.class);
        init.setAction(BeaconService.ACTION_APP_INIT);
        init.putExtra(BeaconService.EXTRA_INIT, start);
        context.startService(init);
        Intent intent = new Intent(context, BeaconService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void startScan() {
        if(service != null) {
            service.startScan(false);
        } else {
            //Service not connected yet. Update prefs to be sure.
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, true).apply();
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, false).apply();
            //Start service and connect
            initService(mAppContext, true);
        }
        mBus.post(new ScanRequestEvent(true));
    }

    public void pauseScan() {
        if(service != null) {
            service.pauseScan(false);
        } else {
            //Service not connected. Update prefs to be sure.
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, false).apply();
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, false).apply();
        }
        mBus.post(new ScanRequestEvent(false));
    }

    public void killScan() {
        if(service != null) {
            service.killScan(false);
        } else {
            //Service not connected. Update prefs to be sure.
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_REQUESTED, false).apply();
            mPrefs.edit().putBoolean(Prefs.KEY_SCAN_KILLED, true).apply();
        }
        mBus.post(new ScanRequestEvent(false));
        unbind();
    }

    public void unbind() {
        service.removeListener(ServiceManager.this);
        service = null;
        mAppContext.unbindService(mConnection);
    }

    public void setModeForeground() {
        if(service != null) service.goForeground();
    }

    public void setModeBackground() {
        if(service != null) service.goBackground();
    }

    @Override @DebugLog
    public boolean isAppOnTop() {
        return ((App)mAppContext.getApplicationContext()).isForeground();
    }

    @Override @DebugLog
    public void goBackground() {
        Intent home = new Intent();
        home.setAction(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mAppContext.startActivity(home);
    }

    @Override @DebugLog
    public void scanRequestChanged(boolean scan) {
        mBus.post(new ScanRequestEvent(scan));
    }

    public void setLogging(boolean logging) {
        if(service != null) {
            service.setLogging(logging);
        }
    }

    public boolean isLogging() {
        if(service != null) {
            return service.isLogging();
        } else {
            return mPrefs.getBoolean(Prefs.KEY_LOG_ON, false);
        }
    }

    @DebugLog
    public boolean isScanRequested() {
        if(service != null) {
            //Log.v("ServiceManager", "service not null");
            return service.isScanRequested();
        } else {
            //Service not connected yet, read prefs.
            boolean requested = mPrefs.getBoolean(Prefs.KEY_SCAN_REQUESTED, false);
            boolean killed = mPrefs.getBoolean(Prefs.KEY_SCAN_KILLED, false);
            return requested && !killed;
        }
    }

    //TODO disconnect
    //if(service != null) service.removeListener(this);

    public void onEvent(SettingChangedEvent event) {
        if(event != null) {
            switch (event.key) {
                case Prefs.KEY_IBC_ID_METHOD:
                case Prefs.KEY_UID_ID_METHOD:
                case Prefs.KEY_URL_ID_METHOD:
                case Prefs.KEY_TLM_ID_METHOD:
                case Prefs.KEY_ALT_ID_METHOD:
                    service.setBeaconIdMethod(event.key, event.selected);
                    break;
            }
        }
    }

    public void onEvent(ServicePriorityChange event) {
        if(service != null) {
            service.setHighPriority(event.high);
        }
    }

    public void onEvent(ServiceExactChange event) {
        if(service != null) service.setExactScheduling(event.exact);
    }

    @DebugLog
    public void onEvent(ScanTimingChangedEvent event) {
        if(service != null) {
            service.setTiming(event.mode, event.duration, event.interval, event.remove, event.split);
        }
    }

    @Override
    public void onNotificationFeedback(int reqState) {
        //if(mLastRequest != reqState) {
            mBus.postSticky(new ScanFeedbackEvent(reqState));
            //mLastRequest = reqState;
        //}
    }

    @Override
    public void onScanStart(int reqState, long duration) {
        mBus.post(new ScanStartEvent(duration));
    }

    @Override
    public void onScanResult(Beacon beacon) {
        //For immediate mode. Unused.
    }

    //TODO if UI blocks offload this procedure to worker thread.
    @Override
    public void onScanEnd(LRUMap<ID, Beacon> incoming, int reqState, long interval, int fresh, int dead) {
        //if(incoming.isEmpty() && mBeacons.isEmpty()) return; //No beacons in range, no beacons to delete from list.
        mNewBeacons = false;
        mOldBeacons = false;
        //Check our map for outdated beacons
        //TODO may be not nescessary in every call
        long curr_time = System.currentTimeMillis();
        if(dead > 0 || mBeacons.size() != incoming.size()) {
            int removed = 0;
            for(Iterator<Beacon> it = mSorted.iterator(); it.hasNext();) {
                Beacon sorted = it.next();
                Beacon arriving = incoming.get(sorted.getId());
                if(arriving == null || arriving != sorted) {
                    //Log.v("RESULTS","Removed");
                    mBeacons.remove(sorted.getId());
                    it.remove();
                    removed++;
                }
            }
        }
        if(fresh > 0 || mBeacons.size() != incoming.size()) {
            for(Iterator<Map.Entry<ID, Beacon>> it = incoming.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ID, Beacon> entry = it.next();
                ID key = entry.getKey();
                Beacon incomingBeacon = entry.getValue();
                Beacon existing = mBeacons.get(key);
                if(existing == null || existing != incomingBeacon) {  //new beacon
                    mBeacons.put(key, incomingBeacon);
                    mSorted.add(incomingBeacon);
                }
            }
        }
        for(Iterator<Map.Entry<ID, Beacon>> it = incoming.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ID, Beacon> entry = it.next();
            ID key = entry.getKey();
            Beacon b = entry.getValue();
            //if(b.getId() instanceof URL && b.getId().getMac().equals("E7:C4:B1:55:79:3E") && b.getId().getEqMode() == EQ.FRMAC){
            //if(b.getId().getType() == ID.T_UID){
                //Log.v("WTF","rssi: "+b.getRssi()+" prev: "+b.getRssiPrev()+" r_max: "+b.getRssiMax()+" r_min: "+b.getRssiMin()+" hot: "+b.isHot());
                //TLM tlm = (TLM) ((URL) b.getId()).beaconTLM.getId();
                //Log.v("WTF","frame TLM: "+(tlm == null ? "null" : "found! "+tlm.batt));
            //}
        }
        mBus.postSticky(new ScanEndEvent(mBeacons, mSorted, fresh > 0, dead > 0, interval, reqState));
    }
}
