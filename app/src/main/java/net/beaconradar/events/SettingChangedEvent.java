package net.beaconradar.events;

public class SettingChangedEvent {
    public final String key;
    public final int selected;

    public SettingChangedEvent(String key, int selected) {
        this.key = key;
        this.selected = selected;
    }
}
