package net.beaconradar.details;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import net.beaconradar.utils.LimitedQueue;
import net.beaconradar.utils.ZeroNorm;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import java.util.ListIterator;

import hugo.weaving.DebugLog;

public class DetailsData implements Parcelable {
    private LimitedQueue<Sample> samples;
    private ID identifier;
    private Beacon beacon;
    private float sumDist = 0f;
    private float sumRssi = 0;
    private int cWeighted = 0;
    private float avgDist = 0f;
    private float avgRssi = 0f;
    private float maxRssi = 0f;
    private float minRssi = 0f;
    private float maxDist = 0f;
    private float minDist = 0f;
    private float minRssiEver;
    private float maxRssiEver;
    private float minDistEver;
    private float maxDistEver;
    private boolean initialized = false;
    private ZeroNorm normDist;
    private ZeroNorm normRssi;

    private float currDist = 0f;
    private float currRssi = 0;
    private Sample mZeroSample;
    private int mSize;

    @DebugLog
    public DetailsData(ID beacon, int size) {
        this.identifier = beacon;
        //normDist = new ZeroNorm(true, (float) beacon.dist_min, (float)Math.ceil(beacon.dist_max));
        mSize = size;
        samples = new LimitedQueue<>(size);
    }

    public void insertAndUpdate(Beacon wrapper) {
        Sample sample;
        if(wrapper != null) {
            this.beacon = wrapper;
            if(!initialized) {
                init(wrapper);
                initialized = true;
            }
            sample = new Sample(
                    normRssi.normalize(wrapper.getRssi()),
                    normDist.normalize((float) wrapper.getDist()),
                    true);
        } else {
            sample = mZeroSample;
        }
        if(initialized) addLimited(sample);
    }

    private void init(Beacon beacon) {
        normDist = new ZeroNorm(true, 0.0f, (float) (10.0f*Math.ceil(beacon.getDistMax()/10.0f)));
        normRssi = new ZeroNorm(false, (float) (5*Math.floor(((float)beacon.getRssiMin())/5.0d)), (float) beacon.getRssiMax());
        mZeroSample = new Sample(
                normRssi.normalize((float) (5*Math.floor(((float)beacon.getRssiMin())/5.0d))),
                //normDist.normalize((float) (5*Math.ceil(beacon.dist_max/5))),
                normDist.normalize((float) (10.0f*Math.ceil(beacon.getDistMax()/10.0f))),
                false);
        for (int i = 0; i < mSize-1; i++) {
            //Log.v("DetailsData", "ADD");
            samples.add(mZeroSample);
        }
        minRssiEver = normRssi.normalize(beacon.getRssiMin());
        maxRssiEver = normRssi.normalize(beacon.getRssiMax());
        minDistEver = normDist.normalize((float) (10.0f*Math.ceil(beacon.getDistMax()/10.0f)));  //Switched, due to invert == true in normDist
        maxDistEver = normDist.normalize(0.0f);                                                  //Switched, due to invert == true in normDist
    }

    private Sample addLimited(Sample sample) {
        if(cWeighted == 0 && sample.weight) {   //First important sample
            maxDist = normDist.normalize((float) beacon.getDist());
            minDist = normDist.normalize((float) beacon.getDist());
            maxRssi = normRssi.normalize((float) beacon.getRssi());
            minRssi = normRssi.normalize((float) beacon.getRssi());
        }
        currDist = sample.distance;
        currRssi = sample.rssi;
        if(sample.weight) {
            if(sample.rssi > maxRssi) {
                maxRssi = sample.rssi;
                maxDist = sample.distance;
            } else if(sample.rssi < minRssi) {
                minRssi = sample.rssi;
                minDist = sample.distance;
            }
            if(sample.rssi > maxRssiEver) {     //TODO when new min/max rssi appears. Should be like min/max in constructor. Not sure, im tired and it's late.
                maxRssiEver = sample.rssi;
                maxDistEver = sample.distance;
            } else if (sample.rssi < minRssiEver) {
                minRssiEver = sample.rssi;
                minDistEver = sample.distance;
            }
            sumDist += sample.distance;
            sumRssi += sample.rssi;
            cWeighted ++;
        }
        Sample removed = samples.addLimited(sample);
        if(removed != null) {
            if(removed.weight) {
                if(maxRssi == removed.rssi) {
                    //Find next biggest value
                    ListIterator<Sample> iterator = samples.listIterator();
                    float secondRssi = Float.MIN_VALUE;
                    float secondDist = Float.MIN_VALUE;
                    Sample candidate;
                    while (iterator.hasNext()) {
                        candidate = iterator.next();
                        if(candidate.rssi > secondRssi) {
                            secondRssi = candidate.rssi;
                            secondDist = candidate.distance;
                        }
                    }
                    maxRssi = secondRssi;
                    maxDist = secondDist;
                } else if (minRssi == removed.rssi) {
                    //Find next smallest value
                    ListIterator<Sample> iterator = samples.listIterator();
                    float secondRssi = Float.MAX_VALUE;
                    float secondDist = Float.MAX_VALUE;
                    Sample candidate;
                    while (iterator.hasNext()) {
                        candidate = iterator.next();
                        if(candidate.rssi < secondRssi && candidate.rssi > 0) {
                            secondRssi = candidate.rssi;
                            secondDist = candidate.distance;
                        }
                    }
                    minRssi = secondRssi;
                    minDist = secondDist;
                }
                sumDist -= removed.distance;
                sumRssi -= removed.rssi;
                cWeighted --;
            }
        }
        if(cWeighted != 0) {
            avgDist = sumDist / cWeighted;
            avgRssi = sumRssi / cWeighted;
        }
        return removed;
    }

