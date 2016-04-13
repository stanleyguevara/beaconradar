package net.beaconradar.logread;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.history.RecyclerViewCursorIdClickListener;
import net.beaconradar.service.Scanner;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.utils.IconsPalette;
import net.beaconradar.utils.CircleImageView;
import net.beaconradar.utils.CursorRecyclerAdapter;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

import hugo.weaving.DebugLog;

public class LogAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> {
    public static final int EVENT_USER = 0;
    public static final int EVENT_SCAN = 1;
    public static final int EVENT_UPDATE = 2;
    public static final int EVENT_SPOTTED = 3;
    public static final int EVENT_REMOVED = 4;
    public static final int EVENT_LOG = 5;

    public static final int TYPE_FOOTER = 6;
    public static final int TYPE_EVENT = 7;

    private int mEvent, mId, mId0, mId1, mId2, mIcon, mColor, mName, mType, mEqMode, mMac, mOther, mTime, mTx;  //TODO join, foreign key?
    private static RecyclerViewCursorIdClickListener mClickListener;
    private boolean mFooter = false;
    SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM HH:mm");

    LogAdapter(Cursor cursor, RecyclerViewCursorIdClickListener listener, boolean footer) {
        super(cursor);
        this.mClickListener = listener;
        if(cursor != null) {
            mIcon = cursor.getColumnIndex("icon");
            mColor = cursor.getColumnIndex("color");
            mName = cursor.getColumnIndex("name");
            mType = cursor.getColumnIndex("type");
            mEqMode = cursor.getColumnIndex("eq_mode");

            mEvent = cursor.getColumnIndex("event");
            mId = cursor.getColumnIndex("_id");
            mId0 = cursor.getColumnIndex("id0");
            mId1 = cursor.getColumnIndex("id1");
            mId2 = cursor.getColumnIndex("id2");
            mTime = cursor.getColumnIndex("time");
            mMac = cursor.getColumnIndex("mac");
            mOther = cursor.getColumnIndex("other");
            mTx = cursor.getColumnIndex("tx");
        }
        mFooter = footer;
    }

    public void setFooter(boolean footer) {
        if(footer && !mFooter) {
            this.mFooter = true;
            notifyItemInserted(getItemCount());
        } else if (!footer && mFooter) {
            this.mFooter = false;
            notifyItemRemoved(getItemCount());
        }
    }

    public boolean hasFooter() {
        return mFooter;
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + (mFooter ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if(mFooter && position == getItemCount() -1) return TYPE_FOOTER;
        else return TYPE_EVENT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case TYPE_EVENT:
                itemView = inflater.inflate(R.layout.item_log, parent, false);
                holder = new CursorHolder(itemView);
                break;
            default:
            case TYPE_FOOTER:
                itemView = inflater.inflate(R.layout.preference_space, parent, false);
                holder = new FooterHolder(itemView);
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof CursorHolder) super.onBindViewHolder(holder, position);    //This calls onBindViewHolderCursor internally
        else onBindViewHolderFooter(holder, position);
    }

    public void onBindViewHolderFooter(RecyclerView.ViewHolder generic_holder, int position) {

    }

    @Override
    public void onBindViewHolderCursor(RecyclerView.ViewHolder generic_holder, Cursor cursor) {
        CursorHolder holder = (CursorHolder) generic_holder;
        long id = cursor.getLong(mId);
        int icon = IconsPalette.getResId(cursor.getString(mIcon));
        int color = cursor.getInt(mColor);
        int type = cursor.getInt(mType);
        int eq = cursor.getInt(mEqMode);
        String id0 = cursor.getString(mId0);
        String id1 = cursor.getString(mId1);
        String id2 = cursor.getString(mId2);
        String mac = cursor.getString(mMac);
        String name = cursor.getString(mName);
        long time = cursor.getLong(mTime);
        int event = cursor.getInt(mEvent);

        holder.id = id;
        holder.event = event;
        holder.time.setText(TimeFormat.sdf6.format(new Date(time)));

        switch (holder.event) {
            case Scanner.EVENT_UPDATE:
                bindBeacon(holder, eq, type, id0, id1, id2, mac, cursor.getString(mOther));
                holder.set("U "+name, color, icon);
                break;
            case Scanner.EVENT_SPOTTED:
                bindBeacon(holder, eq, type, id0, id1, id2, mac, cursor.getString(mOther));
                holder.set("S " + name, color, icon);;
                break;
            case Scanner.EVENT_LOG_START:
                holder.set("Log start", R.color.graphite , R.drawable.ic_record_rec);
                break;
            case Scanner.EVENT_LOG_STOP:
                holder.set("Log pause", R.color.graphite, R.drawable.ic_record_rec);
                break;
            case Scanner.EVENT_SCAN_START:
                holder.set("Scan start", R.color.graphite, R.drawable.ic_play);
                break;
            case Scanner.EVENT_SCAN_STOP:
                holder.set("Scan stop", R.color.graphite, R.drawable.ic_pause);
                break;
            case Scanner.EVENT_USER_START:
                holder.set("User start", R.color.graphite, R.drawable.ic_play_circle);
                break;
            case Scanner.EVENT_USER_STOP:
                holder.set("User pause", R.color.graphite, R.drawable.ic_close_circle);
                break;
            case Scanner.EVENT_REMOVED_INACTIVE:
                bindBeacon(holder, eq, type, id0, id1, id2, mac, cursor.getString(mOther));
                holder.set("RI "+name, color, icon);
                break;
            case Scanner.EVENT_REMOVED_FULL:
                bindBeacon(holder, eq, type, id0, id1, id2, mac, cursor.getString(mOther));
                holder.set("RF "+name, color, icon);
                break;
            case Scanner.EVENT_MODE_CHANGE:
                holder.set(cursor.getString(mOther), R.color.graphite, R.drawable.ic_hexagon_outline);
                break;
        }
        holder.icon.getDrawable().mutate();
    }

