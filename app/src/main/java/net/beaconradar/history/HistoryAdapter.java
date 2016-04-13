package net.beaconradar.history;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.utils.IconsPalette;
import net.beaconradar.service.RawBeacon;
import net.beaconradar.utils.CircleImageView;
import net.beaconradar.utils.CursorRecyclerAdapter;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;

import hugo.weaving.DebugLog;

public class HistoryAdapter extends CursorRecyclerAdapter<RecyclerView.ViewHolder> {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_BEACON = 1;
    public static final int TYPE_FOOTER = 2;

    private int mId, mId0, mId1, mId2, mIcon, mColor, mName, mType, mEqMode, mMac, mOther, mSeen;
    private static RecyclerViewCursorIdClickListener mClickListener;
    private LinkedHashMap<ID, Beacon> mScanData;
    private boolean mHeader = false, mFooter = false;
    private boolean mIsRecording = false;
    private long mLogStarted = 0;
    private long mLogPaused = 0;
    SimpleDateFormat sdf1 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    SimpleDateFormat sdf2 = new SimpleDateFormat("dd.MM HH:mm");
    private HashSet<CursorHolder> mVisible = new HashSet<>(32);    //Well, almost visible. Plus cached by RV.

    HistoryAdapter(Cursor cursor, RecyclerViewCursorIdClickListener listener, @Nullable LinkedHashMap<ID, Beacon> scanData, boolean footer) {
        super(cursor);
        this.mClickListener = listener;
        if(cursor != null) {
            mId = cursor.getColumnIndex("_id");
            mIcon = cursor.getColumnIndex("icon");
            mColor = cursor.getColumnIndex("color");
            mId0 = cursor.getColumnIndex("id0");
            mId1 = cursor.getColumnIndex("id1");
            mId2 = cursor.getColumnIndex("id2");
            mName = cursor.getColumnIndex("name");
            mSeen = cursor.getColumnIndex("last_seen");
            mType = cursor.getColumnIndex("type");
            mEqMode = cursor.getColumnIndex("eq_mode");
            mMac = cursor.getColumnIndex("mac");
            mOther = cursor.getColumnIndex("other");
        }
        this.mScanData = scanData;
        mFooter = footer;
    }

    public void setScanData(LinkedHashMap<ID, Beacon> data) {
        this.mScanData = data;
    }

