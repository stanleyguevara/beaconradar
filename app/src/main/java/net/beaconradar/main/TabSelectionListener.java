package net.beaconradar.main;

import android.support.design.widget.TabLayout;

public interface TabSelectionListener {
    void onTabReselected(TabLayout.Tab tab);
    void onTabSelected(TabLayout.Tab tab);
    void onTabUnselected(TabLayout.Tab tab);
}
