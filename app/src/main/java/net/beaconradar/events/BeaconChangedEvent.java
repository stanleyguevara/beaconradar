package net.beaconradar.events;

import net.beaconradar.service.id.ID;

public class BeaconChangedEvent {
    public final ID id;
    public final String source;

    public BeaconChangedEvent(ID identifier, String sourceTag) {
        this.id = identifier;
        this.source = sourceTag;
    }
}
