package net.beaconradar.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import net.beaconradar.database.MainDatabaseHelper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppContextModule {
    private final App application;

    public AppContextModule(App application) {
        this.application = application;
    }

    /*@Provides @Singleton
    public App provideApplication() {
        return App.getAppContext();
    }*/

    @Provides @Singleton @ForApplication
    public Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton
    SharedPreferences provideSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Provides @Singleton
    MainDatabaseHelper provideMainDatabaseHelper() {
        //return MainDatabaseHelper.getInstance(application);
        return new MainDatabaseHelper(application, MainDatabaseHelper.DATABASE_NAME, null, MainDatabaseHelper.DATABASE_VERSION);  //TODO wont work first time
    }
}
