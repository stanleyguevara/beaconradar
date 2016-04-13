package net.beaconradar.settings;

import android.support.annotation.ArrayRes;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;

public interface SettingsPresenter extends MvpPresenter<SettingsView> {
    long getIntervalLong(int mode, int progress);
    long getRemoveLong(int mode, int progress);
    String getInterval(int mode, int progress);
    String getRemove(int mode, int progress);
    int getIntervalsCount(int mode);
    int getDurationsCount(int mode);
    int getRemovesCount(int mode);
    int getSplitsCount(int mode);
    long getIntervalMs(int mode);
    long getDurationMs(int mode);
    long getRemoveMs(int mode);
    long getSplitTo(int mode);
    int getIntervalProgress(int mode);
    int getDurationProgress(int mode);
    int getRemoveProgress(int mode);
    int getSplitProgress(int mode);
    String getSummaryString(int mode);
    boolean verifyInterval(int mode, long interval);
    boolean verifyDuration(int mode, long duration, long split);
    boolean verifyRemove(int mode, long remove, long interval, long duration);
    boolean verifySplit(int mode, long duration, long split);
    boolean isWarning(int mode);
    void setFabBehavior(int behavior);
    void setCleanupDays(int days);
    int getCleanupDays();

    void restoreDefaults();

    int getListSelectedPosition(String key, @ArrayRes int value, int def);
    void storeListSelection(String key, int selection, int def);
    String getListSelectedText(String key, @ArrayRes int val, @ArrayRes int txt, int def);

    boolean storeIntervalDuration(int mode, long interval, long duration, long remove, long split);

    void setHighPriorityService(boolean high);
    void setExactScheduling(boolean exact);
    void setScanOnBluetooth(boolean scan);
}
