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

public class URL extends ID {
    public Beacon beaconTLM;
    public String url;

    public URL(int eq_mode, String url, String mac, int rssi, int tx) {
        super(T_URL, eq_mode, mac, rssi, tx);
        this.url = url;
    }

    @Override
    public void updateID(ID update) {
        URL upd = (URL) update;
        switch (eq_mode) {
            case EQ.FRM: setMac(upd.getMac()); break;
            case EQ.MAC: url = upd.url; break;
            case EQ.FRMAC: break;
            default: setMac(upd.getMac()); break;
        }
        //TODO process TLM
    }

    //TODO simplfy hashcode by hashing eq_mode ?
    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(37013, 5711);
        hash.append(getType());
        switch (eq_mode) {
            case EQ.FRM: return hash
                    .append(url).toHashCode();
            case EQ.MAC: return hash
                    .append(getMac()).toHashCode();
            case EQ.FRMAC: return hash
                    .append(url)
                    .append(getMac()).toHashCode();
            default: return hash
                    .append(url).toHashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        URL obj = (URL) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        switch (eq_mode) {
            case EQ.FRM: return equals
                    .append(url, obj.url).isEquals();
            case EQ.MAC: return equals
                    .append(getMac(), obj.getMac()).isEquals();
            case EQ.FRMAC: return equals
                    .append(url, obj.url)
                    .append(getMac(), obj.getMac()).isEquals();
            default: return equals
                    .append(url, obj.url).isEquals();
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
            case EQ.FRM:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), url };
            case EQ.MAC:   return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
            case EQ.FRMAC: return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), url, getMac() };
            default:       return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), url };
        }
    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.put("id0", url);
        cv.put("mac", getMac());
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.put("id0", url);
        cv.put("mac", getMac());
    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("id0", url);
        cv.put("mac", getMac());
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", getEqModeString());
        intent.putExtra("url", url);
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
        return "URL Eddystone ["+getEqModeString()+"]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_link_variant;
    }

    protected URL(Parcel in) {
        super(in);
        beaconTLM = in.readParcelable(Beacon.class.getClassLoader());
        url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(beaconTLM, flags);
        dest.writeString(url);
    }

    public static final Creator<URL> CREATOR = new Creator<URL>() {
        @Override
        public URL createFromParcel(Parcel in) {
            return new URL(in);
        }

        @Override
        public URL[] newArray(int size) {
            return new URL[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
