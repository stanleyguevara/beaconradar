package net.beaconradar.service;

import android.os.Build;

public class Clusterfuck {
    final static int SDK = Build.VERSION.SDK_INT;
    public static boolean mForeground = false;
    public static boolean mExact = false;

    public static boolean schedule(long millis) {
        //TODO above KitKat (including) add checkbox in setting "Use inexact alarms"

        if(SDK < Build.VERSION_CODES.KITKAT) {
            //For LOCKED
            //use set() everywhere

            //FOR BACKGROUND  / FOREGROUND
            if(millis > 60000) {
                //set()
            } else {
                //Eternal WakeLock (+JIC ?)                                                         //No warning here
            }

            //FOREGROUND - do not stop service at all.
            //BACKGROUND / LOCKED
            if(millis < 60000); //don't stop service
            else;               //stop service

        } else if(SDK >= Build.VERSION_CODES.KITKAT && SDK < Build.VERSION_CODES.LOLLIPOP_MR1) {

            //Use setExact()
            //Maybe same as below? Only diff is in lowest timing for setExact() (1s here vs 5s below, which is not really useful)
            //Alternative - same as below, but limit for millis is 1000

        } else if(SDK >= Build.VERSION_CODES.LOLLIPOP_MR1 && SDK < Build.VERSION_CODES.M) {
            //For LOCKED
            if(mForeground) {
                if(mExact) {
                    if(millis > 5000) {
                        //setExact()                                                                //High battery usage
                    } else {
                        //Eternal WakeLock (+JIC ?)                                                 //Extreme battery usage
                    }
                } else {
                    //set()
                }
            } else {
                if(mExact) {
                    if(millis > 5000) {
                        //setExact()                                                                //High battery usage
                    } else {
                        //Eternal WakeLock (+JIC ?)                                                 //Extreme battery usage
                    }
                } else {
                    //set()
                }
            }

            //FOR BACKGROUND  / FOREGROUND
            if(millis > 60000) {
                //setExact()
            } else {
                //Eternal WakeLock (+JIC ?)                                                         //No warning here
            }

            //FOREGROUND - do not stop service at all.
            //BACKGROUND / LOCKED
            if(millis < 60000); //don't stop service
            else;               //stop service
        } else if(SDK >= Build.VERSION_CODES.M) {
            //For LOCKED
            //Partial WakeLock requires whitelisting
            if(mForeground) {
                //TODO listen to Doze and minimize app when it starts!
                if(mExact) {
                    if(millis > 5000) {
                        //setExact() (5000 is the limit as fond in web)                             //High battery usage, unable to optimize batt.
                    } else {
                        //Eternal WakeLock (+JIC ?)                                                 //Extreme battery usage
                    }
                } else {
                    if(millis > 5000) {
                        //set()
                    } else {
                        //Eternal WakeLock (+JIC ?)                                                 //Extreme battery usage
                    }
                }
            } else {
                if(mExact) {
                    //Listen for doze and swap alarm in case?
                    if(millis < 60000) {
                        //Eternal WakeLock (+JIC ?)                                                 //Extreme battery usage. Use FG-Service instead.
                    } else {
                        //setExactAndAllowWhileIdle()                                               //May be deferred by Doze (15/9 minutes)
                    }
                } else {
                    //setAndAllowWhileIdle() or just set()
                }
            }

            //FOR BACKGROUND  / FOREGROUND (when there's no Doze)
            if(millis > 60000) {
                //setExact()
            } else {
                //Eternal WakeLock (+JIC ?)                                                         //No warning here
            }

            //FOREGROUND - do not stop service at all.
            //BACKGROUND / LOCKED
            if(millis < 60000); //don't stop service
            else;               //stop service
        }
        return true;
    }

}