    protected DetailsData(Parcel in) {
        samples = in.readParcelable(LimitedQueue.class.getClassLoader());
        identifier = in.readParcelable(ID.class.getClassLoader());
        beacon = in.readParcelable(Beacon.class.getClassLoader());
        sumDist = in.readFloat();
        sumRssi = in.readFloat();
        cWeighted = in.readInt();
        avgDist = in.readFloat();
        avgRssi = in.readFloat();
        maxRssi = in.readFloat();
        minRssi = in.readFloat();
        maxDist = in.readFloat();
        minDist = in.readFloat();
        minRssiEver = in.readFloat();
        maxRssiEver = in.readFloat();
        minDistEver = in.readFloat();
        maxDistEver = in.readFloat();
        initialized = in.readByte() != 0;
        currDist = in.readFloat();
        currRssi = in.readFloat();
        mZeroSample = in.readParcelable(Sample.class.getClassLoader());
        normDist = in.readParcelable(ZeroNorm.class.getClassLoader());
        normRssi = in.readParcelable(ZeroNorm.class.getClassLoader());
        mSize = in.readInt();
    }

    public static final Creator<DetailsData> CREATOR = new Creator<DetailsData>() {
        @Override
        public DetailsData createFromParcel(Parcel in) {
            return new DetailsData(in);
        }

        @Override
        public DetailsData[] newArray(int size) {
            return new DetailsData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(samples, flags);
        dest.writeParcelable(identifier, flags);
        dest.writeParcelable(beacon, flags);
        dest.writeFloat(sumDist);
        dest.writeFloat(sumRssi);
        dest.writeInt(cWeighted);
        dest.writeFloat(avgDist);
        dest.writeFloat(avgRssi);
        dest.writeFloat(maxRssi);
        dest.writeFloat(minRssi);
        dest.writeFloat(maxDist);
        dest.writeFloat(minDist);
        dest.writeFloat(minRssiEver);
        dest.writeFloat(maxRssiEver);
        dest.writeFloat(minDistEver);
        dest.writeFloat(maxDistEver);
        dest.writeByte((byte) (initialized ? 1 : 0));
        dest.writeFloat(currDist);
        dest.writeFloat(currRssi);
        dest.writeParcelable(mZeroSample, flags);
        dest.writeParcelable(normDist, flags);
        dest.writeParcelable(normRssi, flags);
        dest.writeInt(mSize);
    }

    public float getCurrentValue(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return currDist;
        else return currRssi;
    }

    public String getCurrentValueLabel(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return normDist.getLabel(currDist, 2);
        else return normRssi.getLabel(currRssi, 0);
    }

    public float getAverage(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return avgDist;
        else return avgRssi;
    }

    public String getAverageLabel(boolean mode) {
        //if(!initialized) return "N/A";
        if (mode == DetailsView.MODE_DISTANCE) return normDist.getLabel(avgDist, 2);
        else return normRssi.getLabel(avgRssi, 1);
    }

    public ZeroNorm getZeroNorm(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return normDist;
        else return normRssi;
    }

    public float getMax(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return maxDist;
        else return maxRssi;
    }

    public float getMin(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return minDist;
        else return minRssi;
    }

    public float getMinEver(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return minDistEver;
        else return minRssiEver;
    }

    public float getMaxEver(boolean mode) {
        if (mode == DetailsView.MODE_DISTANCE) return maxDistEver;
        else return maxRssiEver;
    }

    public ID getIdentifier() {
        return identifier;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public LimitedQueue<Sample> getSamples() {
        return samples;
    }

    @Nullable
    public Beacon getLastBeacon() {
        return beacon;
    }

    public int getWeightedCount() {
        return cWeighted;
    }
}