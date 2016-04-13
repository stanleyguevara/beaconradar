package net.beaconradar.base;

import android.app.Service;

import net.beaconradar.dagger.App;

public abstract class BaseService extends Service {
    protected final String TAG = getClass().getName();

    @Override
    public void onCreate() {
        injectDependencies( ((App) getApplication()).component() );
        super.onCreate();
    }

    protected abstract void injectDependencies(App.AppComponent component);
}
