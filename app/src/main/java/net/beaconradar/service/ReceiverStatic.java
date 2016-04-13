package net.beaconradar.service;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;

import hugo.weaving.DebugLog;

public class ReceiverStatic extends WakefulBroadcastReceiver {

    @Override @DebugLog
    public void onReceive(Context context, Intent intent) {
        Intent wakeful = new Intent(context, BeaconService.class);
        String action = intent.getAction();
        wakeful.setAction(action);
        switch (action) {
            case BeaconService.ACTION_WAKEUP:
                String kind = intent.getStringExtra(BeaconService.EXTRA_KIND);
                wakeful.putExtra(BeaconService.EXTRA_KIND, kind);
                startWakefulService(context, wakeful);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        boolean scanOnBT = prefs.getBoolean(Prefs.KEY_SCAN_ON_BLUETOOTH, Defaults.SCAN_ON_BLUETOOTH);
                        boolean mRequested = prefs.getBoolean(Prefs.KEY_SCAN_REQUESTED, false);
                        boolean mKilled = prefs.getBoolean(Prefs.KEY_SCAN_KILLED, false);
                        if(scanOnBT && !mRequested && !mKilled) {
                            wakeful.setAction(BeaconService.ACTION_SCAN_BT);
                            startWakefulService(context, wakeful);
                        }
                        break;
                }
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean scanOnBoot = prefs.getBoolean(
                        Prefs.KEY_SCAN_ON_BOOT,
                        Defaults.SCAN_ON_BOOT);
                if(scanOnBoot) {
                    wakeful.setAction(BeaconService.ACTION_SCAN_BOOT);
                    startWakefulService(context, wakeful);
                } else {
                    boolean mRequested = prefs.getBoolean(Prefs.KEY_SCAN_REQUESTED, false);
                    boolean mKilled = prefs.getBoolean(Prefs.KEY_SCAN_KILLED, false);
                    if(mRequested && !mKilled) {
                        //May occur due to e.g. phone restart.
                        wakeful.setAction(BeaconService.ACTION_RESTORE);
                        startWakefulService(context, wakeful);
                    }
                }
                break;
        }
    }
}
