package net.beaconradar.utils;

public class Const {
    public static final long[] INTERVAL_WARN = new long[] {300, 1000, 60000};
    public static final long[] DURATION_WARN = new long[] {300, 300, 300};
    //public static final long[] SUMMARY_WARN = new long[] {1100, 1100, 60000};

    public static final String INTENT_APPEARED    = "net.beaconradar.BEACON_APPEARED";
    public static final String INTENT_VISIBLE     = "net.beaconradar.BEACON_VISIBLE";
    public static final String INTENT_DISAPPEARED = "net.beaconradar.BEACON_DISAPPEARED";

    //Tasker related
    public static final String EXTRA_TYPE = "%type";
    public static final String EXTRA_TYPE_VAL_IBC = "IBC";
    public static final String EXTRA_TYPE_VAL_UID = "UID";
    public static final String EXTRA_TYPE_VAL_URL = "URL";
    public static final String EXTRA_TYPE_VAL_EDD = "EDD";
    public static final String EXTRA_TYPE_VAL_TLM = "TLM";
    public static final String EXTRA_TYPE_VAL_ALT = "ALT";
    public static final String EXTRA_NAME = "%name";
    public static final String EXTRA_MAC = "%mac";
    public static final String EXTRA_TX = "%tx";
    public static final String EXTRA_RAW = "%raw";
    public static final String EXTRA_IBC_MAJOR = "%major";
    public static final String EXTRA_IBC_MINOR = "%minor";
    public static final String EXTRA_IBC_UUID = "%uuid";
    public static final String EXTRA_UID_NAMESPACE = "%namespace";
    public static final String EXTRA_UID_INSTANCE = "%instance";
    public static final String EXTRA_URL_URL = "%url";
}
