package net.beaconradar.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import java.util.Calendar;

/**
 * Scheduler for API up to (inclusive) 18 JellyBean MR2
 */
public class Scheduler18 extends Scheduler {

    public Scheduler18(Context appContext, AlarmManager alarmManager) {
        super(appContext, alarmManager);
    }

    //TODO jic wakeup
    @Override
    protected boolean schedule(long millis, int mode, boolean foreground, boolean exact) {
        if(mode != BeaconService.SCAN_LOCKED) {
            //For FOREGROUND / BACKGROUND
            if(millis > 60000) {
                return setExact(millis);
            } else {
                return false;
            }
        } else {
            //For LOCKED
            //TODO consider even a second.
            //Question is - is abusing AlarmManager better than EternalWakeLock
            if(millis > 5000) {
                return setExact(millis);
            } else {
                return false;
            }
        }
    }

    @Override
    protected boolean cycle(long millis, int mode, boolean foreground, boolean exact) {
        if(mode != BeaconService.SCAN_LOCKED) {
            //For FOREGROUND / BACKGROUND
            if(millis > 60000) {
                return false;
            } else {
                return true;
            }
        } else {
            //For LOCKED
            if(millis > 5000) {
                return false;
            } else {
                return true;
            }
        }
    }

    //Has to correspond to schedule()
    @Override
    public int warning(long millis, int mode, boolean foreground, boolean exact) {
        if(mode != BeaconService.SCAN_LOCKED) {
            //For FOREGROUND / BACKGROUND
            if(millis > 60000) {
                return WARNING_NONE;
            } else {
                return WARNING_NONE;
            }
        } else {
            //For LOCKED
            if(millis > 5000) {
                if(millis > 5*60000) return WARNING_NONE;
                else return WARNING_HIGH_BATT;
            } else {
                return WARNING_EXTREME_BATT;
            }
        }
    }

    private boolean setExact(long millis) {
        Calendar cal = Calendar.getInstance();
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
        PendingIntent pending = getIntent(WAKEUP_SCAN);
        cal.setTimeInMillis(cal.getTimeInMillis() + millis);
        mAlarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
        return true;
    }
}
