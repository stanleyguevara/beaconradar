package net.beaconradar.events;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

public class ShowBeaconDetails {
    public final Fragment fragment;
    public final ID id;
    @Nullable public final Beacon beacon;

    public ShowBeaconDetails(Fragment fragment, ID id, @Nullable Beacon beacon) {
        this.fragment = fragment;
        this.id = id;
        this.beacon = beacon;
    }
}
