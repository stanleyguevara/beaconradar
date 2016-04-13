package net.beaconradar.events;

public class SettingIntChangedEvent {
    public final String key;

    public SettingIntChangedEvent(String key) {
        this.key = key;
    }
}
