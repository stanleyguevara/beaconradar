package net.beaconradar.events;

public class ScanFeedbackEvent {
    public final int state;

    public ScanFeedbackEvent(int state) {
        this.state = state;
    }
}
