package net.beaconradar.dagger;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        NearbyModule.class,
        AppContextModule.class
})
public interface NearbyComponent {
    //SimpleNearbyPresenter mPresenter();
}
