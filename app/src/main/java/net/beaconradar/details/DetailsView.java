package net.beaconradar.details;

import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;

import com.hannesdorfmann.mosby.mvp.MvpView;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

public interface DetailsView extends MvpView {
    boolean MODE_RSSI = true;
    boolean MODE_DISTANCE = false;

    void setChartData(DetailsData data);

    ID getIdentificator();

    void inflateUI();

    void updateBeaconFixed(@Nullable Beacon beacon);
    void updateBeaconDynamic(@Nullable Beacon beacon);
    void updateBeaconTick(@Nullable Beacon beacon);
    void updateChartTick(@Nullable Beacon beacon);

    void setName(String name);
    void setIcon(int icon);
    void setColor(@ColorInt int color);
}
