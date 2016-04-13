package net.beaconradar.history;

import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.LinkedHashMap;

public interface HistoryPresenter extends MvpPresenter<HistoryView> {
    void onPause();
    void onResume();
    @Nullable LinkedHashMap<ID, Beacon> getBeacons();
    void setRecording(boolean recording);
    void deleteLog();
    boolean isRecording();
    boolean isStoringLog();
    long getLogStarted();
    long getLogPaused();

    @Nullable Beacon getDummyBeacon(ID id);
    void setBeaconColor(Beacon wrapper, @ColorInt int color);
    void setBeaconIcon(Beacon wrapper, int icon);
    void setBeaconName(Beacon wrapper, String name);
}
