package net.beaconradar.service;

import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;

import org.apache.commons.collections4.map.LRUMap;

public interface ScanResultListener {

    /**
     * Callback for Scanner.
     * This is called independent of the Scanner immediate/bulk mode when actual LE scan starts.
     *
     * @param reqState
     * @param duration
     */
    void onScanStart(int reqState, long duration);

    /**
     * Callback for Scanner bulk delivery mode.
     * Called when actual LE scan is finished AND all beacons found in scan cycle are processed.
     *
     * @param incoming ConcurrentHashMap containing recently seen beacons.
     * @param reqState
     * @param interval
     * @param fresh Fresh beacons count.
     * @param dead Dead beacons count.
     */
    void onScanEnd(LRUMap<ID, Beacon> incoming, int reqState, long interval, int fresh, int dead);

    /**
     * Callback for Scanner immediate delivery mode (unused, not tested)
     * As soon as beacon is parsed this is called on main thread.
     * This will be called multiple times with the same object reference for updates.
     *
     * @param beacon Scanned, 'live' beacon. Its field may change.
     *               Reading fields of this object from main thread is safe.
     *               In case of reading from threads other than main results may vary.
     */
    void onScanResult(Beacon beacon);
}
