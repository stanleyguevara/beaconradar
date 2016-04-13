package net.beaconradar.nearby;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.ColorInt;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.events.ScanEndEvent;
import net.beaconradar.events.ScanRequestEvent;
import net.beaconradar.events.ShowFabEvent;
import net.beaconradar.main.ServiceManager;
import net.beaconradar.service.BluetoothListener;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.service.BTManager;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class NearbyPresenterImpl extends MvpBasePresenter<NearbyView> implements NearbyPresenter, BluetoothListener {
    private String TAG = getClass().getName();
    private Context mAppContext;
    private EventBus mBus;
    private SharedPreferences mPrefs;
    private SQLiteDatabase mDB;
    private final ServiceManager mManager;
    private LinkedHashMap<ID, Beacon> mBeacons = new LinkedHashMap<>();
    private ArrayList<Beacon> mSorted = new ArrayList<>();
    //private LinkedList<BeaconWrapper> mLinked = new LinkedList<BeaconWrapper>();

    private int mSortMode = SORT_SPOTTED_NORMAL;
    private Comparator<Beacon> sortComparator;
    private final BTManager mBTManager;

    private int mBTState;
    private boolean mScanReq;
    private int mCount = 0;
    public static final int ERR_NO_SCAN_REQ = 0;
    public static final int ERR_BT_OFF = 1;
    public static final int ERR_BT_TURN_ON = 2;
    public static final int ERR_BT_BUSY = 3;
    public static final int ERR_BT_UNAVAIL = 4;

    @Inject @DebugLog
    public NearbyPresenterImpl(@ForApplication Context context, EventBus bus,
                               SharedPreferences prefs, MainDatabaseHelper helper,
                               BTManager manager, ServiceManager scanManager) {
        mAppContext = context;
        mBus = bus;
        mPrefs = prefs;
        mDB = helper.getWritableDatabase();
        mBTManager = manager;
        mManager = scanManager;
        mSortMode = mPrefs.getInt(Prefs.KEY_SORT_NEARBY, Defaults.SORT_NEARBY);
        sortComparator = getComparator(mSortMode);
    }

    @Override @DebugLog
    public void onResume() {
        ScanEndEvent event = mBus.getStickyEvent(ScanEndEvent.class);
        if(isViewAttached()) {
            if(event != null) {
                onEvent(event);
            } else {
                getView().showState(mScanReq, mBTState, mSorted.isEmpty());
            }
            getView().notifyDataSetChanged();
        }
        mBus.register(this);
    }

    @Override @DebugLog
    public void onPause() {
        mBus.unregister(this);
    }

    @Override @DebugLog
    public void attachView(NearbyView view) {
        super.attachView(view);
        mBTManager.setListenerApp(this);
        mBTState = mBTManager.getBluetoothState();
        mScanReq = mManager.isScanRequested();
    }

    @Override @DebugLog
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        mBTManager.setListenerApp(null);
    }

    @Override @DebugLog
    public void requestBluetoothOn() {
        mBTManager.enable();
    }

    @Override
    public void requestScan() {
        if(!mScanReq) {
            mManager.startScan();
            mScanReq = mManager.isScanRequested();
            if(isViewAttached()) getView().showState(mManager.isScanRequested(), mBTState, mSorted.isEmpty());
        }
    }

    @Override
    public void showFAB() {
        if(mScanReq && mSorted.isEmpty() || !mBTManager.canScan()) mBus.post(new ShowFabEvent(true));
    }

    @Override @DebugLog
    public void onBluetoothStateChanged(int state) {
        if(mBTState == state) return;
        mBTState = state;
        if(isViewAttached()) getView().showState(mScanReq, mBTState, mSorted.isEmpty());
    }

    public void onEvent(ScanRequestEvent event) {
        if(mScanReq == event.scan) return;
        mScanReq = event.scan;
        if(isViewAttached()) getView().showState(mScanReq, mBTState, mSorted.isEmpty());

    }

    @DebugLog
    public void onEvent(ScanEndEvent event) {
        mSorted = event.sorted;
        mBeacons = event.beacons;
        if(isViewAttached()) {
            getView().showState(mScanReq, mBTState, mSorted.isEmpty());
            getView().setData(mSorted);
        }

        //Trigger notify / sort.
        if(event.newBeacons || event.oldBeacons) {
            if(event.newBeacons) Collections.sort(mSorted, sortComparator);
            if(mSortMode == SORT_SEEN_NORMAL || mSortMode == SORT_SEEN_REVERSE ||
                    mSortMode == SORT_NAME_AZ || mSortMode == SORT_NAME_ZA) {
                Collections.sort(mSorted, sortComparator);
            }
            if(isViewAttached()) getView().notifyDataSetChanged();
        } else {
            if(mSortMode == SORT_DISTANCE_NORMAL || mSortMode == SORT_DISTANCE_REVERSE ||
                    mSortMode == SORT_SEEN_NORMAL || mSortMode == SORT_SEEN_REVERSE) {
                Collections.sort(mSorted, sortComparator);
                if(isViewAttached()) getView().notifyDataSetChanged();
            }
        }
        if(isViewAttached()) getView().kickAnim();
    }

    @Override @DebugLog
    public void setSortMode(int mode) {
        mPrefs.edit().putInt(Prefs.KEY_SORT_NEARBY, mode).apply();
        mSortMode = mode;
        sortComparator = getComparator(mode);
        Collections.sort(mSorted, sortComparator);
        if(isViewAttached()) getView().notifyDataSetChanged();
    }

    @Override
    public int getSortMode() {
        return mSortMode;
    }

    @Override
    public void setBeaconName(Beacon wrapper, String name) {
        Beacon changed = mBeacons.get(wrapper.getId());
        if(changed != null) changed.setName(name);
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("user", 1);
        mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
    }

    @Override
    public void setBeaconColor(Beacon wrapper, @ColorInt int color) {
        Beacon changed = mBeacons.get(wrapper.getId());
        if(changed != null) changed.setColor(color);
        ContentValues cv = new ContentValues();
        cv.put("color", color);
        cv.put("user", 1);
        mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
    }

    @Override
    public void setBeaconIcon(Beacon wrapper, int icon) {
        Beacon changed = mBeacons.get(wrapper.getId());
        if(changed != null) changed.setIcon(icon);
        ContentValues cv = new ContentValues();
        String iconResName = mAppContext.getResources().getResourceEntryName(icon);
        cv.put("icon", iconResName);
        cv.put("user", 1);
        int updated = mDB.update("known", cv,
                wrapper.getId().getQueryWhere(),
                wrapper.getId().getQueryArgs());
        //Log.v(TAG, "updated: " + updated);
    }

    private Comparator<Beacon> getComparator(int mode) {
        Comparator<Beacon> comparator;
        switch(mode) {
            case SORT_SPOTTED_NORMAL: comparator = new SpottedComparator(); break;
            case SORT_SPOTTED_REVERSE: comparator = new SpottedComparatorReverse(); break;
            case SORT_SEEN_NORMAL: comparator = new SeenComparator(); break;
            case SORT_SEEN_REVERSE: comparator = new SeenComparatorReverse(); break;
            case SORT_DISTANCE_NORMAL: comparator = new DistanceComparator(); break;
            case SORT_DISTANCE_REVERSE: comparator = new DistanceComparatorReverse(); break;
            case SORT_NAME_AZ: comparator = new NameComparator(); break;
            case SORT_NAME_ZA: comparator = new NameComparatorReverse(); break;
            case SORT_TYPE_NORMAL: comparator = new TypeComparator(); break;
            default: comparator = new SpottedComparator();
        }
        return comparator;
    }

    public class DistanceComparator implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return Double.compare(o1.getDist(), o2.getDist());
        }
    }

    public class DistanceComparatorReverse implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return Double.compare(o2.getDist(), o1.getDist());
        }
    }

    public class SeenComparator implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return (int)(o2.getLastSeen() - o1.getLastSeen());
        }
    }

    public class SeenComparatorReverse implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return (int)(o1.getLastSeen() - o2.getLastSeen());
        }
    }

    public class SpottedComparator implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return (int)(o2.getId().spotted - o1.getId().spotted);
        }
    }

    public class SpottedComparatorReverse implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return (int)(o1.getId().spotted - o2.getId().spotted);
        }
    }

    public class NameComparator implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public class NameComparatorReverse implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return o2.getName().compareTo(o1.getName());
        }
    }

    public class TypeComparator implements Comparator<Beacon> {
        @Override
        public int compare(Beacon o1, Beacon o2) {
            return (o1.getId().getType() - o2.getId().getType());
        }
    }
}