package net.beaconradar.service;

public class ScanResult {
    public final String address;
    public final int rssi;
    public final byte[] scanRecord;

    public ScanResult(String address, int rssi, byte[] scanRecord) {
        this.address = address;
        this.rssi = rssi;
        this.scanRecord = scanRecord;
    }
}
