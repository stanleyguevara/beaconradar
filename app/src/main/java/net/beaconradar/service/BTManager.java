package net.beaconradar.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import net.beaconradar.dagger.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class BTManager extends BroadcastReceiver {
    //private final EventBus mBus;
    private BluetoothManager mBTManager;
    private BluetoothAdapter mBTAdapter;
    private BluetoothListener mListenerService;
    private BluetoothListener mListenerApp;
    private boolean mBTOK = false;

    public static final int STATE_OFF = 10;
    public static final int STATE_TURNING_ON = 11;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;
    //Additional
    public static final int STATE_UNAVAILBLE = 20;


    @Inject
    public BTManager(@ForApplication Context appContext, EventBus bus) {
        super();
        mBTManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if(mBTManager != null) {
            mBTAdapter = mBTManager.getAdapter();
            if(mBTAdapter != null) {
                mBTOK = true;
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        appContext.registerReceiver(this, filter);
    }

    public void setListenerService(BluetoothListener listener) {
        this.mListenerService = listener;
    }

    public void setListenerApp(BluetoothListener listener) {
        this.mListenerApp = listener;
    }

    public int getBluetoothState() {
        if(!mBTOK) return STATE_UNAVAILBLE;
        else return mBTAdapter.getState();
    }

    public boolean canScan() {
        if(!mBTOK) return false;
        else return mBTAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    public boolean enable() {
        if(!mBTOK) return false;
        else return mBTAdapter.enable();
    }

    public boolean disable() {
        if(!mBTOK) return false;
        else return mBTAdapter.disable();
    }


    @Override @DebugLog
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    if(mListenerService != null) mListenerService.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);
                    if(mListenerApp != null) mListenerApp.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);
                    break;
                case BluetoothAdapter.STATE_ON:
                    if(mListenerService != null) mListenerService.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);
                    if(mListenerApp != null) mListenerApp.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    if(mListenerService != null) mListenerService.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);
                    if(mListenerApp != null) mListenerApp.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    if(mListenerService != null) mListenerService.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF);
                    if(mListenerApp != null) mListenerApp.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF);
                    break;
            }
        }
    }
}