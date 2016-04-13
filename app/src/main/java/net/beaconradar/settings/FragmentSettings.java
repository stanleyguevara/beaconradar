package net.beaconradar.settings;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import net.beaconradar.R;
import net.beaconradar.base.BaseMvpFragment;
import net.beaconradar.dagger.App;
import net.beaconradar.events.ScanTimingChangedEvent;
import net.beaconradar.events.ScanTimingWarningEvent;
import net.beaconradar.events.SettingChangedEvent;
import net.beaconradar.events.SettingIntChangedEvent;
import net.beaconradar.fab.FabBehavior;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.main.TabSelectionListener;
import net.beaconradar.service.BeaconService;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class FragmentSettings
    extends BaseMvpFragment<SettingsView, SettingsPresenter>
    implements SettingsView, TabSelectionListener, SettingsAdapter.SettingClickListener {

    @Inject SettingsPresenterImpl mPresenter;
    @Inject EventBus mBus;
    @Inject SharedPreferences mPrefs;

    @Bind(R.id.recycler) RecyclerView mRecycler;

    private ViewGroup mCoordinator;
    private Snackbar mSnackbar;
    private SettingsAdapter mAdapter;

    private boolean mFooter;

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Override
    public SettingsPresenter createPresenter() {
        return mPresenter;
    }

    public static FragmentSettings newInstance() {
        FragmentSettings instance = new FragmentSettings();
        Bundle args = new Bundle();
        //args.putBoolean("footer", footer);
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int fabBehavior = mPrefs.getInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR);
        mFooter = (fabBehavior == FabBehavior.FAB_BEHAVIOR_FIXED);
        mAdapter = new SettingsAdapter(getActivity(), mFooter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings2, container, false);
        ButterKnife.bind(this, rootView);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.setAdapter(mAdapter);
        mAdapter.setSettingClickListener(this);
        mCoordinator = findCoordinator(container);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mBus.unregister(this);
    }

    private ViewGroup findCoordinator(View start) {
        View view = start;
        while (view.getParent() != null) {
            view = (View) view.getParent();
            if(view.getId() == R.id.coordinator && view instanceof CoordinatorLayout){
                return (ViewGroup) view;
            }
        }
        return null;
    }

    //--------------------SettingsView methods
    @Override
    public void setSummary(int mode, String summary) {

    }

    public void onEvent(ScanTimingWarningEvent event) {
        showWarning(event.mode, event.message, Snackbar.LENGTH_LONG);
    }

    @Override @DebugLog
    public void showWarning(final int mode, String message, int duration) {
        mSnackbar = Snackbar
                .make(mCoordinator, message, duration)
                .setActionTextColor(ContextCompat.getColor(getContext(), R.color.primary))
                .setAction("Change", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = mAdapter.getPosition(modeToKey(mode));
                        onTimingSettingClick(null, position, (SettingTiming) mAdapter.getData().get(position));
                    }
                });
        mSnackbar.show();
    }

    @Override
    public void dismissWarning(int mode) {
        if(mSnackbar != null) mSnackbar.dismiss();
        iconWarning(mode, false);
    }

    private void iconWarning(int mode, boolean show) {
        //TODO highlight icon
    }

    @Override
    public void showDurationWarning(int mode, String message) {
        //Unused
    }

    @Override
    public void showRemoveWarning(int mode, String message) {
        //Unused
    }

    @Override
    public void showSplitWarning(int mode, String message) {
        //Unused
    }

    @Override
    public void showIntervalWarning(int mode, String message) {

    }

    @Override
    public String getTitle() {
        return "Settings";
    }

    //--------------------SettingsView methods end

    public static int keyToMode(String key) {
        switch (key) {
            case Prefs.FOREGROUND: return BeaconService.SCAN_FOREGROUND;
            case Prefs.BACKGROUND: return BeaconService.SCAN_BACKGROUND;
            case Prefs.LOCKED:     return BeaconService.SCAN_LOCKED;
            default: return 0;
        }
    }

    public static String modeToKey(int mode) {
        switch (mode) {
            case BeaconService.SCAN_FOREGROUND: return Prefs.FOREGROUND;
            case BeaconService.SCAN_BACKGROUND: return Prefs.BACKGROUND;
            case BeaconService.SCAN_LOCKED:     return Prefs.LOCKED;
            default: return Prefs.FOREGROUND;
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        if(mSnackbar != null) mSnackbar.dismiss();
    }

    @Override
    public void onTimingSettingClick(View view, int position, SettingTiming timing) {
        int mode = keyToMode(timing.getKey());
        TimingFragment dialog = TimingFragment.newInstance(
                timing.getKey(),
                mode,
                mPresenter.getIntervalMs(mode),
                mPresenter.getDurationMs(mode),
                mPresenter.getRemoveMs(mode),
                mPresenter.getSplitTo(mode),
                mPresenter.getIntervalProgress(mode),
                mPresenter.getDurationProgress(mode),
                mPresenter.getRemoveProgress(mode),
                mPresenter.getSplitProgress(mode));
        FragmentManager FM = getActivity().getSupportFragmentManager();
        dialog.show(FM, "dialog_setting_timing");
    }

    public void onEvent(ScanTimingChangedEvent event) {
        int position = mAdapter.getPosition(modeToKey(event.mode));
        mAdapter.notifyItemChanged(position);
    }

    public void onIntegerSettingClick(View view, int position, SettingInteger integer) {
        IntegerFragment dialog = IntegerFragment.newInstance(
                integer.getKey(),
                integer.title,
                integer.getCleanupDays(),
                1, 365);
        FragmentManager FM = getActivity().getSupportFragmentManager();
        dialog.show(FM, "dialog_setting_integer");
    }

    public void onEvent(SettingIntChangedEvent event) {
        int position = mAdapter.getPosition(event.key);
        if(position != SettingsAdapter.POSITION_NONE) mAdapter.notifyItemChanged(position);
    }

    @Override
    public void onListSettingClick(View view, int position, SettingsAdapter.SettingHolder holder) {
        SettingList list = (SettingList) holder.setting;
        String key = list.getKey();
        //Log.v(TAG, "key: "+key+" selected position "+mPresenter.getListSelectedPosition(key, list.val, list.def));
        ListFragment dialog = ListFragment.newInstance(
                key,
                mPresenter.getListSelectedPosition(key, list.val, list.def),
                list.title,
                list.val,
                list.txt,
                list.def
        );
        FragmentManager FM = getActivity().getSupportFragmentManager();
        dialog.show(FM, "dialog_setting_list");
    }

    @DebugLog
    public void onEvent(SettingChangedEvent event) {
        if(event.key.equals(Prefs.KEY_FAB_BEHAVIOR)) {
            boolean footer = (event.selected == FabBehavior.FAB_BEHAVIOR_FIXED);
            mAdapter.setFooter(footer);
            mPresenter.setFabBehavior(event.selected);
        }
        int position = mAdapter.getPosition(event.key);
        //Log.v(TAG, "key: "+event.key+" position: "+position+" selected: "+event.selected);
        if(position != SettingsAdapter.POSITION_NONE) mAdapter.notifyItemChanged(position);
    }

    @Override
    public void onCheckboxSettingClick(View view, int position, SettingsAdapter.SettingHolder holder) {
        SettingCheckbox box = (SettingCheckbox) holder.setting;
        boolean checked = box.getChecked();
        if(((SettingCheckbox) holder.setting).key.equals(Prefs.KEY_HIGH_PRIORITY_SERVICE)) {
            mPresenter.setHighPriorityService(!checked);
        } else if(((SettingCheckbox) holder.setting).key.equals(Prefs.KEY_EXACT_SCHEDULING)) {
            mPresenter.setExactScheduling(!checked);
        }
        box.setChecked(!checked);
        holder.cbox.setChecked(!checked);
    }

    @Override @DebugLog
    public void onResetSettingClick(View view, int position, SettingReset setting) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(setting.title)
                .content(Html.fromHtml(
                        "This will reset<br><br>" +
                        "<b>• Scan cycle timing<br>" +
                        "• Beacon ID methods<br>"+
                        "• Scan tweaks<br><br></b>" +
                        "to default values."))
                .positiveText("Reset")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override @DebugLog
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        mPresenter.restoreDefaults();
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .build();
        TextView title = dialog.getTitleView();
        DisplayMetrics DM = getResources().getDisplayMetrics();
        int padBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, DM);
        title.setPadding(title.getPaddingLeft(), title.getPaddingTop(), title.getPaddingRight(), padBottom);
        //TODO ROLL MY OWN TEXT STYLES
        title.setTextAppearance(getContext(), android.R.style.TextAppearance_Material_Title);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        dialog.show();
    }
}
