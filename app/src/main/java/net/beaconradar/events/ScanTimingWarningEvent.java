package net.beaconradar.events;

public class ScanTimingWarningEvent {
    public final int mode;
    public final boolean interval_ok;
    public final boolean duration_ok;
    public final boolean remove_ok;
    public final boolean split_ok;
    public final String message;

    public ScanTimingWarningEvent(int mode, boolean interval_ok, boolean duration_ok, boolean remove_ok, boolean split_ok, String message) {
        this.mode = mode;
        this.interval_ok = interval_ok;
        this.duration_ok = duration_ok;
        this.remove_ok = remove_ok;
        this.split_ok = split_ok;
        this.message = message;
    }
}
