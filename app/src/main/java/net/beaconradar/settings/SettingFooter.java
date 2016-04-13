package net.beaconradar.settings;

public class SettingFooter extends Setting {

    public SettingFooter(String title) {
        super(KEY_NONE, TYPE_FOOTER, title);
    }

    @Override
    public String getSummary() {
        return "Nuffin";
    }
}
