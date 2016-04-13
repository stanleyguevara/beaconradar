package net.beaconradar.service;

public interface ServiceScanResultListener extends ScanResultListener {
    void onNotificationFeedback(int reqState);
    boolean isAppOnTop();
    void goBackground();
    void scanRequestChanged(boolean scan);
    void unbind();
}
