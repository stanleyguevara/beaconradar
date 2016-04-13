package net.beaconradar.utils;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

import net.beaconradar.fab.FabBehavior;

//Just so we can get snackbar height method which was private in original library
public class DefaultFabBehavior extends FabBehavior {

    public DefaultFabBehavior() {
        super();
    }

    @Override
    public void setListener(FabBehaviorListener listener) {
        //Unused
    }

    @Override
    public void hideNow(CoordinatorLayout coordinator, FloatingActionButton child) {
        //Unused
    }

    @Override
    public void showNow(CoordinatorLayout coordinator, FloatingActionButton child) {
        //Unused
    }
}