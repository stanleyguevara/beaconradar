package net.beaconradar.nearby;

import com.hannesdorfmann.mosby.mvp.MvpView;
import net.beaconradar.service.id.Beacon;

import java.util.ArrayList;

public interface NearbyView extends MvpView {
    String getTitle();
    void showState(boolean request, int btState, boolean empty);
    void setData(ArrayList<Beacon> beacons);
    void notifyDataSetChanged();
    void kickAnim();
}
