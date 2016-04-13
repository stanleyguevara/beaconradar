package net.beaconradar.events;

public class ScanRequestEvent {
    public final boolean scan;

    public ScanRequestEvent(boolean scan) {
        this.scan = scan;
    }
}
