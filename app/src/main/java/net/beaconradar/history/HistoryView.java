package net.beaconradar.history;

import android.database.Cursor;

import com.hannesdorfmann.mosby.mvp.MvpView;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.LinkedHashMap;

public interface HistoryView extends MvpView {
    String getTitle();
    void setBeacons(LinkedHashMap<ID, Beacon> beacons, boolean fresh, boolean dead);
    void swapCursor(Cursor data);
    void showRecording(boolean recording, long started, long paused);
}
