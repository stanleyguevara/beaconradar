package net.beaconradar.utils;

import android.os.Parcel;
import android.os.Parcelable;

//Brings set of values to (zero - max) so it has higher dynamic range on screen.
//May be used to invert it so highest value = 0, and lowest value = max;
public class ZeroNorm implements Parcelable {
    private boolean invert = false;
    private float max = 0f;
    private float min = 0f;

    public ZeroNorm(boolean invert, float min, float max) {
        this.min = min;
        this.max = max;
        this.invert = invert;
    }

    public float normalize(float input) {
        if(input > max) {
            //if(invert) Log.v("XX", "ZeroNorm new max "+input);
            max = input;
        }
        if(input < min) {
            //if(invert) Log.v("XX", "ZeroNorm new min "+input);
            min = input;
        }
        if(invert) {
            return max - input;
        } else {
            return input - min;
        }
    }

    public float getValue(float output) {
        if(invert) {
            return max - output;
        } else {
            return output + min;
        }
    }

    public String getLabel(float output, int digits) {
        if(invert) {
            return String.format("%."+digits+"f", max - output);
        } else {
            return String.format("%."+digits+"f", output + min);
        }
    }

    //Parcelable implementation
    protected ZeroNorm(Parcel in) {
        invert = in.readByte() != 0;
        max = in.readFloat();
        min = in.readFloat();
    }

    public static final Creator<ZeroNorm> CREATOR = new Creator<ZeroNorm>() {
        @Override
        public ZeroNorm createFromParcel(Parcel in) {
            return new ZeroNorm(in);
        }

        @Override
        public ZeroNorm[] newArray(int size) {
            return new ZeroNorm[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (invert ? 1 : 0));
        dest.writeFloat(max);
        dest.writeFloat(min);
    }
}
