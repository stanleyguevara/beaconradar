package net.beaconradar.events;

public class ScanStartEvent {
    public final long duration;

    public ScanStartEvent(long duration) {
        this.duration = duration;
    }
}
