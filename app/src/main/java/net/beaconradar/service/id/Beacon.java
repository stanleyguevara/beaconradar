package net.beaconradar.service.id;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.ColorPalette;
import net.beaconradar.utils.IconsPalette;
import net.beaconradar.service.Scanner;
import net.beaconradar.service.id.eddystone.UID;

import java.util.Random;

public class Beacon implements Parcelable {
    //From original RawBeacon
    private final ID identifier;
    private long discovered;
    private long last_seen;
    private int rssi = Integer.MIN_VALUE;
    private int tx = ID.NO_TX_POWER;

    //Identity
    private String name;
    private long dbid;             //database id
    //private long born;
    private int color;
    private int icon;

    //Status
    private boolean fresh = true;                                                                   //TODO parcelable
    private boolean hot = true;
    public boolean live = true;

    //For animation
    private int rssi_prev;
    private int rssi_min = Integer.MAX_VALUE;
    private int rssi_max;
    private double dist;                                                                            //TODO float ?
    private double dist_prev;                                                                       //TODO float ?
    private double dist_min = 0.01d;                                                                //TODO Actually dist_min may be smaller. Consequence = log10(dist_min^2);
    private double dist_max;

    //RawBeacon from which CookedBeacon will be updated before onScanEnd
    //Volatile so main thread can see changes immediately
    private volatile ID vUpdate;

    //TODO parcelable.
    private boolean on_appeared;
    private boolean on_visible;
    private boolean on_disappeared;

    public Beacon(ID identifier) {
        this.identifier = identifier;    //Safe - final.
    }

    public Beacon(Cursor cursor) {
        int type = cursor.getInt(cursor.getColumnIndex("type"));
        int eq = cursor.getInt(cursor.getColumnIndex("eq_mode"));
        this.rssi = cursor.getInt(cursor.getColumnIndex("rssi"));
        this.tx = cursor.getInt(cursor.getColumnIndex("tx"));
        this.dist = calcDistance(rssi, tx);
        this.last_seen = cursor.getLong(cursor.getColumnIndex("last_seen"));
        String id0 = cursor.getString(cursor.getColumnIndex("id0"));
        String id1 = cursor.getString(cursor.getColumnIndex("id1"));
        String id2 = cursor.getString(cursor.getColumnIndex("id2"));
        String mac = cursor.getString(cursor.getColumnIndex("mac"));
        String other = cursor.getString(cursor.getColumnIndex("other"));
        switch (type) {
            case ID.T_IBC:
                identifier = new IBC(eq, id0, Integer.valueOf(id1), Integer.valueOf(id2), mac, rssi, tx);
                break;
            case ID.T_UID:
                identifier = new UID(eq, id0, id1, mac, rssi, tx);
                break;
            case ID.T_URL:
                identifier = new URL(eq, id0, mac, rssi, tx);
                break;
            case ID.T_TLM:
                String[] str_tlm = other.split("\\|");
                identifier = new TLM(
                        eq,
                        Integer.valueOf(str_tlm[0]),
                        Float.valueOf(str_tlm[1]),
                        Long.valueOf(str_tlm[2]),
                        Long.valueOf(str_tlm[3]),
                        mac, rssi, tx);
                break;
            case ID.T_EDD:
                EDD edd = new EDD(mac, rssi, tx);
                edd.namespace = id0;
                edd.instance = id1;
                edd.url = id2;
                identifier = edd;
                break;
            default: identifier = null; break;
        }
        identifier.spotted = cursor.getLong(cursor.getColumnIndex("spotted"));

        this.dbid = cursor.getLong(cursor.getColumnIndex("_id"));
        this.color = cursor.getInt(cursor.getColumnIndex("color"));
        this.icon = IconsPalette.getResId(cursor.getString(cursor.getColumnIndex("icon")));
        this.name = cursor.getString(cursor.getColumnIndex("name"));
        this.on_appeared = cursor.getInt(cursor.getColumnIndex("intent_on_appeared")) != 0;
        this.on_visible = cursor.getInt(cursor.getColumnIndex("intent_on_visible")) != 0;
        this.on_disappeared = cursor.getInt(cursor.getColumnIndex("intent_on_disappeared")) != 0;
        this.discovered = cursor.getLong(cursor.getColumnIndex("discovered"));
        fresh = false;
        hot = false;

    }