    private void bindBeacon(CursorHolder holder, int eq, int type, String id0, String id1, String id2, String mac, String other) {
        switch (type) {
            case ID.T_IBC:
                IBC ibc = new IBC(eq, id0, Integer.valueOf(id1), Integer.valueOf(id2), mac, 0, 0);
                holder.identifier = ibc;
                //holder.essential.setText("Major: "+ibc.major+ " Minor: "+ibc.minor);
                break;
            case ID.T_UID:
                UID uid = new UID(eq, id0, id1, mac, 0, 0);
                holder.identifier = uid;
                //holder.essential.setText("Instance: "+uid.instance);
                break;
            case ID.T_URL:
                URL url = new URL(eq, id0, mac, 0, 0);
                holder.identifier = url;
                //holder.essential.setText(url.url);
                break;
            case ID.T_TLM:
                String[] str_tlm = other.split("\\|");
                TLM tlm = new TLM(
                        eq,
                        Integer.valueOf(str_tlm[0]),
                        Float.valueOf(str_tlm[1]),
                        Long.valueOf(str_tlm[2]),
                        Long.valueOf(str_tlm[3]),
                        mac,
                        0,
                        0);
                holder.identifier = tlm;
                /*holder.essential.setText(
                        "Batt: "+tlm.getBattString()+" "+
                                "Temp: "+String.format("%.2f", tlm.temp));*/
                break;
            case ID.T_EDD:
                EDD edd = new EDD(mac, 0, 0);
                holder.identifier = edd;
                /*if(edd.instance != null) {
                    holder.essential.setText("Instance: "+edd.instance);
                } else {
                    if(edd.url != null) {
                        holder.essential.setText("Url: "+edd.url);
                    } else {
                        if(edd.frameTLM != null) {
                            holder.essential.setText(
                                    "Batt: "+edd.frameTLM.getBattString()+" "+
                                            "Temp: "+String.format("%.2f", edd.frameTLM.temp)
                            );
                        }
                    }
                }*/
                break;
            default:
                holder.identifier = null;
        }
    }

    public class CursorHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        public long id;
        public int event;
        public ID identifier;
        public CircleImageView icon;
        public TextView name, time;

        public CursorHolder(View itemView) {
            super(itemView);
            icon = (CircleImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
            time = (TextView) itemView.findViewById(R.id.time);
            itemView.setOnClickListener(CursorHolder.this);
            itemView.setOnLongClickListener(CursorHolder.this);
        }

        public void set(String name, int color, int icon) {
            this.name.setText(name);
            this.icon.setCircleColor(color);
            this.icon.setImageResource(icon);
        }

        @Override @DebugLog
        public void onClick(View view) {
            mClickListener.onClick(TYPE_EVENT, view, this.getLayoutPosition(), id, CursorHolder.this); //In case we want to pass this viewholder use HistoryAdapter.ViewHolder.this
        }

        @Override @DebugLog
        public boolean onLongClick(View view) {
            mClickListener.onLongClick(TYPE_EVENT, view, this.getLayoutPosition(), id, CursorHolder.this);
            return true;
        }
    }

    public class FooterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public FooterHolder(View itemView) { super(itemView); }

        @Override
        public void onClick(View v) {

        }
    }
}
