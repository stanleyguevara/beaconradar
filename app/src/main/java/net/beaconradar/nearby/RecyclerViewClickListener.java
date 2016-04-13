package net.beaconradar.nearby;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public interface RecyclerViewClickListener {
    public void onClick(View view, int position, int type, RecyclerView.ViewHolder viewHolder);
    public void onLongClick(View view, int position, int type, RecyclerView.ViewHolder viewHolder);
}