    public void dressFromDB(Cursor cursor) {
        this.dbid = cursor.getLong(cursor.getColumnIndex("_id"));
        this.color = cursor.getInt(cursor.getColumnIndex("color"));
        this.icon = IconsPalette.getResId(cursor.getString(cursor.getColumnIndex("icon")));
        this.name = cursor.getString(cursor.getColumnIndex("name"));
        this.on_appeared = cursor.getInt(cursor.getColumnIndex("intent_on_appeared")) != 0;
        this.on_visible = cursor.getInt(cursor.getColumnIndex("intent_on_visible")) != 0;
        this.on_disappeared = cursor.getInt(cursor.getColumnIndex("intent_on_disappeared")) != 0;
        this.discovered = cursor.getLong(cursor.getColumnIndex("discovered"));
    }

    /**
     * This has to be called before getCV.
     * @param rnd
     * @param discovered
     */
    public void dressBrandNew(Random rnd, long discovered) {
        int palette = rnd.nextInt(ColorPalette.PRIMARY_COLORS_SUB.length);
        int color = rnd.nextInt(ColorPalette.PRIMARY_COLORS_SUB[palette].length - 4 ) + 4;         //Offset by 4 to disable faint colors.
        this.color = ColorPalette.PRIMARY_COLORS_SUB[palette][color];
        this.icon = this.identifier.getDefaultIcon();
        this.name = this.identifier.getDefaultName();
        this.discovered = discovered;
    }

    /**
     * This may be called only after dressBrandNew was called.
     * @param appContext
     * @return
     */
    public ContentValues getCV(Context appContext) {
        ContentValues cv = new ContentValues();
        identifier.fillCV(cv);
        cv.put("color", color);
        cv.put("icon", appContext.getResources().getResourceEntryName(icon));
        cv.put("name", name);
        cv.put("user", 0);  //TODO if user set something, exclude from cleaning DB.
        cv.put("spotted", identifier.getSpotted());
        cv.put("discovered", discovered);
        if(fresh) {
            cv.put("last_seen", identifier.getSpotted());
            cv.put("tx", identifier.tx);
        } else {
            cv.put("last_seen", last_seen);
            cv.put("tx", tx);
        }
        return cv;
    }

    public ContentValues getUpdateCV() {
        ContentValues cv = new ContentValues();
        identifier.fillCV(cv);
        cv.put("spotted", identifier.getSpotted());
        if(fresh) {
            cv.put("last_seen", identifier.getSpotted());
            cv.put("tx", identifier.tx);
        } else {
            cv.put("last_seen", last_seen);
            cv.put("tx", tx);
        }
        return cv;
    }

    public ContentValues getUpdateCV(ID id) {
        ContentValues cv = new ContentValues();
        id.fillCV(cv);
        cv.put("_id", dbid);
        cv.put("rssi", id.rssi);
        cv.put("tx", id.tx);
        cv.put("spotted", identifier.getSpotted());
        cv.put("last_seen", id.spotted);
        cv.put("discovered", getDiscovered());
        return cv;
    }

    /*public ContentValues getBackupCV(ID raw) {
        ContentValues cv = new ContentValues();
        raw.fillCV(cv);
        cv.put("rssi", raw.rssi);

    }*/

    public ContentValues getLogCV(Context appContext) {
        ContentValues cv = new ContentValues();
        identifier.fillLogCV(cv);
        cv.put("_id", dbid);
        //cv.put("color", color);
        //cv.put("icon", appContext.getResources().getResourceEntryName(icon));
        //cv.put("name", name);
        if(fresh) {
            cv.put("rssi", identifier.rssi);
            cv.put("tx", identifier.tx);
            cv.put("time", identifier.getSpotted());
        } else {
            cv.put("rssi", rssi);
            cv.put("tx", tx);
            cv.put("time", last_seen);
        }
        return cv;
    }

    public ContentValues getLogCV(ID id, Context appContext) {
        ContentValues cv = new ContentValues();
        id.fillLogCV(cv);
        cv.put("_id", dbid);
        //cv.put("color", color);
        //cv.put("icon", appContext.getResources().getResourceEntryName(icon));
        //cv.put("name", name);
        cv.put("rssi", id.rssi);
        cv.put("tx", id.tx);
        cv.put("time", id.getSpotted());
        return cv;
    }

