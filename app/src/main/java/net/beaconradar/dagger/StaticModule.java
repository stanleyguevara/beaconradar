package net.beaconradar.dagger;

import net.beaconradar.utils.IconsPalette;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module
public class StaticModule {

    @Provides @Singleton
    public EventBus providesEventBus() {
        return EventBus.getDefault();
    }

    @Provides @Singleton
    public IconsPalette providesIconsPalette() {
        return new IconsPalette();
    }
}
