package net.beaconradar.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.NotificationCompat;

import net.beaconradar.R;
import net.beaconradar.dagger.ForApplication;
import net.beaconradar.main.MainActivity;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.utils.TimeFormat;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;

@Singleton
public class NotifyHelper {
    private int mNotifID = 1;
    private boolean mShowing = false;   //Remember?

    @Inject Scanner mScanner;
    @Inject BTManager mBTManager;
    ScanManager mScanManager;       //Cyclic dependency, so no injection

    private NotificationManager mNotifyMgr;
    private boolean mHighPriority = false;

    @Inject @DebugLog
    public NotifyHelper(@ForApplication Context appContext, SharedPreferences prefs) {
        mNotifyMgr = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mHighPriority = prefs.getBoolean(Prefs.KEY_HIGH_PRIORITY_SERVICE, Defaults.HIGH_PRIORITY_SERVICE);
    }

    @DebugLog
    public void setScanManager(ScanManager scanManager) {
        this.mScanManager = scanManager;
    }

    @DebugLog
    public void setHighPriority(Service service, boolean high) {
        if(mHighPriority == high) return;
        mHighPriority = high;
        if(high) {
            boolean play = (mScanManager.getUserRequest() != ScanManager.REQ_SCAN);
            Notification notif = getPauseKill(service, getTitle(), getSubtitle(), play);
            service.startForeground(mNotifID, notif);
            mShowing = true;
        } else {
            service.stopForeground(true);
            mShowing = false;
        }
    }

    private String getTitle() {
        StringBuilder title = new StringBuilder();
        title.append("Beacon Radar ");
        switch (mScanManager.getUserRequest()) {
            case ScanManager.REQ_SCAN:
                if(mBTManager.canScan()) title.append("scanning...");
                else title.append("waiting");
                break;
            case ScanManager.REQ_PAUSE: title.append("paused"); break;
            case ScanManager.REQ_KILL: title.append("stopped"); break;
        }
        return title.toString();
    }

    private String getSubtitle() {
        StringBuilder sub = new StringBuilder();
        if(mBTManager.canScan()) {
            if(mScanManager.getUserRequest() != ScanManager.REQ_SCAN) {
                sub.append("Tap play to scan");
            } else {
                sub.append("Found: ")
                .append(mScanner.getFoundCount()).append(" ")
                .append("Visible: ")
                .append(mScanner.getVisibleCount()).append(" ( ")
                .append(TimeFormat.getDurationBreakdown(mScanner.getInterval())).append(", ")
                .append(TimeFormat.getDurationBreakdown(mScanner.getDuration())).append("/")
                .append(mScanner.getSplit()).append(" )");
            }
        } else {
            sub.append("Waiting for Bluetooth ");
            switch (mBTManager.getBluetoothState()) {
                case BluetoothAdapter.STATE_OFF: sub.append("(OFF)"); break;
                case BluetoothAdapter.STATE_TURNING_OFF: sub.append("(OFF...)"); break;
                case BluetoothAdapter.STATE_TURNING_ON: sub.append("(ON...)"); break;
                case BluetoothAdapter.ERROR: sub.append("(Unavailable)");
            }
        }
        return sub.toString();
    }

    @DebugLog
    public void updateHighPriority(Service service, boolean issue) {
        if(mHighPriority) {
            service.startForeground(mNotifID, getNotification(service));
            mShowing = true;
        } else if (issue) {
            issueNotification(service);
        }
    }

    @DebugLog
    public void issueNotification(Service service) {
        mNotifyMgr.notify(mNotifID, getNotification(service));
        mShowing = true;
    }

    public void issueIfShown(Service service) {
        if(mShowing) issueNotification(service);
    }

    public void remove(Service service) {
        if(mHighPriority) {
            service.stopForeground(false);
        } else {
            mNotifyMgr.cancel(mNotifID);
            mShowing = false;
        }
    }

    public void onDismissed() {
        mShowing = false;
    }

    private Notification getNotification(Service service) {
        boolean play = (mScanManager.getUserRequest() != ScanManager.REQ_SCAN);
        //TODO write comprehensive notifications for user
        Notification notif = getPauseKill(service, getTitle(), getSubtitle(), play);
        return notif;
    }

    private static Notification getPauseKill(Context context, String title, String message, boolean play) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent kill = new Intent(context, BeaconService.class);
        kill.setAction(BeaconService.ACTION_NOTIF_KILL);
        Intent pause = new Intent(context, BeaconService.class);
        pause.setAction(BeaconService.ACTION_NOTIF_PAUSE);
        Intent iplay = new Intent(context, BeaconService.class);
        iplay.setAction(BeaconService.ACTION_NOTIF_SCAN);
        Intent delete = new Intent(BeaconService.ACTION_DISMISS_NOTIF);
        PendingIntent kill_pending = PendingIntent.getService(context, 1, kill, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause_pending = PendingIntent.getService(context, 2, pause, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent play_pending = PendingIntent.getService(context, 3, iplay, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent delete_pending = PendingIntent.getBroadcast(context.getApplicationContext(), 6, delete, PendingIntent.FLAG_UPDATE_CURRENT);
        if(play) builder.addAction(R.drawable.ic_play, "Scan", play_pending);
        else builder.addAction(R.drawable.ic_pause, "Suspend", pause_pending);
        builder.setSmallIcon(R.drawable.ic_beacon_radar)
                .setContentTitle(title)
                .setContentText(message)
                .addAction(R.drawable.ic_close, "Stop", kill_pending)
                .setDeleteIntent(delete_pending)
                .setContentIntent(pending);
        return builder.build();
    }

    public boolean isHighPriority() {
        return mHighPriority;
    }
}
