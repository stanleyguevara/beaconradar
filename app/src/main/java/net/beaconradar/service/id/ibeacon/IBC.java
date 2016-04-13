package net.beaconradar.service.id.ibeacon;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;

import net.beaconradar.R;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class IBC extends ID {
    public String prox_uuid;
    public int major;
    public int minor;

    public IBC(int eq_mode, String prox_uuid, int major, int minor, String mac, int rssi, int tx) { //Constructor for restoring from DB
        super(T_IBC, eq_mode, mac, rssi, tx);
        this.prox_uuid = prox_uuid;
        this.major = major;
        this.minor = minor;
    }

    @Override
    public void updateID(ID update) {
        IBC upd = (IBC) update;
        switch (eq_mode) {
            case EQ.FRM: setMac(upd.getMac()); break;
            case EQ.MAC: prox_uuid = upd.prox_uuid; major = upd.major; minor = upd.minor; break;
            case EQ.FRMAC: break;
            default: setMac(upd.getMac()); break;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(58111, 33811);
        hash.append(getType());
        switch (eq_mode) {
            case EQ.FRM: return hash
                    .append(prox_uuid)
                    .append(major)
                    .append(minor).toHashCode();
            case EQ.MAC: return hash
                    .append(getMac()).toHashCode();
            case EQ.FRMAC: return hash
                    .append(prox_uuid)
                    .append(major)
                    .append(minor)
                    .append(getMac()).toHashCode();
            default: return hash
                    .append(prox_uuid)
                    .append(major)
                    .append(minor).toHashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        IBC obj = (IBC) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        switch (eq_mode) {
            case EQ.FRM: return equals
                    .append(prox_uuid, obj.prox_uuid)
                    .append(major, obj.major)
                    .append(minor, obj.minor).isEquals();
            case EQ.MAC: return equals
                    .append(getMac(), obj.getMac()).isEquals();
            case EQ.FRMAC: return equals
                    .append(prox_uuid, obj.prox_uuid)
                    .append(major, obj.major)
                    .append(minor, obj.minor)
                    .append(getMac(), obj.getMac()).isEquals();
            default: return equals
                    .append(prox_uuid, obj.prox_uuid)
                    .append(major, obj.major)
                    .append(minor, obj.minor).isEquals();
        }
    }

    @Override
    public String getQueryWhere() {
        switch (eq_mode) {
            case EQ.FRM:   return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ? and id2 = ?";
            case EQ.MAC:   return "type = ? AND eq_mode = ? AND mac = ?";
            case EQ.FRMAC: return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ? AND id2 = ? AND mac = ?";
            default:       return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ? and id2 = ?";
        }
    }

    @Override
    public String[] getQueryArgs() {
        switch (eq_mode) {
            case EQ.FRM:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), prox_uuid, String.valueOf(major), String.valueOf(minor)};
            case EQ.MAC:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
            case EQ.FRMAC: return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), prox_uuid, String.valueOf(major), String.valueOf(minor), getMac()};
            default:       return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), prox_uuid, String.valueOf(major), String.valueOf(minor)};
        }
    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.put("mac", getMac());
        cv.put("id0", prox_uuid);
        cv.put("id1", major);
        cv.put("id2", minor);
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.put("mac", getMac());
        cv.put("id0", prox_uuid);
        cv.put("id1", major);
        cv.put("id2", minor);
    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("mac", getMac());
        cv.put("id0", prox_uuid);
        cv.put("id1", major);
        cv.put("id2", minor);
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", getEqModeString());
        intent.putExtra("mac", getMac());
        intent.putExtra("uuid", prox_uuid);
        intent.putExtra("major", major);
        intent.putExtra("minor", minor);
    }

    @Override
    public void dressDummyFromDB(Cursor cursor) {

    }

    @Override
    public String getDefaultName() {
        return "iBeacon ["+getEqModeString()+"]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_apple2_full;
    }

    protected IBC(Parcel in) {
        super(in);
        prox_uuid = in.readString();
        major = in.readInt();
        minor = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(prox_uuid);
        dest.writeInt(major);
        dest.writeInt(minor);
    }

    public static final Creator<IBC> CREATOR = new Creator<IBC>() {
        @Override
        public IBC createFromParcel(Parcel in) {
            return new IBC(in);
        }

        @Override
        public IBC[] newArray(int size) {
            return new IBC[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
