package net.beaconradar.fab;

import com.hannesdorfmann.mosby.mvp.MvpPresenter;

public interface ProgressFABPresenter extends MvpPresenter<ProgressFABView> {
    void setState(boolean scan);
    boolean getState();
    boolean needsFooter();
}
