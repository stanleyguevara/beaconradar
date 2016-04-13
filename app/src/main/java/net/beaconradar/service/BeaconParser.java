package net.beaconradar.service;

import android.support.annotation.Nullable;

import java.util.Arrays;

import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.altbeacon.ALT;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;

/**
 * Parses ScanResponse into new RawBeacon object
 * Public methods of this class are thread safe.
 */
public class BeaconParser {
    @SuppressWarnings("unused") private String TAG = getClass().getName();
    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    //-------------------------------------AS SPECIFIED BY BLUETOOTH--------------------------------
    //Scan response length is always 31 octets
    final public static int SR_LEN = 62;

    //Bluetooth Advertising Data Types
    //Can be found at:
    //https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
    @SuppressWarnings("unused") final public static byte AD_TYPE_FLAGS                  = (byte) 0x01;
    @SuppressWarnings("unused") final public static byte AD_TYPE_LIST_UUID_16_COMPLETE  = (byte) 0x03;
    @SuppressWarnings("unused") final public static byte AD_TYPE_LOCAL_NAME_COMPLETE    = (byte) 0x09;
    @SuppressWarnings("unused") final public static byte AD_TYPE_TX_POWER_LEVEL         = (byte) 0x0A;
    final public static byte AD_TYPE_UUID_16                = (byte) 0x16;
    final public static byte AD_TYPE_MANUFACTURER_SPECIFIC  = (byte) 0xFF;

    //Data Type Flags (there's only one 0x06 = 0000 0110)
    //Specification at Bluetooth CSS v6
    //32, 64, 128 reserved.
    @SuppressWarnings("unused") final public static int FLAG_LE_LIMITED_DISCOVERABLE_MODE = 1;
    @SuppressWarnings("unused") final public static int FLAG_LE_GENERAL_DISCOVERABLE_MODE = 2;
    @SuppressWarnings("unused") final public static int FLAG_BR_EDR_NOT_SUPPORTED         = 4;
    @SuppressWarnings("unused") final public static int FLAG_SIMULT_LE_BR_EDR_CONTROLLER  = 8;
    @SuppressWarnings("unused") final public static int FLAG_SIMULT_LE_BR_EDR_HOST        = 16;

    //Company identifiers
    //Full list at:
    //https://www.bluetooth.org/en-us/specification/assigned-numbers/company-identifiers
    final public static byte[] TYPE_IBEACON   = new byte[]{(byte) 0x4C, (byte) 0x00 };
    final public static byte[] TYPE_EDDYSTONE = new byte[]{(byte) 0xAA, (byte) 0xFE };              //Actually not specified on bluetooth.org. Why?
    final public static byte[] TYPE_ALTBEACON = new byte[]{(byte) 0xBE, (byte) 0xAC };
    //-------------------------------------END OF BLUETOOTH SPEC------------------------------------

    //-------------------------------------iBeacon SPEC---------------------------------------------
    //We care only for 1 frame FF4C00
    //All bytes 0-based, inclusive.
    //bytes 5-20    Proximity UUID
    //bytes 21-22   Major
    //bytes 23-24   Minor
    //byte  25      TxPower calibrated at 1m from beacon
    final public static int IBC_PROXUUID_START = 5;
    final public static int IBC_PROXUUID_END   = 20;
    final public static int IBC_MAJOR_1        = 21;
    final public static int IBC_MAJOR_2        = 22;
    final public static int IBC_MINOR_1        = 23;
    final public static int IBC_MINOR_2        = 24;
    final public static int IBC_TX_POWER       = 25;
    //-------------------------------------iBeacon SPEC---------------------------------------------

    //-------------------------------------AltBeacon SPEC-------------------------------------------
    //We care only for 1 frame FFBEAC
    //All bytes 0-based, inclusive.
    //bytes 3-23    20 byte beacon id
    //byte  24      TxPower calibrated at 1m from beacon
    final public static int ALT_ID_START = 3;
    final public static int ALT_ID_END   = 23;
    final public static int ALT_TX_POWER = 24;
    //-------------------------------------AltBeacon SPEC-------------------------------------------

