package net.beaconradar.nearby;

import android.support.annotation.ColorInt;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import net.beaconradar.service.id.Beacon;

public interface NearbyPresenter extends MvpPresenter<NearbyView> {
    int SORT_SPOTTED_NORMAL = 0;
    int SORT_SPOTTED_REVERSE = 1;
    int SORT_SEEN_NORMAL = 2;
    int SORT_SEEN_REVERSE = 3;
    int SORT_DISTANCE_NORMAL = 4;
    int SORT_DISTANCE_REVERSE = 5;
    int SORT_NAME_AZ = 6;
    int SORT_NAME_ZA = 7;
    int SORT_TYPE_NORMAL = 8;

    void onResume();
    void onPause();

    void setSortMode(int mode);
    int getSortMode();
    void setBeaconColor(Beacon wrapper, @ColorInt int color);
    void setBeaconIcon(Beacon wrapper, int icon);
    void setBeaconName(Beacon wrapper, String name);

    void requestBluetoothOn();
    void requestScan();

    void showFAB();
}
