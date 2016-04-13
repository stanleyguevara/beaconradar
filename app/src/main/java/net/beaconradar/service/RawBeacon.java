package net.beaconradar.service;

public class RawBeacon {
    public final static int TYPE_IBEACON = 0;
    public final static int TYPE_EDDYSTONE = 1;

    Identifier identifier;
    String mac;
    long born;
    int rssi = Integer.MIN_VALUE;
    int tx = Integer.MIN_VALUE;

    public RawBeacon(Identifier identifier, String mac, int rssi, int tx) {
        this.identifier = identifier;                                                               //TODO maybe make this vendor specific class? Solves it all?
        this.mac = mac;
        this.rssi = rssi;
        this.tx = tx;
        this.born = System.currentTimeMillis();
    }
}
