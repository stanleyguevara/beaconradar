package net.beaconradar.settings;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.ArrayRes;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.events.FabBehaviorChangedEvent;
import net.beaconradar.events.ScanOnBluetoothEvent;
import net.beaconradar.events.ServiceExactChange;
import net.beaconradar.events.ServicePriorityChange;
import net.beaconradar.events.ScanTimingChangedEvent;
import net.beaconradar.events.ScanTimingOkEvent;
import net.beaconradar.events.SettingChangedEvent;
import net.beaconradar.events.SettingIntChangedEvent;
import net.beaconradar.fab.FabBehavior;
import net.beaconradar.service.Scheduler;
import net.beaconradar.service.Scheduler18;
import net.beaconradar.service.Scheduler22;
import net.beaconradar.service.SchedulerXX;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.service.BeaconService;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class SettingsPresenterImpl extends MvpBasePresenter<SettingsView> implements SettingsPresenter {
    protected String TAG = getClass().getName();
    protected final Context mAppContext;
    protected final EventBus mBus;
    protected final SharedPreferences mPrefs;
    protected final Scheduler mScheduler;
    protected long[] intervals = new long[3];
    protected long[] durations = new long[3];
    protected long[] removes = new long[3];
    protected long[] splits = new long[3];
    protected int[] interval_progresses = new int[3];
    protected int[] duration_progresses = new int[3];
    protected int[] remove_progresses = new int[3];
    protected int[] split_progresses = new int[3];
    static final String[] mIntervals = new String[] {
            "0 ms", "100 ms", "200 ms", "300 ms", "400 ms", "500 ms", "600 ms", "700 ms", "800 ms", "900 ms",
            "1 s", "2 s", "3 s", "4 s", "5 s", "6 s", "7 s", "8 s", "9 s", "10 s", "20 s", "30 s", "40 s", "50 s",
            "1 min", "2 min", "3 min", "4 min", "5 min", "6 min", "7 min", "8 min", "9 min", "10 min", "15 min", "20 min", "30 min", "40 min", "50 min",
            "1 hr", "2 hr", "3 hr", "4 hr", "5 hr", "6 hr", "7 hr", "8hr", "9 hr", "10 hr", "11 hr", "12 hr", "24 hr"
    };
    static final long[] mIntervalsLong = new long[] {
            0, 100, 200, 300, 400, 500, 600, 700, 800, 900,
            1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 20000, 30000, 40000, 50000,
            1*60000, 2*60000, 3*60000, 4*60000, 5*60000, 6*60000, 7*60000, 8*60000, 9*60000, 10*60000, 15*60000, 20*60000, 30*60000, 40*60000, 50*60000,
            1*60*60000, 2*60*60000, 3*60*60000, 4*60*60000, 5*60*60000, 6*60*60000, 7*60*60000, 8*60*60000, 9*60*60000, 10*60*60000, 11*60*60000, 12*60*60000, 24*60*60000
    };
    static final String[] mRemoves = new String[] {
            "0 ms", "100 ms", "200 ms", "300 ms", "400 ms", "500 ms", "600 ms", "700 ms", "800 ms", "900 ms",
            "1 s", "2 s", "3 s", "4 s", "5 s", "6 s", "7 s", "8 s", "9 s", "10 s", "20 s", "30 s", "40 s", "50 s",
            "1 min", "2 min", "3 min", "4 min", "5 min", "6 min", "7 min", "8 min", "9 min", "10 min", "15 min", "20 min", "30 min", "40 min", "50 min",
            "1 hr", "2 hr", "3 hr", "4 hr", "5 hr", "6 hr", "7 hr", "8hr", "9 hr", "10 hr", "11 hr", "12 hr", "24 hr"
    };
    static final long[] mRemovesLong = new long[] {
            0, 100, 200, 300, 400, 500, 600, 700, 800, 900,
            1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 20000, 30000, 40000, 50000,
            1*60000, 2*60000, 3*60000, 4*60000, 5*60000, 6*60000, 7*60000, 8*60000, 9*60000, 10*60000, 15*60000, 20*60000, 30*60000, 40*60000, 50*60000,
            1*60*60000, 2*60*60000, 3*60*60000, 4*60*60000, 5*60*60000, 6*60*60000, 7*60*60000, 8*60*60000, 9*60*60000, 10*60*60000, 11*60*60000, 12*60*60000, 24*60*60000
    };
    private boolean mFooter;

    private boolean mHighPriority = Defaults.HIGH_PRIORITY_SERVICE;
    private boolean mExact = Defaults.EXACT_SCHEDULING;

    @Inject
    public SettingsPresenterImpl(@ForApplication Context appContext, EventBus bus, SharedPreferences prefs) {
        mAppContext = appContext;
        mBus = bus;
        mPrefs = prefs;

        AlarmManager AM = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        int SDK = Build.VERSION.SDK_INT;
        if(SDK < Build.VERSION_CODES.KITKAT) {
            mScheduler = new Scheduler18(mAppContext, AM);
        } else if(SDK >= Build.VERSION_CODES.KITKAT && SDK <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mScheduler = new Scheduler22(mAppContext, AM);
        } else if(SDK >= Build.VERSION_CODES.M) {
            mScheduler = new SchedulerXX(mAppContext, AM);
        } else mScheduler = null;

        init();
    }

    private void init() {
        mHighPriority = mPrefs.getBoolean(Prefs.KEY_HIGH_PRIORITY_SERVICE, Defaults.HIGH_PRIORITY_SERVICE);
        mExact = mPrefs.getBoolean(Prefs.KEY_EXACT_SCHEDULING, Defaults.EXACT_SCHEDULING);
        readPreferences(BeaconService.SCAN_FOREGROUND);
        readPreferences(BeaconService.SCAN_BACKGROUND);
        readPreferences(BeaconService.SCAN_LOCKED);
        int fabBehavior = mPrefs.getInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR);
        mFooter = (fabBehavior == FabBehavior.FAB_BEHAVIOR_FIXED);
    }

    private void readPreferences(int mode) {
        readDuration(mode);
        readInterval(mode);
        readRemove(mode);
        readSplit(mode);
    }

    protected long readDuration(int m) {
        String keyDuration = Prefs.KEYS_DURATION[m];
        long duration = mPrefs.getLong(keyDuration, Defaults.DURATION[m]);
        duration_progresses[m] = (int) ((duration - 100) / 100);
        durations[m] = duration;
        return durations[m];
    }

    protected long readInterval(int m) {
        String keyInterval = Prefs.KEYS_INTERVAL[m];
        long interval = mPrefs.getLong(keyInterval, Defaults.INTERVAL[m]);
        for(int i = 0; i < mIntervalsLong.length; i++) {
            if(mIntervalsLong[i] == interval) { interval_progresses[m] = i; break; }
        }
        intervals[m] = mIntervalsLong[interval_progresses[m]];
        return intervals[m];
    }

    protected long readRemove(int m) {
        String keyRemove = Prefs.KEYS_REMOVE[m];
        long remove = mPrefs.getLong(keyRemove, Defaults.REMOVE[m]);
        for(int i = 0; i < mRemovesLong.length; i++) {
            if(mRemovesLong[i] == remove) { remove_progresses[m] = i; break; }
        }
        removes[m] = mRemovesLong[remove_progresses[m]];
        return removes[m];
    }

    protected long readSplit(int m) {
        String keySplit = Prefs.KEYS_SPLIT[m];
        long split = mPrefs.getLong(keySplit, Defaults.SPLIT[m]);
        split_progresses[m] = (int) (split -1);
        splits[m] = split;
        return splits[m];
    }

    protected void storeTiming(String key, long time) {
        mPrefs.edit()
                .putLong(key, time)
                .apply();
    }

    @Override
    public boolean storeIntervalDuration(int mode, long interval, long duration, long remove, long split) {
        boolean i_changed = false;
        boolean d_changed = false;
        boolean r_changed = false;
        boolean s_changed = false;

        if(interval != intervals[mode]) {
            i_changed = true;
            storeTiming(Prefs.KEYS_INTERVAL[mode], interval);
            readInterval(mode);    //This should actualize intervals[m] and interval_progresses[m]
        }

        if(remove != removes[mode]) {
            r_changed = true;
            storeTiming(Prefs.KEYS_REMOVE[mode], remove);
            readRemove(mode);    //This should actualize removes[m] and remove_progresses[m]
        }

        if(duration != durations[mode]) {
            d_changed = true;
            storeTiming(Prefs.KEYS_DURATION[mode], duration);
            readDuration(mode);    //This should actualize durations like above
        }

        if(split != splits[mode]) {
            s_changed = true;
            storeTiming(Prefs.KEYS_SPLIT[mode], split);
            readSplit(mode);
        }

        boolean i_ok = verifyInterval(mode, interval);
        boolean d_ok = verifyDuration(mode, duration, split);
        boolean r_ok = verifyRemove(mode, remove, interval, duration);
        boolean s_ok = verifySplit(mode, duration, split);
        boolean sch_ok = true;
        int scheduler_warn = mScheduler.warning(interval, mode, mHighPriority, mExact);
        if(scheduler_warn != 0) sch_ok = false;
        if(!i_ok || !d_ok || !r_ok || !s_ok || !sch_ok) {
            String msg = warningMessage(mode, i_ok, d_ok, r_ok, s_ok, interval, duration, remove, split, scheduler_warn);
            //if (isViewAttached()) getView().showWarning(mode, msg, Snackbar.LENGTH_LONG);
            //mBus.post(new ScanTimingWarningEvent(mode, i_ok, d_ok, r_ok, s_ok, msg));
        } else {
            if (isViewAttached()) getView().dismissWarning(mode);
            mBus.post(new ScanTimingOkEvent(mode));
        }
        if(i_changed || d_changed || r_changed || s_changed) {
            mBus.post(new ScanTimingChangedEvent(mode, interval, duration, remove, split, i_changed, d_changed, r_changed, s_changed));
            if (isViewAttached()) getView().setSummary(mode, getSummaryString(mode));
        }
        return (i_ok && d_ok && r_ok && s_ok);
    }

    @Override
    public boolean verifyInterval(int mode, long interval) {
        return true;
        /*if(interval > Const.INTERVAL_WARN[mode]) return true;
        else {
            if(isViewAttached()) getView().showIntervalWarning(mode, "WTF");
            return false;
        }*/
    }

    @Override
    public boolean verifyDuration(int mode, long duration, long split) {
        return true;
        /*if(duration > Const.DURATION_WARN[mode] && duration / split > Const.DURATION_WARN[mode]) return true;
        else {
            if(isViewAttached()) getView().showDurationWarning(mode, "WTF");
            return false;
        }*/
    }

    @Override
    public boolean verifyRemove(int mode, long remove, long interval, long duration) {
        return true;
        /*if(remove > interval + duration) return true;
        else {
            if(isViewAttached()) getView().showRemoveWarning(mode, "WTF");
            return false;
        }*/
    }

    @Override
    public boolean verifySplit(int mode, long duration, long split) {
        return true;
        /*if(duration / split > Const.DURATION_WARN[mode]) return true;
        else {
            if(isViewAttached()) getView().showSplitWarning(mode, "WTF");
            return false;
        }*/
    }

    @Override
    public boolean isWarning(int mode) {
        return false;
        /*return !verifyInterval(mode, intervals[mode])
                || !verifyDuration(mode, durations[mode], splits[mode])
                || !verifyRemove(mode, removes[mode], intervals[mode], durations[mode]);*/
    }

    protected String warningMessage(int mode, boolean interval_ok, boolean duration_ok, boolean remove_ok, boolean split_ok, long interval, long duration, long remove, long split, int scheduler_warn) {
        String msgDuration = "Duration may be too short";
        String msgInterval = "Interval may be too short";
        String msgRemove = "Inefficient remove time (recommended at least 2x(interval+duration)";
        String msgIdiot = "Duration > Interval, are you sure?"; //only in locked
        //String msgMinute = "Interval + Duration < 60s -> Use high priority service";

        String msgInaccurate = "Timing may be inaccurate.";
        String msgHighBatt = "High battery usage.";
        String msgExtrBatt = "Extreme battery usage.";
        String msgHighPrio = "Consider using high priority service.";

        switch (scheduler_warn) {
            case Scheduler.WARNING_MAY_BE_INACCURATE: return msgInaccurate;
            case Scheduler.WARNING_HIGH_BATT: return msgHighBatt;
            case Scheduler.WARNING_EXTREME_BATT: return msgExtrBatt;
            case Scheduler.WARNING_USE_HP_INSTEAD: return msgHighPrio;
        }
        if(!interval_ok) return msgInterval;
        if(!duration_ok) return msgDuration;
        if(duration > interval) return msgIdiot;
        /*if(mode == BeaconService.SCAN_LOCKED) {
            if(!duration_ok) return msgDuration;
            //if(interval + duration < Const.SUMMARY_WARN[mode]) return msgMinute;
            if(duration > interval) return msgIdiot;
            if(!interval_ok) return msgInterval;
        } else {
            if(!duration_ok) return msgDuration;
            if(duration > interval) return msgIdiot;
            if(!interval_ok) return msgInterval;
        }*/
        if(!remove_ok) return msgRemove;
        return "This may drain battery";
    }

    @DebugLog
    @Override public void setFabBehavior(int behavior) {
        mBus.post(new FabBehaviorChangedEvent(behavior));
    }

    @Override @DebugLog
    public void setCleanupDays(int days) {
        long millis = days * 1000*60*60*24L;
        mPrefs.edit().putLong(Prefs.KEY_CLEANUP_AFTER, millis).apply();
        mBus.post(new SettingIntChangedEvent(Prefs.KEY_CLEANUP_AFTER));
    }

    @DebugLog
    public int getCleanupDays() {
        long millis = mPrefs.getLong(Prefs.KEY_CLEANUP_AFTER, Defaults.CLEANUP_AFTER);
        return (int) (millis / (1000*60*60*24L));
    }

    @Override
    public void restoreDefaults() {
        storeIntervalDuration(BeaconService.SCAN_FOREGROUND,
                Defaults.INTERVAL[BeaconService.SCAN_FOREGROUND],
                Defaults.DURATION[BeaconService.SCAN_FOREGROUND],
                Defaults.REMOVE[BeaconService.SCAN_FOREGROUND],
                Defaults.SPLIT[BeaconService.SCAN_FOREGROUND]);
        storeIntervalDuration(BeaconService.SCAN_BACKGROUND,
                Defaults.INTERVAL[BeaconService.SCAN_BACKGROUND],
                Defaults.DURATION[BeaconService.SCAN_BACKGROUND],
                Defaults.REMOVE[BeaconService.SCAN_BACKGROUND],
                Defaults.SPLIT[BeaconService.SCAN_BACKGROUND]);
        storeIntervalDuration(BeaconService.SCAN_LOCKED,
                Defaults.INTERVAL[BeaconService.SCAN_LOCKED],
                Defaults.DURATION[BeaconService.SCAN_LOCKED],
                Defaults.REMOVE[BeaconService.SCAN_LOCKED],
                Defaults.SPLIT[BeaconService.SCAN_LOCKED]);
        storeListSelection(Prefs.KEY_IBC_ID_METHOD, Defaults.EQ_MODE_IBC, Defaults.EQ_MODE_IBC);
        storeListSelection(Prefs.KEY_UID_ID_METHOD, Defaults.EQ_MODE_UID, Defaults.EQ_MODE_UID);
        storeListSelection(Prefs.KEY_URL_ID_METHOD, Defaults.EQ_MODE_URL, Defaults.EQ_MODE_URL);
        storeListSelection(Prefs.KEY_TLM_ID_METHOD, Defaults.EQ_MODE_TLM, Defaults.EQ_MODE_TLM);
        storeListSelection(Prefs.KEY_ALT_ID_METHOD, Defaults.EQ_MODE_ALT, Defaults.EQ_MODE_ALT);
        setHighPriorityService(Defaults.HIGH_PRIORITY_SERVICE);
        setExactScheduling(Defaults.EXACT_SCHEDULING);
        //No event needed
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_ON_BLUETOOTH, Defaults.SCAN_ON_BLUETOOTH).apply();
        mPrefs.edit().putBoolean(Prefs.KEY_SCAN_ON_BOOT, Defaults.SCAN_ON_BOOT).apply();
        mPrefs.edit().putBoolean(Prefs.KEY_BLUETOOTH_ON_BOOT, Defaults.BLUETOOTH_ON_BOOT).apply();
        mPrefs.edit().putLong(Prefs.KEY_CLEANUP_AFTER, Defaults.CLEANUP_AFTER).apply();
    }

    @Override
    public int getListSelectedPosition(String key, @ArrayRes int value, int def) {
        int v = mPrefs.getInt(key, def);
        int[] values = mAppContext.getResources().getIntArray(value);
        for (int i = 0; i < values.length; i++) {
            if(values[i] == v) return i;
        }
        return -1;
    }

    @Override
    public String getListSelectedText(String key, @ArrayRes int value, @ArrayRes int text, int def) {
        int constant = mPrefs.getInt(key, def);
        int[] val = mAppContext.getResources().getIntArray(value);
        String[] txt = mAppContext.getResources().getStringArray(text);
        for (int i = 0; i < val.length; i++) {
            if(val[i] == constant) return txt[i];
        }
        return "---";
    }

    @Override
    public void storeListSelection(String key, int selected, int def) {
        int previous = mPrefs.getInt(key, def);
        if(previous != selected) {
            mPrefs.edit().putInt(key, selected).apply();
            mBus.post(new SettingChangedEvent(key, selected));
        }
    }

    //Simple getters
    @Override public long getIntervalLong(int mode, int progress) {
        return mIntervalsLong[progress];
    }
    @Override public long getRemoveLong(int mode, int progress) {
        return mRemovesLong[progress];
    }
    @Override public String getInterval(int mode, int progress) {
        return mIntervals[progress];
    }
    @Override public String getRemove(int mode, int progress) {
        return mRemoves[progress];
    }
    @Override public int getIntervalsCount(int mode) {
        return mIntervals.length;
    }
    @Override public int getDurationsCount(int mode) {    //TODO ugly, correct
        return 100;
    }
    @Override public int getRemovesCount(int mode) {
        return mRemoves.length;
    }
    @Override public int getSplitsCount(int mode) {
        return 10;
    }
    @Override public long getIntervalMs(int mode) {
        return intervals[mode];
    }
    @Override public long getDurationMs(int mode) {
        return durations[mode];
    }
    @Override public long getRemoveMs(int mode) {
        return removes[mode];
    }
    @Override public long getSplitTo(int mode) {
        return splits[mode];
    }
    @Override public int getIntervalProgress(int mode) {
        return interval_progresses[mode];
    }
    @Override public int getDurationProgress(int mode) {
        return duration_progresses[mode];
    }
    @Override public int getRemoveProgress(int mode) {
        return remove_progresses[mode];
    }
    @Override public int getSplitProgress(int mode) {
        return split_progresses[mode];
    }

    @Override public String getSummaryString(int mode) {
        if(splits[mode] > 1) {
            return  mIntervals[interval_progresses[mode]]+", "+
                    durations[mode]+" ms / "+splits[mode]+", "+
                    "remove "+mRemoves[remove_progresses[mode]];   //TODO strings.xml
        } else {
            return  mIntervals[interval_progresses[mode]]+", "+
                    durations[mode]+" ms, "+
                    "remove "+mRemoves[remove_progresses[mode]];   //TODO strings.xml
        }
    }

    @Override public void setScanOnBluetooth(boolean scan) {
        mBus.post(new ScanOnBluetoothEvent(scan));
    }

    @Override public void setHighPriorityService(boolean high) {
        mHighPriority = high;
        mPrefs.edit().putBoolean(Prefs.KEY_HIGH_PRIORITY_SERVICE, high).apply();
        mBus.post(new ServicePriorityChange(high));
    }

    @Override
    public void setExactScheduling(boolean exact) {
        mExact = exact;
        mPrefs.edit().putBoolean(Prefs.KEY_EXACT_SCHEDULING, exact).apply();
        mBus.post(new ServiceExactChange(exact));
    }
}
