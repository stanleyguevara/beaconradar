package net.beaconradar.utils;

import android.os.Parcel;
import android.os.Parcelable;

import net.beaconradar.details.Sample;

import java.util.LinkedList;

public class LimitedQueue<E> extends LinkedList<E> implements Parcelable {
    private int limit;

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    protected LimitedQueue(Parcel in) {
        limit = in.readInt();
        in.readList(this, Sample.class.getClassLoader());
    }

    public static final Creator<LimitedQueue> CREATOR = new Creator<LimitedQueue>() {
        @Override
        public LimitedQueue createFromParcel(Parcel in) {
            return new LimitedQueue(in);
        }

        @Override
        public LimitedQueue[] newArray(int size) {
            return new LimitedQueue[size];
        }
    };

    /**
     * Adds element to list, up to limit. If size > limit removes first element.
     * Warning: Do not use any other method involving adding/removing on this list
     * @param element Element to add
     * @return Removed element if size exceeded, else null
     */
    public E addLimited(E element) {
        super.add(element);
        if (size() > limit) return super.remove();
        else return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(limit);
        dest.writeList(this);
    }
}
