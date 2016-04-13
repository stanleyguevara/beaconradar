package net.beaconradar.utils;

import net.beaconradar.R;
import net.beaconradar.details.DetailsView;
import net.beaconradar.fab.FabBehavior;
import net.beaconradar.nearby.NearbyPresenter;
import net.beaconradar.service.id.EQ;

public class Defaults {
    public static final long[] DURATION = new long[] { 300, 2000, 2000  };
    public static final long[] INTERVAL = new long[] { 300,  8000, 15*60000 };
    public static final long[] REMOVE = new long[] { 60000,  5*60000, 30*60000 };
    public static final long[] SPLIT = new long[] { 1, 4, 4 };
    public static final int FAB_BEHAVIOR = FabBehavior.FAB_BEHAVIOR_SCALE;

    public static final int SORT_NEARBY = NearbyPresenter.SORT_SPOTTED_NORMAL;

    public static final int MIN_SENS = -100;

    public static final boolean HIGH_PRIORITY_SERVICE = false;
    public static final boolean EXACT_SCHEDULING = false;
    public static final boolean MERGE_MAC = false;
    public static final boolean SCAN_ON_BLUETOOTH = false;
    public static final boolean SCAN_ON_BOOT = false;
    public static final boolean BLUETOOTH_ON_BOOT = false;
    public static final boolean DETAILS_MODE = DetailsView.MODE_DISTANCE;
    public static final int DETAILS_SAMPLES = 10;
    public static final boolean DETAILS_AVERAGE = true;
    public static final boolean DETAILS_EXCLUDE = true;
    public static final boolean DETAILS_AUTOSCALE = true;

    public static final int UNKNOWN_ICON = R.drawable.ic_help_circle;

    public static final int EQ_MODE_IBC = EQ.FRM;
    public static final int EQ_MODE_UID = EQ.FRM;
    public static final int EQ_MODE_URL = EQ.FRM;
    public static final int EQ_MODE_TLM = EQ.MAC;
    public static final int EQ_MODE_ALT = EQ.FRM;

    public static final long CLEANUP_AFTER = 2592000000L;  //30 days
}