    //-------------------------------------Eddystone SPEC-------------------------------------------
    //We care only for 1 frame (16AAFE), that contains 1 of 3 sub-frames.
    //Eddystone-UID 16AAFE00
    //Eddystone-URL 16AAFE10
    //Eddystone-TLM 16AAFE20
    //All indices 0-based, inclusive.
    final public static int EDD_TYPE_UID = (byte) 0x00;                                             //TODO should be byte, not int.
    final public static int EDD_TYPE_URL = (byte) 0x10;                                             //TODO should be byte, not int.
    final public static int EDD_TYPE_TLM = (byte) 0x20;                                             //TODO should be byte, not int.
    //Indices for all types:
    final public static int EDD_FRM_TYPE = 3;
    final public static int EDD_TX_POWER = 4;
    //Power normalization to 1m instead of 0m (41 dBm is power loss at 1m in air)
    final public static int EDD_TX_NORMALIZATION = -41;
    //--------------UID:
    //byte  3       Frame Type (UID=0x00)
    //byte  4       TxPower calibrated at 0m from beacon
    //bytes 5-14    10 byte Namespace
    //bytes 15-20   6 byte Instance
    final public static int EDD_NAMESPACE_START = 5;
    final public static int EDD_NAMESPACE_END   = 14;
    final public static int EDD_INSTANCE_START  = 15;
    final public static int EDD_INSTANCE_END    = 20;
    final public static int EDD_UID_LEN         = 21;
    //--------------URL:
    //byte 3        Frame Type (URL=0x10)
    //byte 4        TxPower calibrated at 0m from beacon
    //byte 5        URL Scheme (Encoded URL prefix 00, 01, 02, 03)
    //byte 6-LEN    Encoded URL
    final public static int      EDD_URL_PREFIX_INDEX = 5;
    final public static int      EDD_URL_START        = 6;
    final public static byte     EDD_URL_RESERVED_BELOW = (byte) 0x20;
    final public static byte     EDD_URL_RESERVED_ABOVE = (byte) 0x7F;
    final public static String[] EDD_URL_PREFIX_LIST =    { "http://www.", "https://www.", "http://", "https://" };
    final public static String[] EDD_URL_EXPANSION_LIST = {
        ".com/", ".org/", ".edu/", ".net/", ".info/", ".biz/", ".gov/",
        ".com",  ".org",  ".edu",  ".net",  ".info",  ".biz",  ".gov" };
    //--------------TLM:
    //byte 3        Frame Type (TLM=0x20)
    //byte 4        TLM version (0x00)
    //byte 5-6      Battery voltage, 1mV/bit
    //byte 7-8      Beacon temperature
    //byte 9-12     Advertising PDU count
    //byte 13-16    Time since power-on or reboot
    final public static int EDD_TLM_BATT_1 = 5;
    final public static int EDD_TLM_BATT_2 = 6;
    final public static int EDD_TLM_TEMP_1 = 7;
    final public static int EDD_TLM_TEMP_2 = 8;
    final public static int EDD_TLM_CPDU_START = 9;
    final public static int EDD_TLM_CPDU_END = 12;
    final public static int EDD_TLM_CUPT_START = 13;
    final public static int EDD_TLM_CUPT_END = 16;
    //-------------------------------------Eddystone SPEC-------------------------------------------

    //Example
    //02 0106 03   03AAFE 15 16AAFE00DCF7826DA6BC5B71E0893E784B59445748       08 094B6F6E74616B74 02 0AF4 0A 160DD04C377743333064 00000000000000000000		    //Eddystone-UID
    //02 0106 03   03AAFE 17 16AAFE10DC026B6E746B2E696F2F6564647973746F6E65   08 094B6F6E74616B74 02 0AF4 0A 160DD05659736C333064 0000000000000000			    //Eddystone-URL
    //02 0106 03   03AAFE 11 16AAFE20000BA813C001D64081022C4512               08 094B6F6E74616B74 02 0AF4 0A 160DD04C377743333064 0000000000000000000000000000	//Eddystone-TLM
    //02 0106 1A   FF4C00 0215 F7826DA64FA24E988024BC5B71E0893EC07FBC4AB3     08 094B6F6E74616B74 02 0AF4 0A 160DD07A67364C323655 000000000000000000			//iBeacon
    //Specif. BT | Defined by beacon protocol (iBeacon/Eddystone)           | Kontakt (BT spec)   TxPower No idea.

    public BeaconParser() {

    }

