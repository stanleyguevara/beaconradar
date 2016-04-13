package net.beaconradar.dagger;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v7.preference.PreferenceManager;

import net.beaconradar.database.MainDatabaseHelper;
import net.beaconradar.details.DetailsActivity;
import net.beaconradar.events.BeaconServiceModeEvent;
import net.beaconradar.history.HistoryFragment;
import net.beaconradar.service.BTManager;
import net.beaconradar.main.MainActivity;
import net.beaconradar.main.ServiceManager;
import net.beaconradar.nearby.FragmentNearby;
import net.beaconradar.service.BeaconService;
import net.beaconradar.settings.FragmentSettings;
import net.beaconradar.settings.IntegerFragment;
import net.beaconradar.settings.ListFragment;
import net.beaconradar.settings.SettingCheckbox;
import net.beaconradar.settings.SettingInteger;
import net.beaconradar.settings.SettingList;
import net.beaconradar.settings.SettingTiming;
import net.beaconradar.settings.TimingFragment;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.fab.ProgressFAB;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Component;
import de.greenrobot.event.EventBus;

public class App extends Application {
    //Good and working singleton instance. Not useful now.
    /*private static App instance;

    public static App getAppContext() {
        return instance;
    }*/

    //For detecting Foreground / Background
    private final long MAX_WAIT_TIME = 1000;
    private final Handler mHandler = new Handler();
    private Runnable mModeDelay;
    private boolean mForeground = false;

    public void onPause() {
        mHandler.removeCallbacks(mModeDelay);
        mModeDelay = new Runnable() {
            @Override public void run() {
                //Log.v("onScan", "BACKGROUND posted");
                mForeground = false;
                sManager.setModeBackground();
            }
        };
        mHandler.postDelayed(mModeDelay, MAX_WAIT_TIME);
    }

    public void onResume() {
        mHandler.removeCallbacks(mModeDelay);
        //Log.v("onScan", "FOREGROUND posted");
        mForeground = true;
        sManager.setModeForeground();
    }

    public boolean isForeground() {
        return mForeground;
    }

    @Inject ServiceManager sManager;    //Here, to keep it alive for application lifetime.
    @Inject BTManager mBTManager;

    @Singleton @Component(modules = {
            AppContextModule.class,
            StaticModule.class
    })
    public interface AppComponent {
        //SimpleNearbyPresenter mPresenter(); //then we call component.mPresenter inside createPresenter()

        void inject(App application);
        void inject(BeaconService service);
        void inject(MainActivity activity);
        void inject(DetailsActivity activity);
        void inject(FragmentNearby fragment);
        void inject(FragmentSettings fragment);
        void inject(HistoryFragment fragment);
        void inject(SettingList list);
        void inject(SettingInteger integer);
        void inject(SettingCheckbox checkbox);
        void inject(TimingFragment fragment);
        void inject(SettingTiming timing);
        void inject(ListFragment fragment);
        void inject(IntegerFragment fragment);
        void inject(ProgressFAB fab);
        //void inject(BaseMvpLceFragment<RecyclerView, HashMap<String, Beacon>, NearbyView, NearbyPresenter> fragment); //Problem with generics and base class.
    }

    private static AppComponent component; //i can haz static?

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerApp_AppComponent.builder()
                .appContextModule(new AppContextModule(this))
                .build();
        component.inject(this);
        EventBus.getDefault().post(new BeaconServiceModeEvent(BeaconService.SCAN_FOREGROUND));

        //Application Context will fail for things like layout inflation.
        //Otherwise it is safe to keep. (We're not leaking activity context)
        //instance = (App) getApplicationContext();                             //Good and working singleton instance. Not useful now.

        initSharedPreferences();
        cleanupDB();
    }

    public static AppComponent component() {   //i can haz static? (result is similar to singleton - call anywhere)
        return component;
    }

    private void initSharedPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean init = prefs.getBoolean(Prefs.KEY_INIT_DONE, false);
        if(!init) {
            prefs.edit().putInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR).apply();
            prefs.edit().putLong(Prefs.KEYS_DURATION[BeaconService.SCAN_FOREGROUND], Defaults.DURATION[BeaconService.SCAN_FOREGROUND]).apply();
            prefs.edit().putLong(Prefs.KEYS_DURATION[BeaconService.SCAN_BACKGROUND], Defaults.DURATION[BeaconService.SCAN_BACKGROUND]).apply();
            prefs.edit().putLong(Prefs.KEYS_DURATION[BeaconService.SCAN_LOCKED],     Defaults.DURATION[BeaconService.SCAN_LOCKED]).apply();

            prefs.edit().putLong(Prefs.KEYS_INTERVAL[BeaconService.SCAN_FOREGROUND], Defaults.INTERVAL[BeaconService.SCAN_FOREGROUND]).apply();
            prefs.edit().putLong(Prefs.KEYS_INTERVAL[BeaconService.SCAN_BACKGROUND], Defaults.INTERVAL[BeaconService.SCAN_BACKGROUND]).apply();
            prefs.edit().putLong(Prefs.KEYS_INTERVAL[BeaconService.SCAN_LOCKED], Defaults.INTERVAL[BeaconService.SCAN_LOCKED]).apply();

            prefs.edit().putLong(Prefs.KEY_PREV_DB_CLEANUP, System.currentTimeMillis()).apply();
            prefs.edit().putLong(Prefs.KEY_CLEANUP_AFTER, Defaults.CLEANUP_AFTER).apply();
            prefs.edit().putBoolean(Prefs.KEY_INIT_DONE, true).apply();
        }
    }

    private void cleanupDB() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long cleanup = prefs.getLong(Prefs.KEY_PREV_DB_CLEANUP, 0);
        long now = System.currentTimeMillis();
        if(now - cleanup > 1000*60*60*24) {   //Once a day
            SQLiteDatabase db = MainDatabaseHelper.getInstance(this).getWritableDatabase();
            long limit = prefs.getLong(Prefs.KEY_CLEANUP_AFTER, Defaults.CLEANUP_AFTER);
            db.delete("known", "last_seen < ?", new String[]{String.valueOf(now - limit)});
            prefs.edit().putLong(Prefs.KEY_PREV_DB_CLEANUP, now).apply();
        }
    }
}