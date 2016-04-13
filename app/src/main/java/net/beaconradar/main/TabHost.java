package net.beaconradar.main;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;

public interface TabHost {
    TabLayout.Tab getTabForFragment(Fragment fragment);
    int getCurrentItem();
}
