package net.beaconradar.main;

import com.hannesdorfmann.mosby.mvp.MvpView;

public interface MainView extends MvpView {
    void displaySnackbar(String key, String message, int duration);
    void clearSnackbar(String key);
}
