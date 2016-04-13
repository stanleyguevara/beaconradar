package net.beaconradar.events;

import android.support.v4.app.Fragment;

public class ReplaceMainFragmentEvent {
    public final Fragment fragment;
    public final String tag;

    public ReplaceMainFragmentEvent(Fragment fragment, String tag) {
        this.fragment = fragment;
        this.tag = tag;
    }
}
