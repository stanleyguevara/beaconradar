package net.beaconradar.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import java.util.Calendar;

/**
 * Scheduler for API above (inclusive) 23 Marshmallow
 */
@TargetApi(Build.VERSION_CODES.M)
public class SchedulerXX extends Scheduler {

    public SchedulerXX(Context appContext, AlarmManager alarmManager) {
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
                //TODO listen to Doze and minimize app when it starts!
                if(exact) {
                    if(millis > 5000) {
                        //return setExact(millis);                                                  //High battery usage, unable to optimize batt.
                        //TODO test
                        return setExactIdle(millis);
                    } else {
                        //TODO test. Wakelock might get overriden by Doze.
                        return false;                                                               //Extreme battery usage
                    }
                } else {
                    //Same as above. TODO Merge if works
                    if(millis > 5000) {
                        //return setInexact(millis);
                        //TODO test
                        return setInexactIdle(millis);
                    } else {
                        //TODO test. Wakelock might get overriden by Doze.
                        return false;                                                               //Extreme battery usage
                    }
                }
            } else {
                //This seems shady. If user selects exact but not foreground it will result in worse performance than foreground without exact.
                if(exact) {
                    if(millis < 60000) {
                        //TODO test. Wakelock might get overriden by Doze.
                        return false;                                                               //Extreme battery usage. Use FG-Service instead.
                    } else {
                        return setExactIdle(millis);                                                //May be deferred by Doze (15/9 minutes)
                    }
                } else {
                    return setInexactIdle(millis);
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
                        return false;
                    } else {
                        return true;                                                               //Extreme battery usage
                    }
                } else {
                    if(millis > 5000) {
                        return false;
                    } else {
                        return true;                                                               //Extreme battery usage
                    }
                }
            } else {
                if(exact) {
                    if(millis < 60000) {
                        return true;                                                               //Extreme battery usage. Use FG-Service instead.
                    } else {
                        return false;                                                //May be deferred by Doze (15/9 minutes)
                    }
                } else {
                    return false;
                }
            }
        }
    }

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
                //TODO listen to Doze and minimize app when it starts!
                if(exact) {
                    if(millis > 5000) {
                        //return setExact(millis);                                                  //High battery usage, unable to optimize batt.
                        if(millis > 10*60000) return WARNING_NONE;
                        else return WARNING_HIGH_BATT;
                    } else {
                        return WARNING_EXTREME_BATT;                                                //Extreme battery usage
                    }
                } else {
                    //Same as above. TODO Merge if works
                    if(millis > 5000) {
                        if(millis > 10*60000) return WARNING_NONE;
                        else return WARNING_HIGH_BATT;
                    } else {
                        return WARNING_EXTREME_BATT;                                                //Extreme battery usage
                    }
                }
            } else {
                //This seems shady. If user selects exact but not foreground it will result in worse performance than foreground without exact.
                if(exact) {
                    if(millis < 60000) {
                        //TODO test. Wakelock might get overriden by Doze.
                        return WARNING_USE_HP_INSTEAD;                                              //Extreme battery usage. Use FG-Service instead.
                    } else {
                        if(millis > 10*60000) return WARNING_MAY_BE_INACCURATE;
                        else return WARNING_MAY_BE_INACCURATE;                                      //May be deferred by Doze (15/9 minutes)
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

    private boolean setExactIdle(long millis) {
        Calendar cal = Calendar.getInstance();
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
        PendingIntent pending = getIntent(WAKEUP_SCAN);
        cal.setTimeInMillis(cal.getTimeInMillis() + millis);
        mAlarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
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

    private boolean setInexactIdle(long millis) {
        Calendar cal = Calendar.getInstance();
        mAlarm.cancel(getIntent(WAKEUP_SCAN));
        PendingIntent pending = getIntent(WAKEUP_SCAN);
        cal.setTimeInMillis(cal.getTimeInMillis() + millis);
        mAlarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
        return true;
    }
}
