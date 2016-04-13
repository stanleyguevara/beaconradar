package net.beaconradar.events;

public class BluetoothStateChangedEvent {
    public final int state;

    public BluetoothStateChangedEvent(int state) {
        this.state = state;
    }
}
