package net.beaconradar.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import java.util.Calendar;

/**
 * Scheduler for API up to (inclusive) 22 Lollipop MR1
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class Scheduler22 extends Scheduler {

    public Scheduler22(Context appContext, AlarmManager alarmManager) {
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
            if(foreground) {
                if(exact) {
                    if(millis > 5000) {
                        return setExact(millis);                                                    //High battery usage
                    } else {
                        return false;                                                               //Extreme battery usage
                    }
                } else {
                    return setInexact(millis);
                }
            } else {
                //Same as above. TODO merge if works.
                if(exact) {
                    if(millis > 5000) {
                        return setExact(millis);                                                    //High battery usage
                    } else {
                        //TODO check if holding lock will work at all
                        return false;                                                               //Extreme battery usage
                    }
                } else {
                    return setInexact(millis);
                }
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
            if(foreground) {
                if(exact) {
                    if(millis > 5000) {
                        return false;                                                    //High battery usage
                    } else {
                        return true;                                                               //Extreme battery usage
                    }
                } else {
                    return false;
                }
            } else {
                if(exact) {
                    if(millis > 5000) {
                        return false;                                                    //High battery usage
                    } else {
                        return true;                                                               //Extreme battery usage
                    }
                } else {
                    return false;
                }
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
            if(foreground) {
                if(exact) {
                    if(millis > 5000) {
                        if(millis > 5*60000) return WARNING_NONE;
                        else return WARNING_HIGH_BATT;                                              //High battery usage
                    } else {
                        return WARNING_EXTREME_BATT;                                                //Extreme battery usage
                    }
                } else {
                    return WARNING_MAY_BE_INACCURATE;
                }
            } else {
                //Same as above. TODO merge if works.
                if(exact) {
                    if(millis > 5000) {
                        if(millis > 5*60000) return WARNING_NONE;
                        else return WARNING_HIGH_BATT;                                              //High battery usage
                    } else {
                        //TODO check if holding lock will work at all
                        return WARNING_EXTREME_BATT;                                                //Extreme battery usage
                    }
                } else {
                    return WARNING_MAY_BE_INACCURATE;
                }
            }
        }
    }

    private boolean setExact(long millis) {
        Calendar cal = Calendar.getInstance();
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
        PendingIntent pending = getIntent(WAKEUP_SCAN);
        cal.setTimeInMillis(cal.getTimeInMillis() + millis);
        mAlarm.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
        return true;
    }

    private boolean setInexact(long millis) {
        Calendar cal = Calendar.getInstance();
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
        PendingIntent pending = getIntent(WAKEUP_SCAN);
        cal.setTimeInMillis(cal.getTimeInMillis() + millis);
        mAlarm.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
        return true;
    }
}
