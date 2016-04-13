package net.beaconradar.details;

import android.support.annotation.Nullable;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

public interface DetailsPresenter extends MvpPresenter<DetailsView> {

    void setIdentificator(ID id);

    void setMode(boolean rssi);
    void setAverage(boolean average);
    void setAutoscale(boolean autoscale);
    void setExcludeMissing(boolean excludeMissing);

    int getSamplesCount();
    boolean getMode();
    boolean getAverage();
    boolean getAutoscale();
    boolean getExcludeMissing();

    @Nullable Beacon getBeacon();
    @Nullable Beacon getStickyBeacon();

    int getColor();
    String getName();
    int getIcon();
    boolean getOnAppeared();
    boolean getOnVisible();
    boolean getOnDisappeared();

    void setColor(int color);
    void setName(String name);
    void setIcon(int icon);
    void setOnAppeared(boolean send);
    void setOnVisible(boolean send);
    void setOnDisappeared(boolean send);

    void forgetBeacon();

    /*String getMac();
    String getTx();
    String getNamespace();
    String getInstance();
    String getLink();
    String getProxUUID();
    int getMajor();
    int getMinor();*/
}
