package net.beaconradar.main;

import android.content.Context;
import android.content.SharedPreferences;

import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import net.beaconradar.dagger.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

@Singleton
public class MainPresenterImpl extends MvpBasePresenter<MainView> implements MainPresenter {
    private final Context appContext;
    private final EventBus mBus;
    private final SharedPreferences mPrefs;

    //This presenter has no responsibilities now. One that comes to mind is handling intents.

    @Inject
    public MainPresenterImpl(@ForApplication Context context, EventBus bus, SharedPreferences prefs) {
        appContext = context;
        mPrefs = prefs;
        mBus = bus;
    }

    @Override @DebugLog
    public void attachView(MainView view) {
        super.attachView(view);
    }

    @Override @DebugLog
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
    }
}
