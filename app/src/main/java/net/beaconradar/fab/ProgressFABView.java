package net.beaconradar.fab;

import com.hannesdorfmann.mosby.mvp.MvpView;

public interface ProgressFABView extends MvpView {
    void showState(boolean scan);
    void setProgress(float progress, boolean reverse);
    void setBehavior(FabBehavior behavior, boolean animate);
    void restore();
    void hideNow();
    void showNow();
}
