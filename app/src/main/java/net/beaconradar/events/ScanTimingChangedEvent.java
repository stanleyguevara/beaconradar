package net.beaconradar.events;

import hugo.weaving.DebugLog;

public class ScanTimingChangedEvent {
    public final int mode;
    public final long interval;
    public final long duration;
    public final long remove;
    public final long split;
    public final boolean i_change;
    public final boolean d_change;
    public final boolean r_change;
    public final boolean s_change;

    @DebugLog
    public ScanTimingChangedEvent(int mode, long interval, long duration, long remove, long split, boolean i_change, boolean d_change, boolean r_change, boolean s_change) {
        this.mode = mode;
        this.interval = interval;
        this.duration = duration;
        this.remove = remove;
        this.split = split;
        this.i_change = i_change;
        this.d_change = d_change;
        this.r_change = r_change;
        this.s_change = s_change;
    }
}