    //Parse scanResponse, update beacons hashmap if beacon exist, return new/updated Beacon.
    @Nullable
    public ID parse(byte[] scanResponse, String mac, int rssi) {
        if(scanResponse == null || scanResponse.length != SR_LEN) return null;
        IterableFrame frame = new IterableFrame();
        frame.setArray(scanResponse);
        ID result = null;
        outerloop:
        while (frame.next()) {
            if(frame.length() > 4) {        //To make name check safe, and omit short frames which will not contain any useful info.
                //Log.v(TAG, bytesToHex(mFrame.getRange(0, mFrame.length())));
                switch (frame.get(0)) {
                    case AD_TYPE_UUID_16:
                        if(TYPE_EDDYSTONE[0] == frame.get(1) && TYPE_EDDYSTONE[1] == frame.get(2)) {
                            //We have Eddystone frame. We care only for UID and URL.
                            result = parseEddystone(frame, rssi, mac);
                            break outerloop;
                        }
                        break;
                    case AD_TYPE_MANUFACTURER_SPECIFIC:
                        if(TYPE_IBEACON[0] == frame.get(1) && TYPE_IBEACON[1] == frame.get(2)) {
                            //We have iBeacon frame.
                            result = parseIBeacon(frame, rssi, mac);
                            break outerloop;
                        } else if (TYPE_ALTBEACON[0] == frame.get(1) && TYPE_ALTBEACON[1] == frame.get(2)) {
                            result = parseAltBeacon(frame, rssi, mac);
                        }
                        break;
                }
            }
        }
        return result;
    }

    @Nullable
    private ID parseEddystone(IterableFrame frame, int rssi, String mac) {
        ID parsed;
        if(frame.get(EDD_FRM_TYPE) == EDD_TYPE_UID) {
            //UID Frame
            int compare = EQ.M_UID;
            if(frame.length() >= EDD_UID_LEN && compare != EQ.IGNORE) {
                parsed = new UID(
                        compare,
                        bytesToHex(frame.getRange(EDD_NAMESPACE_START, EDD_NAMESPACE_END)),
                        bytesToHex(frame.getRange(EDD_INSTANCE_START, EDD_INSTANCE_END)),
                        mac,
                        rssi,
                        frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION
                );
                //rawBeacon = new RawBeacon(id, rssi, frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION, false);
            } else {
                //Malformed frame, or EQ = IGNORE
                return null;
            }

        } else if (frame.get(EDD_FRM_TYPE) == EDD_TYPE_URL) {
            //URL Frame
            int compare = EQ.M_URL;
            if(frame.length() >= EDD_URL_START && compare != EQ.IGNORE) {
                //beacon.type = Beacon.TYPE_EDDYSTONE;
                if(frame.get(EDD_URL_PREFIX_INDEX) <= 3 && frame.get(EDD_URL_PREFIX_INDEX) >= 0) {  //Only 0-3 valid
                    //beacon.url = EDD_URL_PREFIX_LIST[frame.get(EDD_URL_PREFIX_INDEX)] + bytesToHex(frame.getRange(EDD_URL_START, frame.length()));
                    parsed = new URL(
                            compare,
                            decodeURL(frame.getRange(EDD_URL_PREFIX_INDEX, frame.length()-1)),
                            mac,
                            rssi,
                            frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION
                    );
                    //rawBeacon = new RawBeacon(id, rssi, frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION, false);
                    //TODO url expansion list
                } else {
                    //Invalid URL prefix. What now?
                    //beacon.url = "Invalid URL prefix";
                    parsed = new URL(
                            compare,
                            "Invalid URL prefix",
                            mac,
                            rssi,
                            frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION
                    );
                    //rawBeacon = new RawBeacon(id, rssi, frame.get(EDD_TX_POWER) + EDD_TX_NORMALIZATION, false);
                }
            } else {
                //Malformed frame, or EQ = IGNORE
                return null;
            }
        } else if (frame.get(EDD_FRM_TYPE) == EDD_TYPE_TLM) {
            //TLM frame
            int compare = EQ.M_TLM;
            //TODO secure length!!! (so it's impossible to call something outside getRange)
            if(compare != EQ.IGNORE) {
                parsed = new TLM(
                        compare,
                        getUInt16(frame.get(EDD_TLM_BATT_1), frame.get(EDD_TLM_BATT_2)),          //1mV per bit
                        (int)frame.get(EDD_TLM_TEMP_1)+(frame.get(EDD_TLM_TEMP_2) & 0xFF)/256.0F, //8:8 fixed point notation to float
                        getUInt32(frame.getRange(EDD_TLM_CPDU_START, EDD_TLM_CPDU_END)),          //PDU counter
                        getUInt32(frame.getRange(EDD_TLM_CUPT_START, EDD_TLM_CUPT_END)),          //Uptime, 0.1s per bit
                        mac,
                        rssi,
                        ID.NO_TX_POWER
                );
            } else {
                //Malformed frame, or EQ = IGNORE
                return null;
            }
        } else {
            //Log.v(TAG, "Unknown EDD frame appeared");
            return null;
        }
        return parsed;
    }

