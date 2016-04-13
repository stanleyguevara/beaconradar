package net.beaconradar.settings;

public abstract class Setting {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_CHECKBOX = 1;
    public static final int TYPE_DIALOG_TIMING = 2;
    public static final int TYPE_DIALOG_LIST = 3;
    public static final int TYPE_DIALOG_RESET = 4;
    public static final int TYPE_DIALOG_INTEGER = 5;
    public static final int TYPE_FOOTER = 6;

    public static final int TYPES_COUNT = TYPE_FOOTER + 1;

    public static final String KEY_NONE = "KEY_NONE";

    protected int type;
    protected String title;
    protected final String key;

    public Setting(String key, int type, String title) {
        this.key = key;
        this.type = type;
        this.title = title;
    }

    public abstract String getSummary();

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }
}
