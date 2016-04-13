package net.beaconradar.details;

import android.os.Parcel;
import android.os.Parcelable;

public class Sample implements Parcelable {
    public float rssi;
    public float distance;
    public final boolean weight;

    public Sample(float rssi, float distance, boolean weight) {
        this.rssi = rssi;
        this.distance = distance;
        this.weight = weight;
    }

    protected Sample(Parcel in) {
        rssi = in.readFloat();
        distance = in.readFloat();
        weight = in.readByte() != 0;
    }

    public static final Creator<Sample> CREATOR = new Creator<Sample>() {
        @Override
        public Sample createFromParcel(Parcel in) {
            return new Sample(in);
        }

        @Override
        public Sample[] newArray(int size) {
            return new Sample[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(rssi);
        dest.writeFloat(distance);
        dest.writeByte((byte) (weight ? 1 : 0));
    }
}
