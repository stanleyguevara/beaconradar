package net.beaconradar.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public abstract class Scheduler {
    public static final String WAKEUP_JIC = "just_in_case_wakeup";
    public static final String WAKEUP_SCAN = "scheduled_scan_wakeup";

    public static final int WARNING_NONE = 0;
    public static final int WARNING_MAY_BE_INACCURATE = 1;
    public static final int WARNING_HIGH_BATT = 2;
    public static final int WARNING_EXTREME_BATT = 3;
    public static final int WARNING_USE_HP_INSTEAD = 4;

    protected final Context mAppContext;
    protected final AlarmManager mAlarm;

    public Scheduler(Context appContext, AlarmManager alarmManager) {
        mAppContext = appContext;
        mAlarm = alarmManager;
    }

    /**
     * Schedules alarm, or lets caller know that it should use EternalWakeLock (TM)
     * @param millis
     * @param mode
     * @param foreground
     * @param exact
     * @return true if scheduled, false if caller should use EternalWakeLock (TM)
     */
    protected abstract boolean schedule(long millis, int mode, boolean foreground, boolean exact);
    protected abstract boolean cycle(long millis, int mode, boolean foreground, boolean exact);
    public abstract int warning(long millis, int mode, boolean foreground, boolean exact);

    //This actually should be used in BeaconService.
    public boolean shouldStopService(long millis, int mode) {
        if(mode == BeaconService.SCAN_FOREGROUND) return false;
        else return millis > 60000;
    }

    public PendingIntent getIntent(String extra) {
        Intent intent = new Intent(mAppContext, ReceiverStatic.class);
        intent.setAction(BeaconService.ACTION_WAKEUP);
        intent.putExtra(BeaconService.EXTRA_KIND, extra);
        //Can't use FLAG_CANCEL_CURRENT due to Samsung 500 alarms limit.
        return PendingIntent.getBroadcast(mAppContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void cancelScheduled() {
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
    }
}
