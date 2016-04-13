package net.beaconradar.service.id;

import net.beaconradar.utils.Defaults;

/**
 * Utility class for changing mode of equals and hashCode of beacons.
 */
public class EQ {
    //Available equals / hashcode modes.
    public static final int FRM = 0;        //Available for everyone, except TLM.
    public static final int MAC = 1;        //Available for everyone. Not safe in case of rotating MAC. (TLM has no other safe option, except IGNORE)
    public static final int FRMAC = 2;      //Available for everyone except TLM. Not safe in case of rotating mac.
    public static final int MERGE = 3;      //Available for UID, URL, TLM. Not safe in case of rotating mac.
    public static final int IGNORE = 4;     //Available for everyone. Ignore at parser level.

    //Modes for given type of frame.
    //Volatile may be overkill.
    //TODO this should be read from SharedPreferences on start.

    public static volatile int M_IBC = Defaults.EQ_MODE_IBC;    //Legal for IBC: FRM, MAC, FRMAC, IGNORE
    public static volatile int M_UID = Defaults.EQ_MODE_UID;    //Legal for UID: FRM, MAC, FRMAC, MERGE, IGNORE
    public static volatile int M_URL = Defaults.EQ_MODE_URL;    //Legal for URL: FRM, MAC, FRMAC, MERGE, IGNORE
    public static volatile int M_TLM = Defaults.EQ_MODE_TLM;    //Legal for TLM: MAC, MERGE, IGNORE
    public static volatile int M_ALT = Defaults.EQ_MODE_ALT;    //Legal for ALT: FRM, MAC, FRMAC, IGNORE
}
