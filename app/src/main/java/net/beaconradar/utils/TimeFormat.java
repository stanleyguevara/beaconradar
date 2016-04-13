package net.beaconradar.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeFormat {
    public static final SimpleDateFormat sdf0 = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat sdf1 = new SimpleDateFormat("EEEE HH:mm");
    public static final SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM dd HH:mm");
    public static final SimpleDateFormat sdf3 = new SimpleDateFormat("EEE HH:mm");
    public static final SimpleDateFormat sdf4 = new SimpleDateFormat("MM/dd HH:mm");
    public static final SimpleDateFormat sdf5 = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");
    public static final SimpleDateFormat sdf6 = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final Date date = new Date();

    public static String getTimeAgo(long seen, long diff) {
        if(diff < 60*1000) {    //Less than minute
            //01 s ago, 59 s ago etc
            return String.format("%02d",TimeUnit.MILLISECONDS.toSeconds(diff))+" seconds ago";
        } else if (diff < 24*60*60*1000) {    //Less than day
            //Full hour e.g. 13:37
            date.setTime(seen);
            return "Seen at "+sdf0.format(date);
        } else if (diff < 7*24*60*60*1000) {    //Less than week
            //Tuesday, 13:37
            date.setTime(seen);
            return "Seen "+sdf1.format(date);
        } else {
            date.setTime(seen);
            return "Seen "+sdf2.format(date);
        }
    }

    public static String getTimeAgoShort(long seen, long diff) {
        if(diff < 60*1000) {    //Less than minute
            //01 s ago, 59 s ago etc
            return String.valueOf(TimeUnit.MILLISECONDS.toSeconds(diff)+" s");
        } else if (diff < 24*60*60*1000) {    //Less than day
            //Full hour e.g. 13:37
            date.setTime(seen);
            return sdf0.format(date);
        } else if (diff < 7*24*60*60*1000) {    //Less than week
            //Tue, 13:37
            date.setTime(seen);
            return sdf3.format(date);
        } else {
            date.setTime(seen);
            return sdf4.format(date);
        }
    }

    public static String getDurationBreakdown(long millis)
    {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);
        long milliseconds = TimeUnit.MILLISECONDS.toMillis(millis);

        StringBuilder sb = new StringBuilder(64);
        if(days != 0) sb.append(days).append("d");
        if(hours != 0) sb.append(hours).append("h");
        if(minutes != 0) sb.append(minutes).append("m");
        if(seconds != 0) sb.append(seconds).append("s");
        if(milliseconds != 0) sb.append(milliseconds).append("ms");

        return(sb.toString());
    }

}