    @Nullable
    private ID parseIBeacon(IterableFrame frame, int rssi, String mac) {
        ID parsed;
        int compare = EQ.M_IBC;
        if(frame.length() >= IBC_TX_POWER && compare != EQ.IGNORE) {
            parsed = new IBC(
                    compare,
                    bytesToHex(frame.getRange(IBC_PROXUUID_START, IBC_PROXUUID_END)),
                    Integer.valueOf(String.valueOf(((frame.get(IBC_MAJOR_1) & 0xff) << 8) | (frame.get(IBC_MAJOR_2) & 0xff))),  //Unsafe as fuck. TODO
                    Integer.valueOf(String.valueOf(((frame.get(IBC_MINOR_1) & 0xff) << 8) | (frame.get(IBC_MINOR_2) & 0xff))),  //Unsafe as fuck. TODO
                    mac,
                    rssi,
                    frame.get(IBC_TX_POWER)
            );
            //parsed = new RawBeacon(id, rssi, frame.get(IBC_TX_POWER), false);
        } else {
            //Malformed frame, or EQ = IGNORE
            return null;
        }
        return parsed;
    }

    @Nullable
    public ID parseAltBeacon(IterableFrame frame, int rssi, String mac) {
        ID parsed;
        int compare = EQ.M_ALT;
        if(frame.length() >= ALT_TX_POWER && compare != EQ.IGNORE) {
            parsed = new ALT(
                    compare,
                    bytesToHex(frame.getRange(ALT_ID_START, ALT_ID_END)),
                    mac,
                    rssi,
                    frame.get(ALT_TX_POWER)
            );
            //parsed = new RawBeacon(id, rssi, frame.get(ALT_TX_POWER), false);
        } else {
            //Malformed frame, or EQ = IGNORE
            return null;
        }
        return parsed;
    }

    private static String decodeURL(byte[] encoded) {
        StringBuilder decoded = new StringBuilder();
        decoded.append(EDD_URL_PREFIX_LIST[encoded[0]]);
        byte prev = -1;
        for (int i = 1; i < encoded.length; i++) {
            if(prev == 0 && encoded[i] == 0) {
                break;
            }
            prev = encoded[i];
            if(encoded[i] < EDD_URL_RESERVED_ABOVE && encoded[i] > EDD_URL_RESERVED_BELOW) {
                //Encoded character
                decoded.append((char)encoded[i]);
            } else {
                //noinspection StatementWithEmptyBody
                if(encoded[i] < (byte) EDD_URL_EXPANSION_LIST.length) {
                    //Suffix, in currently used range.
                    decoded.append(EDD_URL_EXPANSION_LIST[encoded[i]]);
                } else {
                    //Suffix, currently unused range
                }
            }
        }
        return decoded.toString();
    }

    public static long getUInt32(byte[] bytes) {
        long value = bytes[3] & 0xFF;
        value |= (bytes[2] << 8) & 0xFFFF;
        value |= (bytes[1] << 16) & 0xFFFFFF;
        value |= (bytes[0] << 24);
        return value;
    }

    public static int getUInt16(byte first, byte second) {
        return ((first & 0xff) << 8) | (second & 0xff);
    }

    @SuppressWarnings("unused")
    public static String byteToHex(byte b) {
        char[] hexChars = new char[2];
        int v = b & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //Convinience class for iterating over BT Scan Response Frames
    //Frame structure: | 1 byte length | length bytes data |
    private class IterableFrame {                                                                   //TODO enhancement. This could be implemented in ScanResult
        private int pointer = -1;
        private int len = 0;
        private byte[] array;

        public IterableFrame() { }

        public void setArray(byte array[]) {
            pointer = -1;
            len = 0;
            this.array = array;
        }

        //Safe.
        public boolean next() {
            pointer = pointer + len + 1;
            if(array.length > pointer) {
                len = array[pointer];
                //Simplified:
                return len != 0 && array.length > pointer + len;
                //Previously:
                //if(len == 0) return false;
                //return array.length > pointer + len;
            } else {
                return false;
            }
        }

        public int length() {
            return len;
        }

        //Not safe. if called with index above length() may read next frame or OutOfBandException   //TODO enhancement
        public byte get(int index) {
            return array[pointer+index+1];
        }

        //Not safe. if called with index above length() may read next frame or OutOfBandException   //TODO enhancement
        public byte[] getRange(int start, int end) {
            return Arrays.copyOfRange(array, pointer+start+1, pointer+end+2);
        }

    }
}
