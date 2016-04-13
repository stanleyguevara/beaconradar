package net.beaconradar.settings;

import com.hannesdorfmann.mosby.mvp.MvpView;

public interface SettingsView extends MvpView {
    void setSummary(int mode, String summary);
    void showIntervalWarning(int mode, String message);
    void showDurationWarning(int mode, String message);
    void showRemoveWarning(int mode, String message);
    void showSplitWarning(int mode, String message);
    void showWarning(int mode, String message, int duration);
    void dismissWarning(int mode);

    String getTitle();
}
