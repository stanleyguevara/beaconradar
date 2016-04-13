package net.beaconradar.settings;

public class SettingReset extends SettingIcon {

    private final String subtitle;

    public SettingReset(String title, String subtitle, int icon) {
        super(KEY_NONE, TYPE_DIALOG_RESET, title, icon);
        this.subtitle = subtitle;
    }

    @Override
    public boolean isWarning() {
        return false;
    }

    @Override
    public String getSummary() {
        return subtitle;
    }
}
