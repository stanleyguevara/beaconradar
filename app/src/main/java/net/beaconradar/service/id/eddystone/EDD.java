package net.beaconradar.service.id.eddystone;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;

import net.beaconradar.R;
import net.beaconradar.service.id.EQ;
import net.beaconradar.service.id.ID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.concurrent.TimeUnit;

@SuppressLint("ParcelCreator")
public class EDD extends ID {
    public String namespace;
    public String instance;
    public String url;
    public TLM frameTLM;
    //public int batt;
    //public float temp;
    //public long cpdu;
    //public long cupt;

    public String getBattString() {
        if(frameTLM != null) return String.valueOf(frameTLM.batt/1000.0f)+" V";
        else return "N/A";
    }

    public String getTempString() {
        if(frameTLM != null) return String.format("%.5f", frameTLM.temp)+" °C";
        else return "N/A";
    }

    public String getCPDUString() {
        if(frameTLM != null) return String.valueOf(frameTLM.cpdu);
        else return "N/A";
    }

    public String getCUPTString() {
        if(frameTLM != null) return getDurationBreakdown(frameTLM.cupt * 100);
        else return "N/A";
    }

    public EDD(String mac, int rssi, int tx) {
        super(ID.T_EDD, EQ.MAC, mac, rssi, tx);
    }

    @Override
    public void updateID(ID update) {
        switch (update.getType()) {
            case ID.T_UID:
                UID uid = (UID) update;
                this.namespace = uid.namespace;
                this.instance = uid.instance;
                break;
            case ID.T_URL:
                URL url = (URL) update;
                this.url = url.url;
                break;
            case ID.T_TLM:
                TLM tlm = (TLM) update;
                //this.batt = tlm.batt;
                //this.temp = tlm.temp;
                //this.cpdu = tlm.cpdu;
                //this.cupt = tlm.cupt;
                this.frameTLM = tlm;
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder(165443, 15137);
        //TODO consider hashing eq_mode
        return hash.append(getType())
                   .append(getMac()).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        EDD obj = (EDD) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        return equals.append(getMac(), obj.getMac()).isEquals();
    }

    @Override
    public String getQueryWhere() {
        return "type = ? AND eq_mode = ? AND mac = ?";
    }

    @Override
    public String[] getQueryArgs() {
        return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.put("id2", url);
        cv.put("mac", getMac());
        if(frameTLM != null) cv.put("other",
                String.valueOf(frameTLM.batt)+"|"+
                String.valueOf(frameTLM.temp)+"|"+
                String.valueOf(frameTLM.cpdu)+"|"+
                String.valueOf(frameTLM.cupt));
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.put("id2", url);
        cv.put("mac", getMac());
        if(frameTLM != null) cv.put("other",
                String.valueOf(frameTLM.batt)+"|"+
                        String.valueOf(frameTLM.temp)+"|"+
                        String.valueOf(frameTLM.cpdu)+"|"+
                        String.valueOf(frameTLM.cupt));

    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("id0", namespace);
        cv.put("id1", instance);
        cv.put("id2", url);
        cv.put("mac", getMac());
        if(frameTLM != null) cv.put("other",
                String.valueOf(frameTLM.batt)+"|"+
                String.valueOf(frameTLM.temp)+"|"+
                String.valueOf(frameTLM.cpdu)+"|"+
                String.valueOf(frameTLM.cupt));
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", "MERGE");
        intent.putExtra("namespace", namespace);
        intent.putExtra("instance", instance);
        intent.putExtra("url", url);
        intent.putExtra("mac", getMac());
        if(frameTLM != null) {
            intent.putExtra("batt", frameTLM.batt);
            intent.putExtra("temp", frameTLM.temp);
            intent.putExtra("cpdu", frameTLM.cpdu);
            intent.putExtra("cupt", frameTLM.cupt);
        }
    }

    @Override
    public void dressDummyFromDB(Cursor cursor) {

    }

    @Override
    public String getDefaultName() {
        return "Eddystone [MERGE]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_call_merge;
    }

    protected EDD(Parcel in) {
        super(in);
        //beaconTLM = in.readParcelable(Beacon.class.getClassLoader());
        namespace = in.readString();
        instance = in.readString();
        url = in.readString();
        frameTLM = in.readParcelable(TLM.class.getClassLoader());
        //batt = in.readInt();
        //temp = in.readFloat();
        //cpdu = in.readLong();
        //cupt = in.readLong();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        //dest.writeParcelable(beaconTLM, flags);
        dest.writeString(namespace);
        dest.writeString(instance);
        dest.writeString(url);
        dest.writeParcelable(frameTLM, flags);
        //dest.writeInt(batt);
        //dest.writeFloat(temp);
        //dest.writeLong(cpdu);
        //dest.writeLong(cupt);

    }

    public static final Creator<EDD> CREATOR = new Creator<EDD>() {
        @Override
        public EDD createFromParcel(Parcel in) {
            return new EDD(in);
        }

        @Override
        public EDD[] newArray(int size) {
            return new EDD[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public static String getDurationBreakdown(long millis)
    {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(days);
        sb.append(" d ");
        sb.append(hours);
        sb.append(" h ");
        sb.append(minutes);
        sb.append(" m ");
        sb.append(seconds);
        sb.append(" s");

        return(sb.toString());
    }
}
