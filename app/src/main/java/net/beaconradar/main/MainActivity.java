package net.beaconradar.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.hannesdorfmann.mosby.mvp.viewstate.RestorableViewState;
import net.beaconradar.R;
import net.beaconradar.base.BaseMvpViewStateActivity;
import net.beaconradar.dagger.App;
import net.beaconradar.fab.ProgressFAB;
import net.beaconradar.utils.TintableImageView;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class MainActivity
        extends BaseMvpViewStateActivity<MainView, MainPresenter>
        implements MainView, ColorChooserDialog.ColorCallback, TabHost {

    public static final String KEY_NOTIFICATION = "net.beaconradar.SHOW_NOTIFICATION_ITEM";
    public static final String KEY_KILL_SCAN = "net.beaconradar.KILL_SCAN";

    @Inject EventBus mBus;
    @Inject MainPresenterImpl mPresenter;
    @Bind(R.id.fab)
    ProgressFAB mFab;
    @Bind(R.id.coordinator) CoordinatorLayout mCoordinator;
    @Bind(R.id.viewpager) ViewPager mViewPager;
    @Bind(R.id.tabs) TabLayout mTabs;

    private ViewPagerAdapter mTabAdapter;

    //State //TODO put mTitle into MainViewState
    private MainViewState getVS() {
        return (MainViewState) viewState;
    }

    //Other
    private Snackbar mSnackbar; //Currently shown snackbar

    @Override @DebugLog
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mFab.setCoordinator(mCoordinator);
        setupViewPager();
    }

    @Override @DebugLog
    protected void onPause() {
        super.onPause();
        mFab.onPause();
        //mBus.unregister(this);
        ((App) getApplication()).onPause();
    }

    @Override @DebugLog
    protected void onResume() {
        super.onResume();
        mFab.onResume();
        //mBus.register(this);
        ((App) getApplication()).onResume();
    }

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Override @NonNull
    public MainPresenter createPresenter() {
        return mPresenter;
    }

    @Override
    public RestorableViewState createViewState() {
        return new MainViewState();
    }

    //Switch to NearbyFragment when no view state exists yet
    @Override @DebugLog
    public void onNewViewStateInstance() {

    }

    //Receives navigation intents from Notifications.
    //Build exclusively on Intents, EventBus can't receive events from notifications
    //TODO intents may also come to DetailsActivity
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if(action == null) return;

        //Notification intents
        else if (KEY_NOTIFICATION.equals(action)) { //TODO unused
            //TODO swipe to nearby
        }
        //Kill scan intent
        else if (KEY_KILL_SCAN.equals(action)) {     //TODO unused
            //mPresenter.killScan();
        }
    }

    private void setupViewPager() {
        mTabAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabAdapter);
        mTabs.setupWithViewPager(mViewPager);
        setupTabIcons();
    }

    @Nullable
    public TabLayout.Tab getTabForFragment(Fragment fragment) {
        int position = mTabAdapter.getPositionForFragment(fragment.getTag());
        if(position != PagerAdapter.POSITION_NONE) return mTabs.getTabAt(position);
        else return null;
    }

    public int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }

    private void setupTabIcons() {
        FrameLayout tab = (FrameLayout) View.inflate(this, R.layout.custom_tab, null);
        TintableImageView icon = (TintableImageView) tab.findViewById(R.id.tab_icon);
        tab.findViewById(R.id.tab_divider).setVisibility(View.GONE);
        //Below line is due to https://code.google.com/p/android/issues/detail?id=190429 (it said released, but it still occurs with height)
        tab.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        icon.setImageResource(R.drawable.ic_radar);
        TintableImageView dropdown = (TintableImageView) tab.findViewById(R.id.tab_dropdown);
        dropdown.getDrawable().mutate();
        mTabs.getTabAt(0).setCustomView(tab);

        tab = (FrameLayout) View.inflate(this, R.layout.custom_tab, null);
        icon = (TintableImageView) tab.findViewById(R.id.tab_icon);
        icon.setImageResource(R.drawable.ic_history);
        dropdown = (TintableImageView) tab.findViewById(R.id.tab_dropdown);
        dropdown.getDrawable().mutate();
        dropdown.setColorFilter(getResources().getColorStateList(R.color.tab_dropdown_selector_record));
        //API 23+ we can:
        //dropdown.setColorFilter(getResources().getColorStateList(R.color.tab_dropdown_selector_record, null));
        //dropdown.setVisibility(View.GONE);
        tab.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mTabs.getTabAt(1).setCustomView(tab);

        tab = (FrameLayout) View.inflate(this, R.layout.custom_tab, null);
        icon = (TintableImageView) tab.findViewById(R.id.tab_icon);
        icon.setImageResource(R.drawable.ic_settings);
        dropdown = (TintableImageView) tab.findViewById(R.id.tab_dropdown);
        dropdown.getDrawable().mutate();
        dropdown.setVisibility(View.GONE);
        tab.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mTabs.getTabAt(2).setCustomView(tab);

        //Workaround for https://code.google.com/p/android/issues/detail?id=194873
        mTabs.getTabAt(1).select();
        mTabs.getTabAt(0).select();

        mTabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                TabSelectionListener fragment = ((TabSelectionListener) mTabAdapter.getFragment(tab.getPosition()));
                if(fragment != null) fragment.onTabReselected(tab);
                super.onTabReselected(tab);
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TabSelectionListener fragment = ((TabSelectionListener) mTabAdapter.getFragment(tab.getPosition()));
                if(fragment != null) fragment.onTabSelected(tab);
                super.onTabSelected(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                TabSelectionListener fragment = ((TabSelectionListener) mTabAdapter.getFragment(tab.getPosition()));
                if(fragment != null) fragment.onTabUnselected(tab);
                super.onTabUnselected(tab);
            }
        });
    }

    public void displaySnackbar(String key, String message, int duration) {
        mSnackbar = Snackbar.make(mCoordinator, message, duration).setAction("Ok", null);
        mSnackbar.show();
    }

    @Override
    public void clearSnackbar(String key) {
        mSnackbar.dismiss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, int color) {
        //Required to show ColorChooserDialog
    }
}