    public void putExtras(Intent intent) {
        identifier.putExtras(intent);
        intent.putExtra("name", name);
        intent.putExtra("rssi", rssi);
        intent.putExtra("tx", tx);
    }

    public void putExtras(Intent intent, ID id) {
        id.putExtras(intent);
        intent.putExtra("name", name);
        intent.putExtra("rssi", id.rssi);
        intent.putExtra("tx", id.tx);
    }

    /**
     * If this method was called then beacon may be used only as data container.
     * @param cursor
     */
    public void dressDummyFromDB(Cursor cursor) {
        dressFromDB(cursor);
        identifier.dressDummyFromDB(cursor);
        this.tx = cursor.getInt(cursor.getColumnIndex("tx"));
        this.last_seen = cursor.getLong(cursor.getColumnIndex("last_seen"));
        this.hot = false;
        this.fresh = false;
        this.live = false;
    }

    //TODO good place to start thinking about duplicate beacons / MAC / including MAC in identifier for some beacons etc
    /**
     * Sets {@link ID} for updating rssi, last_seen, mac and tx.
     * This is thread safe (as is pointer write in Java).
     *
     * There may arise situation where {@link Scanner} detects duplicate beacons.
     * Duplicate meaning same identifier, so MAC can differ. In that case last update wins.
     * Detecting that situation is not worth additional synchronization.
     * (And chances of duplicate beacon are slim to none)
     * @param update {@link ID} with fresh values.
     */
    public void setPendingUpdate(ID update) {                                                //TODO after changing package make access modifier <none> instead of public.
        this.vUpdate = update;
    }

    /**
     * Updates only minimum device sensitivity in dBm.
     * This should be called from main thread only, after {@code update} if needed.
     * @param new_min_sens new minimum device sensitivity in dBm,
     *                     for calculating theoretical maximum distance
     *                     from which this beacon is detectable.
     */
    public void updateMinSens(int new_min_sens) {
        if(new_min_sens < this.rssi_min) {
            this.rssi_min = new_min_sens;
            this.dist_max = calcDistance(rssi_min, tx);
        }
    }

    public void updatePrevValues(int min_sens) {
        this.rssi_prev = min_sens;
        this.dist_prev = calcDistance(rssi_prev, tx);
    }

    /**
     * Updates this {@link Beacon} with values from previously set {@link ID}
     *
     * This method is not thread safe and should be called only from main thread.
     */                                                                                        //TODO threading check
    public boolean update() {                                                      //TODO after changing package make access modifier <none> instead of public.
        this.rssi_prev = this.rssi;
        this.dist_prev = this.dist;

        ID update = vUpdate;         //Volatile read
        if(update == null) return fresh;
        this.identifier.updateID(update);   //TODO watch out to not change it so it's not retrivable from hashmaps
        this.rssi = update.rssi;
        this.last_seen = update.spotted;
        if(this.tx != update.tx && update.tx != ID.NO_TX_POWER) {
            this.tx = update.tx;
            this.rssi_max = (int)((double)tx - 10.0*Math.log10(dist_min*dist_min));
            this.dist_max = calcDistance(rssi_min, tx);
        }
        if(rssi > rssi_max) rssi_max = rssi;
        this.dist = calcDistance(rssi, tx);
        boolean wasFresh = this.fresh;
        this.fresh = false;
        this.hot = true;
        this.vUpdate = null; //This forces visibility of fields written above for other threads (But worker thread has to access [NOT write] this field)
        return wasFresh;
    }

    /**
     * Static method for calculating distance based on received power and transmitting power.
     * @param rssi Received Signal Strength Indicator in dBm.
     * @param tx Beacon transmitting power in dBm as declared by Beacon itself.
     * @return Estimated distance to beacon in meters.
     */
    public static double calcDistance(int rssi, int tx) {
        double ratio_db = tx - rssi;
        double ratio_linear = Math.pow(10, ratio_db / 10);
        return Math.sqrt(ratio_linear);
    }

