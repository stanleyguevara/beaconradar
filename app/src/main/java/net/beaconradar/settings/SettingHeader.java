package net.beaconradar.settings;

public class SettingHeader extends Setting {

    public SettingHeader(String title) {
        super(KEY_NONE, TYPE_HEADER, title);
    }

    @Override
    public String getSummary() {
        return "Nuffin";
    }
}
