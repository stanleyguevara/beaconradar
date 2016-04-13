package net.beaconradar.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import net.beaconradar.dagger.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;

/**
 * ACTION_SCREEN_OFF is sent only to dynamically registered receiver.
 * ACTION_DEVICE_IDLE_MODE_CHANGED too
 */
@Singleton
public class ReceiverDynamic extends BroadcastReceiver {
    private ScreenListener mListener;
    private boolean isRegistred;
    @Inject NotifyHelper mNotify;

    public interface ScreenListener {
        void onIdle();
        void onLocked();
        void onUnlocked();
    }

    @Inject
    public ReceiverDynamic(@ForApplication Context appContext) {
        super();
        /*IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        appContext.registerReceiver(this, filter);*/
    }

    public void setListener(ScreenListener listener) {
        mListener = listener;
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(BeaconService.ACTION_DISMISS_NOTIF);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        context.getApplicationContext().registerReceiver(this, filter);
        isRegistred = true;
    }

    public void unregister(Context context) {
        if(isRegistred) context.getApplicationContext().unregisterReceiver(this);
    }

    @Override @DebugLog
    public void onReceive(Context context, Intent intent) {
        if(mListener == null) return;
        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            mListener.onLocked();
        } else if(Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
            mListener.onUnlocked();
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED.equals(intent.getAction())){
                mListener.onIdle();
            }
        } else if(BeaconService.ACTION_DISMISS_NOTIF.equals(intent.getAction())) {
            mNotify.onDismissed();
        }
    }
}
