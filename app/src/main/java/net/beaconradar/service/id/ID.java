package net.beaconradar.service.id;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class and all descendants should have identifying members effectively immutable.
 */
public abstract class ID implements Parcelable {
    public static final int T_IBC = 100;
    public static final int T_EDD = 200;    //For MAC identification case.
    public static final int T_UID = 300;
    public static final int T_URL = 400;
    public static final int T_TLM = 500;
    public static final int T_ALT = 600;

    public static final int NO_TX_POWER = -127;

    private final int TYPE;
    protected final int eq_mode;
    public long spotted;    //Should be final. Temporary relaxed for restoring beacons from nearby table.
    public final int rssi;
    public final int tx;
    private String mac;

    public ID(int type, int eq_mode, String mac, int rssi, int tx) {
        this.TYPE = type;
        this.eq_mode = eq_mode;
        this.mac = mac;
        this.spotted = System.currentTimeMillis();
        this.rssi = rssi;
        this.tx = tx;
    }

    protected ID(Parcel in) {
        TYPE = in.readInt();
        eq_mode = in.readInt();
        mac = in.readString();
        spotted = in.readLong();
        rssi = in.readInt();
        tx = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(TYPE);
        dest.writeInt(eq_mode);
        dest.writeString(mac);
        dest.writeLong(spotted);
        dest.writeInt(rssi);
        dest.writeInt(tx);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /*public static final Creator<ID> CREATOR = new Creator<ID>() {
        @Override
        public ID createFromParcel(Parcel in) {
            return new ID(in);
        }

        @Override
        public ID[] newArray(int size) {
            return new ID[size];
        }
    };*/

    public String getMac() {
        return this.mac;
    }
    public void setMac(String mac) { this.mac = mac; }  //TODO can't be public.

    public int getType() {
        return TYPE;
    }

    public long getSpotted() {
        return spotted;
    }

    public int getEqMode() {
        return eq_mode;
    }

    public String getEqModeString() {
        switch (eq_mode) {
            case EQ.FRM: return "FRM";
            case EQ.MAC: return "MAC";
            case EQ.FRMAC: return "FRMAC";
            case EQ.MERGE: return "MERGE";
        }
        return "NONE";
    }

    public abstract String getQueryWhere();
    public abstract String[] getQueryArgs();
    public abstract void fillCV(ContentValues cv);
    public abstract void fillLogCV(ContentValues cv);
    public abstract void fillUpdateCV(ContentValues cv);    //With current table indexing it's pointless, and blocks opportunity to use prepared statement (which are not used now anyway)
    public abstract void putExtras(Intent intent);
    public abstract void dressDummyFromDB(Cursor cursor);
    public abstract String getDefaultName();
    public abstract int getDefaultIcon();
    public abstract void updateID(ID update);
}
