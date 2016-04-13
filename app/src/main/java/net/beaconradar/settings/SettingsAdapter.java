package net.beaconradar.settings;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.utils.Defaults;
import net.beaconradar.utils.Prefs;
import net.beaconradar.utils.TintableImageView;

import java.util.ArrayList;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingHolder> {
    public static final int POSITION_NONE = -1;

    private ArrayList<Setting> mData = new ArrayList<>();

    {
        mData.add(new SettingHeader("Scan cycle timing"));
        mData.add(new SettingTiming(Prefs.FOREGROUND, "Foreground",    R.drawable.ic_arrange_bring_forward));
        mData.add(new SettingTiming(Prefs.BACKGROUND, "Background",    R.drawable.ic_arrange_send_backward));
        mData.add(new SettingTiming(Prefs.LOCKED,     "Device Locked", R.drawable.ic_lock));
        mData.add(new SettingCheckbox(Prefs.KEY_HIGH_PRIORITY_SERVICE, "High priority service", "Reliable scanning. Uses more battery.", R.drawable.ic_star_outline, Defaults.HIGH_PRIORITY_SERVICE));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mData.add(new SettingCheckbox(Prefs.KEY_EXACT_SCHEDULING, "Exact timing", "Android won't optimize scan timing. Uses more battery.", R.drawable.ic_clock, Defaults.EXACT_SCHEDULING));
        }
        mData.add(new SettingHeader("Beacon ID methods"));
        mData.add(new SettingList(Prefs.KEY_IBC_ID_METHOD, "iBeacon",       R.drawable.ic_apple2_full, R.array.id_beacon_val, R.array.id_beacon_txt, Defaults.EQ_MODE_IBC));
        mData.add(new SettingList(Prefs.KEY_UID_ID_METHOD, "Eddystone UID", R.drawable.ic_fingerprint, R.array.id_edd_val, R.array.id_edd_txt, Defaults.EQ_MODE_UID));
        mData.add(new SettingList(Prefs.KEY_URL_ID_METHOD, "Eddystone URL", R.drawable.ic_link_variant, R.array.id_edd_val, R.array.id_edd_txt, Defaults.EQ_MODE_URL));
        mData.add(new SettingList(Prefs.KEY_TLM_ID_METHOD, "Eddystone TLM", R.drawable.ic_thermometer, R.array.id_tlm_val,    R.array.id_tlm_txt,    Defaults.EQ_MODE_TLM));
        mData.add(new SettingList(Prefs.KEY_ALT_ID_METHOD, "AltBeacon",     R.drawable.ic_hexagon_outline, R.array.id_beacon_val, R.array.id_beacon_txt, Defaults.EQ_MODE_ALT));
        mData.add(new SettingHeader("Scan tweaks"));
        mData.add(new SettingCheckbox(Prefs.KEY_SCAN_ON_BLUETOOTH,  "Scan on Bluetooth",     "Start scan when BT turns on.", R.drawable.ic_radar,         Defaults.SCAN_ON_BLUETOOTH));
        mData.add(new SettingCheckbox(Prefs.KEY_SCAN_ON_BOOT,       "Scan on boot",          "Start scan when phone boots.", R.drawable.ic_radar,         Defaults.SCAN_ON_BOOT));
        mData.add(new SettingCheckbox(Prefs.KEY_BLUETOOTH_ON_BOOT,  "Bluetooth on boot",     "Turn BT on when phone boots.", R.drawable.ic_bluetooth,    Defaults.BLUETOOTH_ON_BOOT));
        mData.add(new SettingInteger(Prefs.KEY_CLEANUP_AFTER, "Remove old beacons", R.drawable.ic_delete));
        mData.add(new SettingReset("Reset settings", "Set defaults for everything above.", R.drawable.ic_backup_restore));
        mData.add(new SettingHeader("UI tweaks"));
        mData.add(new SettingList(Prefs.KEY_FAB_BEHAVIOR, "FAB Behavior", R.drawable.ic_play_circle, R.array.fab_mode_val, R.array.fab_mode_txt, Defaults.FAB_BEHAVIOR));
    }

    SettingClickListener mClickListener;
    Context mAppContext;

    public interface SettingClickListener {
        void onTimingSettingClick(View view, int position, SettingTiming setting);
        void onIntegerSettingClick(View view, int position, SettingInteger integer);
        void onListSettingClick(View view, int position, SettingHolder holder);
        void onCheckboxSettingClick(View view, int position, SettingHolder holder); //TODO diversify params
        void onResetSettingClick(View view, int position, SettingReset setting);    //TODO diversify params;
    }

    public SettingsAdapter(Context appContext, boolean footer) {
        mAppContext = appContext;
        if(footer) mData.add(new SettingFooter("Footer"));
    }

    public void setSettingClickListener(SettingClickListener listener) {
        this.mClickListener = listener;
    }
    public void setFooter(boolean footer) {
        boolean hasFooter = mData.get(mData.size()-1).getType() == Setting.TYPE_FOOTER;
        if(footer && !hasFooter) {
            mData.add(new SettingFooter("Footer"));
            notifyItemInserted(mData.size()-1);
        } else if (!footer && hasFooter) {
            mData.remove(mData.size()-1);
            notifyItemRemoved(mData.size());
        }
    }

    public int getPosition(String key) {
        for (int i = 0; i < mData.size(); i++) {
            if(mData.get(i).key.equals(key)) return i;
        }
        return -1;
    }

    ArrayList<Setting> getData() {
        return mData;
    }

    @Override
    public SettingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case Setting.TYPE_HEADER:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_header, parent, false);
                //itemView = SettingHeaderLayout.inflateLayoutInto(R.layout., parent);
                break;
            case Setting.TYPE_CHECKBOX:
                itemView = IconCheckboxLayout.inflateLayoutInto(
                        R.layout.item_setting_icon_checkbox, parent);
                break;
            case Setting.TYPE_FOOTER:
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.preference_space, parent, false);
                break;
            default:
                itemView = IconLayout.inflateLayoutInto(
                        R.layout.item_setting_icon, parent);
                break;
        }
        //View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        SettingHolder holder = new SettingHolder(itemView, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(SettingHolder holder, int position) {
        Setting setting = mData.get(position);
        holder.setting = setting;
        String title = setting.getTitle();
        String summary = setting.getSummary();
        if(holder.title != null) holder.title.setText(title);
        switch (holder.type) {
            case Setting.TYPE_HEADER:
                break;
            case Setting.TYPE_CHECKBOX:
                SettingCheckbox scb = (SettingCheckbox) setting;
                holder.summary.setText(summary);
                holder.cbox.setChecked(scb.getChecked());
                holder.icon.setImageResource(scb.getIcon());
                holder.icon.getDrawable().mutate();
                holder.icon.setColorFilter(mAppContext.getResources().getColorStateList(R.color.setting_icon_selector));
                holder.icon.setActivated(scb.isWarning());
                break;
            case Setting.TYPE_FOOTER:
                break;
            default:
                //Must have icon. (Only header has no icon.)
                SettingIcon si = (SettingIcon) setting;
                holder.summary.setText(summary);
                holder.icon.setImageResource(si.getIcon());
                holder.icon.getDrawable().mutate();
                holder.icon.setColorFilter(mAppContext.getResources().getColorStateList(R.color.setting_icon_selector));
                holder.icon.setActivated(si.isWarning());
                //if(si.isWarning()) holder.icon.setColorFilter();
                //else holder.icon.setColorFilter();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }

    public class SettingHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public Setting setting;
        public int type;

        public TintableImageView icon;
        public TextView title, summary;
        public CheckBox cbox;

        public SettingHolder(View itemView, int type) {
            super(itemView);
            this.type = type;
            icon = (TintableImageView) itemView.findViewById(R.id.icon);
            title = (TextView) itemView.findViewById(R.id.text);
            summary = (TextView) itemView.findViewById(R.id.sub);
            cbox = (CheckBox) itemView.findViewById(R.id.box);
            itemView.setOnClickListener(SettingHolder.this);
        }

        @Override
        public void onClick(View view) {
            switch (type) {
                case Setting.TYPE_DIALOG_TIMING:
                    if(mClickListener != null) mClickListener.onTimingSettingClick(view, SettingHolder.this.getAdapterPosition(), (SettingTiming) setting);
                    break;
                case Setting.TYPE_DIALOG_LIST:
                    if(mClickListener != null) mClickListener.onListSettingClick(view, SettingHolder.this.getAdapterPosition(), SettingHolder.this);
                    break;
                case Setting.TYPE_DIALOG_RESET:
                    if(mClickListener != null) mClickListener.onResetSettingClick(view, SettingHolder.this.getAdapterPosition(), (SettingReset) setting);
                    break;
                case Setting.TYPE_DIALOG_INTEGER:
                    if(mClickListener != null) mClickListener.onIntegerSettingClick(view, SettingHolder.this.getAdapterPosition(), (SettingInteger) setting);
                    break;
                case Setting.TYPE_CHECKBOX:
                    if(mClickListener != null) mClickListener.onCheckboxSettingClick(view, SettingHolder.this.getAdapterPosition(), SettingHolder.this);
                    break;
            }
            //Pass click to fragment if dialog is needed
            //Then dialog communicates with mPresenter, changes data.
            //Then it's enough to call notifyItemChanged when closing dialog.
            //View should update itself via onBindViewHolder
            //Also, view has contact with mPresenter.
        }
    }
}
