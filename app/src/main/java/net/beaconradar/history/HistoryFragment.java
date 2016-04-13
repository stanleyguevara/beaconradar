package net.beaconradar.history;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import net.beaconradar.R;
import net.beaconradar.base.BaseMvpFragment;
import net.beaconradar.dagger.App;
import net.beaconradar.database.MainContentProvider;
import net.beaconradar.details.DetailsActivity;
import net.beaconradar.dialogs.IconDialog;
import net.beaconradar.dialogs.OptionsDialog;
import net.beaconradar.dialogs.SingleChoiceListener;
import net.beaconradar.dialogs.TextDialog;
import net.beaconradar.events.BeaconChangedEvent;
import net.beaconradar.events.FabBehaviorChangedEvent;
import net.beaconradar.fab.FabBehavior;
import net.beaconradar.logread.LogReadActivity;
import net.beaconradar.main.MainActivity;
import net.beaconradar.main.TabHost;
import net.beaconradar.main.TabSelectionListener;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.utils.ColorChooserNoTitle;
import net.beaconradar.utils.CustomAppCompatPopupWindow;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.utils.TintableImageView;

import java.util.LinkedHashMap;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class HistoryFragment extends BaseMvpFragment<HistoryView, HistoryPresenter>
        implements HistoryView, RecyclerViewCursorIdClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, TabSelectionListener,
        SingleChoiceListener, ColorChooserDialog.ColorCallback, TextDialog.NameChangedListener, IconDialog.IconChangedListener {

    @Inject
    HistoryPresenterImpl mPresenter;
    @Inject SharedPreferences mPrefs;
    @Inject EventBus mBus;

    @Bind(R.id.recycler) RecyclerView mRecycler;

    private HistoryAdapter mAdapter;

    private boolean mFooter;
    private Beacon mSelectedBeacon;

    private int mRestorePosition = RecyclerView.NO_POSITION;
    private int mRestoreOffset = 0;

    @Override
    public HistoryPresenter createPresenter() {
        return mPresenter;
    }

    public static HistoryFragment newInstance() {
        HistoryFragment instance = new HistoryFragment();
        Bundle args = new Bundle();
        //args.putBoolean("footer", footer);
        instance.setArguments(args);
        return instance;
    }

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Nullable
    @Override @DebugLog
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        View layout = inflater.inflate(R.layout.fragment_history_beacons, container, false);
        ButterKnife.bind(this, layout);
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        int fabBehavior = mPrefs.getInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR);
        mFooter = (fabBehavior == FabBehavior.FAB_BEHAVIOR_FIXED);
        if(mAdapter == null) getLoaderManager().restartLoader(0, null, this);
        else mRecycler.setAdapter(mAdapter);
        if(inState != null) {
            mSelectedBeacon = inState.getParcelable("SELECTED_BEACON");
            //Restore state if possible
            if(inState.containsKey("FIRST_VISIBLE_POSITION")) {
                mRestorePosition = inState.getInt("FIRST_VISIBLE_POSITION");
                mRestoreOffset = inState.getInt("FIRST_VISIBLE_OFFSET");
            }
        }
        return layout;
    }

    @DebugLog
    @Override
    public void onResume() {
        super.onResume();
        mPresenter.onResume();
        mBus.register(this);
        ColorChooserNoTitle color = (ColorChooserNoTitle) ColorChooserNoTitle.findVisible((MainActivity)getActivity(), ColorChooserDialog.TAG_PRIMARY);
        if(color != null) { color.setCallback(this); return; }
        OptionsDialog menu = OptionsDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_context");
        if(menu != null) { menu.setSelectionListener(this); return; }
        TextDialog name = TextDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_name");
        if(name != null) { name.setChangedListener(this); return; }
        IconDialog icon = IconDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_icon");
        if(icon != null) { icon.setSelectionListener(this); return; }
    }

    @Override @DebugLog
    public void onPause() {
        super.onPause();
        mPresenter.onPause();
        mBus.unregister(this);
    }

    @Override @DebugLog
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("SELECTED_BEACON", mSelectedBeacon);
        //Save state of recycler
        int first = RecyclerView.NO_POSITION;
        //mManager is sometimes null. Weird. (when rotating device in settings fragment)
        LinearLayoutManager manager = (LinearLayoutManager) mRecycler.getLayoutManager();
        if(manager != null) first = manager.findFirstCompletelyVisibleItemPosition();
        if(first != RecyclerView.NO_POSITION) {
            //OR: RecyclerView.ViewHolder holder = mRecycler.findViewHolderForLayoutPosition(first);
            RecyclerView.ViewHolder holder = mRecycler.findViewHolderForAdapterPosition(first);
            int offset = ((int) holder.itemView.getY()) - mRecycler.getPaddingTop();
            outState.putInt("FIRST_VISIBLE_POSITION", first);
            outState.putInt("FIRST_VISIBLE_OFFSET", offset);
        }
    }

    @Override
    public String getTitle() { return "History"; }

    @Override
    public void setBeacons(LinkedHashMap<ID, Beacon> beacons, boolean fresh, boolean dead) {
        if(mAdapter == null || mRecycler == null) return;
        mAdapter.setScanData(beacons);
        mAdapter.updateTimers();
        /*RecyclerView.ViewHolder generic_holder;
        HistoryAdapter.CursorHolder holder;
        for (int i = 0; i < mRecycler.getChildCount(); i++) {
            generic_holder = mRecycler.getChildViewHolder(mRecycler.getChildAt(i));
            if(generic_holder instanceof HistoryAdapter.CursorHolder) {
                holder = (HistoryAdapter.CursorHolder) generic_holder;
                Beacon beacon = beacons.get(holder.identifier);
                if(beacon != null && beacon.isHot()) {
                    //Log.v(TAG, "Identifier: " + Arrays.toString(holder.identifier.getQueryArgs()));
                    holder.seen_icon.setVisibility(View.VISIBLE);
                } else {
                    holder.seen_icon.setVisibility(View.GONE);
                }
            }
        }*/
        if(fresh || dead) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void swapCursor(Cursor data) {
        if(mAdapter == null) {
            //Log.v(TAG, "New");
            LinkedHashMap<ID, Beacon> seenBeacons = mPresenter.getBeacons();
            if(seenBeacons == null) seenBeacons = new LinkedHashMap<>();
            mAdapter = new HistoryAdapter(data, this, seenBeacons, mFooter);
            boolean header = mPresenter.isRecording() || mPresenter.isStoringLog();
            mAdapter.setHasHeader(header);
            if(header) mAdapter.setIsRecording(mPresenter.isRecording(), mPresenter.getLogStarted(), mPresenter.getLogPaused());
            mRecycler.setAdapter(mAdapter);
            //Restore state if needed
            if(mRestorePosition != RecyclerView.NO_POSITION) {
                int size = mAdapter.getItemCount()-1;
                LinearLayoutManager manager = (LinearLayoutManager) mRecycler.getLayoutManager();
                if(mRestorePosition <= size) manager.scrollToPositionWithOffset(mRestorePosition, mRestoreOffset);
                else manager.scrollToPosition(size);
                mRestoreOffset = 0;
                mRestorePosition = RecyclerView.NO_POSITION;
            }
        } else {
            //Log.v(TAG, "Change");
            mAdapter.changeCursor(data);
        }
    }

    @Override
    public void onClick(int type, View view, int position, long id, RecyclerView.ViewHolder vh) {
        if(type == HistoryAdapter.TYPE_BEACON) {
            HistoryAdapter.CursorHolder holder = (HistoryAdapter.CursorHolder) vh;
            Intent intent = new Intent(getContext(), DetailsActivity.class);
            intent.putExtra("identifier", holder.identifier);
            startActivityForResult(intent, 1);
        } else if (type == HistoryAdapter.TYPE_HEADER) {
            if(mPresenter.isRecording()) {
                new MaterialDialog.Builder(getActivity())
                        .title("Pause logging?")
                        .content("Logging can be resumed later.\nTap log item again to share.")
                        .positiveText("Pause")
                        .negativeText("Cancel")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                                mPresenter.setRecording(false);
                            }
                        })
                        .show();
            } else {
                Intent intent = new Intent(getActivity(), LogReadActivity.class);
                startActivityForResult(intent, 2);
            }
        }
    }

    @Override @DebugLog
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            if(resultCode == 1) getLoaderManager().restartLoader(0, null, this);
        } else if(requestCode == 2) {
            if(resultCode == 1) getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onLongClick(int type, View view, int position, long id, RecyclerView.ViewHolder vh) {
        if(type == HistoryAdapter.TYPE_BEACON) {
            FragmentManager FM = getActivity().getSupportFragmentManager();
            HistoryAdapter.CursorHolder holder = (HistoryAdapter.CursorHolder) vh;
            mSelectedBeacon = mPresenter.getDummyBeacon(holder.identifier);
            if(mSelectedBeacon != null) {
                OptionsDialog dialog = OptionsDialog.newInstance(
                        mSelectedBeacon.getName(),
                        new int[]{},
                        new int[]{},
                        mSelectedBeacon.getColor(),
                        mSelectedBeacon.getIcon());
                dialog.show(FM, "dialog_beacon_context");
                dialog.setSelectionListener(this);
            }
        } else if(type == HistoryAdapter.TYPE_HEADER) {
            new MaterialDialog.Builder(getActivity())
                    .title("Delete log file?")
                    .positiveText("Delete")
                    .negativeText("Cancel")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            mPresenter.deleteLog();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onSelection(int position) {
        switch (position) {
            case 0: showTextInput(mSelectedBeacon); break;
            case 1: showColorPicker(mSelectedBeacon); break;
            case 2: showIconPicker(mSelectedBeacon); break;
        }
    }

    public void showColorPicker(Beacon wrapper) {
        ColorChooserNoTitle.Builder builder = new ColorChooserNoTitle.Builder((MainActivity)getActivity(), R.string.color_picker_title);
        builder.titleSub(R.string.color_picker_sub);
        builder.doneButton(R.string.positive_button);
        builder.cancelButton(R.string.negative_button);
        builder.customButton(R.string.color_picker_custom);
        builder.presetsButton(R.string.color_picker_presets);
        builder.allowUserColorInputAlpha(false);
        builder.dynamicButtonColor(false);
        builder.preselect(wrapper.getColor());
        ColorChooserNoTitle dialog = builder.build();
        dialog.setBeacon(
                wrapper.getName(),
                wrapper.getColor(),
                wrapper.getIcon());
        dialog.setCallback(this);
        dialog.show((MainActivity) getActivity());
    }

    private void showTextInput(final Beacon wrapper) {
        TextDialog dialog = TextDialog.newInstance(
                wrapper.getName(),
                wrapper.getColor(),
                wrapper.getIcon());
        FragmentManager FM = getActivity().getSupportFragmentManager();
        dialog.show(FM, "dialog_beacon_name");
        dialog.setChangedListener(this);
    }

    public void showIconPicker(Beacon wrapper) {
        IconDialog dialog = IconDialog.newInstance(
                wrapper.getName(),
                R.layout.icon_grid_2,
                wrapper.getIcon(),
                wrapper.getColor());
        FragmentManager FM = getActivity().getSupportFragmentManager();
        dialog.show(FM, "dialog_beacon_icon");
        dialog.setSelectionListener(this);
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, @ColorInt int color) {
        if(mSelectedBeacon != null) {
            mPresenter.setBeaconColor(mSelectedBeacon, color);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
        }
    }

    @Override
    public void onIconChanged(int icon) {
        if(mSelectedBeacon != null) {
            mPresenter.setBeaconIcon(mSelectedBeacon, icon);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
        }
    }

    @Override
    public void onNameChanged(String name) {
        if (mSelectedBeacon != null) {
            mPresenter.setBeaconName(mSelectedBeacon, name);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String query = "SELECT _id, icon, color, name, id0, id1, id2, " +
                            "other, type, mac, eq_mode, last_seen, user " +
                       "FROM known ORDER BY user DESC, spotted DESC";
        String uri = "content://" + MainContentProvider.AUTHORITY + "/RAW_QUERY";
        CursorLoader loader = new CursorLoader(getActivity(), Uri.parse(uri), null, query, null, null);
        return loader;
    }

    @Override @DebugLog
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data);
    }

    @Override @DebugLog
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    public void showPopup(View anchor, final TintableImageView indicator) {
        //Do layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View layout = inflater.inflate(R.layout.item_menu, null);
        View clickable = layout.findViewById(R.id.clickable);
        TextView text = (TextView) layout.findViewById(R.id.text);
        TintableImageView icon = (TintableImageView) layout.findViewById(R.id.icon);
        final boolean rec = mPresenter.isRecording();
        boolean storing = mPresenter.isStoringLog();
        text.setText(rec ? "Pause logging" : storing ? "Resume logging" : "Start new log");
        icon.setImageResource(R.drawable.ic_record_rec);
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_selected},
                new int[] {-android.R.attr.state_selected}
        };
        int[] colors = new int[] {
                ContextCompat.getColor(getActivity(), R.color.warningText),
                ContextCompat.getColor(getActivity(), R.color.primaryDark)
        };
        ColorStateList tint = new ColorStateList(states, colors);
        icon.setColorFilter(tint);
        layout.setSelected(rec);
        indicator.setActivated(rec);

        //Build popup
        final CustomAppCompatPopupWindow popup = new CustomAppCompatPopupWindow(getActivity(), null, R.attr.customAppCompatPopupWindowStyle);
        popup.setContentView(layout);
        popup.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        clickable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean recording = mPresenter.isRecording();
                mPresenter.setRecording(!recording);
                layout.setSelected(!recording);
                popup.dismiss();    //popup.dismissDelayed(); //is working too, enables additional feedback of selection to user
            }
        });

        //Show popup
        popup.showAsDropDown(anchor, 0, 0);
    }

    @Override
    public void showRecording(boolean recording, long started, long paused) {
        if(mAdapter != null) {
            boolean header = recording || mPresenter.isStoringLog();
            mAdapter.setHasHeader(header);
            if(header) mAdapter.setIsRecording(recording, started, paused);
        }
        if(recording) mRecycler.scrollToPosition(0);
        TabLayout.Tab tab = ((TabHost) getActivity()).getTabForFragment(this);
        TintableImageView indicator = (TintableImageView) tab.getCustomView().findViewById(R.id.tab_dropdown);
        indicator.setActivated(recording);
    }

    public boolean isRecording() {
        return mPresenter.isRecording();
    }

    @Override @DebugLog
    public void onTabReselected(TabLayout.Tab tab) {
        TintableImageView indicator = (TintableImageView) tab.getCustomView().findViewById(R.id.tab_dropdown);
        showPopup(tab.getCustomView(), indicator);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    public void onEvent(FabBehaviorChangedEvent event) {
        mAdapter.setFooter(event.behavior == FabBehavior.FAB_BEHAVIOR_FIXED);
    }

    @DebugLog
    public void onEvent(BeaconChangedEvent event) {
        //if(TAG.equals(event.source)) return;
        getLoaderManager().restartLoader(0, null, this);
    }
}
