package net.beaconradar.settings;

public abstract class SettingIcon extends Setting {
    private final int icon;

    public SettingIcon(String key, int type, String title, int icon) {
        super(key, type, title);
        this.icon = icon;
    }

    public int getIcon() {
        return icon;
    }

    public abstract boolean isWarning();
}