    //Setters
    //Basically all public setters are thread safe, since changes from other threads
    //are always performed in update() functions on main thread.
    public void setCold() {                                                                         //TODO after changing package make access modifier <none> instead of public.
        hot = false;
    }

    public void setDbId(long database_id) {                                                         //TODO after changing package make access modifier <none> instead of public.
        this.dbid = database_id;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void sendOnAppeared(boolean send) {
        this.on_appeared = send;
    }

    public void sendOnVisible(boolean send) {
        this.on_visible = send;
    }

    public void sendOnDisappeared(boolean send) {
        this.on_disappeared = send;
    }

    /**
     * May be called only after update()
     * @param tx
     */
    public void setTx(int tx) { //TODO protected
        this.tx = tx;
        int rssi_max_theory = (int)((double)tx - 10.0*Math.log10(dist_min*dist_min));
        this.rssi_max = Math.max(rssi_max_theory, rssi_max);
        this.dist_max = calcDistance(rssi_min, tx);
        this.dist = calcDistance(rssi, tx);
    }

    //Getters
    @NonNull
    public ID getId() {
        return identifier;
    }

    public int getRssi() {
        return rssi;
    }

    public long getLastSeen() {
        return last_seen;
    }

    public int getTx() {
        return tx;
    }

    public boolean isHot() {
        return hot;
    }

    public boolean isFresh() {
        return fresh;
    }

    public ID getPendingUpdate() {                                                           //TODO after changing package make access modifier <none> instead of public.
        return this.vUpdate;
    }

    @SuppressWarnings("unused")
    public long getDbId() {
        return dbid;
    }

    public long getDiscovered() {
        return discovered;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public int getRssiPrev() {
        return rssi_prev;
    }

    public int getRssiMin() {
        return rssi_min;
    }

    public int getRssiMax() {
        return rssi_max;
    }

    public double getDist() {
        return dist;
    }

    public double getDistPrev() {
        return dist_prev;
    }

    public double getDistMin() {
        return dist_min;
    }

    public double getDistMax() {
        return dist_max;
    }

    public boolean getOnAppeared() { return on_appeared; }

    public boolean getOnVisible() { return on_visible; }

    public boolean getOnDisappeared() { return on_disappeared; }

    //Parcelable implementation
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(identifier, flags);
        dest.writeLong(last_seen);
        dest.writeInt(rssi);
        dest.writeInt(tx);
        dest.writeString(name);
        dest.writeLong(dbid);
        //dest.writeLong(born);
        dest.writeInt(color);
        dest.writeInt(icon);
        dest.writeByte((byte) (fresh ? 1 : 0));
        dest.writeByte((byte) (hot ? 1 : 0));
        dest.writeInt(rssi_prev);
        dest.writeInt(rssi_min);
        dest.writeInt(rssi_max);
        dest.writeDouble(dist);
        dest.writeDouble(dist_prev);
        dest.writeDouble(dist_min);
        dest.writeDouble(dist_max);
        dest.writeByte((byte) (on_appeared ? 1 : 0));
        dest.writeByte((byte) (on_visible ? 1 : 0));
        dest.writeByte((byte) (on_disappeared ? 1 : 0));
    }

    protected Beacon(Parcel in) {
        identifier = in.readParcelable(ID.class.getClassLoader());
        last_seen = in.readLong();
        rssi = in.readInt();
        tx = in.readInt();
        name = in.readString();
        dbid = in.readLong();
        //born = in.readLong();
        color = in.readInt();
        icon = in.readInt();
        fresh = in.readByte() != 0;
        hot = in.readByte() != 0;
        rssi_prev = in.readInt();
        rssi_min = in.readInt();
        rssi_max = in.readInt();
        dist = in.readDouble();
        dist_prev = in.readDouble();
        dist_min = in.readDouble();
        dist_max = in.readDouble();
        on_appeared = in.readByte() != 0;
        on_visible = in.readByte() != 0;
        on_disappeared = in.readByte() != 0;
    }

    public static final Creator<Beacon> CREATOR = new Creator<Beacon>() {
        @Override
        public Beacon createFromParcel(Parcel in) {
            return new Beacon(in);
        }

        @Override
        public Beacon[] newArray(int size) {
            return new Beacon[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}