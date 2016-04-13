package net.beaconradar.utils;

import net.beaconradar.service.BeaconService;

//TODO maybe make it a injectable PrefsController
//TODO so we can init / read / store every preference here as simple function call.
public class Prefs {
    private static final String DURATION = "DURATION";
    private static final String INTERVAL = "INTERVAL";
    private static final String REMOVE = "REMOVE";
    private static final String SPLIT = "SPLIT";

    //Keys for preferences.xml
    public static final String FOREGROUND = "FOREGROUND";
    public static final String BACKGROUND = "BACKGROUND";
    public static final String LOCKED     = "LOCKED";

    public static final String[] KEYS_DURATION = new String [] {
            DURATION+"_"+ BeaconService.SCAN_FOREGROUND,
            DURATION+"_"+BeaconService.SCAN_BACKGROUND,
            DURATION+"_"+BeaconService.SCAN_LOCKED
    };

    public static final String[] KEYS_INTERVAL = new String[] {
            INTERVAL+"_"+BeaconService.SCAN_FOREGROUND,
            INTERVAL+"_"+BeaconService.SCAN_BACKGROUND,
            INTERVAL+"_"+BeaconService.SCAN_LOCKED
    };

    public static final String[] KEYS_REMOVE = new String[] {
            REMOVE+"_"+BeaconService.SCAN_FOREGROUND,
            REMOVE+"_"+BeaconService.SCAN_BACKGROUND,
            REMOVE+"_"+BeaconService.SCAN_LOCKED
    };

    public static final String[] KEYS_SPLIT = new String[] {
            SPLIT+"_"+BeaconService.SCAN_FOREGROUND,
            SPLIT+"_"+BeaconService.SCAN_BACKGROUND,
            SPLIT+"_"+BeaconService.SCAN_LOCKED
    };

    public static final String KEY_INIT_DONE = "INIT_DONE";
    public static final String KEY_FAB_BEHAVIOR = "FAB_BEHAVIOR";

    public static final String KEY_FOOTER_SPACE = "FOOTER_SPACE";   //Holds nothing, just footer.

    public static final String KEY_SORT_NEARBY = "SORT_MODE_NEARBY";

    public static final String KEY_DETAILS_MODE = "MODE_DETAILS";
    public static final String KEY_DETAILS_AVERAGE = "AVERAGE_DETAILS";
    public static final String KEY_DETAILS_SAMPLES = "SAMPLES_DETAILS";

    //Service-related
    public static final String KEY_LOG_ON = "LOG_ON";
    public static final String KEY_LOG_START_TIME = "LOG_START_TIME";
    public static final String KEY_LOG_PAUSE_TIME = "LOG_PAUSE_TIME";

    public static final String KEY_SCAN_REQUESTED = "SCAN_REQUESTED";
    public static final String KEY_SCAN_KILLED = "SCAN_KILLED";
    public static final String KEY_HIGH_PRIORITY_SERVICE = "FOREGROUND_SERVICE";
    public static final String KEY_EXACT_SCHEDULING = "EXACT_SCHEDULING";
    public static final String KEY_LAST_SCAN_MODE = "LAST_SCAN_MODE";
    //public static final String KEY_LAST_USER_REQ = "LAST_USER_REQ";
    public static final String KEY_BLUETOOTH_ON_BOOT = "BLUETOOTH_ON_BOOT";
    public static final String KEY_SCAN_ON_BOOT = "SCAN_ON_BOOT";
    public static final String KEY_MERGE_BY_MAC = "MERGE_BY_MAC";
    public static final String KEY_SCAN_ON_BLUETOOTH = "SCAN_ON_BLUETOOTH";
    public static final String KEY_DETAILS_EXCLUDE = "EXCLUDE_MISSING";
    public static final String KEY_DETAILS_AUTOSCALE = "CHART_AUTOSCALE";
    public static final String KEY_MIN_SENS = "MINIMAL_SENSITIVITY";

    //Identification mode
    public static final String KEY_IBC_ID_METHOD = "ID_IBC_MODE";
    public static final String KEY_UID_ID_METHOD = "ID_UID_MODE";
    public static final String KEY_URL_ID_METHOD = "ID_URL_MODE";
    public static final String KEY_TLM_ID_METHOD = "ID_TLM_MODE";
    public static final String KEY_ALT_ID_METHOD = "ID_ALT_MODE";

    public static final String KEY_PREV_DB_CLEANUP = "PREV_DB_CLEANUP";
    public static final String KEY_CLEANUP_AFTER = "CLEANUP_AFTER";
}
