package net.beaconradar.main;

import android.os.Bundle;

import com.hannesdorfmann.mosby.mvp.viewstate.RestorableViewState;

public class MainActivityViewState implements RestorableViewState<MainView> {

    private static final String KEY_STATE = "MAVS-state";

    public static final int STATE_HAMBURGER = 0;
    public static final int STATE_ARROW = 1;

    public int state = STATE_HAMBURGER;

    @Override
    public void saveInstanceState(Bundle out) {
        out.putInt(KEY_STATE, state);
    }

    @Override
    public RestorableViewState<MainView> restoreInstanceState(Bundle in) {
        if(in == null) return null;
        state = in.getInt(KEY_STATE);
        return this;
    }

    @Override
    public void apply(MainView mainView, boolean b) {

    }
}
