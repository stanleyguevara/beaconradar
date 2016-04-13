package net.beaconradar.nearby;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import net.beaconradar.R;
import net.beaconradar.base.BaseMvpFragment;
import net.beaconradar.dagger.App;
import net.beaconradar.details.DetailsActivity;
import net.beaconradar.dialogs.IconDialog;
import net.beaconradar.dialogs.OptionsDialog;
import net.beaconradar.dialogs.SingleChoiceListener;
import net.beaconradar.dialogs.TextDialog;
import net.beaconradar.events.BeaconChangedEvent;
import net.beaconradar.fab.FabBehavior;
import net.beaconradar.main.MainActivity;
import net.beaconradar.main.TabHost;
import net.beaconradar.main.TabSelectionListener;
import net.beaconradar.service.BTManager;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.utils.ColorChooserNoTitle;
import net.beaconradar.utils.CustomAppCompatPopupWindow;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.MenuAdapter;
import net.beaconradar.utils.Prefs;
import net.beaconradar.utils.WrappyLinearLayoutManager;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class FragmentNearby extends BaseMvpFragment<NearbyView, NearbyPresenter>
        implements RecyclerViewClickListener, NearbyView,
        ColorChooserNoTitle.DismissCallback, ColorChooserDialog.ColorCallback,
        SingleChoiceListener, TextDialog.NameChangedListener, IconDialog.IconChangedListener, TabSelectionListener {

    //Constant
    //Has to correspond to R.arrays.nearby_dropdown_options and R.arrays.nearby_dropdown_icons
    private static final int[] mSortModesOrder = new int[] {
            NearbyPresenter.SORT_SPOTTED_NORMAL,
            NearbyPresenter.SORT_DISTANCE_NORMAL,
            NearbyPresenter.SORT_SEEN_NORMAL,
            NearbyPresenter.SORT_DISTANCE_REVERSE,
            NearbyPresenter.SORT_NAME_AZ,
            NearbyPresenter.SORT_NAME_ZA,
            NearbyPresenter.SORT_TYPE_NORMAL
    };

    //Views
    @Bind(R.id.errorView) TextView mMessage;
    @Bind(R.id.contentView) RecyclerView mRecycler;

    //Dependencies
    @Inject NearbyPresenterImpl mPresenter;
    @Inject EventBus mBus;
    @Inject SharedPreferences mPrefs;

    //Members
    private BeaconAdapter mAdapter;
    private LinearLayoutManager mManager;

    //State
    private Beacon mSelectedBeacon;

    public static FragmentNearby newInstance() {
        FragmentNearby instance = new FragmentNearby();
        Bundle args = new Bundle();
        //args.putBoolean("footer", footer);
        instance.setArguments(args);
        return instance;
    }

    @Override
    protected void injectDependencies(App.AppComponent component) {
        component.inject(this);
    }

    @Override
    public NearbyPresenter createPresenter() {
        return mPresenter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle inState) {
        final View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, rootView);
        initRecycler(inState, rootView);
        if(inState != null) mSelectedBeacon = inState.getParcelable("SELECTED_BEACON");
        return rootView;
    }

    @Override @DebugLog
    public void onResume() {
        super.onResume();
        mPresenter.onResume();
        mBus.register(this);
        //Restoring dialog callbacks after rotation.
        //Return after every 'if' because only 1 dialog may be visible at any moment
        ColorChooserNoTitle color = (ColorChooserNoTitle) ColorChooserNoTitle.findVisible((MainActivity)getActivity(), ColorChooserDialog.TAG_PRIMARY);
        if(color != null) { color.setCallback(this); color.setDismissCallback(this); return; }
        OptionsDialog menu = OptionsDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_context");
        if(menu != null) { menu.setSelectionListener(this); return; }
        TextDialog name = TextDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_name");
        if(name != null) { name.setChangedListener(this); return; }
        IconDialog icon = IconDialog.findVisible((MainActivity) getActivity(), "dialog_beacon_icon");
        if(icon != null) { icon.setSelectionListener(this); return; }
        //Restore FAB
        int pos = ((TabHost) getActivity()).getCurrentItem();
        if(pos == 0) mPresenter.showFAB();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.onPause();
        mBus.unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("SELECTED_BEACON", mSelectedBeacon);
        //Save state of recycler
        int first = RecyclerView.NO_POSITION;
        //mManager is sometimes null. Weird. (when rotating device in settings fragment)
        if(mManager != null) first = mManager.findFirstCompletelyVisibleItemPosition();
        if(first != RecyclerView.NO_POSITION) {
            //OR: RecyclerView.ViewHolder holder = mRecycler.findViewHolderForLayoutPosition(first);
            RecyclerView.ViewHolder holder = mRecycler.findViewHolderForAdapterPosition(first);
            if(holder != null && holder.itemView != null) {
                int offset = ((int) holder.itemView.getY()) - mRecycler.getPaddingTop();    //If due to NPE on this line (once)
                outState.putInt("FIRST_VISIBLE_POSITION", first);
                outState.putInt("FIRST_VISIBLE_OFFSET", offset);
            }
        }
    }

    @Override @DebugLog
    public void onDestroyView() {
        //TODO might be better to move it to onPause()
        //Leak fix.
        mRecycler.setAdapter(null);
        mAdapter.setClickListener(null);
        super.onDestroyView();
        //Unnecessary
        ButterKnife.unbind(this);
    }

    private void initRecycler(Bundle inState, final View rootView) {
        //Setup recycler/adapter/layout manager
        int fabBehavior = mPrefs.getInt(Prefs.KEY_FAB_BEHAVIOR, Defaults.FAB_BEHAVIOR);
        boolean footer = (fabBehavior == FabBehavior.FAB_BEHAVIOR_FIXED);
        mAdapter = new BeaconAdapter(this, getActivity(), footer);
        mAdapter.setHasStableIds(true);
        mManager = new LinearLayoutManager(getActivity());
        ((SimpleItemAnimator) mRecycler.getItemAnimator()).setSupportsChangeAnimations(false);
        mRecycler.setLayoutManager(mManager);
        mRecycler.setAdapter(mAdapter);

        //Required to get reliably width of recyclerview for signal/distance bar animation
        mRecycler.post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics DM = getResources().getDisplayMetrics();
                float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, DM);     //TODO parametrize 28 (item_beacon_4)
                mAdapter.setBarWidth(rootView.getWidth() - (int) padding);
            }
        });

        //Restore state if possible
        if(inState != null && inState.containsKey("FIRST_VISIBLE_POSITION")) {
            int first = inState.getInt("FIRST_VISIBLE_POSITION");
            int offset = inState.getInt("FIRST_VISIBLE_OFFSET");
            //ID firstVisibleID = inState.getParcelable("FIRST_VISIBLE_BEACON_ID");
            int size = mAdapter.getItemCount()-1;
            if(first <= size) mManager.scrollToPositionWithOffset(first, offset);
            else mManager.scrollToPosition(size);
        }
    }

    @Override
    public String getTitle() { return "Nearby"; }

    @Override @DebugLog
    public void notifyDataSetChanged() {    //TODO more fine-grained changes. Problem is - it's dependent on sorting etc.
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void kickAnim() {
        mAdapter.kickAnim();
    }

    @Override @DebugLog
    public void showState(boolean request, int btState, boolean empty) {
        if(btState == BTManager.STATE_ON) {
            if(!empty) {
                //Show actual data
                mRecycler.setVisibility(View.VISIBLE);
                mMessage.setVisibility(View.GONE);
            } else {
                if(request) {
                    //Show empty list scanning indicator
                    mRecycler.setVisibility(View.GONE);
                    mMessage.setVisibility(View.VISIBLE);
                    setDrawable(mMessage, R.drawable.ic_radar_48);
                    mMessage.setText("Scanning...");
                    mMessage.setClickable(false);
                    mMessage.setOnClickListener(null);
                } else {
                    //Show "tap to scan"
                    mRecycler.setVisibility(View.GONE);
                    mMessage.setVisibility(View.VISIBLE);
                    mMessage.setText(R.string.error_scan_not_requested);
                    setDrawable(mMessage, R.drawable.ic_play_circle_48);
                    mMessage.setClickable(true);
                    mMessage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { mPresenter.requestScan();
                        }
                    });
                }
            }
        } else {
            mRecycler.setVisibility(View.GONE);
            mMessage.setVisibility(View.VISIBLE);
            if(btState == BTManager.STATE_OFF) {
                //Show "tap to turn BT on"
                setDrawable(mMessage, R.drawable.ic_bluetooth_48);
                mMessage.setText(R.string.error_bluetooth_off);
                mMessage.setClickable(true);
                mMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { mPresenter.requestBluetoothOn();
                    }
                });
            } else {
                //Show bluetooth state
                mMessage.setClickable(false);
                mMessage.setOnClickListener(null);
                switch (btState) {
                    case BTManager.STATE_TURNING_ON:
                        setDrawable(mMessage, R.drawable.ic_bluetooth_settings_48);
                        mMessage.setText(R.string.error_bluetooth_turning_on);
                        break;
                    case BTManager.STATE_TURNING_OFF:
                        setDrawable(mMessage, R.drawable.ic_bluetooth_settings_48);
                        mMessage.setText(R.string.error_bluetooth_turning_off);
                        break;
                    case BTManager.STATE_UNAVAILBLE:
                        setDrawable(mMessage, R.drawable.ic_bluetooth_off_48);
                        mMessage.setText(R.string.error_bluetooth_unavailable);
                        break;
                }
            }
        }
    }

    private void setDrawable(TextView message, int resource) {
        int color = ContextCompat.getColor(getContext(), R.color.darkGray);
        Drawable drawable = ContextCompat.getDrawable(getContext(), resource);
        drawable.mutate();
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        message.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
    }

    public void setData(ArrayList<Beacon> beacons) {
        mAdapter.setData(beacons);
    }

    @Override @DebugLog
    public void onTabReselected(TabLayout.Tab tab) {
        showPopup(tab.getCustomView());
    }

    public void showPopup(final View anchor) {
        //Do layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View layout = inflater.inflate(R.layout.popup_recycler, null);

        //Build popup
        final CustomAppCompatPopupWindow popup = new CustomAppCompatPopupWindow(getActivity(), null, R.attr.customAppCompatPopupWindowStyle);
        popup.setContentView(layout);
        popup.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);

        //Build recycler
        RecyclerView recycler = (RecyclerView) layout.findViewById(R.id.recycler);
        recycler.setLayoutManager(new WrappyLinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        final MenuAdapter adapter = new MenuAdapter(getActivity(), R.array.nearby_dropdown_options, R.array.nearby_dropdown_icons);
        //final MenuAdapter adapter = new MenuAdapter(getActivity(), mSortModesNames, mSortModesIcons);
        recycler.setAdapter(adapter);
        int sortMode = mPresenter.getSortMode();
        for (int i = 0; i < mSortModesOrder.length; i++) {
            if(mSortModesOrder[i] == sortMode) {
                adapter.setSelected(i);
                break;
            }
        }
        adapter.setMenuClickListener(new MenuAdapter.MenuClickListener() {
            @Override
            public void onMenuClick(View view, int position, MenuAdapter.MenuHolder holder) {
                mPresenter.setSortMode(mSortModesOrder[position]);
                adapter.setSelected(position);
                popup.dismiss();    //popup.dismissDelayed(); //working too, enables additional feedback of selection to user
                //If popup is not dismissed it recycler may change its width depending on chosen item
                //It's a bug in WrappyLinearLayoutManager which is workaround for recycler not supporting WRAP_CONTENT properly
                //Not worth fixing because soon RecyclerView will support WRAP_CONTENT properly.
                //https://code.google.com/p/android/issues/detail?id=74772
            }
        });

        //Show popup
        final int firstTabOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getContext().getResources().getDisplayMetrics());
        popup.showAsDropDown(anchor, firstTabOffset, 0); //Since it's a first tab we have to space it from edge of screen
    }

    //------------------Item click handling below-------------------

    @Override
    public void onClick(View view, int position, int type, RecyclerView.ViewHolder vh) {
        if(type == BeaconAdapter.TYPE_BEACON) {
            BeaconAdapter.BeaconHolder holder = (BeaconAdapter.BeaconHolder) vh;
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra("identifier", holder.beacon.getId());
            startActivity(intent);
            //TODO No more events at this point, will save lot of processing. Back on when details fragment is dismissed.
        }
    }

    @Override
    public void onLongClick(View view, int position, int type, RecyclerView.ViewHolder vh) {
        if(type == BeaconAdapter.TYPE_BEACON) {
            FragmentManager FM = getActivity().getSupportFragmentManager();
            BeaconAdapter.BeaconHolder holder = (BeaconAdapter.BeaconHolder) vh;
            mSelectedBeacon = holder.beacon;
            OptionsDialog dialog = OptionsDialog.newInstance(
                    mSelectedBeacon.getName(),
                    new int[]{},
                    new int[]{},
                    mSelectedBeacon.getColor(),
                    mSelectedBeacon.getIcon());
            dialog.show(FM, "dialog_beacon_context");
            dialog.setSelectionListener(this);
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
        dialog.setDismissCallback(this);
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
    public void onColorDismiss() {
        //mSelectedBeacon = null;  //Nope, gets lost on rotation.
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, @ColorInt int color) {
        if(mSelectedBeacon != null) {
            mPresenter.setBeaconColor(mSelectedBeacon, color);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
            //mSelectedBeacon.setColor(color);
            //int position = mAdapter.getData().indexOf(mSelectedBeacon);
            //if(position != -1) mAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onIconChanged(int icon) {
        if(mSelectedBeacon != null) {
            mPresenter.setBeaconIcon(mSelectedBeacon, icon);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
            //mSelectedBeacon.setIcon(icon);
            //int position = mAdapter.getData().indexOf(mSelectedBeacon);
            //if(position != -1) mAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onNameChanged(String name) {
        if (mSelectedBeacon != null) {
            mPresenter.setBeaconName(mSelectedBeacon, name);
            mBus.post(new BeaconChangedEvent(mSelectedBeacon.getId(), TAG));
            //mSelectedBeacon.setName(name);
            //int position = mAdapter.getData().indexOf(mSelectedBeacon);
            //if (position != -1) mAdapter.notifyItemChanged(position);
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

    public void onEvent(BeaconChangedEvent event) {
        //if(TAG.equals(event.source)) return;
        RecyclerView.ViewHolder genericHolder;
        BeaconAdapter.BeaconHolder holder;
        for (int i = 0; i < mRecycler.getChildCount(); i++) {
            View child = mRecycler.getChildAt(i);
            genericHolder = mRecycler.getChildViewHolder(child);
            if(genericHolder instanceof BeaconAdapter.BeaconHolder) {
                holder = (BeaconAdapter.BeaconHolder) genericHolder;
                if(holder.beacon.getId().equals(event.id)) {
                    mAdapter.notifyItemChanged(mRecycler.getChildAdapterPosition(child));
                    return;
                }
            }
        }
    }

    @Override @DebugLog
    public void onTabSelected(TabLayout.Tab tab) {
        mPresenter.showFAB();
    }

    @Override @DebugLog
    public void onTabUnselected(TabLayout.Tab tab) {

    }
}