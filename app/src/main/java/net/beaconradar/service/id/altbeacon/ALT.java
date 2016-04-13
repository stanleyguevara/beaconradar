package net.beaconradar.service.id.altbeacon;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import net.beaconradar.R;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressLint("ParcelCreator")
public class ALT extends ID implements Parcelable {
    private String beacon_id;

    public ALT(int eq_mode, String beacon_id, String mac, int rssi, int tx) {
        super(T_ALT, eq_mode, mac, rssi, tx);
        this.beacon_id = beacon_id;
    }

    @Override
    public void updateID(ID update) {
        //Nuffin
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(182089, 50227);    //TODO choose primes (now collide with URL)
        hash.append(getType());
        switch (eq_mode) {
            case EQ.FRM: return hash
                    .append(beacon_id).toHashCode();
            case EQ.MAC: return hash
                    .append(getMac()).toHashCode();
            case EQ.FRMAC: return hash
                    .append(beacon_id)
                    .append(getMac()).toHashCode();
            default: return hash
                    .append(beacon_id).toHashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        ALT obj = (ALT) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        switch (eq_mode) {
            case EQ.FRM: return equals
                    .append(beacon_id, obj.beacon_id).isEquals();
            case EQ.MAC: return equals
                    .append(getMac(), obj.getMac()).isEquals();
            case EQ.FRMAC: return equals
                    .append(beacon_id, obj.beacon_id)
                    .append(getMac(), obj.getMac()).isEquals();
            default: return equals
                    .append(beacon_id, obj.beacon_id).isEquals();
        }
    }

    @Override
    public String getQueryWhere() {
        switch (eq_mode) {
            case EQ.FRM: return   "type = ? AND eq_mode = ? AND id0 = ?";
            case EQ.MAC: return   "type = ? AND eq_mode = ? AND mac = ?";
            case EQ.FRMAC: return "type = ? AND eq_mode = ? AND id0 = ? AND mac = ?";
            default: return       "type = ? AND eq_mode = ? AND id0 = ?";
        }
    }

    @Override
    public String[] getQueryArgs() {
        switch (eq_mode) {
            case EQ.FRM:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), beacon_id };
            case EQ.MAC:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
            case EQ.FRMAC: return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), beacon_id, getMac() };
            default:       return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), beacon_id };
        }    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.put("id0", beacon_id);
        cv.putNull("id1");
        cv.putNull("id2");
        cv.put("mac", getMac());
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.put("id0", beacon_id);
        cv.putNull("id1");
        cv.putNull("id2");
        cv.put("mac", getMac());
    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("id0", beacon_id);
        cv.put("mac", getMac());
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", getEqModeString());
        intent.putExtra("beacon_id", beacon_id);
        intent.putExtra("mac", getMac());
    }

    @Override
    public void dressDummyFromDB(Cursor cursor) {

    }

    @Override
    public String getDefaultName() {
        return "AltBeacon ["+getEqModeString()+"]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_hexagon_outline;
    }

    protected ALT(Parcel in) {
        super(in);
        beacon_id = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(beacon_id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ALT> CREATOR = new Creator<ALT>() {
        @Override
        public ALT createFromParcel(Parcel in) {
            return new ALT(in);
        }

        @Override
        public ALT[] newArray(int size) {
            return new ALT[size];
        }
    };
}
