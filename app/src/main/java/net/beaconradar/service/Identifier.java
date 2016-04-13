package net.beaconradar.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Identifier implements Parcelable {                                                     //TODO consider using MAC in identifier
    public static final int TYPE_IBEACON = 100;
    public static final int TYPE_EDD_UID = 200;
    public static final int TYPE_EDD_URL = 300;
    public static final int TYPE_EDD_TLM = 400; //TODO TLM are not identifiable, in way other than mac.
    public static final int TYPE_ALTBEAC = 500;

    private int type = -1;                                                                          //TODO include eddystone types in type
    private String[] identifiers = new String[3];

    protected Identifier(Parcel in) {
        type = in.readInt();
        identifiers = in.createStringArray();
    }

    public static final Creator<Identifier> CREATOR = new Creator<Identifier>() {
        @Override
        public Identifier createFromParcel(Parcel in) {
            return new Identifier(in);
        }

        @Override
        public Identifier[] newArray(int size) {
            return new Identifier[size];
        }
    };

    public String[] getIdentifiers() {
        return identifiers;
    }

    public String getPart(int part) {
        return identifiers[part];
    }

    public int getType() {
        return type;
    }

    /**
     * Constructor for Identifier object. Pass "" (empty string) instead of null as one/two/three.
     * @param type  Either {@link RawBeacon} TYPE_IBEACON or TYPE_EDDYSTONE
     * @param one   Most significant part of identifier (major or namespace, mac in case of Eddystone)
     * @param two   Middle part of identifier (minor or instance)
     * @param three Least significant part of identifier (prox_uuid or url)
     */
    public Identifier(int type, @NonNull String one, @NonNull String two, @NonNull String three) {
        this.type = type;
        identifiers[0] = one;
        identifiers[1] = two;
        identifiers[2] = three;
    }

    @Override
    public boolean equals(Object another) {
        if(another instanceof Identifier) {
            if( type == ((Identifier) another).getType()
                && identifiers[0].equals(((Identifier) another).getPart(0))
                && identifiers[1].equals(((Identifier) another).getPart(1))
                && identifiers[2].equals(((Identifier) another).getPart(2))
            ) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(identifiers[0])
                .append(identifiers[1])
                .append(identifiers[2])
                .toHashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeStringArray(identifiers);
    }
}
