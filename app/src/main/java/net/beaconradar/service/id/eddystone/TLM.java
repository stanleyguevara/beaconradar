package net.beaconradar.service.id.eddystone;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;

import net.beaconradar.R;
import net.beaconradar.service.id.ID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.concurrent.TimeUnit;

public class TLM extends ID {
    public int batt;
    public float temp;
    public long cpdu;
    public long cupt;

    public String getBattString() {
        return String.valueOf(batt/1000.0f)+" V";
    }

    public String getTempString() {
        return String.format("%.5f", temp)+" °C";
    }

    public String getCPDUString() {
        return String.valueOf(cpdu);
    }

    public String getCUPTString() {
        return getDurationBreakdown(cupt * 100);
    }

    public TLM(int eq_mode, int batt, float temp, long cpdu, long cupt, String mac, int rssi, int tx) {
        super(T_TLM, eq_mode, mac, rssi, tx);
        this.batt = batt;
        this.temp = temp;
        this.cpdu = cpdu;
        this.cupt = cupt;
        //Log.v("TLM","Batt: "+batt+" Temp: "+temp+" cPDU: "+cpdu+" cUPT: "+cupt);
    }

    @Override
    public void updateID(ID update) {
        TLM upd = (TLM) update;
        this.batt = upd.batt;
        this.temp = upd.temp;
        this.cpdu = upd.cpdu;
        this.cupt = upd.cupt;
    }

    @Override
    public int hashCode() {
        //TODO consider hashing also eq_mode
        HashCodeBuilder hash = new HashCodeBuilder(304979, 172169);
        hash.append(getType())
            .append(getMac());
        return hash.toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        TLM obj = (TLM) other;
        if (eq_mode != obj.eq_mode) { return false; }
        EqualsBuilder equals = new EqualsBuilder();
        return equals
                //.append(getType(), obj.getType())   //Should not be needed
                .append(getMac(), obj.getMac())
                .isEquals();
    }



    @Override
    public String getQueryWhere() {
        return "type = ? AND eq_mode = ? AND mac = ?";  //For MAC (separate). For STICKY it can't happen (not kept it in DB)
    }

    @Override
    public String[] getQueryArgs() {
        return new String[] { String.valueOf(getType()), String.valueOf(eq_mode), getMac() };
    }

    @Override
    public void fillCV(ContentValues cv) {
        cv.put("type", getType());
        cv.put("eq_mode", eq_mode);
        cv.putNull("id0");
        cv.putNull("id1");
        cv.putNull("id2");
        cv.put("mac", getMac());
        cv.put("other", String.valueOf(batt)+"|"+String.valueOf(temp)+"|"+String.valueOf(cpdu)+"|"+String.valueOf(cupt));
    }

    @Override
    public void fillLogCV(ContentValues cv) {
        cv.putNull("id0");
        cv.putNull("id1");
        cv.putNull("id2");
        cv.put("mac", getMac());
        cv.put("other", String.valueOf(batt)+"|"+String.valueOf(temp)+"|"+String.valueOf(cpdu)+"|"+String.valueOf(cupt));
    }

    @Override
    public void fillUpdateCV(ContentValues cv) {
        cv.put("mac", getMac());
        cv.put("other", String.valueOf(batt)+"|"+String.valueOf(temp)+"|"+String.valueOf(cpdu)+"|"+String.valueOf(cupt));
    }

    @Override
    public void putExtras(Intent intent) {
        intent.putExtra("type", getType());
        intent.putExtra("eq_mode", getEqModeString());
        intent.putExtra("mac", getMac());
        intent.putExtra("batt", batt);
        intent.putExtra("temp", temp);
        intent.putExtra("cpdu", cpdu);
        intent.putExtra("cupt", cupt);
    }

    @Override
    public void dressDummyFromDB(Cursor cursor) {
        //Get all fields from other

    }

    @Override
    public String getDefaultName() {
        return "TLM Eddystone ["+getEqModeString()+"]";
    }

    @Override
    public int getDefaultIcon() {
        return R.drawable.ic_thermometer;
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

    protected TLM(Parcel in) {
        super(in);
        batt = in.readInt();
        temp = in.readFloat();
        cpdu = in.readLong();
        cupt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(batt);
        dest.writeFloat(temp);
        dest.writeLong(cpdu);
        dest.writeLong(cupt);
    }

    public static final Creator<TLM> CREATOR = new Creator<TLM>() {
        @Override
        public TLM createFromParcel(Parcel in) {
            return new TLM(in);
        }

        @Override
        public TLM[] newArray(int size) {
            return new TLM[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
