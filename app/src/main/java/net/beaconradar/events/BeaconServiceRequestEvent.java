package net.beaconradar.events;

public class BeaconServiceRequestEvent {
    public final int request;

    public BeaconServiceRequestEvent(int request) {
        this.request = request;
    }
}
