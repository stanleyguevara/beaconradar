package net.beaconradar.events;

import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ScanEndEvent {
    public final LinkedHashMap<ID, Beacon> beacons;
    public final ArrayList<Beacon> sorted;
    public final boolean newBeacons;
    public final boolean oldBeacons;
    public final long interval;
    public final int request;

    public ScanEndEvent(LinkedHashMap<ID, Beacon> beacons, ArrayList<Beacon> sorted, boolean newBeacons, boolean oldBeacons, long interval, int request) {
        this.beacons = beacons;
        this.sorted = sorted;
        this.newBeacons = newBeacons;
        this.oldBeacons = oldBeacons;
        this.interval = interval;
        this.request = request;
    }
}
