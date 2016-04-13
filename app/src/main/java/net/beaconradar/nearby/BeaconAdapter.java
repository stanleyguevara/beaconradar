package net.beaconradar.nearby;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import net.beaconradar.R;
import net.beaconradar.service.id.Beacon;
import net.beaconradar.service.id.ID;
import net.beaconradar.service.id.eddystone.EDD;
import net.beaconradar.service.id.eddystone.TLM;
import net.beaconradar.service.id.eddystone.UID;
import net.beaconradar.service.id.eddystone.URL;
import net.beaconradar.service.id.ibeacon.IBC;
import net.beaconradar.utils.CircleImageView;
import net.beaconradar.utils.TimeFormat;

import java.util.ArrayList;
import java.util.HashSet;

import hugo.weaving.DebugLog;

public class BeaconAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private String TAG = getClass().getName();

    public static final int TYPE_BEACON = 1;
    public static final int TYPE_FOOTER = 2;

    private static final int BAR_STICK_OUT_PX = 10;

    private ArrayList<Beacon> mBeacons = new ArrayList<>();
    private ValueAnimator mAnimator;
    private RecyclerViewClickListener mClickListener;
    private boolean mFooter = false;
    private int mWidth;
    private long mNow;
    private HashSet<BeaconHolder> mVisible = new HashSet<>(32);    //Well, almost visible. Plus cached by RV.

    public BeaconAdapter(RecyclerViewClickListener listener, Context context, boolean footer) {
        mClickListener = listener;
        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setDuration(300);
        mAnimator.setFloatValues(0, 1);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                for(BeaconHolder holder : mVisible) {
                    if(holder.beacon.isHot() && holder.beacon.getTx() != ID.NO_TX_POWER) {
                        holder.animateSignal(va.getAnimatedFraction());
                    }
                }
            }
        });
        mFooter = footer;
    }

    public void setClickListener(RecyclerViewClickListener listener) {
        mClickListener = listener;
    }

    public void setData(ArrayList<Beacon> data) {
        mBeacons = data;
    }

    @DebugLog
    public void setFooter(boolean footer) {
        if(footer && !mFooter) {
            mFooter = footer;
            notifyItemInserted(getItemCount());
        } else if (!footer && mFooter) {
            mFooter = footer;
            notifyItemRemoved(getItemCount());
        }
    }

    public void setBarWidth(int px) {
        mWidth = px - BAR_STICK_OUT_PX;
        for(BeaconHolder holder : mVisible) {
            animBars(holder, holder.beacon);
        }
    }

    public boolean hasFooter() {
        return mFooter;
    }

    public ArrayList<Beacon> getData() {
        return mBeacons;
    }

    public void kickAnim() {
        mNow = System.currentTimeMillis();
        for(BeaconHolder holder : mVisible) {
            bindDynamicData(holder, holder.beacon);
        }
        mAnimator.start();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case TYPE_BEACON:
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_beacon_5, parent, false);
                holder = new BeaconHolder(itemView);
                break;
            case TYPE_FOOTER:
                View footerLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.preference_space, parent, false);
                holder = new FooterHolder(footerLayout);
                break;
            default:
                return null;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if(viewHolder instanceof BeaconHolder) {
            BeaconHolder holder = (BeaconHolder) viewHolder;
            mVisible.add(holder);
            Beacon beacon = mBeacons.get(position);
            holder.beacon = beacon;
            holder.position = position;

            mNow = System.currentTimeMillis();
            bindDynamicData(holder, beacon);

            int color = beacon.getColor();
            holder.name.setText(beacon.getName());
            holder.icon.setImageResource(beacon.getIcon());
            holder.icon.getDrawable().mutate();
            holder.icon.setCircleColor(color);
            holder.seen_icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            holder.signal.setBackgroundColor(Color.argb(64, Color.red(color), Color.green(color), Color.blue(color)));  //alpha 0.2
            holder.meters.setBackgroundColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color))); //alpha 0.4

            animBars(holder, beacon);
        } else if (viewHolder instanceof FooterHolder) {
            //Nuffin.
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        if(viewHolder.getClass() == BeaconHolder.class) {
            BeaconHolder holder = (BeaconHolder) viewHolder;
            mVisible.remove(holder);
        }
        super.onViewRecycled(viewHolder);
    }

    @Override
    public long getItemId(int position) {
        if(position < mBeacons.size()) return mBeacons.get(position).getDbId();
        else return -2; //On the off chance that it would interfere with RecyclerView's NO_ID = -1
    }

    @Override
    public int getItemViewType(int position) {
        if(position == mBeacons.size()) return TYPE_FOOTER;
        else return TYPE_BEACON;
    }

    @Override
    public int getItemCount() {
        //if(mBeacons.size() == 0) return 0;
        //else return mBeacons.size() + (mFooter ? 1 : 0);
        return mBeacons.size() + (mFooter ? 1 : 0);
    }

    private void bindDynamicData(BeaconHolder holder, Beacon beacon) {
        ID identifier = beacon.getId();
        switch (identifier.getType()) {
            case ID.T_IBC:
                IBC ibc = (IBC) identifier;
                holder.essential.setText("Major: "+ibc.major+ " Minor: "+ibc.minor/*+" UUID: "+ibc.prox_uuid*/);
                holder.signal_txt.setText(beacon.getRssi() + " dBm");
                holder.meters_txt.setText(String.format("%.2f", beacon.getDist()) + " m");
                break;
            case ID.T_UID:
                UID uid = (UID) identifier;
                holder.essential.setText("Instance: "+uid.instance/*+" Namespace: "+uid.namespace*/);
                holder.signal_txt.setText(beacon.getRssi() + " dBm");
                holder.meters_txt.setText(String.format("%.2f", beacon.getDist()) + " m");
                break;
            case ID.T_URL:
                URL url = (URL) identifier;
                holder.essential.setText(url.url);
                holder.signal_txt.setText(beacon.getRssi() + " dBm");
                holder.meters_txt.setText(String.format("%.2f", beacon.getDist()) + " m");
                break;
            case ID.T_TLM:
                TLM tlm = (TLM) identifier;
                holder.essential.setText(
                        "Batt: "+tlm.getBattString()+" "+
                        "Temp: "+String.format("%.2f", tlm.temp));
                holder.signal_txt.setText(beacon.getRssi() + " dBm");
                if(beacon.getTx() == ID.NO_TX_POWER) {
                    holder.meters_txt.setText("N/A, unknown Tx Power");
                } else {
                    holder.meters_txt.setText(String.format("%.2f", beacon.getDist()) + " m (inferred)");
                }
                break;
            case ID.T_EDD:
                EDD edd = (EDD) identifier;
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
                holder.signal_txt.setText(beacon.getRssi() + " dBm");
                if(beacon.getTx() == ID.NO_TX_POWER) {
                    holder.meters_txt.setText("N/A, unknown Tx Power");
                } else {
                    holder.meters_txt.setText(String.format("%.2f", beacon.getDist()) + " m");
                }
                break;
        }
        holder.seen.setText(TimeFormat.getTimeAgoShort(beacon.getLastSeen(), mNow - beacon.getLastSeen()));
        holder.seen_icon.setVisibility(beacon.isHot() ? View.VISIBLE : View.GONE);
    }

    private void animBars(BeaconHolder holder, Beacon beacon) {
        if(beacon.getTx() != ID.NO_TX_POWER) {
            if(mAnimator.isRunning() && beacon.isHot()) {
                holder.animateSignal(mAnimator.getAnimatedFraction());
            } else holder.animateSignal(1.0f);
        } else {
            holder.signal.setTranslationX(0);
            holder.meters.setTranslationX(0);
        }
    }

    public class FooterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public FooterHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onClick(View view) {
            mClickListener.onClick(view, this.getLayoutPosition(), TYPE_FOOTER, FooterHolder.this);
        }
    }

    public class BeaconHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public int position;
        public Beacon beacon;
        public TextView name;
        public TextView essential;
        public TextView meters_txt, signal_txt;
        public TextView seen;
        public CircleImageView icon;
        public ImageView seen_icon;
        public View signal, meters;

        public BeaconHolder(final View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            essential = (TextView) itemView.findViewById(R.id.essential);
            signal = itemView.findViewById(R.id.beacon_signal_bar);
            meters = itemView.findViewById(R.id.beacon_distance_bar);
            icon = (CircleImageView) itemView.findViewById(R.id.icon);
            meters_txt = (TextView) itemView.findViewById(R.id.distance_txt);
            signal_txt = (TextView) itemView.findViewById(R.id.signal_txt);
            seen_icon = (ImageView) itemView.findViewById(R.id.seen_icon);
            seen = (TextView) itemView.findViewById(R.id.seen);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mClickListener.onClick(view, this.getLayoutPosition(), TYPE_BEACON, BeaconHolder.this);
        }

        @Override
        public boolean onLongClick(View view) {
            mClickListener.onLongClick(view, this.getLayoutPosition(), TYPE_BEACON, BeaconHolder.this);
            return true;
        }

        public void animateSignal(float fraction) {
            /*Log.v("ANIM", "fraction "+fraction+" mWidth "+mWidth+
                    " rmax "+beacon.getRssiMax()+" rmin "+beacon.getRssiMin()+
                    " rprev" +beacon.getRssiPrev()+" rssi "+beacon.getRssi());*/
            {
                double max = beacon.getRssiMax() - beacon.getRssiMin();
                double coeff = (double) mWidth / max;
                double anim_rssi = -beacon.getRssiMax() + beacon.getRssiPrev() + ((beacon.getRssi() - beacon.getRssiPrev()) * fraction);
                int result = (int)(anim_rssi * coeff);
                signal.setTranslationX(result);
                //Log.v("ANIM", "result: "+result);
            }

            {
                double max = beacon.getDistMax() - beacon.getDistMin();
                double coeff = (double) mWidth / max;
                double anim_dist = beacon.getDistPrev() + ((beacon.getDist() - beacon.getDistPrev()) * fraction);
                int result = (int)(anim_dist * coeff);
                meters.setTranslationX(-result);
            }
        }
    }
}