    public void setHasHeader(boolean header) {
        if(header != mHeader) {
            this.mHeader = header;
            if(mHeader) notifyItemInserted(0);
            else notifyItemRemoved(0);
        }
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

    public void setIsRecording(boolean recording, long started, long paused) {
        this.mIsRecording = recording;
        mLogStarted = started;
        mLogPaused = paused;
        notifyItemChanged(0);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + (mFooter ? 1 : 0) + (mHeader ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if(mHeader && position == 0) return TYPE_HEADER;
        else if(mFooter && position == getItemCount() -1) return TYPE_FOOTER;
        else return TYPE_BEACON;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case TYPE_BEACON:
                itemView = inflater.inflate(R.layout.item_history2, parent, false);
                holder = new CursorHolder(itemView);
                break;
            case TYPE_HEADER:
                itemView = inflater.inflate(R.layout.item_history_header, parent, false);
                holder = new HeaderHolder(itemView);
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
        if(holder instanceof CursorHolder) super.onBindViewHolder(holder, position - (mHeader ? 1 : 0));    //This calls onBindViewHolderCursor internally
        else if (holder instanceof HeaderHolder) onBindViewHolderHeader(holder, position);
        else onBindViewHolderFooter(holder, position);
    }

    public void onBindViewHolderFooter(RecyclerView.ViewHolder generic_holder, int position) {

    }

    public void onBindViewHolderHeader(RecyclerView.ViewHolder generic_holder, int position) {
        HeaderHolder holder = (HeaderHolder) generic_holder;
        holder.icon.setCircleColor(ContextCompat.getColor(holder.icon.getContext(), R.color.white));
        holder.name.setText(sdf1.format(new Date(mLogStarted)));
        holder.icon.getDrawable().setColorFilter(
                (mIsRecording ?
                        ContextCompat.getColor(holder.icon.getContext(), R.color.warningText) :
                        ContextCompat.getColor(holder.icon.getContext(), R.color.graphite)),
                PorterDuff.Mode.SRC_IN
        );
        holder.essential.setText(mIsRecording ? "Logging in progress..." : "Paused at "+sdf2.format(new Date(mLogPaused)));
    }

    public void updateTimers() {
        if(mScanData == null) return;
        long now = System.currentTimeMillis();
        for(CursorHolder holder : mVisible) {
            Beacon beacon = mScanData.get(holder.identifier);
            if(beacon != null) {
                if(beacon.isHot()) holder.seen_icon.setVisibility(View.VISIBLE);
                else holder.seen_icon.setVisibility(View.GONE);
                holder.seen.setText(TimeFormat.getTimeAgoShort(beacon.getLastSeen(), now - beacon.getLastSeen()));
            } else {
                holder.seen_icon.setVisibility(View.GONE);
                holder.seen.setText(TimeFormat.getTimeAgoShort(holder.last_seen, now - holder.last_seen));
            }
        }
    }

    @Override @DebugLog
    public void onBindViewHolderCursor(RecyclerView.ViewHolder generic_holder, Cursor cursor) {
        CursorHolder holder = (CursorHolder) generic_holder;
        mVisible.add(holder);
        long id = cursor.getLong(mId);
        int icon = IconsPalette.getResId(cursor.getString(mIcon));
        int color = cursor.getInt(mColor);
        int type = cursor.getInt(mType);
        int eq = cursor.getInt(mEqMode);
        String id0 = cursor.getString(mId0);
        String id1 = cursor.getString(mId1);
        String id2 = cursor.getString(mId2);
        String mac = cursor.getString(mMac);
        long seen = cursor.getLong(mSeen);
        switch (type) {
            case ID.T_IBC:
                IBC ibc = new IBC(eq, id0, Integer.valueOf(id1), Integer.valueOf(id2), mac, 0, 0);
                holder.identifier = ibc;
                holder.essential.setText("Major: "+ibc.major+ " Minor: "+ibc.minor);
                break;
            case ID.T_UID:
                UID uid = new UID(eq, id0, id1, mac, 0, 0);
                holder.identifier = uid;
                holder.essential.setText("Instance: "+uid.instance);
                break;
            case ID.T_URL:
                URL url = new URL(eq, id0, mac, 0, 0);
                holder.identifier = url;
                holder.essential.setText(url.url);
                break;
            case ID.T_TLM:
                String other = cursor.getString(mOther);
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
                holder.essential.setText(
                        "Batt: "+tlm.getBattString()+" "+
                        "Temp: "+String.format("%.2f", tlm.temp));
                break;
            case ID.T_EDD:
                EDD edd = new EDD(mac, 0, 0);
                holder.identifier = edd;
                if(edd.instance != null) {
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
                }
                break;
            default:
                holder.identifier = null;
        }
        holder.id = id;
        holder.icon.setImageResource(icon);
        holder.icon.setCircleColor(color);
        holder.name.setText(cursor.getString(mName));
        switch (type) {
            case RawBeacon.TYPE_IBEACON:
                holder.essential.setText("Major: "+id0+ " Minor: "+id1);
                break;
            case RawBeacon.TYPE_EDDYSTONE:
                if("".equals(id1)) holder.essential.setText(id2); //URL
                else holder.essential.setText("Instance: "+id1); //Instance
                break;
        }
        //holder.essential.setHighlighted(true); Only for marquee. Not needed now.
        long now = System.currentTimeMillis();
        holder.seen_icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if(mScanData != null) {
            Beacon beacon = mScanData.get(holder.identifier);
            if(beacon != null) {
                if(beacon.isHot()) holder.seen_icon.setVisibility(View.VISIBLE);
                else holder.seen_icon.setVisibility(View.GONE);
                holder.seen.setText(TimeFormat.getTimeAgoShort(beacon.getLastSeen(), now - beacon.getLastSeen()));
                holder.last_seen = beacon.getLastSeen();
            } else {
                holder.seen.setText(TimeFormat.getTimeAgoShort(seen, now - seen));
                holder.seen_icon.setVisibility(View.GONE);
                holder.last_seen = seen;
            }
        } else {
            holder.seen.setText(TimeFormat.getTimeAgoShort(seen, now - seen));
            holder.seen_icon.setVisibility(View.GONE);
            holder.last_seen = seen;
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        if(viewHolder.getClass() == CursorHolder.class) {
            CursorHolder holder = (CursorHolder) viewHolder;
            mVisible.remove(holder);
        }
        super.onViewRecycled(viewHolder);
    }

    public class HeaderHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public CircleImageView icon;
        public TextView name, essential;
        public ImageView delete;

        public HeaderHolder(final View itemView) {
            super(itemView);
            icon = (CircleImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
            essential = (TextView) itemView.findViewById(R.id.essential);
            delete = (ImageView) itemView.findViewById(R.id.delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HeaderHolder.this.onLongClick(itemView);
                }
            });
            itemView.setOnClickListener(HeaderHolder.this);
        }

        @Override @DebugLog
        public void onClick(View view) {
            mClickListener.onClick(HistoryAdapter.TYPE_HEADER, view, this.getLayoutPosition(), -1, HeaderHolder.this);
        }

        //Not actually used to do long click, just using interface mehtod for delete action.
        @Override @DebugLog
        public boolean onLongClick(View view) {
            mClickListener.onLongClick(HistoryAdapter.TYPE_HEADER, view, this.getLayoutPosition(), -1, HeaderHolder.this);
            return true;
        }
    }

    public class FooterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public FooterHolder(View itemView) { super(itemView); }

        @Override
        public void onClick(View v) {

        }
    }

    public class CursorHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        public long id;
        public ID identifier;
        public CircleImageView icon;
        public ImageView seen_icon;
        public TextView name, essential, seen, date;
        public long last_seen;

        public CursorHolder(View itemView) {
            super(itemView);
            icon = (CircleImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
            essential = (TextView) itemView.findViewById(R.id.essential);
            seen = (TextView) itemView.findViewById(R.id.seen);
            date = (TextView) itemView.findViewById(R.id.date);
            seen_icon = (ImageView) itemView.findViewById(R.id.seen_icon);
            itemView.setOnClickListener(CursorHolder.this);
            itemView.setOnLongClickListener(CursorHolder.this);
        }

        @Override @DebugLog
        public void onClick(View view) {
            mClickListener.onClick(HistoryAdapter.TYPE_BEACON, view, this.getLayoutPosition(), id, CursorHolder.this); //In case we want to pass this viewholder use HistoryAdapter.ViewHolder.this
        }

        @Override @DebugLog
        public boolean onLongClick(View view) {
            mClickListener.onLongClick(HistoryAdapter.TYPE_BEACON, view, this.getLayoutPosition(), id, CursorHolder.this);
            return true;
        }
    }
}
