package net.beaconradar.main;

import android.os.Bundle;

import com.hannesdorfmann.mosby.mvp.viewstate.RestorableViewState;
import net.beaconradar.service.ScanManager;

public class MainViewState implements RestorableViewState<MainView> {

    private final String KEY_REQUEST = "MainVS_request";

    public int request = ScanManager.REQ_PAUSE;

    @Override
    public void saveInstanceState(Bundle out) {
        out.putInt(KEY_REQUEST, request);
    }

    @Override
    public RestorableViewState<MainView> restoreInstanceState(Bundle in) {
        if(in == null) return null;
        else {
            request = in.getInt(KEY_REQUEST);
            return this;
        }
    }

    @Override
    public void apply(MainView view, boolean retained) {
        //view.indicateScanning(request);
    }
}
