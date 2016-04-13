package net.beaconradar.service.id.eddystone;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;

import net.beaconradar.R;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UID extends ID {
    public Beacon beaconTLM;
    public String namespace;
    public String instance;

    public UID(int eq_mode, String namespace, String instance, String mac, int rssi, int tx) {
        super(T_UID, eq_mode, mac, rssi, tx);
        this.namespace = namespace;
        this.instance = instance;
    }

    /**
     * Called on main thread only, so modified values should be safe to read on main thread too.
     * (Any fields read by worker threads via {@code hashCode} are effectively immutable.)
     * @param update
     */
    @Override
    public void updateID(ID update) {
        UID upd = (UID) update;
        switch (eq_mode) {
            case EQ.FRM: setMac(upd.getMac()); break;
            case EQ.MAC: namespace = upd.namespace; instance = upd.instance; break;
            case EQ.FRMAC: break;
            default: setMac(upd.getMac()); break;
        }
        if(beaconTLM != null) {

        }
        //TODO process beaconTLM
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(251263, 33923);
        hash.append(getType());
        //TODO consider hashing eq_mode
        switch (eq_mode) {
            case EQ.FRM: return hash
                        .append(namespace)
                        .append(instance).toHashCode();
            case EQ.MAC: return hash
                        .append(getMac()).toHashCode();
            case EQ.FRMAC: return hash
                        .append(namespace)
                        .append(instance)
                        .append(getMac()).toHashCode();
            default: return hash
                        .append(namespace)
                        .append(instance).toHashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        UID obj = (UID) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        switch (eq_mode) {
            case EQ.FRM: return equals
                    .append(namespace, obj.namespace)
                    .append(instance, obj.instance).isEquals();
            case EQ.MAC: return equals
                    .append(getMac(), obj.getMac()).isEquals();
            case EQ.FRMAC: return equals
                    .append(namespace, obj.namespace)
                    .append(instance, obj.instance)
                    .append(getMac(), obj.getMac()).isEquals();
            default: return equals
                    .append(namespace, obj.namespace)
                    .append(instance, obj.instance).isEquals();
        }
    }

    @Override
    public String getQueryWhere() {
        switch (eq_mode) { //This has to be instance field, set on creation from static.
            case EQ.FRM:   return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ?";                 //TODO cleanup DB when X records or once per X time.
            case EQ.MAC:   return "type = ? AND eq_mode = ? AND mac = ?";
            case EQ.FRMAC: return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ? AND mac = ?";
            default:       return "type = ? AND eq_mode = ? AND id0 = ? AND id1 = ?";
        }
    }

    @Override
    public String[] getQueryArgs() {
        switch (eq_mode) {
            case EQ.FRM:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), namespace, instance };
            case EQ.MAC:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
            case EQ.FRMAC: return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), namespace, instance, getMac() };
            default:       return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), namespace, instance };
        }
    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.putNull("id2");
        cv.put("mac", getMac());
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.putNull("id2");
        cv.put("mac", getMac());
    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.put("mac", getMac());
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", getEqModeString());
        intent.putExtra("namespace", namespace);
        intent.putExtra("instance", instance);
        intent.putExtra("mac", getMac());
        if(beaconTLM != null) {
            intent.putExtra("batt", ((TLM)beaconTLM.getId()).batt);
            intent.putExtra("temp", ((TLM)beaconTLM.getId()).temp);
            intent.putExtra("cpdu", ((TLM)beaconTLM.getId()).cpdu);
            intent.putExtra("cupt", ((TLM)beaconTLM.getId()).cupt);
        }
    }

    @Override
    public void dressDummyFromDB(Cursor cursor) {

    }

    @Override
    public String getDefaultName() {
        return "UID Eddystone ["+getEqModeString()+"]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_fingerprint;
    }

    protected UID(Parcel in) {
        super(in);
        beaconTLM = in.readParcelable(Beacon.class.getClassLoader());
        namespace = in.readString();
        instance = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(beaconTLM, flags);
        dest.writeString(namespace);
        dest.writeString(instance);
    }

    public static final Creator<UID> CREATOR = new Creator<UID>() {
        @Override
        public UID createFromParcel(Parcel in) {
            return new UID(in);
        }

        @Override
        public UID[] newArray(int size) {
            return new UID[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
