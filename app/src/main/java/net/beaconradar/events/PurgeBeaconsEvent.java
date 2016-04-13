package net.beaconradar.events;

import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PurgeBeaconsEvent {
    public final LinkedHashMap<ID, Beacon> beacons;
    public final ArrayList<Beacon> sorted;

    public PurgeBeaconsEvent(LinkedHashMap<ID, Beacon> beacons, ArrayList<Beacon> sorted) {
        this.beacons = beacons;
        this.sorted = sorted;
    }
}
