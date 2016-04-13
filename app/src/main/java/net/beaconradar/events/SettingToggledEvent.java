package net.beaconradar.events;

public class SettingToggledEvent {
    public final String key;
    public final boolean check;

    public SettingToggledEvent(String key, boolean check) {
        this.key = key;
        this.check = check;
    }
}
